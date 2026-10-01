"""The fission power station's formed look (design section 8), drawn as smooth quads.

The control core draws the whole station from this design once it forms (StationRenderer, from a
GPU mesh): the round parts are smooth surfaces of revolution and upright bars square to the circle,
the straight parts boxes (see quad_design.py). The chamber glass is a smooth translucent tint, the
rotor spins, and the rods are drawn by the game from the player's core layout (here they are drawn
for the picture only). Run:
    python art/tools/fission_concept.py [OUT.png]

Layout, bottom to top (block units, 12 across, centred on 6, 6). The game places the rods, the haze
and the glass light by these numbers, so keep them if the look changes:
    0-1    base plinth, with the ports on a flat console at the front and one on the east side
    1-5    reactor chamber: a glass ring (radius 86 pixels) between steel ribs, the 5 x 5 core inside
           (rods 20 pixels apart, standing on a pedestal at 24 up to the head at 80)
    5-6    reactor head: a dark band with a cyan light strip
    6-8    the roof is one giant turbine rotor, seen through open louvres
    8-11   the outer wall rises into a hyperbolic steam stack, open at the top
"""
import math
import os
import sys

import model_preview as mp
import big_machine_textures
from quad_design import Design

B = 16  # pixels per block
C = 6 * B  # centre of the footprint, in pixels

TEXTURES = {name: 'ryzergen:block/microreactor/' + name for name in (
    'steel', 'steel_dark', 'copper', 'lead', 'hazard', 'glow', 'screen', 'port_coolant', 'port_fuel', 'port_energy')}
TEXTURES.update({
    'glass': 'ryzergen:block/machine/smooth_glass',
    'console': 'ryzergen:block/machine/console',
    'console_top': 'ryzergen:block/machine/console_top',
    'rib': 'ryzergen:block/station/rib',
    'plinth': 'ryzergen:block/station/plinth',
    'front_panel': 'ryzergen:block/station/front_panel',
})
STATION = ('skirt', 'deck', 'core_plate', 'pillar', 'rib_web', 'clamp', 'copper_pipe', 'head', 'head_top', 'rim',
           'column', 'vane', 'stack_a', 'stack_b', 'stack_base', 'stack_inner', 'warning', 'beacon',
           'stencil_a', 'stencil_b', 'hatch_a', 'hatch_b',
           'fuel_pin', 'spacer', 'nozzle', 'cherenkov', 'moderator', 'control', 'steam',
           'housing', 'housing_slope', 'housing_cheek', 'hazard_upright', 'tag_coolant', 'tag_fuel', 'tag_energy')
TEXTURES.update({name: 'ryzergen:block/station/' + name for name in STATION})
d = Design(C, TEXTURES, {'glow', 'screen', 'beacon', 'fuel_pin', 'cherenkov', 'tag_coolant', 'tag_fuel', 'tag_energy'}, concept_only={'rods', 'plumes'})
FRONT = math.radians(270)   # the angle round the station that faces the front


def band(mat_of, y1, y2, r1, r2, n=48, v0=None):
    """One ring band from (y1, r1) to (y2, r2), its texture picked per step by `mat_of(k)`."""
    h = min(16, math.hypot(y2 - y1, r2 - r1))
    top = 16 - h if v0 is None else v0
    for k in range(n):
        p0, p1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        pm = (p0 + p1) / 2
        dy, dr = y2 - y1, r2 - r1
        out = (dy * math.cos(pm), -dr, dy * math.sin(pm))
        d.quad(mat_of(k), [d.at(p0, r2, y2), d.at(p0, r1, y1), d.at(p1, r1, y1), d.at(p1, r2, y2)],
               [(0, top), (0, top + h), (16, top + h), (16, top)], out)


def panels(offset, specials=None):
    """Cladding two steps wide: step k is the left or right half of its panel. `specials` maps a
    panel's first step to a two-step picture drawn there instead (a hatch, the stencil)."""
    specials = specials or {}

    def mat(k):
        first = k - ((k - offset) % 2)
        if first in specials:
            return specials[first] + ('_a' if k == first else '_b')
        return 'stack_a' if k == first else 'stack_b'
    return mat

