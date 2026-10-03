"""The Melt Drill's concept (design section 11c), drawn as smooth quads, not yet in the game. Run:
    python art/tools/melt_drill_concept.py [OUT.png]

The user's idea (3 Oct 2026): a quarry block, one block with the inventory and the controls, sits
just outside a chunk and works the chunk it faces, split into four 8 x 8 quarters. Drills are
items: put one in the quarry block and it stands a drill up in the next free quarter. Better drills
are crafted from the one before (Mk I, II, III), so a quarry is upgraded, not replaced (rule 11).
The picture shows a chunk with a Mk I, a Mk II and a Mk III drill and one quarter waiting.
- A drill: a compact tower raised on four splayed legs over its quarter, so it stands over its pit
  as the pit deepens. Its bottom storey is the beam chamber behind glass, the emitter a graphite
  cone pointing down, its beam sweeping the whole 8 x 8 below; above it the drill core, the beam
  generator. Each mark is a little grander: the Mk I a plain core, the Mk II taller with a light
  band and cooling fins, the Mk III taller again with a resonator drum and a beacon.
- Below, cut away: each quarter's pit, its walls glazed in fused rock, the hot spot where the beam
  is melting.
- The quarry block outside the chunk's edge: a console with the station's screen, the four drills'
  conduits running to it along the ground, bringing up what they melt free.
Real basis: millimetre-wave drilling (a gyrotron's beam vaporises rock and the vapour is blown back
up the hole, glazing the walls), descended from the rock-melting drills of the 1970s.
"""
import math
import os
import sys

import model_preview as mp
import breeder_textures
from quad_design import Design

B = 16
CHUNK = 16 * B
QUARTER = 8 * B
DEPTH = 40             # how far the cutaway shows below ground
TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in ('hazard', 'glow', 'screen')})
TEXTURES['front_panel'] = 'ryzergen:block/breeder/front_panel'
TEXTURES['glass'] = 'ryzergen:block/breeder/glass'
d = Design(CHUNK / 2, TEXTURES, {'glow', 'screen', 'breeder_amber'})
UV = [(0, 0), (16, 0), (16, 16), (0, 16)]


def centred(x, z, draw):
    whole = d.at
    d.at = lambda phi, rad, y: (x + rad * math.cos(phi), y, z + rad * math.sin(phi))
    try:
        draw()
    finally:
        d.at = whole


def pit(x1, z1):
    """A quarter's pit, cut away: fused rock walls facing in, the floor dark, the hot spot glowing."""
    x2, z2 = x1 + QUARTER, z1 + QUARTER
    for y in range(-DEPTH, 0, B):
        top = min(0, y + B)
        for xa in range(int(x1), int(x2), B):
            d.quad('breeder_graphite', [(xa, top, z1), (xa + B, top, z1), (xa + B, y, z1), (xa, y, z1)], UV, (0, 0, 1))
            d.quad('breeder_graphite', [(xa + B, top, z2), (xa, top, z2), (xa, y, z2), (xa + B, y, z2)], UV, (0, 0, -1))
        for za in range(int(z1), int(z2), B):
            d.quad('breeder_graphite', [(x1, top, za + B), (x1, top, za), (x1, y, za), (x1, y, za + B)], UV, (1, 0, 0))
            d.quad('breeder_graphite', [(x2, top, za), (x2, top, za + B), (x2, y, za + B), (x2, y, za)], UV, (-1, 0, 0))
    for xa in range(int(x1), int(x2), B):
        for za in range(int(z1), int(z2), B):
            d.quad('breeder_concrete', [(xa, -DEPTH, za), (xa + B, -DEPTH, za), (xa + B, -DEPTH, za + B), (xa, -DEPTH, za + B)], UV, (0, 1, 0))


def marker(x1, z1):
    """Yellow corner marks on the ground round a quarter: the plot a drill will stand on."""
    for cx, cz, sx, sz in ((x1, z1, 1, 1), (x1 + QUARTER, z1, -1, 1), (x1, z1 + QUARTER, 1, -1), (x1 + QUARTER, z1 + QUARTER, -1, -1)):
        a, b = sorted((cx, cx + sx * 20))
        d.box('breeder_rail', a, 0, min(cz, cz + sz * 2), b, 1, max(cz, cz + sz * 2))
        a, b = sorted((cz, cz + sz * 20))
        d.box('breeder_rail', min(cx, cx + sx * 2), 0, a, max(cx, cx + sx * 2), 1, b)


