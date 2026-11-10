import json

import numpy as np

from iris import events
from iris.config import Config
from iris.vision.activity import ActivityMonitor, ActivityRecognizer, scene_changed, scene_signature

from .fakes import FakeClient, response, text


class FakeCamera:
    def __init__(self, frame=None):
        self.frame = frame

    def latest(self):
        return self.frame


def frame(value):
    return np.full((480, 640, 3), value, np.uint8)


def answer(activity, confidence=0.9):
    return response(text(json.dumps({"activity": activity, "description": "d", "confidence": confidence})))


def monitor(*responses, camera=None):
    cfg = Config()
    client = FakeClient(*responses)
    bus = events.EventBus()
    recognizer = ActivityRecognizer(client, cfg.scene_model, cfg.activities)
    return ActivityMonitor(cfg, camera or FakeCamera(frame(10)), recognizer, bus), client, bus


def test_scene_change_detection():
    a, b = scene_signature(frame(10)), scene_signature(frame(12))
    assert not scene_changed(a, b)
    assert scene_changed(a, scene_signature(frame(80)))
    assert scene_changed(None, a)
    moved = frame(10)
    moved[:, :320] = 200
    assert scene_changed(a, scene_signature(moved))


def test_signature_handles_odd_frame_sizes():
    assert scene_signature(np.zeros((481, 643, 3), np.uint8)).shape == (12, 16)


def test_first_frame_is_classified_and_published():
    m, client, bus = monitor(answer("reading"))
    assert m.check(0)["activity"] == "reading"
    event = bus.get(timeout=0)
    assert event.kind == events.ACTIVITY and event.data["activity"] == "reading"
    assert m.current["activity"] == "reading"
    request = client.requests[0]
    assert request["messages"][0]["content"][0]["type"] == "image"
    assert request["output_config"]["format"]["schema"]["properties"]["activity"]["enum"] == Config().activities


def test_static_scene_is_not_resent():
    m, client, _ = monitor(answer("reading"))
    m.check(0)
    assert m.check(20) is None
    assert len(client.requests) == 1


def test_static_scene_is_rechecked_eventually():
    m, client, bus = monitor(answer("reading"), answer("reading"))
    m.check(0)
    bus.get(timeout=0)
    assert m.check(Config().activity_max_interval + 1) is None  # same activity: no event
    assert len(client.requests) == 2
    assert bus.get(timeout=0) is None


def test_changed_scene_with_new_activity_publishes():
    camera = FakeCamera(frame(10))
    m, _, bus = monitor(answer("reading"), answer("away"), camera=camera)
    m.check(0)
    bus.get(timeout=0)
    camera.frame = frame(120)
    assert m.check(20)["activity"] == "away"
    event = bus.get(timeout=0)
    assert event.data == {"activity": "away", "description": "d", "previous": "reading"}


def test_low_confidence_is_ignored():
    m, _, bus = monitor(answer("cooking", confidence=0.3))
    assert m.check(0) is None
    assert m.current is None and bus.get(timeout=0) is None


def test_no_frame_no_request():
    m, client, _ = monitor(camera=FakeCamera(None))
    assert m.check(0) is None and client.requests == []
