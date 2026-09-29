"""Draws the mod icon: the fission station, running, on a graphite tile with a soft cyan glow.

The station is rendered from its design (fission_concept.py, the same quads the game draws), with
its rods glowing and steam rising from the stack, so the icon always matches the game. Run from
the repo root:
    python art/tools/mod_icon.py
Writes art/icon/ryzergen_icon.png (512 px, for Modrinth and CurseForge) and the in-game logo
shown in the NeoForge mods list.
"""
import math
import os
import shutil

import model_preview
from pixelart import PAL, ROOT, rgb, write_png

SIZE = 512
RADIUS = 72
# How much of the tile the station fills (its larger side), and the camera's tilt.
FILL = 0.84
RISE = 0.42
OUT = os.path.join(ROOT, 'art', 'icon', 'ryzergen_icon.png')
LOGO = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'ryzergen_logo.png')


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def inside(x, y, inset=0):
    """Whether a pixel centre is inside the rounded tile, shrunk by inset pixels."""
    r = RADIUS - inset
    lo, hi = inset + r, SIZE - inset - r
    cx, cy = min(max(x + 0.5, lo), hi), min(max(y + 0.5, lo), hi)
    return (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r


def tile():
    top, bottom = rgb(PAL['T']), rgb(PAL['U'])
    glow, cx, cy = rgb(PAL['i']), SIZE / 2, SIZE * 0.44
    img = [[(0, 0, 0, 0)] * SIZE for _ in range(SIZE)]
    for y in range(SIZE):
        base = mix(top, bottom, y / SIZE)
        for x in range(SIZE):
            if not inside(x, y):
                continue
            if not inside(x, y, 2):
                # One-pixel bevel, lit along the top as the GUIs are.
                img[y][x] = (*rgb(PAL['b'] if y < SIZE / 2 else PAL['K']), 255)
                continue
            d = math.hypot(x - cx, (y - cy) * 1.1) / (SIZE * 0.46)
            c = mix(base, glow, max(0.0, 1 - d) ** 2.0 * 0.55)
            img[y][x] = (*c, 255)
    # A thin cyan light line under the reactor, like the one above the GUI inventories.
    for x in range(RADIUS, SIZE - RADIUS):
        edge = min(x - RADIUS, SIZE - RADIUS - x) / 40
        img[SIZE - 44][x] = (*mix(img[SIZE - 44][x][:3], rgb(PAL['i']), min(1.0, edge)), 255)
    return img


def main():
    import fission_concept
    station = fission_concept.d
    # Size the picture from a small trial render, then draw it at the scale that fills the tile.
    trial = station.render((1, 1), 0.5, RISE, background=None)
    scale = 0.5 * SIZE * FILL / max(len(trial), len(trial[0]))
    art = station.render((1, 1), scale, RISE, background=None)
    img = tile()
    h, w = len(art), len(art[0])
    ox, oy = (SIZE - w) // 2, (SIZE - h) // 2 - 4
    # Soft contact shadow, then the reactor itself.
    sy = oy + h - 40
    for y in range(SIZE):
        for x in range(SIZE):
            d = ((x - SIZE / 2) / (w * 0.5)) ** 2 + ((y - sy) / 26) ** 2
            if d < 1 and img[y][x][3]:
                img[y][x] = (*mix(img[y][x][:3], (0, 0, 0), (1 - d) * 0.45), 255)
    for y in range(h):
        for x in range(w):
            a = art[y][x][3]
            if a and 0 <= oy + y < SIZE and 0 <= ox + x < SIZE:
                under = img[oy + y][ox + x]
                f = a / 255
                img[oy + y][ox + x] = (*mix(under[:3], art[y][x][:3], f), max(under[3], a))
    steam(img, ox + w / 2, oy + h * 0.13, w)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    write_png(OUT, img)
    shutil.copyfile(OUT, LOGO)
    print('wrote', OUT, 'station', w, 'x', h)


def steam(img, cx, cy, width):
    """A soft plume of steam rising out of the stack's mouth at (cx, cy), drifting a little and
    fading as it climbs, drawn over the picture and clipped to the tile."""
    puffs = []
    for i in range(9):
        t = i / 8
        puffs.append((cx + math.sin(t * 2.4) * width * 0.06 * t, cy - t * width * 0.34,
                      width * (0.13 + 0.12 * t), 0.5 * (1 - t) ** 1.3))
    white = (236, 242, 246)
    for y in range(SIZE):
        for x in range(SIZE):
            if not inside(x, y, 2):
                continue
            a = 0.0
            for px, py, r, strength in puffs:
                d = math.hypot(x + 0.5 - px, (y + 0.5 - py) * 1.25) / r
                if d < 1:
                    a = max(a, strength * (1 - d * d))
            if a > 0.01:
                img[y][x] = (*mix(img[y][x][:3], white, a), 255)


def spec_of(spec):
    name, _, rest = spec.partition('@')
    return name, tuple(int(v) for v in rest.split(',')) if rest else (0, 0, 0), 0


if __name__ == '__main__':
    main()