def drill(x1, z1, mark, aim):
    """A drill over the quarter at (x1, z1): legs, the glass beam chamber, the core for its mark,
    and its beam aimed at `aim` (a point on the pit's floor, as it sweeps)."""
    qx, qz = x1 + QUARTER / 2, z1 + QUARTER / 2
    half = 2 * B            # the tower is 4 x 4
    base = 28               # the chamber's floor, up on the legs
    chamber = base + 2 * B
    core = chamber + 4 + (2, 3, 3)[mark - 1] * B
    # Legs from the quarter's corners up to the tower's corners, with a foot plate and a brace.
    for sx in (-1, 1):
        for sz in (-1, 1):
            foot = (qx + sx * (QUARTER / 2 - 5), 0, qz + sz * (QUARTER / 2 - 5))
            top = (qx + sx * (half - 3), base, qz + sz * (half - 3))
            d.sweep('breeder_leg', [foot, top], 1.6, 1.6, lambda i: (0, 1, 0), closed=False, caps=True)
            d.box('breeder_fitting', foot[0] - 4, 0, foot[2] - 4, foot[0] + 4, 2, foot[2] + 4, skip=('down',))
    # The chamber's floor: a graphite frame with a hazard edge, open in the middle for the beam.
    d.box('breeder_graphite', qx - half, base - 4, qz - half, qx + half, base, qz + half, skip=())
    for y, h in ((base - 4, 4),):
        d.box('hazard', qx - half - 0.4, y, qz - half - 0.4, qx + half + 0.4, y + h, qz + half + 0.4, skip=('up', 'down'))
    # Pillars and glass round the chamber.
    for sx in (-1, 1):
        for sz in (-1, 1):
            px, pz = qx + sx * (half - 2), qz + sz * (half - 2)
            d.box('breeder_leg', px - 2, base, pz - 2, px + 2, chamber, pz + 2, skip=('up', 'down'))
    d.group = 'glass'
    for y in range(base, chamber, B):
        for side in (-1, 1):
            z = qz + side * (half - 1)
            for sign in (1, -1):
                d.quad('glass', [(qx - half + 4, y + B, z), (qx + half - 4, y + B, z), (qx + half - 4, y, z), (qx - half + 4, y, z)], UV,
                       (0, 0, side * sign))
            x = qx + side * (half - 1)
            for sign in (1, -1):
                d.quad('glass', [(x, y + B, qz - half + 4), (x, y + B, qz + half - 4), (x, y, qz + half - 4), (x, y, qz - half + 4)], UV,
                       (side * sign, 0, 0))
    d.group = 'static'
    d.box('breeder_graphite', qx - half, chamber, qz - half, qx + half, chamber + 4, qz + half)
    d.box('glow', qx - half - 0.2, chamber + 1.5, qz - half - 0.2, qx + half + 0.2, chamber + 2.5, qz + half + 0.2, skip=('up', 'down'))
    # The emitter: a mount and a graphite cone pointing down, its lens glowing.
    tip = chamber - 18
    centred(qx, qz, lambda: (
        d.cylinder('breeder_hub', chamber - 8, chamber, 7, n=12),
        d.annulus('breeder_fitting', chamber - 8, 0, 7, up=False, n=12),
        d.lathe('breeder_graphite', [(tip + 2, 2), (chamber - 8, 6)], n=12),
        d.cylinder('glow', tip, tip + 2, 2, n=8)))
    # The beam, from the lens to where it is melting on the pit's floor, and the hot spot there.
    d.sweep('glow', [(qx, tip, qz), (aim[0], -DEPTH + 0.5, aim[1])], 0.8, 0.8, lambda i: (1, 0, 0), closed=False, caps=True)
    centred(aim[0], aim[1], lambda: d.disc('breeder_amber', -DEPTH, -DEPTH + 0.6, 7, n=12))
    # The core: casing panels between graphite bands, a light strip under the top band.
    inset = 2
    d.box('breeder_hub', qx - half + inset, chamber + 4, qz - half + inset, qx + half - inset, core, qz + half - inset, top='breeder_grate')
    d.box('breeder_graphite', qx - half, core - 4, qz - half, qx + half, core, qz + half, top='breeder_grate', skip=('down',))
    if mark >= 2:
        d.box('glow', qx - half - 0.2, core - 6, qz - half - 0.2, qx + half + 0.2, core - 5, qz + half + 0.2, skip=('up', 'down'))
        # Cooling fins down the sides.
        for k in range(3):
            off = (k - 1) * 10
            for sx in (-1, 1):
                d.box('breeder_fitting', qx + sx * (half - inset) - (0 if sx > 0 else 3), chamber + 10, qz + off - 1,
                      qx + sx * (half - inset) + (3 if sx > 0 else 0), core - 10, qz + off + 1)
    if mark >= 3:
        drum = (core, core + 14)
        centred(qx, qz, lambda: (
            d.cylinder('breeder_steel_pipe', drum[0], drum[1], 20, n=16),
            d.cylinder('breeder_graphite', drum[0] + 5, drum[0] + 8, 20.2, n=16),
            d.cylinder('glow', drum[0] + 6, drum[0] + 7, 20.4, n=16),
            d.annulus('breeder_fitting', drum[1], 0, 20, n=16),
            d.cylinder('breeder_graphite', drum[1], drum[1] + 4, 4, n=8),
            d.cylinder('breeder_amber', drum[1] + 4, drum[1] + 7, 2, n=8)))
    # The mark's plaque on the front: one, two or three cyan bars.
    for k in range(mark):
        x = qx - (mark - 1) * 3 + k * 6
        d.box('glow', x - 1.5, chamber + 10, qz - half + inset - 0.3, x + 1.5, chamber + 18, qz - half + inset, skip=('south',))
    return (qx - QUARTER / 2 + 5, qz - QUARTER / 2 + 5)


