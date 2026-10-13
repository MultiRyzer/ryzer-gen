"""The Electrorefiner's own textures, for its model (electrorefinerBoxes in datagen) and the parts
and glows its renderer draws (MachineMotion).

Pyroprocessing, as Argonne ran it for EBR-II's fuel: a gunmetal pot of molten salt at some 500
degrees, sunk in a heated well, four baskets of chopped fuel turning in the salt as the anode and a
steel cathode that the uranium plates onto, fed by a rectifier through copper bus bars. A sight
glass in the pot's front looks onto the salt: a pale frozen crust while the cell is cold, molten
and glowing while it is hot (drawn over the crust by the renderer, dull red while it waits, bright
while it works). Cabinet, panels, screen, vent, metal, bright steel, copper, accent and the hazard
skid use the machine family's textures (machine_family.py).

Surfaces, and where each maps:
    cell            pot walls, by position (rows 3 to 8: y 13 down to the well)
    lining          the pot's inside walls, seen through the glass, by position
    salt            the frozen salt's top, by position (x and z 2.75 to 13.25 by 10.25)
    salt_molten     the molten salt the renderer draws over it, animated, lit
    heater          the heater ring round the pot's foot, drawn by the renderer, lit
    well            the dark well the pot sits in
    glass           the sight glass, 8 x 3, cut out: clear but for two glints
    lid_plate       the pot's lid, 13 x 10, bolted round its edge
    port            the drive head's top, 6 x 6: the charging port, where a pipe meets it
    rectifier_top   the rectifier's top, 13 x 4, with its vent slots
    label           the label plate on the pot's west flank, 4 x 2
    basket          the anode baskets' perforated steel, by position
    dendrite        the uranium plated on the cathode, by position

Run from the repo root:
    python art/tools/electrorefiner_textures.py
"""
import math
import os

from pixelart import *  # noqa: F401,F403
from pixelart import ART_TEXTURES, MOD_TEXTURES
import machine_family

FOLDER = 'block/machine/electrorefiner/'

# Molten salt, dark to white hot.
SALT_DEEP = 'b4400e'
SALT_BODY = 'ff8a1e'
SALT_HOT = 'ffb444'
SALT_WHITE = 'ffe6a6'
# Frozen salt: a pale crust.
CRUST = 'c9ced2'
CRUST_LIT = 'e0e4e7'
CRUST_DARK = 'a3aab0'


def put(t, x, y, colour):
    """Sets a pixel to a colour outside the palette (hex, no #)."""
    t.px[y][x] = colour


def noise(x, y, seed):
    """A steady pseudo-random number in [0, 1) for a pixel."""
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65536


