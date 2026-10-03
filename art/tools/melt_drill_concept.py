"""The Melt Drill's concept (design section 11c), drawn as smooth quads, not yet in the game. Run:
    python art/tools/melt_drill_concept.py [OUT.png]

One machine as big as what it mines: it stands over a chunk quarter (8 x 8 blocks).
- The base: a slab-high frame round the quarter's edge, hazard striped outside, with cross girders
  dividing it into four 4 x 4 cells, each open to the ground beneath.
- The beam chamber: the bottom storey, three blocks high, walled in glass between white pillars, so
  you watch the drill work. From its ceiling hang the emitters, up to four, one over each cell (the
  miners, crafted on their own and fitted: a rig drills faster as emitters are added, rule 11).
  Each is a graphite cone pointing down, its lens glowing, and its beam runs straight down through
  the cell into the shaft, to the hot spot glowing where it melts the rock.
- The drill core above: the beam generator (a gyrotron, the microwave source that heats fusion
  plasma), a light-cased block of panels between graphite bands, its front carrying the controller
  (the station's front panel and screen, as the other big machines), with copper cooling lines up
  its corners and the round resonator drum on its roof, a light ring and a beacon on top.
- Below, cut away: each emitter's 4 x 4 shaft, its walls glazed in fused rock, the hot spot at the
  bottom.
Real basis: millimetre-wave drilling, where a gyrotron's beam vaporises rock at the bottom of a
borehole, the vapour is blown back up it, and the walls glaze to glass.
"""
import math
import os
import sys

import model_preview as mp
import breeder_textures
from quad_design import Design

B = 16
SIZE = 8 * B           # a chunk quarter
C = SIZE / 2
BASE = 8               # the base frame is a slab high
CELL = 4 * B           # each emitter's column
CHAMBER = (BASE, BASE + 3 * B)      # the glass beam chamber
CORE = (CHAMBER[1] + 6, CHAMBER[1] + 6 + 4 * B)   # the drill core's casing
DEPTH = 56             # how far the cutaway shows below the base

TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in ('hazard', 'glow', 'screen')})
TEXTURES['front_panel'] = 'ryzergen:block/breeder/front_panel'
TEXTURES['glass'] = 'ryzergen:block/breeder/glass'
d = Design(C, TEXTURES, {'glow', 'screen', 'breeder_amber'})
CELLS = [(C - CELL / 2, C - CELL / 2), (C + CELL / 2, C - CELL / 2), (C - CELL / 2, C + CELL / 2), (C + CELL / 2, C + CELL / 2)]


def centred(x, z, draw):
    """Draws round a centre other than the machine's."""
    whole = d.at
    d.at = lambda phi, rad, y: (x + rad * math.cos(phi), y, z + rad * math.sin(phi))
    try:
        draw()
    finally:
        d.at = whole


# ---------------------------------------------------------------- the base
EDGE = B
for x1, z1, x2, z2 in ((0, 0, SIZE, EDGE), (0, SIZE - EDGE, SIZE, SIZE), (0, EDGE, EDGE, SIZE - EDGE), (SIZE - EDGE, EDGE, SIZE, SIZE - EDGE)):
    d.box('breeder_bund', x1, 0, z1, x2, BASE, z2, top='breeder_grate', skip=('down',))
for box in ((-0.5, 0, -0.5, SIZE + 0.5, BASE, 0), (-0.5, 0, SIZE, SIZE + 0.5, BASE, SIZE + 0.5),
            (-0.5, 0, 0, 0, BASE, SIZE), (SIZE, 0, 0, SIZE + 0.5, BASE, SIZE)):
    d.box('hazard', *box, skip=('down',))
