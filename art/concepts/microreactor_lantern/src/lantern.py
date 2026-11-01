"""The Lantern: the microreactor as a pool reactor in a glass jar (concept, second pass).

One wide, two deep, two high, facing north: x 0 to 16 west to east, y up, z 0 to 32 front to back.

Front block: a lead-glass jar on a round pedestal. The sealed fuel core stands under water on the
jar's floor; three control rods hang beside it from drives on the lid. Running water-cooled, the
water round the core glows Cherenkov blue; dry, the jar is empty and the core glows with heat.
Back block: the plant. Lower half: the steam generator and turbine, with water in on the east
(your left as you face the front), steam out on the west (your right) and energy out on the back.
Upper half: the fuel bay, fuel in on the east and spent cores out on the west, the generator's
cooling fan on the back, and on the roof a telescopic crane that lifts cores in and out of the jar.

    python lantern.py STATE [--close]      STATE: idle, running, overdrive, loss, refuel
"""
import math
import os
import sys
import zlib

from mr_base import C, UP, mp, fan_round
import pixelart as px
from pixelart import Tex

STATE = sys.argv[1] if len(sys.argv) > 1 else 'running'
CLOSE = '--close' in sys.argv
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lantern')


# ================================================================ textures (concept only, in memory)
def rows(t, alpha=255):
    return [[(*t.rgba(x, y)[:3], alpha if t.px[y][x] else 0) for x in range(t.w)] for y in range(t.h)]


def fin(t, name, strength=1.0):
    return px.finish(t, zlib.crc32(name.encode()) & 0xFFFF, strength)


def rgb(k):
    c = px.PAL[k]
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))


def tex_rows(fn):
    return [[fn(x, y) for x in range(16)] for y in range(16)]


def skid_side():
    """The skid's 3-pixel side: a lit graphite lip over a band of hazard stripes."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'T')
    t.rect(0, 13, 15, 13, 'b')
    for y in (14, 15):
        for x in range(16):
            t.set(x, y, 'Y' if (x + y) % 4 < 2 else 'B')
    return rows(t)


def band(*keys):
    """A band for a round part, read from the foot: one palette key per row, bottom row last."""
    t = Tex()
    t.rect(0, 0, 15, 15, keys[0])
    for i, k in enumerate(keys):
        t.rect(0, 16 - len(keys) + i, 15, 16 - len(keys) + i, k)
    return rows(t)


def bolted_band():
    """A one-pixel flange round the jar: graphite with a bolt head every four pixels."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'b')
    for x in range(1, 16, 4):
        t.set(x, 15, 'H')
    return rows(t)