def cell():
    """The pot's wall: mid gunmetal, a dark line under the lid's lip, a lit top course, and a
    shaded foot where it sinks into the well."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    t.rect(0, 3, 15, 3, 'z')
    t.rect(0, 4, 15, 4, 's')
    t.rect(0, 8, 15, 8, 'z')
    return t


def lining():
    """The pot's inside: dark refractory brick in courses of two, joints staggered, so the salt and
    the baskets stand out against it through the glass."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'T')
    for y in range(0, 16, 2):
        t.rect(0, y, 15, y, 'U')
        offset = 0 if (y // 2) % 2 == 0 else 2
        for x in range(offset, 16, 4):
            t.set(x, y + 1, 'U')
    return t


def salt():
    """Frozen salt, a pale crust: cracks and glinting crystals over an even white-grey."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            n = noise(x, y, 7)
            put(t, x, y, CRUST_LIT if n > 0.86 else CRUST_DARK if n < 0.12 else CRUST)
    return t


def molten_frame(i, count):
    """One frame of the molten salt: slow convection, bright upwellings drifting across darker
    lanes where the surface cools. Each wave turns a whole number of times over the loop."""
    t = Tex()
    a = 2 * math.pi * i / count
    for y in range(16):
        for x in range(16):
            v = (0.5 + 0.22 * math.sin(0.8 * x + 0.5 * y + a)
                 + 0.18 * math.sin(-0.6 * x + 0.9 * y + 2 * a + 1.3)
                 + 0.1 * math.sin(1.7 * x - 1.1 * y - a + 0.4))
            put(t, x, y, SALT_DEEP if v < 0.3 else SALT_BODY if v < 0.62 else SALT_HOT if v < 0.84 else SALT_WHITE)
    return t


def heater():
    """The heater glowing in the well round the pot's foot: orange, a few hot specks."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            put(t, x, y, SALT_HOT if noise(x, y, 3) > 0.8 else SALT_BODY)
    return t


def well():
    """The heated well round the pot: dark, the heater hidden under it until it glows."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'U')
    return t


def glass():
    """The sight glass, 8 x 3: clear (cut out), but for two short glints across it."""
    t = Tex()
    t.set(2, 0, 'A')
    t.set(1, 1, 'E')
    t.set(6, 1, 'A')
    t.set(5, 2, 'E')
    return t


def lid_plate():
    """The pot's lid, 13 x 10 (x along, z down from the front): gunmetal, lit along its front and
    west edges, shaded on the others, bolted round its edge."""
    t = Tex()
    t.rect(0, 0, 12, 9, 'u')
    t.rect(0, 0, 12, 0, 's')
    t.rect(0, 0, 0, 9, 's')
    t.rect(12, 1, 12, 9, 'z')
    t.rect(1, 9, 12, 9, 'z')
    for x, y in ((1, 1), (4, 1), (8, 1), (11, 1), (1, 8), (4, 8), (8, 8), (11, 8), (1, 3), (1, 6), (11, 3), (11, 6)):
        t.set(x, y, 'h')
    return t


def port():
    """The drive head's top, 6 x 6: the charging port, the family connector's flange and socket
    at the drive head's size, lit top and left."""
    t = Tex()
    t.rect(0, 0, 5, 5, 'S')
    t.rect(0, 0, 5, 0, 'b')
    t.rect(0, 0, 0, 5, 'b')
    t.rect(5, 1, 5, 5, 'U')
    t.rect(1, 5, 5, 5, 'U')
    t.rect(1, 1, 4, 4, 'T')
    t.rect(2, 2, 3, 3, 'U')
    t.rect(1, 1, 4, 1, 'k')
    t.rect(1, 1, 1, 4, 'k')
    return t


def rectifier_top():
    """The rectifier's top, 13 x 4 (its front edge under the lid's back): light casing with a lit
    front and west edge, shaded east and back, and four vent slots in the middle."""
    t = Tex()
    t.rect(0, 0, 12, 3, 'E')
    t.rect(0, 0, 12, 0, 'A')
    t.rect(0, 0, 0, 3, 'A')
    t.rect(12, 1, 12, 3, 'J')
    t.rect(1, 3, 12, 3, 'J')
    for x in (4, 6, 8):
        t.rect(x, 1, x, 2, 'J')
        t.set(x + 1, 2, 'F')
    return t


def label():
    """The label plate, 4 x 2: white, a line of black "text", in a grey frame. With the hazard
    skid it marks the cell the way process plants mark their vessels."""
    t = Tex()
    t.rect(0, 0, 3, 1, 'A')
    t.rect(0, 0, 0, 1, 'I')
    t.rect(3, 0, 3, 1, 'I')
    t.rect(1, 1, 2, 1, 'B')
    return t


def basket():
    """An anode basket's perforated steel, dark against the pale frozen crust: mid steel bars
    between dark slots, a lit rim along its top course (rows 5 to 7 are the part above the salt)."""
    t = Tex()
    for x in range(16):
        t.rect(x, 0, x, 15, 'L' if x % 2 == 0 else 'D')
    t.rect(0, 5, 15, 5, 'h')
    return t


def dendrite():
    """Uranium plated on the cathode: a rough crystalline grey, glinting, darker than the crust."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            n = noise(x, y, 11)
            t.set(x, y, 'h' if n > 0.85 else 'L' if n > 0.55 else 'l' if n > 0.2 else 'm')
    return t


# Surfaces with the surface finish (pixelart.finish); the rest are lit or cut out, kept crisp.
FINISHED = {
    'cell': cell,
    'lining': lining,
    'well': well,
    'lid_plate': lid_plate,
    'rectifier_top': rectifier_top,
    'basket': basket,
}
CRISP = {
    'salt': salt,
    'heater': heater,
    'glass': glass,
    'port': port,
    'label': label,
    'dendrite': dendrite,
}
# Retired: the old sight window, a lid that shadowed the family's name, and the readout and lamp,
# now the family's screen and lamp.
RETIRED = ('window', 'window_on', 'lid', 'readout', 'readout_on', 'lamp')


def main():
    machine_family.main()
    for name, fn in FINISHED.items():
        publish_finished(FOLDER + name, fn())
    for name, fn in CRISP.items():
        publish(FOLDER + name, fn())
    publish_frames(FOLDER + 'salt_molten', [molten_frame(i, 16) for i in range(16)], 3, True)
    for name in RETIRED:
        for root in (ART_TEXTURES, MOD_TEXTURES):
            for path in (os.path.join(root, FOLDER + name + '.png'), os.path.join(root, FOLDER + name + '.png.mcmeta')):
                if os.path.exists(path):
                    os.remove(path)


if __name__ == '__main__':
    main()
