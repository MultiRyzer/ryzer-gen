"""The Melt Drill's concept (design section 11c): a first sketch, drawn as smooth quads, not yet in
the game. Run:
    python art/tools/melt_drill_concept.py [OUT.png]

The rig is as big as what it mines: a deck that outlines a chunk quarter (8 x 8 blocks) on the
surface, divided by cross girders into four 4 x 4 cells. Each cell takes a miner, crafted on its own
and mounted on the deck, so a rig mines faster as miners are added (upgrade, don't replace: rule
11). Up to 4 miners per quarter.
- The deck: a slab-high girder frame round the quarter's edge with hazard stripes on its outer face
  (melting rock is dangerous), grating on top, and two cross girders between the cells.
- A miner: a white four-legged derrick standing over its cell, its legs on the deck, braced on each
  face, with a graphite crown and a light strip on top. Hanging in it, the heat unit (the drill's
  melt furnace) with a glowing amber band, and from it the drill string straight down into the
  shaft, to the melt head glowing at the bottom.
- Below the deck, cut away: each miner's 4 x 4 shaft with its walls glazed in fused rock, and the
  molten pool at the bottom.
- The rig's controller on the front of the deck, the station's front panel and screen, so all
  three big machines share a face. A pipe from each miner's drill string runs along the deck's
  girders into it: the drill string brings everything up, and the controller holds it all.
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
DECK = 8               # the deck is a slab high
CELL = 4 * B           # each miner's column
TOP = 64               # the derricks' crowns
STRING = 2.5           # the drill string's radius
DEPTH = 56             # how far the cutaway shows below the deck

TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in ('hazard', 'glow', 'screen')})
TEXTURES['front_panel'] = 'ryzergen:block/breeder/front_panel'
d = Design(C, TEXTURES, {'glow', 'screen', 'breeder_amber'})

ALL = ('north', 'south', 'east', 'west', 'up', 'down')


def only(*faces):
    return tuple(f for f in ALL if f not in faces)


# ---------------------------------------------------------------- the deck
# The frame round the quarter's edge, hazard striped outside, grated on top.
EDGE = B
for x1, z1, x2, z2 in ((0, 0, SIZE, EDGE), (0, SIZE - EDGE, SIZE, SIZE), (0, EDGE, EDGE, SIZE - EDGE), (SIZE - EDGE, EDGE, SIZE, SIZE - EDGE)):
    d.box('breeder_bund', x1, 0, z1, x2, DECK, z2, top='breeder_grate', skip=('down',))
for face, box in (('north', (-0.5, 0, -0.5, SIZE + 0.5, DECK, 0)), ('south', (-0.5, 0, SIZE, SIZE + 0.5, DECK, SIZE + 0.5)),
                  ('west', (-0.5, 0, 0, 0, DECK, SIZE)), ('east', (SIZE, 0, 0, SIZE + 0.5, DECK, SIZE))):
    d.box('hazard', *box, skip=('down',))
# Two cross girders between the four cells.
BEAM = 4
d.box('breeder_girder', C - BEAM, 0, EDGE, C + BEAM, DECK, SIZE - EDGE, top='breeder_grate', skip=('down',))
d.box('breeder_girder', EDGE, 0, C - BEAM, C - BEAM, DECK, C + BEAM, top='breeder_grate', skip=('down',))
d.box('breeder_girder', C + BEAM, 0, C - BEAM, SIZE - EDGE, DECK, C + BEAM, top='breeder_grate', skip=('down',))


# ---------------------------------------------------------------- the shafts, cut away below
def shaft(cx, cz):
    """A miner's 4 x 4 shaft below the deck: walls of fused rock, facing in, so the near walls
    drop out of the picture and the far ones show; the molten pool glowing at the bottom."""
    x1, x2, z1, z2 = cx - CELL / 2, cx + CELL / 2, cz - CELL / 2, cz + CELL / 2
    y1, y2 = -DEPTH, 0
    for y in range(y1, y2, B):
        d.quad('breeder_graphite', [(x1, y + B, z1), (x2, y + B, z1), (x2, y, z1), (x1, y, z1)],
               [(0, 0), (16, 0), (16, 16), (0, 16)], (0, 0, 1))
        d.quad('breeder_graphite', [(x2, y + B, z2), (x1, y + B, z2), (x1, y, z2), (x2, y, z2)],
               [(0, 0), (16, 0), (16, 16), (0, 16)], (0, 0, -1))
        d.quad('breeder_graphite', [(x1, y + B, z2), (x1, y + B, z1), (x1, y, z1), (x1, y, z2)],
               [(0, 0), (16, 0), (16, 16), (0, 16)], (1, 0, 0))
        d.quad('breeder_graphite', [(x2, y + B, z1), (x2, y + B, z2), (x2, y, z2), (x2, y, z1)],
               [(0, 0), (16, 0), (16, 16), (0, 16)], (-1, 0, 0))
    d.quad('breeder_amber', [(x1, y1, z1), (x2, y1, z1), (x2, y1, z2), (x1, y1, z2)],
           [(0, 0), (16, 0), (16, 16), (0, 16)], (0, 1, 0))


# ---------------------------------------------------------------- a miner
def miner(cx, cz):
    """A derrick over its cell: four legs from the deck's corners leaning in to the crown, braced
    on each face; the heat unit hanging inside; the drill string down to the melt head."""
    foot, head = CELL / 2 - 6, 6
    corners = [(-1, -1), (1, -1), (1, 1), (-1, 1)]

    def leg_at(sx, sz, y):
        t = (y - DECK) / (TOP - DECK)
        r = foot + (head - foot) * t
        return (cx + sx * r, y, cz + sz * r)

    for sx, sz in corners:
        path = [leg_at(sx, sz, y) for y in (DECK, DECK + 16, DECK + 32, TOP)]
        d.sweep('breeder_leg', path, 1.2, 1.2, lambda i: (0, 1, 0), closed=False, caps=True)
        # A foot plate on the deck.
        fx, _, fz = leg_at(sx, sz, DECK)
        d.box('breeder_fitting', fx - 3, DECK, fz - 3, fx + 3, DECK + 1, fz + 3, skip=('down',))
    # Girts round the derrick, and X bracing on each face between them.
    levels = (DECK + 12, DECK + 30, DECK + 46)
    for y in levels:
        ring = [leg_at(sx, sz, y) for sx, sz in corners]
        d.sweep('breeder_fitting', ring, 0.6, 0.6, lambda i: (0, 1, 0), closed=True)
    for (ya, yb) in zip(levels, levels[1:]):
        for k in range(4):
            a, b = corners[k], corners[(k + 1) % 4]
            for p, q in ((leg_at(*a, ya), leg_at(*b, yb)), (leg_at(*a, yb), leg_at(*b, ya))):
                d.sweep('breeder_fitting', [p, q], 0.4, 0.4, lambda i: (0, 1, 0), closed=False, caps=True)
    # The crown: a graphite block with a light strip round it.
    d.box('breeder_graphite', cx - 8, TOP, cz - 8, cx + 8, TOP + 6, cz + 8, skip=('down',))
    d.box('glow', cx - 8.2, TOP + 2.5, cz - 8.2, cx + 8.2, TOP + 3.5, cz + 8.2, skip=('up', 'down'))
    # The heat unit: a white drum with a glowing amber band, hung from the crown.
    d.cylinder('breeder_steel_pipe', TOP - 22, TOP - 4, 7, n=12)
    d.annulus('breeder_fitting', TOP - 4, 0, 7, n=12)
    d.annulus('breeder_fitting', TOP - 22, 0, 7, up=False, n=12)
    d.cylinder('breeder_amber', TOP - 15, TOP - 12, 7.2, n=12)
    d.cylinder('breeder_fitting', TOP - 4, TOP, 2, n=8)
    # The drill string, down through the deck into the shaft, and the melt head at the bottom.
    with_centre(cx, cz, lambda: (
        d.cylinder('breeder_fitting', -DEPTH + 8, TOP - 22, STRING, n=8),
        d.lathe('breeder_amber', [(-DEPTH + 2, 0), (-DEPTH + 8, 5)], n=8),
        d.cylinder('breeder_fitting', -DEPTH + 8, -DEPTH + 11, 5, n=8),
        d.annulus('breeder_fitting', -DEPTH + 11, STRING, 5, n=8)))


def with_centre(x, z, draw):
    whole = d.at
    d.at = lambda phi, rad, y: (x + rad * math.cos(phi), y, z + rad * math.sin(phi))
    try:
        draw()
    finally:
        d.at = whole


CELLS = [(C - CELL / 2, C - CELL / 2), (C + CELL / 2, C - CELL / 2), (C - CELL / 2, C + CELL / 2), (C + CELL / 2, C + CELL / 2)]
for cx, cz in CELLS:
    shaft(cx, cz)
    with_centre(cx, cz, lambda cx=cx, cz=cz: None)
    whole = d.at
    d.at = lambda phi, rad, y, cx=cx, cz=cz: (cx + rad * math.cos(phi), y, cz + rad * math.sin(phi))
    try:
        miner(cx, cz)
    finally:
        d.at = whole

# ---------------------------------------------------------------- the controller, and the pipes into it
# On the front of the deck, the station's front panel (32 x 16 drawn once across it) with the
# screen standing proud in its bezel: all three big machines share a face.
FACE = -1.5
d.box('breeder_graphite', C - B, DECK, FACE, C + B, DECK + 15, EDGE, skip=('north', 'down'))
d.quad('front_panel', [(C + B, DECK + 15, FACE), (C - B, DECK + 15, FACE), (C - B, DECK, FACE), (C + B, DECK, FACE)],
       [(0, 1), (16, 1), (16, 16), (0, 16)], (0, 0, -1))
d.box('breeder_fitting', C - 5, DECK + 5, FACE - 1, C + 5, DECK + 11, FACE - 0.5, decals={'north': 'screen'}, skip=('south',))
d.box('breeder_fitting', C - 6, DECK + 4, FACE - 0.5, C + 6, DECK + 12, FACE, skip=('south',))
# From each drill string, just above the deck, a pipe runs to the cross girders and along them to
# the controller's back: what the strings bring up, gathered in one place.
for k, (cx, cz) in enumerate(CELLS):
    sx = 1 if cx < C else -1
    sz = 1 if cz < C else -1
    off = (k - 1.5) * 2.6
    y = DECK + 2
    path = [(cx, y, cz), (C - sx * 6 + off * 0.3, y, cz), (C - sx * 6 + off * 0.3, y, C - sz * 6), (C + off, y, C - sz * 6),
            (C + off, y, EDGE + 0.5)] if cz < C else [(cx, y, cz), (C - sx * 6 + off * 0.3, y, cz), (C - sx * 6 + off * 0.3, y, C - sz * 6 + 2),
            (C + off, y, C - sz * 6 + 2), (C + off, y, EDGE + 0.5)]
    d.sweep('breeder_steel_pipe', path, 1.2, 1.2, lambda i: (0, 1, 0), closed=False, caps=True)
    # A collar where the string comes up through the cell to the pipe.
    d.box('breeder_fitting', cx - 4, y - 2, cz - 4, cx + 4, y + 2, cz + 4)

def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'melt_drill.png')
    breeder_textures.main()
    d.save_png(out, [((1, 1), 3, 0.45), ((-1, 0.5), 3, 0.3)])


if __name__ == '__main__':
    main()
