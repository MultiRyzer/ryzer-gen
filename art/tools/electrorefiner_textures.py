"""The Electrorefiner's own textures, for its model (electrorefinerModel in datagen).

Pyroprocessing, as real fast reactor fuel plants plan it: a sealed gunmetal cell holding a bath of
molten salt at some 500 degrees, electrodes down through its lid, and a small sight window onto the
bath, dark while idle and glowing orange while the current runs. Cabinet, lid, screen and the
hazard band use the machine family's textures (machine_family.py).

Run from the repo root:
    python art/tools/electrorefiner_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/electrorefiner/'


def cell():
    """The cell's wall: mid gunmetal, lit along its top edge and shaded at its foot, with a seam
    between two insulation courses."""
    t = Tex()
    t.rect(0, 0, 15, 15, 's')
    t.rect(0, 0, 15, 0, 'h')
    t.rect(0, 1, 15, 1, 'L')
    t.rect(0, 7, 15, 7, 'u')
    t.rect(0, 8, 15, 8, 'L')
    t.rect(0, 15, 15, 15, 'z')
    return t


def window(lit):
    """The sight window onto the bath, 6 x 2: glass onto the salt in a dark frame, dark while
    idle, and glowing orange while it works, hottest in the middle."""
    t = Tex()
    t.rect(0, 0, 5, 1, 'U')
    if lit:
        t.rect(1, 0, 4, 1, 'X')
        t.rect(2, 0, 3, 0, 'Y')
    else:
        t.rect(1, 0, 4, 1, 'T')
        t.rect(2, 0, 3, 0, 'S')
    return t


TEXTURES = {
    'cell': cell,
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
