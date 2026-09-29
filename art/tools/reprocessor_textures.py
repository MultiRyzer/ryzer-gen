"""The Reprocessor's own textures, for its model (reprocessorBoxes in datagen).

Two high: a dissolver vessel below, where spent fuel meets fluorine, and a tall lead-lined
separation column above, with a sight glass, a hazard band and label plate, and a copper line
between them. The one fuel cycle machine that handles hot, radioactive material, so it keeps
hazard stripes on its skid. Real basis: fluoride volatility reprocessing, which turns uranium into a gas to separate it.

Run from the repo root:
    python art/tools/reprocessor_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/reprocessor/'


def vessel():
    """The dissolver vessel: light steel plate with a welded seam every four pixels of height."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    for y in (3, 7, 11, 15):
        t.rect(0, y, 15, y, 'I')
    t.rect(0, 0, 15, 0, 'E')
    return t


def column():
    """The separation column's lead lining: dark gunmetal, a shade deeper than the family's."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'z')
    t.rect(0, 0, 15, 0, 'u')
    t.rect(0, 15, 15, 15, 'T')
    return t


def sight(lit):
    """A sight glass, 2 x 10: dark glass idle; working, the solution inside glows teal, with
    bubbles rising through it."""
    t = Tex()
    t.rect(0, 0, 1, 9, 'T')
    if lit:
        t.rect(0, 0, 1, 9, 't')
        for x, y in ((0, 2), (1, 5), (0, 7), (1, 1)):
            t.set(x, y, 'C')
    else:
        t.rect(0, 0, 0, 9, 'b')
    return t


def label():
    """The column's label plate, 6 x 3: white, with a line of black "text", in a grey frame. With
    the hazard band above it, it marks the column the way process plants mark their vessels, by
    shape and colour rather than a pictogram too small to read."""
    t = Tex()
    t.rect(0, 0, 5, 2, 'A')
    t.rect(0, 0, 0, 2, 'I')
    t.rect(5, 0, 5, 2, 'I')
    t.rect(1, 1, 4, 1, 'B')
    return t


TEXTURES = {
    'vessel': vessel,
    'column': column,
    'sight': lambda: sight(False),
    'sight_on': lambda: sight(True),
    'label': label,
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        # Lit textures stay crisp; the rest get the surface finish (pixelart.finish).
        (publish if name.endswith('_on') else publish_finished)(FOLDER + name, fn())


if __name__ == '__main__':
    main()
