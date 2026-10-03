"""The Melt Drill's concept (design section 11c), drawn as smooth quads, not yet in the game. Run:
    python art/tools/melt_drill_concept.py [OUT.png]

The user's idea (3 Oct 2026): a quarry block, one block with the inventory and the controls, sits
just outside a chunk and works the chunk it faces, split into four 8 x 8 quarters. Drills are
items: put one in the quarry block and it stands a drill up in the next free quarter. Better drills
are crafted from the one before (Mk I, II, III), so a quarry is upgraded, not replaced (rule 11).
The picture shows a chunk with a Mk I, a Mk II and a Mk III drill and one quarter waiting.
- A drill: a small fission station of its own, raised on four splayed legs over its quarter, so it
  stands over its pit as the pit deepens. A graphite skirt, the glass chamber ring between white
  ribs with the emitter inside (a graphite cone pointing down, its beam sweeping the whole 8 x 8
  below), a gunmetal head band with its light line, and a short white hyperbolic stack with the
  station's aviation marking round its top. Each mark stands taller: the Mk II's stack carries a
  second light ring, the Mk III's is taller again with beacons on its rim.
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
    """A drill over the quarter at (x1, z1): a small fission station of its own, raised on four
    splayed legs over its pit. Bottom to top: a graphite skirt round the chamber's floor, the glass
    chamber ring between white ribs with the emitter inside firing down, a gunmetal head band with
    its cyan light line, and a short white hyperbolic stack topped with the station's aviation
    marking. Each mark stands taller: the Mk II's stack carries a second light ring, the Mk III's
    is taller again with beacons on its rim. Its beam is aimed at `aim` on the pit's floor."""
    qx, qz = x1 + QUARTER / 2, z1 + QUARTER / 2
    r = 26                  # the station's radius (a little under 4 blocks across)
    base = 28               # the chamber's floor, up on the legs
    chamber = (base, base + 2 * B)
    head = (chamber[1], chamber[1] + 8)
    stack_h = (24, 36, 48)[mark - 1]
    top = head[1] + stack_h

    def at(phi, rad, y):
        return (qx + rad * math.cos(phi), y, qz + rad * math.sin(phi))

    whole = d.at
    d.at = at
    try:
        # Legs from the quarter's corners up under the skirt, with foot plates.
        for k in range(4):
            phi = math.radians(45 + 90 * k)
            foot = at(phi, (QUARTER / 2 - 5) * math.sqrt(2), 0)
            d.sweep('breeder_leg', [foot, at(phi, r - 4, base - 6)], 1.6, 1.6, lambda i: (0, 1, 0), closed=False, caps=True)
            d.box('breeder_fitting', foot[0] - 4, 0, foot[2] - 4, foot[0] + 4, 2, foot[2] + 4, skip=('down',))
        # The skirt and the chamber's floor: a ring, open in the middle for the beam.
        d.cylinder('drill_skirt', base - 8, base, r + 1, n=16)
        d.annulus('drill_skirt', base - 8, 8, r + 1, up=False, n=16)
        d.annulus('breeder_grate', base, 8, r, n=16)
        d.cylinder('breeder_fitting', base - 8, base, 8, n=12, inward=True)
        # The glass chamber between eight white ribs.
        with_group('glass', lambda: (d.cylinder('glass', chamber[0], chamber[1], r - 1, n=16),
                                     d.cylinder('glass', chamber[0], chamber[1], r - 1, n=16, inward=True)))
        for k in range(8):
            d.post('breeder_leg', math.radians(22.5 * (2 * k + 1)), r, chamber[0], chamber[1], 1.5, 1.5)
        # The emitter, hung from the head: a mount and a graphite cone pointing down, lens glowing.
        tip = chamber[1] - 18
        d.cylinder('drill_frame', chamber[1] - 8, chamber[1], 7, n=12, v0=8)
        d.annulus('breeder_fitting', chamber[1] - 8, 0, 7, up=False, n=12)
        d.lathe('breeder_graphite', [(tip + 2, 2), (chamber[1] - 8, 6)], n=12)
        d.cylinder('glow', tip, tip + 2, 2, n=8)
        # The head band and its light line.
        d.cylinder('drill_ring', head[0], head[1], r + 1, n=16, v0=8)
        d.annulus('drill_ring', head[0], 0, r + 1, up=False, n=16)
        d.cylinder('glow', head[0] + 2.5, head[0] + 3.5, r + 1.2, n=16)
        # The stack: a short hyperbolic tower in white cladding, narrowing then flaring a little.
        profile = []
        for k in range(7):
            t = k / 6
            y = head[1] + stack_h * t
            profile.append((y, r - 2 - 6 * (1 - (2 * t - 1) ** 2) * 0.8))
        d.lathe('drill_casing', profile, n=16)
        d.lathe('breeder_graphite', profile, n=16, inward=True)
        if mark >= 2:
            mid = head[1] + stack_h * 0.5
            d.cylinder('glow', mid, mid + 1, r - 2 - 6 * 0.8 + 0.3, n=16)
        # The aviation marking round the top, and the rim.
        d.cylinder('drill_warning', top, top + 6, r - 1.5, n=16, v0=10)
        d.annulus('breeder_fitting', top + 6, r - 4, r - 1.5, n=16)
        d.cylinder('breeder_graphite', top, top + 6, r - 4, n=16, inward=True)
        if mark >= 3:
            for k in range(4):
                d.post('breeder_amber', math.radians(45 + 90 * k), r - 2.8, top + 6, top + 9, 1, 1)
        # The mark: one, two or three cyan bars on the head band's front.
        for k in range(mark):
            x = qx - (mark - 1) * 3 + k * 6
            d.box('glow', x - 1.5, head[0] + 4.5, qz - r - 1.6, x + 1.5, head[1] - 0.5, qz - r - 1.1, skip=('south',))
    finally:
        d.at = whole
    # The beam, from the lens to where it is melting on the pit's floor, and the hot spot there.
    d.sweep('glow', [(qx, tip, qz), (aim[0], -DEPTH + 0.5, aim[1])], 0.8, 0.8, lambda i: (1, 0, 0), closed=False, caps=True)
    centred(aim[0], aim[1], lambda: d.disc('breeder_amber', -DEPTH, -DEPTH + 0.6, 7, n=12))
    return (qx - QUARTER / 2 + 5, qz - QUARTER / 2 + 5)


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
    d.save_png(out, [((1, 1), 1.5, 0.5), ((-1, 1), 1.5, 0.4)])


if __name__ == '__main__':
    main()
