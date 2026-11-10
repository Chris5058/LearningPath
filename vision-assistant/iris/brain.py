"""The assistant's brain: a Claude conversation with tools.

Claude can look through the camera, check what the user is doing, control
devices, set timers and search the web. Replies are written to be spoken.
"""

from __future__ import annotations

import datetime
import json
import logging
import threading
import time
from typing import Any, Callable

import anthropic

from . import events
from .devices import ACTIONS, DeviceBackend, DeviceError
from .vision.camera import image_block

log = logging.getLogger(__name__)

MAX_TOOL_ROUNDS = 8

SYSTEM_PROMPT = """You are {name}, a voice assistant that lives on a small \
device in the user's home. The device has a microphone, a speaker and a camera \
facing the room.

Everything you write is read aloud by a text-to-speech engine, so write the way \
a person talks: plain sentences, no markdown, lists, emoji or URLs. Keep answers \
to one to three sentences unless the user asks for more detail. What you receive \
is a speech transcript, so it may contain small recognition mistakes; go with \
the most sensible reading rather than asking the user to repeat themselves.

You can see. When a question is about the user, the room, or something they are \
holding or pointing at ("what is this?", "how do I look?", "what am I doing?"), \
call look_at_camera and answer from the image. The camera is only read when you \
call that tool.

Use control_device for lights and music, and set_timer for timers and \
reminders. After doing something, confirm it in a few words. Use web search for \
news, weather and anything that may have changed recently."""

REACT_PROMPT = """The camera noticed a change in what the user is doing.

Previous activity: {previous}
Current activity: {activity}
What the camera sees: {description}
Devices: {devices}

If adjusting a device would clearly help with what they are doing now, do it \
with control_device. Most changes need nothing, and doing nothing is the right \
call whenever you are unsure. Then reply with one short sentence to say aloud \
about what you did, or reply with exactly NOTHING if you did nothing or it is \
not worth interrupting them."""


def request_extras(cfg) -> dict[str, Any]:
    """Request options shared by every Claude call."""
    if not cfg.refusal_fallbacks:
        return {}
    # If a safety classifier declines a request, the API retries it on a
    # fallback model within the same call.
    return {"betas": ["server-side-fallback-2026-07-01"], "fallbacks": "default"}


class BrainError(Exception):
    """The exchange could not be completed; `spoken` is safe to say aloud."""

    def __init__(self, spoken: str) -> None:
        super().__init__(spoken)
        self.spoken = spoken


