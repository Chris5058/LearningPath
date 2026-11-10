"""The web UI's HTTP API, against a real server on a free port."""

import json
import urllib.error
import urllib.request

import numpy as np
import pytest

from iris.app import App
from iris.config import Config
from iris.vision.gestures import draw_hand

from .fakes import FakeClient, response, text


@pytest.fixture
def ui():
    app = App(Config(), text_mode=True, use_camera=False, use_speech=False, client=FakeClient(response(text("Hello there."))))
    app.speaker.speak = lambda _text: None
    app._worker.start()
    app.serve_web("127.0.0.1", 0)
    yield app, f"http://127.0.0.1:{app.web.port}"
    app.web.stop()


def get(url):
    with urllib.request.urlopen(url, timeout=5) as r:
        return r.status, r.headers.get("Content-Type"), r.read()


def post(url, body, headers=None):
    request = urllib.request.Request(
        url, data=json.dumps(body).encode(), headers=headers or {"Content-Type": "application/json"}, method="POST"
    )
    try:
        with urllib.request.urlopen(request, timeout=5) as r:
            return r.status
    except urllib.error.HTTPError as exc:
        return exc.code


def pump(app):
    """Deliver queued events the way the main loop would."""
    while (event := app.bus.get(timeout=0.2)) is not None:
        app.handle(event)
    app._jobs.join()


def state(base, since=0):
    return json.loads(get(f"{base}/api/state?since={since}")[2])


def test_serves_the_page(ui):
    _, base = ui
    status, content_type, body = get(base + "/")
    assert status == 200 and content_type.startswith("text/html")
    assert b"<title>Iris</title>" in body


def test_state_shape(ui):
    _, base = ui
    s = state(base)
    assert s["name"] == "Iris" and s["status"] == "idle"
    assert s["camera"] is False and s["activity"] is None
    assert set(s["devices"]) == {"desk_lamp", "ceiling_light", "speaker"}
    assert s["feed"] == []


def test_chat_needs_no_wake_word_and_shows_in_feed(ui):
    app, base = ui
    app.text_mode = False  # as when running with a microphone
    assert post(base + "/api/chat", {"text": "hello"}) == 200
    pump(app)
    feed = state(base)["feed"]
    assert [(e["kind"], e["text"]) for e in feed] == [("heard", "hello"), ("said", "Hello there.")]
    assert state(base, since=feed[-1]["id"])["feed"] == []


def test_simulated_gesture_changes_device_and_feed(ui):
    app, base = ui
    assert post(base + "/api/gesture", {"name": "thumbs_up"}) == 200
    pump(app)
    s = state(base)
    assert s["devices"]["speaker"]["volume"] == 50
    assert [(e["kind"], e["text"]) for e in s["feed"]] == [("gesture", "thumbs_up"), ("device", "speaker: volume up")]


def test_rejects_requests_other_websites_could_forge(ui):
    app, base = ui
    # A plain HTML form on another site cannot send application/json...
    assert post(base + "/api/chat", {"text": "x"}, {"Content-Type": "text/plain"}) == 400
    # ...and a script on another site carries its own Origin.
    assert post(base + "/api/chat", {"text": "x"}, {"Content-Type": "application/json", "Origin": "https://evil.example"}) == 400
    assert app.bus.get(timeout=0.1) is None


def test_bad_input(ui):
    _, base = ui
    assert post(base + "/api/chat", {"text": "  "}) == 400
    assert post(base + "/api/gesture", {}) == 400
    assert post(base + "/api/nope", {"a": 1}) == 404
    with pytest.raises(urllib.error.HTTPError) as err:
        get(base + "/missing")
    assert err.value.code == 404


def test_stream_without_camera_is_404(ui):
    _, base = ui
    with pytest.raises(urllib.error.HTTPError) as err:
        get(base + "/stream")
    assert err.value.code == 404


def test_draw_hand_marks_the_frame():
    frame = np.zeros((120, 160, 3), np.uint8)
    draw_hand(frame, [(0.1 + 0.04 * i, 0.5) for i in range(21)])
    assert frame.any()
