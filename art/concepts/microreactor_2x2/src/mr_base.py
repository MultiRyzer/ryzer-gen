"""Shared bits for the microreactor idea renders: concept textures and a Design with free axes."""
import math
import os
import sys

sys.path.insert(0, r'C:/Users/Quintinimus10k/Documents/Ryzer Gen/art/tools')
import model_preview as mp
from quad_design import Design, norm, cross, sub, dot

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'renders')
UP = (0, 1, 0)


def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def rnd(x, y, k=0):
    return ((x * 73856093) ^ (y * 19349663) ^ (k * 83492791)) % 1009 / 1009


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def mul(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def make(fn, w=16, h=16):
    return [[fn(x, y) for x in range(w)] for y in range(h)]


def panel(base, bevel=True, noise=0.035, rivets=False, k=0):
    def fn(x, y):
        c = mul(base, 1 + (rnd(x, y, k) - 0.5) * 2 * noise)
        if bevel:
            if y == 0 or x == 0:
                c = mul(base, 1.14)
            if y == 15 or x == 15:
                c = mul(base, 0.78)
        if rivets and (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            c = mul(base, 0.7)
        return (*c, 255)
    return make(fn)


def chrome():
    ramp = [1.0, 0.95, 0.85, 0.7, 0.55, 0.45, 0.42, 0.5, 0.65, 0.8, 0.92, 0.98, 0.9, 0.75, 0.6, 0.5]
    return make(lambda x, y: (*mul((205, 214, 226), 0.55 + 0.5 * ramp[y]), 255))


def pebbles(body, hi, gap, emissive_gap=None):
    centres = [(2 + 4 * i + (2 if j % 2 else 0) + (0.4 if (i + j) % 3 == 0 else 0), 2 + 4 * j) for i in range(4) for j in range(4)]

    def fn(x, y):
        best, bd = None, 9
        for cx, cy in centres:
            dx = min(abs(x + 0.5 - cx), 16 - abs(x + 0.5 - cx))
            dy = min(abs(y + 0.5 - cy), 16 - abs(y + 0.5 - cy))
            d = math.hypot(dx, dy)
            if d < bd:
                bd, best = d, (x + 0.5 - cx, y + 0.5 - cy)
        if bd > 1.95:
            return (*gap, 255)
        c = mix(body, mul(body, 0.6), bd / 1.95)
        if best[0] < -0.2 and best[1] < -0.2 and bd < 1.3:
            c = mix(c, hi, 0.7)
        return (*c, 255)
    return make(fn)


def heat():
    def fn(x, y):
        t = abs(x - 7.5) / 8
        c = mix((255, 214, 130), (196, 58, 22), t ** 0.8)
        return (*mul(c, 0.94 + 0.08 * rnd(x, y)), 255)
    return make(fn)


def rod_glow():
    return make(lambda x, y: (*mix((215, 250, 255), (50, 160, 250), abs(x - 7.5) / 8), 255))


def water(a=150):
    def fn(x, y):
        c = (18, 62, 140)
        if (x + 2 * y) % 11 == 0 or (x * 3 + y) % 13 == 0:
            c = (40, 104, 190)
        return (*c, a)
    return make(fn)


def glow_glass(col, a, sheen=True):
    def fn(x, y):
        s = max(0.0, 1 - abs((x - y) - 3) / 2.5) if sheen else 0
        return (*mix(col, (255, 255, 255), 0.5 * s), a + round(40 * s))
    return make(fn)


def exhaust():
    def fn(x, y):
        t = y / 15
        if t < 0.7:
            c = mix((255, 250, 215), (255, 170, 60), t / 0.7)
        else:
            c = mix((255, 170, 60), (170, 40, 12), (t - 0.7) / 0.3)
        return (*c, 255)
    return make(fn)


def dial(face=(238, 228, 200), needle=(200, 40, 30), angle=-40):
    def fn(x, y):
        dx, dy = x + 0.5 - 8, y + 0.5 - 8
        d = math.hypot(dx, dy)
        if d > 7.9:
            return (0, 0, 0, 0)
        if d > 7.0:
            return (40, 40, 44, 255)
        a = math.degrees(math.atan2(dx, -dy))
        if d > 5.2 and -135 <= a <= 135 and abs(a % 27) < 6:
            return (40, 40, 44, 255)
        if d > 4.2 and a > 85 and a < 135:
            return (200, 40, 30, 255)
        na = math.radians(angle)
        along = dx * math.sin(na) - dy * math.cos(na)
        across = abs(dx * math.cos(na) + dy * math.sin(na))
        if 0 <= along <= 6 and across < 0.7:
            return (*needle, 255)
        if d < 1.1:
            return (40, 40, 44, 255)
        return (*face, 255)
    return make(fn)


def tyre():
    def fn(x, y):
        c = (30, 30, 33)
        if y % 4 == 0:
            c = (18, 18, 20)
        if x in (0, 15):
            c = (44, 44, 48)
        return (*c, 255)
    return make(fn)


def fins(base, gap):
    return make(lambda x, y: (*(base if x % 3 else gap), 255))


def stencil(paint):
    """Olive paint with white stencil marks: a bar code of digits and a hazard plate."""
    marks = {(2, 5), (3, 5), (4, 5), (4, 6), (4, 7), (2, 8), (3, 8), (4, 8), (2, 9), (2, 10), (3, 10), (4, 10),
             (7, 5), (7, 6), (7, 7), (7, 8), (7, 9), (7, 10), (10, 5), (11, 5), (12, 5), (12, 6), (11, 7), (12, 7),
             (12, 8), (12, 9), (10, 10), (11, 10), (12, 10)}
    base = panel(paint)

    def fn(x, y):
        if (x, y) in marks:
            return (226, 226, 214, 255)
        return base[y][x]
    return make(fn)


def fan_round(blades=5, hub=(200, 120, 50)):
    def fn(x, y):
        dx, dy = x + 0.5 - 8, y + 0.5 - 8
        d = math.hypot(dx, dy)
        if d > 7.9:
            return (0, 0, 0, 0)
        if d > 7.1:
            return (58, 63, 70, 255)
        if d < 2.2:
            return (*mix(mul(hub, 1.2), hub, d / 2.2), 255)
        a = math.atan2(dy, dx) + d * 0.18
        f = (a * blades / (2 * math.pi)) % 1
        if f < 0.55:
            return (*mix((178, 186, 196), (120, 128, 140), f / 0.55), 255)
        return (24, 26, 30, 255)
    return make(fn)


def lamp(col):
    return make(lambda x, y: (*mix(mul(col, 1.25), col, max(abs(x - 7.5), abs(y - 7.5)) / 8), 255))


CONCEPT = {
    'pebbles': lambda: pebbles((48, 51, 58), (120, 128, 140), (12, 13, 16)),
    'pebbles_hot': lambda: pebbles((255, 160, 60), (255, 240, 190), (190, 50, 12)),
    'pebbles_warm': lambda: pebbles((170, 70, 34), (230, 130, 70), (60, 18, 8)),
    'pebbles_spent': lambda: pebbles((128, 112, 92), (178, 162, 138), (52, 46, 40)),
    'heat': heat,
    'rod_glow': rod_glow,
    'water': lambda: water(185),
    'water_lit': lambda: glow_glass((90, 210, 255), 55, sheen=False),
    'bubble': lambda: glow_glass((225, 245, 255), 150, sheen=False),
    'uglass': lambda: glow_glass((150, 255, 110), 95),
    'uglow': lambda: make(lambda x, y: (*mix((235, 255, 200), (110, 235, 70), abs(x - 7.5) / 8), 255)),
    'exhaust': exhaust,
    'dial': dial,
    'dial_small': lambda: dial(angle=30),
    'chrome': chrome,
    'cream': lambda: panel((232, 222, 196), noise=0.02),
    'cream_plain': lambda: panel((232, 222, 196), bevel=False, noise=0.02),
    'redpaint': lambda: panel((178, 46, 40), noise=0.02),
    'bakelite': lambda: panel((96, 34, 24), bevel=False),
    'olive': lambda: panel((92, 98, 56), rivets=True),
    'olive_plain': lambda: panel((92, 98, 56), bevel=False),
    'olive_dark': lambda: panel((60, 64, 38)),
    'stencil': lambda: stencil((92, 98, 56)),
    'tyre': tyre,
    'hub': lambda: panel((70, 74, 46), noise=0.05),
    'blade': lambda: make(lambda x, y: (*((120, 126, 136) if y < 2 else mix((205, 210, 218), (150, 156, 166), x / 15)), 255)),
    'fins_cu': lambda: fins((205, 120, 70), (110, 56, 30)),
    'fins_al': lambda: fins((200, 206, 214), (110, 116, 126)),
    'alu': lambda: panel((196, 202, 210), bevel=False, noise=0.02),
    'alu_edge': lambda: make(lambda x, y: (*((150, 156, 166) if y % 2 else (172, 178, 188)), 255)),
    'fan_round': fan_round,
    'lamp_red': lambda: lamp((235, 50, 40)),
    'lamp_amber': lambda: lamp((255, 170, 30)),
    'lamp_green': lambda: lamp((70, 230, 90)),
}

EXISTING = {
    **{n: 'ryzergen:block/microreactor/' + n for n in (
        'steel', 'steel_dark', 'lead', 'copper', 'hazard', 'glow', 'glow_off', 'accent', 'screen', 'grille',
        'radiator', 'porthole_on', 'port_energy', 'port_coolant', 'port_steam', 'port_fuel')},
    **{n: 'ryzergen:block/station/' + n for n in (
        'cherenkov', 'deck', 'housing', 'control', 'column', 'rib', 'copper_pipe', 'lamp', 'front_panel', 'warning')},
    **{n: 'ryzergen:block/breeder/' + n for n in (
        'fan', 'louvre', 'exchanger', 'girder', 'trefoil', 'shell', 'pump', 'grate', 'graphite', 'concrete')},
    'glass': 'ryzergen:block/machine/smooth_glass',
    'family_screen': 'ryzergen:block/machine/family/screen',
    'cabinet': 'ryzergen:block/machine/family/cabinet',
    'vent': 'ryzergen:block/machine/family/vent',
}

EMISSIVE = {'glow', 'screen', 'heat', 'pebbles_hot', 'pebbles_warm', 'rod_glow', 'water_lit', 'uglass', 'uglow',
            'exhaust', 'lamp_red', 'lamp_amber', 'lamp_green', 'porthole_on', 'cherenkov', 'lamp', 'control',
            'family_screen'}


def textures():
    out = dict(EXISTING)
    for name, fn in CONCEPT.items():
        ref = 'concept:' + name
        mp._tex_cache[ref] = fn()
        out[name] = ref
    return out


class C(Design):
    """A Design whose round parts turn about any axis, not just the machine's upright middle."""

    def __init__(self):
        super().__init__(8, textures(), EMISSIVE)

    def rev(self, mat, origin, axis, profile, n=16, mats=None, phase=0.0):
        """A surface of revolution about `axis` through `origin`: `profile` is (t along the axis,
        radius) points. A segment's outside is to the right of travel, so go out from the axis,
        along, and back in for a closed solid."""
        a = norm(axis)
        ref = UP if abs(a[1]) < 0.9 else (0, 0, 1)
        u = norm(cross(a, ref))
        v = cross(a, u)

        def P(t, r, phi):
            c, s = math.cos(phi), math.sin(phi)
            return tuple(origin[i] + a[i] * t + r * (c * u[i] + s * v[i]) for i in range(3))

        for k_seg in range(len(profile) - 1):
            (t1, r1), (t2, r2) = profile[k_seg], profile[k_seg + 1]
            if max(r1, r2) < 1e-6 or (abs(t2 - t1) < 1e-6 and abs(r2 - r1) < 1e-6):
                continue
            m = mats[k_seg] if mats else mat
            L = min(16, math.hypot(t2 - t1, r2 - r1))
            w = min(16, max(1, 2 * math.pi * max(r1, r2) / n))
            for k in range(n):
                p0 = phase + 2 * math.pi * k / n
                p1 = phase + 2 * math.pi * (k + 1) / n
                pm = (p0 + p1) / 2
                radial = tuple(math.cos(pm) * u[i] + math.sin(pm) * v[i] for i in range(3))
                out = norm(tuple(radial[i] * (t2 - t1) - a[i] * (r2 - r1) for i in range(3)))
                u0 = (k * w) % 16
                if u0 + w > 16:
                    u0 = 16 - w
                self.quad(m, [P(t2, r2, p0), P(t1, r1, p0), P(t1, r1, p1), P(t2, r2, p1)],
                          [(u0, 16 - L), (u0, 16), (u0 + w, 16), (u0 + w, 16 - L)], out)

    def rod(self, mat, origin, axis, length, r, n=12, caps=None):
        caps = caps or mat
        self.rev(mat, origin, axis, [(0, 0), (0, r), (length, r), (length, 0)], n=n, mats=[caps, mat, caps])

    def vrod(self, mat, x, z, y1, y2, r, n=12, caps=None):
        self.rod(mat, (x, y1, z), UP, y2 - y1, r, n, caps)

    def ring(self, mat, origin, axis, t1, t2, r_in, r_out, n=16):
        self.rev(mat, origin, axis, [(t1, r_in), (t1, r_out), (t2, r_out), (t2, r_in), (t1, r_in)], n=n)

    def band(self, mat, origin, axis, t1, t2, r, n=16):
        self.rev(mat, origin, axis, [(t1, r), (t2, r)], n=n)

    def ball(self, mat, c, r, n=10):
        prof = [(-r * math.cos(math.pi * k / 6), r * math.sin(math.pi * k / 6)) for k in range(7)]
        self.rev(mat, c, UP, prof, n=n)

    def pipe(self, mat, pts, r, n=10):
        for i in range(len(pts) - 1):
            p, q = pts[i], pts[i + 1]
            d = sub(q, p)
            self.rod(mat, p, d, math.sqrt(dot(d, d)), r, n)
        for p in pts[1:-1]:
            self.ball(mat, p, r * 1.02, n)

    def plate(self, mat, c, n, w, h):
        """A flat upright picture facing `n`, the whole texture stretched over w x h."""
        right = norm(cross(tuple(-v for v in n), UP))
        tl = tuple(c[i] - right[i] * w / 2 + UP[i] * h / 2 for i in range(3))
        bl = tuple(c[i] - right[i] * w / 2 - UP[i] * h / 2 for i in range(3))
        br = tuple(c[i] + right[i] * w / 2 - UP[i] * h / 2 for i in range(3))
        tr = tuple(c[i] + right[i] * w / 2 + UP[i] * h / 2 for i in range(3))
        self.quad(mat, [tl, bl, br, tr], [(0, 0), (0, 16), (16, 16), (16, 0)], n)

    def hplate(self, mat, cx, y, cz, w, dz=None, up=True):
        """A flat horizontal picture centred on (cx, y, cz), the whole texture stretched over it."""
        dz = dz or w
        x1, x2, z1, z2 = cx - w / 2, cx + w / 2, cz - dz / 2, cz + dz / 2
        self.quad(mat, [(x1, y, z1), (x1, y, z2), (x2, y, z2), (x2, y, z1)], [(0, 0), (0, 16), (16, 16), (16, 0)],
                  (0, 1 if up else -1, 0))

    def para(self, mat, o, e1, e2, e3, top=None):
        """A parallelepiped from corner `o` along edges e1, e2, e3."""
        P = lambda a, b, c: tuple(o[i] + a * e1[i] + b * e2[i] + c * e3[i] for i in range(3))
        centre = P(0.5, 0.5, 0.5)
        L = lambda e: min(16, math.sqrt(dot(e, e)))
        for fixed, (ea, eb) in ((0, (1, 2)), (1, (0, 2)), (2, (0, 1))):
            for val in (0, 1):
                def pt(s, t):
                    coords = [0, 0, 0]
                    coords[fixed], coords[ea], coords[eb] = val, s, t
                    return P(*coords)
                pts = [pt(0, 0), pt(1, 0), pt(1, 1), pt(0, 1)]
                fc = tuple(sum(p[i] for p in pts) / 4 for i in range(3))
                out = sub(fc, centre)
                ed = (e1, e2, e3)
                la, lb = L(ed[ea]), L(ed[eb])
                m = top if (top and fixed == 2 and val == 1) else mat
                self.quad(m, pts, [(0, 0), (la, 0), (la, lb), (0, lb)], out)

    def prism_x(self, mat, poly, x1, x2, edge=None):
        """A convex polygon in (z, y), extruded along x from x1 to x2."""
        cz = sum(p[0] for p in poly) / len(poly)
        cy = sum(p[1] for p in poly) / len(poly)
        zs, ys = [p[0] for p in poly], [p[1] for p in poly]
        for x, sgn in ((x1, -1), (x2, 1)):
            pts = [(x, y, z) for z, y in poly]
            uvs = [(min(16, z - min(zs)), min(16, max(ys) - y)) for z, y in poly]
            # pad a triangle to four points
            while len(pts) < 4:
                pts.append(pts[-1])
                uvs.append(uvs[-1])
            self.quad(mat, pts[:4], uvs[:4], (sgn, 0, 0))
        for i in range(len(poly)):
            (za, ya), (zb, yb) = poly[i], poly[(i + 1) % len(poly)]
            mz, my = (za + zb) / 2, (ya + yb) / 2
            out = (0, my - cy, mz - cz)
            ln = min(16, math.hypot(zb - za, yb - ya))
            self.quad(edge or mat, [(x1, ya, za), (x1, yb, zb), (x2, yb, zb), (x2, ya, za)],
                      [(0, 0), (0, ln), (x2 - x1, ln), (x2 - x1, 0)], out)

    def glass(self):
        self.group = 'glass'
        return self

    def solid(self):
        self.group = 'static'
        return self

    def save(self, name, scale=10, rise=0.5):
        os.makedirs(OUT, exist_ok=True)
        self.save_png(os.path.join(OUT, name + '.png'), [((1, 1), scale, rise), ((-1, -1), scale, rise)])
