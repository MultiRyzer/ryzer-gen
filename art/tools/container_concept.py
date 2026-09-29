"""Concept render of the Container Battery, formed (design section 12).

Eight blocks long, three wide and three high, facing north: a battery container the size of a
20 foot shipping container (seven blocks), with a fan unit on its east end. Built, it is a plain white box. The front has 20 module slots, one per
block face round the controller in the middle bay; installing a battery module (as in the home
battery stack) turns that face into a glazed door with the rack set back behind it, lit. The fan unit is the required thermal
management: one big fan fills the east end, with coolant in at its hub. Energy comes in on the
back at the east end and goes out at the west, so as you face the front, inputs are on your left
and outputs on your right. The west end carries the electrical hazard signs. Orange corner castings, a graphite base rail and a top rail make
it read as a container.

Laid out on the pool's grid (pool_concept.py): one framed plate per block face, a pixel of frame
on every block edge, rows 13, 14 and 13 high between a 2 pixel base rail and a 2 pixel top rail,
and every feature centred on its plate. Textures are placeholders until the design is settled.

This script is the container's one source, as pool_concept.py is the pool's: it publishes the
textures (block/container/), exports the design in two states for datagen (art/designs/
container_battery.json: 'empty', every slot a hatch, and 'full', every slot a lit rack; each
front block picks its own), draws the LFP rack's item icon, and renders a preview. The fan's
blades are left out of the export: the controller's renderer spins them.

Run from the repo root:
    python art/tools/container_concept.py [OUT.png] [--scale N]
"""
import sys

from pixelart import Tex, write_png
import model_preview as mp
import pool_concept as pc
from pool_concept import Box, rows

LONG, WIDE, HIGH = 128, 48, 48
# The container's seven bays; the eighth, on the east end, is the fan unit.
BOX = 112
ROWS = [(2, 15), (17, 31), (33, 46)]
# The 20 module slots: every block face of the front but the controller's, as (bay, row).
SLOTS = [(i, r) for r in range(3) for i in range(7) if (i, r) != (3, 1)]
# The preview's: the bottom row full and the middle row part filled, as a player building it up
# would have it.
INSTALLED = {(i, 0) for i in range(7)} | {(0, 1), (1, 1), (2, 1), (4, 1), (5, 1)}


# ---------------------------------------------------------------- placeholder textures

def door(w, h):
    """A cabinet door at size: a light leaf round a window (left clear, so the rack shows through),
    with an orange handle on the right."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'F')
    t.rect(0, 0, w - 1, 0, 'A')
    t.rect(0, 0, 0, h - 1, 'E')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    wx, wy = (w - 8) // 2, (h - 7) // 2
    for y in range(wy, wy + 7):
        for x in range(wx, wx + 8):
            t.px[y][x] = None
    t.rect(wx - 1, wy - 1, wx + 8, wy - 1, 'J')
    t.rect(wx - 1, wy - 1, wx - 1, wy + 7, 'J')
    t.rect(w - 2, h // 2 - 1, w - 2, h // 2 + 1, 'X')
    return rows(t)


def hatch(w, h):
    """An empty module slot at size: a white plate with a hatch set into it, the door you open to
    slide a module in. A dark seam round the door, two hinges on the left, an orange pull latch on
    the right, a label strip at the top and a dim status light that shows the slot is empty."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'F')
    t.rect(0, 0, w - 1, 0, 'A')
    t.rect(0, 0, 0, h - 1, 'E')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    x0, y0, x1, y1 = 2, 2, w - 3, h - 3
    t.rect(x0, y0, x1, y1, 'E')
    t.rect(x0, y0, x1, y0, 'J')
    t.rect(x0, y0, x0, y1, 'J')
    t.rect(x0, y1, x1, y1, 'I')
    t.rect(x1, y0, x1, y1, 'I')
    t.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 1, 'A')
    for y in (y0 + 2, y1 - 3):
        t.rect(x0, y, x0, y + 1, 'T')
    mid = (y0 + y1) // 2
    t.rect(x1 - 2, mid - 1, x1 - 2, mid + 1, 'T')
    t.rect(x1 - 3, mid, x1 - 3, mid, 'X')
    t.rect(x0 + 3, y0 + 3, x1 - 4, y0 + 3, 'I')
    t.set(x1 - 2, y0 + 3, 'f')
    return rows(t)


