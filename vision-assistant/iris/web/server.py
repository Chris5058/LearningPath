"""A small local web page for watching and driving the assistant.

Shows the live camera with the tracked hand, the recognized gesture and
activity, device states and the conversation, and lets you type to the
assistant. Uses only the standard library.

There is no login. It listens on 127.0.0.1 by default, so only this machine
can open it; anyone who can reach the port can see the camera.
"""

from __future__ import annotations

import json
import logging
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from .. import events

log = logging.getLogger(__name__)

INDEX = Path(__file__).with_name("index.html")
MAX_BODY = 16 * 1024
BOUNDARY = "frame"


def build_state(app, since: int) -> dict:
    points, pose = app.gestures.seen if app.gestures else ([], None)
    return {
        "name": app.cfg.name,
        "status": app.status,
        "listening": app.gate.is_open,
        "wake_word": app.cfg.wake_words[0],
        "microphone": app.mic is not None,
        "camera": app.camera is not None,
        "hand_visible": bool(points),
        "pose": pose,
        "activity": app.activity.current if app.activity else None,
        "activity_enabled": bool(app.activity and app.activity.enabled),
        "devices": app.devices.list_devices(),
        "speech_backend": app.speaker.backend,
        "feed": app.feed_since(since),
    }


def make_handler(app):
    class Handler(BaseHTTPRequestHandler):
        protocol_version = "HTTP/1.1"

        def log_message(self, fmt, *args):  # keep the terminal quiet
            log.debug("web: " + fmt, *args)

        # ------------------------------------------------------------ helpers

        def _send(self, status: int, body: bytes, content_type: str) -> None:
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)

        def _json(self, payload, status: int = 200) -> None:
            self._send(status, json.dumps(payload).encode(), "application/json")

        def _read_json(self) -> dict | None:
            """Parse a JSON request body. Requiring a JSON content type and a
            matching Origin stops other websites open in the same browser from
            sending commands to the assistant."""
            if self.headers.get("Content-Type", "").split(";")[0].strip() != "application/json":
                return None
            origin = self.headers.get("Origin")
            if origin and origin.split("://", 1)[-1] != self.headers.get("Host"):
                return None
            try:
                length = int(self.headers.get("Content-Length", "0"))
            except ValueError:
                return None
            if not 0 < length <= MAX_BODY:
                return None
            try:
                body = json.loads(self.rfile.read(length))
            except (ValueError, UnicodeDecodeError):
                return None
            return body if isinstance(body, dict) else None

        # ------------------------------------------------------------- routes

        def do_GET(self):
            path, _, query = self.path.partition("?")
            if path == "/":
                self._send(200, INDEX.read_bytes(), "text/html; charset=utf-8")
            elif path == "/api/state":
                since = 0
                for part in query.split("&"):
                    if part.startswith("since=") and part[6:].isdigit():
                        since = int(part[6:])
                self._json(build_state(app, since))
            elif path == "/stream":
                self._stream()
            else:
                self._json({"error": "not found"}, 404)

        def do_POST(self):
            body = self._read_json()
            if body is None:
                self._json({"error": "expected a JSON object"}, 400)
                return
            if self.path == "/api/chat":
                text = str(body.get("text", "")).strip()
                if not text:
                    self._json({"error": "text is required"}, 400)
                    return
                app.bus.publish(events.UTTERANCE, text=text, typed=True)
            elif self.path == "/api/gesture":
                name = str(body.get("name", "")).strip()
                if not name:
                    self._json({"error": "name is required"}, 400)
                    return
                app.bus.publish(events.GESTURE, name=name)
            elif self.path == "/api/settings":
                if app.activity and isinstance(body.get("activity_enabled"), bool):
                    app.activity.enabled = body["activity_enabled"]
            else:
                self._json({"error": "not found"}, 404)
                return
            self._json({"ok": True})

        def _stream(self) -> None:
            """Live camera as an MJPEG stream."""
            if app.camera is None:
                self._json({"error": "camera is off"}, 404)
                return
            import cv2

            self.send_response(200)
            self.send_header("Content-Type", f"multipart/x-mixed-replace; boundary={BOUNDARY}")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Connection", "close")
            self.end_headers()
            try:
                while not app.web_stopping.is_set():
                    frame = app.preview()
                    if frame is None:
                        time.sleep(0.2)
                        continue
                    ok, jpeg = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 70])
                    if ok:
                        data = jpeg.tobytes()
                        self.wfile.write(
                            f"--{BOUNDARY}\r\nContent-Type: image/jpeg\r\n"
                            f"Content-Length: {len(data)}\r\n\r\n".encode()
                        )
                        self.wfile.write(data)
                        self.wfile.write(b"\r\n")
                    time.sleep(0.08)
            except (BrokenPipeError, ConnectionError):
                pass  # the browser closed the page
            self.close_connection = True

    return Handler


class WebUI:
    def __init__(self, app, host: str = "127.0.0.1", port: int = 8080) -> None:
        self.app = app
        app.web_stopping = threading.Event()
        self.server = ThreadingHTTPServer((host, port), make_handler(app))
        self.server.daemon_threads = True
        self.host, self.port = self.server.server_address[:2]

    @property
    def url(self) -> str:
        shown = "localhost" if self.host in ("127.0.0.1", "0.0.0.0") else self.host
        return f"http://{shown}:{self.port}"

    def start(self) -> None:
        threading.Thread(target=self.server.serve_forever, name="web", daemon=True).start()
        log.info("Web UI: %s", self.url)
        if self.host not in ("127.0.0.1", "localhost"):
            log.warning("The web UI has no login: anyone on your network can see the camera.")

    def stop(self) -> None:
        self.app.web_stopping.set()
        self.server.shutdown()
        self.server.server_close()
