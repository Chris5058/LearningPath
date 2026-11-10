"""Camera capture: a background thread that always holds the latest frame."""

from __future__ import annotations

import base64
import logging
import sys
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
        self.error: str | None = None  # why there is no picture, for the UI
        self._halt = threading.Event()

    def run(self) -> None:
        try:
            import cv2
        except ImportError as exc:
            self.error = "OpenCV is not installed. Run: python -m pip install opencv-python-headless"
            log.error("Camera is disabled (%s). Install opencv-python-headless.", exc)
            return

        capture = self._open(cv2)
        if capture is None:
            self.error = (
                f"Could not open camera {self.index}. Check that a webcam is connected, that no "
                "other app (Teams, Zoom, the Camera app) is using it, and that camera access is "
                "allowed for desktop apps in your privacy settings."
            )
            log.error("Could not open camera %s; vision is disabled.", self.index)
            return
        log.info("Camera %s open.", self.index)
        failures = 0
        try:
            while not self._halt.is_set():
                ok, frame = capture.read()
                if not ok:
                    failures += 1
                    if failures == 25:
                        self.error = f"Camera {self.index} opened but is not sending pictures."
                    time.sleep(0.2)
                    continue
                failures = 0
                self.error = None
                with self._lock:
                    self._frame = frame
        finally:
            capture.release()

    def _open(self, cv2):
        """Open the camera, trying the alternative Windows driver if the
        default one fails."""
        backends = [cv2.CAP_ANY]
        if sys.platform == "win32":
            backends += [cv2.CAP_DSHOW, cv2.CAP_MSMF]
        for backend in backends:
            capture = cv2.VideoCapture(self.index, backend)
            if capture.isOpened():
                capture.set(cv2.CAP_PROP_FRAME_WIDTH, self.width)
                capture.set(cv2.CAP_PROP_FRAME_HEIGHT, self.height)
                return capture
            capture.release()
        return None

    def stop(self) -> None:
        self._halt.set()

    @property
    def has_frame(self) -> bool:
        return self._frame is not None

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
