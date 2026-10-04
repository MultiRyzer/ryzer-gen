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
  yellow handrail round it with a gap for the ladder up from the ground, and the console against the
  column facing the ladder's top.
- The column: an open lattice, eight posts with big X braces on its four faces, the drive shaft
  inside with an amber band, a square duct up one side.
- The collar: a wider cut-corner ring with vents and its light line.
- The drive housing (reworked 4 Oct, as the round head looked like a lighthouse): a heavy box with
  louvred vents and an inspection hatch, generator pods with cooling fins bolted to its flanks, and
  on its roof a rig's crown frame with its sheave block, square cooling fans, short exhaust stacks
  and warning lamps on the corners.
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
TEXTURES.update({'drill_' + name: 'ryzergen:block/drill/' + name for name in list(drill_textures.TEXTURES) + ['melt', 'beam']})
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in ('hazard', 'glow', 'screen')})
d = Design(C, TEXTURES, {'glow', 'screen', 'breeder_amber', 'drill_melt', 'drill_beam', 'drill_window'})
ALL = ('north', 'south', 'east', 'west', 'up', 'down')
UV = [(0, 0), (16, 0), (16, 16), (0, 16)]


def face_only(face):
    return tuple(f for f in ALL if f != face)


def outline(x1, y1, z1, x2, y2, z2, mat, t=0.3):
    d.box(mat, x1 - t, y1, z1 - t, x2 + t, y2, z2 + t, skip=('up', 'down'))


