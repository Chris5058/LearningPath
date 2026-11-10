import pytest

from iris.automation import Automation
from iris.devices import DeviceError, SimulatedBackend


class Clock:
    def __init__(self):
        self.now = 100.0

    def __call__(self):
        return self.now


def make(rules, clock=None):
    devices = SimulatedBackend()
    said, calls = [], []
    auto = Automation(
        rules,
        devices,
        say=said.append,
        builtins={"stop": lambda: calls.append("stop"), "listen": lambda: calls.append("listen")},
        cooldown=3,
        clock=clock or Clock(),
    )
    return auto, devices, said, calls


def test_activity_rule_controls_device():
    auto, devices, _, _ = make([{"when": "activity:reading", "do": [{"device": "desk_lamp", "action": "on"}]}])
    assert auto.handle("activity", "reading") == 1
    assert devices.state["desk_lamp"]["on"] is True


def test_gesture_builtin_and_say():
    auto, _, said, calls = make([{"when": "gesture:open_palm", "do": [{"builtin": "stop"}, {"say": "Okay."}]}])
    auto.handle("gesture", "open_palm")
    assert calls == ["stop"] and said == ["Okay."]


def test_unmatched_trigger_does_nothing():
    auto, _, _, _ = make([])
    assert auto.handle("gesture", "victory") == 0


def test_cooldown_prevents_repeat_firing():
    clock = Clock()
    auto, devices, _, _ = make(
        [{"when": "gesture:thumbs_up", "do": [{"device": "speaker", "action": "volume_up"}]}], clock
    )
    auto.handle("gesture", "thumbs_up")
    auto.handle("gesture", "thumbs_up")
    assert devices.state["speaker"]["volume"] == 50
    clock.now += 4
    auto.handle("gesture", "thumbs_up")
    assert devices.state["speaker"]["volume"] == 60


def test_device_failure_does_not_stop_other_actions():
    auto, _, said, _ = make(
        [{"when": "activity:cooking", "do": [{"device": "oven", "action": "on"}, {"say": "Enjoy."}]}]
    )
    assert auto.handle("activity", "cooking") == 1
    assert said == ["Enjoy."]


@pytest.mark.parametrize(
    "rule",
    [
        {"when": "open_palm", "do": []},
        {"when": "gesture:open_palm", "do": [{"builtin": "explode"}]},
        {"when": "gesture:open_palm", "do": [{"device": "desk_lamp"}]},
        {"when": "gesture:open_palm", "do": [{"nonsense": 1}]},
    ],
)
def test_invalid_rules_are_rejected(rule):
    with pytest.raises(ValueError):
        make([rule])


def test_default_rules_are_valid():
    from iris.config import Config

    make(Config().rules)


def test_simulated_backend_rejects_wrong_action():
    with pytest.raises(DeviceError):
        SimulatedBackend().execute("desk_lamp", "next")
    with pytest.raises(DeviceError):
        SimulatedBackend().execute("desk_lamp", "set_brightness")
