"""The Melt Drill's own textures (block/drill/), for its concept (melt_drill_concept.py), drawn to
the house style and even, as the breeder's are: big faces get panels drawn a block each, bands get
textures drawn at their height, round parts change only down their height.
- casing: the drill core's light cladding, one plate per block, lit along the top and the left, a
  shadowed foot and right edge, so a wall of them reads as neat plates (as the station's stack).
- pot: the pressure vessel's brushed steel, changing only across, so the round pot shows it evenly.
- vent: a louvred graphite vent, a block, set into the middle of each face of the core.
- band: a graphite band 4 high (rows 12 to 15), lit along its top, bolted every 4 pixels: the
  core's top and the chamber's ceiling.
- frame: the beam chamber's floor frame, 4 high, gunmetal with a lit lip.
- skirt, ring and warning: the quarry block's round body, a small reactor station of its own: a
  graphite skirt with vent slots, a gunmetal head ring with the groove for its light line, and
  the station's aviation marking (red, white, red) round its top.
- band8: a graphite band 8 high (rows 8 to 15), for the mega drill's deck edge and cap: a lit lip,
  a bolt line, a groove and a shadowed foot.
- leg: the mega drill's buttress legs, drawn for swept bars (u runs along the leg, v across it):
  light plating with a lit edge, a recessed groove down the middle and bolts every 8 pixels.
- window (lit): a head window onto the beam generator: its coils glowing orange behind the glass,
  dark bars between them, a pale glint across the top corner.
- crust: the pit while the drill stands idle, the melt cooled to dark glassy rock with faint red
  cracks still glowing in it.
- melt and beam (animated, lit): the molten pit, dark crust drifting slowly over glowing melt,
  and the laser beam, a bright core flickering along its length.
Run from the repo root:
    python art/tools/drill_textures.py
"""
import json
import math
import os

from pixelart import *  # noqa: F401,F403
from pixelart import ART_TEXTURES, MOD_TEXTURES, write_png
from fluid_textures import smooth_noise

FOLDER = 'block/drill/'


def casing():
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 15, 1, 'A')
    t.rect(0, 2, 0, 14, 'A')
    t.rect(15, 2, 15, 14, 'J')
    t.rect(0, 15, 15, 15, 'I')
    t.rect(1, 14, 14, 14, 'F')
    return t


def pot():
    """The pressure vessel's brushed steel: light, with fine vertical brushing (columns a shade
    apart), changing only across, so a round pot shows it evenly; lit along the top, shadowed at
    the foot."""
    t = Tex()
    for x in range(16):
        t.rect(x, 0, x, 15, ('E', 'E', 'F', 'E', 'A', 'E', 'F', 'F')[x % 8])
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 15, 15, 15, 'I')
    return t


def vent():
    t = Tex()
    t.rect(0, 0, 15, 15, 'b')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 0, 0, 15, 'M')
    t.rect(15, 0, 15, 15, 'U')
    t.rect(0, 15, 15, 15, 'U')
    for y in range(2, 14):
        t.rect(2, y, 13, y, ('M', 'S', 'U')[(y - 2) % 3])
    return t


def band():
    t = Tex()
    t.rect(0, 12, 15, 15, 'S')
    t.rect(0, 12, 15, 12, 'M')
    t.rect(0, 15, 15, 15, 'U')
    for x in range(1, 16, 4):
        t.set(x, 13, 'l')
        t.set(x, 14, 'T')
    return t


def band8():
    t = Tex()
    t.rect(0, 8, 15, 15, 'S')
    t.rect(0, 8, 15, 8, 'M')
    t.rect(0, 9, 15, 9, 'b')
    for x in range(2, 16, 4):
        t.set(x, 11, 'l')
    t.rect(0, 13, 15, 13, 'T')
    t.rect(0, 15, 15, 15, 'U')
    return t


def leg():
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 1, 15, 1, 'A')
    t.rect(0, 15, 15, 15, 'I')
    t.rect(0, 14, 15, 14, 'F')
    t.rect(0, 7, 15, 7, 'I')
    t.rect(0, 8, 15, 8, 'A')
    for x in range(3, 16, 8):
        for y in (4, 11):
            t.set(x, y, 'I')
            t.set(x - 1, y - 1, 'A')
    return t


