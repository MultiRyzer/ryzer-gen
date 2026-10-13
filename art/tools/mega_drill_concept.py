"""The mega drill's design (design section 11c, option B), drawn as smooth quads and exported for
the game, which draws it round the real drill's pit and the creative preview's (MegaDrillRenderer).
The real drill's block layout (machine/drill/MegaDrillLayout) follows it. Run:
    python art/tools/mega_drill_concept.py [OUT.png]

One landmark tower that never moves, standing over the middle of a chunk on a 9 x 9 base (the
user's references, 4 Oct 2026): angular, not round (squares with their corners cut off), and open at
the foot so you see the laser going in. Bottom to top:
- The base: a hazard-striped ring round a 6 x 6 molten pit glowing amber, and four sloped buttress
  legs from the corners up under the deck, the space between them open. In the middle of each side
  a port console under a hooded housing: energy, tips and water in on the east (the front's left),
  the output on the west. From each port a service line (the mod's own cable and pipes, in the
  port's colour) runs up to the deck, over it and up the column into the collar; the output line
  comes back down the west side (13 Oct 2026, so you see where everything goes).
- The emitter: a pod hung under the deck, its beam leaving through a lit aperture in its foot,
  straight down into the pit, and four floodlights round it aimed down into the pit.
- The deck: a cut-corner square platform in tread plate, its edge a graphite band with the light
  line, a yellow handrail round it with a gap for the ladder up from the ground, the console
  against the column facing the ladder's top.
- The column: an open lattice, eight posts with big X braces on its four faces, the drive shaft
  inside with an amber band, the service lines up its east and west faces in orange brackets.
- The collar: a wider cut-corner ring with vents and its light line.
- The drive housing (reworked 4 Oct, as the round head looked like a lighthouse): a heavy box with
  louvred vents and an inspection hatch, generator pods with cooling fins bolted to its flanks, and
  on its roof a rig's crown frame with its sheave block and an aviation light on a mast, open
  cooling fans whose blades turn, short exhaust stacks and beacons on the corners that sweep their
  light round while it runs (the game moves the groups fan_0, fan_1, beacon, aviation and lamp_*).
Every surface has its own texture (drill_textures.py), drawn for the part it goes on and mapped
evenly: boxes cut into panels from their own corner (ebox), bars with their texture running on a
pixel a pixel (bar, not Design.sweep, which stretches one picture over a whole run), and the
cut-corner edges with their bands running on unbroken, centred on each edge (prism).
Real basis: blind-shaft boring rigs stand over the hole they bore and lift the cuttings up through
it; this one melts the rock with a beam (thermal spallation).
"""
import math
import os
import sys

import model_preview as mp
import drill_textures
from quad_design import Design, cross, dot, norm, sub

B = 16
SIZE = 9 * B
C = SIZE / 2
TEXTURES = {'drill_' + name: 'ryzergen:block/drill/' + name for name in drill_textures.NAMES}
TEXTURES['obsidian'] = 'minecraft:block/obsidian'
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in ('glow', 'glow_off')})
TEXTURES.update({name: 'ryzergen:block/station/' + name for name in (
    'housing_slope', 'housing_cheek', 'hazard_upright', 'tag_coolant', 'tag_fuel', 'tag_energy')})
LIT = {'drill_red_lamp', 'drill_flood', 'tag_coolant', 'tag_fuel', 'tag_energy', 'glow', 'drill_hot_obsidian', 'drill_hot_obsidian_core', 'drill_beam', 'drill_window', 'drill_screen', 'drill_amber', 'drill_lamps'}
d = Design(C, TEXTURES, LIT)
ALL = ('north', 'south', 'east', 'west', 'up', 'down')
UV = [(0, 0), (16, 0), (16, 16), (0, 16)]


def face_only(face):
    return tuple(f for f in ALL if f != face)


def outline(x1, y1, z1, x2, y2, z2, mat, t=0.3):
    d.box(mat, x1 - t, y1, z1 - t, x2 + t, y2, z2 + t, skip=('up', 'down'))


