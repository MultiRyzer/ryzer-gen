"""Concept render of the Spent Fuel Pool, formed (design section 7).

Five blocks long, three wide and three high, facing north. A lined basin of water on a hazard
skid: two rows of storage racks on the floor with the stored fuel glowing Cherenkov blue, windows
in the front wall to see them, the controller in the middle of the front, and a yellow gantry
crane on rails inside the rim, lifting a rod out. Inputs (hot fuel, water) on the east end, cooled
fuel out on the west end, so as you face the front inputs are on your left and outputs on your
right (the front's clockwise and anticlockwise sides, as in StationLayout.portFace).

The design is one list of boxes in pixels (x 0 to 80 west to east, y 0 to 48, z 0 to 48 front to
back), in three states: dry, full of water, and full with fuel glowing in the racks. This script is
the pool's one source: it publishes the pool's textures (block/pool/), exports the design for
datagen (art/designs/spent_fuel_pool.json, read by PoolModel, which cuts one model per block and
state), and renders a preview cut exactly as datagen cuts it.

Textures drawn at a size (a plate 14 x 13, a pipe run 24 x 4) are decals, mapped one texel per
pixel from the face's top left. The game maps a face's UVs over the whole texture, so each goes on
a square canvas of the next multiple of 16 and the export records the canvas, for datagen to scale
by. Plain materials (liner, water, rack) are 16 x 16 and map by position.

Run from the repo root:
    python art/tools/pool_concept.py [OUT.png] [--scale N] [--state dry|wet|active]
"""
import json
import math
import os
import sys

from pixelart import PAL, Tex, write_png, ART_TEXTURES, MOD_TEXTURES, ROOT
import model_preview as mp

LONG, WIDE, HIGH = 80, 48, 48


# ---------------------------------------------------------------- concept textures

def rows(tex, alpha=255):
    return [[(*tex.rgba(x, y)[:3], alpha if tex.px[y][x] else 0) for x in range(tex.w)] for y in range(tex.h)]


def water():
    """Pool water: deep blue, lighter ripples, see-through."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'w')
    for x, y in ((2, 3), (3, 3), (9, 6), (10, 6), (11, 6), (5, 11), (6, 11), (13, 13), (14, 1)):
        t.set(x, y, 'v')
    return rows(t, 150)


def panel(w, h):
    """A casing plate drawn at its exact size, so it is never cut: a lit top and left edge, a shaded
    right and foot, and a shallow inset three pixels in, as on the microreactor's casing."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'F')
    t.rect(0, 0, w - 1, 0, 'A')
    t.rect(0, 0, 0, h - 1, 'E')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    x0, y0, x1, y1 = 3, 3, w - 4, h - 4
    t.rect(x0, y0, x1, y0, 'J')
    t.rect(x0, y0, x0, y1, 'J')
    t.rect(x0 + 1, y1, x1, y1, 'E')
    t.rect(x1, y0 + 1, x1, y1, 'E')
    return rows(t)


def glass(w, h):
    """A window pane at its exact size: clear, with two glints."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'V')
    for i in range(4):
        t.set(3 + i, h - 4 - i, 'A')
        t.set(7 + i, h - 3 - i, 'A')
    return rows(t, 70)


# Light to dark across a round part, for copper pipe and the exchanger's gunmetal shell. The
# highlight sits a little off centre, towards the light.
COPPER = ['R', 'e', 'r', 'O']
GUNMETAL = ['u', 's', 's', 's', 'u', 'u', 'u', 'z', 'z', 'T']


def ramp(colours, n):
    """The shade for each of n pixels across a round part."""
    return [colours[min(len(colours) - 1, i * len(colours) // n)] for i in range(n)]


def pipe(length, across, along_x):
    """A pipe face drawn at size: shaded across its width so it reads as round, with a raised
    coupling ring every eight pixels (a lit pixel, then its shadow). along_x: the run goes across
    the texture, shaded down it; otherwise it runs down the texture, shaded across it."""
    shade = ramp(COPPER, across)
    t = Tex(length, across) if along_x else Tex(across, length)
    for i in range(length):
        ring = i % 8 == 4
        shadow = i % 8 == 5
        for j in range(across):
            k = 'e' if ring and j < across - 1 else 'o' if shadow else shade[j]
            if along_x:
                t.set(i, j, k)
            else:
                t.set(j, i, k)
    return rows(t)


def mitre(n, up):
    """The face of a mitred elbow, n x n, where a pipe from the left turns down (or up): cut at 45
    degrees like a real welded bend. The half against the left edge carries the horizontal pipe's
    shading on, the half against the bottom (or top) edge the vertical pipe's, and a weld seam runs
    along the diagonal between them."""
    shade = ramp(COPPER, n)
    t = Tex(n, n)
    for j in range(n):
        for i in range(n):
            # Down: the cut runs from the outer corner (top right) to the inner (bottom left).
            d = (i - j) if up else (i + j - (n - 1))
            if d == 0:
                k = 'o'
            elif d < 0:
                k = shade[j]
            else:
                k = shade[i]
            t.set(i, j, k)
    return rows(t)


def shell(w, h):
    """The heat exchanger's shell: gunmetal shaded as a cylinder, a welded seam every six pixels."""
    t = Tex(w, h)
    shade = ramp(GUNMETAL, w)
    for x in range(w):
        t.rect(x, 0, x, h - 1, shade[x])
    for y in range(5, h, 6):
        for x in range(w):
            t.set(x, y, 'z' if shade[x] in 'su' else 'T')
    return rows(t)


