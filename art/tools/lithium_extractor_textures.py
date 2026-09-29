"""The Lithium Extractor's own textures, for its model (lithiumExtractorModel in datagen).

Direct lithium extraction from brine, as real plants do it: a brine basin with white salt crusting
its edge, and two ion-exchange columns standing in it, whose sorbent beds pull the lithium out, with
a copper header across their tops and a small control box. Cabinet, screen and pipes use the machine
family's textures (machine_family.py).

Run from the repo root:
    python art/tools/lithium_extractor_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/lithium_extractor/'


def brine():
    """The basin's brine, seen from above: blue water with white salt crust round the edge and a
    few crystals floating. The basin spans the block inside its one-pixel rim."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    t.rect(2, 2, 13, 13, 'v')
    t.rect(2, 2, 13, 2, 'A')
    t.rect(2, 2, 2, 13, 'A')
    t.rect(13, 3, 13, 13, 'E')
    t.rect(3, 13, 13, 13, 'E')
    for x, y in ((5, 5), (10, 4), (4, 10), (11, 10), (8, 11)):
        t.set(x, y, 'A')
    t.rect(6, 7, 9, 8, 'G')
    return t


def basin():
    """The basin's walls: light steel, lit along the top, with a white salt line at the brim."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 15, 15, 15, 'I')
    return t


def column(lit):
    """An ion-exchange column: frosted white shell with a band every five pixels, and down its
    middle a sight strip onto the sorbent bed, dim idle and glowing cyan as the brine passes."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'H')
    for y in (4, 9, 14):
        t.rect(0, y, 15, y, 'J')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(7, 1, 8, 13, 'j' if lit else 'I')
    if lit:
        t.set(7, 6, 'W')
        t.set(8, 11, 'W')
    return t


TEXTURES = {
    'brine': brine,
    'basin': basin,
    'column': lambda: column(False),
    'column_on': lambda: column(True),
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        # Lit textures stay crisp; the rest get the surface finish (pixelart.finish).
        (publish if name.endswith('_on') else publish_finished)(FOLDER + name, fn())


if __name__ == '__main__':
    main()
