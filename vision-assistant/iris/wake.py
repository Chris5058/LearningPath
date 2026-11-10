"""Decides whether a transcribed utterance is addressed to the assistant."""

from __future__ import annotations

import re
import time
from typing import Callable


class WakeGate:
    """Utterances count when they contain the wake word, or arrive inside an
    open listening window (after a reply, a bare wake word, or a gesture)."""

    def __init__(
        self,
        wake_words: list[str],
        followup_seconds: float = 8.0,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        # Longest first so "hey iris" wins over "iris".
        words = sorted((w.lower().strip() for w in wake_words if w.strip()), key=len, reverse=True)
        if not words:
            raise ValueError("At least one wake word is required")
        pattern = "|".join(r"\s+".join(map(re.escape, w.split())) for w in words)
        self._pattern = re.compile(rf"\b(?:{pattern})\b[\s,.!?:;-]*", re.IGNORECASE)
        self.followup_seconds = followup_seconds
        self.clock = clock
        self._open_until = 0.0

    def open(self, seconds: float | None = None) -> None:
        self._open_until = self.clock() + (self.followup_seconds if seconds is None else seconds)

    def close(self) -> None:
        self._open_until = 0.0

    @property
    def is_open(self) -> bool:
        return self.clock() < self._open_until

    def filter(self, text: str) -> str | None:
        """Return the command to act on, "" for a bare wake word, or None to ignore."""
        text = text.strip()
        match = self._pattern.search(text)
        if match:
            # Keep what follows the wake word; fall back to what preceded it
            # ("what time is it, Iris?").
            command = text[match.end():].strip() or text[: match.start()].strip(" ,")
            return command
        if self.is_open and text:
            return text
        return None
