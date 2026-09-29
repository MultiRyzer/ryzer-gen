"""Shared toolkit for the big machines drawn by a renderer (the fission station, the fusion reactor).

A design is a list of free quads in world pixels (16 to a block): smooth surfaces of revolution,
tori and swept bars for the round parts, and boxes for the straight parts, their faces tiled on the
16-pixel grid so textures keep their scale. `export` writes the quads for the game (StationGeometry
loads them into a GPU mesh), and `render` draws a concept picture with the same quads, glass
blended over the solid parts.

    d = Design(centre=96, textures={...}, emissive={...})
    d.cylinder('steel', 0, 16, 90)
    d.export(path); d.save_png(path)

Quads carry a group: the game draws 'static' from a mesh, turns 'rotor', and draws 'glass' (and
'steam', faded by the machine's power) translucent every frame. The picture blends 'glass' and adds the light of 'plasma' (a concept-only
group for glow the game draws in code). Groups listed in `concept_only` appear in the picture but are not exported
(the station's rods, which the game draws from the player's layout).
"""
import json
import math
import os

import model_preview as mp

PANEL = [(0, 0), (0, 16), (16, 16), (16, 0)]
FACES = {'west': (0, -1), 'east': (0, 1), 'down': (1, -1), 'up': (1, 1), 'north': (2, -1), 'south': (2, 1)}


def sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def norm(a):
    n = math.sqrt(dot(a, a)) or 1
    return (a[0] / n, a[1] / n, a[2] / n)


def grid(a, b):
    """Splits a..b at multiples of 16."""
    cuts = [a] + [k * 16 for k in range(math.floor(a / 16) + 1, math.ceil(b / 16))] + [b]
    return [(cuts[i], cuts[i + 1]) for i in range(len(cuts) - 1) if cuts[i + 1] - cuts[i] > 0.01]


def smooth_glass():
    """Smooth glass: a faint cool tint with one soft diagonal sheen. Drawn translucent, so there is
    no frame and no pixel noise."""
    tex = []
    for y in range(16):
        row = []
        for x in range(16):
            sheen = max(0.0, 1 - abs((x - y) - 3) / 2.5)
            c = tuple(round(v + (255 - v) * 0.6 * sheen) for v in (170, 215, 235))
            row.append((*c, 60 + round(50 * sheen)))
        tex.append(row)
    return tex


def write_texture(path, tex):
    """Writes a generated texture (e.g. 'block/machine/smooth_glass') to art/textures and the mod."""
    for base in (os.path.join(mp.ROOT, 'art', 'textures'), mp.TEXTURES):
        out = os.path.join(base, path + '.png')
        os.makedirs(os.path.dirname(out), exist_ok=True)
        mp.write_png(out, tex)
    mp._tex_cache.pop(path, None)


