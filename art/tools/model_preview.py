"""Renders generated block models as an isometric preview, straight from the datagen output.

Reads the model JSON in mod/src/generated and the textures in mod/src/main, so what you see is
what the game will load. Pure Python, no PIL needed. Run from the repo root after runData:
    python art/tools/model_preview.py [--scale N] OUT.png MODEL[@dx,dy,dz] [MODEL[@dx,dy,dz] ...]

Offsets are in blocks. For example the formed microreactor:
    python art/tools/model_preview.py preview.png microreactor_lower_front microreactor_upper_front@0,1,0 \
        microreactor_lower_back@0,0,1 microreactor_upper_back@0,1,1
"""
import json
import math
import os
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODELS = os.path.join(ROOT, 'mod', 'src', 'generated', 'resources', 'assets', 'ryzergen', 'models', 'block')
TEXTURES = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'textures')

# Minecraft's fixed face shading.
SHADE = {'up': 1.0, 'down': 0.5, 'north': 0.8, 'south': 0.8, 'west': 0.6, 'east': 0.6}
NORMAL = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1),
          'west': (-1, 0, 0), 'east': (1, 0, 0)}


def read_png(path):
    data = open(path, 'rb').read()
    pos, idat, w = 8, b'', 0
    while pos < len(data):
        length, tag = struct.unpack('>I4s', data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        if tag == b'IHDR':
            w, h, depth, ctype = struct.unpack('>IIBB', body[:10])
            assert depth == 8 and ctype in (2, 6), 'only 8-bit RGB/RGBA PNGs'
            bpp = 4 if ctype == 6 else 3
        elif tag == b'IDAT':
            idat += body
        pos += 12 + length
    raw = zlib.decompress(idat)
    stride = w * bpp
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        f, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append([tuple(line[x:x + bpp]) + ((255,) if bpp == 3 else ()) for x in range(0, stride, bpp)])
        prev = line
    return rows


_tex_cache = {}


def texture(ref):
    path = ref.split(':', 1)[-1]
    if path not in _tex_cache:
        _tex_cache[path] = read_png(os.path.join(TEXTURES, path + '.png'))
    return _tex_cache[path]


def auto_uv(face, f, t):
    """Minecraft's default UVs when a face gives none."""
    x1, y1, z1 = f
    x2, y2, z2 = t
    return {
        'north': (16 - x2, 16 - y2, 16 - x1, 16 - y1),
        'south': (x1, 16 - y2, x2, 16 - y1),
        'west': (z1, 16 - y2, z2, 16 - y1),
        'east': (16 - z2, 16 - y2, 16 - z1, 16 - y1),
        'up': (x1, z1, x2, z2),
        'down': (x1, 16 - z2, x2, 16 - z1),
    }[face]


def face_frame(face, f, t):
    """Corner at uv (u1, v1), then the directions of increasing u and v, in model pixels."""
    x1, y1, z1 = f
    x2, y2, z2 = t
    return {
        'north': ((x2, y2, z1), (x1 - x2, 0, 0), (0, y1 - y2, 0)),
        'south': ((x1, y2, z2), (x2 - x1, 0, 0), (0, y1 - y2, 0)),
        'west': ((x1, y2, z1), (0, 0, z2 - z1), (0, y1 - y2, 0)),
        'east': ((x2, y2, z2), (0, 0, z1 - z2), (0, y1 - y2, 0)),
        'up': ((x1, y2, z1), (x2 - x1, 0, 0), (0, 0, z2 - z1)),
        'down': ((x1, y1, z2), (x2 - x1, 0, 0), (0, 0, z1 - z2)),
    }[face]


TURN = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north', 'up': 'up', 'down': 'down'}


def turn(face, origin, eu, ev, quarters):
    """A blockstate y rotation: clockwise seen from above, 90 degrees per quarter."""
    for _ in range(quarters):
        face = TURN[face]
        origin = (16 - origin[2], origin[1], origin[0])
        eu = (-eu[2], eu[1], eu[0])
        ev = (-ev[2], ev[1], ev[0])
    return face, origin, eu, ev


def collect_faces(models):
    faces = []
    for name, (dx, dy, dz), rot in models:
        model = json.load(open(os.path.join(MODELS, name + '.json')))
        textures = model.get('textures', {})
        for el in model.get('elements', []):
            f, t = el['from'], el['to']
            for face, spec in el['faces'].items():
                ref = spec['texture']
                while ref.startswith('#'):
                    ref = textures[ref[1:]]
                uv = spec.get('uv') or auto_uv(face, f, t)
                origin, eu, ev = face_frame(face, f, t)
                face, origin, eu, ev = turn(face, origin, eu, ev, rot // 90)
                origin = (origin[0] + dx * 16, origin[1] + dy * 16, origin[2] + dz * 16)
                faces.append((face, origin, eu, ev, uv, texture(ref), spec.get('rotation', 0)))
    return faces


def render(faces, view, scale):
    """view is the horizontal direction the camera looks along, e.g. (1, 1) from the north west."""
    vx, vz = view
    n = math.hypot(vx, vz)
    vx, vz = vx / n, vz / n
    look = (vx * 0.866, -0.5, vz * 0.866)
    right = (-vz, 0, vx)

    def project(p):
        sx = (p[0] * right[0] + p[2] * right[2]) * scale
        sy = (-p[1] * 0.866 + (p[0] * vx + p[2] * vz) * -0.5) * scale
        depth = p[0] * look[0] + p[1] * look[1] + p[2] * look[2]
        return sx, sy, depth

    visible = [fc for fc in faces if sum(a * b for a, b in zip(NORMAL[fc[0]], look)) < 0]
    pts = [project(p) for fc in visible for p in (fc[1], tuple(fc[1][i] + fc[2][i] + fc[3][i] for i in range(3)),
                                                    tuple(fc[1][i] + fc[2][i] for i in range(3)),
                                                    tuple(fc[1][i] + fc[3][i] for i in range(3)))]
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
        k = SHADE[face]
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
                # Face rotation turns the texture clockwise on the face in 90 degree steps.
                su, tv = s, t
                for _ in range(rotation // 90):
                    su, tv = tv, 1 - su
                u = uv[0] + su * (uv[2] - uv[0])
                v = uv[1] + tv * (uv[3] - uv[1])
                r, g, b, al = tex[min(th - 1, int(v / 16 * th))][min(tw - 1, int(u / 16 * tw))]
                if al < 128:
                    continue
                zbuf[py][px] = depth
                img[py][px] = (int(r * k), int(g * k), int(b * k), 255)
    return img


def write_png(path, rows):
    h, w = len(rows), len(rows[0])
    raw = b''.join(b'\x00' + bytes(v for p in row for v in p) for row in rows)

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
                + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def main():
    args = sys.argv[1:]
    scale = 8
    if args[0] == '--scale':
        scale, args = int(args[1]), args[2:]
    out, specs = args[0], args[1:]
    models = []
    for spec in specs:
        # MODEL[@dx,dy,dz[@rotation]], the rotation being a blockstate y rotation in degrees.
        name, _, rest = spec.partition('@')
        off, _, rot = rest.partition('@')
        models.append((name, tuple(int(v) for v in off.split(',')) if off else (0, 0, 0), int(rot) if rot else 0))
    faces = collect_faces(models)
    views = [render(faces, (1, 1), scale), render(faces, (-1, -1), scale)]
    h = max(len(v) for v in views)
    rows = []
    for y in range(h):
        row = []
        for v in views:
            row += v[y] if y < len(v) else [(40, 44, 52, 255)] * len(v[0])
        rows.append(row)
    write_png(out, rows)


if __name__ == '__main__':
    main()
