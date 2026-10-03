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
Run from the repo root:
    python art/tools/drill_textures.py
"""
from pixelart import *  # noqa: F401,F403

FOLDER = 'block/drill/'


def casing():
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 15, 1, 'A')
    t.rect(0, 2, 0, 14, 'A')
    t.rect(15, 2, 15, 14, 'J')
    t.rect(0, 15, 15, 15, 'I')
    t.rect(1, 14, 14, 14, 'F')
    # Rivets in the corners.
    for x, y in ((2, 3), (13, 3), (2, 12), (13, 12)):
        t.set(x, y, 'I')
        t.set(x - 1, y - 1, 'A')
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
    'skirt': skirt,
    'ring': ring,
    'warning': warning,
}


def main():
    for name, fn in TEXTURES.items():
        publish_finished(FOLDER + name, fn())


if __name__ == '__main__':
    main()
