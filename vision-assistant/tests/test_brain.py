import numpy as np

from iris import events
from iris.brain import Brain
from iris.config import Config
from iris.devices import SimulatedBackend

from .fakes import FakeClient, response, text, tool_use


class Clock:
    def __init__(self):
        self.now = 0.0

    def __call__(self):
        return self.now


def make(*responses, frame=None, cfg=None, clock=None):
    client = FakeClient(*responses)
    devices = SimulatedBackend()
    bus = events.EventBus()
    brain = Brain(
        cfg or Config(),
        client,
        devices,
        bus,
        get_frame=lambda: frame,
        get_activity=lambda: {"activity": "reading", "description": "x", "confidence": 0.9},
        clock=clock or Clock(),
    )
    return brain, client, devices, bus


def test_plain_answer():
    brain, client, _, _ = make(response(text("Paris.")))
    assert brain.ask("capital of France?") == "Paris."
    request = client.requests[0]
    assert request["model"] == "claude-opus-5-5"
    assert request["fallbacks"] == "default"
    assert request["output_config"] == {"effort": "low"}
    assert request["messages"][0]["content"].endswith("capital of France?")


def test_device_tool_round_trip():
    brain, client, devices, _ = make(
        response(tool_use("control_device", {"device": "desk_lamp", "action": "on", "value": None}), stop_reason="tool_use"),
        response(text("The lamp is on.")),
    )
    assert brain.ask("turn on the lamp") == "The lamp is on."
    assert devices.state["desk_lamp"]["on"] is True
    result = client.requests[1]["messages"][-1]["content"][0]
    assert result["type"] == "tool_result" and result["tool_use_id"] == "tu_1"
    assert result["is_error"] is False


def test_tool_errors_are_reported_back_not_raised():
    brain, client, _, _ = make(
        response(tool_use("control_device", {"device": "toaster", "action": "on", "value": None}), stop_reason="tool_use"),
        response(text("I don't have a toaster.")),
    )
    assert brain.ask("toast") == "I don't have a toaster."
    assert client.requests[1]["messages"][-1]["content"][0]["is_error"] is True


def test_look_at_camera_sends_image():
    frame = np.zeros((48, 64, 3), np.uint8)
    brain, client, _, _ = make(
        response(tool_use("look_at_camera", {}), stop_reason="tool_use"),
        response(text("A dark room.")),
        frame=frame,
    )
    assert brain.ask("what do you see?") == "A dark room."
    content = client.requests[1]["messages"][-1]["content"][0]["content"]
    assert content[0]["type"] == "image"
    assert content[0]["source"]["media_type"] == "image/jpeg"


def test_look_without_camera_is_an_error_result():
    brain, client, _, _ = make(
        response(tool_use("look_at_camera", {}), stop_reason="tool_use"),
        response(text("My camera is off.")),
    )
    brain.ask("what do you see?")
    assert client.requests[1]["messages"][-1]["content"][0]["is_error"] is True


def test_parallel_tool_calls_return_all_results_in_one_message():
    brain, client, devices, _ = make(
        response(
            tool_use("control_device", {"device": "desk_lamp", "action": "on", "value": None}, id="a"),
            tool_use("control_device", {"device": "speaker", "action": "set_volume", "value": 20}, id="b"),
            stop_reason="tool_use",
        ),
        response(text("Done.")),
    )
    brain.ask("lamp on and volume 20")
    results = client.requests[1]["messages"][-1]["content"]
    assert [r["tool_use_id"] for r in results] == ["a", "b"]
    assert devices.state["speaker"]["volume"] == 20


def test_conversation_context_is_kept_then_forgotten_after_idle():
    clock = Clock()
    brain, client, _, _ = make(response(text("one")), response(text("two")), response(text("three")), clock=clock)
    brain.ask("first")
    clock.now = 10
    brain.ask("second")
    assert len(client.requests[1]["messages"]) == 3
    clock.now = 10 + Config().conversation_idle_seconds + 1
    brain.ask("third")
    assert len(client.requests[2]["messages"]) == 1


def test_failed_exchange_is_rolled_back():
    brain, client, _, _ = make(RuntimeError("boom"), response(text("ok")))
    try:
        brain.ask("first")
    except RuntimeError:
        pass
    assert brain.messages == []
    brain.ask("second")
    assert len(client.requests[1]["messages"]) == 1


def test_refusal_is_spoken_and_rolled_back():
    brain, _, _, _ = make(response(stop_reason="refusal"))
    assert "can't help" in brain.ask("something")
    assert brain.messages == []


def test_pause_turn_resumes():
    brain, client, _, _ = make(response(stop_reason="pause_turn"), response(text("Sunny.")))
    assert brain.ask("weather?") == "Sunny."
    assert len(client.requests) == 2


def test_runaway_tool_loop_gives_up():
    loop = [response(tool_use("list_devices", {}), stop_reason="tool_use") for _ in range(20)]
    brain, _, _, _ = make(*loop)
    assert "too long" in brain.ask("loop")
    assert brain.messages == []


def test_timer_publishes_event():
    brain, _, _, bus = make(
        response(tool_use("set_timer", {"seconds": 1, "label": "tea"}), stop_reason="tool_use"),
        response(text("Timer set.")),
    )
    brain.ask("set a tea timer")
    event = bus.get(timeout=3)
    assert event.kind == events.TIMER_DONE and event.data["label"] == "tea"


def test_react_to_activity_stays_silent_on_nothing():
    brain, client, _, _ = make(response(text("NOTHING")))
    assert brain.react_to_activity("eating", "eating at a table", "cooking") is None
    names = [t["name"] for t in client.requests[0]["tools"]]
    assert names == ["control_device"]
    assert brain.messages == []


def test_react_to_activity_can_act():
    brain, _, devices, _ = make(
        response(tool_use("control_device", {"device": "desk_lamp", "action": "on", "value": None}), stop_reason="tool_use"),
        response(text("I turned on the lamp for reading.")),
    )
    assert brain.react_to_activity("reading", "holding a book", "relaxing").startswith("I turned on")
    assert devices.state["desk_lamp"]["on"] is True


def test_fallbacks_can_be_disabled():
    cfg = Config(refusal_fallbacks=False, web_search=False)
    brain, client, _, _ = make(response(text("hi")), cfg=cfg)
    brain.ask("hi")
    assert "fallbacks" not in client.requests[0]
    assert all(t.get("name") != "web_search" for t in client.requests[0]["tools"])