class Brain:
    def __init__(
        self,
        cfg,
        client: anthropic.Anthropic,
        devices: DeviceBackend,
        bus: events.EventBus,
        get_frame: Callable[[], Any] = lambda: None,
        get_activity: Callable[[], dict | None] = lambda: None,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self.cfg = cfg
        self.client = client
        self.devices = devices
        self.bus = bus
        self.get_frame = get_frame
        self.get_activity = get_activity
        self.clock = clock
        self.messages: list[dict[str, Any]] = []
        self._last_used = clock()
        self._timers: list[threading.Timer] = []
        self.system = SYSTEM_PROMPT.format(name=cfg.name)

    # ------------------------------------------------------------------ tools

    def _tool_definitions(self, include_all: bool = True) -> list[dict[str, Any]]:
        device_names = sorted(self.devices.list_devices())
        control = {
            "name": "control_device",
            "description": "Control a light or the speaker. set_brightness and "
            "set_volume need a value from 0 to 100; pass null otherwise.",
            "strict": True,
            "input_schema": {
                "type": "object",
                "properties": {
                    "device": {"type": "string", "enum": device_names},
                    "action": {"type": "string", "enum": ACTIONS},
                    "value": {"type": ["integer", "null"]},
                },
                "required": ["device", "action", "value"],
                "additionalProperties": False,
            },
        }
        if not include_all:
            return [control]
        tools: list[dict[str, Any]] = [
            {
                "name": "look_at_camera",
                "description": "Take a picture with the device's camera and look at it. "
                "Use it whenever the answer depends on what is in front of the device.",
                "input_schema": {"type": "object", "properties": {}, "additionalProperties": False},
            },
            {
                "name": "get_current_activity",
                "description": "What the camera last recognized the user doing "
                "(for example cooking, reading, away), without taking a new picture.",
                "input_schema": {"type": "object", "properties": {}, "additionalProperties": False},
            },
            control,
            {
                "name": "list_devices",
                "description": "List controllable devices and their current state.",
                "input_schema": {"type": "object", "properties": {}, "additionalProperties": False},
            },
            {
                "name": "set_timer",
                "description": "Start a timer. When it ends the device announces the label.",
                "strict": True,
                "input_schema": {
                    "type": "object",
                    "properties": {
                        "seconds": {"type": "integer"},
                        "label": {"type": "string", "description": "e.g. 'pasta' or 'stretch break'"},
                    },
                    "required": ["seconds", "label"],
                    "additionalProperties": False,
                },
            },
        ]
        if self.cfg.web_search:
            tools.append({"type": "web_search_20260209", "name": "web_search", "max_uses": 3})
        return tools

    def _run_tool(self, name: str, args: dict[str, Any]) -> tuple[Any, bool]:
        """Returns (tool_result content, is_error)."""
        try:
            if name == "look_at_camera":
                frame = self.get_frame()
                if frame is None:
                    return "The camera is not available right now.", True
                return [image_block(frame)], False
            if name == "get_current_activity":
                activity = self.get_activity()
                return (json.dumps(activity) if activity else "Nothing recognized yet."), False
            if name == "control_device":
                return self.devices.execute(args["device"], args["action"], args.get("value")), False
            if name == "list_devices":
                return json.dumps(self.devices.list_devices()), False
            if name == "set_timer":
                return self._set_timer(int(args["seconds"]), str(args["label"])), False
            return f"Unknown tool '{name}'", True
        except DeviceError as exc:
            return str(exc), True
        except (KeyError, TypeError, ValueError) as exc:
            return f"Invalid input for {name}: {exc}", True

    def _set_timer(self, seconds: int, label: str) -> str:
        if seconds <= 0:
            raise ValueError("seconds must be positive")
        timer = threading.Timer(seconds, lambda: self.bus.publish(events.TIMER_DONE, label=label))
        timer.daemon = True
        timer.start()
        self._timers.append(timer)
        return f"Timer '{label}' set for {seconds} seconds."

    def cancel_timers(self) -> None:
        for timer in self._timers:
            timer.cancel()
        self._timers.clear()

    # ----------------------------------------------------------- conversation

    def ask(self, text: str) -> str:
        """Answer something the user said. Keeps conversation context until the
        user has been quiet for `conversation_idle_seconds`."""
        now = self.clock()
        if now - self._last_used > self.cfg.conversation_idle_seconds:
            self.messages = []
        self._last_used = now

        checkpoint = len(self.messages)
        stamp = datetime.datetime.now().strftime("%A %d %B %Y, %H:%M")
        self.messages.append({"role": "user", "content": f"[{stamp}] {text}"})
        try:
            reply = self._converse(self.messages, self.system, self._tool_definitions())
        except BrainError as exc:
            del self.messages[checkpoint:]
            return exc.spoken
        except Exception:
            # Don't leave a half-finished exchange in the history.
            del self.messages[checkpoint:]
            raise
        self._last_used = self.clock()
        return reply

    def react_to_activity(self, activity: str, description: str, previous: str | None) -> str | None:
        """Let Claude decide whether a change of activity calls for action.
        Runs outside the spoken conversation. Returns a sentence to say, or None."""
        prompt = REACT_PROMPT.format(
            previous=previous or "unknown",
            activity=activity,
            description=description,
            devices=json.dumps(self.devices.list_devices()),
        )
        try:
            reply = self._converse(
                [{"role": "user", "content": prompt}],
                self.system,
                self._tool_definitions(include_all=False),
            )
        except BrainError:
            return None
        reply = reply.strip()
        return None if not reply or reply.strip(".").upper() == "NOTHING" else reply

    def _converse(self, messages: list[dict[str, Any]], system: str, tools: list[dict]) -> str:
        """Run the tool loop until Claude gives a final answer. Appends to `messages`."""
        for _ in range(MAX_TOOL_ROUNDS):
            response = self.client.beta.messages.create(
                model=self.cfg.model,
                max_tokens=16000,
                system=system,
                tools=tools,
                messages=messages,
                output_config={"effort": self.cfg.effort},
                **request_extras(self.cfg),
            )
            # Keep the full content: thinking and tool_use blocks must be
            # passed back unchanged on the next request.
            messages.append({"role": "assistant", "content": response.content})

            if response.stop_reason == "pause_turn":
                continue  # a server-side tool is still working; resume the turn
            if response.stop_reason == "refusal":
                raise BrainError("Sorry, I can't help with that.")
            if response.stop_reason == "tool_use":
                results = []
                for block in response.content:
                    if block.type != "tool_use":
                        continue
                    log.info("Tool: %s %s", block.name, block.input)
                    content, is_error = self._run_tool(block.name, block.input)
                    results.append(
                        {
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": content,
                            "is_error": is_error,
                        }
                    )
                messages.append({"role": "user", "content": results})
                continue

            text = " ".join(b.text.strip() for b in response.content if b.type == "text").strip()
            if response.stop_reason == "max_tokens":
                log.warning("Reply was cut off at max_tokens")
            return text
        log.warning("Gave up after %d tool rounds", MAX_TOOL_ROUNDS)
        raise BrainError("Sorry, that took me too long. Could you ask again?")