def band(w, h):
    """A copper band round the shell, two high: lit above, shaded below, a bolt every three."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, 0, 'e')
    t.rect(0, 1, w - 1, h - 1, 'r')
    for x in range(1, w - 1, 3):
        t.set(x, h - 1, 'o')
    return rows(t)


def liner():
    """The pool's steel liner, seen inside: plain plate welded in squares of eight."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    for i in (7, 15):
        t.rect(i, 0, i, 15, 'I')
        t.rect(0, i, 15, i, 'I')
    return rows(t)


def rack():
    """Plain rack steel, for the concept's crane grab and anything mapped by position."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'U')
    return rows(t)


# The storage rack: one cell per cooling slot, laid out as the controller's screen lays them out
# (6 across, 3 deep), so a rod stands in the water where its slot sits on the screen.
RACK_COLS, RACK_ROWS, CELL = 6, 3, 10
RACK_X, RACK_Z = 10, 7
RACK_TOP = 14


def racktop(w, h):
    """The rack seen from above, at size: a 10 x 10 cell per slot, each a raised steel collar (a
    pixel of frame, lit on its top and left) round a dark socket for the rod, all even."""
    t = Tex(w, h)
    for cy in range(0, h, CELL):
        for cx in range(0, w, CELL):
            t.rect(cx, cy, cx + CELL - 1, cy + CELL - 1, 'U')
            t.rect(cx, cy, cx + CELL - 1, cy, 'b')
            t.rect(cx, cy, cx, cy + CELL - 1, 'b')
            t.rect(cx + CELL - 1, cy, cx + CELL - 1, cy + CELL - 1, 'k')
            t.rect(cx, cy + CELL - 1, cx + CELL - 1, cy + CELL - 1, 'k')
            t.rect(cx + 2, cy + 2, cx + 7, cy + 7, 'S')
            t.rect(cx + 3, cy + 3, cx + 6, cy + 6, 'K')
    return rows(t)


def rackside(w, h):
    """The rack's side, at size: a lit top edge, a shaded foot, and a seam at every cell."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'U')
    for cx in range(0, w, CELL):
        t.rect(cx, 1, cx, h - 2, 'b')
        t.rect(cx + CELL - 1, 1, cx + CELL - 1, h - 2, 'k')
    t.rect(0, 0, w - 1, 0, 'b')
    t.rect(0, h - 1, w - 1, h - 1, 'k')
    return rows(t)


def rod_side():
    """A fuel assembly's side, 4 wide and 16 tall from the top, drawn as one picture down its
    length, as a real assembly reads: a steel top fitting (lit lip, a slot either side) and its
    lifting collar, then two fuel pins a face, each shaded round with the dark gap between them,
    banded by bright spacer grids. The renderer maps it from each face's top left and tints it
    Cherenkov blue while the rod is hot. Rows 0 to 7 stand above the rack."""
    t = Tex(16)
    t.rect(0, 0, 3, 0, 'H')
    t.rect(0, 1, 3, 2, 'L')
    t.set(0, 2, 'S')
    t.set(3, 2, 'S')
    t.rect(0, 3, 3, 3, 'S')
    for y in range(4, 16):
        t.set(0, y, 'h')
        t.set(1, y, 'm')
        t.set(2, y, 'h')
        t.set(3, y, 'l')
    for y in (7, 12):
        t.rect(0, y, 3, y, 'H')
        t.rect(0, y + 1, 3, y + 1, 'S')
    return rows(t)


def rod_top():
    """A fuel assembly's top, 4 x 4: the top fitting's lit rim round the dark socket the crane's
    grab takes."""
    t = Tex(16)
    t.rect(0, 0, 3, 3, 'L')
    t.rect(0, 0, 3, 0, 'H')
    t.rect(0, 0, 0, 3, 'H')
    t.rect(1, 1, 2, 2, 'U')
    return rows(t)


