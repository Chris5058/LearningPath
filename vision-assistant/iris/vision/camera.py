"""Camera capture: a background thread that always holds the latest frame."""

from __future__ import annotations

import base64
import logging
import threading
import time

import numpy as np

log = logging.getLogger(__name__)


class Camera(threading.Thread):
    def __init__(self, index: int = 0, width: int = 640, height: int = 480) -> None:
        super().__init__(name="camera", daemon=True)
        self.index = index
        self.width = width
        self.height = height
        self._lock = threading.Lock()
        self._frame: np.ndarray | None = None
        self._halt = threading.Event()

    def run(self) -> None:
        import cv2

        capture = cv2.VideoCapture(self.index)
        capture.set(cv2.CAP_PROP_FRAME_WIDTH, self.width)
        capture.set(cv2.CAP_PROP_FRAME_HEIGHT, self.height)
        if not capture.isOpened():
            log.error("Could not open camera %s; vision is disabled.", self.index)
            return
        log.info("Camera %s open.", self.index)
        try:
            while not self._halt.is_set():
                ok, frame = capture.read()
                if not ok:
                    time.sleep(0.2)
                    continue
                with self._lock:
                    self._frame = frame
        finally:
            capture.release()

    def stop(self) -> None:
        self._halt.set()

    def latest(self) -> np.ndarray | None:
        """The most recent BGR frame, or None if the camera has not produced one."""
        with self._lock:
            return None if self._frame is None else self._frame.copy()


def frame_to_jpeg_b64(frame: np.ndarray, quality: int = 80) -> str:
    """Encode a BGR frame as base64 JPEG, ready for the Claude API."""
    import cv2

    ok, buffer = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, quality])
    if not ok:
        raise RuntimeError("JPEG encoding failed")
    return base64.standard_b64encode(buffer.tobytes()).decode("ascii")


def image_block(frame: np.ndarray) -> dict:
    return {
        "type": "image",
        "source": {"type": "base64", "media_type": "image/jpeg", "data": frame_to_jpeg_b64(frame)},
    }
