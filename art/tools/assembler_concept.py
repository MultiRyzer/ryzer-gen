"""The Assembler, formed (design section 14): an open robot cell, 3 x 3 x 3, facing north.

A machine bay below, its front carrying the controller's console and a light strip, its ends the
ports: energy and items in on the east (your left as you face it), items out on the west. On its
worktop the open cell: four safety-yellow corner posts under a graphite top rim, an orange
six-axis arm on a turntable at the back, the 3 x 3 fixture bed in front of it (the crafting grid),
an in-feed belt down the east side and an out-feed belt down the west, the pattern rack (nine
recipe cartridges) standing at the back, and a stack light in the back east corner.

The design is one list of boxes in pixels (x 0 to 48 west to east, y up, z 0 to 48 front to back),
in two states, idle and working (light strip lit, belts running). The arm is six parts of its own,
each built whole about its joint, which the controller's renderer poses (AssemblerRenderer): the
turret turns, the upper arm and forearm pitch, and the tool hangs plumb from the wrist. The items,
the lamps' colours and the cartridges' icons are drawn live too. This script publishes the
textures (block/assembler/), exports the design for datagen (art/designs/assembler.json) and
renders a preview.

Positions the renderer and the logic use (AssemblerLayout in the mod) must match the constants
here: the bed's fixtures, the belts, the arm's joints and part origins, the rack's cartridges and
the stack light.

Run from the repo root:
    python art/tools/assembler_concept.py [OUT.png] [--scale N] [--state idle|working]
"""
import sys

import zlib

from pixelart import Tex, write_png, publish_frames, finish
import model_preview as mp
import pool_concept as pc
from pool_concept import Box, rows

S = 48
ROWS = [(2, 15)]
BACK = 45
FOLDER = 'block/assembler/'

# The fixture bed: 16 x 16, a pixel of border, three 4-pixel fixtures a pixel apart. Fixture
# centres (x from the west, z from the front; row 0 of the crafting grid is the back row).
BED = (16, 4, 32, 20)
BED_TOP = 18
FIX_X = (19, 24, 29)
FIX_Z = (17, 12, 7)
# The belts: their x span and z span, and their top.
IN_BELT = (35, 43)
OUT_BELT = (5, 13)
BELT_Z = (6, 40)
BELT_TOP = 19.25
# The back of the hood over each belt's front end: items show on a belt only behind it.
HOOD_BACK = 10.5
# The back wall (its z span) and its two rows of plates, with what each outside plate carries.
WALL = (45.5, 47.5)
WALL_ROWS = ((17, 31), (33, 45))
WALL_PLATES = {(17, 31): ('vent', 'hatch', 'vent'), (33, 45): ('plate', 'label', 'plate')}
# The arm: the turret's axis, the shoulder and elbow heights, the upper arm and forearm lengths,
# and how far the grip sits below the wrist. Each part's model origin (in pixels) is listed with it.
PIVOT = (24, 31)
TURRET_Y = 18.5
SHOULDER_Y = 24
UPPER = 15
FORE = 18
GRIP = 12.5
# The pattern rack: its front face (z), its span, and its 3 x 3 cartridges' centres.
RACK = (9, 19, 39, 43)
RACK_FACE = BACK - 2
CART_X = (14, 24, 34)
CART_Y = (39, 31, 23)
# The stack light's lamps, bottom to top: red, amber, green.
STACK = (41.75, 39.75, 44.25, 42.25)
STACK_Y = (34, 36.5, 39)


# ---------------------------------------------------------------- textures

