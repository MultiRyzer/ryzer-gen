"""The Monazite Processor's own textures, for its model (monaziteProcessorModel in datagen).

Rare earth processing, as real plants do it: raw monazite digested in hot acid in a rubber-lined
steel vessel, then thorium and yttrium drawn apart in solvent extraction columns. The vessel is a
dark green-grey (the lining's colour shows at its seams), with a small sight window onto the acid,
dark while idle and glowing yellow-green while it digests; the columns are bright steel. Cabinet,
lid, screen, copper and accent use the machine family's textures (machine_family.py).

Run from the repo root:
    python art/tools/monazite_processor_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/monazite_processor/'
LINING = '2f3a33'       # the vessel's acid-proof lining, a dark green-grey
LINING_LIT = '4a5a4f'
LINING_DARK = '1f2722'
ACID = 'c8e04a'         # hot acid through the window, yellow-green
ACID_HOT = 'f0ff9a'


def put(t, x, y, colour):
    """Sets a pixel to a colour outside the palette (hex, no #)."""
    t.px[y][x] = colour


def vessel():
    """The digester's wall: dark green-grey lined steel, lit along its top, shaded at its foot, with a
    weld seam between two courses."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            put(t, x, y, LINING)
    for x in range(16):
        put(t, x, 0, LINING_LIT)
        put(t, x, 7, LINING_DARK)
        put(t, x, 8, LINING_LIT)
        put(t, x, 15, LINING_DARK)
    return t


def column():
    """An extraction column: bright steel, lit down its left, a flange line each 4 pixels."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 0, 15, 'A')
    t.rect(15, 0, 15, 15, 'I')
    for y in range(3, 16, 4):
        t.rect(0, y, 15, y, 'I')
    return t


def column_on():
    """An extraction column while it works (drawn by its renderer to a level that pulses): the same
    flange lines over the pale yellow-green solvent glowing through, brighter at its middle."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            put(t, x, y, ACID_HOT if x in (6, 7, 8, 9) else ACID)
    for y in range(3, 16, 4):
        t.rect(0, y, 15, y, 'I')
    return t


def window(lit):
    """The sight window onto the acid, 4 x 2: glass in a dark frame, dark while idle and glowing
    yellow-green while it digests, brightest in the middle."""
    t = Tex()
    t.rect(0, 0, 3, 1, 'U')
    if lit:
        put(t, 1, 0, ACID)
        put(t, 2, 0, ACID_HOT)
        put(t, 1, 1, ACID)
        put(t, 2, 1, ACID)
    else:
        t.rect(1, 0, 2, 1, 'T')
        t.set(2, 0, 'S')
    return t


TEXTURES = {
    'vessel': vessel,
    'column': column,
    'column_on': column_on,
    'window': lambda: window(False),
    'window_on': lambda: window(True),
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        # Lit textures stay crisp; the rest get the surface finish (pixelart.finish).
        (publish if name.endswith('_on') else publish_finished)(FOLDER + name, fn())


if __name__ == '__main__':
    main()
