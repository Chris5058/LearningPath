"""Activity recognition: what is the person in front of the camera doing?

Every so often a frame is sent to Claude, which names the activity. To keep
cost and chatter down, a frame is only sent when the scene has visibly changed
since the last check (or a long time has passed), and an ACTIVITY event is only
published when the recognized activity actually changes.
"""

from __future__ import annotations

import json
import logging
import threading
import time

import anthropic
import numpy as np

from .. import events
from .camera import image_block

log = logging.getLogger(__name__)

PROMPT = """This is a frame from a fixed camera in someone's home. Decide what \
the person in view is doing right now.

Pick the single best label from: {labels}.
Use "away" when nobody is visible and "other" when nothing fits.
The previous label was "{previous}"; keep it unless the frame clearly shows \
something different. Give a one-sentence description of what you see, and a \
confidence between 0 and 1."""


def scene_signature(frame: np.ndarray) -> np.ndarray:
    """A tiny grayscale thumbnail used to measure how much the scene changed."""
    gray = frame.mean(axis=2) if frame.ndim == 3 else frame
    h, w = gray.shape
    gh, gw = 12, 16
    cropped = gray[: h - h % gh, : w - w % gw]
    return cropped.reshape(gh, cropped.shape[0] // gh, gw, cropped.shape[1] // gw).mean(axis=(1, 3))


def scene_changed(a: np.ndarray | None, b: np.ndarray, threshold: float = 6.0) -> bool:
    """True when thumbnails differ by more than `threshold` gray levels on average."""
    return a is None or float(np.abs(a - b).mean()) > threshold


class ActivityRecognizer:
    """Asks Claude to classify a single frame."""

    def __init__(
        self,
        client: anthropic.Anthropic,
        model: str,
        labels: list[str],
        request_extras: dict | None = None,
    ) -> None:
        self.client = client
        self.model = model
        self.labels = labels
        self.request_extras = request_extras or {}
        self.schema = {
            "type": "object",
            "properties": {
                "activity": {"type": "string", "enum": labels},
                "description": {"type": "string"},
                "confidence": {"type": "number"},
            },
            "required": ["activity", "description", "confidence"],
            "additionalProperties": False,
        }

    def recognize(self, frame: np.ndarray, previous: str | None) -> dict | None:
        response = self.client.beta.messages.create(
            model=self.model,
            max_tokens=1024,
            **self.request_extras,
            output_config={
                "effort": "low",
                "format": {"type": "json_schema", "schema": self.schema},
            },
            messages=[
                {
                    "role": "user",
                    "content": [
                        image_block(frame),
                        {
                            "type": "text",
                            "text": PROMPT.format(
                                labels=", ".join(self.labels), previous=previous or "unknown"
                            ),
                        },
                    ],
                }
            ],
        )
        if response.stop_reason != "end_turn":
            log.warning("Activity check ended with stop_reason=%s", response.stop_reason)
            return None
        text = next((b.text for b in response.content if b.type == "text"), None)
        return json.loads(text) if text else None


class ActivityMonitor(threading.Thread):
    def __init__(self, cfg, camera, recognizer: ActivityRecognizer, bus: events.EventBus) -> None:
        super().__init__(name="activity", daemon=True)
        self.cfg = cfg
        self.camera = camera
        self.recognizer = recognizer
        self.bus = bus
        self._halt = threading.Event()
        self._lock = threading.Lock()
        self._current: dict | None = None
        self._signature: np.ndarray | None = None
        self._last_check = float("-inf")

    @property
    def current(self) -> dict | None:
        """The latest recognized activity: {"activity", "description", "confidence"}."""
        with self._lock:
            return dict(self._current) if self._current else None

    def stop(self) -> None:
        self._halt.set()

    def run(self) -> None:
        while not self._halt.wait(self.cfg.activity_interval):
            try:
                self.check(time.monotonic())
            except anthropic.RateLimitError:
                log.warning("Rate limited on activity check; backing off.")
                self._halt.wait(60)
            except anthropic.APIStatusError as exc:
                log.warning("Activity check failed (%s): %s", exc.status_code, exc.message)
            except anthropic.APIConnectionError:
                log.warning("Activity check failed: network unreachable.")

    def check(self, now: float) -> dict | None:
        """Classify the current frame if it is worth it. Returns the new
        activity when it changed, else None."""
        frame = self.camera.latest()
        if frame is None:
            return None
        signature = scene_signature(frame)
        stale = now - self._last_check >= self.cfg.activity_max_interval
        if not stale and not scene_changed(self._signature, signature):
            return None
        self._signature = signature
        self._last_check = now

        previous = self._current["activity"] if self._current else None
        result = self.recognizer.recognize(frame, previous)
        if not result or result["confidence"] < self.cfg.activity_min_confidence:
            return None
        with self._lock:
            self._current = result
        if result["activity"] == previous:
            return None
        log.info("Activity: %s (%s)", result["activity"], result["description"])
        self.bus.publish(
            events.ACTIVITY,
            activity=result["activity"],
            description=result["description"],
            previous=previous,
        )
        return result