def bed(w, h):
    """The fixture bed, 16 x 16: a 3 x 3 grid of fixtures, as the crafting grid is, each a graphite
    pocket round a steel pad with a locating pin; the middle pin a cyan lamp (lit by the renderer)."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'T')
    t.rect(0, 0, w - 1, 0, 'b')
    t.rect(0, 0, 0, h - 1, 'b')
    for cx in range(3):
        for cy in range(3):
            x0, y0 = 1 + cx * 5, 1 + cy * 5
            t.rect(x0, y0, x0 + 3, y0 + 3, 'S')
            t.rect(x0 + 1, y0 + 1, x0 + 2, y0 + 2, 'I')
            t.rect(x0, y0, x0 + 3, y0, 'b')
            t.rect(x0 + 3, y0 + 1, x0 + 3, y0 + 3, 'U')
    return rows(t)


def belt(frame, forward):
    """A conveyor belt from above, 16 x 16, tiling along its length: dark rubber with a cleat every
    four pixels, steel edges. Frames 0 to 3 move the cleats a pixel each, forward along +z (down
    the texture) or back."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'U')
    shift = frame if forward else -frame
    for y in range(16):
        if (y - shift) % 4 == 0:
            t.rect(1, y, 14, y, 'S')
    t.rect(0, 0, 0, 15, 'I')
    t.rect(15, 0, 15, 15, 'J')
    return t


def patterns(w, h):
    """The pattern rack's face, 30 x 24: a graphite cabinet of nine cartridges in a 3 x 3, each a
    light cartridge with a dark window (where the renderer draws its recipe's result) and a lamp."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'S')
    t.rect(0, 0, w - 1, 0, 'b')
    t.rect(0, h - 1, w - 1, h - 1, 'U')
    for i in range(9):
        cx, cy = i % 3, i // 3
        x0, y0 = 1 + cx * 10, 1 + cy * 8
        t.rect(x0, y0, x0 + 7, y0 + 6, 'E')
        t.rect(x0, y0, x0 + 7, y0, 'A')
        t.rect(x0 + 7, y0 + 1, x0 + 7, y0 + 6, 'J')
        t.rect(x0 + 1, y0 + 6, x0 + 6, y0 + 6, 'I')
        t.rect(x0 + 2, y0 + 1, x0 + 5, y0 + 4, 'U')
        t.set(x0 + 6, y0 + 5, 'f')
    return rows(t)


def console(w, h):
    """The controller's console, 10 x 9: a graphite bezel round a readout (a 3 x 3 grid, an arrow
    and a result, a progress line)."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'S')
    t.rect(0, 0, w - 1, 0, 'b')
    t.rect(0, h - 1, w - 1, h - 1, 'T')
    t.rect(1, 1, w - 2, h - 3, 'U')
    for x in (2, 3, 4):
        for y in (2, 3, 4):
            t.set(x, y, 'f')
    t.set(3, 3, 'i')
    t.set(6, 3, 'f')
    t.set(7, 3, 'j')
    t.rect(2, h - 4, w - 3, h - 4, 'f')
    t.rect(2, h - 4, w // 2, h - 4, 'i')
    return rows(t)


def flat(k, edge=None):
    t = Tex()
    t.rect(0, 0, 15, 15, k)
    if edge:
        t.rect(0, 0, 15, 0, edge)
    return rows(t)


def fin(name, t, strength=1.0):
    """A texture with the house surface finish on its fills (pixelart.finish), seeded by its name."""
    return finish(t, zlib.crc32(name.encode()) & 0xFFFF, strength)


# The machine bay's plates. Each block face is one plate drawn at its size (14 or 12 across, 13 high):
# a light casing panel with a softened outline, a lit bevel and a screw in each corner, and on some a
# feature drawn into it, so the bay reads as a machine's cabinet, not a wall of the same tile.

def plate(w, h):
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'E')
    t.rect(0, 0, w - 1, 0, 'I')
    t.rect(0, 0, 0, h - 1, 'I')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    t.rect(1, 1, w - 2, 1, 'A')
    t.rect(1, 1, 1, h - 2, 'A')
    t.rect(w - 2, 2, w - 2, h - 2, 'F')
    t.rect(2, h - 2, w - 2, h - 2, 'F')
    for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
        t.set(x, y, 'J')
    return t


def plate_vent(w, h):
    """A plate with a louvred grille across its middle: dark slots under lit lips, the motors' air."""
    t = plate(w, h)
    x1, x2 = 4, w - 5
    for y in range(4, h - 4, 2):
        t.rect(x1, y, x2, y, 'T')
        t.rect(x1, y + 1, x2, y + 1, 'I')
    t.rect(x1 - 1, 3, x2 + 1, 3, 'J')
    t.rect(x1 - 1, 3, x1 - 1, h - 4, 'J')
    return t


