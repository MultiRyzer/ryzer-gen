"""Fluid textures: steam, a pale haze that drifts upward, and liquid sodium, a molten silver metal
with slow bright sheens across it. Run: python art/tools/fluid_textures.py

Steam is drawn opaque; GUIs and the pressure tank's sight glass set how see-through it is. The
texture is an animated strip (16 wide, one 16 x 16 frame per step) with a .mcmeta beside it.
"""
import json
import os
import random

from pixelart import ART_TEXTURES, MOD_TEXTURES, write_png

FRAMES = 16


def smooth_noise(seed, size=16, cells=4):
    """Tileable value noise: a coarse random grid, smoothly interpolated and wrapped at the edges."""
    rnd = random.Random(seed)
    grid = [[rnd.random() for _ in range(cells)] for _ in range(cells)]

    def at(x, y):
        fx, fy = x / size * cells, y / size * cells
        x0, y0 = int(fx) % cells, int(fy) % cells
        x1, y1 = (x0 + 1) % cells, (y0 + 1) % cells
        tx, ty = fx - int(fx), fy - int(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        top = grid[y0][x0] * (1 - tx) + grid[y0][x1] * tx
        bottom = grid[y1][x0] * (1 - tx) + grid[y1][x1] * tx
        return top * (1 - ty) + bottom * ty

    return at


def steam():
    """Two layers of soft noise scrolling up at different speeds, so the haze churns as it rises."""
    slow = smooth_noise(11)
    fast = smooth_noise(23, cells=8)
    rows = []
    for frame in range(FRAMES):
        for y in range(16):
            row = []
            for x in range(16):
                a = slow(x, (y + frame) % 16)
                b = fast((x + 3) % 16, (y + frame * 2) % 16)
                v = 0.65 * a + 0.35 * b
                # Billows: soft grey hollows between bright white puffs, so the churn reads.
                v = max(0.0, min(1.0, (v - 0.3) / 0.5))
                shade = 150 + int(105 * v * v * (3 - 2 * v))
                row.append((shade - 6, shade - 2, min(255, shade + 4), 255))
            rows.append(row)
    return rows


def sodium():
    """Liquid sodium: a bright silvery metal (molten sodium is mirror-like), with soft sheens
    sliding slowly across it and darker swirls between, faintly warm. Two layers of noise drift
    sideways at different speeds, so the surface looks liquid without churning like steam."""
    slow = smooth_noise(37)
    fast = smooth_noise(41, cells=8)
    rows = []
    for frame in range(FRAMES):
        for y in range(16):
            row = []
            for x in range(16):
                a = slow((x + frame) % 16, y)
                b = fast((x + frame * 2) % 16, (y + 5) % 16)
                v = max(0.0, min(1.0, (0.7 * a + 0.3 * b - 0.25) / 0.55))
                v = v * v * (3 - 2 * v)
                shade = 150 + int(95 * v)
                row.append((shade + 2, shade, shade - 6, 255))
            rows.append(row)
    return rows


def tank_glass():
    """Sight glass: clear, with opaque diagonal glare streaks (the model draws it as cutout, so
    pixels are either solid or empty). Mapped by position, so the streaks repeat every block down a
    tall tower."""
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            d = (x + y) % 16
            if d == 3:
                row.append((236, 246, 255, 255))
            elif d == 4:
                row.append((196, 222, 240, 255))
            elif d == 9:
                row.append((196, 222, 240, 255))
            else:
                row.append((0, 0, 0, 0))
        rows.append(row)
    return rows


def publish(name, rows):
    for base in (ART_TEXTURES, MOD_TEXTURES):
        out = os.path.join(base, name + '.png')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        write_png(out, rows)


def publish_animated(name, rows, frametime):
    for base in (ART_TEXTURES, MOD_TEXTURES):
        out = os.path.join(base, name + '.png')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        write_png(out, rows)
        with open(out + '.mcmeta', 'w', encoding='utf-8') as f:
            json.dump({'animation': {'frametime': frametime, 'interpolate': True}}, f, indent=2)


def main():
    publish_animated('block/steam', steam(), 3)
    publish_animated('block/sodium', sodium(), 4)
    publish('block/machine/tank_glass', tank_glass())


if __name__ == '__main__':
    main()