class Design:
    def __init__(self, centre, textures, emissive, concept_only=()):
        self.c = centre
        self.textures = textures
        self.emissive = set(emissive)
        self.concept_only = set(concept_only)
        self.group = 'static'
        self.quads = []

    # ------------------------------------------------------------ primitives
    def quad(self, tex, pts, uvs, outward):
        """Adds a quad, wound so it faces `outward` (the game culls the back)."""
        # Newell's method, so a quad with two corners together (a triangle) still gets a normal.
        n = [0.0, 0.0, 0.0]
        for i in range(4):
            p, q = pts[i], pts[(i + 1) % 4]
            n[0] += (p[1] - q[1]) * (p[2] + q[2])
            n[1] += (p[2] - q[2]) * (p[0] + q[0])
            n[2] += (p[0] - q[0]) * (p[1] + q[1])
        n = norm(n)
        if dot(n, outward) < 0:
            pts, uvs = pts[::-1], uvs[::-1]
            n = (-n[0], -n[1], -n[2])
        self.quads.append((tex, [(*p, *uv) for p, uv in zip(pts, uvs)], n, self.group))

    def box(self, mat, x1, y1, z1, x2, y2, z2, decals=None, sides=None, top=None, skip=()):
        """An axis-aligned box. Faces are tiled at the 16-pixel grid with Minecraft's default UVs;
        a decal face is drawn whole, from the texture's corner."""
        decals = decals or {}
        f, t = (x1, y1, z1), (x2, y2, z2)
        for face, (axis, sign) in FACES.items():
            if face in skip:
                continue
            name = decals.get(face) or (top if face == 'up' and top else sides if sides and axis != 1 else mat)
            outward = tuple(sign if i == axis else 0 for i in range(3))
            others = [i for i in range(3) if i != axis]
            if face in decals:
                tiles = [(f[others[0]], t[others[0]], f[others[1]], t[others[1]])]
            else:
                tiles = [(a1, a2, b1, b2) for a1, a2 in grid(f[others[0]], t[others[0]])
                         for b1, b2 in grid(f[others[1]], t[others[1]])]
            plane = t[axis] if sign > 0 else f[axis]
            # The block this face belongs to: the one just inside the box.
            base = math.floor((plane - sign * 0.01) / 16) * 16
            for a1, a2, b1, b2 in tiles:
                lo, hi, origin = [0.0] * 3, [0.0] * 3, [0.0] * 3
                for i, (p1, p2) in zip(others, ((a1, a2), (b1, b2))):
                    origin[i] = math.floor((p1 + 0.01) / 16) * 16
                    lo[i], hi[i] = p1 - origin[i], p2 - origin[i]
                origin[axis] = base
                lo[axis] = hi[axis] = plane - base
                if face in decals:
                    w = t[0] - f[0] if axis != 0 else t[2] - f[2]
                    h = t[1] - f[1] if axis != 1 else t[2] - f[2]
                    uv = (0, 0, w, h)
                else:
                    uv = mp.auto_uv(face, lo, hi)
                o, eu, ev = mp.face_frame(face, lo, hi)
                o = tuple(o[i] + origin[i] for i in range(3))
                pts = [o, tuple(o[i] + ev[i] for i in range(3)), tuple(o[i] + eu[i] + ev[i] for i in range(3)),
                       tuple(o[i] + eu[i] for i in range(3))]
                self.quad(name, pts, [(uv[0], uv[1]), (uv[0], uv[3]), (uv[2], uv[3]), (uv[2], uv[1])], outward)

    # ------------------------------------------------------------ round about the machine's upright axis
    def at(self, phi, rad, y):
        return (self.c + rad * math.cos(phi), y, self.c + rad * math.sin(phi))

    def steps(self, r, n=None):
        return n or max(12, math.ceil(2 * math.pi * r / 16))

    def cylinder(self, mat, y1, y2, r, n=None, inward=False, v0=None):
        """The side of an upright cylinder, one panel per step (each about 16 pixels round). A band
        shorter than a block shows the foot of the texture, or from row `v0` down if given (for a
        texture drawn for that band)."""
        self.lathe(mat, [(y1, r), (y2, r)], n=n, inward=inward, v0=v0)

    def lathe(self, mat, profile, n=None, inward=False, mats=None, v0=None):
        """A surface of revolution through `profile` points (y, radius), bottom to top. `mats`, if
        given, picks a material per profile segment."""
        n = self.steps(max(r for _, r in profile), n)
        for s in range(len(profile) - 1):
            (y1, r1), (y2, r2) = profile[s], profile[s + 1]
            m = mats[s] if mats else mat
            h = min(16, math.hypot(y2 - y1, r2 - r1))
            for k in range(n):
                p0, p1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
                pm = (p0 + p1) / 2
                # Outward normal of the slanted side: across the profile, away from the axis.
                dy, dr = y2 - y1, r2 - r1
                out = norm((dy * math.cos(pm), -dr, dy * math.sin(pm)))
                if inward:
                    out = (-out[0], -out[1], -out[2])
                top = 16 - h if v0 is None else v0
                self.quad(m, [self.at(p0, r2, y2), self.at(p0, r1, y1), self.at(p1, r1, y1), self.at(p1, r2, y2)],
                          [(0, top), (0, top + h), (16, top + h), (16, top)], out)

    def annulus(self, mat, y, r1, r2, up=True, step=16, n=None):
        """A flat ring (or disc, r1 = 0) of panels, facing up or down."""
        r = r1
        while r < r2 - 0.01:
            ro = min(r2, r + step)
            k_steps = self.steps(ro, n)
            for k in range(k_steps):
                p0, p1 = 2 * math.pi * k / k_steps, 2 * math.pi * (k + 1) / k_steps
                self.quad(mat, [self.at(p0, r, y), self.at(p0, ro, y), self.at(p1, ro, y), self.at(p1, r, y)],
                          PANEL, (0, 1 if up else -1, 0))
            r = ro

    def disc(self, mat, y1, y2, r, sides=None, top=None, bottom=True, n=None):
        """A solid upright cylinder: side, top and (optionally) bottom."""
        self.cylinder(sides or mat, y1, y2, r, n=n)
        self.annulus(top or mat, y2, 0, r, n=n)
        if bottom:
            self.annulus(mat, y1, 0, r, up=False, n=n)

    def wall(self, mat, y1, y2, r_out, r_in, inner=None, n=None):
        """A thick round wall: outside, inside, and the top and bottom rims."""
        n = self.steps(r_out, n)
        self.cylinder(mat, y1, y2, r_out, n=n)
        self.cylinder(inner or mat, y1, y2, r_in, n=n, inward=True)
        self.annulus(mat, y2, r_in, r_out, n=n)
        self.annulus(mat, y1, r_in, r_out, up=False, n=n)

    # ------------------------------------------------------------ bars
    def sweep(self, mat, path, half_n, half_b, binormal, closed=True, caps=False, along_v=False):
        """A rectangular bar along `path` (3D points). `binormal(i)` is the bar's width direction;
        the other direction is square to it and the path. `half_b` may be a function of the point.
        The texture runs along the bar in u, or in v with `along_v` (a texture drawn upright)."""
        count = len(path)
        width_at = half_b if callable(half_b) else (lambda i: half_b)
        frames = []
        for i in range(count):
            prev = path[i - 1] if (closed or i > 0) else path[i]
            nxt = path[(i + 1) % count] if (closed or i < count - 1) else path[i]
            tangent = norm(sub(nxt, prev))
            b = norm(binormal(i))
            frames.append((norm(cross(tangent, b)), b))

        def corner(i, sn, sb):
            n, b = frames[i]
            p = path[i]
            hb = width_at(i)
            return tuple(p[k] + sn * half_n * n[k] + sb * hb * b[k] for k in range(3))

        segments = count if closed else count - 1
        for i in range(segments):
            j = (i + 1) % count
            u = min(16, math.dist(path[i], path[j]))
            for (s1, t1), (s2, t2) in (((1, -1), (1, 1)), ((-1, 1), (-1, -1)), ((-1, -1), (1, -1)), ((1, 1), (-1, 1))):
                pts = [corner(i, s1, t1), corner(j, s1, t1), corner(j, s2, t2), corner(i, s2, t2)]
                mid_n = tuple((frames[i][0][k] + frames[j][0][k]) / 2 for k in range(3))
                mid_b = tuple((frames[i][1][k] + frames[j][1][k]) / 2 for k in range(3))
                side = tuple((s1 + s2) / 2 * mid_n[k] + (t1 + t2) / 2 * mid_b[k] for k in range(3))
                width = min(16, 2 * (half_n if s1 == s2 else width_at(i)))
                uvs = [(0, 0), (0, u), (width, u), (width, 0)] if along_v else [(0, 0), (u, 0), (u, width), (0, width)]
                self.quad(mat, pts, uvs, side)
        if caps and not closed:
            for i, sign in ((0, -1), (count - 1, 1)):
                t = norm(sub(path[min(i + 1, count - 1)], path[max(i - 1, 0)]))
                pts = [corner(i, 1, 1), corner(i, 1, -1), corner(i, -1, -1), corner(i, -1, 1)]
                hb = min(16, 2 * width_at(i))
                self.quad(mat, pts, [(0, 0), (0, hb), (min(16, 2 * half_n), hb), (min(16, 2 * half_n), 0)],
                          tuple(sign * v for v in t))

    def post(self, mat, phi, rad, y1, y2, half_radial, half_round):
        """An upright bar standing on the circle of radius `rad`, square to it (not to the grid). Cut
        every 16 pixels up, so a texture drawn upright (like station/rib) keeps its scale."""
        heights = [y1 + 16 * k for k in range(int((y2 - y1 - 0.01) // 16) + 1)] + [y2]
        path = [self.at(phi, rad, y) for y in heights]
        round_dir = (-math.sin(phi), 0, math.cos(phi))
        self.sweep(mat, path, half_radial, half_round, lambda i: round_dir, closed=False, caps=True, along_v=True)

    # ------------------------------------------------------------ output
    def export(self, path, extra=None):
        r = lambda v: round(v, 3)
        groups = {}
        for tex, verts, n, group in self.quads:
            if group in self.concept_only:
                continue
            groups.setdefault(group, []).append([tex] + [r(c) for v in verts for c in v] + [r(c) for c in n])
        used = {q[0] for g in groups.values() for q in g}
        data = {'textures': {k: v for k, v in self.textures.items() if k in used},
                'emissive': sorted(self.emissive & used), 'quads': groups}
        data.update(extra or {})
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as fh:
            json.dump(data, fh, separators=(',', ':'))
        total = sum(len(g) for g in groups.values())
        print(f'exported {total} quads ({", ".join(f"{g} {len(q)}" for g, q in groups.items())}) -> {path}')

    def render(self, view, scale, rise, background=(40, 44, 52)):
        """Draws every quad from a camera looking along `view`, tilted down by `rise`: solid quads
        with a depth buffer, then the glass blended over them. background None leaves the picture
        transparent where nothing is drawn (for the mod icon); see-through layers keep their own
        opacity there."""
        vx, vz = view
        k = math.hypot(vx, vz)
        vx, vz = vx / k, vz / k
        horiz = math.sqrt(1 - rise * rise)
        look = (vx * horiz, -rise, vz * horiz)
        right = (-vz, 0, vx)

        def project(p):
            return ((p[0] * right[0] + p[2] * right[2]) * scale,
                    (-p[1] * horiz + (p[0] * vx + p[2] * vz) * -rise) * scale,
                    dot(p, look))

        visible = [q for q in self.quads if dot(q[2], look) < 0]
        pts = [project(v[:3]) for q in visible for v in q[1]]
        minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
        miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
        pad = 12
        w, h = int(maxx - minx) + 2 * pad, int(maxy - miny) + 2 * pad
        ox, oy = pad - minx, pad - miny
        img = [[background or (0, 0, 0)] * w for _ in range(h)]
        alpha = [[255 if background else 0] * w for _ in range(h)]
        zbuf = [[1e9] * w for _ in range(h)]

        def shade(n):
            # Minecraft's face shading, blended for a slanted face.
            return n[0] * n[0] * 0.6 + n[2] * n[2] * 0.8 + n[1] * n[1] * (1.0 if n[1] > 0 else 0.5)

        def raster(q, blend):
            tex = mp.texture(self.textures[q[0]])
            # An animated texture is its frames stacked; the picture shows the first.
            tw = len(tex[0])
            th = min(len(tex), tw)
            light = 1.0 if q[0] in self.emissive else shade(q[2])
            proj = [project(v[:3]) for v in q[1]]
            for tri in ((0, 1, 2), (0, 2, 3)):
                (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = (proj[i] for i in tri)
                uvs = [q[1][i][3:] for i in tri]
                det = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
                if abs(det) < 1e-9:
                    continue
                for py in range(max(0, int(min(y0, y1, y2) + oy)), min(h, int(max(y0, y1, y2) + oy) + 2)):
                    for px in range(max(0, int(min(x0, x1, x2) + ox)), min(w, int(max(x0, x1, x2) + ox) + 2)):
                        sx, sy = px + 0.5 - ox, py + 0.5 - oy
                        b1 = ((sx - x0) * (y2 - y0) - (x2 - x0) * (sy - y0)) / det
                        b2 = ((x1 - x0) * (sy - y0) - (sx - x0) * (y1 - y0)) / det
                        b0 = 1 - b1 - b2
                        if b0 < 0 or b1 < 0 or b2 < 0:
                            continue
                        depth = b0 * z0 + b1 * z1 + b2 * z2
                        if depth >= zbuf[py][px] - 1e-4:
                            continue
                        u = b0 * uvs[0][0] + b1 * uvs[1][0] + b2 * uvs[2][0]
                        v = b0 * uvs[0][1] + b1 * uvs[1][1] + b2 * uvs[2][1]
                        r, g, b, a = tex[min(th - 1, max(0, int(v / 16 * th)))][min(tw - 1, max(0, int(u / 16 * tw)))]
                        if blend == 'add':
                            # Light added, tinted violet: black stays clear.
                            base = img[py][px]
                            img[py][px] = tuple(min(255, round(base[i] + c * t * 0.5)) for i, (c, t) in
                                                enumerate(zip((r, g, b), (0.85, 0.45, 1.0))))
                            alpha[py][px] = max(alpha[py][px], min(255, (r + g + b) // 3))
                            continue
                        if blend:
                            f = a / 255
                            if alpha[py][px] == 0:
                                img[py][px] = tuple(round(c * light) for c in (r, g, b))
                                alpha[py][px] = a
                                continue
                            base = img[py][px]
                            img[py][px] = tuple(round(base[i] * (1 - f) + c * light * f) for i, c in enumerate((r, g, b)))
                            alpha[py][px] = max(alpha[py][px], a)
                            continue
                        if a < 128:
                            continue
                        zbuf[py][px] = depth
                        alpha[py][px] = 255
                        img[py][px] = (int(r * light), int(g * light), int(b * light))

        blended = ('steam', 'plumes', 'plasma', 'glass')
        for q in visible:
            if q[3] not in blended:
                raster(q, False)
        for group in blended:
            for q in (self.quads if group == 'plasma' else visible):
                if q[3] == group:
                    raster(q, 'add' if group == 'plasma' else True)
        return [[(*p, alpha[y][x]) for x, p in enumerate(row)] for y, row in enumerate(img)]

    def save_png(self, out, views):
        """Renders each (view, scale, rise) side by side into one picture."""
        images = [self.render(*v) for v in views]
        h = max(len(v) for v in images)
        rows = []
        for y in range(h):
            row = []
            for v in images:
                row += v[y] if y < len(v) else [(40, 44, 52, 255)] * len(v[0])
            rows.append(row)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        mp.write_png(out, rows)
        print('rendered ->', out)