def plate_hatch(w, h):
    """An access hatch: a door inset in the plate, hinged on the left, an orange latch on the right."""
    t = plate(w, h)
    x1, y1, x2, y2 = 3, 3, w - 4, h - 4
    t.rect(x1, y1, x2, y1, 'J')
    t.rect(x1, y1, x1, y2, 'J')
    t.rect(x1 + 1, y2, x2, y2, 'A')
    t.rect(x2, y1 + 1, x2, y2, 'A')
    for y in (y1 + 1, y2 - 2):
        t.rect(x1, y, x1, y + 1, 'S')
    mid = (y1 + y2) // 2
    t.rect(x2 - 2, mid - 1, x2 - 2, mid + 1, 'X')
    t.set(x2 - 2, mid + 1, 'Z')
    return t


def plate_label(w, h):
    """The maker's plate: a graphite label on the plate, a cyan line across its top and two lines of
    lettering (too small to read, as on a real machine seen from a step away), over a small grille."""
    t = plate(w, h)
    x1, x2 = 3, w - 4
    t.rect(x1, 3, x2, 7, 'S')
    t.rect(x1, 3, x2, 3, 'b')
    t.rect(x1 + 1, 4, x2 - 1, 4, 'f')
    for y, end in ((5, x2 - 2), (6, x2 - 4)):
        for x in range(x1 + 1, end + 1, 2):
            t.set(x, y, 'I')
    for y in (9, 10):
        t.rect(x1, y, x2, y, 'T' if y % 2 else 'J')
    return t


def plate_estop(w, h):
    """The emergency stop's plate: a graphite backing, ringed in yellow, where the stop's box stands,
    so the yellow box and its red button read against it."""
    t = plate(w, h)
    cx = w // 2
    t.rect(cx - 4, 3, cx + 3, h - 4, 'Y')
    t.rect(cx - 3, 4, cx + 2, h - 5, 'S')
    t.rect(cx - 3, 4, cx + 2, 4, 'T')
    return t


def hood_mouth(w, h):
    """A belt's hood seen from inside the cell, 9 x 6: a yellow and black header over the mouth the
    belt runs into, rubber strip flaps hanging across it, and the graphite housing round it."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'S')
    for x in range(w):
        t.set(x, 0, 'Y' if x % 4 < 2 else 'B')
    for x in range(1, w - 1):
        t.rect(x, 1, x, 3, 'U' if x % 2 else 'T')
    t.rect(0, h - 1, w - 1, h - 1, 'T')
    return t


def hood_front(w, h):
    """The hood's front, 9 x 6: a graphite housing, lit along its top, a yellow and black band and a
    bolt either side."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'S')
    t.rect(0, 0, w - 1, 0, 'b')
    for x in range(w):
        t.set(x, 2, 'Y' if x % 4 < 2 else 'B')
    t.set(1, 4, 'h')
    t.set(w - 2, 4, 'h')
    t.rect(0, h - 1, w - 1, h - 1, 'T')
    return t


def hood_side(w, h):
    """The hood's flank, 5 x 6: graphite, lit along its top, a bolt in the middle."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'S')
    t.rect(0, 0, w - 1, 0, 'b')
    t.set(w // 2, 3, 'h')
    t.rect(0, h - 1, w - 1, h - 1, 'T')
    return t


def casing():
    """The bay's casing round its plates, light, lit along the top of each block."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    t.rect(0, 0, 15, 0, 'E')
    t.rect(0, 15, 15, 15, 'I')
    return t


def table():
    """The worktop from above, 16 x 16 tiling: a machine table in gunmetal with T-slots every eight
    pixels along its length (front to back), each a dark slot with a lit lip, as robot cells bolt their
    fixtures down."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    for x in (3, 11):
        t.rect(x, 0, x, 15, 'U')
        t.rect(x - 1, 0, x - 1, 15, 's')
        t.rect(x + 1, 0, x + 1, 15, 'z')
    return t


def tape():
    """Floor marking, yellow and black, two pixels each along any line: the arm's working area."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            t.set(x, y, 'Y' if (x + y) % 4 < 2 else 'B')
    return t


