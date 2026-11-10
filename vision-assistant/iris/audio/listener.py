"""Microphone capture, utterance segmentation and speech-to-text.

Everything runs on the device: audio is cut into utterances by a simple
energy-based voice activity detector and transcribed with faster-whisper.
Only the resulting text ever leaves the device.
"""

from __future__ import annotations

import collections
import logging
import threading

import numpy as np

from .. import events

log = logging.getLogger(__name__)

SAMPLE_RATE = 16000
CHUNK_SECONDS = 0.03


class UtteranceSegmenter:
    """Cuts a stream of audio chunks into utterances.

    Speech starts when a chunk is clearly louder than the tracked background
    noise and ends after `silence_seconds` of quiet.
    """

    def __init__(
        self,
        sample_rate: int = SAMPLE_RATE,
        silence_seconds: float = 0.8,
        min_speech_seconds: float = 0.3,
        max_seconds: float = 20.0,
        preroll_seconds: float = 0.3,
        min_rms: float = 0.01,
        noise_ratio: float = 3.0,
    ) -> None:
        self.sample_rate = sample_rate
        self.silence_seconds = silence_seconds
        self.min_speech_seconds = min_speech_seconds
        self.max_seconds = max_seconds
        self.min_rms = min_rms
        self.noise_ratio = noise_ratio
        self._noise = min_rms / noise_ratio
        self._preroll: collections.deque[np.ndarray] = collections.deque()
        self._preroll_samples = int(preroll_seconds * sample_rate)
        self.reset()

    def reset(self) -> None:
        self._chunks: list[np.ndarray] = []
        self._in_speech = False
        self._speech_samples = 0
        self._silence_samples = 0
        self._total_samples = 0
        self._preroll.clear()

    def feed(self, chunk: np.ndarray) -> np.ndarray | None:
        """Add a mono float32 chunk. Returns a finished utterance, or None."""
        chunk = np.asarray(chunk, dtype=np.float32).reshape(-1)
        if chunk.size == 0:
            return None
        rms = float(np.sqrt(np.mean(chunk**2)))
        loud = rms > max(self.min_rms, self._noise * self.noise_ratio)

        if not self._in_speech:
            if not loud:
                self._noise = 0.95 * self._noise + 0.05 * rms
                self._preroll.append(chunk)
                while sum(len(c) for c in self._preroll) > self._preroll_samples:
                    self._preroll.popleft()
                return None
            self._in_speech = True
            self._chunks = list(self._preroll)
            self._preroll.clear()
            self._total_samples = sum(len(c) for c in self._chunks)

        self._chunks.append(chunk)
        self._total_samples += len(chunk)
        if loud:
            self._speech_samples += len(chunk)
            self._silence_samples = 0
        else:
            self._silence_samples += len(chunk)

        ended = self._silence_samples >= self.silence_seconds * self.sample_rate
        too_long = self._total_samples >= self.max_seconds * self.sample_rate
        if not (ended or too_long):
            return None
        long_enough = self._speech_samples >= self.min_speech_seconds * self.sample_rate
        audio = np.concatenate(self._chunks) if long_enough else None
        self.reset()
        return audio


class MicListener(threading.Thread):
    """Publishes an UTTERANCE event for everything it hears."""

    def __init__(self, cfg, bus: events.EventBus) -> None:
        super().__init__(name="mic", daemon=True)
        self.cfg = cfg
        self.bus = bus
        self._halt = threading.Event()
        self._muted = threading.Event()
        self.segmenter = UtteranceSegmenter(silence_seconds=cfg.silence_seconds)

    def mute(self, muted: bool) -> None:
        """Mute while the assistant speaks so it does not hear itself."""
        if muted:
            self._muted.set()
        else:
            self.segmenter.reset()
            self._muted.clear()

    def stop(self) -> None:
        self._halt.set()

    def run(self) -> None:
        import sounddevice as sd
        from faster_whisper import WhisperModel

        log.info("Loading speech model '%s'...", self.cfg.whisper_model)
        model = WhisperModel(self.cfg.whisper_model, device="cpu", compute_type="int8")
        blocksize = int(SAMPLE_RATE * CHUNK_SECONDS)
        with sd.InputStream(
            samplerate=SAMPLE_RATE,
            channels=1,
            dtype="float32",
            blocksize=blocksize,
            device=self.cfg.input_device,
        ) as stream:
            log.info("Listening. Say '%s' to start.", self.cfg.wake_words[0])
            while not self._halt.is_set():
                chunk, _overflowed = stream.read(blocksize)
                if self._muted.is_set():
                    continue
                audio = self.segmenter.feed(chunk[:, 0])
                if audio is None:
                    continue
                segments, _info = model.transcribe(audio, language="en", beam_size=1, vad_filter=True)
                text = " ".join(s.text.strip() for s in segments).strip()
                if text:
                    log.info("Heard: %s", text)
                    self.bus.publish(events.UTTERANCE, text=text)
