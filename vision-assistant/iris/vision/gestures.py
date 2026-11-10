"""Hand gesture recognition, fully on-device.

Static poses (open palm, fist, thumbs up, ...) come from MediaPipe's gesture
recognizer. Swipes are detected here from the movement of the hand across
frames. Both are debounced so one gesture fires one event.
"""

from __future__ import annotations

import collections
import logging
import threading
import time
from pathlib import Path

from .. import events

log = logging.getLogger(__name__)

# MediaPipe category name -> our gesture name
STATIC_GESTURES = {
    "Open_Palm": "open_palm",
    "Closed_Fist": "fist",
    "Pointing_Up": "pointing_up",
    "Thumb_Up": "thumbs_up",
    "Thumb_Down": "thumbs_down",
    "Victory": "victory",
    "ILoveYou": "i_love_you",
}


# Pairs of MediaPipe hand landmarks to join when drawing the hand skeleton.
HAND_CONNECTIONS = [
    (0, 1), (1, 2), (2, 3), (3, 4),
    (0, 5), (5, 6), (6, 7), (7, 8),
    (5, 9), (9, 10), (10, 11), (11, 12),
    (9, 13), (13, 14), (14, 15), (15, 16),
    (13, 17), (17, 18), (18, 19), (19, 20), (0, 17),
]


def draw_hand(frame, points: list[tuple[float, float]]) -> None:
    """Draw a hand skeleton on a BGR frame. `points` are normalized (x, y)."""
    import cv2

    h, w = frame.shape[:2]
    pixels = [(int(x * w), int(y * h)) for x, y in points]
    for a, b in HAND_CONNECTIONS:
        if a < len(pixels) and b < len(pixels):
            cv2.line(frame, pixels[a], pixels[b], (80, 220, 120), 2, cv2.LINE_AA)
    for point in pixels:
        cv2.circle(frame, point, 4, (255, 255, 255), -1, cv2.LINE_AA)


class GestureStabilizer:
    """Fires a pose once after it has been held for `hold_frames` frames in a
    row, then stays quiet until the pose changes or the hand goes away."""

    def __init__(self, hold_frames: int = 4) -> None:
        self.hold_frames = hold_frames
        self._current: str | None = None
        self._count = 0
        self._fired = False

    def update(self, label: str | None) -> str | None:
        if label != self._current:
            self._current = label
            self._count = 0
            self._fired = False
        if label is None:
            return None
        self._count += 1
        if self._count >= self.hold_frames and not self._fired:
            self._fired = True
            return label
        return None


class SwipeDetector:
    """Detects a fast horizontal hand movement.

    `x` is the hand position in image coordinates, 0 (left edge) to 1 (right
    edge). The camera faces the user, so a hand moving toward the user's right
    moves toward the *left* of the image; swipes are named from the user's
    point of view.
    """

    def __init__(
        self, window: float = 0.5, min_distance: float = 0.3, cooldown: float = 1.0
    ) -> None:
        self.window = window
        self.min_distance = min_distance
        self.cooldown = cooldown
        self._track: collections.deque[tuple[float, float]] = collections.deque()
        self._quiet_until = 0.0

    def update(self, x: float | None, t: float) -> str | None:
        if x is None:
            self._track.clear()
            return None
        self._track.append((t, x))
        while self._track and t - self._track[0][0] > self.window:
            self._track.popleft()
        if t < self._quiet_until or len(self._track) < 3:
            return None
        xs = [p[1] for p in self._track]
        distance = xs[-1] - xs[0]
        if abs(distance) < self.min_distance:
            return None
        steps = [b - a for a, b in zip(xs, xs[1:])]
        # Require a mostly one-directional movement, not jitter.
        consistent = sum(1 for s in steps if s * distance > 0) >= 0.8 * len(steps)
        if not consistent:
            return None
        self._track.clear()
        self._quiet_until = t + self.cooldown
        return "swipe_right" if distance < 0 else "swipe_left"


class GestureWatcher(threading.Thread):
    """Runs the recognizer on camera frames and publishes GESTURE events."""

    def __init__(self, cfg, camera, bus: events.EventBus) -> None:
        super().__init__(name="gestures", daemon=True)
        self.cfg = cfg
        self.camera = camera
        self.bus = bus
        self._halt = threading.Event()
        self.stabilizer = GestureStabilizer(cfg.gesture_hold_frames)
        self.swipes = SwipeDetector()
        # What the recognizer sees right now, for the live preview:
        # (hand landmark points, pose name or None).
        self.seen: tuple[list[tuple[float, float]], str | None] = ([], None)
        self.error: str | None = None  # why gestures are off, for the UI

    def stop(self) -> None:
        self._halt.set()

    def run(self) -> None:
        try:
            import cv2
            import mediapipe as mp
            from mediapipe.tasks.python import BaseOptions, vision
        except (ImportError, OSError) as exc:
            self.error = f"Gestures are off: MediaPipe or OpenCV could not be loaded ({exc})."
            log.error("Gestures are disabled (%s). Install opencv-python-headless and mediapipe.", exc)
            return

        model_path = Path(self.cfg.gesture_model)
        if not model_path.exists():
            self.error = f"Gestures are off: the model file {model_path} is missing (see the README)."
            log.error(
                "Gesture model not found at %s. Run scripts/download_models.sh; "
                "gestures are disabled.",
                model_path,
            )
            return
        options = vision.GestureRecognizerOptions(
            base_options=BaseOptions(model_asset_path=str(model_path)),
            running_mode=vision.RunningMode.VIDEO,
            num_hands=1,
        )
        recognizer = vision.GestureRecognizer.create_from_options(options)
        period = 1.0 / self.cfg.gesture_fps
        start = time.monotonic()
        last_ms = -1
        try:
            while not self._halt.wait(period):
                frame = self.camera.latest()
                if frame is None:
                    continue
                now = time.monotonic()
                timestamp_ms = max(last_ms + 1, int((now - start) * 1000))
                last_ms = timestamp_ms
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
                result = recognizer.recognize_for_video(image, timestamp_ms)

                label = None
                hand_x = None
                if result.hand_landmarks:
                    hand_x = result.hand_landmarks[0][0].x  # wrist
                    if result.gestures and result.gestures[0]:
                        top = result.gestures[0][0]
                        if top.score >= 0.6:
                            label = STATIC_GESTURES.get(top.category_name)

                points = (
                    [(p.x, p.y) for p in result.hand_landmarks[0]] if result.hand_landmarks else []
                )
                self.seen = (points, label)

                swipe = self.swipes.update(hand_x, now)
                if swipe:
                    # A moving hand passes through poses; don't fire those too.
                    self.stabilizer.update(None)
                    self._emit(swipe)
                    continue
                pose = self.stabilizer.update(label)
                if pose:
                    self._emit(pose)
        finally:
            recognizer.close()

    def _emit(self, name: str) -> None:
        log.info("Gesture: %s", name)
        self.bus.publish(events.GESTURE, name=name)
