import argparse
import logging

from .app import App
from .config import Config


def main() -> None:
    parser = argparse.ArgumentParser(prog="iris", description="Voice + vision assistant powered by Claude")
    parser.add_argument("-c", "--config", help="Path to a YAML config file")
    parser.add_argument("--text", action="store_true", help="Type instead of using the microphone")
    parser.add_argument("--ui", action="store_true", help="Serve the web UI (camera view, devices, chat)")
    parser.add_argument("--host", default="127.0.0.1", help="Web UI address; 0.0.0.0 exposes it to your network")
    parser.add_argument("--port", type=int, default=8080, help="Web UI port")
    parser.add_argument("--no-browser", action="store_true", help="Do not open the web UI in a browser automatically")
    parser.add_argument("--no-mic", action="store_true", help="Disable the microphone (use the web UI to type)")
    parser.add_argument("--no-camera", action="store_true", help="Disable the camera, gestures and activity recognition")
    parser.add_argument("--no-speech", action="store_true", help="Print replies instead of speaking them")
    parser.add_argument("-v", "--verbose", action="store_true")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
        datefmt="%H:%M:%S",
    )
    logging.getLogger("httpx2").setLevel(logging.WARNING)
    logging.getLogger("httpx").setLevel(logging.WARNING)

    cfg = Config.load(args.config)
    if args.no_mic and not (args.ui or args.text):
        parser.error("--no-mic needs --ui or --text, otherwise there is no way to talk to the assistant")
    app = App(
        cfg,
        text_mode=args.text,
        use_camera=not args.no_camera,
        use_speech=not args.no_speech,
        use_mic=not args.no_mic,
    )
    if args.ui:
        app.serve_web(args.host, args.port, open_browser=not args.no_browser)
    app.run()


if __name__ == "__main__":
    main()