def post(w, h):
    """A safety post's face, 2 x 28: safety yellow, a black and yellow band at its foot, and a dark
    splice where it crosses the block join."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'Y')
    for i, y in enumerate(range(h - 8, h, 2)):
        t.rect(0, y, w - 1, y + 1, 'B' if i % 2 == 0 else 'Y')
    t.rect(0, 12, w - 1, 12, 'y')
    t.rect(0, 15, w - 1, 15, 'y')
    t.rect(0, 0, w - 1, 0, 'y')
    return t


def curtain(w, h, lit):
    """A light curtain's emitter strip, 1 x 26: a dark lens with a red emitter every three pixels,
    glowing while the cell works. Real basis: open robot cells are guarded by light curtains, which
    stop the arm when anything crosses them."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'U')
    for y in range(1, h - 1, 3):
        t.rect(0, y, w - 1, y, 'x' if lit else '8')
    t.rect(0, 0, w - 1, 0, 'T')
    t.rect(0, h - 1, w - 1, h - 1, 'T')
    return t


def lens(key, dark):
    """A stack light's lens, ribbed: the colour in columns, a darker rib every other pixel (the
    renderer lays the lit colour over the one that shows the cell's state)."""
    t = Tex()
    for x in range(16):
        t.rect(x, 0, x, 15, key if x % 2 == 0 else dark)
    return t


def conveyor():
    """A conveyor's side rail, 16 x 16 tiling, drawn in its foot rows (the rail is 2.5 high): graphite,
    lit along the top, with a bolt every four pixels."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 13, 15, 13, 'b')
    for x in range(1, 16, 4):
        t.set(x, 14, 'h')
    t.rect(0, 15, 15, 15, 'T')
    return t


def arm_tile():
    """The arm's orange casing, 16 x 16 tiling: robot orange, lit along the top of each block, with a
    soft seam a quarter of the way down."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'X')
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 4, 15, 4, 'Z')
    t.rect(0, 15, 15, 15, 'Z')
    return t


def rack_back(w, h):
    """The pattern rack's back, 30 x 24: a casing plate with two grilles (its electronics' air) and a
    graphite label between them."""
    t = plate(w, h)
    for gx in (4, w - 11):
        for y in range(5, h - 5, 2):
            t.rect(gx, y, gx + 6, y, 'T')
            t.rect(gx, y + 1, gx + 6, y + 1, 'I')
    cx = w // 2
    t.rect(cx - 3, 6, cx + 2, 9, 'S')
    t.rect(cx - 3, 6, cx + 2, 6, 'b')
    t.rect(cx - 2, 7, cx + 1, 7, 'f')
    return t


