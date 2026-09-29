"""The machine family's shared textures: what makes the single-block machines look like one
family, while each machine's own script draws the parts that make it itself (a smelter's coil, a
crusher's flywheel, a pump's motor).

Every machine stands on the same graphite skid, keeps its controls in the same light cabinet with a
lit screen and a cyan status strip, and uses the same gunmetal, bright steel and copper. Hazard
stripes are not part of the family: only machines that are actually dangerous carry them.

Materials map by position: a face shows the part of the texture at its place in the block, so bands
use the rows at their height. Decals map from their own top left, one texel per pixel.

Run from the repo root (the machine scripts publish these too):
    python art/tools/machine_family.py
"""
from pixelart import *  # noqa: F401,F403

FOLDER = 'block/machine/family/'


def skid():
    """The graphite skid every machine stands on. Its sides show rows 14 and 15 (it is 2 pixels
    high): a lit lip, then dark feet under the corners and the middle. Its top is a plain plate."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 0, 15, 0, 'b')
    t.rect(0, 0, 0, 15, 'b')
    t.rect(15, 0, 15, 15, 'T')
    t.rect(0, 14, 15, 14, 'b')
    t.rect(0, 15, 15, 15, 'T')
    for x0 in (0, 7, 13):
        t.rect(x0, 15, x0 + 2, 15, 'U')
    return t


def cabinet():
    """The light casing of a machine's control cabinet: one clean panel per face, lit top and left
    edges, a shaded foot and right edge, and a seam across the middle where the doors meet."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 0, 0, 15, 'A')
    t.rect(15, 0, 15, 15, 'J')
    t.rect(0, 15, 15, 15, 'I')
    t.rect(1, 8, 14, 8, 'I')
    t.rect(1, 9, 14, 9, 'F')
    return t


def lid():
    """A cabinet's lid: gunmetal with two rows of vent slots."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    t.rect(0, 0, 15, 0, 's')
    t.rect(0, 0, 0, 15, 's')
    t.rect(15, 0, 15, 15, 'z')
    t.rect(0, 15, 15, 15, 'z')
    for y in (5, 9):
        for x in range(3, 13):
            t.set(x, y, 'T')
            t.set(x, y + 1, 'z')
    return t


def screen():
    """A cabinet's readout, 9 x 2: a dark screen with one cyan power bar and a green status dot.
    Always lit, as screens are in the house style."""
    t = Tex()
    t.rect(0, 0, 8, 1, 'U')
    t.rect(1, 1, 5, 1, 'i')
    t.set(7, 0, 'n')
    return t


def vent():
    """A vent grille, 4 x 6: dark slots in a graphite frame."""
    t = Tex()
    t.rect(0, 0, 3, 5, 'S')
    for y in (1, 3):
        t.rect(0, y, 3, y, 'U')
    t.rect(0, 5, 3, 5, 'T')
    return t


def metal():
    """Plain gunmetal, for vessels, frames and pedestals: lit along the top of each block."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    t.rect(0, 0, 15, 0, 's')
    t.rect(0, 15, 15, 15, 'z')
    return t


def bright():
    """Bright machined steel, for rams, rods and rings."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 15, 15, 15, 'I')
    return t


def copper():
    """Clean copper, for pipes and bars: lit along the top."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'R')
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 15, 15, 15, 'r')
    return t


def accent():
    """Orange accent, for small hardware: latches, caps, clips and handles."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'X')
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 15, 15, 15, 'Z')
    return t


def hazard():
    """Yellow on graphite hazard stripes, for the skids of machines that are actually dangerous."""
    return letters_rows([['Y' if (x + y) % 6 < 3 else 'B' for x in range(16)] for y in range(16)])


def connector():
    """Where a pipe meets a machine, 8 x 8: a graphite flange (lit top and left, shaded foot) round
    a dark socket with a ring, the pipe's size. Neutral, as any side of a machine takes anything;
    the multiblocks' ports carry colours instead."""
    t = Tex()
    t.rect(0, 0, 7, 7, 'S')
    t.rect(0, 0, 7, 0, 'b')
    t.rect(0, 0, 0, 7, 'b')
    t.rect(7, 1, 7, 7, 'U')
    t.rect(1, 7, 7, 7, 'U')
    t.rect(2, 2, 5, 5, 'T')
    t.rect(3, 3, 4, 4, 'U')
    t.rect(2, 2, 5, 2, 'k')
    t.rect(2, 2, 2, 5, 'k')
    return t


def panel(w, h):
    """A light cabinet panel drawn at its size, for a big flat face: a softened outline, a lit
    bevel inside it, a shaded foot and right edge, and a screw in each corner (a dark head, lit on
    its top left). Designed per face, as Mekanism's machines are, rather than tiled."""
    t = Tex()
    t.rect(0, 0, w - 1, h - 1, 'E')
    t.rect(0, 0, w - 1, 0, 'I')
    t.rect(0, 0, 0, h - 1, 'I')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    t.rect(1, 1, w - 2, 1, 'A')
    t.rect(1, 1, 1, h - 2, 'A')
    t.rect(w - 2, 2, w - 2, h - 2, 'F')
    t.rect(2, h - 2, w - 2, h - 2, 'F')
    if w >= 8 and h >= 6:
        for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
            t.set(x, y, 'J')
    return t


# The panel sizes the machines' models use (lpanel_WxH in datagen): the service frame's roof
# (13 x 14) and rear cabinet backs (13 x 12, the fuel fabricator's 13 x 5), the electric smelter's
# cabinet sides, and the fuel fabricator's base cabinet (13 x 6 front and back, 14 x 6 sides).
PANELS = [(13, 12), (6, 12), (13, 6), (13, 14), (13, 5), (14, 6)]


def letters_rows(rows):
    t = Tex()
    for y, row in enumerate(rows):
        for x, k in enumerate(row):
            t.set(x, y, k)
    return t


TEXTURES = {
    'skid': skid,
    'cabinet': cabinet,
    'lid': lid,
    'screen': screen,
    'vent': vent,
    'metal': metal,
    'bright': bright,
    'copper': copper,
    'accent': accent,
    'hazard': hazard,
    'connector': connector,
}


# Always lit, so kept crisp; everything else gets the surface finish (pixelart.finish).
PLAIN = {'screen', 'connector'}


def main():
    for name, fn in TEXTURES.items():
        (publish if name in PLAIN else publish_finished)(FOLDER + name, fn())
    for w, h in PANELS:
        publish_finished(FOLDER + f'lpanel_{w}x{h}', panel(w, h))


if __name__ == '__main__':
    main()