def clad():
    """Fuel rod cladding: bright zirconium, a faint ring every five pixels."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'H')
    for y in range(0, 16, 5):
        t.rect(0, y, 15, y, 'h')
    return rows(t)


def b4c():
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    for y in range(1, 16, 4):
        t.rect(0, y, 15, y, 'b')
    return rows(t)


def casing(w, h, name, fill='F', lit='A', lit2='E', shade='I', shade2='J'):
    """A light casing plate drawn at its size in the top left: a crisp bevel and a screw in each
    corner."""
    t = Tex()
    t.rect(0, 0, w - 1, h - 1, fill)
    t.rect(0, 0, w - 1, 0, lit)
    t.rect(0, 0, 0, h - 1, lit)
    t.rect(1, 1, w - 2, 1, lit2)
    t.rect(1, 1, 1, h - 2, lit2)
    t.rect(0, h - 1, w - 1, h - 1, shade2)
    t.rect(w - 1, 0, w - 1, h - 1, shade2)
    t.rect(1, h - 2, w - 2, h - 2, shade)
    t.rect(w - 2, 1, w - 2, h - 2, shade)
    for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
        t.set(x, y, shade2)
    return fin(t, name)


def roof():
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(1, 1, 14, 1, 'b')
    t.rect(1, 1, 1, 14, 'b')
    t.rect(1, 14, 14, 14, 'T')
    t.rect(14, 1, 14, 14, 'T')
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.set(x, y, 'U')
    return fin(t, 'lantern_roof')


def round_tex(fn):
    """A round picture for a disc, drawn as a square plate; outside the circle is clear."""
    def px_at(x, y):
        dx, dy = x + 0.5 - 8, y + 0.5 - 8
        d = math.hypot(dx, dy)
        if d > 7.95:
            return (0, 0, 0, 0)
        return fn(dx, dy, d, math.degrees(math.atan2(dy, dx)) % 360)
    return tex_rows(px_at)


DRIVE_ANGLES = (30, 150, 270)
DRIVE_R = 5.2


def lid(open_hatch=False):
    """The jar's lid from above: plain gunmetal, a ring of eight bolts, three sockets for the rod
    drives and the fuel hatch in the middle."""
    def fn(dx, dy, d, a):
        if d > 7.4:
            return (*rgb('z'), 255)
        if d > 6.5:
            near = abs((a - 22.5) % 45 - 22.5)
            return (*rgb('H' if near < 5 and d < 7.1 else 'u'), 255)
        for ang in DRIVE_ANGLES:
            sx, sy = DRIVE_R / 7.6 * 8 * math.cos(math.radians(ang)), DRIVE_R / 7.6 * 8 * math.sin(math.radians(ang))
            if math.hypot(dx - sx, dy - sy) < 1.5:
                return (*rgb('z'), 255)
        hatch = 3.6 / 7.6 * 8
        if d < hatch:
            if open_hatch:
                return (0, 0, 0, 0)
            if d > hatch - 0.7:
                return (*rgb('z'), 255)
            if abs(dy) < 0.6 and abs(dx) < 2.0:
                return (*rgb('X'), 255)
            return (*rgb('u'), 255)
        return (*rgb('s'), 255)
    return round_tex(fn)


def floor():
    """The jar's floor under the water: a plate with rings of flow holes."""
    def fn(dx, dy, d, a):
        if d > 7.4:
            return (*rgb('z'), 255)
        ring = round(d / 1.6)
        if ring >= 2 and abs(d - ring * 1.6) < 0.45 and (a * ring / 30) % 2 < 1:
            return (*rgb('U'), 255)
        return (*rgb('u'), 255)
    return round_tex(fn)


def water_side():
    def fn(x, y):
        base = (24, 82, 166) if (x * 7 + y * 3) % 17 else (38, 104, 190)
        deep = tuple(round(v * (1 - 0.18 * y / 15)) for v in base)
        return (*deep, 140)
    return tex_rows(fn)


def water_top():
    def fn(x, y):
        rip = (x + 2 * y) % 9 == 0 or (2 * x - y) % 11 == 0
        return (*((120, 190, 245) if rip else (52, 128, 210)), 150)
    return tex_rows(fn)


def glow_fade(col, top=3, power=1.0):
    """An additive glow read from the foot: full at the bottom row, fading to nothing by `top`."""
    def fn(x, y):
        t = max(0.0, (y - top) / (15 - top)) ** 1.4 * power
        return (*(round(v * t) for v in col), 255)
    return tex_rows(fn)


def glow_flat(col, power):
    return tex_rows(lambda x, y: (*(round(v * power) for v in col), 255))


def scale():
    """A level scale printed on the glass: ticks every two pixels, longer every four, a red line at
    the full mark."""
    def fn(x, y):
        if y in (0, 1) and x < 12:
            return (220, 60, 50, 255)
        if y % 4 == 3 and x < 10:
            return (236, 240, 244, 255)
        if y % 2 == 1 and x < 5:
            return (236, 240, 244, 255)
        return (0, 0, 0, 0)
    return tex_rows(fn)


