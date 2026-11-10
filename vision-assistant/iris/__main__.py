import argparse
import logging

from .app import App
from .config import Config


def main() -> None:
    parser = argparse.ArgumentParser(prog="iris", description="Voice + vision assistant powered by Claude")
    parser.add_argument("-c", "--config", help="Path to a YAML config file")
    parser.add_argument("--text", action="store_true", help="Type instead of using the microphone")
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
    App(cfg, text_mode=args.text, use_camera=not args.no_camera, use_speech=not args.no_speech).run()


if __name__ == "__main__":
    main()