def estop_cap():
    """The emergency stop's mushroom: red, lit on top."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'x')
    t.rect(0, 0, 15, 0, 'N')
    t.rect(0, 15, 15, 15, '8')
    return t


MADE = {'plate': plate, 'vent': plate_vent, 'hatch': plate_hatch, 'label': plate_label, 'estop': plate_estop,
        'post': post, 'curtain': lambda w, h: curtain(w, h, False), 'curtainon': lambda w, h: curtain(w, h, True),
        'rackback': rack_back, 'hoodmouth': hood_mouth, 'hoodfront': hood_front,
        'hoodside': hood_side}


def textures():
    tex = pc.textures()
    tex.update({
        'belt': rows(belt(0, True)), 'belt_in': rows(belt(0, True)), 'belt_out': rows(belt(0, False)),
        'bed': bed(16, 16), 'patterns': patterns(30, 24), 'console': console(10, 9),
        'lamp_red': rows(lens('8', 'o')), 'lamp_amber': rows(lens('Z', 'O')), 'lamp_green': rows(lens('5', 'k')),
        'port_energy': mp.texture('block/microreactor/port_energy'),
        'port_items': mp.texture('block/microreactor/port_fuel'),
        'bay': fin('bay', casing()), 'table': fin('table', table()), 'tape': rows(tape()),
        'conveyor': fin('conveyor', conveyor()), 'arm': fin('arm', arm_tile()), 'estop_cap': rows(estop_cap()),
    })
    base = type(tex).__missing__

    def missing(self, name):
        kind, _, size = name.rpartition('_')
        if kind in MADE:
            w, h = (int(v) for v in size.split('x'))
            made = MADE[kind](w, h)
            # Lit strips stay crisp; the rest get the surface finish.
            self[name] = rows(made) if kind in ('curtain', 'curtainon', 'post') else fin(name, made)
            return self[name]
        return base(self, name)

    type(tex).__missing__ = missing
    return tex


# ---------------------------------------------------------------- the design

# The machine bay's plates, by face, in the order pc.bays gives them (west to east along the front and
# back, front to back along the ends): the maker's plate, the controller's (its console stands on it)
# and the emergency stop's along the front; vents either side of a hatch along the back; on the ends
# plain plates behind the ports, a vent at the back, and a hatch at the west end's front.
PLATES = {
    'north': ('label', 'plate', 'estop'),
    'south': ('vent', 'hatch', 'vent'),
    'east': ('plate', 'plate', 'vent'),
    'west': ('hatch', 'plate', 'vent'),
}

ARM_PARTS = {
    # part: (model origin, boxes about the design facing north at rest)
    'turret': (PIVOT[0], TURRET_Y, PIVOT[1]),
    'upper': (PIVOT[0], SHOULDER_Y, PIVOT[1]),
    'fore': (PIVOT[0], SHOULDER_Y + UPPER, PIVOT[1] - 16),
    'tool': (PIVOT[0], SHOULDER_Y + UPPER, PIVOT[1] - FORE),
    'finger_west': (PIVOT[0], SHOULDER_Y + UPPER, PIVOT[1] - FORE),
    'finger_east': (PIVOT[0], SHOULDER_Y + UPPER, PIVOT[1] - FORE),
}


def arm(part):
    """One of the arm's parts at rest: upright, the forearm reaching forward over the bed, the
    tool hanging from the wrist."""
    boxes = []

    def add(m, *c):
        b = Box(m, *c)
        boxes.append(b)
        return b

    px, pz = PIVOT
    elbow = SHOULDER_Y + UPPER
    wz = pz - FORE
    if part == 'turret':
        add('arm', px - 5, TURRET_Y, pz - 5, px + 5, TURRET_Y + 3, pz + 5)
        add('trim', px - 3.5, TURRET_Y + 3, pz - 3.5, px + 3.5, SHOULDER_Y + 2, pz + 3.5)
        add('metal', px + 3.5, SHOULDER_Y - 2, pz - 2, px + 5.5, SHOULDER_Y + 1.5, pz + 2)
    elif part == 'upper':
        add('arm', px - 2, SHOULDER_Y + 2, pz - 2, px + 2, elbow - 1, pz + 2)
        add('copper', px + 2, SHOULDER_Y + 2, pz - 1, px + 2.5, elbow - 2, pz + 1)
    elif part == 'fore':
        add('trim', px - 3, elbow - 2.5, pz - 3, px + 3, elbow + 2.5, pz + 3)
        add('metal', px + 3, elbow - 1.5, pz - 1.5, px + 4.5, elbow + 1.5, pz + 1.5)
        add('arm', px - 2, elbow - 1.5, wz + 2.5, px + 2, elbow + 1.5, pz - 3)
    elif part == 'tool':
        add('trim', px - 2.5, elbow - 4, wz - 2.5, px + 2.5, elbow + 2, wz + 2.5)
        add('bright', px - 1.5, elbow - 7, wz - 1.5, px + 1.5, elbow - 4, wz + 1.5)
        add('trim', px - 3, elbow - 10, wz - 2, px + 3, elbow - 7, wz + 2)
    elif part == 'finger_west':
        add('bright', px - 3, elbow - 14, wz - 1, px - 2, elbow - 10, wz + 1)
    elif part == 'finger_east':
        add('bright', px + 2, elbow - 14, wz - 1, px + 3, elbow - 10, wz + 1)
    return boxes


def design(state='working'):
    working = state == 'working'
    boxes = []

    def add(m, *c):
        b = Box(m, *c)
        boxes.append(b)
        return b

    def size(a, b, row):
        return f'{round(b - a)}x{row[1] - row[0]}'

    add('skid', 0, 0, 0, S, 2, S)
    # The machine bay, solid, and the worktop over it.
    add('trim', 3, 2, 3, S - 3, 15, S - 3)
    add('metal', 2, 15, 2, S - 2, 16.5, S - 2).face('up', 'table')
    # Floor marking round the cell's inside edge, yellow and black: the arm's reach.
    for tx1, tz1, tx2, tz2 in ((3, 3, S - 3, 4), (3, S - 4, S - 3, S - 3), (3, 4, 4, S - 4), (S - 4, 4, S - 3, S - 4)):
        add('tape', tx1, 16.5, tz1, tx2, 16.6, tz2)
    # Its faces: a plate per block face, set in behind the frame, each drawn for its place (PLATES).
    row = ROWS[0]
    for i, (a, b) in enumerate(pc.bays(0, S)):
        add('bay', a, row[0], 1.5, b, row[1], 3).decal('north', PLATES['north'][i] + '_' + size(a, b, row))
        add('bay', a, row[0], BACK, b, row[1], S - 1.5).decal('south', PLATES['south'][i] + '_' + size(a, b, row))
    for x1, x2, side in ((1.5, 3, 'west'), (S - 3, S - 1.5, 'east')):
        for i, (a, b) in enumerate(pc.bays(0, S)):
            a, b = max(a, 3), min(b, BACK)
            add('bay', x1, row[0], a, x2, row[1], b).decal(side, PLATES[side][i] + '_' + size(a, b, row))
    # The emergency stop, on its yellow backing on the front's left-hand plate (as you face it):
    # a yellow box and a red mushroom button. Real basis: every robot cell carries one in reach.
    add('paint', 38, 6, 0.75, 42, 11, 1.5)
    add('estop_cap', 39, 7.5, 0, 41, 9.5, 0.75)
    # The controller's console, centred on the front's middle plate, and the light strip on the
    # rail above it.
    add('trim', 19, 4, 1, 29, 13, 1.5).decal('north', 'console').glow('north')
    strip = add('glow' if working else 'glow_off', 3, 15.5, 0.75, S - 3, 16.25, 1)
    if working:
        strip.glow()
    # Ports, each centred on its end plate: energy and items in on the east, items out on the west.
    add('metal', S - 1.5, 3.5, 3, S - 1, 13.5, 13).decal('east', 'port_energy')
    add('metal', S - 1.5, 3.5, 19, S - 1, 13.5, 29).decal('east', 'port_items')
    add('metal', 1, 3.5, 19, 1.5, 13.5, 29).decal('west', 'port_items')
    # Frames on the machine bay's block edges.
    for x in (0, S - 1):
        for z in (0, S - 1):
            add('trim', x, 2, z, x + 1, 15, z + 1)
    for v in (16, 32):
        add('trim', v - 1, 2, 0, v + 1, 15, 2)
        add('trim', v - 1, 2, S - 2, v + 1, 15, S)
        add('trim', 0, 2, v - 1, 2, 15, v + 1)
        add('trim', S - 2, 2, v - 1, S, 15, v + 1)
    add('trim', 1, 15, 0.25, S - 1, 17, 2)
    add('trim', 1, 15, S - 2, S - 1, 17, S - 0.25)
    add('trim', 0.25, 15, 1, 2, 17, S - 1)
    add('trim', S - 2, 15, 1, S - 0.25, 17, S - 1)

    # The open cell: safety-yellow corner posts up to a graphite top rim, its light strip along the
    # front.
    for x in (0, S - 2):
        for z in (0, S - 2):
            post_box = add('paint', x, 17, z, x + 2, 45, z + 2)
            for face in ('north', 'south', 'west', 'east'):
                post_box.decal(face, 'post_2x28')
    # Light curtains across the open front: an emitter strip down each front post's inner face, lit
    # while the cell works. Real basis: an open robot cell is guarded by light curtains, which stop the
    # arm when anything crosses them.
    beam = 'curtainon_1x26' if working else 'curtain_1x26'
    for cx1, cx2, face in ((2, 2.25, 'east'), (S - 2.25, S - 2, 'west')):
        strip = add('trim', cx1, 18, 0.5, cx2, 44, 1.5).decal(face, beam)
        if working:
            strip.glow(face)
    add('trim', 0, 45, 0, S, 47, 2)
    add('trim', 0, 45, S - 2, S, 47, S)
    add('trim', 0, 45, 2, 2, 47, S - 2)
    add('trim', S - 2, 45, 2, S, 47, S - 2)
    rim = add('glow' if working else 'glow_off', 2, 44.5, 1.5, S - 2, 45, 1.75)
    if working:
        rim.glow()

    # The back wall between the back posts, closing the cell behind the rack: a plate per block face
    # inside and out, graphite joins and a rail between the rows, as the bay below.
    wz1, wz2 = WALL
    for row in WALL_ROWS:
        for i, (a, b) in enumerate(pc.bays(0, S)):
            a, b = max(a, 2), min(b, S - 2)
            name = f'{round(b - a)}x{row[1] - row[0]}'
            outside = WALL_PLATES[row][i]
            add('bay', a, row[0], wz1, b, row[1], wz2).decal('north', 'plate_' + name).decal('south', outside + '_' + name)
    for v in (16, 32):
        add('trim', v - 1, 17, wz1 - 0.25, v + 1, 45, wz2 + 0.25)
    add('trim', 2, 31, wz1 - 0.25, S - 2, 33, wz2 + 0.25)

    # The fixture bed.
    x1, z1, x2, z2 = BED
    add('metal', x1, 16.5, z1, x2, BED_TOP, z2).decal('up', 'bed')
    # The belts, each on a steel frame with end rollers.
    for (bx1, bx2), kind in ((IN_BELT, 'belt_in'), (OUT_BELT, 'belt_out')):
        add('conveyor', bx1, 16.5, BELT_Z[0], bx2, 19, BELT_Z[1])
        add('metal', bx1 + 0.5, 19, BELT_Z[0] + 0.5, bx2 - 0.5, BELT_TOP, BELT_Z[1] - 0.5) \
            .face('up', (kind + '_on') if working else kind)
        add('bright', bx1 - 0.5, 17.5, BELT_Z[1] - 1, bx2 + 0.5, 19, BELT_Z[1])
        # The hood over its front end, standing on the worktop, where the belt runs down into the bay
        # (the in-feed, up out of it): the items come and go through its strip flaps.
        add('trim', bx1 - 0.5, 16.5, BELT_Z[0] - 0.5, bx2 + 0.5, 22.5, HOOD_BACK)             .decal('south', 'hoodmouth_9x6').decal('north', 'hoodfront_9x6')             .decal('west', 'hoodside_5x6').decal('east', 'hoodside_5x6')
    # The arm's base, bolted to the worktop (the turret and the rest turn on it).
    add('trim', PIVOT[0] - 6, 16.5, PIVOT[1] - 6, PIVOT[0] + 6, TURRET_Y, PIVOT[1] + 6)

    # The pattern rack, standing at the back on its feet: nine cartridges facing the arm.
    rx1, ry1, rx2, ry2 = RACK
    add('trim', rx1, ry1, RACK_FACE, rx2, ry2, BACK).decal('north', 'patterns')
    add('trim', rx1, 16.5, RACK_FACE - 0.5, rx2, ry1, BACK + 0.5)
    # The stack light in the back east corner: a pole, a collar, three ribbed lenses (dim here; the
    # renderer lights the one that shows the cell's state) with a ring between each, and a cap, so the
    # column is closed from foot to cap.
    sx1, sz1, sx2, sz2 = STACK
    add('bright', 42.5, 16.5, 40.5, 43.5, STACK_Y[0] - 0.5, 41.5)
    add('trim', sx1 - 0.25, STACK_Y[0] - 0.5, sz1 - 0.25, sx2 + 0.25, STACK_Y[0], sz2 + 0.25)
    for i, (y, lamp) in enumerate(zip(STACK_Y, ('lamp_red', 'lamp_amber', 'lamp_green'))):
        add(lamp, sx1, y, sz1, sx2, y + 2, sz2)
        if i < 2:
            add('trim', sx1 - 0.25, y + 2, sz1 - 0.25, sx2 + 0.25, STACK_Y[i + 1], sz2 + 0.25)
    add('trim', sx1 - 0.25, STACK_Y[2] + 2, sz1 - 0.25, sx2 + 0.25, STACK_Y[2] + 3, sz2 + 0.25)
    return boxes


# ---------------------------------------------------------------- publishing and export

def part_textures(tex):
    """The parts' own faces before the cell forms: the frame a framed plate, the controller the same
    with its console, the arm's part orange with a graphite joint."""
    frame = pc.framed(fin('part_frame', plate_hatch(14, 14)))
    controller = [row[:] for row in frame]
    for y, row in enumerate(tex['console']):
        for x, px in enumerate(row):
            controller[3 + y][2 + x] = px
    arm_face = pc.framed(rows(arm_plate()))
    return {'part_frame': frame, 'part_controller': controller, 'part_arm': arm_face}


