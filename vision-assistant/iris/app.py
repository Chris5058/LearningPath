"""Wires the sensors, the brain and the outputs together."""

from __future__ import annotations

import collections
import logging
import queue
import sys
import threading
import time

import anthropic

from . import events
from .audio.speaker import Speaker
from .automation import Automation
from .brain import Brain, request_extras
from .config import Config
from .devices import make_backend
from .wake import WakeGate

log = logging.getLogger(__name__)


class _RecordedDevices:
    """Wraps a device backend so every action shows up in the activity feed."""

    def __init__(self, backend, record) -> None:
        self._backend = backend
        self._record = record

    def list_devices(self):
        return self._backend.list_devices()

    def execute(self, device, action, value=None):
        result = self._backend.execute(device, action, value)
        detail = f"{device}: {action.replace('_', ' ')}" + (f" {value}" if value is not None else "")
        self._record("device", detail)
        return result

    def __getattr__(self, name):
        return getattr(self._backend, name)


class App:
    def __init__(
        self,
        cfg: Config,
        text_mode: bool = False,
        use_camera: bool = True,
        use_speech: bool = True,
        use_mic: bool = True,
        client: anthropic.Anthropic | None = None,
    ) -> None:
        self.cfg = cfg
        self.text_mode = text_mode
        self.bus = events.EventBus()
        # A running log of what happened, shown by the web UI.
        self.feed: collections.deque[dict] = collections.deque(maxlen=200)
        self._feed_lock = threading.Lock()
        self._feed_id = 0
        self.status = "idle"  # idle | thinking | speaking
        self.devices = _RecordedDevices(make_backend(cfg), self.record)
        self.client = client or anthropic.Anthropic()
        self.speaker = Speaker(cfg.piper_model, enabled=use_speech)
        self.gate = WakeGate(cfg.wake_words, cfg.followup_seconds)
        self.threads: list[threading.Thread] = []
        self.web = None

        self.camera = None
        self.activity = None
        self.gestures = None
        if use_camera:
            from .vision.activity import ActivityMonitor, ActivityRecognizer
            from .vision.camera import Camera
            from .vision.gestures import GestureWatcher

            self.camera = Camera(cfg.camera_index, cfg.frame_width, cfg.frame_height)
            recognizer = ActivityRecognizer(
                self.client, cfg.scene_model, cfg.activities, request_extras(cfg)
            )
            self.activity = ActivityMonitor(cfg, self.camera, recognizer, self.bus)
            self.gestures = GestureWatcher(cfg, self.camera, self.bus)
            self.threads += [self.camera, self.gestures, self.activity]

        self.mic = None
        if use_mic and not text_mode:
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

    # ---------------------------------------------------------------- feed

    def record(self, kind: str, text: str) -> None:
        """Add an entry to the activity feed. Kinds: heard, said, gesture,
        activity, device, system."""
        with self._feed_lock:
            self._feed_id += 1
            self.feed.append({"id": self._feed_id, "at": time.time(), "kind": kind, "text": text})

    def feed_since(self, last_id: int) -> list[dict]:
        with self._feed_lock:
            return [entry for entry in self.feed if entry["id"] > last_id]

    def preview(self):
        """The camera frame as the user would see it in a mirror, with the
        tracked hand drawn on it. None when there is no camera frame."""
        frame = self.camera.latest() if self.camera else None
        if frame is None:
            return None
        from .vision.gestures import draw_hand

        frame = frame[:, ::-1].copy()
        points, _pose = self.gestures.seen if self.gestures else ([], None)
        if points:
            draw_hand(frame, [(1 - x, y) for x, y in points])
        return frame

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
                    self.status = "thinking"
                    text = self._think(lambda: self.brain.ask(payload))
                elif kind == "react":
                    text = self._think(lambda: self.brain.react_to_activity(**payload), quiet=True)
                else:
                    text = payload
                if text:
                    self.record("said", text)
                    self.status = "speaking"
                    self._speak(text)
                    if then_listen:
                        self.gate.open()
            except Exception:
                log.exception("Conversation worker error")
            finally:
                self.status = "idle"
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
            self._on_utterance(event.data["text"], typed=event.data.get("typed", False))
        elif event.kind == events.GESTURE:
            self.record("gesture", event.data["name"])
            self.automation.handle("gesture", event.data["name"])
        elif event.kind == events.ACTIVITY:
            self.record("activity", f"{event.data['activity']}: {event.data['description']}")
            self.automation.handle("activity", event.data["activity"])
            if self.cfg.proactive:
                self._jobs.put(("react", dict(event.data), False))
        elif event.kind == events.TIMER_DONE:
            self.say(f"Your {event.data['label']} timer is done.")

    def _on_utterance(self, text: str, typed: bool = False) -> None:
        # Typed input is always addressed to the assistant.
        command = text.strip() if typed or self.text_mode else self.gate.filter(text)
        if command is None:
            return
        if not command:
            self.say("Yes?", then_listen=True)
            return
        self.record("heard", command)
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

    def serve_web(self, host: str, port: int, open_browser: bool = False) -> None:
        from .web.server import WebUI

        self.web = WebUI(self, host, port)
        self.web.start(open_browser)

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
        if self.web:
            self.web.stop()
        self.speaker.stop()
        self.brain.cancel_timers()
        for thread in self.threads:
            thread.stop()