# The chunk: quarters 1 to 4 (as the user's sketch numbers them), each with its pit; drills in three.
Q = {1: (QUARTER, QUARTER), 2: (0, QUARTER), 3: (0, 0), 4: (QUARTER, 0)}
for x1, z1 in Q.values():
    pit(x1, z1)
feet = []
feet.append(drill(*Q[1], 3, (Q[1][0] + 40, Q[1][1] + 80)))
feet.append(drill(*Q[2], 2, (Q[2][0] + 88, Q[2][1] + 50)))
feet.append(drill(*Q[3], 1, (Q[3][0] + 64, Q[3][1] + 64)))
marker(*Q[4])

# The quarry block, just outside the chunk's front edge by quarter 4, facing in: a console block
# with the screen, on a graphite plinth with a hazard band.
BX, BZ = CHUNK - B - 4, -B - 4
d.box('breeder_graphite', BX - 2, 0, BZ - 2, BX + B + 2, 3, BZ + B + 2, skip=('down',))
d.box('hazard', BX - 2.4, 0, BZ - 2.4, BX + B + 2.4, 3, BZ + B + 2.4, skip=('up', 'down'))
d.box('breeder_hub', BX, 3, BZ, BX + B, 3 + B, BZ + B, top='breeder_grate')
d.box('breeder_fitting', BX + 3, 7, BZ - 0.5, BX + B - 3, 15, BZ, decals={'north': 'screen'}, skip=('south',))
d.box('glow', BX - 0.2, 3 + B - 3, BZ - 0.2, BX + B + 0.2, 3 + B - 2, BZ + B + 0.2, skip=('up', 'down'))
# Conduits from each drill's front foot along the ground to the quarry block.
for k, (fx, fz) in enumerate(feet):
    y = 1.5
    path = [(fx, y, fz), (fx, y, -1 - k * 1.5), (BX - 6 - k * 3, y, -1 - k * 1.5), (BX - 6 - k * 3, y, BZ + B / 2), (BX, y, BZ + B / 2)]
    d.sweep('breeder_steel_pipe', path, 1, 1, lambda i: (0, 1, 0), closed=False, caps=True)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'melt_drill.png')
    breeder_textures.main()
    d.save_png(out, [((1, 1), 1.5, 0.5), ((-1, 1), 1.5, 0.4)])


if __name__ == '__main__':
    main()