# ---------------------------------------------------------------- base
# A dark skirt, the plinth above it, and a flat console across the front for the ports.
d.cylinder('skirt', 0, 6, 6 * B - 3)
d.annulus('rim', 6, 6 * B - 4, 6 * B - 3)
d.cylinder('plinth', 6, 16, 6 * B - 4, v0=0)
d.annulus('deck', 16, 0, 6 * B - 4)
# The front panel: two blocks wide (the core in cell 5 and the block beside it in cell 6, the only
# blocks that open the screen), centred on the station. Its face is one texture drawn once across
# both blocks (station/front_panel, 32 x 16, rows 1 to 15), and the core's screen stands proud of it.
PANEL_Z = 1.5
d.box('console', C - 16, 0, PANEL_Z, C + 16, 15, 2 * B, top='console_top', skip=('south', 'down', 'north'))
# Seen from the front the viewer's left is the east (+x) end, so u runs from x = C + 16 to C - 16.
d.quad('front_panel', [(C + 16, 15, PANEL_Z), (C - 16, 15, PANEL_Z), (C - 16, 0, PANEL_Z), (C + 16, 0, PANEL_Z)],
       [(0, 1), (16, 1), (16, 16), (0, 16)], (0, 0, -1))
d.box('steel_dark', C - 5, 5, PANEL_Z - 1, C + 5, 11, PANEL_Z - 0.5, decals={'north': 'screen'}, skip=('south',))
d.box('steel_dark', C - 6, 4, PANEL_Z - 0.5, C + 6, 12, PANEL_Z, skip=('south',))
# The ports, on consoles across the two flat sides (cells 5 and 6 from the front), so pipes and
# cables come in from the sides and the front stays clear (StationLayout.PORTS). As you face the
# front: water and fuel in on your left (east), energy and spent rods out on your right (west).
# Each is a 10 x 10 flange flush with its block's face, drawn whole so its ring sits centred.
SIDES = [('east', [('port_coolant', 5), ('port_fuel', 6)]), ('west', [('port_energy', 5), ('port_fuel', 6)])]
# Each side's ports sit in a housing where the lines pass into the station (a containment
# penetration, as real plants call it): a light face holding the ports, a graphite hood sloping
# back over them with a light line and a lit tag above each port in its ring colour, and graphite
# cheeks with hazard stripes either end. It rises between two chamber ribs, which frame it.
TAGS = {'port_coolant': 'tag_coolant', 'port_fuel': 'tag_fuel', 'port_energy': 'tag_energy'}
Z1, Z2 = 5 * B - 4, 7 * B + 4
FACE = 12 * B - 1.5        # the housing's face (the port flanges stand 1.5 proud of it, flush with the block)
HOOD_BACK, HOOD_TOP = 11 * B - 4, 26


def housing(side, ports):
    east = side == 'east'
    out, inside = ('east', 'west') if east else ('west', 'east')

    def mx(x1, x2):
        return (x1, x2) if east else (12 * B - x2, 12 * B - x1)

    def px(x):
        return x if east else 12 * B - x

    def box(mat, x1, y1, z1, x2, y2, z2, **kw):
        a, b = mx(x1, x2)
        d.box(mat, a, y1, z1, b, y2, z2, **kw)

    box('housing', 11 * B - 4, 0, Z1 + 3, FACE, 16, Z2 - 3, skip=(inside, 'down', 'up'))
    # The hood: from the face's top edge back and up to the housing's top.
    sx = 1 if east else -1
    d.quad('housing_slope', [(px(HOOD_BACK), HOOD_TOP, Z2 - 3), (px(FACE), 16, Z2 - 3), (px(FACE), 16, Z1 + 3),
                             (px(HOOD_BACK), HOOD_TOP, Z1 + 3)],
           [(0, 0), (0, 16), (16, 16), (16, 0)], (sx * 10, FACE - HOOD_BACK, 0))
    box('housing_cheek', HOOD_BACK - 4, 16, Z1 + 3, HOOD_BACK, HOOD_TOP, Z2 - 3, skip=(inside, 'down'))
    # Cheeks either end, standing proud and a little taller, hazard striped on the front.
    for z1, z2 in ((Z1, Z1 + 3), (Z2 - 3, Z2)):
        box('housing_cheek', HOOD_BACK - 4, 0, z1, FACE + 1, HOOD_TOP + 2, z2, skip=('down', inside))
        box('hazard_upright', FACE + 1, 0, z1, FACE + 1.5, HOOD_TOP + 2, z2, skip=('down', inside))
    # A slim light bar on the hood above each port, in its ring colour, kept clear of
    # the chamber glass behind.
    lift = 0.3
    for kind, cell in ports:
        z = cell * B
        t0, t1 = 0.2, 0.33
        xa, ya = FACE + (HOOD_BACK - FACE) * t1, 16 + (HOOD_TOP - 16) * t1
        xb, yb = FACE + (HOOD_BACK - FACE) * t0, 16 + (HOOD_TOP - 16) * t0
        d.quad(TAGS[kind], [(px(xa) + sx * lift, ya + lift, z + 14), (px(xb) + sx * lift, yb + lift, z + 14),
                            (px(xb) + sx * lift, yb + lift, z + 2), (px(xa) + sx * lift, ya + lift, z + 2)],
               [(0, 0), (0, 3), (12, 3), (12, 0)], (sx * 10, FACE - HOOD_BACK, 0))


