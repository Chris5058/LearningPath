"""A tiny thread-safe event bus.

Sensor threads (microphone, camera, timers) publish events; the main loop
consumes them one at a time, so handlers never race each other.
"""

from __future__ import annotations

import queue
import time
from dataclasses import dataclass, field
from typing import Any

# Event kinds
UTTERANCE = "utterance"  # data: {"text": str}
GESTURE = "gesture"  # data: {"name": str}
ACTIVITY = "activity"  # data: {"activity": str, "description": str, "previous": str | None}
TIMER_DONE = "timer_done"  # data: {"label": str}
SHUTDOWN = "shutdown"


@dataclass
class Event:
    kind: str
    data: dict[str, Any] = field(default_factory=dict)
    at: float = field(default_factory=time.time)


class EventBus:
    def __init__(self) -> None:
        self._queue: queue.Queue[Event] = queue.Queue()

    def publish(self, kind: str, **data: Any) -> None:
        self._queue.put(Event(kind, data))

    def get(self, timeout: float | None = None) -> Event | None:
        try:
            return self._queue.get(timeout=timeout)
        except queue.Empty:
            return None
