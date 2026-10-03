"""The Melt Drill's concept (design section 11c), drawn as smooth quads, not yet in the game. Run:
    python art/tools/melt_drill_concept.py [OUT.png]

The user's idea (3 Oct 2026): a quarry block, one block with the inventory and the controls, sits
just outside a chunk and works the chunk it faces, split into four 8 x 8 quarters. Drills are
items: put one in the quarry block and it stands a drill up in the next free quarter. Better drills
are crafted from the one before (Mk I, II, III), so a quarry is upgraded, not replaced (rule 11).
The picture shows a chunk with a Mk I, a Mk II and a Mk III drill and one quarter waiting.
- A drill: a small reactor of its own, raised on four splayed legs over its quarter, so it stands
  over its pit as the pit deepens. A graphite skirt, the glass chamber ring between white ribs with
  the emitter inside (a graphite cone pointing down, its beam sweeping the whole 8 x 8 below), and
  above it the pressure vessel, shaped like a pressure cooker (as a real reactor vessel is): a
  squat brushed-steel pot, a bolted flange with studs all round and the light line in it, a domed
  lid with the weighted regulator at its crown, and a handle either side. Each mark is bigger: the
  Mk II's pot taller with a pressure gauge, the Mk III's taller again with a relief valve and
  beacons on its flange.
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
    pot_h = (18, 26, 34)[mark - 1]
    dome_h = (8, 10, 12)[mark - 1]

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
        # The pressure vessel, the shape of a pressure cooker (as a reactor's pressure vessel is):
        # a squat brushed-steel pot, a bolted flange ring with studs all round and the light line in
        # it, a domed lid, the weighted pressure regulator at its crown, and a handle either side.
        pot = (chamber[1], chamber[1] + pot_h)
        d.cylinder('drill_ring', pot[0], pot[0] + 4, r + 1, n=16, v0=12)
        d.annulus('drill_ring', pot[0], 0, r + 1, up=False, n=16)
        d.cylinder('drill_pot', pot[0] + 4, pot[1], r, n=16)
        flange = (pot[1], pot[1] + 5)
        d.cylinder('drill_ring', flange[0], flange[1], r + 3, n=16, v0=11)
        d.annulus('drill_ring', flange[0], r, r + 3, up=False, n=16)
        d.annulus('drill_ring', flange[1], r - 1, r + 3, n=16)
        d.cylinder('glow', flange[0] + 2, flange[0] + 3, r + 3.2, n=16)
        for k in range(16):
            d.post('breeder_fitting', math.radians(11.25 + 22.5 * k), r + 1.5, flange[1], flange[1] + 2, 1, 1)
        dome = []
        for k in range(7):
            t = k / 6
            dome.append((flange[1] + dome_h * math.sin(t * math.pi / 2), (r - 1) * math.cos(t * math.pi / 2)))
        d.lathe('drill_pot', dome, n=16)
        crown = flange[1] + dome_h
        # The regulator: a stem and its weight.
        d.cylinder('breeder_fitting', crown - 1, crown + 4, 1.5, n=8)
        d.cylinder('breeder_graphite', crown + 4, crown + 9, 4, n=8)
        d.annulus('breeder_graphite', crown + 9, 0, 4, n=8)
        d.cylinder('breeder_amber', crown + 9, crown + 10, 1.5, n=6)
        # Handles either side of the pot, under the flange.
        for side in (0, math.pi):
            p0, p1, p2, p3 = (at(side, r, pot[1] - 8), at(side, r + 7, pot[1] - 8), at(side, r + 7, pot[1] - 2), at(side, r, pot[1] - 2))
            d.sweep('breeder_graphite', [p0, p1, p2, p3], 1.5, 3, lambda i, a=side: (-math.sin(a), 0, math.cos(a)), closed=False, caps=True)
        if mark >= 2:
            # A pressure gauge on the front of the pot: a dial with its cyan face.
            gx, gy, gz = at(math.radians(270), r + 0.5, pot[0] + pot_h * 0.55)
            d.box('breeder_graphite', gx - 4, gy - 4, gz - 1.5, gx + 4, gy + 4, gz + 1, skip=('south',))
            d.box('breeder_fitting', gx - 3, gy - 3, gz - 2, gx + 3, gy + 3, gz - 1.5, decals={'north': 'screen'}, skip=('south',))
        if mark >= 3:
            # A relief valve beside the regulator, and beacons round the flange.
            vx, vy, vz = at(math.radians(315), r * 0.55, 0)
            centred(vx, vz, lambda: (d.cylinder('breeder_fitting', crown - 6, crown + 2, 1.2, n=6),
                                     d.cylinder('breeder_amber', crown + 2, crown + 4, 2, n=6)))
            for k in range(4):
                d.post('breeder_amber', math.radians(45 + 90 * k), r + 2.5, flange[1] + 2, flange[1] + 5, 1, 1)
        head = (flange[0], flange[1])
        # The mark: one, two or three cyan bars on the head band's front.
        for k in range(mark):
            x = qx - (mark - 1) * 3 + k * 6
            d.box('glow', x - 1.5, pot[0] + 6, qz - r - 0.6, x + 1.5, pot[0] + 11, qz - r - 0.1, skip=('south',))
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
