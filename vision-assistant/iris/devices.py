"""Things the assistant can control.

`SimulatedBackend` keeps state in memory and logs every action, so the whole
system can be developed without any smart-home hardware. `HomeAssistantBackend`
drives real devices through Home Assistant's REST API.
"""

from __future__ import annotations

import json
import logging
import os
import urllib.request
from typing import Any, Protocol

log = logging.getLogger(__name__)

ACTIONS = [
    "on",
    "off",
    "toggle",
    "set_brightness",
    "play",
    "pause",
    "next",
    "previous",
    "volume_up",
    "volume_down",
    "set_volume",
]


class DeviceError(Exception):
    pass


class DeviceBackend(Protocol):
    def list_devices(self) -> dict[str, dict[str, Any]]: ...

    def execute(self, device: str, action: str, value: int | None = None) -> str: ...


class SimulatedBackend:
    def __init__(self) -> None:
        self.state: dict[str, dict[str, Any]] = {
            "desk_lamp": {"kind": "light", "on": False, "brightness": 100},
            "ceiling_light": {"kind": "light", "on": True, "brightness": 100},
            "speaker": {"kind": "media_player", "playing": False, "volume": 40, "track": 1},
        }

    def list_devices(self) -> dict[str, dict[str, Any]]:
        return {name: dict(state) for name, state in self.state.items()}

    def execute(self, device: str, action: str, value: int | None = None) -> str:
        if device not in self.state:
            raise DeviceError(f"Unknown device '{device}'. Known: {', '.join(self.state)}")
        s = self.state[device]
        kind = s["kind"]
        if kind == "light" and action in ("on", "off", "toggle", "set_brightness"):
            if action == "on":
                s["on"] = True
            elif action == "off":
                s["on"] = False
            elif action == "toggle":
                s["on"] = not s["on"]
            else:
                s["brightness"] = _percent(value)
                s["on"] = s["brightness"] > 0
        elif kind == "media_player" and action in (
            "play", "pause", "next", "previous", "volume_up", "volume_down", "set_volume",
        ):
            if action == "play":
                s["playing"] = True
            elif action == "pause":
                s["playing"] = False
            elif action == "next":
                s["track"] += 1
            elif action == "previous":
                s["track"] = max(1, s["track"] - 1)
            elif action == "volume_up":
                s["volume"] = min(100, s["volume"] + 10)
            elif action == "volume_down":
                s["volume"] = max(0, s["volume"] - 10)
            else:
                s["volume"] = _percent(value)
        else:
            raise DeviceError(f"'{device}' is a {kind} and does not support '{action}'")
        public = {k: v for k, v in s.items() if k != "kind"}
        log.info("[device] %s %s -> %s", device, action, public)
        return f"{device}: {json.dumps(public)}"


def _percent(value: int | None) -> int:
    if value is None:
        raise DeviceError("This action needs a value between 0 and 100")
    return max(0, min(100, int(value)))


class HomeAssistantBackend:
    """Maps friendly device names to Home Assistant entities.

    The long-lived access token is read from the HOME_ASSISTANT_TOKEN
    environment variable.
    """

    _SERVICES = {
        "on": "turn_on",
        "off": "turn_off",
        "toggle": "toggle",
        "set_brightness": "turn_on",
        "play": "media_play",
        "pause": "media_pause",
        "next": "media_next_track",
        "previous": "media_previous_track",
        "volume_up": "volume_up",
        "volume_down": "volume_down",
        "set_volume": "volume_set",
    }

    def __init__(self, base_url: str, devices: dict[str, str], token: str | None = None) -> None:
        self.base_url = base_url.rstrip("/")
        self.devices = devices
        self.token = token or os.environ.get("HOME_ASSISTANT_TOKEN", "")
        if not self.token:
            raise DeviceError("Set HOME_ASSISTANT_TOKEN to use the home_assistant backend")

    def list_devices(self) -> dict[str, dict[str, Any]]:
        return {name: {"entity_id": entity} for name, entity in self.devices.items()}

    def execute(self, device: str, action: str, value: int | None = None) -> str:
        if device not in self.devices:
            raise DeviceError(f"Unknown device '{device}'. Known: {', '.join(self.devices)}")
        if action not in self._SERVICES:
            raise DeviceError(f"Unknown action '{action}'")
        entity = self.devices[device]
        domain = entity.split(".", 1)[0]
        payload: dict[str, Any] = {"entity_id": entity}
        if action == "set_brightness":
            payload["brightness_pct"] = _percent(value)
        elif action == "set_volume":
            payload["volume_level"] = _percent(value) / 100
        request = urllib.request.Request(
            f"{self.base_url}/api/services/{domain}/{self._SERVICES[action]}",
            data=json.dumps(payload).encode(),
            headers={"Authorization": f"Bearer {self.token}", "Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=10):
                pass
        except OSError as exc:
            raise DeviceError(f"Home Assistant call failed: {exc}") from exc
        return f"{device}: {action} done"


def make_backend(cfg) -> DeviceBackend:
    if cfg.device_backend == "simulated":
        return SimulatedBackend()
    if cfg.device_backend == "home_assistant":
        return HomeAssistantBackend(cfg.home_assistant_url, cfg.devices)
    raise ValueError(f"Unknown device_backend '{cfg.device_backend}'")
