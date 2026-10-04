"""The Melt Drill's concept (design section 11c), drawn as smooth quads, not yet in the game. Run:
    python art/tools/melt_drill_concept.py [OUT.png]

The user's idea (3 Oct 2026): a quarry block, one block with the inventory and the controls, sits
just outside a chunk and works the chunk it faces, split into four 8 x 8 quarters. Drills are
items: put one in the quarry block and it stands a drill up in the next free quarter. Better drills
are crafted from the one before (Mk I, II, III), so a quarry is upgraded, not replaced (rule 11).
The picture shows a chunk with a Mk I, a Mk II and a Mk III drill and one quarter waiting.
- A drill: a tall eight-sided tower over its quarter. Four buttress legs splay to the quarter's
  corners, and between them the glass beam chamber, the emitter cone at its top firing down into
  the pit; above, an eight-sided deck with a handrail, a ladder, a console and a tank; then a lattice
  column braced in X with the drive shaft inside, a graphite collar, and the head, its windows onto
  the beam generator glowing inside, capped with a beacon. Each mark's column is taller; the Mk
  III's head carries side pods.
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
TEXTURES.update({'drill_' + name: 'ryzergen:block/drill/' + name for name in drill_textures.NAMES})
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
    """A drill: a tall eight-sided tower over its quarter. Bottom to top:
    - four heavy buttress legs splayed to the quarter's corners, and between them the glass beam
      chamber, an eight-sided ring of glass between white ribs, the emitter cone at its top firing
      its beam down into the pit;
    - the deck, an eight-sided platform with a yellow handrail, a ladder up from the ground on the
      front, a console with its screen and a tank with its pipe down a leg;
    - the column, eight white posts braced in X, the drive shaft inside;
    - the collar, a graphite ring with vents and its light line;
    - the head, light cladding with windows onto the beam generator glowing inside, a cap and a
      beacon. Each mark's column is taller; the Mk III's head carries side pods.
    Its beam is aimed at `aim` on the pit's floor."""
    qx, qz = x1 + QUARTER / 2, z1 + QUARTER / 2
    oct_phase = math.pi / 8     # eight-sided parts turned so a flat faces each side
    deck_y = 40
    column = (deck_y + 6, deck_y + 6 + (40, 56, 72)[mark - 1])
    collar = (column[1], column[1] + 8)
    head = (collar[1], collar[1] + 30)

    def at(phi, rad, y):
        return (qx + rad * math.cos(phi + oct_phase), y, qz + rad * math.sin(phi + oct_phase))

    whole = d.at
    d.at = at
    try:
        # The ground ring under the chamber, hazard striped.
        d.cylinder('drill_frame', 0, 4, 30, n=8, v0=12)
        d.cylinder('hazard', 0, 4, 30.3, n=8)
        d.annulus('breeder_grate', 4, 20, 30, n=8)
        # The glass chamber between white ribs, the emitter cone at its top.
        with_group('glass', lambda: (d.cylinder('glass', 4, deck_y, 26, n=8),
                                     d.cylinder('glass', 4, deck_y, 26, n=8, inward=True)))
        for k in range(8):
            d.post('breeder_leg', 2 * math.pi * k / 8 - oct_phase + math.pi / 8, 26.5, 4, deck_y, 1.6, 1.6)
        tip = deck_y - 16
        d.lathe('breeder_graphite', [(tip + 2, 2), (deck_y - 4, 8)], n=8)
        d.cylinder('drill_frame', deck_y - 4, deck_y, 10, n=8, v0=12)
        d.cylinder('glow', tip, tip + 2, 2, n=8)
        # Four buttress legs from the quarter's corners up under the deck.
        for k in range(4):
            phi = math.radians(45 + 90 * k)
            fx, fz = qx + (QUARTER / 2 - 8) * math.cos(phi), qz + (QUARTER / 2 - 8) * math.sin(phi)
            tx, tz = qx + 30 * math.cos(phi), qz + 30 * math.sin(phi)
            across = (-math.sin(phi), 0, math.cos(phi))
            d.sweep('drill_casing', [(fx, 0, fz), (fx, 8, fz), (tx, deck_y, tz)], 3, 4.5, lambda i, v=across: v, closed=False, caps=True)
            d.box('drill_frame', fx - 7, 0, fz - 7, fx + 7, 4, fz + 7, top='breeder_grate', skip=('down',))
            d.box('hazard', fx - 7.3, 0, fz - 7.3, fx + 7.3, 4, fz + 7.3, skip=('up', 'down'))
            # An orange clamp on each leg.
            mx, mz = fx + (tx - fx) * 0.45, fz + (tz - fz) * 0.45
            d.box('breeder_amber' if False else 'breeder_rail', mx - 3, 18, mz - 3, mx + 3, 21, mz + 3)
        # The deck: an eight-sided platform, grated, its edge a graphite band with the light line.
        d.cylinder('drill_band', deck_y, deck_y + 6, 42, n=8, v0=10)
        d.annulus('drill_frame', deck_y, 10, 42, up=False, n=8)
        d.annulus('breeder_grate', deck_y + 6, 18, 42, n=8)
        d.cylinder('glow', deck_y + 2.5, deck_y + 3.5, 42.2, n=8)
        # The handrail round the deck's edge, with a gap at the ladder.
        rail_r = 40
        corners = [at(2 * math.pi * k / 8 - oct_phase + math.pi / 8 * 0, rail_r, deck_y + 13) for k in range(9)]
        pts = [at(math.radians(-90 + 12) + 2 * math.pi * k / 48, rail_r, deck_y + 13) for k in range(41)]
        d.sweep('breeder_rail', pts, 0.6, 0.6, lambda i: (0, 1, 0), closed=False, caps=True)
        for k in range(0, 41, 5):
            px, _, pz = pts[k]
            d.box('breeder_rail', px - 0.6, deck_y + 6, pz - 0.6, px + 0.6, deck_y + 13, pz + 0.6)
        # The column: eight white posts, X braces between them, the drive shaft inside.
        posts = 8
        for k in range(posts):
            phi = 2 * math.pi * k / posts - oct_phase + math.pi / 8
            d.post('breeder_leg', phi, 18, column[0], column[1], 1.8, 1.8)
        for k in range(posts):
            a0, a1 = 2 * math.pi * k / posts - oct_phase + math.pi / 8, 2 * math.pi * (k + 1) / posts - oct_phase + math.pi / 8
            for ya in range(column[0], column[1], 16):
                yb = min(column[1], ya + 16)
                for p0, p1 in ((at(a0, 18, ya), at(a1, 18, yb)), (at(a0, 18, yb), at(a1, 18, ya))):
                    d.sweep('drill_frame', [p0, p1], 0.6, 0.6, lambda i: (0, 1, 0), closed=False, caps=True)
        d.cylinder('breeder_graphite', column[0], column[1], 6, n=8)
        d.cylinder('breeder_amber', column[0] + 10, column[0] + 12, 6.2, n=8)
        d.annulus('drill_ring', column[0], 6, 20, n=8)
        # The collar: a graphite ring with vents and its light line.
        d.cylinder('drill_ring', collar[0], collar[1], 30, n=8, v0=8)
        d.annulus('drill_ring', collar[0], 18, 30, up=False, n=8)
        d.cylinder('glow', collar[0] + 2.5, collar[0] + 3.5, 30.2, n=8)
        # The head: light cladding, a window in each face onto the generator glowing inside.
        d.cylinder('drill_casing', head[0], head[1], 26, n=8)
        d.annulus('drill_ring', collar[1], 26, 30, n=8)
        d.cylinder('breeder_amber', head[0] + 4, head[1] - 6, 14, n=8)
        for k in range(8):
            phi = 2 * math.pi * k / 8
            cx, _, cz = at(phi, 26 * math.cos(math.pi / 8) + 0.3, 0)
            nx, nz = math.cos(phi + oct_phase), math.sin(phi + oct_phase)
            across = (-nz, nx)
            w = 5
            pts4 = [(cx + across[0] * w, head[1] - 8, cz + across[1] * w), (cx - across[0] * w, head[1] - 8, cz - across[1] * w),
                    (cx - across[0] * w, head[0] + 6, cz - across[1] * w), (cx + across[0] * w, head[0] + 6, cz + across[1] * w)]
            d.quad('drill_vent' if k % 2 else 'screen', pts4, [(0, 0), (16, 0), (16, 16), (0, 16)], (nx, 0, nz))
        cap = (head[1], head[1] + 6)
        d.lathe('drill_band', [(cap[0], 26), (cap[1], 20)], n=8)
        d.annulus('drill_frame', cap[1], 0, 20, n=8)
        d.cylinder('breeder_graphite', cap[1], cap[1] + 5, 5, n=8)
        d.cylinder('breeder_amber', cap[1] + 5, cap[1] + 8, 2.5, n=8)
        if mark >= 3:
            for side in (0, math.pi):
                px, _, pz = at(side - oct_phase, 30, 0)
                centred(px, pz, lambda: (d.cylinder('drill_casing', head[0] + 2, head[1] - 4, 6, n=8),
                                         d.annulus('drill_band', head[1] - 4, 0, 6, n=8),
                                         d.cylinder('glow', head[1] - 9, head[1] - 8, 6.2, n=8)))
    finally:
        d.at = whole
    # The ladder up the front from the ground to the deck.
    lz = qz - 44
    for lx in (qx - 4, qx + 4):
        d.box('breeder_rail', lx - 0.7, 0, lz - 0.7, lx + 0.7, deck_y + 12, lz + 0.7)
    for y in range(4, deck_y + 6, 4):
        d.box('breeder_rail', qx - 4, y, lz - 0.4, qx + 4, y + 0.8, lz + 0.4)
    # A console on the deck by the ladder, and a tank at the back with its pipe down a leg.
    d.box('breeder_graphite', qx + 14, deck_y + 6, qz - 36, qx + 30, deck_y + 18, qz - 26, skip=('down',))
    d.box('breeder_fitting', qx + 17, deck_y + 10, qz - 36.5, qx + 27, deck_y + 16, qz - 36, decals={'north': 'screen'}, skip=('south',))
    for k in range(mark):
        d.box('glow', qx + 16 + k * 4, deck_y + 7, qz - 36.4, qx + 18 + k * 4, deck_y + 9, qz - 36, skip=('south',))
    centred(qx + 24, qz + 24, lambda: (d.cylinder('drill_pot', deck_y + 6, deck_y + 30, 7, n=10),
                                       d.annulus('drill_band', deck_y + 30, 0, 7, n=10),
                                       d.cylinder('drill_band', deck_y + 18, deck_y + 21, 7.2, n=10)))
    d.sweep('breeder_steel_pipe', [(qx + 30, deck_y + 10, qz + 24), (qx + 46, deck_y + 4, qz + 40), (qx + 52, 4, qz + 52)],
            1.4, 1.4, lambda i: (0, 1, 0), closed=False, caps=True)
    # The beam, from the lens to where it is melting on the pit's floor, and the hot spot there.
    d.sweep('glow', [(qx, deck_y - 16, qz), (aim[0], -DEPTH + 0.5, aim[1])], 0.8, 0.8, lambda i: (1, 0, 0), closed=False, caps=True)
    centred(aim[0], aim[1], lambda: d.disc('breeder_amber', -DEPTH, -DEPTH + 0.6, 7, n=12))
    return (x1 + 8, z1 + 8)


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
    d.save_png(out, [((1, 1), 1.15, 0.35), ((1, 0.25), 1.15, 0.08)])


if __name__ == '__main__':
    main()
