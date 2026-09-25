"""Concept render of the fission power station (design section 8), before any of it is built.

The station is drawn as boxes in world pixels (16 to a block), cut at block boundaries the way the
microreactor's formed model is (so each block would get its own model), and rendered with the
mod's real textures by model_preview. Run:
    python art/tools/fission_concept.py [OUT.png]

Layout, bottom to top (block units, 12 across, centred on 6, 6):
    0-1    base plinth, hazard striped, with the ports on the front
    1-5    reactor chamber: a glass ring between steel ribs, the 5 x 5 core of rods inside
    5-6    reactor head: a dark band with a cyan light strip
    6-8    the roof is one giant turbine rotor, seen through open louvres
    8-11   the outer wall rises into a hyperbolic steam stack, open at the top
"""
import json
import math
import os
import shutil
import sys
import tempfile

import model_preview as mp

B = 16  # pixels per block
C = 6 * B  # centre of the footprint, in pixels


# Which part of the machine new boxes belong to. The game draws 'rotor' spinning and will draw
# 'rods' from the core layout the player sets; everything else is 'static'.
group = 'static'


class Box:
    def __init__(self, mat, x1, y1, z1, x2, y2, z2, decals=None, sides=None, top=None):
        self.group = group
        self.mat = mat
        self.f = (x1, y1, z1)
        self.t = (x2, y2, z2)
        self.decals = decals or {}
        self.sides = sides
        self.top = top


boxes = []


def box(*args, **kw):
    boxes.append(Box(*args, **kw))


def disc(mat, y1, y2, r, cx=C, cz=C, step=4, sides=None, top=None):
    """A cylinder standing upright, as strips `step` pixels wide."""
    x = -r
    while x < r:
        mid = min(abs(x), abs(x + step)) if x < 0 < x + step else min(abs(x), abs(x + step))
        half = math.sqrt(max(0, r * r - mid * mid))
        if half > 0.5:
            box(mat, cx + x, y1, cz - half, cx + x + step, y2, cz + half, sides=sides, top=top)
        x += step


def ring(mat, y1, y2, ro, ri, step=4, proud=0):
    """A cylinder wall: the strips of a disc with the middle left out. Each strip's piece reaches from
    the wall's outside at the strip's inner edge to its inside at the strip's outer edge, so pieces
    overlap their neighbours and a thin wall has no gaps on the curve.

    Strips are laid out from the centre (one straddles it), so both halves are mirror images and two
    rings of different radius share their strip edges. `proud` pushes each piece's outer end that
    far past its strip, so a trim ring laid on a wall stands clear of the wall's steps on every side."""
    x = -step / 2 - math.ceil(max(0, ro - step / 2) / step) * step
    while x < ro:
        near = min(abs(x), abs(x + step)) if not (x < 0 < x + step) else 0
        far = max(abs(x), abs(x + step))
        zo = math.sqrt(max(0, ro * ro - near * near))
        x1 = x - (proud if x + step <= 0 else 0)
        x2 = x + step + (proud if x >= 0 else 0)
        if zo > 0.5:
            if far < ri:
                zi = math.sqrt(ri * ri - far * far)
                box(mat, C + x1, y1, C - zo, C + x2, y2, C - zi)
                box(mat, C + x1, y1, C + zi, C + x2, y2, C + zo)
            else:
                box(mat, C + x1, y1, C - zo, C + x2, y2, C + zo)
        x += step


