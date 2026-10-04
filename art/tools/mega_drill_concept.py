"""The mega drill's concept (design section 11c, option B), drawn as smooth quads, not yet in the
game. Run:
    python art/tools/mega_drill_concept.py [OUT.png]

One landmark tower that never moves, standing over the middle of a chunk on a 9 x 9 base (the
user's references, 4 Oct 2026): angular, not round (squares with their corners cut off), and open at
the foot so you see the laser going in. Bottom to top:
- The base: a hazard-striped ring round a 6 x 6 molten pit glowing amber, and four sloped buttress
  legs from the corners up under the deck, the space between them open.
- The emitter: a graphite pyramid hung under the deck, its lens glowing, its beam straight down into
  the pit.
- The deck: a cut-corner square platform, grated, its edge a graphite band with the light line, a
  yellow handrail round it, a ladder up from the ground, a console with its screen and a tank.
- The column: an open lattice, eight posts with big X braces on its four faces, the drive shaft
  inside with an amber band, a square duct up one side.
- The collar: a wider cut-corner ring with vents and its light line.
- The head: light cladding with windows onto the beam generator glowing inside and orange lamps on
  its corners, a stepped cap and a beacon.
Real basis: blind-shaft boring rigs stand over the hole they bore and lift the cuttings up through
it; this one melts the rock with a beam (thermal spallation).
"""
import math
import os
import sys

import model_preview as mp
import breeder_textures
import drill_textures
from quad_design import Design

B = 16
SIZE = 9 * B
C = SIZE / 2
TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({'drill_' + name: 'ryzergen:block/drill/' + name for name in drill_textures.TEXTURES})
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in ('hazard', 'glow', 'screen')})
d = Design(C, TEXTURES, {'glow', 'screen', 'breeder_amber'})
ALL = ('north', 'south', 'east', 'west', 'up', 'down')
UV = [(0, 0), (16, 0), (16, 16), (0, 16)]


def face_only(face):
    return tuple(f for f in ALL if f != face)


def outline(x1, y1, z1, x2, y2, z2, mat, t=0.3):
    d.box(mat, x1 - t, y1, z1 - t, x2 + t, y2, z2 + t, skip=('up', 'down'))


def corners(w, c):
    """A square of half width w with its corners cut by c, as 8 points round the centre."""
    return [(C + w, C - w + c), (C + w, C + w - c), (C + w - c, C + w), (C - w + c, C + w),
            (C - w, C + w - c), (C - w, C - w + c), (C - w + c, C - w), (C + w - c, C - w)]


def prism(mat, y1, y2, w, c, top=None, bottom=None, inward=False, v0=None):
    """A cut-corner square prism: its sides cut into 16-pixel panels along each edge (so a texture
    shows whole), a top and bottom if asked."""
    pts = corners(w, c)
    h = y2 - y1
    va = (16 - h) if v0 is None else v0
    for k in range(8):
        (ax, az), (bx, bz) = pts[k], pts[(k + 1) % 8]
        length = math.hypot(bx - ax, bz - az)
        n = max(1, round(length / 16))
        nx, nz = (bz - az) / length, -(bx - ax) / length
        mx, mz = (ax + bx) / 2 - C, (az + bz) / 2 - C
        if nx * mx + nz * mz < 0:
            nx, nz = -nx, -nz
        if inward:
            nx, nz = -nx, -nz
        for i in range(n):
            t0, t1 = i / n, (i + 1) / n
            p0 = (ax + (bx - ax) * t0, az + (bz - az) * t0)
            p1 = (ax + (bx - ax) * t1, az + (bz - az) * t1)
            u = length / n
            d.quad(mat, [(p0[0], y2, p0[1]), (p1[0], y2, p1[1]), (p1[0], y1, p1[1]), (p0[0], y1, p0[1])],
                   [(0, va), (min(16, u), va), (min(16, u), va + min(16, h)), (0, va + min(16, h))], (nx, 0, nz))
    for cap, y, up in ((top, y2, 1), (bottom, y1, -1)):
        if cap is None:
            continue
        for k in range(1, 7, 2):
            q = [pts[0], pts[k], pts[k + 1], pts[min(k + 2, 7)]]
            d.quad(cap, [(x, y, z) for x, z in q], [((x - C) % 16, (z - C) % 16) for x, z in q], (0, up, 0))
        d.quad(cap, [(x, y, z) for x, z in (pts[0], pts[7], pts[7], pts[0])], UV, (0, up, 0))


