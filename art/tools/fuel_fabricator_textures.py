"""The Fuel Fabricator's own textures, for its model (fuelFabricatorModel in datagen).

A pellet press and rod loading station, as fuel plants use: a hydraulic press on two uprights
presses pellets on the bed, and finished fuel rods lie on a loading tray at the front. The cabinet,
frame, ram and caps use the machine family's textures (machine_family.py).

Run from the repo root:
    python art/tools/fuel_fabricator_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/fuel_fabricator/'


def cylinder():
    """The hydraulic cylinder on the crosshead: industrial orange paint, lit on top, with a dark
    band where the gland seal sits."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'X')
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 15, 15, 15, 'Z')
    t.rect(0, 12, 15, 12, 'T')
    return t


def rod():
    """A finished fuel rod: bright steel cladding, lit along its top, with a dark weld ring every
    four pixels where the pellets stack."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 15, 15, 15, 'I')
    for x in (3, 7, 11, 15):
        t.rect(x, 0, x, 15, 'I')
    return t


def bed():
    """The press bed, seen from above: gunmetal with the pellet die in the middle, a bright ring
    round a dark bore."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    t.rect(6, 6, 9, 9, 'F')
    t.rect(7, 7, 8, 8, 'T')
    return t


TEXTURES = {
    'cylinder': cylinder,
    'rod': rod,
    'bed': bed,
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        # Lit textures stay crisp; the rest get the surface finish (pixelart.finish).
        (publish if name.endswith('_on') else publish_finished)(FOLDER + name, fn())


if __name__ == '__main__':
    main()