def status(state):
    """The status strip on the pedestal, 6 x 2 in the top left: a lit bar of segments."""
    colour = {'running': 'i', 'overdrive': 'a', 'loss': 'x', 'idle': 'f', 'refuel': 'n'}[state]
    t = Tex()
    t.rect(0, 0, 15, 15, 'U')
    lit = {'idle': 1, 'running': 4, 'overdrive': 5, 'loss': 6, 'refuel': 2}[state]
    for x in range(6):
        t.set(x, 0, colour if x < lit else 'S')
    return rows(t)


def boom():
    t = Tex()
    t.rect(0, 0, 15, 15, 'X')
    t.rect(0, 0, 15, 0, 'a')
    for x in range(3, 16, 4):
        t.set(x, 1, 'Z')
    return rows(t)


def hatch_plate():
    t = Tex()
    t.rect(0, 0, 15, 15, 'z')
    t.rect(0, 0, 3, 3, 'z')
    t.rect(1, 1, 2, 2, 'u')
    t.set(1, 2, 'X')
    t.set(2, 2, 'X')
    return rows(t)


def strip(k, k2):
    t = Tex()
    t.rect(0, 0, 15, 15, k)
    for y in range(0, 16, 2):
        t.rect(0, y, 15, y, k2)
    return rows(t)


STRIP = {'running': 'glow', 'idle': 'glow_off', 'refuel': 'glow', 'overdrive': 'strip_amber', 'loss': 'strip_red'}[STATE]

CONCEPT = {
    'skid_side': skid_side,
    'drum': lambda: band('u', 's', 'u', 'z'),
    'lid_drum': lambda: band('u', 's'),
    'bolted': bolted_band,
    'trim': lambda: band('T', 'T'),
    'clad': clad,
    'b4c': b4c,
    'cab_low': lambda: casing(15, 13, 'lantern_cab_low'),
    'cab_up': lambda: casing(14, 13, 'lantern_cab_up'),
    'cab_front': lambda: casing(15, 13, 'lantern_cab_front'),
    'roof': roof,
    'lid_top': lambda: lid(STATE == 'refuel'),
    'floor': floor,
    'water': water_side,
    'water_top': water_top,
    'water_far': lambda: tex_rows(lambda x, y: (14, 52, 118, 200)),
    'ck_core': lambda: glow_fade((70, 175, 255), top=1, power=0.5),
    'ck_mid': lambda: glow_fade((50, 140, 255), top=2, power=0.36),
    'ck_out': lambda: glow_fade((40, 110, 240), top=4, power=0.24),
    'ck_cap': lambda: glow_flat((40, 110, 240), 0.10),
    'hk_core': lambda: glow_fade((255, 120, 40), top=4, power=0.6),
    'bubble': lambda: tex_rows(lambda x, y: (225, 245, 255, 150)),
    'smoke': lambda: tex_rows(lambda x, y: (150, 150, 150, 90 if (x + y) % 3 else 110)),
    'scale': scale,
    'status': lambda: status(STATE),
    'boom': boom,
    'hatch': hatch_plate,
    'strip_amber': lambda: strip('a', 'X'),
    'strip_red': lambda: strip('x', '8'),
    'fan': lambda: fan_round(5, (53, 200, 245)),
}


def textures(design):
    for name, fn in CONCEPT.items():
        mp._tex_cache['lantern:' + name] = fn()
        design.textures[name] = 'lantern:' + name
    design.textures['plinth'] = 'ryzergen:block/breeder/plinth'


EMISSIVE = {'ck_core', 'ck_mid', 'ck_out', 'ck_cap', 'hk_core', 'status', 'strip_amber', 'strip_red', 'fan_hub'}