# ---------------------------------------------------------------- base
# A plain dark band, pulled in a little so the front ports stand clear of it.
disc('steel_dark', 0, 6, 6 * B - 3)
disc('steel', 6, 16, 6 * B - 4, top='steel_dark')
# The front four blocks of the base (cells 4 to 7 on the front row): water in, the control core,
# the fuel port and energy out. Each port is a 10 x 10 flange flush with its block's face (z = 0),
# with a neck back to the curved base; the core shows its screen there.
FRONT = [('port_coolant', 4), ('core', 5), ('port_fuel', 6), ('port_energy', 7)]
for kind, cell in FRONT:
    x = cell * B
    if kind == 'core':
        # A bezel flush with the block face, the 10 x 6 screen set in it, and a neck behind.
        box('steel_dark', x + 3, 5, 0, x + 13, 11, 0.5, decals={'north': 'screen'})
        box('steel_dark', x + 1, 2, 0.5, x + 15, 14, 1.5)
        box('steel_dark', x + 3, 3, 1.5, x + 13, 13, 6)
    else:
        box('steel_dark', x + 3, 3, 0, x + 13, 13, 1, decals={'north': kind})
        box('steel_dark', x + 4, 4, 1, x + 12, 12, 6)
# The output port (spent rods out) on the east flat, cell 4 from the front: the same flange, facing
# out to the side, since the front has only four flat blocks.
OUTPUT_CELL = 4
z = OUTPUT_CELL * B
box('steel_dark', 12 * B - 1, 3, z + 3, 12 * B, 13, z + 13, decals={'east': 'port_fuel'})
box('steel_dark', 12 * B - 6, 4, z + 4, 12 * B - 1, 12, z + 12)

# ---------------------------------------------------------------- reactor chamber
ring('glass', 16, 80, 6 * B - 10, 6 * B - 11)
for k in range(12):
    a = math.radians(k * 30 + 15)
    rx, rz = C + (6 * B - 10) * math.cos(a), C + (6 * B - 10) * math.sin(a)
    box('steel', rx - 4, 16, rz - 4, rx + 4, 80, rz + 4)
# Core: a raised pedestal, then the 5 x 5 channels hanging from the reactor head to the pedestal.
# Wide enough that the corner rods' collars (about 64 out) sit fully on it.
disc('steel_dark', 16, 22, 4.4 * B, sides='hazard')
disc('steel', 22, 24, 4.2 * B)
LAYOUT = [
    'CMFMC',
    'MFMFM',
    'FMXMF',
    'MFMFM',
    'CMFMC',
]
ROD = {'F': 'glow', 'M': 'steel_dark', 'X': 'steel', 'C': 'copper'}
group = 'rods'
for row, line in enumerate(LAYOUT):
    for col, kind in enumerate(line):
        x = C + (col - 2) * 20
        z = C + (row - 2) * 20
        box(ROD[kind], x - 4, 24, z - 4, x + 4, 80, z + 4)
        # A collar where each rod meets the pedestal.
        box('steel_dark', x - 5, 24, z - 5, x + 5, 27, z + 5)

group = 'static'

# ---------------------------------------------------------------- head
disc('steel_dark', 80, 96, 6 * B - 6)
ring('glow', 88, 89, 6 * B - 5.5, 6 * B - 7, proud=0.5)


def wall(mat, y1, y2, r, thickness=5):
    """A thin round wall of radius r (to its outside), gapless like ring()."""
    ring(mat, y1, y2, r, r - thickness)


# ---------------------------------------------------------------- turbine
# The whole roof is one turbine: steam rising from the core drives a rotor as wide as the station,
# then leaves up the stack. In game the rotor is the one moving part (spun by a renderer).
RY = 100
group = 'rotor'
disc('lead', 96, 116, 14)                      # hub
disc('steel_dark', 116, 120, 10)               # hub cap
for k in range(16):
    a = math.radians(k * 22.5)
    # Blades: a run of small steps from the hub to the shroud, fanning slightly as they go out, so
    # the rotor reads as one big fan disc.
    # Tips stop inside the shroud (inner radius 78), corners included.
    for step in range(14, 70, 3):
        bx = C + step * math.cos(a)
        bz = C + step * math.sin(a)
        half = 3 + step / 28
        box('steel', bx - half, RY, bz - half, bx + half, RY + 4, bz + half)
