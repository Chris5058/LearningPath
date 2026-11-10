"""Configuration: defaults, overridden by a YAML file."""

from __future__ import annotations

from dataclasses import dataclass, field, fields
from pathlib import Path
from typing import Any

import yaml

DEFAULT_RULES: list[dict[str, Any]] = [
    # Gestures
    {"when": "gesture:open_palm", "do": [{"builtin": "stop"}]},
    {"when": "gesture:pointing_up", "do": [{"builtin": "listen"}]},
    {"when": "gesture:thumbs_up", "do": [{"device": "speaker", "action": "volume_up"}]},
    {"when": "gesture:thumbs_down", "do": [{"device": "speaker", "action": "volume_down"}]},
    {"when": "gesture:swipe_right", "do": [{"device": "speaker", "action": "next"}]},
    {"when": "gesture:swipe_left", "do": [{"device": "speaker", "action": "previous"}]},
    {"when": "gesture:fist", "do": [{"device": "speaker", "action": "pause"}]},
    # Activities
    {"when": "activity:reading", "do": [{"device": "desk_lamp", "action": "on"}]},
    {
        "when": "activity:sleeping",
        "do": [
            {"device": "desk_lamp", "action": "off"},
            {"device": "ceiling_light", "action": "off"},
            {"device": "speaker", "action": "pause"},
        ],
    },
    {"when": "activity:away", "do": [{"device": "ceiling_light", "action": "off"}]},
]

DEFAULT_ACTIVITIES = [
    "working_at_computer",
    "reading",
    "writing",
    "cooking",
    "eating",
    "exercising",
    "watching_tv",
    "on_phone",
    "talking",
    "cleaning",
    "sleeping",
    "relaxing",
    "away",  # nobody in view
    "other",
]


@dataclass
class Config:
    # --- Assistant ---
    name: str = "Iris"
    wake_words: list[str] = field(default_factory=lambda: ["hey iris", "iris"])
    followup_seconds: float = 8.0  # after a reply, accept a follow-up without the wake word
    conversation_idle_seconds: float = 300.0  # forget the conversation after this much silence

    # --- Claude ---
    model: str = "claude-opus-5-5"
    scene_model: str = "claude-opus-5-5"  # model used to recognize activity from frames
    effort: str = "low"  # low keeps spoken replies fast; raise for harder questions
    web_search: bool = True
    refusal_fallbacks: bool = True

    # --- Audio ---
    whisper_model: str = "base.en"
    input_device: str | int | None = None
    piper_model: str | None = None  # path to a Piper .onnx voice; falls back to espeak-ng / say
    silence_seconds: float = 0.8

    # --- Vision ---
    camera_index: int = 0
    frame_width: int = 640
    frame_height: int = 480
    gesture_fps: float = 10.0
    gesture_model: str = "models/gesture_recognizer.task"
    gesture_hold_frames: int = 4
    activity_interval: float = 20.0  # seconds between activity checks while the scene changes
    activity_max_interval: float = 180.0  # re-check a static scene at least this often
    activity_min_confidence: float = 0.6
    activities: list[str] = field(default_factory=lambda: list(DEFAULT_ACTIVITIES))
    proactive: bool = False  # let Claude decide what to do when the activity changes

    # --- Devices ---
    device_backend: str = "simulated"  # "simulated" or "home_assistant"
    home_assistant_url: str = "http://homeassistant.local:8123"
    # Friendly name -> Home Assistant entity id (home_assistant backend only)
    devices: dict[str, str] = field(default_factory=dict)

    # --- Automation ---
    rules: list[dict[str, Any]] = field(default_factory=lambda: [dict(r) for r in DEFAULT_RULES])
    rule_cooldown_seconds: float = 3.0

    @classmethod
    def load(cls, path: str | Path | None) -> "Config":
        cfg = cls()
        if path is None:
            return cfg
        raw = yaml.safe_load(Path(path).read_text()) or {}
        known = {f.name for f in fields(cls)}
        unknown = set(raw) - known
        if unknown:
            raise ValueError(f"Unknown config keys: {', '.join(sorted(unknown))}")
        for key, value in raw.items():
            setattr(cfg, key, value)
        return cfg
