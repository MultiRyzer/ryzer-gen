"""Draws the mod icon: the running microreactor on a graphite tile with a soft cyan glow.

The reactor is rendered from the generated models (model_preview), so the icon always matches the
game. Run from the repo root after runData:
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
REACTOR = ['microreactor_lower_front_running', 'microreactor_upper_front_running@0,1,0',
           'microreactor_lower_back_running@0,0,1', 'microreactor_upper_back_running@0,1,1']
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
    faces = model_preview.collect_faces([spec_of(s) for s in REACTOR])
    art = model_preview.render(faces, (1, 1), 8.5, background=(0, 0, 0, 0))
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
            if art[y][x][3] and 0 <= oy + y < SIZE and 0 <= ox + x < SIZE:
                img[oy + y][ox + x] = art[y][x]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    write_png(OUT, img)
    shutil.copyfile(OUT, LOGO)
    print('wrote', OUT, 'reactor', w, 'x', h)


def spec_of(spec):
    name, _, rest = spec.partition('@')
    return name, tuple(int(v) for v in rest.split(',')) if rest else (0, 0, 0), 0


if __name__ == '__main__':
    main()