BEAM = 4
d.box('breeder_girder', C - BEAM, 0, EDGE, C + BEAM, BASE, SIZE - EDGE, top='breeder_grate', skip=('down',))
d.box('breeder_girder', EDGE, 0, C - BEAM, C - BEAM, BASE, C + BEAM, top='breeder_grate', skip=('down',))
d.box('breeder_girder', C + BEAM, 0, C - BEAM, SIZE - EDGE, BASE, C + BEAM, top='breeder_grate', skip=('down',))


# ---------------------------------------------------------------- the shafts, cut away below
def shaft(cx, cz):
    """An emitter's 4 x 4 shaft: fused rock walls facing in (the near ones drop out of the
    picture), and the hot spot glowing at the bottom."""
    x1, x2, z1, z2 = cx - CELL / 2, cx + CELL / 2, cz - CELL / 2, cz + CELL / 2
    uv = [(0, 0), (16, 0), (16, 16), (0, 16)]
    for y in range(-DEPTH, 0, B):
        top = min(0, y + B)
        d.quad('breeder_graphite', [(x1, top, z1), (x2, top, z1), (x2, y, z1), (x1, y, z1)], uv, (0, 0, 1))
        d.quad('breeder_graphite', [(x2, top, z2), (x1, top, z2), (x1, y, z2), (x2, y, z2)], uv, (0, 0, -1))
        d.quad('breeder_graphite', [(x1, top, z2), (x1, top, z1), (x1, y, z1), (x1, y, z2)], uv, (1, 0, 0))
        d.quad('breeder_graphite', [(x2, top, z1), (x2, top, z2), (x2, y, z2), (x2, y, z1)], uv, (-1, 0, 0))
    d.quad('breeder_amber', [(x1, -DEPTH, z1), (x2, -DEPTH, z1), (x2, -DEPTH, z2), (x1, -DEPTH, z2)], uv, (0, 1, 0))


for cx, cz in CELLS:
    shaft(cx, cz)

# ---------------------------------------------------------------- the beam chamber
# White pillars at the corners and the middle of each side, glass between them, a graphite ceiling
# slab with a light strip round it.
PILLAR = 5
for px in (0, C - PILLAR / 2, SIZE - PILLAR):
    for pz in (0, C - PILLAR / 2, SIZE - PILLAR):
        if px == C - PILLAR / 2 and pz == C - PILLAR / 2:
            continue
        d.box('breeder_leg', px, CHAMBER[0], pz, px + PILLAR, CHAMBER[1], pz + PILLAR, skip=('down', 'up'))
INSET = 1.5
d.group = 'glass'
for a1, a2 in ((PILLAR, C - PILLAR / 2), (C + PILLAR / 2, SIZE - PILLAR)):
    for y in range(CHAMBER[0], CHAMBER[1], B):
        y2 = min(CHAMBER[1], y + B)
        for z, out in ((INSET, (0, 0, -1)), (SIZE - INSET, (0, 0, 1))):
            for sign in (1, -1):
                d.quad('glass', [(a1, y2, z), (a2, y2, z), (a2, y, z), (a1, y, z)], [(0, 0), (16, 0), (16, 16), (0, 16)],
                       tuple(sign * v for v in out))
        for x, out in ((INSET, (-1, 0, 0)), (SIZE - INSET, (1, 0, 0))):
            for sign in (1, -1):
                d.quad('glass', [(x, y2, a1), (x, y2, a2), (x, y, a2), (x, y, a1)], [(0, 0), (16, 0), (16, 16), (0, 16)],
                       tuple(sign * v for v in out))
d.group = 'static'
d.box('breeder_graphite', 0, CHAMBER[1], 0, SIZE, CORE[0], SIZE)
d.box('glow', -0.2, CHAMBER[1] + 2, -0.2, SIZE + 0.2, CHAMBER[1] + 3, SIZE + 0.2, skip=('up', 'down'))