group = 'static'
# Shroud round the blade tips, inside the louvre ribs (which start at 84).
wall('steel_dark', 98, 108, 82, thickness=4)
# A bearing beam across the stack holds the hub.
box('steel_dark', C - 78, 116, C - 4, C + 78, 122, C + 4)
box('steel_dark', C - 4, 116, C - 78, C + 4, 122, C + 78)
# Struts from the beam ends down to the shroud, so the bearing sits on something.
for sx, sz in ((C - 80, C), (C + 80, C), (C, C - 80), (C, C + 80)):
    box('steel_dark', sx - 3, 108, sz - 3, sx + 3, 116, sz + 3)

# ---------------------------------------------------------------- the outer wall rises into a steam stack
# Around the rotor: open louvres between ribs, so the turbine can be seen turning.
for k in range(24):
    a = math.radians(k * 15)
    rx, rz = C + (6 * B - 9) * math.cos(a), C + (6 * B - 9) * math.sin(a)
    box('steel', rx - 3, 96, rz - 3, rx + 3, 124, rz + 3)
wall('steel_dark', 96, 98, 6 * B - 6, thickness=6)
# Above: a hyperbolic stack, narrowest two thirds of the way up, flaring to an open top.
TOP = 176
for y in range(124, TOP, 4):
    t = (y - 158) / 34
    r = (5.0 * B) * math.sqrt(1 + 0.22 * t * t)
    mat = 'steel_dark' if y in (124, TOP - 4) else 'steel'
    wall(mat, y, y + 4, r)
ring('glow', 140, 141, 5.0 * B * math.sqrt(1 + 0.22 * ((140 - 158) / 34) ** 2) + 0.5,
     5.0 * B * math.sqrt(1 + 0.22 * ((140 - 158) / 34) ** 2) - 2, proud=0.5)


# ---------------------------------------------------------------- cut into blocks and render

TEXTURE = {
    'glass': 'ryzergen:block/machine/tank_glass',
}


def tex(name):
    return TEXTURE.get(name, 'ryzergen:block/microreactor/' + name)


def render_low(faces, view, scale, rise=0.15):
    """A lower camera than model_preview's, close to a player standing beside the station."""
    return _render_pitch(faces, view, scale, rise)


def _render_pitch(faces, view, scale, rise):
    """A copy of model_preview.render with the camera's downward tilt as a parameter."""
    vx, vz = view
    n = math.hypot(vx, vz)
    vx, vz = vx / n, vz / n
    horiz = math.sqrt(1 - rise * rise)
    look = (vx * horiz, -rise, vz * horiz)
    right = (-vz, 0, vx)

    def project(p):
        sx = (p[0] * right[0] + p[2] * right[2]) * scale
        sy = (-p[1] * horiz + (p[0] * vx + p[2] * vz) * -rise) * scale
        depth = p[0] * look[0] + p[1] * look[1] + p[2] * look[2]
        return sx, sy, depth

    visible = [fc for fc in faces if sum(a * b for a, b in zip(mp.NORMAL[fc[0]], look)) < 0]
    pts = [project(p) for fc in visible for p in (fc[1], tuple(fc[1][i] + fc[2][i] + fc[3][i] for i in range(3)))]
    minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
    miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
    pad = 12
    w, h = int(maxx - minx) + 2 * pad, int(maxy - miny) + 2 * pad
    ox, oy = pad - minx, pad - miny
    img = [[(40, 44, 52, 255)] * w for _ in range(h)]
    zbuf = [[1e9] * w for _ in range(h)]
    for face, origin, eu, ev, uv, tex, rotation in visible:
        a = project(origin)
        pu = project(tuple(origin[i] + eu[i] for i in range(3)))
        pv = project(tuple(origin[i] + ev[i] for i in range(3)))
        e1 = (pu[0] - a[0], pu[1] - a[1])
        e2 = (pv[0] - a[0], pv[1] - a[1])
        det = e1[0] * e2[1] - e1[1] * e2[0]
        if abs(det) < 1e-9:
            continue
        xs = [a[0], pu[0], pv[0], pu[0] + e2[0]]
        ys = [a[1], pu[1], pv[1], pu[1] + e2[1]]
        th, tw = len(tex), len(tex[0])
        k = mp.SHADE[face]
        for py in range(max(0, int(min(ys) + oy)), min(h, int(max(ys) + oy) + 2)):
            for px in range(max(0, int(min(xs) + ox)), min(w, int(max(xs) + ox) + 2)):
                dx, dy = px + 0.5 - ox - a[0], py + 0.5 - oy - a[1]
                s = (dx * e2[1] - dy * e2[0]) / det
                t = (e1[0] * dy - e1[1] * dx) / det
                if not (0 <= s < 1 and 0 <= t < 1):
                    continue
                depth = a[2] + s * (pu[2] - a[2]) + t * (pv[2] - a[2])
                if depth >= zbuf[py][px] - 1e-4:
                    continue
                u = uv[0] + s * (uv[2] - uv[0])
                v = uv[1] + t * (uv[3] - uv[1])
                r, g, b, al = tex[min(th - 1, int(v / 16 * th))][min(tw - 1, int(u / 16 * tw))]
                if al < 128:
                    continue
                zbuf[py][px] = depth
                img[py][px] = (int(r * k), int(g * k), int(b * k), 255)
    return img