# ================================================================ the renderer: water, glass and light
class Lantern(C):
    """Adds the jar's see-through layers: water and bubbles blended, then the glass, then the
    Cherenkov (or heat) glow added as light."""
    BLEND = ('glass_far', 'water_far', 'water', 'bubbles', 'glass', 'steam')
    ADD = ('light',)

    def render(self, view, scale, rise, background=(40, 44, 52)):
        vx, vz = view
        k = math.hypot(vx, vz)
        vx, vz = vx / k, vz / k
        horiz = math.sqrt(1 - rise * rise)
        look = (vx * horiz, -rise, vz * horiz)
        right = (-vz, 0, vx)
        dot = lambda a, b: a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

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
        img = [[background] * w for _ in range(h)]
        zbuf = [[1e9] * w for _ in range(h)]

        def shade(n):
            return n[0] * n[0] * 0.6 + n[2] * n[2] * 0.8 + n[1] * n[1] * (1.0 if n[1] > 0 else 0.5)

        def raster(q, mode):
            tex = mp.texture(self.textures[q[0]])
            tw, th = len(tex[0]), min(len(tex), len(tex[0]))
            light = 1.0 if q[0] in self.emissive else shade(q[2])
            proj = [project(v[:3]) for v in q[1]]
            for tri in ((0, 1, 2), (0, 2, 3)):
                (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = (proj[i] for i in tri)
                uvs = [q[1][i][3:] for i in tri]
                det = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
                if abs(det) < 1e-9:
                    continue
                for py in range(max(0, int(min(y0, y1, y2) + oy)), min(h, int(max(y0, y1, y2) + oy) + 2)):
                    for pxl in range(max(0, int(min(x0, x1, x2) + ox)), min(w, int(max(x0, x1, x2) + ox) + 2)):
                        sx, sy = pxl + 0.5 - ox, py + 0.5 - oy
                        b1 = ((sx - x0) * (y2 - y0) - (x2 - x0) * (sy - y0)) / det
                        b2 = ((x1 - x0) * (sy - y0) - (sx - x0) * (y1 - y0)) / det
                        b0 = 1 - b1 - b2
                        if b0 < 0 or b1 < 0 or b2 < 0:
                            continue
                        depth = b0 * z0 + b1 * z1 + b2 * z2
                        if depth >= zbuf[py][pxl] - 1e-4:
                            continue
                        u = b0 * uvs[0][0] + b1 * uvs[1][0] + b2 * uvs[2][0]
                        v = b0 * uvs[0][1] + b1 * uvs[1][1] + b2 * uvs[2][1]
                        r, g, b, a = tex[min(th - 1, max(0, int(v / 16 * th)))][min(tw - 1, max(0, int(u / 16 * tw)))]
                        base = img[py][pxl]
                        if mode == 'add':
                            img[py][pxl] = tuple(min(255, round(base[i] + c * 0.9)) for i, c in enumerate((r, g, b)))
                        elif mode == 'blend':
                            f = a / 255
                            img[py][pxl] = tuple(round(base[i] * (1 - f) + c * light * f) for i, c in enumerate((r, g, b)))
                        else:
                            if a < 128:
                                continue
                            zbuf[py][pxl] = depth
                            img[py][pxl] = (int(r * light), int(g * light), int(b * light))

        for q in visible:
            if q[3] not in self.BLEND + self.ADD:
                raster(q, 'solid')
        for group in self.BLEND:
            for q in visible:
                if q[3] == group:
                    raster(q, 'blend')
        for q in self.quads:
            if q[3] in self.ADD:
                raster(q, 'add')
        return [[(*p, 255) for p in row] for row in img]


# ================================================================ the design
d = Lantern()
textures(d)
d.emissive |= EMISSIVE
O = (8, 0, 8)
X, Z = (1, 0, 0), (0, 0, 1)
running = STATE in ('running', 'overdrive')
wet = STATE != 'loss'


def group(name):
    d.group = name


# ---- Skid: graphite, a hazard band round its foot, tread plate on top.
d.box('trim', 0, 0, 0, 16, 3, 32, sides='skid_side', top='plinth')

# ---- The jar.
# Pedestal: a gunmetal drum, a light ring, a bolted flange under the glass.
d.rev(None, O, UP, [(3, 7.6), (6, 7.6), (6, 0)], n=24, mats=['drum', 'drum'])
d.band(STRIP, O, UP, 6, 7, 7.65, n=24)
d.rev(None, O, UP, [(7, 0), (7, 7.85), (8, 7.85), (8, 7.25)], n=24, mats=['bolted', 'bolted', 'bolted'])
# Status strip on the pedestal's front.
d.box('trim', 4.5, 3.6, 0.5, 11.5, 5.6, 1.4)
d.box('trim', 5, 4.1, 0.3, 11, 5.1, 0.6, decals={'north': 'status'})
# The floor inside, under the water.
d.hplate('floor', 8, 8.01, 8, 14.5)
# Top flange, light ring and lid drum, the lid's face, and the rod drives on it.
d.rev(None, O, UP, [(25, 7.25), (25, 7.85), (26, 7.85), (26, 0)], n=24, mats=['bolted', 'bolted', 'bolted'])
d.band(STRIP, O, UP, 26, 27, 7.65, n=24)
d.rev(None, O, UP, [(27, 7.6), (28, 7.6)], n=24, mats=['lid_drum'])
d.hplate('lid_top', 8, 28, 8, 15.2)
drive_pos = [(8 + DRIVE_R * math.cos(math.radians(a)), 8 + DRIVE_R * math.sin(math.radians(a))) for a in DRIVE_ANGLES]
for x, z in drive_pos:
    d.vrod('steel_dark', x, z, 28, 29.4, 1.05, n=10)
    d.band(STRIP, (x, 0, z), UP, 28.9, 29.15, 1.1, n=10)
# Tie rods from flange to flange.
for k in range(4):
    phi = math.radians(45 + 90 * k)
    d.vrod('steel', 8 + 7.5 * math.cos(phi), 8 + 7.5 * math.sin(phi), 7, 26, 0.45, n=8)

# The sealed fuel core: a hex bundle of seven rods between two fittings, a lifting bail on top.
core_y = 13.5 if STATE == 'refuel' else 8.6
rod_tex = 'heat' if STATE == 'loss' else 'clad'
d.rev(None, (8, core_y, 8), UP, [(0, 0), (0, 3.2), (0.8, 3.2), (0.8, 0)], n=6, phase=math.pi / 6,
      mats=['steel_dark', 'steel_dark', 'steel_dark'])
for k in range(7):
    if k == 6:
        x, z = 8, 8
    else:
        phi = math.radians(60 * k)
        x, z = 8 + 2.25 * math.cos(phi), 8 + 2.25 * math.sin(phi)
    d.vrod(rod_tex, x, z, core_y + 0.8, core_y + 7.6, 0.92, n=8)
d.rev('steel', (8, core_y, 8), UP, [(4.0, 3.25), (4.5, 3.25)], n=6, phase=math.pi / 6)
d.rev(None, (8, core_y, 8), UP, [(7.6, 0), (7.6, 3.2), (8.4, 3.2), (8.4, 0)], n=6, phase=math.pi / 6,
      mats=['steel_dark', 'steel_dark', 'steel_dark'])
bail = [(8 - 1.6, core_y + 8.4, 8), (8 - 1.6, core_y + 9.5, 8), (8 + 1.6, core_y + 9.5, 8), (8 + 1.6, core_y + 8.4, 8)]
d.pipe('accent', bail, 0.3, n=6)

# Control rods: hanging from the drives. In: tips at the core's foot. Running: part out.
# Overdrive: the interlocks off, the rods come further out.
tip = {'idle': 9.4, 'refuel': 9.4, 'running': 12.5, 'overdrive': 17.0, 'loss': 17.0}[STATE]
for x, z in drive_pos:
    d.vrod('b4c', x, z, tip, 25, 0.5, n=8, caps='steel_dark')

# Water, its surface, bubbles.
level = {'loss': 12.5}.get(STATE, 23.5)
if True:
    group('water')
    d.rev('water', O, UP, [(8, 7.15), (level, 7.15)], n=24)
    # The far side's inside: looking through the jar crosses two walls of water, so what lies
    # behind the core is seen through deep water and the core stands out against it.
    group('water_far')
    d.rev('water_far', O, UP, [(level, 7.1), (8, 7.1)], n=24)
    d.rev('water_top', O, UP, [(level, 7.15), (level, 0)], n=24)
group('bubbles')
bubbles = {'idle': 0, 'refuel': 3, 'running': 7, 'overdrive': 18, 'loss': 14}[STATE]
for i in range(bubbles):
    a = i * 2.39996
    r = 1.0 + (i * 0.618 % 1) * 4.6
    y0 = (core_y + 8.5) if STATE != 'loss' else 9.5
    y = y0 + (i * 0.37 % 1) * (level - y0 - 0.6)
    d.ball('bubble', (8 + r * math.cos(a), y, 8 + r * math.sin(a)), 0.32 + 0.22 * (i % 3) / 2, n=6)
# Glass: the near wall and the far wall's inside, so the far side tints the view too.
group('glass')
d.rev('glass', O, UP, [(8, 7.25), (25, 7.25)], n=24)
group('glass_far')
d.rev('glass', O, UP, [(25, 7.2), (8, 7.2)], n=24)
group('static')
# Level scale printed on the glass, front right.
phi = math.radians(-125)
d.plate('scale', (8 + 7.3 * math.cos(phi), 15.75, 8 + 7.3 * math.sin(phi)), (math.cos(phi), 0, math.sin(phi)), 1.6, 15.5)

# Light: the Cherenkov glow round the core while running under water; heat glow when dry.
group('light')
if running:
    p = 1.0 if STATE == 'running' else 1.35
    top = core_y + 13 if STATE == 'running' else core_y + 14.5
    d.rev('ck_core', O, UP, [(core_y + 0.5, 3.6), (top - 2, 3.6)], n=12)
    d.rev('ck_mid', O, UP, [(core_y + 0.2, 5.0), (top, 5.0)], n=16)
    d.rev('ck_out', O, UP, [(8.2, 6.6), (min(top + 1.5, level - 0.2), 6.6)], n=20)
    if STATE == 'overdrive':
        d.rev('ck_mid', O, UP, [(core_y + 0.2, 4.3), (top + 1, 4.3)], n=16)
elif STATE == 'loss':
    d.rev('hk_core', O, UP, [(core_y + 0.5, 4.0), (core_y + 9, 4.0)], n=12)
group('static')

# ---- Back block, lower half: the steam generator and turbine. Ports on three faces.
d.box('cab_low', 0.5, 3, 16.5, 15.5, 16, 31.5,
      decals={'east': 'cab_low', 'west': 'cab_low', 'south': 'cab_low', 'north': 'cab_front'})
d.box('steel_dark', 14.5, 4, 20, 15.5, 12, 28)
d.box('steel_dark', 15.5, 3, 19, 16, 13, 29, decals={'east': 'port_coolant'})
d.box('steel_dark', 0.5, 4, 20, 1.5, 12, 28)
d.box('steel_dark', 0, 3, 19, 0.5, 13, 29, decals={'west': 'port_steam'})
d.box('steel_dark', 4, 4, 30.5, 12, 12, 31.5)
d.box('steel_dark', 3, 3, 31.5, 13, 13, 32, decals={'south': 'port_energy'})
# Trim between the halves, with a light strip.
d.box('trim', 0.25, 16, 16.25, 15.75, 17, 31.75)
d.box(STRIP, 0.15, 16.35, 16.15, 15.85, 16.65, 31.85)

# ---- Back block, upper half: the fuel bay. Fuel in east, spent out west, the fan on the back.
d.box('cab_up', 1, 17, 17, 15, 30, 31,
      decals={'east': 'cab_up', 'west': 'cab_up', 'south': 'cab_up', 'north': 'cab_up'}, top='roof')
d.box('steel_dark', 15, 19, 19, 16, 29, 29, decals={'east': 'port_fuel'})
d.box('steel_dark', 0, 19, 19, 1, 29, 29, decals={'west': 'port_fuel'})
d.box(STRIP, 0.9, 29.3, 16.9, 15.1, 29.6, 31.1)
# The generator's cooling fan: a shroud and a turning fan.
d.rev(None, (8, 23.5, 31), Z, [(0, 5.8), (0.7, 5.8), (0.7, 5.0), (0.3, 5.0), (0.3, 0)], n=20,
      mats=['steel_dark', 'steel_dark', 'steel_dark', 'steel_dark'])
d.plate('fan', (8, 23.5, 31.35), (0, 0, 1), 10, 10)
# Hot legs from the lid drum into the bay, cold legs from the pedestal into the plant.
for x in (3.6, 12.4):
    d.pipe('copper_pipe', [(x, 27.5, 13.4), (x, 27.5, 17.2)], 0.85, n=10)
    d.pipe('copper_pipe', [(x, 4.6, 13.4), (x, 4.6, 16.7)], 0.95, n=10)

# ---- The fuel crane on the roof: a turret, a telescopic boom over the jar, a hook.
d.box('hatch', 6, 30, 19, 10, 30.1, 23, decals={'up': 'hatch'})
d.rev(None, (8, 30, 26), UP, [(0, 2.6), (0.5, 2.6), (0.5, 0)], n=16, mats=['steel_dark', 'steel_dark'])
d.box('boom', 6.6, 30.5, 13.5, 9.4, 32, 28)
d.box('steel_dark', 6.2, 30.5, 27, 9.8, 32, 30.5)
tip_z = 7.3 if STATE == 'refuel' else 11.2
d.box('steel', 7.0, 30.8, tip_z + 1.2, 9.0, 31.8, 14)
d.box('steel_dark', 6.8, 30.3, tip_z, 9.2, 32, tip_z + 1.4)
d.rod('steel_dark', (6.5, 31.1, tip_z + 0.7), X, 3.0, 0.75, n=10)
d.box('lamp_amber', 7.6, 32, tip_z + 0.3, 8.4, 32.6, tip_z + 1.1)
hook_z = tip_z + 0.7
hook_top = core_y + 9.7 if STATE == 'refuel' else 28.8
d.box('steel_dark', 7.85, hook_top + 1.2, hook_z - 0.15, 8.15, 30.3, hook_z + 0.15)
d.box('accent', 7.2, hook_top, hook_z - 0.6, 8.8, hook_top + 1.2, hook_z + 0.6)

# Steam and smoke: a coolant loss boils the last water off and smokes.
if STATE in ('loss', 'overdrive'):
    group('steam')
    puffs = [(5, 30.5, 6), (10.5, 31.2, 9), (7.5, 32.2, 4.5)] if STATE == 'loss' else [(1.0, 9, 23.5), (-1.0, 10.5, 24.5)]
    for x, y, z in puffs:
        d.ball('smoke' if STATE == 'loss' else 'bubble', (x, y, z), 1.6, n=8)
    group('static')

os.makedirs(OUT, exist_ok=True)
if CLOSE:
    view = (-1, -1) if '--back' in sys.argv else (1, 1)
    d.save_png(os.path.join(OUT, f'close_{STATE}{"_back" if "--back" in sys.argv else ""}.png'), [(view, 22, 0.35)])
else:
    d.save_png(os.path.join(OUT, f'{STATE}.png'), [((1, 1), 11, 0.5), ((-1, -1), 11, 0.5)])
