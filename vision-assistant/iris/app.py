"""Wires the sensors, the brain and the outputs together."""

from __future__ import annotations

import logging
import queue
import sys
import threading

import anthropic

from . import events
from .audio.speaker import Speaker
from .automation import Automation
from .brain import Brain, request_extras
from .config import Config
from .devices import make_backend
from .wake import WakeGate

log = logging.getLogger(__name__)


class App:
    def __init__(
        self,
        cfg: Config,
        text_mode: bool = False,
        use_camera: bool = True,
        use_speech: bool = True,
        client: anthropic.Anthropic | None = None,
    ) -> None:
        self.cfg = cfg
        self.text_mode = text_mode
        self.bus = events.EventBus()
        self.devices = make_backend(cfg)
        self.client = client or anthropic.Anthropic()
        self.speaker = Speaker(cfg.piper_model, enabled=use_speech)
        self.gate = WakeGate(cfg.wake_words, cfg.followup_seconds)
        self.threads: list[threading.Thread] = []

        self.camera = None
        self.activity = None
        if use_camera:
            from .vision.activity import ActivityMonitor, ActivityRecognizer
            from .vision.camera import Camera
            from .vision.gestures import GestureWatcher

            self.camera = Camera(cfg.camera_index, cfg.frame_width, cfg.frame_height)
            recognizer = ActivityRecognizer(
                self.client, cfg.scene_model, cfg.activities, request_extras(cfg)
            )
            self.activity = ActivityMonitor(cfg, self.camera, recognizer, self.bus)
            self.threads += [self.camera, GestureWatcher(cfg, self.camera, self.bus), self.activity]

        self.mic = None
        if not text_mode:
            from .audio.listener import MicListener

            self.mic = MicListener(cfg, self.bus)
            self.threads.append(self.mic)

        self.brain = Brain(
            cfg,
            self.client,
            self.devices,
            self.bus,
            get_frame=self.camera.latest if self.camera else lambda: None,
            get_activity=(lambda: self.activity.current) if self.activity else lambda: None,
        )
        self.automation = Automation(
            cfg.rules,
            self.devices,
            say=self.say,
            builtins={"stop": self.stop_talking, "listen": self.start_listening},
            cooldown=cfg.rule_cooldown_seconds,
        )
        # Talking to Claude and speaking are slow, so they run on a worker
        # thread; the main loop stays free to react to gestures meanwhile.
        self._jobs: queue.Queue = queue.Queue()
        self._worker = threading.Thread(target=self._work, name="conversation", daemon=True)

    # ------------------------------------------------------------- actions

    def say(self, text: str, then_listen: bool = False) -> None:
        self._jobs.put(("say", text, then_listen))

    def stop_talking(self) -> None:
        """Cut off speech and drop anything queued to be said."""
        while True:
            try:
                self._jobs.get_nowait()
                self._jobs.task_done()
            except queue.Empty:
                break
        self.speaker.stop()
        self.gate.close()

    def start_listening(self) -> None:
        """Accept the next utterance without the wake word."""
        log.info("Listening for a command...")
        self.gate.open()

    # ------------------------------------------------------- conversation

    def _work(self) -> None:
        while True:
            job = self._jobs.get()
            try:
                kind, payload, then_listen = job
                if kind == "ask":
                    text = self._think(lambda: self.brain.ask(payload))
                elif kind == "react":
                    text = self._think(lambda: self.brain.react_to_activity(**payload), quiet=True)
                else:
                    text = payload
                if text:
                    self._speak(text)
                    if then_listen:
                        self.gate.open()
            except Exception:
                log.exception("Conversation worker error")
            finally:
                self._jobs.task_done()

    def _think(self, call, quiet: bool = False) -> str | None:
        """Run a Claude call, turning API failures into something to say."""
        try:
            return call()
        except anthropic.AuthenticationError:
            log.error("Claude rejected the credentials. Set ANTHROPIC_API_KEY.")
            spoken = "My Claude API key isn't set up correctly."
        except anthropic.RateLimitError:
            spoken = "I'm being rate limited. Try again in a minute."
        except anthropic.APIStatusError as exc:
            log.error("Claude API error %s: %s", exc.status_code, exc.message)
            spoken = "Something went wrong talking to Claude."
        except anthropic.APIConnectionError:
            spoken = "I can't reach the internet right now."
        except TypeError as exc:
            # The SDK raises this when no credentials can be found at all.
            log.error("Could not call Claude: %s", exc)
            spoken = "My Claude API key isn't set up."
        return None if quiet else spoken

    def _speak(self, text: str) -> None:
        if self.mic:
            self.mic.mute(True)
        try:
            self.speaker.speak(text)
        finally:
            if self.mic:
                self.mic.mute(False)

    # ---------------------------------------------------------- event loop

    def handle(self, event: events.Event) -> None:
        if event.kind == events.UTTERANCE:
            self._on_utterance(event.data["text"])
        elif event.kind == events.GESTURE:
            self.automation.handle("gesture", event.data["name"])
        elif event.kind == events.ACTIVITY:
            self.automation.handle("activity", event.data["activity"])
            if self.cfg.proactive:
                self._jobs.put(("react", dict(event.data), False))
        elif event.kind == events.TIMER_DONE:
            self.say(f"Your {event.data['label']} timer is done.")

    def _on_utterance(self, text: str) -> None:
        # Typed input is always addressed to the assistant.
        command = text.strip() if self.text_mode else self.gate.filter(text)
        if command is None:
            return
        if not command:
            self.say("Yes?", then_listen=True)
            return
        self.gate.close()
        self._jobs.put(("ask", command, True))

    def _read_stdin(self) -> None:
        """Text mode: type instead of talking. `/gesture NAME` and
        `/activity NAME` simulate what the camera would report."""
        print(f"{self.cfg.name} is ready. Type a message, or /gesture NAME, /activity NAME. Ctrl-D quits.")
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            if line.startswith("/gesture "):
                self.bus.publish(events.GESTURE, name=line.split(None, 1)[1])
            elif line.startswith("/activity "):
                name = line.split(None, 1)[1]
                self.bus.publish(
                    events.ACTIVITY, activity=name, description=f"(simulated) {name}", previous=None
                )
            else:
                self.bus.publish(events.UTTERANCE, text=line)
        self.bus.publish(events.SHUTDOWN)

    def run(self) -> None:
        self._worker.start()
        for thread in self.threads:
            thread.start()
        if self.text_mode:
            threading.Thread(target=self._read_stdin, name="stdin", daemon=True).start()
        try:
            while True:
                event = self.bus.get(timeout=1.0)
                if event is None:
                    continue
                if event.kind == events.SHUTDOWN:
                    self._jobs.join()  # finish answering before exiting
                    break
                self.handle(event)
        except KeyboardInterrupt:
            pass
        finally:
            self.shutdown()

    def shutdown(self) -> None:
        log.info("Shutting down.")
        self.speaker.stop()
        self.brain.cancel_timers()
        for thread in self.threads:
            thread.stop()
