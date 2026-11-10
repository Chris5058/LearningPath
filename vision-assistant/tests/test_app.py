"""End-to-end through the app's event handling, with a scripted Claude."""

from iris import events
from iris.app import App
from iris.config import Config

from .fakes import FakeClient, response, text, tool_use


def make(*responses, text_mode=False, **overrides):
    cfg = Config(**overrides)
    app = App(cfg, text_mode=True, use_camera=False, use_speech=False, client=FakeClient(*responses))
    app.text_mode = text_mode  # build without a microphone, then behave as configured
    spoken = []
    app.speaker.speak = spoken.append
    app._worker.start()
    return app, spoken


def send(app, kind, **data):
    app.handle(events.Event(kind, data))
    app._jobs.join()


def test_wake_word_question_is_answered_and_followup_window_opens():
    app, spoken = make(response(text("It is sunny.")), response(text("Rain tomorrow.")))
    send(app, events.UTTERANCE, text="some background chatter")
    assert spoken == []
    send(app, events.UTTERANCE, text="Hey Iris, how's the weather?")
    assert spoken == ["It is sunny."]
    send(app, events.UTTERANCE, text="and tomorrow?")  # no wake word needed
    assert spoken == ["It is sunny.", "Rain tomorrow."]


def test_bare_wake_word_prompts_then_listens():
    app, spoken = make(response(text("Hello!")))
    send(app, events.UTTERANCE, text="Iris")
    assert spoken == ["Yes?"]
    send(app, events.UTTERANCE, text="say hello")
    assert spoken == ["Yes?", "Hello!"]


def test_voice_command_controls_device():
    app, spoken = make(
        response(tool_use("control_device", {"device": "desk_lamp", "action": "on", "value": None}), stop_reason="tool_use"),
        response(text("Lamp on.")),
    )
    send(app, events.UTTERANCE, text="iris, turn on the desk lamp")
    assert app.devices.state["desk_lamp"]["on"] is True and spoken == ["Lamp on."]


def test_gesture_rules():
    app, _ = make()
    send(app, events.GESTURE, name="thumbs_up")
    assert app.devices.state["speaker"]["volume"] == 50
    send(app, events.GESTURE, name="swipe_right")
    assert app.devices.state["speaker"]["track"] == 2


def test_pointing_up_opens_listening_without_wake_word():
    app, spoken = make(response(text("Four.")))
    send(app, events.GESTURE, name="pointing_up")
    send(app, events.UTTERANCE, text="what is two plus two")
    assert spoken == ["Four."]


def test_open_palm_stops_and_closes_window():
    app, spoken = make()
    app.gate.open()
    send(app, events.GESTURE, name="open_palm")
    send(app, events.UTTERANCE, text="this should be ignored")
    assert spoken == []


def test_activity_rule_and_proactive_reaction():
    app, spoken = make(response(text("NOTHING")), proactive=True)
    send(app, events.ACTIVITY, activity="reading", description="holding a book", previous="relaxing")
    assert app.devices.state["desk_lamp"]["on"] is True  # from the rule
    assert spoken == []  # Claude chose to stay quiet
    assert len(app.client.requests) == 1


def test_activity_without_proactive_makes_no_claude_call():
    app, _ = make()
    send(app, events.ACTIVITY, activity="sleeping", description="in bed", previous="reading")
    assert app.devices.state["ceiling_light"]["on"] is False
    assert app.client.requests == []


def test_timer_done_is_announced():
    app, spoken = make()
    send(app, events.TIMER_DONE, label="pasta")
    assert spoken == ["Your pasta timer is done."]


def test_api_failure_is_spoken_not_fatal():
    import anthropic
    import httpx2

    error = anthropic.APIConnectionError(request=httpx2.Request("POST", "https://api.anthropic.com"))
    app, spoken = make(error, response(text("Back.")))
    send(app, events.UTTERANCE, text="iris hello")
    assert "internet" in spoken[0]
    send(app, events.UTTERANCE, text="iris hello again")
    assert spoken[1] == "Back."


def test_text_mode_needs_no_wake_word():
    app, spoken = make(response(text("Hi.")), text_mode=True)
    send(app, events.UTTERANCE, text="hello")
    assert spoken == ["Hi."]