for side, ports in SIDES:
    housing(side, ports)
    for kind, cell in ports:
        z = cell * B
        if side == 'east':
            d.box('steel_dark', 12 * B - 1.5, 3, z + 3, 12 * B, 13, z + 13, decals={'east': kind}, skip=('west',))
        else:
            d.box('steel_dark', 0, 3, z + 3, 1.5, 13, z + 13, decals={'west': kind}, skip=('east',))

# ---------------------------------------------------------------- reactor chamber
# Smooth glass, drawn translucent: its outside, and its inside so the far wall tints the view too.
d.group = 'glass'
d.cylinder('glass', 16, 80, 6 * B - 10, n=48)
d.cylinder('glass', 16, 80, 6 * B - 10.5, n=48, inward=True)
d.group = 'static'
# Twelve I-beam ribs hold the head up: a light outer flange, a graphite web through the glass and
# an inner flange, standing in gunmetal shoes, clipped to the glass halfway up, collared under the
# head. Every other rib carries a copper coolant downcomer from the head into the plinth, the way
# a real reactor's loops run (and the colour tells you where the water goes). Nothing reaches past
# the plinth's edge (radius 92), so the base keeps a clean line.
for k in range(12):
    a = math.radians(k * 30 + 15)
    d.post('pillar', a, 90.5, 16, 79, 1, 4)                 # outer flange (ends inside the collar and stops
    # short of the clamps' faces: no two faces share a plane, which would flicker)
    d.post('rib_web', a, 85.25, 16, 79, 4.25, 1.5)              # web, through the glass
    d.post('rib_web', a, 80.5, 16, 79, 1, 3)              # inner flange
    d.post('clamp', a, 85.5, 16, 21, 6.5, 5.5)                # shoe
    d.post('clamp', a, 86.5, 46, 50, 5.5, 4.5)            # glass clip
    d.post('clamp', a, 85.75, 74, 80, 6.25, 5)                # head collar
    if k % 2 == 0:
        pa = a + 8.5 / 90.5
        d.post('copper_pipe', pa, 90.25, 21, 80, 1.25, 1.25)
        d.post('clamp', pa, 90, 16, 21, 2, 2.5)         # where it enters the plinth
        # The glass clip reaches out to hold the pipe.
        d.post('clamp', a + 5 / 90.5, 90.5, 46.5, 49.5, 1.5, 4.5)
# Core: a raised pedestal, wide enough that the corner rods' collars (about 64 out) sit fully on it.
d.disc('steel_dark', 16, 22, 4.4 * B, sides='hazard', bottom=False)
d.disc('core_plate', 22, 24, 4.2 * B, bottom=False)
# The 5 x 5 channels (the game draws these from the player's layout; this is one for the picture).
LAYOUT = [
    'CMFMC',
    'MFMFM',
    'FMXMF',
    'MFMFM',
    'CMFMC',
]
# Each kind of channel has its own look, so a layout reads at a glance:
#   F fuel: a bundle of four green-glowing pins held by bright spacer grids, with end fittings,
#     as real fuel assemblies are built
#   C coolant: a column of water glowing Cherenkov blue, in copper hoops, with steam rising off it
#   M moderator: a column of graphite bricks keyed together, banded with steel between them, on
#     an end fitting with a cap, as the fuel's bundles stand
#   X control: a polished steel rod on a drive shaft, part way in (its depth marks show travel)
d.group = 'rods'


# The moderator's three graphite bricks, with a steel band in each gap between them.
MODERATOR_BRICKS = ((31, 45), (46.5, 60.5), (62, 76))


