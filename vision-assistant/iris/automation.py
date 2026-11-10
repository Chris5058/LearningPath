"""Rules that turn gestures and recognized activities into actions.

A rule looks like:

    - when: "gesture:open_palm"      # or "activity:reading"
      do:
        - {builtin: stop}            # stop | listen
        - {device: desk_lamp, action: "on"}
        - {say: "Lamp is on."}
"""

from __future__ import annotations

import logging
import time
from typing import Any, Callable

from .devices import DeviceBackend, DeviceError

log = logging.getLogger(__name__)


class Automation:
    def __init__(
        self,
        rules: list[dict[str, Any]],
        devices: DeviceBackend,
        say: Callable[[str], None],
        builtins: dict[str, Callable[[], None]],
        cooldown: float = 3.0,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self.devices = devices
        self.say = say
        self.builtins = builtins
        self.cooldown = cooldown
        self.clock = clock
        self._last_fired: dict[str, float] = {}
        self._rules: dict[str, list[dict[str, Any]]] = {}
        for rule in rules:
            trigger = rule.get("when")
            actions = rule.get("do")
            if not isinstance(trigger, str) or ":" not in trigger or not isinstance(actions, list):
                raise ValueError(f"Invalid rule: {rule!r}")
            for action in actions:
                self._validate(action)
            self._rules.setdefault(trigger, []).extend(actions)

    def _validate(self, action: dict[str, Any]) -> None:
        if "builtin" in action:
            if action["builtin"] not in self.builtins:
                raise ValueError(f"Unknown builtin '{action['builtin']}'")
        elif "device" in action:
            if "action" not in action:
                raise ValueError(f"Device action needs an 'action': {action!r}")
        elif "say" not in action:
            raise ValueError(f"Invalid action: {action!r}")

    def handle(self, kind: str, name: str) -> int:
        """Run the actions for e.g. ("gesture", "open_palm"). Returns how many ran."""
        trigger = f"{kind}:{name}"
        actions = self._rules.get(trigger)
        if not actions:
            return 0
        now = self.clock()
        last = self._last_fired.get(trigger)
        if last is not None and now - last < self.cooldown:
            return 0
        self._last_fired[trigger] = now
        ran = 0
        for action in actions:
            try:
                if "builtin" in action:
                    self.builtins[action["builtin"]]()
                elif "device" in action:
                    self.devices.execute(action["device"], action["action"], action.get("value"))
                else:
                    self.say(action["say"])
                ran += 1
            except DeviceError as exc:
                log.warning("Rule %s: %s", trigger, exc)
        return ran
