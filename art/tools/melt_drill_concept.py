"""The Melt Drill's concept (design section 11c): a first sketch, drawn as smooth quads, not yet in
the game. Run:
    python art/tools/melt_drill_concept.py [OUT.png]

The rig is as big as what it mines: a deck that outlines a chunk quarter (8 x 8 blocks) on the
surface, divided by cross girders into four 4 x 4 cells. Each cell takes a miner, crafted on its own
and mounted on the deck, so a rig mines faster as miners are added (upgrade, don't replace: rule
11). Up to 4 miners per quarter.
- The deck: a slab-high girder frame round the quarter's edge with hazard stripes on its outer face
  (melting rock is dangerous), grating on top, and two cross girders between the cells.
- A miner: a rocket standing on four fins over its cell, nose up, its nozzle pointing down into the
  shaft: the melt head is its exhaust, burning its way into the ground. White welded plates with
  graphite bands, an orange stripe, a cyan light ring and portholes, an ogive nose with a beacon,
  and the drill string from the nozzle down to the head glowing at the bottom.
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
def fin(cx, cz, ax, az):
    """A fin along the direction (ax, az): a flat graphite plate from the body out to the deck,
    swept back at its leading edge, a pixel thick, drawn both sides with an edge cap."""
    inner, outer = BODY_R - 0.5, CELL / 2 - 2
    profile = [(inner, FIN_TOP), (outer, DECK + 10), (outer, DECK), (inner, DECK + 4)]
    across = (-az, 0, ax)
    for side in (-0.6, 0.6):
        pts = [(cx + ax * r + across[0] * side, y, cz + az * r + across[2] * side) for r, y in profile]
        d.quad('breeder_graphite', pts, [(0, 0), (16, 0), (16, 16), (0, 16)], (across[0] * side, 0, across[2] * side))
    # The fin's outer edge, where it stands on the deck.
    p0 = [(cx + ax * outer + across[0] * s, y, cz + az * outer + across[2] * s) for s, y in ((-0.6, DECK + 10), (0.6, DECK + 10), (0.6, DECK), (-0.6, DECK))]
    d.quad('breeder_fitting', p0, [(0, 0), (1, 0), (1, 10), (0, 10)], (ax, 0, az))
    # A foot plate under it.
    fx, fz = cx + ax * (outer - 2), cz + az * (outer - 2)
    d.box('breeder_fitting', fx - 3, DECK, fz - 3, fx + 3, DECK + 1, fz + 3, skip=('down',))


BODY_R = 8             # the rocket's body
NOZZLE_Y = DECK + 6    # the nozzle's mouth, just above the deck
BODY_Y = (DECK + 14, 70)   # the body's straight part
FIN_TOP = DECK + 30


def miner(cx, cz):
    """A miner as a rocket standing on its fins over its cell, nose up, its nozzle pointing down
    into the shaft: the melt head is its exhaust, burning its way into the ground. A white body in
    welded plates with graphite bands and an orange stripe, a cyan light ring and portholes near
    the top, an ogive nose with a beacon, four graphite fins standing on the deck, and the drill
    string from the nozzle down to the glowing head."""
    # The nozzle: a graphite bell flaring down to its mouth, glowing amber inside.
    d.lathe('breeder_graphite', [(NOZZLE_Y, 6.5), (NOZZLE_Y + 4, 5), (BODY_Y[0] - 2, 4)], n=12)
    d.lathe('breeder_amber', [(BODY_Y[0] - 2, 3.5), (NOZZLE_Y + 2, 5.6)], n=12, inward=True)
    d.cylinder('breeder_fitting', BODY_Y[0] - 2, BODY_Y[0], 6, n=12)
    # The body: a boat tail into the straight part, then the nose.
    d.lathe('breeder_steel_pipe', [(BODY_Y[0], 6), (BODY_Y[0] + 6, BODY_R)], n=12)
    d.cylinder('breeder_shell', BODY_Y[0] + 6, BODY_Y[1], BODY_R, n=12)
    for y in (BODY_Y[0] + 8, BODY_Y[1] - 12):
        d.cylinder('breeder_graphite', y, y + 3, BODY_R + 0.2, n=12)
    d.cylinder('breeder_amber', BODY_Y[0] + 20, BODY_Y[0] + 22, BODY_R + 0.2, n=12)
    d.cylinder('glow', BODY_Y[1] - 4, BODY_Y[1] - 3, BODY_R + 0.25, n=12)
    nose = [(BODY_Y[1], BODY_R)]
    for k in range(1, 7):
        t = k / 6
        nose.append((BODY_Y[1] + 22 * t, BODY_R * math.sqrt(max(0.0, 1 - t * t))))
    d.lathe('breeder_shell', nose, n=12)
    d.cylinder('breeder_graphite', BODY_Y[1] + 21, BODY_Y[1] + 24, 1.2, n=6)
    d.cylinder('glow', BODY_Y[1] + 24, BODY_Y[1] + 25.5, 1.4, n=6)
    # Portholes round the upper body: small dark plates with a cyan rim.
    for k in range(4):
        phi = math.radians(45 + 90 * k)
        px, _, pz = d.at(phi, BODY_R + 0.3, 0)
        nx, nz = math.cos(phi), math.sin(phi)
        across = (-nz, nx)
        y0, y1, w = BODY_Y[1] - 10, BODY_Y[1] - 6, 2
        pts = [(px + across[0] * w, y1, pz + across[1] * w), (px - across[0] * w, y1, pz - across[1] * w),
               (px - across[0] * w, y0, pz - across[1] * w), (px + across[0] * w, y0, pz + across[1] * w)]
        d.quad('screen', pts, [(0, 0), (4, 0), (4, 4), (0, 4)], (nx, 0, nz))
    for ax, az in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        fin(cx, cz, ax, az)
    # The drill string, down from the nozzle into the shaft, and the melt head at the bottom.
    d.cylinder('breeder_fitting', -DEPTH + 8, NOZZLE_Y, STRING, n=8)
    d.lathe('breeder_amber', [(-DEPTH + 2, 0), (-DEPTH + 8, 5)], n=8)
    d.cylinder('breeder_fitting', -DEPTH + 8, -DEPTH + 11, 5, n=8)
    d.annulus('breeder_fitting', -DEPTH + 11, STRING, 5, n=8)


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