def rod_cells(count):
    """The first count rack cells, in screen order (left to right, front to back), as (x, z)."""
    cells = []
    for i in range(count):
        cells.append((RACK_X + (i % RACK_COLS) * CELL, RACK_Z + (i // RACK_COLS) * CELL))
    return cells


# A lit crane yellow for top edges (the palette's yellows are the hazard paint and its shadow).
PAL['9'] = 'f4d35e'


def yellow(w, h):
    """Crane paint at size: a lit top edge and a shaded foot."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'Y')
    t.rect(0, 0, w - 1, 0, '9')
    if h > 1:
        t.rect(0, h - 1, w - 1, h - 1, 'y')
    return rows(t)


def girder(w, h):
    """The bridge's box girder, side on: lit top flange, shaded bottom flange, a stiffener every
    four pixels, and hazard stripes over the last four at each end."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'Y')
    for x in range(3, w - 3, 4):
        t.rect(x, 1, x, h - 2, 'y')
    t.rect(0, 0, w - 1, 0, '9')
    t.rect(0, h - 1, w - 1, h - 1, 'y')
    for x in list(range(4)) + list(range(w - 4, w)):
        for y in range(h):
            t.set(x, y, 'B' if (x + y) % 4 < 2 else 'Y')
    return rows(t)


def stripes(w, h):
    """Hazard stripes on the crane's ends and grab: black and yellow, two pixels each, diagonal."""
    t = Tex(w, h)
    for y in range(h):
        for x in range(w):
            t.set(x, y, 'B' if (x + y) % 4 < 2 else 'Y')
    return rows(t)


def deck(w, h):
    """The bridge's top, seen from above (w across, h along the span): yellow edges and a dark
    walkway with a tread line every two pixels."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'Y')
    t.rect(0, 0, 0, h - 1, '9')
    t.rect(w - 1, 0, w - 1, h - 1, 'y')
    t.rect(2, 0, w - 3, h - 1, 'S')
    for y in range(0, h, 2):
        t.rect(2, y, w - 3, y, 'b')
    return rows(t)


def truck(w, h):
    """An end truck, side on: yellow, with a wheel near each end (dark, a steel hub)."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'Y')
    t.rect(0, 0, w - 1, 0, '9')
    for x0 in (1, w - 4):
        t.rect(x0, 0, x0 + 2, h - 1, 'B')
        t.set(x0 + 1, h // 2, 'L')
    return rows(t)


def hoist(w, h):
    """The trolley from above: a yellow frame round the hoist's cable drum (grey, shaded round,
    wrapped in rope) and its motor."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'Y')
    t.rect(0, 0, w - 1, 0, '9')
    t.rect(0, 0, 0, h - 1, '9')
    t.rect(w - 1, 0, w - 1, h - 1, 'y')
    t.rect(0, h - 1, w - 1, h - 1, 'y')
    drum = ['H', 'h', 'L', 'l']
    for j, k in enumerate(drum):
        t.rect(1, 2 + j, w - 5, 2 + j, k)
    for x in range(2, w - 5, 2):
        t.rect(x, 2, x, 5, 'S')
    t.rect(w - 4, 2, w - 2, 5, 'S')
    t.rect(w - 4, 2, w - 2, 2, 'b')
    return rows(t)


def rope(w, h):
    """Wire rope: dark strands laid diagonally."""
    t = Tex(w, h)
    for y in range(h):
        for x in range(w):
            t.set(x, y, 'l' if (x + y) % 2 == 0 else 'S')
    return rows(t)


def rail(w, h):
    """A crane rail's head from above: a polished running line down the middle."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'L')
    t.rect(0, 0, w - 1, 0, 'H')
    return rows(t)


def paint():
    """Crane yellow, with a lit top edge and a shaded foot."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'Y')
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 15, 15, 15, 'y')
    return rows(t)


def trefoil():
    """The radiation trefoil, 12 x 12: black on yellow in a black border. Placed by hand from the
    real proportions (a centre dot, a gap, then three 60 degree blades, two up and one down), since
    at this size a computed one breaks up."""
    t = Tex(12)
    t.rect(0, 0, 11, 11, 'B')
    t.rect(1, 1, 10, 10, 'Y')
    t.stamp(1, 1, [
        '..........',
        '..B....B..',
        '.BBB..BBB.',
        '.BBB..BBB.',
        'BBB.BB.BBB',
        '....BB....',
        '..........',
        '....BB....',
        '...BBBB...',
        '...BBBB...',
    ])
    return rows(t)


def hatch(w, h):
    """The chute's opening in the wall, at size: a graphite frame round a dark mouth, the chute
    running back into the wall."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'S')
    t.rect(0, 0, w - 1, 0, 'b')
    t.rect(1, 1, w - 2, h - 2, 'K')
    t.rect(1, 1, w - 2, 1, 'U')
    return rows(t)


def console():
    """The controller, 10 x 10: a graphite bezel round a readout of the racks (two rows of three
    slots, cyan while cooling, green when ready) and a status strip below, all centred."""
    t = Tex(10, 10)
    t.rect(0, 0, 9, 9, 'S')
    t.rect(0, 0, 9, 0, 'b')
    t.rect(0, 9, 9, 9, 'T')
    t.rect(1, 1, 8, 6, 'U')
    for x in (2, 4, 6):
        for y in (2, 4):
            t.rect(x, y, x + 1, y, 'n' if (x, y) in ((2, 2), (6, 4)) else 'i')
    t.rect(2, 8, 7, 8, 'i')
    return rows(t)


CONCEPT = {
    'water': water, 'rack': rack, 'paint': paint, 'trefoil': trefoil, 'console': console, 'liner': liner,
    'rod_side': rod_side, 'rod_top': rod_top,
}
EXISTING = {
    'casing': 'block/microreactor/steel', 'trim': 'block/microreactor/steel_dark',
    'skid': 'block/machine/family/skid', 'hazard': 'block/machine/family/hazard',
    'bright': 'block/machine/family/bright', 'metal': 'block/machine/family/metal',
    'copper': 'block/machine/family/copper', 'accent': 'block/machine/family/accent',
    'glow': 'block/microreactor/glow', 'glow_off': 'block/microreactor/glow_off',
    'cherenkov': 'block/station/cherenkov',
    'port_fuel': 'block/microreactor/port_fuel', 'port_coolant': 'block/microreactor/port_coolant',
    'cable': 'block/machine/family/lid', 'motor': 'block/machine/intake_pump/motor',
}


class Textures(dict):
    """Named textures; 'panel_WxH' and 'glass_WxH' are drawn on demand at that size."""

    def __missing__(self, name):
        kind, _, size = name.partition('_')
        w, h = (int(v) for v in size.split('x'))
        make = {
            'panel': panel, 'glass': glass, 'shell': shell, 'band': band,
            'pipex': lambda a, b: pipe(a, b, True), 'pipey': lambda a, b: pipe(b, a, False),
            'mitreup': lambda a, b: mitre(a, True), 'mitredown': lambda a, b: mitre(a, False),
            'yellow': yellow, 'girder': girder, 'stripes': stripes, 'deck': deck, 'truck': truck,
            'hoist': hoist, 'rope': rope, 'rail': rail, 'racktop': racktop, 'rackside': rackside,
            'hatch': hatch,
        }[kind]
        self[name] = make(w, h)
        return self[name]


def textures():
    out = Textures({name: fn() for name, fn in CONCEPT.items()})
    for name, path in EXISTING.items():
        out[name] = mp.texture(path)
    return out


# ---------------------------------------------------------------- the design

class Box:
    def __init__(self, material, x1, y1, z1, x2, y2, z2):
        self.material = material
        self.f = (x1, y1, z1)
        self.t = (x2, y2, z2)
        self.faces = {}
        self.decals = {}
        self.glowing = set()

    def face(self, d, m):
        self.faces[d] = m
        return self

    def sides(self, m):
        for d in ('north', 'south', 'west', 'east'):
            self.faces[d] = m
        return self

    def decal(self, d, m):
        self.decals[d] = m
        return self

    def glow(self, *dirs):
        """Faces drawn at full brightness in the dark: screens and lights."""
        self.glowing |= set(dirs or ('north', 'south', 'west', 'east', 'up', 'down'))
        return self


STATES = ('dry', 'wet', 'active')


# Rows of panels, bottom to top: the hazard skid under the first, frames between, the coping on top.
ROWS = [(2, 15), (17, 31), (33, 47)]
# The back wall's outer face, set in so the cooling loop fits behind it inside the footprint.
BACK = 43


def bays(lo, hi):
    """Panel spans along a face from lo to hi: one per block, between the frames on the block
    edges (a pixel each side of a join), clipped to lo and hi."""
    out = []
    for k in range(lo // 16, (hi + 15) // 16):
        out.append((max(lo, k * 16 + 1), min(hi, k * 16 + 15)))
    return out


# The crane's parts, each a model of its own that the pool controller's renderer moves (PoolRenderer):
# the bridge on its end trucks runs along the rails, the trolley across the bridge, and the grab
# (with a rod, while it carries one) hangs from the trolley on the cable, a one-pixel length the
# renderer stretches. Each is exported in place, parked over the middle of the pool; the renderer
# draws them relative to CRANE_ORIGIN (x, y, z in pixels), where the park sits.
CRANE_ORIGIN = (40, 40, 19)


def crane_parts():
    """The crane's moving parts, parked, as a dict of state name to boxes."""
    def skin(b, kinds):
        dx, dy, dz = (round(b.t[i] - b.f[i]) for i in range(3))
        sizes = {'north': (dx, dy), 'south': (dx, dy), 'west': (dz, dy), 'east': (dz, dy),
                 'up': (dx, dz), 'down': (dx, dz)}
        for d, kind in kinds.items():
            w, h = sizes[d]
            b.decal(d, f'{kind}_{w}x{h}')
        return b

    bridge = []
    for z0 in (3, BACK - 5):
        bridge.append(skin(Box('paint', 34, 41, z0, 46, 44, z0 + 3),
                           {'north': 'truck', 'south': 'truck', 'west': 'stripes', 'east': 'stripes', 'up': 'yellow'}))
    bridge.append(skin(Box('paint', 36, 42, 5, 44, 45, BACK - 4),
                       {'west': 'girder', 'east': 'girder', 'up': 'deck', 'north': 'stripes', 'south': 'stripes'}))
    trolley = [skin(Box('paint', 35, 45, 18, 45, 47, 26),
                    {'up': 'hoist', 'north': 'yellow', 'south': 'yellow', 'west': 'yellow', 'east': 'yellow'})]
    cable = [skin(Box('cable', 39, 40, 21, 41, 41, 23), {d: 'rope' for d in ('north', 'south', 'west', 'east')})]
    grab = [skin(Box('accent', 37, 38, 19, 43, 41, 25), {d: 'stripes' for d in ('north', 'south', 'west', 'east')})]
    # The rod it carries, drawn as the fuel assemblies in the rack are (rod_side from its top).
    rod = [Box('metal', 38, 28, 20, 42, 38, 24).decal('up', 'rod_top')]
    for d in ('north', 'south', 'west', 'east'):
        rod[0].decal(d, 'rod_side')
    return {'crane_bridge': bridge, 'crane_trolley': trolley, 'crane_cable': cable, 'crane_grab': grab, 'crane_rod': rod}


def crane_parked():
    """The crane parked, for the preview: its parts, the cable stretched from grab to trolley."""
    parts = crane_parts()
    boxes = parts['crane_bridge'] + parts['crane_trolley'] + parts['crane_grab']
    cable = parts['crane_cable'][0]
    boxes.append(Box(cable.material, cable.f[0], 41, cable.f[2], cable.t[0], 45, cable.t[2]))
    return boxes


def preview_rods(count):
    """Rods standing in the first count cells, as the renderer draws them, for the preview only."""
    boxes = []
    for x0, z0 in rod_cells(count):
        rod = Box('rod_side', x0 + 3, 6, z0 + 3, x0 + 7, RACK_TOP + 8, z0 + 7).decal('up', 'rod_top')
        for d in ('north', 'south', 'west', 'east'):
            rod.decal(d, 'rod_side')
        boxes.append(rod)
    return boxes


def design(state='active'):
    """The formed pool. 'dry': no water. 'wet': full, the racks empty. 'active': full, with fuel
    in the racks glowing Cherenkov blue. The light strip is lit whenever there is water."""
    boxes = []
    # Pipes stop half a pixel short of the back edge, so the clamps and bands round them stand
    # proud and no two faces share a plane.
    z1, z2 = BACK + 1, WIDE - 0.5

    def add(m, *c):
        b = Box(m, *c)
        boxes.append(b)
        return b

    def run(x1, y1, z1, x2, y2, z2):
        """A pipe along its longest side, each long face shaded at its own size."""
        b = add('copper', x1, y1, z1, x2, y2, z2)
        dx, dy, dz = round(x2 - x1), round(y2 - y1), round(z2 - z1)
        if dx >= dy:
            for d, h in (('north', dy), ('south', dy), ('up', dz), ('down', dz)):
                b.decal(d, f'pipex_{dx}x{h}')
        else:
            for d, w in (('north', dx), ('south', dx), ('west', dz), ('east', dz)):
                b.decal(d, f'pipey_{w}x{dy}')
        return b

    def skin(b, kinds):
        """Gives each named face of a box a texture of that kind drawn at the face's size."""
        dx, dy, dz = (round(b.t[i] - b.f[i]) for i in range(3))
        sizes = {'north': (dx, dy), 'south': (dx, dy), 'west': (dz, dy), 'east': (dz, dy),
                 'up': (dx, dz), 'down': (dx, dz)}
        for d, kind in kinds.items():
            w, h = sizes[d]
            b.decal(d, f'{kind}_{w}x{h}')
        return b

    def elbow(x, y, turn):
        """A mitred 4 x 4 x 4 corner at (x, y) in the loop's plane, where a pipe from the west turns
        'up' or 'down'. Its outer faces carry each run's shading on round the bend."""
        b = add('copper', x, y, z1, x + 4, y + 4, z2)
        b.decal('south', f'mitre{turn}_4x4').decal('north', f'mitre{turn}_4x4')
        b.decal('east', 'pipey_4x4')
        b.decal('down' if turn == 'up' else 'up', 'pipex_4x4')
        return b

    def size(a, b, row):
        return f'{round(b - a)}x{row[1] - row[0]}'

    # Skid, and the liner floor inside.
    add('skid', 0, 0, 0, LONG, 2, WIDE).sides('hazard')
    add('liner', 3, 2, 3, LONG - 3, 4, BACK - 2)

    # Front: a plate per block face, windows in the second and fourth bays' lower two rows, the
    # trefoil signs on the end bays and the controller in the middle, all centred on their plates.
    for i, (a, b) in enumerate(bays(0, LONG)):
        for r, row in enumerate(ROWS):
            if i in (1, 3) and r < 2:
                pane = 'glass_' + size(a, b, row)
                add(pane, a, row[0], 1.5, b, row[1], 2).decal('north', pane)
            else:
                add('liner', a, row[0], 0.5, b, row[1], 3).decal('north', 'panel_' + size(a, b, row))
    # Back: the same, set in to BACK, between the end walls.
    for a, b in bays(3, LONG - 3):
        for row in ROWS:
            add('liner', a, row[0], BACK - 2, b, row[1], BACK - 0.5).decal('south', 'panel_' + size(a, b, row))
    # Ends: full depth, so they frame the recess the cooling loop sits in.
    for x1, x2, side, inward in ((0.5, 3, 'west', 'east'), (LONG - 3, LONG - 0.5, 'east', 'west')):
        for a, b in bays(0, WIDE):
            for row in ROWS:
                add('liner', x1, row[0], a, x2, row[1], b).decal(side, 'panel_' + size(a, b, row)) \
                    .face(inward, 'trim' if b > BACK - 2 else 'liner')
    add('trim', 1, 2, WIDE - 1, 3, 47, WIDE)
    add('trim', LONG - 3, 2, WIDE - 1, LONG - 1, 47, WIDE)

    # Frames on every block edge: corner posts, joins, and rails between the rows (set a quarter
    # pixel behind the joins, so no two faces share a plane).
    for x in (0, LONG - 1):
        for z in (0, WIDE - 1):
            add('trim', x, 2, z, x + 1, 47, z + 1)
    for x in (16, 32, 48, 64):
        add('trim', x - 1, 2, 0, x + 1, 47, 3)
        add('trim', x - 1, 2, BACK - 2, x + 1, 47, BACK)
    for z in (16, 32):
        add('trim', 0, 2, z - 1, 3, 47, z + 1)
        add('trim', LONG - 3, 2, z - 1, LONG, 47, z + 1)
    for y1, y2 in ((15, 17), (31, 33)):
        add('trim', 1, y1, 0.25, LONG - 1, y2, 3)
        add('trim', 3, y1, BACK - 2, LONG - 3, y2, BACK - 0.25)
        add('trim', 0.25, y1, 1, 3, y2, WIDE - 1)
        add('trim', LONG - 3, y1, 1, LONG - 0.25, y2, WIDE - 1)

    # Coping round the rim, and a cyan light strip along the front on the upper rail.
    add('trim', 0, 47, 0, LONG, 48, 3)
    add('trim', 3, 47, BACK - 2, LONG - 3, 48, BACK)
    add('trim', 0, 47, 3, 3, 48, WIDE)
    add('trim', LONG - 3, 47, 3, LONG, 48, WIDE)
    if state == 'dry':
        add('glow_off', 1, 31.5, -0.25, LONG - 1, 32.5, 0.25)
    else:
        add('glow', 1, 31.5, -0.25, LONG - 1, 32.5, 0.25).glow()

    # Signs and the controller, centred on the middle row's plates (x 1 to 15 and 33 to 47, y 17
    # to 31).
    for x0 in (2, 66):
        add('bright', x0, 18, 0.25, x0 + 12, 30, 0.5).decal('north', 'trefoil')
    add('trim', 35, 19, -0.5, 45, 29, 0.5).decal('north', 'console').glow('north')

    # Ports, centred on their block faces in the middle row: hot fuel and water in on the east
    # end (your left as you face the front), cooled fuel out on the west.
    add('metal', LONG - 0.5, 19, 3, LONG, 29, 13).decal('east', 'port_fuel')
    add('metal', LONG - 0.5, 19, 35, LONG, 29, 45).decal('east', 'port_coolant')
    add('metal', 0, 19, 19, 0.5, 29, 29).decal('west', 'port_fuel')

    # The rack on the floor, centred in the pool (x 3 to 77, z 3 to 41): a cell per cooling slot.
    # The rods in it are drawn by the controller's renderer, one per filled slot, so the model
    # has none; the preview draws some (see preview_rods).
    w, d = RACK_COLS * CELL, RACK_ROWS * CELL
    skin(add('rack', RACK_X, 4, RACK_Z, RACK_X + w, RACK_TOP, RACK_Z + d),
         {'up': 'racktop', 'north': 'rackside', 'south': 'rackside', 'west': 'rackside', 'east': 'rackside'})

    # The chute hatch inside the west wall, under the output port, where the crane drops each cooled
    # rod: a dark opening, and in front of it the hatch tilted open like a parcel box's, a scoop
    # that catches the rod. Its front leans out towards the top and its cheeks curve, drawn in
    # three steps; a handle on its front.
    add('trim', 3, 19, 19.5, 3.5, 27.5, 28.5).decal('east', 'hatch_9x8')
    steps = ((20, 22.5, 6.5), (22.5, 25, 9.5), (25, 27.5, 12.5))
    for z in (20, 27.5):
        for y1, y2, out in steps:
            add('trim', 3.5, y1, z, out, y2, z + 0.5)
    for y1, y2, out in steps:
        add('trim', out - 0.5, y1, 20.5, out, y2, 27.5)
    add('trim', 3.5, 19.5, 20.5, 6.5, 20, 27.5)
    add('trim', 6.5, 22, 20.5, 9.5, 22.5, 27.5)
    add('trim', 9.5, 24.5, 20.5, 12.5, 25, 27.5)
    add('bright', 12.5, 25.5, 22.5, 13, 26, 25.5)

    if state != 'dry':
        add('water', 3, 4, 3, LONG - 3, 38, BACK - 2)

    # The crane's rails inside the rim, along the long walls. The crane itself moves, so its parts
    # are drawn by the controller's renderer (crane_parts); the preview adds them parked.
    for z0 in (3, BACK - 4):
        skin(add('bright', 3, 40, z0, LONG - 3, 41, z0 + 2), {'up': 'rail'})

    # The cooling loop in the recess behind the back wall, as real pools have: warm water drawn
    # off at the east end, down and along to the heat exchanger in the middle bay, on to the pump,
    # and back into the pool at the west end. Pipes are 4 across, a pixel clear of the plates;
    # flanges sit centred on the middle row's end plates, and clamps on the joins.
    for x0 in (6, 68):
        add('bright', x0, 21, BACK, x0 + 6, 27, BACK + 1)                  # flanges
    run(69, 12, z1, 73, 26, z2)                                    # east: down
    run(45, 8, z1, 69, 12, z2)                                    # along to the exchanger
    add('trim', 34, 2, BACK, 46, 4, WIDE)                                   # exchanger saddle
    add('metal', 35, 4, BACK + 1.5, 45, 30, WIDE - 0.5).decal('south', 'shell_10x26') \
        .decal('west', 'shell_3x26').decal('east', 'shell_3x26')               # exchanger shell
    for y in (7, 13, 19, 25):
        add('copper', 34.5, y, BACK + 1, 45.5, y + 2, WIDE).decal('south', 'band_11x2') \
            .decal('west', 'band_3x2').decal('east', 'band_3x2')               # bands
    add('bright', 38, 15.5, BACK + 1, 42, 18.5, BACK + 1.5)                     # nameplate
    run(30, 8, z1, 35, 12, z2)                                    # on to the pump
    add('trim', 18, 2, BACK, 30, 4, WIDE)                                   # pump plinth
    add('metal', 25, 5, z1, 30, 14, z2)                                     # pump casing
    add('motor', 18, 4, BACK + 1.5, 25, 14, WIDE - 0.5)                     # motor
    run(25, 14, z1, 29, 22, z2)                                   # discharge, up
    run(7, 22, z1, 25, 26, z2)
    elbow(25, 22, 'down')
    elbow(69, 8, 'up')                                    # west: back in
    add('bright', 15, 21, BACK + 0.5, 17, 27, WIDE)                         # clamps at the joins
    for x in (48, 64):
        add('bright', x - 1, 7, BACK + 0.5, x + 1, 13, WIDE)
    add('bright', 68, 15, BACK + 0.5, 74, 17, WIDE)                         # clamp on the drop
    return boxes


# ---------------------------------------------------------------- cutting and drawing

def decal_uv(d, bf, bt, f, t):
    v1, v2 = bt[1] - t[1], bt[1] - f[1]
    return {
        'north': (bt[0] - t[0], v1, bt[0] - f[0], v2),
        'south': (f[0] - bf[0], v1, t[0] - bf[0], v2),
        'west': (f[2] - bf[2], v1, t[2] - bf[2], v2),
        'east': (bt[2] - t[2], v1, bt[2] - f[2], v2),
        'up': (f[0] - bf[0], f[2] - bf[2], t[0] - bf[0], t[2] - bf[2]),
        'down': (f[0] - bf[0], bt[2] - t[2], t[0] - bf[0], bt[2] - f[2]),
    }[d]


AXIS = {'west': (0, False), 'east': (0, True), 'down': (1, False), 'up': (1, True),
        'north': (2, False), 'south': (2, True)}


def cut(lo, hi):
    """Splits a span at the 16-pixel block joins; a span inside one block stays whole."""
    edges = [lo] + [v for v in range(-16, 256, 16) if lo < v < hi] + [hi]
    return list(zip(edges, edges[1:]))


def faces_of(boxes, tex):
    faces = []
    for b in boxes:
        for x1, x2 in cut(b.f[0], b.t[0]):
            for y1, y2 in cut(b.f[1], b.t[1]):
                for z1, z2 in cut(b.f[2], b.t[2]):
                    f, t = (x1, y1, z1), (x2, y2, z2)
                    block = tuple(math.floor(min(a, c - 1e-6) / 16) * 16 if c - a > 0 else 0
                                  for a, c in zip(f, t))
                    lf = tuple(f[i] - block[i] for i in range(3))
                    lt = tuple(t[i] - block[i] for i in range(3))
                    for d, (axis, pos) in AXIS.items():
                        if (t if pos else f)[axis] != (b.t if pos else b.f)[axis]:
                            continue
                        if d in b.decals:
                            name, uv = b.decals[d], decal_uv(d, b.f, b.t, f, t)
                            w, h = len(tex[name][0]), len(tex[name])
                            uv = (uv[0] * 16 / w, uv[1] * 16 / h, uv[2] * 16 / w, uv[3] * 16 / h)
                        else:
                            name, uv = b.faces.get(d, b.material), mp.auto_uv(d, lf, lt)
                        origin, eu, ev = mp.face_frame(d, lf, lt)
                        origin = tuple(origin[i] + block[i] for i in range(3))
                        faces.append((d, origin, eu, ev, uv, tex[name], 0))
    return faces


# ---------------------------------------------------------------- publishing and export

DESIGNS = os.path.join(ROOT, 'art', 'designs')
FOLDER = 'block/pool/'


# Textures the game already has. The pool's water is vanilla's still water (animated, grey, and
# tinted by the biome's water colour, as real water blocks are); the preview draws its own blue.
VANILLA = {'water': 'minecraft:block/water_still'}


def canvas(name, tex):
    """The square canvas a texture goes on: 16, or the next multiple of 16 that holds a texture
    drawn at a larger size (a long decal, the fan's guard)."""
    h, w = len(tex[name]), len(tex[name][0])
    return 16 * max(1, math.ceil(max(w, h) / 16))


def publish_rows(name, pixels, size):
    """Writes a texture, placed top left on a size x size canvas, to art/textures and the mod."""
    img = [[(0, 0, 0, 0)] * size for _ in range(size)]
    for y, row in enumerate(pixels):
        for x, px in enumerate(row):
            img[y][x] = px
    for base in (ART_TEXTURES, MOD_TEXTURES):
        out = os.path.join(base, name + '.png')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        write_png(out, img)


def framed(inner):
    """A 16 x 16 block face: a pixel of graphite frame round a 14 x 14 picture, as each face of the
    formed pool reads (a frame on every block edge)."""
    img = [[(*[int(PAL['S'][i:i + 2], 16) for i in (0, 2, 4)], 255)] * 16 for _ in range(16)]
    for y, row in enumerate(inner):
        for x, px in enumerate(row):
            img[1 + y][1 + x] = px
    return img


def part_textures(tex):
    """The parts' own faces before the pool forms: the liner a framed plate, the controller the
    same with its console, the crane its yellow paint with a hazard band."""
    liner = framed(tex['panel_14x14'])
    controller = [row[:] for row in liner]
    for y, row in enumerate(tex['console']):
        for x, px in enumerate(row):
            controller[3 + y][3 + x] = px
    crane = framed(yellow(14, 14))
    band = stripes(14, 4)
    for y, row in enumerate(band):
        for x, px in enumerate(row):
            crane[6 + y][1 + x] = px
    return {'part_liner': liner, 'part_controller': controller, 'part_crane': crane}


def export_design(design_name, folder, designs, tex, size, existing=None):
    """Publishes every texture a multiblock design uses into folder and writes the design, one
    box list per state, to art/designs/<name>.json for datagen (DesignModel). size is in blocks
    (x, y, z); existing maps texture names to textures the mod already has."""
    existing = {**EXISTING, **(existing or {})}
    names = set()
    for boxes in designs.values():
        for b in boxes:
            names |= {b.material, *b.faces.values(), *b.decals.values()}
    textures = {}
    for name in sorted(names):
        # See-through (water, glass) draws in the translucent layer; everything else is solid, so
        # the block outline still shows over it.
        translucent = any(0 < px[3] < 255 for row in tex[name] for px in row)
        if name in VANILLA:
            textures[name] = {'path': VANILLA[name], 'canvas': 16, 'translucent': True, 'tint': 0}
            continue
        if name in existing:
            textures[name] = {'path': 'ryzergen:' + existing[name], 'canvas': 16, 'translucent': translucent}
            continue
        size_px = canvas(name, tex)
        publish_rows(folder + name, tex[name], size_px)
        textures[name] = {'path': 'ryzergen:' + folder + name, 'canvas': size_px, 'translucent': translucent}

    def box_json(b):
        out = {'material': b.material, 'from': list(b.f), 'to': list(b.t)}
        if b.faces:
            out['faces'] = b.faces
        if b.decals:
            out['decals'] = b.decals
        if b.glowing:
            out['glow'] = sorted(b.glowing)
        return out

    os.makedirs(DESIGNS, exist_ok=True)
    data = {
        'size': list(size),
        'textures': textures,
        'states': {state: [box_json(b) for b in boxes] for state, boxes in designs.items()},
    }
    with open(os.path.join(DESIGNS, design_name + '.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=1)
        f.write('\n')


def export(tex):
    """Publishes every texture the pool uses and writes the design for datagen: the three looks,
    and the crane's parts (states named crane_*, built whole rather than cut per block)."""
    designs = {state: design(state) for state in STATES}
    designs.update(crane_parts())
    export_design('spent_fuel_pool', FOLDER, designs, tex, (LONG // 16, HIGH // 16, WIDE // 16))
    for name, pixels in part_textures(tex).items():
        publish_rows(FOLDER + name, pixels, 16)
    for name in ('rod_side', 'rod_top'):
        publish_rows(FOLDER + name, tex[name], 16)


def main():
    args = sys.argv[1:]
    scale, state = 6, 'active'
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
    faces = faces_of(design(state) + crane_parked() + (preview_rods(12) if state == 'active' else []), tex)
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