def rod(kind, x, z):
    d.box('steel_dark', x - 5, 24, z - 5, x + 5, 27, z + 5, skip=('down',))
    if kind == 'F':
        d.box('nozzle', x - 4.5, 27, z - 4.5, x + 4.5, 30, z + 4.5)
        for dx in (-3.5, 0.5):
            for dz in (-3.5, 0.5):
                d.box('fuel_pin', x + dx, 30, z + dz, x + dx + 3, 76, z + dz + 3, skip=('up', 'down'))
        for y in (41, 55, 69):
            d.box('spacer', x - 4.5, y, z - 4.5, x + 4.5, y + 1.5, z + 4.5)
        d.box('nozzle', x - 4.5, 76, z - 4.5, x + 4.5, 80, z + 4.5, skip=('up',))
    elif kind == 'C':
        d.box('cherenkov', x - 2.5, 27, z - 2.5, x + 2.5, 80, z + 2.5, skip=('up',))
        for y in range(29, 79, 8):
            d.box('copper', x - 3.5, y, z - 3.5, x + 3.5, y + 2, z + 3.5)
    elif kind == 'M':
        d.box('nozzle', x - 4.5, 27, z - 4.5, x + 4.5, 31, z + 4.5)
        for y1, y2 in MODERATOR_BRICKS:
            d.box('moderator', x - 4, y1, z - 4, x + 4, y2, z + 4, skip=('up', 'down'))
        for y in (45, 60.5):
            d.box('spacer', x - 4.5, y, z - 4.5, x + 4.5, y + 1.5, z + 4.5)
        d.box('nozzle', x - 4.5, 76, z - 4.5, x + 4.5, 80, z + 4.5, skip=('up',))
    elif kind == 'X':
        d.box('control', x - 3, 44, z - 3, x + 3, 72, z + 3)
        d.box('spacer', x - 1, 72, z - 1, x + 1, 80, z + 1, skip=('up',))
        d.box('nozzle', x - 4.5, 27, z - 4.5, x + 4.5, 31, z + 4.5)   # the guide it drops into


for row, line in enumerate(LAYOUT):
    for col, kind in enumerate(line):
        rod(kind, C + (col - 2) * 20, C + (row - 2) * 20)

# Steam, drawn translucent and animated in the game, fading in with the station's power: a plume
# rising off each coolant channel (two crossed sheets, like vanilla fire; the game draws these from
# the player's layout, see StationRenderer), and a veil just inside the glass, from the pedestal to
# the head, so the whole chamber looks full of it.
d.group = 'plumes'
for row, line in enumerate(LAYOUT):
    for col, kind in enumerate(line):
        if kind == 'C':
            x, z = C + (col - 2) * 20, C + (row - 2) * 20
            for sx, sz in ((1, 0), (0, 1)):
                pts = [(x - 7 * sx, 80, z - 7 * sz), (x - 7 * sx, 30, z - 7 * sz), (x + 7 * sx, 30, z + 7 * sz),
                       (x + 7 * sx, 80, z + 7 * sz)]
                uvs = [(0, 0), (0, 16), (16, 16), (16, 0)]
                d.quad('steam', pts, uvs, (sz, 0, -sx))
                d.quad('steam', pts, uvs, (-sz, 0, sx))
d.group = 'steam'
# One band per block of height, so the wisps keep their scale.
d.lathe('steam', [(y, 78) for y in (24, 40, 56, 72, 80)], n=32)
d.group = 'static'

# ---------------------------------------------------------------- head
# 48 steps round, like the stack and the louvre ribs above, so every ring's seams line up.
d.disc('head', 80, 96, 6 * B - 6, n=48, top='head_top')
d.cylinder('glow', 88, 89, 6 * B - 5.5, n=48)

# ---------------------------------------------------------------- turbine
# The whole roof is one turbine: steam rising from the core drives a rotor as wide as the station,
# then leaves up the stack. In game the rotor is the one moving part (the 'rotor' group spins).
RY = 100
d.group = 'rotor'
d.disc('lead', 96, 116, 14, n=24)                       # hub
d.disc('steel_dark', 116, 120, 10, bottom=False, n=24)  # hub cap
for k in range(16):
    a = math.radians(k * 22.5)
    # Blades from the hub to inside the shroud (inner radius 78), widening as they go out and
    # pitched 30 degrees, so the rotor reads as one big fan.
    radii = [14, 28, 42, 56, 70]
    path = [d.at(a, r, RY + 2) for r in radii]
    tangential = (-math.sin(a), 0, math.cos(a))
    pitch = math.radians(30)
    blade = tuple(math.cos(pitch) * tangential[i] + math.sin(pitch) * (0, 1, 0)[i] for i in range(3))
    d.sweep('steel', path, 1, lambda i: 3 + radii[i] / 28, lambda i: blade, closed=False, caps=True)