def ebox(mat, x1, y1, z1, x2, y2, z2, top=None, bottom=None, skip=(), v0=None, sides=None, panels=None):
    """An even box: each face cut into 16-pixel panels from its own corner (not the world's grid),
    so a box sized in whole panels shows every plate whole. A face shorter than a block shows the
    texture's foot (rows 16 - h to 16), or from row v0, so a band drawn at its height fits it.
    `sides` maps a face to its own texture, `panels` a single panel, (face, across, down) counted
    from the face's top left as you look at it, to a picture drawn for it."""
    sides = sides or {}
    panels = panels or {}
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
        for row, (yt, yb) in enumerate(zip(ys, ys[1:])):
            hh = yt - yb
            va = (16 - hh) if v0 is None else v0
            us = [16 * k for k in range(int((length - 0.01) // 16) + 1)] + [length]
            for col, (ua, ub) in enumerate(zip(us, us[1:])):
                pa = (a[0] + (b[0] - a[0]) * ua / length, a[1] + (b[1] - a[1]) * ua / length)
                pb = (a[0] + (b[0] - a[0]) * ub / length, a[1] + (b[1] - a[1]) * ub / length)
                d.quad(panels.get((face, col, row), tex), [(pa[0], yt, pa[1]), (pb[0], yt, pb[1]), (pb[0], yb, pb[1]), (pa[0], yb, pa[1])],
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


def bar(mat, path, hn, hb, b=(0, 1, 0), caps=True, phase=0.0):
    """A square-cut bar along a path, mapped evenly: its texture runs on along it a pixel a pixel,
    cut every 16 pixels (Design.sweep stretches one picture over a whole run), and across it each
    face shows rows 0 to its own width, row 0 along its upper or outer edge. `b` gives the bar's
    width direction (squared to the bar here); the faces across b are 2 hb wide, the others 2 hn.
    A bend is a true mitre: each edge runs on to the plane halfway between the two runs, so the
    bar keeps its full size round it. The texture is carried along each edge (u is the distance
    along the bar's middle, shared by every edge at each bend), so it wraps round the bend
    unbroken and no face folds back through another. `phase` shifts where the 16-pixel cuts fall."""
    def frame(t):
        w = norm(tuple(b[k] - dot(b, t) * t[k] for k in range(3)))
        return norm(cross(t, w)), w

    segs = [norm(sub(path[i + 1], path[i])) for i in range(len(path) - 1)]
    frames = [frame(t) for t in segs]
    signs = ((1, 1), (1, -1), (-1, -1), (-1, 1))

    def offset(p, fr, sn, sw):
        n, w = fr
        return tuple(p[k] + sn * hn * n[k] + sw * hb * w[k] for k in range(3))

    # Each path point's four corners: square to the run at the ends, on the mitre plane at a bend.
    verts = []
    for j, p in enumerate(path):
        if j == 0:
            verts.append({c: offset(p, frames[0], *c) for c in signs})
        elif j == len(path) - 1:
            verts.append({c: offset(p, frames[-1], *c) for c in signs})
        else:
            t_in, m = segs[j - 1], norm(tuple(segs[j - 1][k] + segs[j][k] for k in range(3)))
            corner = {}
            for c in signs:
                e = offset(p, frames[j - 1], *c)
                s = -dot(sub(e, p), m) / dot(t_in, m)
                corner[c] = tuple(e[k] + s * t_in[k] for k in range(3))
            verts.append(corner)
    us = [phase]
    for i in range(len(path) - 1):
        us.append(us[-1] + math.dist(path[i], path[i + 1]))

    def edge(c, u):
        for j in range(len(path) - 1):
            if u < us[j + 1] or j == len(path) - 2:
                f = (u - us[j]) / (us[j + 1] - us[j])
                return tuple(verts[j][c][k] + (verts[j + 1][c][k] - verts[j][c][k]) * f for k in range(3)), j

    breaks = sorted(set([round(u, 6) for u in us]
                        + [16.0 * m for m in range(int(us[0] // 16) + 1, int(us[-1] // 16) + 1) if us[0] + 0.01 < 16 * m < us[-1] - 0.01]))
    for u0, u1 in zip(breaks, breaks[1:]):
        if u1 - u0 < 1e-4:
            continue
        block = math.floor(u0 / 16 + 1e-6) * 16
        ua, ub = u0 - block, u1 - block
        for ca, cb, width, side, sign in (((1, 1), (1, -1), 2 * hb, 0, 1), ((-1, 1), (-1, -1), 2 * hb, 0, -1),
                                          ((1, 1), (-1, 1), 2 * hn, 1, 1), ((1, -1), (-1, -1), 2 * hn, 1, -1)):
            a0, seg = edge(ca, u0 + 1e-7)
            a1, _ = edge(ca, u1 - 1e-7)
            b0, _ = edge(cb, u0 + 1e-7)
            b1, _ = edge(cb, u1 - 1e-7)
            out = tuple(sign * v for v in frames[seg][side])
            d.quad(mat, [a0, a1, b1, b0], [(ua, 0), (ub, 0), (ub, min(16, width)), (ua, min(16, width))], out)
    if caps:
        for j, fr, sign in ((0, frames[0], -1), (len(path) - 1, frames[-1], 1)):
            t = cross(fr[1], fr[0])
            v = verts[j]
            d.quad(mat, [v[(1, 1)], v[(1, -1)], v[(-1, -1)], v[(-1, 1)]],
                   [(0, 0), (min(16, 2 * hb), 0), (min(16, 2 * hb), min(16, 2 * hn)), (0, min(16, 2 * hn))],
                   tuple(sign * k for k in t))


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
    """A cut-corner square prism. Its sides take bands and patterns that repeat within a block:
    each edge's texture runs on unbroken along it, centred on it (so the pattern ends the same at
    both corners), cut where it wraps. A top and bottom if asked."""
    pts = corners(w, c)
    h = y2 - y1
    va = (16 - h) if v0 is None else v0
    for k in range(8):
        (ax, az), (bx, bz) = pts[k], pts[(k + 1) % 8]
        length = math.hypot(bx - ax, bz - az)
        nx, nz = (bz - az) / length, -(bx - ax) / length
        mx, mz = (ax + bx) / 2 - C, (az + bz) / 2 - C
        if nx * mx + nz * mz < 0:
            nx, nz = -nx, -nz
        if inward:
            nx, nz = -nx, -nz
        off = (16 - length % 16) / 2
        cuts = [0.0] + [16 * m - off for m in range(1, int((length + off) // 16) + 1) if 0.01 < 16 * m - off < length - 0.01] + [length]
        for s0, s1 in zip(cuts, cuts[1:]):
            ua = (s0 + off) % 16 if s0 > 0 else off
            ub = ua + (s1 - s0)
            p0 = (ax + (bx - ax) * s0 / length, az + (bz - az) * s0 / length)
            p1 = (ax + (bx - ax) * s1 / length, az + (bz - az) * s1 / length)
            d.quad(mat, [(p0[0], y2, p0[1]), (p1[0], y2, p1[1]), (p1[0], y1, p1[1]), (p0[0], y1, p0[1])],
                   [(ua, va), (ub, va), (ub, va + min(16, h)), (ua, va + min(16, h))], (nx, 0, nz))
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
lo, hi = C - PIT, C + PIT
# The ring: a steel frame round the pit, standing on its legs' footings over the hole the drill
# mines from the surface down: its edges a lit lip over hazard stripes inside and out, its top bar
# grating, joists under it.
ebox('drill_base', 0, 0, 0, SIZE, RING, SIZE, skip=('up', 'down', 'east', 'west'))
# The ring's east and west faces stop at the port consoles, which stand in its middle cells.
CONSOLE = (3 * B, 6 * B)
for z1, z2 in ((0, CONSOLE[0]), (CONSOLE[1], SIZE)):
    ebox('drill_base', 0, 0, z1, SIZE, RING, z2, skip=('up', 'down', 'north', 'south'))
for x1, z1, x2, z2 in ((0, 0, SIZE, lo), (0, hi, SIZE, SIZE), (0, lo, lo, hi), (hi, lo, SIZE, hi)):
    d.box('drill_grating', x1, 0, z1, x2, RING, z2, skip=('north', 'south', 'east', 'west'))
    d.box('drill_underside', x1, 0, z1, x2, RING, z2, skip=('north', 'south', 'east', 'west', 'up'))
steps = [lo + k * B for k in range(int((hi - lo) // B))] + [hi]
WALL = [(0, 10), (16, 10), (16, 16), (0, 16)]
for xa, xb in zip(steps, steps[1:]):
    d.quad('drill_base', [(xa, RING, lo), (xb, RING, lo), (xb, 0, lo), (xa, 0, lo)], WALL, (0, 0, 1))
    d.quad('drill_base', [(xb, RING, hi), (xa, RING, hi), (xa, 0, hi), (xb, 0, hi)], WALL, (0, 0, -1))
    d.quad('drill_base', [(lo, RING, xb), (lo, RING, xa), (lo, 0, xa), (lo, 0, xb)], WALL, (1, 0, 0))
    d.quad('drill_base', [(hi, RING, xa), (hi, RING, xb), (hi, 0, xb), (hi, 0, xa)], WALL, (-1, 0, 0))
# The port consoles, a block deep in the middle three cells of the base's sides, their faces the
# block's faces. As you face the front (north), inputs on your left (east): energy, tungsten tips and
# water; on your right (west) the output for what the drill brings up, each port in its own cell,
# drawn into its panel so it sits flush (drill/panel_port_*): red energy, orange items, blue water.
# Each is housed as the fission station's ports are: a graphite hood sloping back over the ports
# with a lit tag above each in its ring colour, and graphite cheeks with hazard stripes either end.
TAGS = {'water': 'tag_coolant', 'items': 'tag_fuel', 'energy': 'tag_energy'}
HOOD_BACK, HOOD_TOP = 12, 26
SIDES = (('west', {1: 'items'}), ('east', {0: 'water', 1: 'items', 2: 'energy'}))


def console(side, ports):
    east = side == 'east'
    inside = 'west' if east else 'east'
    sx = 1 if east else -1

    def px(x):
        return SIZE - x if east else x

    def box(mat, x1, y1, z1, x2, y2, z2, **kw):
        a, b = sorted((px(x1), px(x2)))
        ebox(mat, a, y1, z1, b, y2, z2, **kw)

    box('drill_panel', 0, 0, CONSOLE[0], B, B, CONSOLE[1], skip=('down', 'up'), sides={inside: 'housing_cheek'},
        panels={(side, cell, 0): 'drill_panel_port_' + kind for cell, kind in ports.items()})
    # The hood, from the face's top edge back and up, and its back plate down to the body.
    d.quad('housing_slope', [(px(HOOD_BACK), HOOD_TOP, CONSOLE[1]), (px(0), B, CONSOLE[1]), (px(0), B, CONSOLE[0]),
                             (px(HOOD_BACK), HOOD_TOP, CONSOLE[0])],
           [(0, 0), (0, 16), (16, 16), (16, 0)], (sx * 10, HOOD_BACK, 0))
    box('housing_cheek', HOOD_BACK, B, CONSOLE[0], B, HOOD_TOP, CONSOLE[1], skip=('down',))
    # Cheeks either end, standing proud and a little taller, hazard striped on their outer face.
    for z1, z2 in ((CONSOLE[0] - 3, CONSOLE[0]), (CONSOLE[1], CONSOLE[1] + 3)):
        box('housing_cheek', -0.5, 0, z1, B, HOOD_TOP + 2, z2, skip=('down',))
        box('hazard_upright', -1, 0, z1, -0.5, HOOD_TOP + 2, z2, skip=('down', inside))
    # A slim light bar on the hood above each port, in its ring colour. (A face's panels count from
    # its top left as you look at it: the east face's from its south end.)
    lift = 0.3
    for cell, kind in ports.items():
        z = CONSOLE[1] - (cell + 1) * B if east else CONSOLE[0] + cell * B
        t0, t1 = 0.2, 0.33
        xa, ya = HOOD_BACK * t1, B + (HOOD_TOP - B) * t1
        xb, yb = HOOD_BACK * t0, B + (HOOD_TOP - B) * t0
        d.quad(TAGS[kind], [(px(xa) + sx * lift, ya + lift, z + 14), (px(xb) + sx * lift, yb + lift, z + 14),
                            (px(xb) + sx * lift, yb + lift, z + 2), (px(xa) + sx * lift, ya + lift, z + 2)],
               [(0, 0), (0, 3), (12, 3), (12, 0)], (sx * 10, HOOD_BACK, 0))


for side, ports in SIDES:
    console(side, ports)
# In the pictures only: the layer being mined, as red-hot obsidian, tiled on the block grid (half
# tiles at its edges) so the beam strikes the middle of one tile, white hot under it. In the game the
# real ground is mined and the renderer draws the heat over it, and the beam down to it.
FLOOR = [lo] + list(range(int(lo) + 8, int(hi), B)) + [hi]
for xa, xb in zip(FLOOR, FLOOR[1:]):
    for za, zb in zip(FLOOR, FLOOR[1:]):
        u0, v0 = xa % 16, za % 16
        tex = 'drill_hot_obsidian_core' if (xa, za) == (C - B / 2, C - B / 2) else 'drill_hot_obsidian'
        d.quad(tex, [(xa, 1, za), (xb, 1, za), (xb, 1, zb), (xa, 1, zb)],
               [(u0, v0), (u0 + xb - xa, v0), (u0 + xb - xa, v0 + zb - za), (u0, v0 + zb - za)], (0, 1, 0))

# A hazard border round the pit's edge, a pixel proud of the grating.
for x1, z1, x2, z2 in ((lo - 3, lo - 3, hi + 3, lo), (lo - 3, hi, hi + 3, hi + 3), (lo - 3, lo, lo, hi), (hi, lo, hi + 3, hi)):
    d.box('drill_hazard', x1, RING, z1, x2, RING + 0.4, z2, skip=('down',))

# ---------------------------------------------------------------- the buttress legs
DECK_Y = 4 * B
DECK_W, DECK_C = 60, 18
CW, CC = 26, 8          # the column's half width and cut corners
for sx, sz in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
    foot = (C + sx * (C - 12), C + sz * (C - 12))
    head = (C + sx * 36, C + sz * 36)
    out = (sx / math.sqrt(2), 0, sz / math.sqrt(2))
    across = (-out[2], 0, out[0])
    # Upright at both ends, so each meets the pad and the deck's underside flat (a bar's end is cut
    # square to its run, which on the slope would leave one edge in the deck and the other hanging),
    # mitred at its knees. Its plates are set so the top knee falls in the middle of one, which
    # wraps round it rather than ending on it.
    leg = [(foot[0], RING, foot[1]), (foot[0], RING + 10, foot[1]), (head[0], DECK_Y - 8, head[1]), (head[0], DECK_Y, head[1])]
    knee = math.dist(leg[0], leg[1]) + math.dist(leg[1], leg[2])
    bar('drill_leg', leg, 7, 7, across, phase=(8 - knee) % 16)
    # A foot pad, hazard striped round its edge, tread on top, an anchor bolt in each corner.
    ebox('drill_hazard', foot[0] - 11, RING, foot[1] - 11, foot[0] + 11, RING + 4, foot[1] + 11, top='drill_tread', skip=('down',))
    for ax in (-8, 8):
        for az in (-8, 8):
            ebox('drill_rod', foot[0] + ax - 1, RING + 4, foot[1] + az - 1, foot[0] + ax + 1, RING + 5, foot[1] + az + 1,
                 skip=('down',), v0=1)

# The deck's girders: graphite angle steel in a square from leg head to leg head, each end buried in
# its leg, so nothing runs on past the legs; the ribbed decking spans between them.
GIRDER = 36
for k in (-1, 1):
    bar('drill_brace', [(C - GIRDER, DECK_Y - 2, C + k * GIRDER), (C + GIRDER, DECK_Y - 2, C + k * GIRDER)], 2, 2)
    bar('drill_brace', [(C + k * GIRDER, DECK_Y - 2, C - GIRDER), (C + k * GIRDER, DECK_Y - 2, C + GIRDER)], 2, 2)

# Floodlights hung under the deck between the legs and the pod, aimed down into the pit: a short
# hanger, a graphite housing, and the lens on its underside, lit while the drill runs.
for sx in (-1, 1):
    for sz in (-1, 1):
        fx, fz = C + sx * 26, C + sz * 26
        ebox('drill_trim', fx - 0.6, DECK_Y - 4, fz - 0.6, fx + 0.6, DECK_Y, fz + 0.6, skip=('up',), v0=0)
        ebox('drill_band', fx - 3, DECK_Y - 8, fz - 3, fx + 3, DECK_Y - 4, fz + 3, top='drill_trim', bottom='drill_flood')

# ---------------------------------------------------------------- the emitter pod and its beam
# The emitter (6 Oct 2026, the user's pick, replacing a pyramid with a lens on its tip): a pod hung
# straight from the deck, one unit (its yoke, a crossbar and two arms on pivot bosses, was taken
# off on 12 Oct 2026 as too busy). The pod is a cut-corner box, two plates high: panels on its four faces, post steel on its corners, a light
# line round it. Under it a short collar, and in the collar's foot the aperture: a lit ring round the
# dark bore the beam leaves through (the renderer lights the bore as it charges).
POD_W, POD_C = 12, 4            # the pod: half width 12 (its flat faces one plate wide) and corners cut 4
POD = (30, 62)                  # its foot and top, two plates high, up to a band under the deck
APERTURE_Y = 26                 # the collar's foot, where the beam leaves
APERTURE_HALF = 5


def pod_prism(y1, y2, w, c, flat, corner, top=None, bottom=None):
    """A cut-corner prism with one texture on its flat faces (drawn whole: they are a plate wide) and
    another on its cut corners (a narrow texture, drawn at the corner's width)."""
    pts = corners(w, c)
    for k in range(8):
        (ax, az), (bx, bz) = pts[k], pts[(k + 1) % 8]
        length = math.hypot(bx - ax, bz - az)
        mx, mz = (ax + bx) / 2 - C, (az + bz) / 2 - C
        mat = flat if k % 2 == 0 else corner
        u = 16 if k % 2 == 0 else min(5, length)
        # A plate a block high, from the top down.
        for yt in range(int(y2), int(y1), -16):
            yb = max(y1, yt - 16)
            h = yt - yb
            d.quad(mat, [(ax, yt, az), (bx, yt, bz), (bx, yb, bz), (ax, yb, az)],
                   [(0, 16 - h), (u, 16 - h), (u, 16), (0, 16)], (mx, 0, mz))
    for cap, y, up in ((top, y2, 1), (bottom, y1, -1)):
        if cap is not None:
            tiled_cap(cap, pts, y, up)


# The pod, rising straight up to the deck as one unit, a graphite band where it meets the deck, and
# its light line round its lower plate.
pod_prism(POD[0], POD[1], POD_W, POD_C, 'drill_panel', 'drill_post', bottom='drill_lid')
prism('drill_band', POD[1], DECK_Y, POD_W, POD_C)
prism('glow', POD[0] + 11, POD[0] + 12, POD_W + 0.3, POD_C)
# The collar under the pod, and in its foot the aperture: a lit ring round the dark bore.
prism('drill_band', APERTURE_Y, POD[0], 7, 2, bottom='drill_trim')
ring = 1.5
for x1, z1, x2, z2 in ((C - APERTURE_HALF, C - APERTURE_HALF, C + APERTURE_HALF, C - APERTURE_HALF + ring),
                       (C - APERTURE_HALF, C + APERTURE_HALF - ring, C + APERTURE_HALF, C + APERTURE_HALF),
                       (C - APERTURE_HALF, C - APERTURE_HALF + ring, C - APERTURE_HALF + ring, C + APERTURE_HALF - ring),
                       (C + APERTURE_HALF - ring, C - APERTURE_HALF + ring, C + APERTURE_HALF, C + APERTURE_HALF - ring)):
    d.box('glow', x1, APERTURE_Y - 0.3, z1, x2, APERTURE_Y, z2, skip=('up',))
d.box('drill_beam', C - 1.2, 1, C - 1.2, C + 1.2, APERTURE_Y, C + 1.2, skip=('up', 'down'))

# ---------------------------------------------------------------- the deck
prism('drill_band8', DECK_Y, DECK_Y + 8, DECK_W, DECK_C, top='drill_tread', bottom='drill_underside', v0=8)
prism('glow', DECK_Y + 3, DECK_Y + 4, DECK_W + 0.3, DECK_C)
# The handrail round the deck's edge: one run from the right of the ladder's gap, round the deck,
# back to the left of it, with a post at every corner and either side of the gap.
GAP = 8
rail = corners(DECK_W - 3, DECK_C - 1)
RY = DECK_Y + 8
front_z = rail[6][1]
run = [(C + GAP, front_z)] + [rail[7]] + rail[0:7] + [(C - GAP, front_z)]
bar('drill_rail', [(x, RY + 8, z) for x, z in run], 1, 1)
bar('drill_rail', [(x, RY + 4, z) for x, z in run], 0.5, 0.5)
for x, z in run:
    ebox('drill_rail_post', x - 0.8, RY, z - 0.8, x + 0.8, RY + 9, z + 0.8, v0=0)
# The ladder up the front, in the rail's gap: two stiles from the ground to hand height over the
# deck's edge, rungs every 4 pixels.
LZ = C - DECK_W - 1.5
for lx in (C - 5, C + 5):
    ebox('drill_rail_post', lx - 0.8, 0, LZ - 0.8, lx + 0.8, RY + 9, LZ + 0.8, v0=0)
for y in range(4, int(RY), 4):
    ebox('drill_rail', C - 4.2, y, LZ - 0.5, C + 4.2, y + 1, LZ + 0.5, v0=0)

# The console: its back against the column, facing the top of the ladder, so it greets whoever
# climbs onto the deck: a desk with cabinet doors and three status lights, and an upright panel
# carrying the screen over its middle.
CON = (C - 16, C + 16)          # across the column's front face
CZ = C - 26                     # the column's front
ebox('drill_desk', CON[0], RY, CZ - 10, CON[1], RY + 8, CZ, top='drill_desk_top', skip=('down',),
     sides={'west': 'drill_band8', 'east': 'drill_band8'})
ebox('drill_panel', CON[0], RY + 8, CZ - 4, CON[1], RY + 24, CZ, top='drill_desk_top', skip=('down',))
d.box('drill_trim', C - 8, RY + 12, CZ - 4.6, C + 8, RY + 21, CZ - 4, decals={'north': 'drill_screen'}, skip=('south',))
for k in range(3):
    x0, y0, z0 = C - 7 + k * 6, RY + 8, CZ - 9
    ebox('drill_band', x0, y0, z0, x0 + 2, y0 + 0.6, z0 + 2, skip=('up', 'down'))
    # Each lamp a group of its own, lit by the game for the drill's state: green working, amber
    # waiting, red short of power (MegaDrillRenderer).
    d.group = f'lamp_{k}'
    d.quad('drill_lamps', [(x0, y0 + 0.6, z0), (x0 + 2, y0 + 0.6, z0), (x0 + 2, y0 + 0.6, z0 + 2), (x0, y0 + 0.6, z0 + 2)],
           [(4 * k, 0), (4 * k + 2, 0), (4 * k + 2, 2), (4 * k, 2)], (0, 1, 0))
    d.group = 'static'
# ---------------------------------------------------------------- the column
# Eight steel posts with a bolted splice plate over every joint, angle-steel X braces on its four
# faces (back to back where they cross), the drive shaft inside (ribbed, a flange each block, a lit band you see through the
# lattice), and a copper coolant duct up one side clamped at each joint.
COL = (RY, RY + 6 * B)
for x, z in corners(CW, CC):
    ebox('drill_post', x - 2.5, COL[0], z - 2.5, x + 2.5, COL[1], z + 2.5, skip=('down', 'up'), v0=0)
for face, (ax, az, bx, bz) in (('north', (C - CW + CC, C - CW, C + CW - CC, C - CW)), ('south', (C - CW + CC, C + CW, C + CW - CC, C + CW)),
                               ('west', (C - CW, C - CW + CC, C - CW, C + CW - CC)), ('east', (C + CW, C - CW + CC, C + CW, C + CW - CC))):
    normal = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}[face]
    # Each panel's X and its strut are bolted back to back, as a real tower's are, so no two bars
    # pass through each other: the rising diagonal outermost, the strut in the middle, the falling
    # diagonal behind it, each 2 pixels deep (the strut 1), all within the posts' depth.
    def layer(points, depth):
        return [(x + normal[0] * depth, y, z + normal[2] * depth) for x, y, z in points]

    for ya in range(int(COL[0]), int(COL[1]), 2 * B):
        yb = ya + 2 * B
        bar('drill_brace', layer([(ax, ya, az), (bx, yb, bz)], 1.6), 2, 1, normal)
        bar('drill_brace', layer([(ax, yb, az), (bx, ya, bz)], -1.6), 2, 1, normal)
        bar('drill_brace', [(ax, ya, az), (bx, ya, bz)], 2, 0.5, normal)
ebox('drill_shaft', C - 8, COL[0], C - 8, C + 8, COL[1], C + 8, skip=('down', 'up'))
ebox('drill_amber', C - 8.3, COL[0] + 40, C - 8.3, C + 8.3, COL[0] + 44, C + 8.3, skip=('up', 'down'))
# (The column's services, a line from each port, are drawn with the consoles' lines below.)

# The service lines, so you see where everything goes, each as the mod's own pipe in its port's colour:
# from the east console's three ports (water, tips, energy) straight up to the deck's underside, over
# the deck to the column and up its east face side by side into the collar, power and tips to the
# drive, water to cool it; and from the collar down the column's west face, over the deck and down
# into the west console's output port, what the drill brings up. Each line rises from its port's
# cell; they stand clear of the column's braces (2.6 out from its faces) and of the collar's knee
# braces (on its posts, z 53 to 55 and 89 to 91), held in an orange bracket at every block.
LINE_X = C + CW + 7                      # up the column's east face; the west face mirrors it
RISE_X = SIZE - 14                       # up from the consoles, under the deck's edge
LINES_IN = (('line_fluid', C + 12), ('line_items', C), ('line_energy', C - 12))
LINE_OUT = ('line_items', C)
for mat, z in LINES_IN:
    bar('drill_' + mat, [(RISE_X, 26, z), (RISE_X, DECK_Y, z)], 2, 2, (0, 0, 1))
    bar('drill_' + mat, [(RISE_X, RY + 1.5, z), (LINE_X, RY + 1.5, z), (LINE_X, COL[1], z)], 2, 2, (0, 0, 1))
mat, z = LINE_OUT
bar('drill_' + mat, [(SIZE - RISE_X, DECK_Y, z), (SIZE - RISE_X, 26, z)], 2, 2, (0, 0, 1))
bar('drill_' + mat, [(SIZE - LINE_X, COL[1], z), (SIZE - LINE_X, RY + 1.5, z), (SIZE - RISE_X, RY + 1.5, z)], 2, 2, (0, 0, 1))
# The brackets: across the three lines up the east face (back to the braces) and round the one on the
# west, and one on each riser group under the deck.
for y in range(int(RY) + B, int(COL[1]), B):
    ebox('drill_accent', C + CW + 2.6, y - 1, C - 15, LINE_X + 2.6, y + 1, C + 15, v0=0)
    ebox('drill_accent', SIZE - LINE_X - 2.6, y - 1, C - 3, SIZE - (C + CW + 2.6), y + 1, C + 3, v0=0)
ebox('drill_accent', RISE_X - 2.6, 44, C - 15, RISE_X + 2.6, 46, C + 15, v0=0)
ebox('drill_accent', SIZE - RISE_X - 2.6, 44, C - 3, SIZE - RISE_X + 2.6, 46, C + 3, v0=0)

# ---------------------------------------------------------------- the collar and the drive housing
# Not a lantern: a heavy drive housing with louvred vents, generator pods bolted to its flanks, and
# on its roof the crown frame, cooling fans, exhaust stacks and warning lamps, as a rig's top.
# Even textures: every box here is sized in whole 16-pixel panels and cut from its own corner,
# bands are drawn at their height, and the decals once at their size.
COLLAR = (COL[1], COL[1] + 12)
prism('drill_collar', COLLAR[0], COLLAR[1], 40, 12, top='drill_tread', bottom='drill_underside', v0=4)
prism('glow', COLLAR[0] + 7, COLLAR[0] + 8, 40.3, 12)
# Knee braces carry the collar where it overhangs the column: one from each post, up and out
# along the column's face, in the braces' angle steel, into the collar's underside.
for (x, z), (ox, oz) in zip(corners(CW, CC), ((1, 0), (1, 0), (0, 1), (0, 1), (-1, 0), (-1, 0), (0, -1), (0, -1))):
    bar('drill_brace', [(x, COLLAR[0] - 12, z), (x + ox * 12, COLLAR[0] + 2, z + oz * 12)], 2, 1, (-oz, 0, ox))
HW = 2 * B              # the housing: 4 x 4 panels across, 2 high
HOUSE = (COLLAR[1], COLLAR[1] + 2 * B)
# The front's lower row carries the inspection hatch on the left and the stencil on the right,
# each drawn into its panel.
ebox('drill_panel', C - HW, HOUSE[0], C - HW, C + HW, HOUSE[1], C + HW, skip=('up', 'down'),
     panels={('north', 0, 1): 'drill_panel_hatch', ('north', 3, 1): 'drill_panel_stencil'})
# Its cap band, 8 high and a pixel proud, with the light line under it; the roof in tread plate.
ROOF = HOUSE[1] + 8
ebox('drill_band8', C - HW - 1, HOUSE[1], C - HW - 1, C + HW + 1, ROOF, C + HW + 1, top='drill_tread', bottom='drill_underside', v0=8)
outline(C - HW, HOUSE[1] - 1.5, C - HW, C + HW, HOUSE[1] - 0.5, C + HW, 'glow')
# Louvred vents over the middle two panels of the upper row, front and back; the hatch's window,
# lit, in its door.
for z, face, push in ((C - HW, 'north', -0.3), (C + HW, 'south', 0.3)):
    a1, a2 = sorted((z, z + push))
    plate('drill_vent', face, C - B, HOUSE[0] + B, a1, C + B, HOUSE[1], a2)
d.box('drill_trim', C + B + 5, HOUSE[0] + 3, C - HW - 0.4, C + 2 * B - 5, HOUSE[0] + 13, C - HW, decals={'north': 'drill_window'}, skip=('south',))
# Generator pods on the flanks: half a block deep, so their faces stand flush over the collar's
# edge and they sit on the collar rather than hanging past it; two long, two high, graphite end
# plates, vents over their upper row, their light line level with the housing's so it runs on round.
for sx in (-1, 1):
    x1, x2 = sorted((C + sx * HW, C + sx * 40))
    ebox('drill_panel', x1, HOUSE[0], C - B, x2, HOUSE[1], C + B, top='drill_lid', skip=('down',),
         sides={'north': 'drill_trim', 'south': 'drill_trim'})
    outline(x1, HOUSE[1] - 1.5, C - B, x2, HOUSE[1] - 0.5, C + B, 'glow')
    fx = x2 if sx > 0 else x1
    a1, a2 = sorted((fx, fx + sx * 0.3))
    face = 'east' if sx > 0 else 'west'
    for za in (C - B, C):
        plate('drill_vent', face, a1, HOUSE[0] + B, za, a2, HOUSE[1], za + B)
# The crown frame on the roof: an A-frame of angle steel carrying a beam (a band, 8 high, two
# panels long) with the sheave block hung under it, as a drilling rig's crown, over the rotary
# table the drill string goes down through.
for sx in (-1, 1):
    for sz in (-1, 1):
        bar('drill_brace', [(C + sx * 22, ROOF, C + sz * 8), (C + sx * 6, ROOF + 24, C + sz * 8)], 2, 2, (0, 0, 1))
ebox('drill_band8', C - 8, ROOF + 24, C - B, C + 8, ROOF + 32, C + B, top='drill_lid', bottom='drill_underside', v0=8)
ebox('drill_sheave', C - 4, ROOF + 16, C - 8, C + 4, ROOF + 24, C + 8, bottom='drill_underside', skip=('up',),
     sides={'north': 'drill_sheave_end', 'south': 'drill_sheave_end'})
for z in (C - 5, C, C + 5):
    ebox('drill_pulley', C - 4.5, ROOF + 17, z - 1.5, C + 4.5, ROOF + 23, z + 1.5, top='drill_trim', bottom='drill_trim')
ebox('drill_band', C - 8, ROOF, C - 8, C + 8, ROOF + 4, C + 8, top='drill_rotary', skip=('down',))
def fan_blades(cx, cz, y, angle):
    """Four blades round a hub at (cx, cz), turned to angle degrees: in the game a part that turns
    while the drill works (and idles round slowly on standby)."""
    for k in range(4):
        a = math.radians(angle + 90 * k)
        ux, uz = math.cos(a), math.sin(a)
        vx, vz = -uz, ux
        r0, r1, w = 1.6, 6.6, 1.4
        pts = [(cx + ux * r0 + vx * w, y, cz + uz * r0 + vz * w), (cx + ux * r1 + vx * w * 1.3, y, cz + uz * r1 + vz * w * 1.3),
               (cx + ux * r1 - vx * w * 0.6, y, cz + uz * r1 - vz * w * 0.6), (cx + ux * r0 - vx * w, y, cz + uz * r0 - vz * w)]
        d.quad('drill_blade', pts, [(0, 0), (5, 0), (5, 3), (0, 3)], (0, 1, 0))


# Two cooling fans on the roof's front corners (a block each, an orange hub cap), two exhaust stacks
# at the back with an orange band.
# The fans stand a pixel clear of the corner lamps and of the crown's A-frame.
for sx in (-1, 1):
    fx = C + sx * 18
    # An open shroud: a dark floor, a rim round it, the blades turning inside, a guard across the top.
    ebox('drill_band', fx - 8, ROOF, C - 27, fx + 8, ROOF + 1.5, C - 11, top='drill_fan_well', skip=('down',))
    for rx1, rz1, rx2, rz2 in ((fx - 8, C - 27, fx + 8, C - 26), (fx - 8, C - 12, fx + 8, C - 11),
                               (fx - 8, C - 26, fx - 7, C - 12), (fx + 7, C - 26, fx + 8, C - 12)):
        ebox('drill_band', rx1, ROOF + 1.5, rz1, rx2, ROOF + 4, rz2, top='drill_trim', skip=('down',))
    # The blades and hub turn in the game (MegaDrillRenderer turns group fan_0 and fan_1 about the hub).
    d.group = 'fan_0' if sx < 0 else 'fan_1'
    fan_blades(fx, C - 19, ROOF + 2.8, 20 + sx * 15)
    ebox('drill_accent', fx - 1.5, ROOF + 1.5, C - 20.5, fx + 1.5, ROOF + 3.2, C - 17.5, skip=('down',), v0=0)
    d.group = 'static'
    for gx1, gz1, gx2, gz2 in ((fx - 7, C - 19.3, fx + 7, C - 18.7), (fx - 0.3, C - 26, fx + 0.3, C - 12)):
        ebox('drill_trim', gx1, ROOF + 3.6, gz1, gx2, ROOF + 4, gz2, v0=0)
    ebox('drill_stack', fx - 4, ROOF, C + 12, fx + 4, ROOF + 16, C + 20, top='drill_stack_top', skip=('down',))
    ebox('drill_accent', fx - 4.4, ROOF + 10, C + 11.6, fx + 4.4, ROOF + 13, C + 20.4, skip=('up', 'down'), v0=0)
# Warning lamps on the roof's corners.
for sx in (-1, 1):
    for sz in (-1, 1):
        lx, lz = C + sx * (HW - 3), C + sz * (HW - 3)
        ebox('drill_band', lx - 2.5, ROOF, lz - 2.5, lx + 2.5, ROOF + 2, lz + 2.5, top='drill_trim', skip=('down',))
        d.group = 'beacon'
        ebox('drill_amber', lx - 2, ROOF + 2, lz - 2, lx + 2, ROOF + 6, lz + 2, skip=('down', 'up'))
        d.group = 'static'
        ebox('drill_trim', lx - 2.3, ROOF + 6, lz - 2.3, lx + 2.3, ROOF + 6.8, lz + 2.3, skip=('down',), v0=0)

# The aviation light: a short mast on the crown beam's back end, a red lamp on top that blinks, as
# a tall structure carries.
ebox('drill_post', C - 0.8, ROOF + 32, C + 11.2, C + 0.8, ROOF + 42, C + 12.8, skip=('down',), v0=0)
ebox('drill_band', C - 1.8, ROOF + 42, C + 10.2, C + 1.8, ROOF + 43, C + 13.8, top='drill_trim', skip=('down',))
d.group = 'aviation'
ebox('drill_red_lamp', C - 1.4, ROOF + 43, C + 10.6, C + 1.4, ROOF + 46, C + 13.4, skip=('down', 'up'))
d.group = 'static'
ebox('drill_trim', C - 1.7, ROOF + 46, C + 10.3, C + 1.7, ROOF + 46.6, C + 13.7, skip=('down',), v0=0)


# ---------------------------------------------------------------- running and idle, export and pictures
# Idle: the pit plain obsidian, the beam off, the lamps, window, light lines and the aperture's
# ring dim.
IDLE = {'drill_flood': 'drill_flood_off', 'drill_hot_obsidian': 'obsidian', 'drill_hot_obsidian_core': 'obsidian', 'drill_amber': 'drill_amber_off', 'glow': 'glow_off', 'drill_window': 'drill_window_off',
        'drill_lamps': 'drill_lamps_off'}
# Drawn by the game itself, so left out of the export: the layer being mined and the beam.
PICTURE_ONLY = ('drill_hot_obsidian', 'drill_hot_obsidian_core', 'drill_beam')
RUNNING_ONLY = ('drill_beam',)
GAME_DATA = os.path.join(mp.ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'drill', 'mega_drill.json')


def idle(quads):
    return [(IDLE.get(t, t), v, n, g) for t, v, n, g in quads if t not in RUNNING_ONLY]


def export():
    """For the game's preview block (MegaDrillPreviewRenderer): the body that never changes in
    'static', what changes when it runs in 'on' (running) and 'off' (idle)."""
    running = d.quads
    d.quads = [q for q in running if q[0] not in PICTURE_ONLY]
    # The parts the game moves or lights by the drill's state keep their own groups (fan_0, fan_1,
    # beacon, aviation, lamp_0 to 2); the rest is the body, split by what changes when it runs.
    moving = [q for q in d.quads if q[3] != 'static']
    body = [q for q in d.quads if q[3] == 'static']
    changes = [q for q in body if q[0] in IDLE or q[0] in RUNNING_ONLY]
    d.quads = ([(t, v, n, 'static') for t, v, n, g in body if t not in IDLE and t not in RUNNING_ONLY]
               + [(t, v, n, 'on') for t, v, n, g in changes]
               + [(t, v, n, 'off') for t, v, n, g in idle(changes)]
               + moving)
    d.export(GAME_DATA)
    d.quads = running


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'mega_drill.png')
    drill_textures.main()
    export()
    d.save_png(out, [((1, 1), 2.0, 0.3), ((-1, 0.5), 2.0, 0.12)])
    running = d.quads
    d.quads = idle(running)
    d.save_png(out.replace('.png', '_idle.png'), [((1, 1), 2.0, 0.3)])
    d.quads = running


if __name__ == '__main__':
    main()
