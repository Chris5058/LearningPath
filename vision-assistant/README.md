# Iris: a voice + vision assistant powered by Claude

Iris is an Alexa-style assistant for a small always-on device (a Raspberry Pi
with a microphone, speaker and camera). You talk to it, it answers with Claude,
and it also *sees*: it reads hand gestures, recognizes what you are doing, and
acts on both.

| You do this | Iris does this |
|---|---|
| Say "Hey Iris, what's the weather tomorrow?" | Answers out loud (Claude + web search) |
| Say "Iris, what am I holding?" | Takes a picture and answers from it |
| Say "Iris, dim the desk lamp and set a pasta timer for 9 minutes" | Controls devices, announces the timer |
| Hold up an open palm | Stops talking |
| Point one finger up | Listens without the wake word |
| Thumbs up / down, swipe left / right, fist | Volume, next / previous track, pause |
| Sit down with a book | Recognizes "reading" and turns on the desk lamp |
| Leave the room | Recognizes "away" and turns the light off |

Every gesture and activity reaction is a rule in `config.yaml`, so you can
change what each one does.

## How it works

```
 microphone ──► voice activity detection ──► Whisper (on device) ──► text ─┐
                                                                           │
 camera ──┬──► MediaPipe hand gestures (on device) ──► gesture ────────────┤
          │                                                                ▼
          └──► changed frame every ~20 s ──► Claude vision ──► activity ─► event bus
                                                                           │
                    ┌──────────────────────────────────────────────────────┤
                    ▼                                                      ▼
            rules (config.yaml)                              Claude conversation with tools:
            gesture/activity → action                        look_at_camera, control_device,
                    │                                        set_timer, web_search, ...
                    ▼                                                      │
            devices (simulated or Home Assistant)  ◄───────────────────────┤
                                                                           ▼
                                                              text-to-speech ──► speaker
```

| Module | Role |
|---|---|
| `iris/audio/listener.py` | Microphone, utterance segmentation, speech-to-text |
| `iris/audio/speaker.py` | Text-to-speech (Piper, espeak-ng or macOS `say`), interruptible |
| `iris/wake.py` | Wake word and follow-up window |
| `iris/vision/camera.py` | Camera capture |
| `iris/vision/gestures.py` | Hand poses and swipes |
| `iris/vision/activity.py` | "What is the user doing?" via Claude vision |
| `iris/brain.py` | Claude conversation and tools |
| `iris/automation.py` | Rules: gesture / activity → actions |
| `iris/devices.py` | Simulated devices and a Home Assistant backend |
| `iris/web/` | Local web UI |
| `iris/app.py` | Ties it all together |

## Try it on your laptop first

The quick way, from a terminal (Git Bash on Windows):

```bash
cd vision-assistant
./start.sh
```

It creates the Python environment, installs the packages, downloads the
gesture model, asks for your Claude API key if `ANTHROPIC_API_KEY` is not set,
and opens the web UI. Later runs skip what is already done. The manual steps
are below.

No device or smart-home hardware is needed to start.

```bash
cd vision-assistant
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
./scripts/download_models.sh
export ANTHROPIC_API_KEY=sk-ant-...        # from console.anthropic.com

# 1. Keyboard only: no mic, no camera
python -m iris --text --no-camera

# 2. Keyboard + webcam (gestures, activity, "what do you see?")
python -m iris --text

# 3. Everything
python -m iris
```

## Web UI

Add `--ui`; the page opens in your browser at http://localhost:8080
(`--no-browser` stops it opening by itself):

```bash
python -m iris --ui              # mic + camera + web page
python -m iris --ui --no-mic     # no microphone: type in the page instead
```

The page shows the live camera with the tracked hand drawn on it, the gesture
it currently sees, the recognized activity (with a switch to turn recognition
off), device states, and the conversation with a box to type in. Buttons let
you simulate each gesture without a camera. If the machine has no speech
engine (for example Windows), the page can speak the replies itself.

If the camera cannot be used (not installed, busy in another app, blocked by
privacy settings), the camera panel says why.

The page has no login and is only reachable from the same machine. To view it
from another computer (for example a Pi without a screen), add
`--host 0.0.0.0`; anyone on your network can then open it and see the camera.

On Windows, activate the environment with `.venv\Scripts\activate` (or
`source .venv/Scripts/activate` in Git Bash) and set the key with
`set ANTHROPIC_API_KEY=...`.

In `--text` mode you can also type `/gesture thumbs_up` or `/activity reading`
to simulate what the camera would report. Devices are simulated by default and
every action is logged, so you can see what would happen.

On Linux, install the system libraries first:

```bash
sudo apt install libegl1 libportaudio2 espeak-ng
```

## Putting it on a device

Recommended hardware:

- Raspberry Pi 5, 8 GB (a Pi 4 works; set `whisper_model: tiny.en`)
- Raspberry Pi Camera Module 3 Wide, or any USB webcam
- A USB speakerphone (mic and speaker in one, with echo cancellation), or a
  USB microphone plus any speaker
- 32 GB microSD card and the official power supply

Steps, on Raspberry Pi OS (64-bit):

```bash
sudo apt install python3-venv libegl1 libportaudio2 espeak-ng
git clone <this repo> && cd LearningPath/vision-assistant
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
./scripts/download_models.sh --voice       # also fetches a natural Piper voice
pip install piper-tts                      # optional: much nicer voice than espeak-ng
cp config.example.yaml config.yaml         # set piper_model, camera_index, rules, ...
python -m iris -c config.yaml
```

To start it at boot, see `deploy/iris.service`.

### Controlling real devices

Set `device_backend: home_assistant` in `config.yaml`, map your device names to
Home Assistant entity ids under `devices:`, and export `HOME_ASSISTANT_TOKEN`
(a long-lived access token from your Home Assistant profile). Any light or
media player Home Assistant knows can then be controlled by voice, gesture or
activity. To support something else, implement the two methods of
`DeviceBackend` in `iris/devices.py`.

## Privacy and cost

- Audio never leaves the device. Speech is transcribed locally; only the text
  of what you say after the wake word is sent to Claude.
- Gestures are recognized locally.
- Camera frames are sent to the Claude API in two cases: when you ask something
  that needs sight, and for activity recognition (one frame, at most every
  `activity_interval` seconds, and only when the scene has changed). Use
  `--no-camera` to turn all vision off. Tell the people you live with that
  there is a camera.
- Activity recognition is the main running cost because it is the only thing
  that calls Claude without being asked. To reduce it, set `scene_model` to a
  smaller model such as `claude-haiku-5-5`, raise `activity_interval`, or turn
  it off (`activity_recognition: false`, or the switch in the web UI).
- `proactive: true` lets Claude decide on its own what to do when your activity
  changes, instead of only following your rules. It is off by default.

## Tests

```bash
pip install -r requirements-dev.txt
python -m pytest
```

The tests cover the wake word, utterance segmentation, gesture debouncing and
swipes, scene-change detection, rules, the Claude tool loop and the app's event
handling, using a scripted stand-in for the Claude API.

## Ideas for next steps

- Speak replies sentence by sentence while Claude is still writing (streaming)
- A dedicated wake-word model (openWakeWord) so Whisper only runs after it
- Recognize who is in view and keep per-person preferences
- Custom gestures trained with MediaPipe Model Maker
- Long-term memory (routines, preferences) across conversations
- A small screen or LED ring to show listening / thinking / speaking
