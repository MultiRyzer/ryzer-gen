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
})
d = Design(C, TEXTURES, {'glow', 'screen'}, concept_only={'rods'})

# ---------------------------------------------------------------- base
# A dark skirt, the plinth above it, and a flat console across the front for the ports.
d.cylinder('steel_dark', 0, 6, 6 * B - 3)
d.annulus('steel_dark', 6, 6 * B - 4, 6 * B - 3)
d.cylinder('plinth', 6, 16, 6 * B - 4, v0=0)
d.annulus('steel_dark', 16, 0, 6 * B - 4)
# The front four blocks of the base (cells 4 to 7 on the front row): water in, the control core,
# the fuel port and energy out. Each port is a 10 x 10 flange flush with its block's face (z = 0),
# drawn whole so its ring sits centred in its cell; the core shows its screen there.
FRONT = [('port_coolant', 4), ('core', 5), ('port_fuel', 6), ('port_energy', 7)]
d.box('console', 4 * B - 4, 0, 1.5, 8 * B + 4, 15, 2 * B, top='console_top', skip=('south', 'down'))
for kind, cell in FRONT:
    x = cell * B
    if kind == 'core':
        d.box('steel_dark', x + 3, 5, 0, x + 13, 11, 0.5, decals={'north': 'screen'}, skip=('south',))
        d.box('steel_dark', x + 1, 2, 0.5, x + 15, 14, 1.5, skip=('south',))
    else:
        d.box('steel_dark', x + 3, 3, 0, x + 13, 13, 1.5, decals={'north': kind}, skip=('south',))
# The output port (spent rods out) on the east side, cell 4 from the front: the same flange on a
# small side console, since the front has only four flat blocks.
OUTPUT_CELL = 4
z = OUTPUT_CELL * B
d.box('console', 11 * B, 0, z - 4, 12 * B - 1.5, 15, z + B + 4, top='console_top', skip=('west', 'down'))
d.box('steel_dark', 12 * B - 1.5, 3, z + 3, 12 * B, 13, z + 13, decals={'east': 'port_fuel'}, skip=('west',))

# ---------------------------------------------------------------- reactor chamber
# Smooth glass, drawn translucent: its outside, and its inside so the far wall tints the view too.
d.group = 'glass'
d.cylinder('glass', 16, 80, 6 * B - 10, n=48)
d.cylinder('glass', 16, 80, 6 * B - 10.5, n=48, inward=True)
d.group = 'static'
for k in range(12):
    d.post('rib', math.radians(k * 30 + 15), 6 * B - 10, 16, 80, 4, 4)
# Core: a raised pedestal, wide enough that the corner rods' collars (about 64 out) sit fully on it.
d.disc('steel_dark', 16, 22, 4.4 * B, sides='hazard', bottom=False)
d.disc('steel', 22, 24, 4.2 * B, bottom=False)
# The 5 x 5 channels (the game draws these from the player's layout; this is one for the picture).
LAYOUT = [
    'CMFMC',
    'MFMFM',
    'FMXMF',
    'MFMFM',
    'CMFMC',
]
ROD = {'F': 'glow', 'M': 'steel_dark', 'X': 'steel', 'C': 'copper'}
d.group = 'rods'
for row, line in enumerate(LAYOUT):
    for col, kind in enumerate(line):
        x = C + (col - 2) * 20
        z = C + (row - 2) * 20
        d.box(ROD[kind], x - 4, 27, z - 4, x + 4, 80, z + 4, skip=('up',))
        d.box('steel_dark', x - 5, 24, z - 5, x + 5, 27, z + 5, skip=('down',))
d.group = 'static'

# ---------------------------------------------------------------- head
d.disc('steel_dark', 80, 96, 6 * B - 6)
d.cylinder('glow', 88, 89, 6 * B - 5.5)

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
# Around the rotor: open louvres between ribs, so the turbine can be seen turning.
for k in range(24):
    d.post('rib', math.radians(k * 15), 6 * B - 9, 96, 124, 4, 4)
d.wall('steel_dark', 96, 98, 6 * B - 6, 6 * B - 12)
# Above: a hyperbolic stack, narrowest two thirds of the way up, flaring to an open top.
TOP = 176


def stack_r(y):
    t = (y - 158) / 34
    return (5.0 * B) * math.sqrt(1 + 0.22 * t * t)


# One band per block of height, so each band shows whole plates.
heights = list(range(124, TOP, 16)) + [TOP]
outer = [(y, stack_r(y)) for y in heights]
bands = ['steel_dark'] + ['steel'] * (len(heights) - 3) + ['steel_dark']
d.lathe('steel', outer, n=48, mats=bands)
d.lathe('steel_dark', [(y, r - 5) for y, r in outer], n=48, inward=True)
d.annulus('steel_dark', TOP, stack_r(TOP) - 5, stack_r(TOP), n=48)
d.annulus('steel_dark', 124, stack_r(124) - 5, stack_r(124), up=False, n=48)
d.cylinder('glow', 140, 141, stack_r(140) + 0.5, n=48)

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
