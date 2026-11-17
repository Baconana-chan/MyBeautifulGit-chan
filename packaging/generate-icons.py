#!/usr/bin/env python3
"""Generate packaging icon assets. Requires Pillow (development-only)."""
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
SIZE = 1024
BACKGROUND = (7, 11, 8, 255)
GREEN = (57, 255, 20, 255)
GREEN_DIM = (27, 92, 31, 255)


def make_icon() -> Image.Image:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle(
        (36, 36, SIZE - 36, SIZE - 36),
        radius=150,
        fill=BACKGROUND,
        outline=GREEN_DIM,
        width=14,
    )
    draw.rounded_rectangle(
        (72, 72, SIZE - 72, SIZE - 72),
        radius=120,
        outline=GREEN,
        width=22,
    )
    # Terminal prompt: >_
    draw.line(
        [(255, 300), (475, 512), (255, 724)],
        fill=GREEN,
        width=62,
        joint="curve",
    )
    draw.line([(555, 690), (790, 690)], fill=GREEN, width=62)
    # Small cursor block.
    draw.rounded_rectangle((805, 654, 875, 726), radius=10, fill=GREEN)
    return image


def main() -> None:
    source = make_icon()
    source.resize((512, 512), Image.Resampling.LANCZOS).save(HERE / "icon.png")
    source.resize((256, 256), Image.Resampling.LANCZOS).save(
        HERE / "icon.ico",
        sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
    )
    source.save(HERE / "icon.icns", format="ICNS")


if __name__ == "__main__":
    main()
