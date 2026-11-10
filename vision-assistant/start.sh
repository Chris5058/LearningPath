#!/usr/bin/env bash
# One command to set up and start Iris with the web UI.
#   ./start.sh              first run installs everything, later runs just start
#   ./start.sh --no-camera  any extra options are passed on to Iris
# Works in Git Bash on Windows, and on macOS and Linux.
set -euo pipefail
cd "$(dirname "$0")"

# Set PYTHON to use a specific interpreter, e.g.
#   PYTHON="/c/Users/you/AppData/Local/Programs/Python/Python312/python.exe" ./start.sh
PY="${PYTHON:-python}"
command -v "$PY" >/dev/null 2>&1 || PY=python3

if [[ ! -d .venv ]]; then
  echo "Creating the Python environment (first run only)..."
  "$PY" -m venv .venv
fi
if [[ -f .venv/Scripts/activate ]]; then
  source .venv/Scripts/activate
else
  source .venv/bin/activate
fi

if ! python -c "import anthropic, yaml, numpy" 2>/dev/null; then
  echo "Installing the core packages..."
  python -m pip install --quiet anthropic pyyaml numpy
fi
if ! python -c "import cv2, mediapipe" 2>/dev/null; then
  echo "Installing the camera packages (this can take a few minutes)..."
  if ! python -m pip install --quiet opencv-python-headless mediapipe; then
    echo
    echo "!! This Python is: $(python -c 'import sys, platform; print(sys.version.split()[0], platform.machine(), sys.executable)')"
    echo "!! The camera packages could not be installed on this computer."
    echo "!! Iris will start without the camera; chat and simulated gestures still work."
    echo
  fi
fi

if [[ ! -f models/gesture_recognizer.task ]]; then
  echo "Downloading the hand gesture model..."
  mkdir -p models
  curl -fL --progress-bar -o models/gesture_recognizer.task \
    https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task \
    || echo "!! Could not download the gesture model; gestures will be off."
fi

if [[ -z "${ANTHROPIC_API_KEY:-}" ]]; then
  read -r -s -p "Paste your Claude API key (sk-ant-...) and press Enter: " ANTHROPIC_API_KEY
  echo
  export ANTHROPIC_API_KEY
fi

CONFIG=()
[[ -f config.yaml ]] && CONFIG=(-c config.yaml)
exec python -m iris --ui --no-mic "${CONFIG[@]}" "$@"