def cut():
    """Splits every box at block boundaries: {(bx, by, bz): [elements]} in each block's own space."""
    blocks = {}
    for b in boxes:
        lo = [int(math.floor(b.f[i] / B)) for i in range(3)]
        hi = [int(math.ceil(b.t[i] / B)) - 1 for i in range(3)]
        for bx in range(lo[0], hi[0] + 1):
            for by in range(lo[1], hi[1] + 1):
                for bz in range(lo[2], hi[2] + 1):
                    origin = (bx * B, by * B, bz * B)
                    f = [max(b.f[i], origin[i]) - origin[i] for i in range(3)]
                    t = [min(b.t[i], origin[i] + B) - origin[i] for i in range(3)]
                    if any(t[i] - f[i] <= 0.01 for i in range(3)):
                        continue
                    faces = {}
                    for face, axis, positive in (('west', 0, False), ('east', 0, True), ('down', 1, False),
                                                 ('up', 1, True), ('north', 2, False), ('south', 2, True)):
                        world = (t if positive else f)[axis] + origin[axis]
                        if abs(world - (b.t if positive else b.f)[axis]) > 0.01:
                            continue  # a cut face, inside the box
                        name = b.mat
                        if face in b.decals:
                            name = b.decals[face]
                            w = t[0] - f[0] if axis != 0 else t[2] - f[2]
                            h = t[1] - f[1] if axis != 1 else t[2] - f[2]
                            faces[face] = {'texture': '#' + name, 'uv': [0, 0, w, h]}
                            continue
                        if b.sides and axis != 1:
                            name = b.sides
                        if b.top and face == 'up':
                            name = b.top
                        faces[face] = {'texture': '#' + name}
                    blocks.setdefault((bx, by, bz), []).append({'from': f, 'to': t, 'faces': faces, 'group': b.group})
    return blocks


GAME_DATA = os.path.join(mp.ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'station', 'fission_station.json')
EMISSIVE = {'glow', 'screen'}


