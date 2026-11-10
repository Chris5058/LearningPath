#!/usr/bin/env bash
# Downloads the on-device models into ./models
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p models

echo "Hand gesture model (8 MB)..."
curl -fL -o models/gesture_recognizer.task \
  https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task

if [[ "${1:-}" == "--voice" ]]; then
  echo "Piper voice (60 MB)..."
  base=https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium
  curl -fL -o models/en_US-lessac-medium.onnx "$base/en_US-lessac-medium.onnx"
  curl -fL -o models/en_US-lessac-medium.onnx.json "$base/en_US-lessac-medium.onnx.json"
fi
echo "Done. The speech-to-text model downloads itself on first run."