def window():
    t = Tex()
    for y in range(16):
        t.rect(0, y, 15, y, ('Z', 'X', 'X', 'e', 'e', 'X', 'X', 'Z')[y % 8])
    for x in (3, 8, 13):
        t.rect(x, 0, x, 15, 'o')
    for k in range(4):
        t.set(1 + k, 3 - k, 'W')
    return t


def crust():
    t = Tex()
    t.rect(0, 0, 15, 15, 'T')
    for x, y in ((2, 3), (3, 4), (4, 4), (5, 5), (9, 2), (10, 3), (10, 4), (11, 5), (12, 5), (6, 10), (7, 11),
                 (8, 11), (9, 12), (13, 9), (13, 10), (14, 11), (1, 12), (2, 13), (3, 13)):
        t.set(x, y, '8')
    for x, y in ((4, 4), (10, 4), (8, 11), (13, 10), (2, 13)):
        t.set(x, y, 'Z')
    for x, y in ((6, 1), (14, 3), (11, 14), (0, 7)):
        t.set(x, y, 'S')
    return t


FRAMES = 16
MELT = [(60, 16, 6), (120, 30, 8), (190, 62, 10), (236, 112, 22), (252, 170, 50), (255, 222, 120)]


def melt():
    """Frames of the molten pit: two layers of slow noise; the low places are crust (dark), the
    high ones open melt glowing orange to yellow, drifting a pixel every other frame."""
    slow = smooth_noise(71)
    fine = smooth_noise(83, cells=8)
    rows = []
    for frame in range(FRAMES):
        for y in range(16):
            row = []
            for x in range(16):
                v = 0.7 * slow((x + frame // 2) % 16, (y + frame // 4) % 16) + 0.3 * fine((x + frame) % 16, y)
                k = max(0, min(len(MELT) - 1, int((v - 0.2) / 0.6 * len(MELT))))
                row.append(MELT[k] + (255,))
            rows.append(row)
    return rows


def beam():
    """Frames of the laser beam (drawn along v): a white-hot core between cyan edges, its
    brightness flickering in bands that run along it."""
    rows = []
    for frame in range(FRAMES):
        for y in range(16):
            pulse = 0.75 + 0.25 * math.sin((y + frame * 3) / 16 * 2 * math.pi)
            row = []
            for x in range(16):
                edge = abs(x - 7.5) / 7.5
                core = max(0.0, 1 - edge * 1.6)
                r = int(min(255, (53 + 200 * core) * pulse))
                g = int(min(255, (200 + 55 * core) * pulse))
                b = int(min(255, 245 * pulse + 10))
                row.append((r, g, b, 255))
            rows.append(row)
    return rows


def publish_animated(name, rows, frametime):
    for base in (ART_TEXTURES, MOD_TEXTURES):
        out = os.path.join(base, name + '.png')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        write_png(out, rows)
        with open(out + '.mcmeta', 'w', encoding='utf-8') as f:
            json.dump({'animation': {'frametime': frametime, 'interpolate': True}}, f, indent=2)


def frame():
    t = Tex()
    t.rect(0, 12, 15, 15, 'u')
    t.rect(0, 12, 15, 12, 's')
    t.rect(0, 15, 15, 15, 'z')
    return t


def skirt():
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 15, 15, 15, 'U')
    for x in (3, 7, 11):
        t.rect(x, 4, x + 1, 11, 'U')
        t.rect(x, 4, x + 1, 4, 'T')
    return t


def ring():
    t = Tex()
    for y, c in enumerate(['H', 's', 'u', 'u', 'z', 'T', 'T', 'z', 's', 'u', 'u', 'u', 'u', 'u', 'z', 'K']):
        t.rect(0, y, 15, y, c)
    return t


def warning():
    t = Tex()
    for y, c in enumerate(['x'] * 5 + ['A'] * 5 + ['x'] * 6):
        t.rect(0, y, 15, y, c)
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 15, 15, 15, '8')
    return t


TEXTURES = {
    'casing': casing,
    'pot': pot,
    'vent': vent,
    'band': band,
    'frame': frame,
    'band8': band8,
    'leg': leg,
    'crust': crust,
    'window': window,
    'skirt': skirt,
    'ring': ring,
    'warning': warning,
}


def main():
    for name, fn in TEXTURES.items():
        publish_finished(FOLDER + name, fn())
    publish_animated(FOLDER + 'melt', melt(), 6)
    publish_animated(FOLDER + 'beam', beam(), 2)


if __name__ == '__main__':
    main()
