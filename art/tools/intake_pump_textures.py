"""The Intake Pump's own textures, for its model (intakePumpModel in datagen).

A vertical pump set, as on a real intake: a gunmetal pump casing (the volute) low down, an electric
motor standing on it painted water-blue with cooling fins, a fan cover on top, and a copper
discharge nozzle with a flange. Skid, casing and copper use the machine family's textures
(machine_family.py).

Run from the repo root:
    python art/tools/intake_pump_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/intake_pump/'


def motor():
    """The motor housing: blue paint with vertical cooling fins every two pixels, lit on the fin
    edge and dark in the groove."""
    t = Tex()
    for x in range(16):
        t.rect(x, 0, x, 15, 'V' if x % 2 == 0 else 'w')
    t.rect(0, 0, 15, 0, 'c')
    t.rect(0, 15, 15, 15, 'g')
    return t


def fan_cover():
    """The motor's fan cover, seen from above: a grille of dark slots in blue."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'v')
    for y in range(5, 11, 2):
        t.rect(5, y, 10, y, 'g')
    return t


TEXTURES = {
    'motor': motor,
    'fan_cover': fan_cover,
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        # Lit textures stay crisp; the rest get the surface finish (pixelart.finish).
        (publish if name.endswith('_on') else publish_finished)(FOLDER + name, fn())


if __name__ == '__main__':
    main()