def arm_plate():
    """The arm part's face, 14 x 14: orange casing round a graphite joint ring with a bright hub."""
    t = Tex(14, 14)
    t.rect(0, 0, 13, 13, 'X')
    t.rect(0, 0, 13, 0, 'e')
    t.rect(0, 13, 13, 13, 'Z')
    t.rect(3, 3, 10, 10, 'S')
    t.rect(3, 3, 10, 3, 'b')
    t.rect(5, 5, 8, 8, 'F')
    t.rect(5, 5, 8, 5, 'A')
    return t


def export(tex):
    designs = {'idle': design('idle'), 'working': design('working')}
    designs.update({'arm_' + part: arm(part) for part in ARM_PARTS})
    for kind, forward in (('belt_in_on', True), ('belt_out_on', False)):
        publish_frames(FOLDER + kind, [belt(i, forward) for i in range(4)], 2, False)
        tex[kind] = rows(belt(0, forward))
    pc.export_design('assembler', FOLDER, designs, tex, (S // 16, S // 16, S // 16),
                     existing={'belt_in_on': FOLDER + 'belt_in_on', 'belt_out_on': FOLDER + 'belt_out_on',
                               'paint': 'block/assembler/paint',
                               'port_energy': 'block/microreactor/port_energy',
                               'port_items': 'block/microreactor/port_fuel'})
    pc.publish_rows(FOLDER + 'paint', tex['paint'], 16)
    for name, pixels in part_textures(tex).items():
        pc.publish_rows(FOLDER + name, pixels, 16)


def posed_arm():
    """The arm at rest, for the preview."""
    boxes = []
    for part in ARM_PARTS:
        boxes += arm(part)
    return boxes


def main():
    args = sys.argv[1:]
    scale, state = 8, 'working'
    for flag in ('--scale', '--state'):
        if flag in args:
            i = args.index(flag)
            if flag == '--scale':
                scale = int(args[i + 1])
            else:
                state = args[i + 1]
            del args[i:i + 2]
    tex = textures()
    export(tex)
    if not args:
        return
    faces = pc.faces_of(design(state) + posed_arm(), tex)
    views = [mp.render(faces, (1, 1), scale), mp.render(faces, (-1, -1), scale)]
    h = max(len(v) for v in views)
    out = []
    for y in range(h):
        row = []
        for v in views:
            row += v[y] if y < len(v) else [(40, 44, 52, 255)] * len(v[0])
        out.append(row)
    write_png(args[0], out)


if __name__ == '__main__':
    main()
