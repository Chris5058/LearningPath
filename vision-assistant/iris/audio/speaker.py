"""Text-to-speech with an interruptible `speak()`.

Uses Piper (natural, runs well on a Raspberry Pi) when a voice model is
configured, otherwise espeak-ng (Linux) or `say` (macOS), otherwise it just
prints the reply.
"""

from __future__ import annotations

import logging
import shutil
import subprocess
import threading

log = logging.getLogger(__name__)


class Speaker:
    def __init__(self, piper_model: str | None = None, enabled: bool = True) -> None:
        self.piper_model = piper_model
        self._lock = threading.Lock()
        self._procs: list[subprocess.Popen] = []
        self.backend = self._pick_backend() if enabled else "print"
        log.info("Speech output: %s", self.backend)

    def _pick_backend(self) -> str:
        if self.piper_model and shutil.which("piper") and shutil.which("aplay"):
            return "piper"
        if shutil.which("espeak-ng"):
            return "espeak-ng"
        if shutil.which("say"):
            return "say"
        return "print"

    def speak(self, text: str) -> None:
        """Speak and block until finished or `stop()` is called."""
        text = text.strip()
        if not text:
            return
        print(f"\n🔊 {text}\n", flush=True)
        if self.backend == "print":
            return
        try:
            if self.backend == "piper":
                piper = subprocess.Popen(
                    ["piper", "--model", self.piper_model, "--output-raw"],
                    stdin=subprocess.PIPE,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.DEVNULL,
                )
                # Piper's medium voices output 22.05 kHz 16-bit mono.
                aplay = subprocess.Popen(
                    ["aplay", "-q", "-r", "22050", "-f", "S16_LE", "-t", "raw", "-"],
                    stdin=piper.stdout,
                )
                procs = [piper, aplay]
                with self._lock:
                    self._procs = procs
                piper.stdin.write(text.encode())
                piper.stdin.close()
            else:
                procs = [subprocess.Popen([self.backend, text])]
                with self._lock:
                    self._procs = procs
            for proc in procs:
                proc.wait()
        except OSError as exc:
            log.warning("Speech failed: %s", exc)
        finally:
            with self._lock:
                self._procs = []

    def stop(self) -> None:
        """Cut off whatever is being spoken."""
        with self._lock:
            for proc in self._procs:
                if proc.poll() is None:
                    proc.terminate()