# ---------------------------------------------------------------- the emitters and their beams
def emitter(cx, cz):
    """An emitter hung from the ceiling: a mount, a graphite cone pointing down, its lens glowing,
    and the beam straight down through the cell to the bottom of the shaft."""
    tip = CHAMBER[1] - 22
    d.box('breeder_fitting', cx - 8, CHAMBER[1] - 4, cz - 8, cx + 8, CHAMBER[1], cz + 8, skip=('up',))
    centred(cx, cz, lambda: (
        d.cylinder('breeder_hub', CHAMBER[1] - 10, CHAMBER[1] - 4, 7, n=12),
        d.annulus('breeder_fitting', CHAMBER[1] - 10, 0, 7, up=False, n=12),
        d.lathe('breeder_graphite', [(tip + 2, 2), (CHAMBER[1] - 10, 6)], n=12),
        d.cylinder('glow', tip, tip + 2, 2, n=8),
        d.cylinder('glow', -DEPTH + 1, tip, 0.9, n=6)))


for cx, cz in CELLS:
    emitter(cx, cz)

# ---------------------------------------------------------------- the drill core
CASE = 4
d.box('breeder_hub', CASE, CORE[0], CASE, SIZE - CASE, CORE[1], SIZE - CASE, top='breeder_grate', skip=('down',))
for y in (CORE[0], CORE[1] - 4):
    d.box('breeder_graphite', CASE - 1, y, CASE - 1, SIZE - CASE + 1, y + 4, SIZE - CASE + 1, skip=('down',) if y == CORE[0] else ())
d.box('glow', CASE - 1.2, CORE[1] - 2.5, CASE - 1.2, SIZE - CASE + 1.2, CORE[1] - 1.5, SIZE - CASE + 1.2, skip=('up', 'down'))
# Copper cooling lines up the corners.
for px in (CASE + 4, SIZE - CASE - 4):
    for pz in (CASE + 4, SIZE - CASE - 4):
        ox = -1 if px < C else 1
        oz = -1 if pz < C else 1
        centred(px + ox * 3.5, pz + oz * 3.5, lambda: (d.cylinder('breeder_pipe', CORE[0] + 4, CORE[1] - 4, 2, n=8),))
# The controller on the front: the station's front panel with the screen in its bezel.
FACE = CASE - 1.5
y0 = CORE[0] + 8
d.quad('front_panel', [(C + B, y0 + 15, FACE), (C - B, y0 + 15, FACE), (C - B, y0, FACE), (C + B, y0, FACE)],
       [(0, 1), (16, 1), (16, 16), (0, 16)], (0, 0, -1))
d.box('breeder_graphite', C - B - 1, y0 - 1, FACE, C + B + 1, y0 + 16, CASE, skip=('south',))
d.box('breeder_fitting', C - 5, y0 + 5, FACE - 1, C + 5, y0 + 11, FACE - 0.5, decals={'north': 'screen'}, skip=('south',))
d.box('breeder_fitting', C - 6, y0 + 4, FACE - 0.5, C + 6, y0 + 12, FACE, skip=('south',))
# The resonator drum on the roof, a light ring round it, and a beacon on top.
DRUM = (CORE[1], CORE[1] + 22)
d.cylinder('breeder_steel_pipe', DRUM[0], DRUM[1], 30, n=24)
d.cylinder('breeder_graphite', DRUM[0] + 9, DRUM[0] + 13, 30.2, n=24)
d.cylinder('glow', DRUM[0] + 10.5, DRUM[0] + 11.5, 30.4, n=24)
d.annulus('breeder_fitting', DRUM[1], 0, 30, n=24)
d.cylinder('breeder_graphite', DRUM[1], DRUM[1] + 6, 8, n=12)
d.cylinder('breeder_amber', DRUM[1] + 6, DRUM[1] + 9, 3, n=8)
d.annulus('breeder_fitting', DRUM[1] + 9, 0, 3, n=8)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'melt_drill.png')
    breeder_textures.main()
    d.save_png(out, [((1, 1), 2.6, 0.4), ((-1, 0.5), 2.6, 0.25)])


if __name__ == '__main__':
    main()
