"""The Electric Alloy Smelter's own textures, for its model (electricSmelterModel in datagen).

It is an induction furnace, as real ones are built: a crucible wrapped in a copper coil at the
front, and the power cabinet that drives the coil behind it, fed by copper busbars. The skid,
cabinet, lid, screen, vents, gunmetal and copper are the machine family's (machine_family.py); this
draws the coil and the crucible's mouth.

Run from the repo root:
    python art/tools/electric_smelter_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/electric_smelter/'


def coil(lit):
    """The induction coil: turns of round copper tube, one every three pixels: a bright highlight
    where the light catches the tube, the tube itself, then a dark gap to the next turn. Lit, the gaps
    glow faintly with the heat of the crucible inside."""
    t = Tex()
    for y in range(16):
        row = y % 3
        k = 'e' if row == 0 else 'R' if row == 1 else ('Z' if lit else 'T')
        t.rect(0, y, 15, y, k)
    return t


def mouth(lit):
    """The crucible's mouth, seen from above. The top face maps by position and the crucible spans
    x 4 to 12 and z 2.5 to 7.5, so the charge is drawn there, inside a one-pixel gunmetal lip.
    Idle, cold dark metal; running, molten metal glowing from orange at the edge to white-hot."""
    t = Tex()
    t.rect(0, 0, 15, 15, 's')
    if lit:
        t.rect(5, 3, 10, 6, 'X')
        t.rect(6, 3, 9, 6, 'a')
        t.rect(7, 4, 8, 5, 'W')
    else:
        t.rect(5, 3, 10, 6, 'U')
        t.rect(6, 4, 9, 5, 'T')
    return t


TEXTURES = {
    'coil': lambda: coil(False),
    'coil_on': lambda: coil(True),
    'mouth': lambda: mouth(False),
    'mouth_on': lambda: mouth(True),
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        # Lit textures stay crisp; the rest get the surface finish (pixelart.finish).
        (publish if name.endswith('_on') else publish_finished)(FOLDER + name, fn())


if __name__ == '__main__':
    main()