def hidden_faces(blocks):
    """Faces that can never be seen: flush against the opposite face of another solid box that covers
    them completely. Glass never hides anything. Returns a set of (block, element index, face)."""
    axis_of = {'west': (0, False), 'east': (0, True), 'down': (1, False), 'up': (1, True), 'north': (2, False), 'south': (2, True)}
    planes = {}
    for key, elements in blocks.items():
        origin = [key[i] * B for i in range(3)]
        for idx, el in enumerate(elements):
            f = [el['from'][i] + origin[i] for i in range(3)]
            t = [el['to'][i] + origin[i] for i in range(3)]
            for face, spec in el['faces'].items():
                axis, positive = axis_of[face]
                other = [i for i in range(3) if i != axis]
                plane = round((t if positive else f)[axis], 3)
                rect = (f[other[0]], f[other[1]], t[other[0]], t[other[1]])
                glass = spec['texture'] == '#glass'
                planes.setdefault((axis, plane), []).append((positive, rect, glass, (key, idx, face)))
    hidden = set()
    eps = 0.01
    for faces in planes.values():
        if len(faces) < 2:
            continue
        coverers = [(p, r) for p, r, g, _ in faces if not g]
        for positive, rect, glass, ident in faces:
            for p2, r2 in coverers:
                if p2 != positive and r2[0] <= rect[0] + eps and r2[1] <= rect[1] + eps                         and r2[2] >= rect[2] - eps and r2[3] >= rect[3] - eps:
                    hidden.add(ident)
                    break
    return hidden


def export(blocks, textures):
    """Writes the design for the game's station renderer: every face with its texture and UVs, in
    world pixels from the station's north-west bottom corner, grouped into static, rotor and rods."""
    short = {'north': 'n', 'south': 's', 'west': 'w', 'east': 'e', 'up': 'u', 'down': 'd'}
    groups = {'static': [], 'rotor': []}
    r = lambda v: round(v, 3)
    hidden = hidden_faces(blocks)
    for (bx, by, bz), elements in blocks.items():
        origin = (bx * B, by * B, bz * B)
        for idx, el in enumerate(elements):
            f, t = el['from'], el['to']
            faces = {}
            for face, spec in el['faces'].items():
                if ((bx, by, bz), idx, face) in hidden:
                    continue
                name = spec['texture'][1:]
                uv = spec.get('uv') or mp.auto_uv(face, f, t)
                faces[short[face]] = [name] + [r(v) for v in uv]
            # The game draws the rods itself, from the player's core layout.
            if not faces or el['group'] == 'rods':
                continue
            groups[el['group']].append([r(f[i] + origin[i]) for i in range(3)] + [r(t[i] + origin[i]) for i in range(3)] + [faces])
    os.makedirs(os.path.dirname(GAME_DATA), exist_ok=True)
    with open(GAME_DATA, 'w') as fh:
        json.dump({'textures': textures, 'emissive': sorted(EMISSIVE & set(textures)), 'groups': groups},
                  fh, separators=(',', ':'))
    total = sum(len(e[6]) for g in groups.values() for e in g)
    print(f'exported {sum(len(g) for g in groups.values())} elements, {total} faces ({len(hidden)} hidden faces dropped) -> {GAME_DATA}')


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'fission_station.png')
    blocks = cut()
    names = {b.mat for b in boxes} | {b.sides for b in boxes if b.sides} | {b.top for b in boxes if b.top} \
        | {d for b in boxes for d in b.decals.values()}
    textures = {n: tex(n) for n in names}
    export(blocks, textures)
    tmp = tempfile.mkdtemp()
    try:
        models = []
        for (bx, by, bz), elements in blocks.items():
            name = f'station_{bx}_{by}_{bz}'
            with open(os.path.join(tmp, name + '.json'), 'w') as f:
                json.dump({'textures': textures, 'elements': elements}, f)
            models.append((name, (bx, by, bz), 0))
        mp.MODELS = tmp
        faces = mp.collect_faces(models)
        scale = 3
        # From above at the usual angle, and at a player's eye level (looking through the glass).
        views = [mp.render(faces, (1, 1), scale), render_low(faces, (1, 0.35), scale)]
        h = max(len(v) for v in views)
        rows = []
        for y in range(h):
            row = []
            for v in views:
                row += v[y] if y < len(v) else [(40, 44, 52, 255)] * len(v[0])
            rows.append(row)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        mp.write_png(out, rows)
        print(f'{len(blocks)} blocks with geometry, {sum(len(e) for e in blocks.values())} elements -> {out}')
    finally:
        shutil.rmtree(tmp)


if __name__ == '__main__':
    main()