d.group = 'static'
# Shroud round the blade tips, inside the louvre ribs.
d.wall('steel_dark', 98, 108, 82, 78)
# A bearing beam across the stack holds the hub (the second in two halves, so the crossing is clean).
d.box('steel_dark', C - 78, 116, C - 4, C + 78, 122, C + 4)
d.box('steel_dark', C - 4, 116, C - 78, C + 4, 122, C - 4, skip=('south',))
d.box('steel_dark', C - 4, 116, C + 4, C + 4, 122, C + 78, skip=('north',))
# Struts from the beam ends down to the shroud, so the bearing sits on something.
for sx, sz in ((C - 80, C), (C + 80, C), (C, C - 80), (C, C + 80)):
    d.box('steel_dark', sx - 3, 108, sz - 3, sx + 3, 116, sz + 3, skip=('down', 'up'))

# ---------------------------------------------------------------- the outer wall rises into a steam stack
# Around the rotor: open louvres between ribs, so the turbine can be seen turning. The ribs sit on
# every other seam of the 48-step rings, their outer faces just inside the stack's rim (88.4 at its
# foot), so the stack reads as resting squarely on them.
# Eight I-beam columns carry the stack; between each pair, two vanes pitched like a turbine's
# stator ring guide the air in round the rotor.
for k in range(24):
    a = math.radians(k * 15)
    if k % 3 == 0:
        d.post('column', a, 89.5, 98, 124, 1.5, 3.5)       # outer flange
        d.post('rib_web', a, 84.5, 98, 124, 3.5, 1.5)      # web
        d.post('column', a, 80, 98, 124, 1, 2.5)           # inner flange
    else:
        radial = (math.cos(a), 0, math.sin(a))
        round_dir = (-math.sin(a), 0, math.cos(a))
        pitch = math.radians(40)
        wide = tuple(math.cos(pitch) * radial[i] + math.sin(pitch) * round_dir[i] for i in range(3))
        path = [d.at(a, 85, y) for y in (98, 114, 124)]
        d.sweep('vane', path, 0.75, 5, lambda i: wide, closed=False, caps=True, along_v=True)
d.wall('rim', 96, 98, 6 * B - 6, 6 * B - 12, n=48)
# Above: a hyperbolic stack, narrowest two thirds of the way up, flaring to an open top.
TOP = 176


def stack_r(y):
    t = (y - 158) / 34
    return (5.0 * B) * math.sqrt(1 + 0.22 * t * t)


# A dark foot, two bands of cladding (panels two steps wide, staggered like real cladding, with the
# station's number and an access hatch facing the front), and the red and white aviation marking
# real tall stacks carry, with a red beacon at each quarter.
heights = [124, 134, 150, 164, TOP]
outer = [(y, stack_r(y)) for y in heights]
front_step = round(FRONT / (2 * math.pi) * 48)   # the step just left of front centre
mats = [lambda k: 'stack_base',
        panels((front_step - 1) % 2, {front_step - 1: 'stencil', front_step - 7: 'hatch'}),
        panels(front_step % 2),
        lambda k: 'warning']
for i in range(len(outer) - 1):
    (y1, r1), (y2, r2) = outer[i], outer[i + 1]
    band(mats[i], y1, y2, r1, r2)
d.lathe('stack_inner', [(y, r - 5) for y, r in outer], n=48, inward=True)
d.annulus('rim', TOP, stack_r(TOP) - 5, stack_r(TOP), n=48)
d.annulus('rim', 124, stack_r(124) - 5, stack_r(124), up=False, n=48)
d.cylinder('glow', 134, 135, stack_r(134) + 0.5, n=48)
for k in range(4):
    d.post('beacon', math.radians(45 + 90 * k), stack_r(168) + 1, 166, 170, 1.5, 1.5)

# ---------------------------------------------------------------- export and picture
GAME_DATA = os.path.join(mp.ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'station', 'fission_station.json')


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'fission_station.png')
    big_machine_textures.publish_all()
    d.export(GAME_DATA)
    # From above at the usual angle, and at a player's eye level (looking through the glass).
    d.save_png(out, [((1, 1), 3, 0.5), ((1, 0.35), 3, 0.15)])


if __name__ == '__main__':
    main()