def rack(w, h):
    """A battery rack's front, seen through its door: modules stacked two pixels high, each with a
    cyan charge light."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'U')
    for y in range(1, h - 1, 2):
        t.rect(1, y, w - 2, y, 'S')
        t.set(w - 3, y, 'i')
    return rows(t)


def corrugated(w, h):
    """Container wall at size: vertical corrugations every two pixels inside a bevelled edge."""
    t = Tex(w, h)
    for x in range(w):
        t.rect(x, 0, x, h - 1, 'F' if x % 2 else 'E')
    t.rect(0, 0, w - 1, 0, 'A')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    return rows(t)


def louvre(w, h):
    """A louvred vent at size: dark slats every two pixels in a light frame."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'F')
    for y in range(2, h - 2, 2):
        t.rect(2, y, w - 3, y, 'T')
        t.rect(2, y + 1, w - 3, y + 1, 'I')
    return rows(t)


def gunmetal():
    """Plain gunmetal plate for the fan unit's roof, mapped by position."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    return rows(t)


def fan():
    """The condenser fan, 12 x 12: a round guard over the blades."""
    t = Tex(12)
    t.rect(0, 0, 11, 11, 'S')
    c = 5.5
    for y in range(12):
        for x in range(12):
            r = ((x - c) ** 2 + (y - c) ** 2) ** 0.5
            if r < 5.6:
                t.set(x, y, 'U' if int(r) % 2 else 'b')
            if r < 1.5:
                t.set(x, y, 'L')
    return rows(t)


def bolt():
    """The electrical hazard sign, 12 x 12: a black lightning bolt on yellow in a black border."""
    t = Tex(12)
    t.rect(0, 0, 11, 11, 'B')
    t.rect(1, 1, 10, 10, 'Y')
    t.stamp(1, 1, [
        '......BB..',
        '.....BB...',
        '....BB....',
        '...BBBBB..',
        '.....BB...',
        '....BB....',
        '...BB.....',
        '..BBB.....',
        '..BB......',
        '..........',
    ])
    return rows(t)


def shroud(w, h):
    """The fan unit's end plate at size: gunmetal, with a round opening (left clear) for the fan,
    ringed in graphite."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'u')
    t.rect(0, 0, w - 1, 0, 's')
    t.rect(0, 0, 0, h - 1, 's')
    t.rect(w - 1, 0, w - 1, h - 1, 'z')
    t.rect(0, h - 1, w - 1, h - 1, 'z')
    cx, cy = w / 2, h / 2
    for y in range(h):
        for x in range(w):
            r = ((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2) ** 0.5
            if r < 20:
                t.px[y][x] = None
            elif r < 21.5:
                t.set(x, y, 'T')
    return rows(t)


def guard(n):
    """The fan guard, n x n: three rings and eight spokes of graphite wire, clear between."""
    t = Tex(n)
    c = n / 2
    import math
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            r = math.hypot(dx, dy)
            a = math.degrees(math.atan2(dy, dx)) % 45
            ring = any(abs(r - rr) < 0.55 for rr in (7, 13.5, 19.5))
            spoke = 6 < r < 20 and (a < 3.5 * 7 / max(r, 7) or a > 45 - 3.5 * 7 / max(r, 7))
            if r < 20 and (ring or spoke):
                t.set(x, y, 'S')
    return rows(t)


def blades(n):
    """The fan's blades, n x n: five swept blades round a hub, clear between them."""
    import math
    t = Tex(n)
    c = n / 2
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            r = math.hypot(dx, dy)
            if r >= 19.5 or r < 5:
                continue
            a = (math.degrees(math.atan2(dy, dx)) + r * 2.2) % 72
            if a < 30:
                t.set(x, y, 'h' if a < 8 else 'L' if a < 20 else 'l')
    return rows(t)


def textures():
    tex = pc.textures()
    tex.update({'fan': fan(), 'bolt': bolt(), 'port_energy': mp.texture('block/microreactor/port_energy'),
                'guard': guard(40), 'blades': blades(40), 'gunmetal': gunmetal()})
    made = {'door': door, 'rack': rack, 'corrugated': corrugated, 'louvre': louvre, 'hatch': hatch,
            'shroud': shroud}
    base = type(tex).__missing__

    def missing(self, name):
        kind, _, size = name.partition('_')
        if kind in made:
            w, h = (int(v) for v in size.split('x'))
            self[name] = made[kind](w, h)
            return self[name]
        return base(self, name)

    type(tex).__missing__ = missing
    return tex


# ---------------------------------------------------------------- the design

def design(installed=INSTALLED, blades=True):
    """The formed container with the given slots installed. blades: draw the fan's blades (the
    preview does; the export leaves them to the renderer)."""
    boxes = []

    def add(m, *c):
        b = Box(m, *c)
        boxes.append(b)
        return b

    def size(a, b, row):
        return f'{b - a}x{row[1] - row[0]}'

    # The dark inside of the container, seen through empty rack slots.
    add('trim', 1, 2, 4, BOX, 46, WIDE - 4)
    # Base and top rails, inset a quarter pixel (and the top rail a quarter pixel low) so the corner
    # castings stand proud of them.
    add('trim', 0.25, 0, 0.25, LONG - 0.25, 2, WIDE - 0.25)
    for z1, z2 in ((0.25, 3), (WIDE - 3, WIDE - 0.25)):
        add('trim', 0.25, 46, z1, LONG - 0.25, 47.75, z2)
    for x1, x2 in ((0.25, 3), (LONG - 3, LONG - 0.25)):
        add('trim', x1, 46, 3, x2, 47.75, WIDE - 3)
    # Roof: a plate per block, between the rails and the joins.
    add('liner', 3, 46, 3, BOX, 47, WIDE - 3)  # roof
    add('gunmetal', BOX, 46, 3, LONG - 3, 47, WIDE - 3)  # the fan unit's roof
    for x in range(16, LONG, 16):
        add('trim', x - 1, 46, 3, x + 1, 48, WIDE - 3)
    for z in (16, 32):
        add('trim', 3, 46, z - 1, LONG - 3, 47.75, z + 1)
    # Corner castings, orange, at the eight corners.
    for x in (0, LONG - 3):
        for z in (0, WIDE - 3):
            for y in (0, 46):
                # A quarter pixel proud of the rails at every face, so no two faces share a plane.
                add('accent', x, y, z, x + 3, y + 2.25 if y == 0 else y + 2, z + 3)

    # The front: 20 module slots, one per block face of the container's seven bays, round the
    # controller in the middle bay. An empty slot is a hatch in a white plate; installing a module
    # turns that face into a glazed door with the rack set back behind the glass, lit. The fan
    # unit's housing is plain gunmetal all round, as its roof.
    for i, (a, b) in enumerate(pc.bays(0, LONG)):
        for r, row in enumerate(ROWS):
            name = size(a, b, row)
            if b > BOX:
                add('gunmetal', a, row[0], 0.5, b, row[1], 3)
            elif (i, r) in installed:
                add('liner', a, row[0], 0.5, b, row[1], 1).decal('north', f'door_{name}')
                add('glass_' + name, a, row[0], 1, b, row[1], 1.25).decal('north', 'glass_' + name)
                add('trim', a + 1, row[0] + 1, 3, b - 1, row[1] - 1, 4) \
                    .decal('north', f'rack_{b - a - 2}x{row[1] - row[0] - 2}').glow('north')
            elif (i, r) != (3, 1):
                add('liner', a, row[0], 0.5, b, row[1], 3).decal('north', f'hatch_{name}')
            else:
                add('liner', a, row[0], 0.5, b, row[1], 3).decal('north', f'panel_{name}')
    add('trim', 51, 19, -0.5, 61, 29, 0.5).decal('north', 'console').glow('north')

    # The back: plates, with energy in at the east end and energy out at the west (your left and
    # right as you face the front), and three vents between them along the same middle row.
    for i, (a, b) in enumerate(pc.bays(0, LONG)):
        for r, row in enumerate(ROWS):
            if b > BOX:
                add('gunmetal', a, row[0], WIDE - 3, b, row[1], WIDE - 0.5)
                continue
            kind = 'louvre' if r == 1 and i in (1, 3, 5) else 'panel'
            add('liner', a, row[0], WIDE - 3, b, row[1], WIDE - 0.5).decal('south', f'{kind}_{size(a, b, row)}')
    add('metal', 99, 19, WIDE - 0.5, 109, 29, WIDE).decal('south', 'port_energy')
    add('metal', 3, 19, WIDE - 0.5, 13, 29, WIDE).decal('south', 'port_energy')

    # The west end: plates, with the electrical hazard signs.
    for j, (a, b) in enumerate(pc.bays(0, WIDE)):
        for r, row in enumerate(ROWS):
            add('liner', 0.5, row[0], a, 3, row[1], b).decal('west', f'panel_{b - a}x{row[1] - row[0]}')
    for z0 in (2, 34):
        add('bright', 0.25, 18, z0, 0.5, 30, z0 + 12).decal('west', 'bolt')

    # The fan unit, on the east end: the thermal management the container needs. One big fan
    # fills the end, behind a round opening in the end plate and a wire guard, turning while the
    # battery works; coolant comes in at its hub, through a port centred on the middle block.
    add('liner', LONG - 3, 2, 1, LONG - 0.5, 46, WIDE - 1).decal('east', 'shroud_46x44')
    add('trim', LONG - 13, 3, 3, LONG - 12, 45, WIDE - 3)                          # the dark well
    if blades:
        add('metal', LONG - 7, 4, 4, LONG - 6, 44, 44).decal('east', 'blades')      # blades
    add('trim', LONG - 1, 4, 4, LONG - 0.75, 44, 44).decal('east', 'guard')         # guard
    add('metal', LONG - 6, 18, 18, LONG - 1.5, 30, 30)                              # hub
    add('metal', LONG - 6, 19, 17, LONG - 1.75, 29, 31)
    add('metal', LONG - 6, 17, 19, LONG - 1.75, 31, 29)
    add('metal', LONG - 0.5, 19, 19, LONG, 29, 29).decal('east', 'port_coolant')
    add('metal', LONG - 1.5, 20, 20, LONG - 0.5, 28, 28)                            # port neck

    # Frames on every block edge: corner posts, joins and rails between the rows.
    for x in (0, LONG - 1):
        for z in (0, WIDE - 1):
            add('trim', x, 2, z, x + 1, 46, z + 1)
    for x in range(16, LONG, 16):
        add('trim', x - 1, 2, 0, x + 1, 46, 3)
        add('trim', x - 1, 2, WIDE - 3, x + 1, 46, WIDE)
    for z in (16, 32):
        add('trim', 0, 2, z - 1, 3, 46, z + 1)
    for y1, y2 in ((15, 17), (31, 33)):
        add('trim', 1, y1, 0.25, LONG - 1, y2, 3)
        add('trim', 1, y1, WIDE - 3, LONG - 1, y2, WIDE - 0.25)
        add('trim', 0.25, y1, 1, 3, y2, WIDE - 1)
    return boxes


FOLDER = 'block/container/'


def rack_icon():
    """The LFP rack's item icon: a battery rack module seen from the front, a graphite case with
    modules stacked in it, each with a cyan charge light, and an orange handle."""
    t = Tex(16)
    t.rect(3, 1, 12, 14, 'S')
    t.rect(3, 1, 12, 1, 'b')
    t.rect(3, 1, 3, 14, 'b')
    t.rect(12, 1, 12, 14, 'U')
    t.rect(3, 14, 12, 14, 'U')
    for y in range(3, 13, 2):
        t.rect(4, y, 10, y, 'T')
        t.set(10, y, 'i')
    t.rect(6, 0, 9, 0, 'X')
    t.rect(1, 2, 2, 13, 'k')
    t.rect(13, 2, 14, 13, 'k')
    return rows(t)


def export(tex):
    """Publishes the container's textures and writes its design for datagen."""
    designs = {'empty': design(set(), blades=False), 'full': design(set(SLOTS), blades=False)}
    pc.export_design('container_battery', FOLDER, designs, tex, (LONG // 16, HIGH // 16, WIDE // 16),
                     existing={'port_energy': 'block/microreactor/port_energy'})
    pc.publish_rows(FOLDER + 'blades', tex['blades'], 48)
    # The parts' own faces before the container forms: a framed white plate, the controller with
    # its console, the thermal unit in gunmetal with its fan.
    frame = pc.framed(tex['panel_14x14'])
    controller = [row[:] for row in frame]
    for y, row in enumerate(tex['console']):
        for x, px in enumerate(row):
            controller[3 + y][3 + x] = px
    thermal = pc.framed([[tex['gunmetal'][y][x] for x in range(14)] for y in range(14)])
    for y, row in enumerate(tex['fan']):
        for x, px in enumerate(row):
            thermal[2 + y][2 + x] = px
    for name, pixels in (('part_frame', frame), ('part_controller', controller), ('part_thermal', thermal)):
        pc.publish_rows(FOLDER + name, pixels, 16)
    pc.publish_rows('item/lfp_battery_rack', rack_icon(), 16)


def main():
    args = sys.argv[1:]
    scale = 6
    if '--scale' in args:
        i = args.index('--scale')
        scale = int(args[i + 1])
        del args[i:i + 2]
    tex = textures()
    export(tex)
    if not args:
        return
    faces = pc.faces_of(design(), tex)
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
