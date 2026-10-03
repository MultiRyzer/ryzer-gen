"""The Melt Drill's concept (design section 11c), drawn as smooth quads, not yet in the game. Run:
    python art/tools/melt_drill_concept.py [OUT.png]

The user's idea (3 Oct 2026): a quarry block, one block with the inventory and the controls, sits
just outside a chunk and works the chunk it faces, split into four 8 x 8 quarters. Drills are
items: put one in the quarry block and it stands a drill up in the next free quarter. Better drills
are crafted from the one before (Mk I, II, III), so a quarry is upgraded, not replaced (rule 11).
The picture shows a chunk with a Mk I, a Mk II and a Mk III drill and one quarter waiting.
- A drill: as big as its quarter (8 x 8), as the user drew it, standing on the glazed rim of its
  own pit. Its base is the beam chamber behind glass, the emitter a graphite cone hung over the
  middle, its beam sweeping the whole quarter; above it the drill core, light cladding between
  graphite bands with a vent in each face, a bolted lid flange with studs along its edges and the
  weighted regulator on the roof (a pressure vessel's touches). Each mark's core is a block
  taller; the Mk II adds a pressure gauge, the Mk III a relief valve and beacons.
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
import drill_textures
from quad_design import Design

B = 16
CHUNK = 16 * B
QUARTER = 8 * B
DEPTH = 40             # how far the cutaway shows below ground
TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in ('hazard', 'glow', 'screen')})
TEXTURES['front_panel'] = 'ryzergen:block/breeder/front_panel'
TEXTURES['glass'] = 'ryzergen:block/breeder/glass'
TEXTURES.update({'drill_' + name: 'ryzergen:block/drill/' + name for name in drill_textures.TEXTURES})
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
    """A drill as big as its quarter (8 x 8), as the user drew it: a rectangle standing on the
    glazed rim of its own pit. Its base is the beam chamber behind glass, the emitter a graphite cone
    hung from the ceiling, its beam sweeping the whole quarter below; above it the drill core, a
    block of light cladding between graphite bands, with a pressure vessel's touches: a bolted lid
    flange along its top edges with studs, the weighted regulator on the roof. Each mark's core is
    a block taller; the Mk II adds a pressure gauge, the Mk III a relief valve and beacons. Its
    beam is aimed at `aim` on the pit's floor."""
    gap = 1                 # neighbouring drills stand a pixel apart
    xa, xb, za, zb = x1 + gap, x1 + QUARTER - gap, z1 + gap, z1 + QUARTER - gap
    qx, qz = x1 + QUARTER / 2, z1 + QUARTER / 2
    chamber = (0, 2 * B)
    core = (chamber[1] + 4, chamber[1] + 4 + (3, 4, 5)[mark - 1] * B)
    # The base frame on the pit's rim, hazard striped outside.
    # A ring, open in the middle, so the pit and the beam show through the glass.
    for bx1, bz1, bx2, bz2 in ((xa, za, xb, za + 6), (xa, zb - 6, xb, zb), (xa, za + 6, xa + 6, zb - 6), (xb - 6, za + 6, xb, zb - 6)):
        d.box('drill_frame', bx1, 0, bz1, bx2, 4, bz2, top='breeder_grate', skip=('down',))
    d.box('hazard', xa - 0.4, 0, za - 0.4, xb + 0.4, 4, zb + 0.4, skip=('up', 'down'))
    # Pillars at the corners and the middle of each side, glass between them.
    P = 5
    for px in (xa, qx - P / 2, xb - P):
        for pz in (za, qz - P / 2, zb - P):
            if px == qx - P / 2 and pz == qz - P / 2:
                continue
            d.box('breeder_leg', px, 4, pz, px + P, chamber[1], pz + P, skip=('up', 'down'))
    d.group = 'glass'
    for a1, a2 in ((xa + P, qx - P / 2), (qx + P / 2, xb - P)):
        for y in range(4, chamber[1], B):
            y2 = min(chamber[1], y + B)
            for z, out in ((za + 1.5, -1), (zb - 1.5, 1)):
                for sign in (1, -1):
                    d.quad('glass', [(a1, y2, z), (a2, y2, z), (a2, y, z), (a1, y, z)], UV, (0, 0, out * sign))
    for b1, b2 in ((za + P, qz - P / 2), (qz + P / 2, zb - P)):
        for y in range(4, chamber[1], B):
            y2 = min(chamber[1], y + B)
            for x, out in ((xa + 1.5, -1), (xb - 1.5, 1)):
                for sign in (1, -1):
                    d.quad('glass', [(x, y2, b1), (x, y2, b2), (x, y, b2), (x, y, b1)], UV, (out * sign, 0, 0))
    d.group = 'static'
    # The chamber's ceiling: a graphite band with the light line round it.
    d.box('drill_band', xa, chamber[1], za, xb, core[0], zb)
    d.box('glow', xa - 0.2, chamber[1] + 1.5, za - 0.2, xb + 0.2, chamber[1] + 2.5, zb + 0.2, skip=('up', 'down'))
    # The emitter, hung from the ceiling over the middle: a mount and a graphite cone pointing down.
    tip = chamber[1] - 18
    centred(qx, qz, lambda: (
        d.cylinder('drill_frame', chamber[1] - 8, chamber[1], 9, n=12, v0=8),
        d.annulus('breeder_fitting', chamber[1] - 8, 0, 9, up=False, n=12),
        d.lathe('breeder_graphite', [(tip + 2, 2), (chamber[1] - 8, 7)], n=12),
        d.cylinder('glow', tip, tip + 2, 2, n=8)))
    # The core: light cladding, a vent set into the middle of each face, the lid flange on top.
    inset = 2
    d.box('drill_casing', xa + inset, core[0], za + inset, xb - inset, core[1], zb - inset, skip=('down',))
    vy = core[0] + B
    for face, box in (('north', (qx - B, vy, za + inset - 0.3, qx + B, vy + B, za + inset)),
                      ('south', (qx - B, vy, zb - inset, qx + B, vy + B, zb - inset + 0.3)),
                      ('west', (xa + inset - 0.3, vy, qz - B, xa + inset, vy + B, qz + B)),
                      ('east', (xb - inset, vy, qz - B, xb - inset + 0.3, vy + B, qz + B))):
        d.box('drill_vent', *box, skip=tuple(f for f in ('north', 'south', 'east', 'west', 'up', 'down') if f != face))
    lid = (core[1], core[1] + 4)
    d.box('drill_band', xa, lid[0], za, xb, lid[1], zb, top='drill_pot')
    d.box('glow', xa - 0.2, lid[0] + 1.5, za - 0.2, xb + 0.2, lid[0] + 2.5, zb + 0.2, skip=('up', 'down'))
    # Studs along the lid's edges, a block apart.
    for k in range(8):
        for sx, sz in ((xa + 8 + k * 16 - 0.5, za + 2), (xa + 8 + k * 16 - 0.5, zb - 3)):
            if sx < xb - 2:
                d.box('breeder_fitting', sx - 1, lid[1], sz - 0.5, sx + 1, lid[1] + 2, sz + 1.5)
        for sx, sz in ((xa + 2, za + 8 + k * 16 - 0.5), (xb - 3, za + 8 + k * 16 - 0.5)):
            if sz < zb - 2:
                d.box('breeder_fitting', sx - 0.5, lid[1], sz - 1, sx + 1.5, lid[1] + 2, sz + 1)
    # The regulator on the roof: a stem, its graphite weight, an amber tip.
    centred(qx, qz, lambda: (
        d.cylinder('breeder_fitting', lid[1], lid[1] + 5, 2, n=8),
        d.cylinder('breeder_graphite', lid[1] + 5, lid[1] + 11, 6, n=10),
        d.annulus('breeder_graphite', lid[1] + 11, 0, 6, n=10),
        d.cylinder('breeder_amber', lid[1] + 11, lid[1] + 12, 2, n=6)))
    if mark >= 2:
        # A pressure gauge on the front, beside the mark.
        gx, gy = qx + 24, core[0] + 8
        d.box('breeder_graphite', gx - 6, gy - 6, za + inset - 1.5, gx + 6, gy + 6, za + inset, skip=('south',))
        d.box('breeder_fitting', gx - 4, gy - 4, za + inset - 2, gx + 4, gy + 4, za + inset - 1.5, decals={'north': 'screen'}, skip=('south',))
    if mark >= 3:
        centred(qx - 30, qz + 30, lambda: (d.cylinder('breeder_fitting', lid[1], lid[1] + 8, 1.5, n=6),
                                         d.cylinder('breeder_amber', lid[1] + 8, lid[1] + 10, 2.5, n=6)))
        for bx, bz in ((xa + 4, za + 4), (xb - 4, za + 4), (xa + 4, zb - 4), (xb - 4, zb - 4)):
            d.box('breeder_amber', bx - 1.5, lid[1], bz - 1.5, bx + 1.5, lid[1] + 3, bz + 1.5, skip=('down',))
    # The mark: one, two or three cyan bars on the core's front.
    for k in range(mark):
        x = qx - 24 - (mark - 1) * 3 + k * 6
        d.box('glow', x - 1.5, core[0] + 4, za + inset - 0.5, x + 1.5, core[0] + 12, za + inset, skip=('south',))
    # The beam, from the lens to where it is melting on the pit's floor, and the hot spot there.
    d.sweep('glow', [(qx, tip, qz), (aim[0], -DEPTH + 0.5, aim[1])], 0.8, 0.8, lambda i: (1, 0, 0), closed=False, caps=True)
    centred(aim[0], aim[1], lambda: d.disc('breeder_amber', -DEPTH, -DEPTH + 0.6, 7, n=12))
    return (xa + 4, za + 4)


def with_group(name, draw):
    d.group = name
    try:
        draw()
    finally:
        d.group = 'static'


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
d.box('drill_casing', BX, 3, BZ, BX + B, 3 + B, BZ + B, top='breeder_grate')
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
    drill_textures.main()
    d.save_png(out, [((1, 1), 1.4, 0.45), ((1, 0.25), 1.4, 0.08)])


if __name__ == '__main__':
    main()