# ---------------------------------------------------------------- the base and the pit
PIT = 3 * B
RING = 6
for x1, z1, x2, z2 in ((0, 0, SIZE, C - PIT), (0, C + PIT, SIZE, SIZE), (0, C - PIT, C - PIT, C + PIT), (C + PIT, C - PIT, SIZE, C + PIT)):
    d.box('drill_skirt', x1, 0, z1, x2, RING, z2, top='breeder_grate', skip=('down',))
outline(0, 0, 0, SIZE, 4, SIZE, 'hazard')
lo, hi = C - PIT, C + PIT
steps = [lo + k * B for k in range(int((hi - lo) // B))] + [hi]
for xa, xb in zip(steps, steps[1:]):
    d.quad('breeder_graphite', [(xa, RING, lo), (xb, RING, lo), (xb, 1, lo), (xa, 1, lo)], UV, (0, 0, 1))
    d.quad('breeder_graphite', [(xb, RING, hi), (xa, RING, hi), (xa, 1, hi), (xb, 1, hi)], UV, (0, 0, -1))
    d.quad('breeder_graphite', [(lo, RING, xb), (lo, RING, xa), (lo, 1, xa), (lo, 1, xb)], UV, (1, 0, 0))
    d.quad('breeder_graphite', [(hi, RING, xa), (hi, RING, xb), (hi, 1, xb), (hi, 1, xa)], UV, (-1, 0, 0))
    for za, zb in zip(steps, steps[1:]):
        d.quad('breeder_amber', [(xa, 1, za), (xb, 1, za), (xb, 1, zb), (xa, 1, zb)], UV, (0, 1, 0))

# ---------------------------------------------------------------- the buttress legs
DECK_Y = 4 * B
DECK_W, DECK_C = 60, 18
for sx, sz in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
    foot = (C + sx * (C - 12), C + sz * (C - 12))
    head = (C + sx * 36, C + sz * 36)
    out = (sx / math.sqrt(2), 0, sz / math.sqrt(2))
    across = (-out[2], 0, out[0])
    d.sweep('drill_casing', [(foot[0], RING, foot[1]), (foot[0], RING + 10, foot[1]), (head[0], DECK_Y, head[1])], 7, 6,
            lambda i, v=across: v, closed=False, caps=True)
    # An orange clamp and a foot pad.
    mx, mz = foot[0] + (head[0] - foot[0]) * 0.4, foot[1] + (head[1] - foot[1]) * 0.4
    d.box('breeder_rail', mx - 6, 26, mz - 6, mx + 6, 29, mz + 6)
    d.box('drill_frame', foot[0] - 11, RING, foot[1] - 11, foot[0] + 11, RING + 4, foot[1] + 11, top='breeder_grate', skip=('down',))
    outline(foot[0] - 11, RING, foot[1] - 11, foot[0] + 11, RING + 4, foot[1] + 11, 'hazard')
    # A hydraulic ram from the leg's foot up under the deck, beside it.
    p0 = (foot[0] - sx * 8, RING + 4, foot[1])
    p1 = (head[0] - sx * 6, DECK_Y - 2, head[1] - sz * 10)
    mid = tuple(p0[i] + (p1[i] - p0[i]) * 0.5 for i in range(3))
    d.sweep('breeder_graphite', [p0, mid], 2, 2, lambda i: (0, 1, 0), closed=False, caps=True)
    d.sweep('breeder_fitting', [mid, p1], 1.3, 1.3, lambda i: (0, 1, 0), closed=False, caps=True)

# ---------------------------------------------------------------- the emitter and its beam
EMIT = 22
tip_y = DECK_Y - 26
d.box('drill_band', C - EMIT, DECK_Y - 6, C - EMIT, C + EMIT, DECK_Y, C + EMIT)
outline(C - EMIT, DECK_Y - 4, C - EMIT, C + EMIT, DECK_Y - 3, C + EMIT, 'glow')
base = [(C - EMIT + 3, DECK_Y - 6, C - EMIT + 3), (C + EMIT - 3, DECK_Y - 6, C - EMIT + 3),
        (C + EMIT - 3, DECK_Y - 6, C + EMIT - 3), (C - EMIT + 3, DECK_Y - 6, C + EMIT - 3)]
tip = (C, tip_y, C)
for k in range(4):
    a, b = base[k], base[(k + 1) % 4]
    d.quad('breeder_graphite', [a, b, tip, tip], [(0, 0), (16, 0), (8, 16), (8, 16)],
           ((a[0] + b[0]) / 2 - C, -0.7 * EMIT, (a[2] + b[2]) / 2 - C))
d.box('glow', C - 2.5, tip_y - 2, C - 2.5, C + 2.5, tip_y + 1, C + 2.5)
d.box('glow', C - 1, 1, C - 1, C + 1, tip_y - 2, C + 1, skip=('up', 'down'))
d.box('breeder_amber', C - 7, 1, C - 7, C + 7, 1.6, C + 7, skip=('down',))

# ---------------------------------------------------------------- the deck
prism('drill_band', DECK_Y, DECK_Y + 8, DECK_W, DECK_C, top='breeder_grate', bottom='drill_frame', v0=8)
prism('glow', DECK_Y + 3, DECK_Y + 4, DECK_W + 0.3, DECK_C)
# The handrail round the deck's edge, with a gap at the ladder on the front.
rail = corners(DECK_W - 3, DECK_C - 1)
RY = DECK_Y + 8
path = [(x, RY + 7, z) for x, z in rail[7:] + rail[:7]]
path = [(C + 10, RY + 7, C - DECK_W + 3)] + path[1:] + [(C - 10 + 0, RY + 7, C - DECK_W + 3)]
d.sweep('breeder_rail', path[:-1], 0.7, 0.7, lambda i: (0, 1, 0), closed=False, caps=True)
for x, z in rail:
    d.box('breeder_rail', x - 0.7, RY, z - 0.7, x + 0.7, RY + 7, z + 0.7)
# The ladder up the front, from the ground to the deck.
LZ = C - DECK_W - 1.5
for lx in (C - 5, C + 5):
    d.box('breeder_rail', lx - 0.8, 0, LZ - 0.8, lx + 0.8, RY + 10, LZ + 0.8)
for y in range(6, int(RY), 5):
    d.box('drill_band', C - 5, y, LZ - 0.5, C + 5, y + 1, LZ + 0.5)
# A console on the deck beside the ladder, and a tank at the back.
d.box('drill_casing', C + 18, RY, C - 46, C + 40, RY + 14, C - 36, top='breeder_graphite', skip=('down',))
d.box('breeder_fitting', C + 21, RY + 5, C - 46.5, C + 37, RY + 12, C - 46, decals={'north': 'screen'}, skip=('south',))
d.box('drill_pot', C - 44, RY, C + 26, C - 24, RY + 26, C + 46, top='drill_band')
d.box('drill_band', C - 45, RY + 12, C + 25, C - 23, RY + 16, C + 47, skip=('up', 'down'))

# ---------------------------------------------------------------- the column
COL = (RY, RY + 6 * B)
CW, CC = 26, 8
for x, z in corners(CW, CC):
    d.box('drill_casing', x - 2.5, COL[0], z - 2.5, x + 2.5, COL[1], z + 2.5, skip=('down', 'up'))
for face, (ax, az, bx, bz) in (('north', (C - CW + CC, C - CW, C + CW - CC, C - CW)), ('south', (C - CW + CC, C + CW, C + CW - CC, C + CW)),
                               ('west', (C - CW, C - CW + CC, C - CW, C + CW - CC)), ('east', (C + CW, C - CW + CC, C + CW, C + CW - CC))):
    normal = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}[face]
    for ya in range(int(COL[0]), int(COL[1]), 2 * B):
        yb = ya + 2 * B
        for p0, p1 in (((ax, ya, az), (bx, yb, bz)), ((ax, yb, az), (bx, ya, bz))):
            d.sweep('breeder_graphite', [p0, p1], 1.8, 2.2, lambda i, v=normal: v, closed=False, caps=True)
        d.sweep('breeder_graphite', [(ax, ya, az), (bx, ya, bz)], 1.8, 2.2, lambda i, v=normal: v, closed=False, caps=True)
d.box('breeder_graphite', C - 10, COL[0], C - 10, C + 10, COL[1], C + 10, skip=('down', 'up'))
d.box('breeder_amber', C - 10.3, COL[0] + 40, C - 10.3, C + 10.3, COL[0] + 44, C + 10.3, skip=('up', 'down'))
d.box('breeder_pipe', C + CW + 2, COL[0], C - 6, C + CW + 8, COL[1] + 4, C + 2)
for y in range(int(COL[0]) + 10, int(COL[1]), 18):
    d.box('breeder_rail', C + CW + 1.4, y, C - 6.6, C + CW + 8.6, y + 2, C + 2.6)

# ---------------------------------------------------------------- the collar and the head
COLLAR = (COL[1], COL[1] + 12)
prism('drill_ring', COLLAR[0], COLLAR[1], 40, 12, top='breeder_graphite', bottom='drill_frame', v0=4)
prism('glow', COLLAR[0] + 6.5, COLLAR[0] + 7.5, 40.3, 12)
HEAD = (COLLAR[1], COLLAR[1] + 3 * B)
prism('drill_casing', HEAD[0], HEAD[1], 34, 12)
# Windows on the four faces onto the generator glowing inside, and orange lamps on the corners.
for face, (x1, z1, x2, z2) in (('north', (C - 12, C - 34.3, C + 12, C - 34)), ('south', (C - 12, C + 34, C + 12, C + 34.3)),
                               ('west', (C - 34.3, C - 12, C - 34, C + 12)), ('east', (C + 34, C - 12, C + 34.3, C + 12))):
    d.box('breeder_graphite', x1 - 2 * (face in ('north', 'south')), HEAD[0] + 8, z1 - 2 * (face in ('west', 'east')),
          x2 + 2 * (face in ('north', 'south')), HEAD[1] - 8, z2 + 2 * (face in ('west', 'east')), skip=face_only(face))
    push = {'north': (0, -0.3), 'south': (0, 0.3), 'west': (-0.3, 0), 'east': (0.3, 0)}[face]
    d.box('breeder_amber', x1 + push[0], HEAD[0] + 11, z1 + push[1], x2 + push[0], HEAD[1] - 11, z2 + push[1], skip=face_only(face))
for x, z in corners(34, 12)[::2]:
    d.box('breeder_rail', x - 2, HEAD[1] - 14, z - 2, x + 2, HEAD[1] - 8, z + 2)
CAP = (HEAD[1], HEAD[1] + 8)
prism('drill_band', CAP[0], CAP[1], 36, 12, top='breeder_graphite', v0=8)
prism('drill_casing', CAP[1], CAP[1] + 10, 22, 7, top='breeder_grate')
d.box('breeder_graphite', C - 4, CAP[1] + 10, C - 4, C + 4, CAP[1] + 15, C + 4, skip=('down',))
d.box('breeder_amber', C - 2.5, CAP[1] + 15, C - 2.5, C + 2.5, CAP[1] + 18, C + 2.5, skip=('down',))


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'mega_drill.png')
    breeder_textures.main()
    drill_textures.main()
    d.save_png(out, [((1, 1), 2.0, 0.3), ((-1, 0.5), 2.0, 0.12)])


if __name__ == '__main__':
    main()