def ebox(mat, x1, y1, z1, x2, y2, z2, top=None, bottom=None, skip=(), v0=None, sides=None):
    """An even box: each face cut into 16-pixel panels from its own corner (not the world's grid),
    so a box sized in whole panels shows every plate whole. A face shorter than a block shows the
    texture's foot (rows 16 - h to 16), or from row v0, so a band drawn at its height fits it.
    `sides` maps a face to its own texture."""
    sides = sides or {}
    w, h, dz = x2 - x1, y2 - y1, z2 - z1
    faces = {
        'north': ((x2, z1), (x1, z1), w, (0, 0, -1)),
        'south': ((x1, z2), (x2, z2), w, (0, 0, 1)),
        'west': ((x1, z1), (x1, z2), dz, (-1, 0, 0)),
        'east': ((x2, z2), (x2, z1), dz, (1, 0, 0)),
    }
    for face, (a, b, length, out) in faces.items():
        if face in skip:
            continue
        tex = sides.get(face, mat)
        ys = [y2 - 16 * k for k in range(int((h - 0.01) // 16) + 1)] + [y1]
        for yt, yb in zip(ys, ys[1:]):
            hh = yt - yb
            va = (16 - hh) if v0 is None else v0
            us = [16 * k for k in range(int((length - 0.01) // 16) + 1)] + [length]
            for ua, ub in zip(us, us[1:]):
                pa = (a[0] + (b[0] - a[0]) * ua / length, a[1] + (b[1] - a[1]) * ua / length)
                pb = (a[0] + (b[0] - a[0]) * ub / length, a[1] + (b[1] - a[1]) * ub / length)
                d.quad(tex, [(pa[0], yt, pa[1]), (pb[0], yt, pb[1]), (pb[0], yb, pb[1]), (pa[0], yb, pa[1])],
                       [(0, va), (ub - ua, va), (ub - ua, va + hh), (0, va + hh)], out)
    for cap, y, up in ((top if top is not None else (None if 'up' in skip else mat), y2, 1),
                       (bottom if bottom is not None else (None if 'down' in skip else mat), y1, -1)):
        if cap is None:
            continue
        xs = [x1 + 16 * k for k in range(int((w - 0.01) // 16) + 1)] + [x2]
        zs = [z1 + 16 * k for k in range(int((dz - 0.01) // 16) + 1)] + [z2]
        for xa, xb in zip(xs, xs[1:]):
            for za, zb in zip(zs, zs[1:]):
                d.quad(cap, [(xa, y, za), (xb, y, za), (xb, y, zb), (xa, y, zb)],
                       [(0, 0), (xb - xa, 0), (xb - xa, zb - za), (0, zb - za)], (0, up, 0))


def plate(mat, face, x1, y1, z1, x2, y2, z2, decal=None):
    """A thin plate standing proud of a face (a vent, a hatch): its texture whole on that face,
    a decal drawn once at its exact size if given."""
    w = (x2 - x1) if face in ('north', 'south') else (z2 - z1)
    h = y2 - y1
    if decal:
        d.box(mat, x1, y1, z1, x2, y2, z2, decals={face: decal}, skip=('up', 'down'))
    else:
        ebox(mat, x1, y1, z1, x2, y2, z2, skip=('up', 'down'), v0=0 if h >= 16 else None)


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
        if cap is not None:
            tiled_cap(cap, pts, y, up)


def clip(poly, a, b):
    """Keeps the part of a polygon on the left of the line a to b (one step of clipping to a convex shape)."""
    out = []
    for i in range(len(poly)):
        p, q = poly[i], poly[(i + 1) % len(poly)]
        sp = (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0])
        sq = (b[0] - a[0]) * (q[1] - a[1]) - (b[1] - a[1]) * (q[0] - a[0])
        if sp >= 0:
            out.append(p)
        if (sp >= 0) != (sq >= 0):
            t = sp / (sp - sq)
            out.append((p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t))
    return out


def tiled_cap(mat, pts, y, up):
    """A flat cap over a convex outline, cut into block cells on the grid and each cell clipped to the
    outline, so its texture shows whole a block at a time, as the grid's flat faces do."""
    xs, zs = [p[0] for p in pts], [p[1] for p in pts]
    order = pts if sum((pts[i][0] * pts[(i + 1) % 8][1] - pts[(i + 1) % 8][0] * pts[i][1]) for i in range(8)) > 0 else pts[::-1]
    for gx in range(int(min(xs) // 16) * 16, int(max(xs)) + 1, 16):
        for gz in range(int(min(zs) // 16) * 16, int(max(zs)) + 1, 16):
            cell = [(gx, gz), (gx + 16, gz), (gx + 16, gz + 16), (gx, gz + 16)]
            for i in range(len(order)):
                cell = clip(cell, order[i], order[(i + 1) % len(order)])
                if len(cell) < 3:
                    break
            if len(cell) < 3:
                continue
            for k in range(1, len(cell) - 1, 2):
                q = [cell[0], cell[k], cell[k + 1], cell[min(k + 2, len(cell) - 1)]]
                d.quad(mat, [(x, y, z) for x, z in q], [(x - gx, z - gz) for x, z in q], (0, up, 0))


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
        d.quad('drill_melt', [(xa, 1, za), (xb, 1, za), (xb, 1, zb), (xa, 1, zb)], UV, (0, 1, 0))

# A hazard border round the pit's edge, a pixel proud of the ring.
for x1, z1, x2, z2 in ((lo - 3, lo - 3, hi + 3, lo), (lo - 3, hi, hi + 3, hi + 3), (lo - 3, lo, lo, hi), (hi, lo, hi + 3, hi)):
    d.box('hazard', x1, RING, z1, x2, RING + 0.4, z2, skip=('down',))
# Heat warning signs on the base's front, either side of the ladder.
for sx in (-1, 1):
    x = C + sx * 34
    d.box('breeder_fitting', x - 3, 0, -0.4, x + 3, 6, 0, decals={'north': 'drill_heat_sign'}, skip=('south',))

# ---------------------------------------------------------------- the buttress legs
DECK_Y = 4 * B
DECK_W, DECK_C = 60, 18
CW, CC = 26, 8          # the column's half width and cut corners
for sx, sz in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
    foot = (C + sx * (C - 12), C + sz * (C - 12))
    head = (C + sx * 36, C + sz * 36)
    out = (sx / math.sqrt(2), 0, sz / math.sqrt(2))
    across = (-out[2], 0, out[0])
    d.sweep('drill_leg', [(foot[0], RING, foot[1]), (foot[0], RING + 10, foot[1]), (head[0], DECK_Y, head[1])], 7, 6,
            lambda i, v=across: v, closed=False, caps=True)
    # A foot pad.
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
d.box('drill_band8', C - EMIT, DECK_Y - 6, C - EMIT, C + EMIT, DECK_Y, C + EMIT)
outline(C - EMIT, DECK_Y - 4, C - EMIT, C + EMIT, DECK_Y - 3, C + EMIT, 'glow')
base = [(C - EMIT + 3, DECK_Y - 6, C - EMIT + 3), (C + EMIT - 3, DECK_Y - 6, C - EMIT + 3),
        (C + EMIT - 3, DECK_Y - 6, C + EMIT - 3), (C - EMIT + 3, DECK_Y - 6, C + EMIT - 3)]
tip = (C, tip_y, C)
for k in range(4):
    a, b = base[k], base[(k + 1) % 4]
    d.quad('drill_emitter', [a, b, tip, tip], [(0, 0), (16, 0), (8, 16), (8, 16)],
           ((a[0] + b[0]) / 2 - C, -0.7 * EMIT, (a[2] + b[2]) / 2 - C))
d.box('glow', C - 2.5, tip_y - 2, C - 2.5, C + 2.5, tip_y + 1, C + 2.5)
d.box('drill_beam', C - 1.2, 1, C - 1.2, C + 1.2, tip_y - 2, C + 1.2, skip=('up', 'down'))
d.box('breeder_amber', C - 7, 1, C - 7, C + 7, 1.6, C + 7, skip=('down',))

# ---------------------------------------------------------------- the deck
prism('drill_band8', DECK_Y, DECK_Y + 8, DECK_W, DECK_C, top='breeder_plinth', bottom='breeder_graphite', v0=8)
prism('glow', DECK_Y + 3, DECK_Y + 4, DECK_W + 0.3, DECK_C)
# The handrail round the deck's edge: one run from the right of the ladder's gap, round the deck,
# back to the left of it, with a post at every corner and either side of the gap.
GAP = 8
rail = corners(DECK_W - 3, DECK_C - 1)
RY = DECK_Y + 8
front_z = rail[6][1]
run = [(C + GAP, front_z)] + [rail[7]] + rail[0:7] + [(C - GAP, front_z)]
d.sweep('breeder_rail', [(x, RY + 8, z) for x, z in run], 0.7, 0.7, lambda i: (0, 1, 0), closed=False, caps=True)
d.sweep('breeder_rail', [(x, RY + 4, z) for x, z in run], 0.5, 0.5, lambda i: (0, 1, 0), closed=False, caps=True)
for x, z in run:
    d.box('breeder_rail', x - 0.8, RY, z - 0.8, x + 0.8, RY + 8.7, z + 0.8)
# The ladder up the front, in the rail's gap: two stiles from the ground to hand height over the
# deck's edge, rungs every 4 pixels.
LZ = C - DECK_W - 1.5
for lx in (C - 5, C + 5):
    d.box('breeder_rail', lx - 0.8, 0, LZ - 0.8, lx + 0.8, RY + 8.7, LZ + 0.8)
for y in range(4, int(RY), 4):
    d.box('breeder_rail', C - 5, y, LZ - 0.5, C + 5, y + 1, LZ + 0.5)

# The console: its back against the column, facing the top of the ladder, so it greets whoever
# climbs onto the deck: a graphite desk with its lamps, and an upright panel carrying the screen.
CON = (C - 16, C + 16)          # across the column's front face
CZ = C - 26                     # the column's front
d.box('breeder_graphite', CON[0], RY, CZ - 10, CON[1], RY + 8, CZ, top='breeder_graphite', skip=('down',))
d.box('drill_casing', CON[0] + 2, RY + 8, CZ - 4, CON[1] - 2, RY + 24, CZ, top='breeder_graphite')
d.box('breeder_fitting', C - 8, RY + 12, CZ - 4.6, C + 8, RY + 21, CZ - 4, decals={'north': 'screen'}, skip=('south',))
for k in range(3):
    d.box('glow', C - 7 + k * 6, RY + 8, CZ - 9, C - 5 + k * 6, RY + 8.6, CZ - 7, skip=('down',))
# ---------------------------------------------------------------- the column
COL = (RY, RY + 6 * B)
for x, z in corners(CW, CC):
    ebox('drill_post', x - 2.5, COL[0], z - 2.5, x + 2.5, COL[1], z + 2.5, skip=('down', 'up'), v0=0)
for face, (ax, az, bx, bz) in (('north', (C - CW + CC, C - CW, C + CW - CC, C - CW)), ('south', (C - CW + CC, C + CW, C + CW - CC, C + CW)),
                               ('west', (C - CW, C - CW + CC, C - CW, C + CW - CC)), ('east', (C + CW, C - CW + CC, C + CW, C + CW - CC))):
    normal = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}[face]
    for ya in range(int(COL[0]), int(COL[1]), 2 * B):
        yb = ya + 2 * B
        for p0, p1 in (((ax, ya, az), (bx, yb, bz)), ((ax, yb, az), (bx, ya, bz))):
            d.sweep('drill_brace', [p0, p1], 1.8, 2.2, lambda i, v=normal: v, closed=False, caps=True)
        d.sweep('drill_brace', [(ax, ya, az), (bx, ya, bz)], 1.8, 2.2, lambda i, v=normal: v, closed=False, caps=True)
d.box('breeder_graphite', C - 10, COL[0], C - 10, C + 10, COL[1], C + 10, skip=('down', 'up'))
d.box('breeder_amber', C - 10.3, COL[0] + 40, C - 10.3, C + 10.3, COL[0] + 44, C + 10.3, skip=('up', 'down'))
d.box('breeder_pipe', C + CW + 2, COL[0], C - 6, C + CW + 8, COL[1] + 4, C + 2)
for y in range(int(COL[0]) + 10, int(COL[1]), 18):
    d.box('breeder_rail', C + CW + 1.4, y, C - 6.6, C + CW + 8.6, y + 2, C + 2.6)

# ---------------------------------------------------------------- the collar and the drive housing
# Not a lantern: a heavy drive housing with louvred vents, generator pods bolted to its flanks, and
# on its roof the crown frame, cooling fans, exhaust stacks and warning lamps, as a rig's top.
# Even textures: every box here is sized in whole 16-pixel panels and cut from its own corner,
# bands are drawn at their height, and the decals once at their size.
COLLAR = (COL[1], COL[1] + 12)
prism('drill_ring', COLLAR[0], COLLAR[1], 40, 12, top='breeder_grate', bottom='breeder_graphite', v0=4)
prism('glow', COLLAR[0] + 6.5, COLLAR[0] + 7.5, 40.3, 12)
HW = 2 * B              # the housing: 4 x 4 panels across, 2 high
HOUSE = (COLLAR[1], COLLAR[1] + 2 * B)
ebox('drill_panel', C - HW, HOUSE[0], C - HW, C + HW, HOUSE[1], C + HW, skip=('up', 'down'))
# Its cap band, 8 high and a pixel proud, with the light line under it; the roof in tread plate.
ROOF = HOUSE[1] + 8
ebox('drill_band8', C - HW - 1, HOUSE[1], C - HW - 1, C + HW + 1, ROOF, C + HW + 1, top='breeder_plinth', skip=('down',), v0=8)
outline(C - HW, HOUSE[1] - 1.5, C - HW, C + HW, HOUSE[1] - 0.5, C + HW, 'glow')
# The front: louvred vents over the middle two panels of the upper row, the stencil centred on the
# lower left panel, an inspection hatch over the lower right with its window onto the generator.
# The back carries vents too.
for z, face, push in ((C - HW, 'north', -0.3), (C + HW, 'south', 0.3)):
    a1, a2 = sorted((z, z + push))
    plate('drill_vent', face, C - B, HOUSE[0] + B, a1, C + B, HOUSE[1], a2)
z1, z2 = C - HW - 0.3, C - HW
d.box('drill_panel', C - HW + 2, HOUSE[0] + 5, z1, C - HW + 14, HOUSE[0] + 11, z2, decals={'north': 'drill_stencil'}, skip=('south',))
d.box('breeder_graphite', C + B + 2, HOUSE[0] + 2, z1 - 0.4, C + 2 * B - 2, HOUSE[0] + 14, z2, skip=('south',))
d.box('breeder_fitting', C + B + 5, HOUSE[0] + 3, z1 - 0.7, C + 2 * B - 5, HOUSE[0] + 13, z1 - 0.4, decals={'north': 'drill_window'}, skip=('south',))
# Generator pods on the flanks: a block deep, two long, two high, on a graphite bracket, vents over
# their upper row, the light line under their caps.
for sx in (-1, 1):
    x1, x2 = sorted((C + sx * HW, C + sx * (HW + B)))
    ebox('drill_panel', x1, HOUSE[0], C - B, x2, HOUSE[1], C + B, top='breeder_graphite', skip=('down',))
    ebox('breeder_graphite', x1 - 0.5, HOUSE[0] - 4, C - B - 0.5, x2 + 0.5, HOUSE[0], C + B + 0.5)
    outline(x1, HOUSE[1] - 3, C - B, x2, HOUSE[1] - 2, C + B, 'glow')
    fx = x2 if sx > 0 else x1
    a1, a2 = sorted((fx, fx + sx * 0.3))
    face = 'east' if sx > 0 else 'west'
    for za in (C - B, C):
        plate('drill_vent', face, a1, HOUSE[0] + B, za, a2, HOUSE[1], za + B)
# The crown frame on the roof: an A-frame of graphite legs carrying a beam (a band, 8 high, two
# panels long) with the sheave block hung under it, as a drilling rig's crown.
for sx in (-1, 1):
    for sz in (-1, 1):
        d.sweep('breeder_graphite', [(C + sx * 22, ROOF, C + sz * 8), (C + sx * 6, ROOF + 24, C + sz * 8)], 2, 2,
                lambda i: (0, 1, 0), closed=False, caps=True)
ebox('drill_band8', C - 8, ROOF + 24, C - B, C + 8, ROOF + 32, C + B, top='breeder_graphite', v0=8)
ebox('breeder_fitting', C - 4, ROOF + 16, C - 8, C + 4, ROOF + 24, C + 8)
for z in (C - 5, C, C + 5):
    ebox('breeder_rail', C - 4.5, ROOF + 17, z - 1.5, C + 4.5, ROOF + 23, z + 1.5)
ebox('breeder_graphite', C - 8, ROOF, C - 8, C + 8, ROOF + 4, C + 8, top='breeder_fitting', skip=('down',))
# Two cooling fans on the roof's front corners (a block each, a grille on top), two stacks at the back.
for sx in (-1, 1):
    fx = C + sx * 20
    ebox('breeder_graphite', fx - 8, ROOF, C - 28, fx + 8, ROOF + 4, C - 12, top='breeder_grate', skip=('down',))
    ebox('breeder_rail', fx - 1.5, ROOF + 4, C - 21.5, fx + 1.5, ROOF + 5, C - 18.5, skip=('down',))
    ebox('breeder_graphite', fx - 4, ROOF, C + 12, fx + 4, ROOF + 16, C + 20, skip=('down',))
    ebox('breeder_rail', fx - 4.4, ROOF + 10, C + 11.6, fx + 4.4, ROOF + 13, C + 20.4, skip=('up', 'down'))
# Warning lamps on the roof's corners.
for sx in (-1, 1):
    for sz in (-1, 1):
        lx, lz = C + sx * (HW - 3), C + sz * (HW - 3)
        ebox('breeder_graphite', lx - 2, ROOF, lz - 2, lx + 2, ROOF + 2, lz + 2, skip=('down',))
        ebox('breeder_amber', lx - 1.5, ROOF + 2, lz - 1.5, lx + 1.5, ROOF + 5, lz + 1.5, skip=('down',))


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'mega_drill.png')
    breeder_textures.main()
    drill_textures.main()
    d.save_png(out, [((1, 1), 2.0, 0.3), ((-1, 0.5), 2.0, 0.12)])
    # Idle: the pit crusted over and dark, the beam off, the windows and light lines dim.
    running = d.quads
    swap = {'drill_melt': 'drill_crust', 'breeder_amber': 'breeder_graphite', 'glow': 'breeder_graphite', 'drill_window': 'breeder_graphite'}
    d.quads = [(swap.get(t, t), v, n, g) for t, v, n, g in running if t != 'drill_beam']
    d.save_png(out.replace('.png', '_idle.png'), [((1, 1), 2.0, 0.3)])
    d.quads = running


if __name__ == '__main__':
    main()
