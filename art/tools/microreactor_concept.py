"""The formed microreactor's design (design section 6): the Lantern, a pool reactor in a glass jar.

One wide, two deep and two high, facing north: x 0 to 16 west to east, y 0 to 32 up, z 0 to 32
front to back (design pixels).

Front block: a lead-glass jar on a round pedestal. The sealed fuel core stands under water on the
jar's floor, and three control rods hang beside it from drives on the lid. Running water-cooled,
the water round the core glows Cherenkov blue; dry, the jar is empty and the core glows with heat.
Real basis: pool-type research reactors (TRIGA), where the core sits in open water that cools it
and shields it, and fuel is handled from above.

Back block, lower half: the steam generator and turbine. Water in on the east (your left as you
face the front), steam out on the west (your right), energy out on the back. Upper half: the fuel
bay, fuel in on the east and spent cores out on the west, the generator's cooling fan on the back.

This script is the microreactor's one source. It publishes the textures it draws into
block/microreactor/, exports the design as quads (quad_design.py) to
assets/ryzergen/microreactor/microreactor.json for the game (MicroreactorRenderer), and renders a
preview. Groups in the export, each drawn by the renderer as the machine's state says:
    static          the body, built once into a GPU mesh
    strips          the light strips, tinted by state (cyan, amber in overdrive, red on coolant loss)
    status          the status strip on the pedestal, tinted the same
    glass, glass_inner   the jar, its outside and the far wall's inside
    core_frame      the fuel core's fittings and lifting bail, standing on the floor
    core_rods, core_rods_spent, core_rods_hot   its seven rods: fresh, spent, or glowing with heat
    rods            the control rods' absorbers, fully in (the renderer lifts them)
    fan             the generator's fan (turned about its middle)
The water, the Cherenkov glow and the bubbles are drawn by the renderer from the machine's state;
here they are drawn for the picture only.

Run from the repo root:
    python art/tools/microreactor_concept.py [OUT.png] [--state idle|running|overdrive|loss] [--close] [--back]
"""
import math
import os
import sys
import zlib

import model_preview as mp
import pixelart as px
from pixelart import Tex
from quad_design import Design, write_texture

FOLDER = 'block/microreactor/'
GAME_DATA = os.path.join(mp.ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'microreactor', 'microreactor.json')
UP, X, Z = (0, 1, 0), (1, 0, 0), (0, 0, 1)
O = (8, 0, 8)                 # the jar's axis, at the foot
JAR_R = 7.25                  # the glass's radius
FLOOR = 8                     # the jar's floor
FULL = 23.5                   # the water's full mark
CORE_Y = 8.6                  # the fuel core's foot, standing on the floor
DRIVE_ANGLES = (30, 150, 270)
DRIVE_R = 5.2
ROD_TIP = 9.4                 # the control rods' tips, fully in
ROD_LIFT = {'running': 3.1, 'overdrive': 7.6, 'loss': 7.6}
LID = 28                      # the lid's face


# ================================================================ textures
def rows(t):
    return [[t.rgba(x, y) for x in range(t.w)] for y in range(t.h)]


def finished(t, name):
    return px.finish(t, zlib.crc32(name.encode()) & 0xFFFF)


def rgb(k):
    c = px.PAL[k]
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))


def grid(fn):
    return [[fn(x, y) for x in range(16)] for y in range(16)]


def pedestal_hazard():
    """The pedestal's foot, 2 pixels: hazard stripes round the jar's base (read from the foot)."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'B')
    for y in (14, 15):
        for x in range(16):
            t.set(x, y, 'Y' if (x + y) % 4 < 2 else 'B')
    return rows(t)


def band(*keys):
    """A band for a round part, read from the foot: one palette key per row, the last at the bottom.
    Round parts change only down their height, so they read the same on any width of panel."""
    t = Tex()
    t.rect(0, 0, 15, 15, keys[0])
    for i, k in enumerate(keys):
        y = 16 - len(keys) + i
        t.rect(0, y, 15, y, k)
    return rows(t)


def bolted():
    """A one-pixel flange round the jar: graphite, a bolt head every four pixels."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'b')
    for x in range(1, 16, 4):
        t.set(x, 15, 'H')
    return rows(t)


def ringed(fill, ring, step=5):
    t = Tex()
    t.rect(0, 0, 15, 15, fill)
    for y in range(0, 16, step):
        t.rect(0, y, 15, y, ring)
    return rows(t)


def white_rods():
    """The core's rods glowing with heat: pale, tinted in the game from dull red to orange-white."""
    return grid(lambda x, y: (232, 232, 232, 255) if y % 5 else (196, 196, 196, 255))


def casing(w, h, name):
    """A light casing plate drawn at its size in the top left: a crisp two-step bevel round it,
    and a screw in each corner just inside the bevel. On a port face the flange covers the screws
    and leaves the bevel as an even frame round the port."""
    t = Tex()
    t.rect(0, 0, w - 1, h - 1, 'F')
    t.rect(0, 0, w - 1, 0, 'A')
    t.rect(0, 0, 0, h - 1, 'A')
    t.rect(1, 1, w - 2, 1, 'E')
    t.rect(1, 1, 1, h - 2, 'E')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    t.rect(1, h - 2, w - 2, h - 2, 'I')
    t.rect(w - 2, 1, w - 2, h - 2, 'I')
    for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
        t.set(x, y, 'J')
    return finished(t, FOLDER + name)


def roof():
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(1, 1, 14, 1, 'b')
    t.rect(1, 1, 1, 14, 'b')
    t.rect(1, 14, 14, 14, 'T')
    t.rect(14, 1, 14, 14, 'T')
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.set(x, y, 'U')
    return finished(t, FOLDER + 'roof')


def round_tex(fn):
    """A round picture for a disc, drawn on a square plate; outside the circle is clear."""
    def at(x, y):
        dx, dy = x + 0.5 - 8, y + 0.5 - 8
        d = math.hypot(dx, dy)
        if d > 7.95:
            return (0, 0, 0, 0)
        return fn(dx, dy, d, math.degrees(math.atan2(dy, dx)) % 360)
    return grid(at)


def status():
    """The status strip, 6 x 1 in the top left: six pale segments, tinted by the machine's state."""
    def fn(x, y):
        if y == 0 and x < 6:
            return (240, 240, 240, 255)
        return (*rgb('U'), 255)
    return grid(fn)


def fan():
    """The generator's fan face on: five swept blades round a cyan hub, in a graphite ring."""
    def fn(dx, dy, d, a):
        if d > 7.1:
            return (58, 63, 70, 255)
        if d < 2.2:
            hub = rgb('i')
            return (*[min(255, round(v * (1.15 - 0.2 * d / 2.2))) for v in hub], 255)
        f = ((math.radians(a) + d * 0.18) * 5 / (2 * math.pi)) % 1
        if f < 0.55:
            c = [round(178 + (120 - 178) * f / 0.55), round(186 + (128 - 186) * f / 0.55), round(196 + (140 - 196) * f / 0.55)]
            return (*c, 255)
        return (24, 26, 30, 255)
    return round_tex(fn)


def light_strip():
    """A light strip, pale so the game can tint it: cyan running, amber in overdrive, red on alarm."""
    return grid(lambda x, y: (236, 242, 246, 255) if y % 2 else (255, 255, 255, 255))


def cherenkov_glow():
    """The glow round the core: white, fading up its height (read from the foot), tinted blue in
    the game and drawn see-through and lit, so it shows as light in the water."""
    return grid(lambda x, y: (255, 255, 255, round(255 * max(0.0, (y - 1) / 14) ** 1.4)))


def bubble():
    """A bubble: pale blue-white and mostly see-through, lit like the water round it."""
    return grid(lambda x, y: (205, 232, 255, 105))


WATER_FRAMES = 16


def water():
    """The jar's water, an animated strip of frames: deep blue, see-through, with soft streaks of
    lighter water drifting through it as light does under a moving surface. The colour is in the
    texture, so the game draws it untinted."""
    out = []
    for k in range(WATER_FRAMES):
        phase = 2 * math.pi * k / WATER_FRAMES
        for y in range(16):
            row = []
            for x in range(16):
                a = math.sin(x * 0.78 + y * 0.39 + phase) + math.sin(x * 0.39 - y * 0.78 + 2 * phase)
                light = max(0.0, a - 0.9) / 1.1
                base = (22, 72, 150)
                hi = (52, 122, 205)
                row.append((*[round(b + (h - b) * light) for b, h in zip(base, hi)], 205))
            out.append(row)
    return out


PUBLISHED = {
    'pedestal_hazard': pedestal_hazard,
    'pedestal': lambda: band('u', 's', 'u', 'z'),
    'lid_band': lambda: band('u', 's'),
    'lid_rim': lambda: band('z', 'u', 's', 'u'),
    'lid_plate': lambda: band('s', 's'),
    'bolted': bolted,
    'trim': lambda: band('T', 'T'),
    'clad': lambda: ringed('H', 'h'),
    'clad_spent': lambda: ringed('l', 'm'),
    'clad_hot': white_rods,
    'absorber': lambda: ringed('S', 'b', 4),
    'cab': lambda: casing(14, 14, 'cab'),
    'roof': roof,
    'floor_rim': lambda: band('z', 'z'),
    'floor_plate': lambda: band('u', 'u'),
    'floor_groove': lambda: band('U', 'U'),
    'pipe': lambda: band('R', 'R'),
    'status': status,
    'fan': fan,
    'light_strip': light_strip,
    'cherenkov_glow': cherenkov_glow,
    'bubble': bubble,
    'water': water,
}

# Textures the mod already has.
EXISTING = {
    **{n: 'ryzergen:block/microreactor/' + n for n in (
        'steel', 'steel_dark', 'accent', 'port_energy', 'port_coolant', 'port_steam', 'port_fuel')},
    'plinth': 'ryzergen:block/breeder/plinth',
    'glass': 'ryzergen:block/machine/smooth_glass',
}
EMISSIVE = {'light_strip', 'status', 'clad_hot'}


def publish_textures():
    for name, fn in PUBLISHED.items():
        write_texture(FOLDER + name, fn())
    # The water is animated: its frames run down the strip.
    for base in (os.path.join(mp.ROOT, 'art', 'textures'), mp.TEXTURES):
        with open(os.path.join(base, FOLDER + 'water.png.mcmeta'), 'w') as f:
            f.write('{\n  "animation": {\n    "frametime": 3,\n    "interpolate": true\n  }\n}\n')


def textures():
    out = dict(EXISTING)
    out.update({name: 'ryzergen:' + FOLDER + name for name in PUBLISHED})
    return out


# ================================================================ the picture's own textures and colours
STATE_COLOUR = {'idle': (27, 90, 115), 'running': (53, 200, 245), 'overdrive': (240, 160, 48),
                'loss': (225, 60, 50)}


def preview_textures(design, state):
    """Water, glow and bubbles for the picture, and the tinted parts in this state's colours (the
    game tints them as it draws)."""
    colour = STATE_COLOUR[state]

    def tint(name, col):
        base = mp.texture('ryzergen:' + FOLDER + name)
        ref = f'preview:{name}'
        mp._tex_cache[ref] = [[(*(round(c * v / 255) for c, v in zip(col, p[:3])), p[3]) for p in row] for row in base]
        design.textures[name] = ref

    tint('light_strip', colour)
    tint('status', colour)
    tint('clad_hot', (255, 140, 60))
    extra = {
        'water': grid(lambda x, y: (*[round(v * (1 - 0.18 * y / 15)) for v in ((24, 82, 166) if (x * 7 + y * 3) % 17 else (38, 104, 190))], 140)),
        'water_top': grid(lambda x, y: (*((120, 190, 245) if (x + 2 * y) % 9 == 0 or (2 * x - y) % 11 == 0 else (52, 128, 210)), 150)),
        'water_far': grid(lambda x, y: (14, 52, 118, 200)),
        'smoke': grid(lambda x, y: (150, 150, 150, 90 if (x + y) % 3 else 110)),
    }
    for strength, name in ((0.5, 'ck_core'), (0.36, 'ck_mid'), (0.24, 'ck_out')):
        base = mp.texture('ryzergen:' + FOLDER + 'cherenkov_glow')
        extra[name] = [[(round(70 * p[3] / 255 * strength), round(175 * p[3] / 255 * strength), round(255 * p[3] / 255 * strength), 255)
                        for p in row] for row in base]
    extra['hk_core'] = [[(round(255 * p[3] / 255 * 0.6), round(120 * p[3] / 255 * 0.6), round(40 * p[3] / 255 * 0.6), 255)
                         for p in row] for row in mp.texture('ryzergen:' + FOLDER + 'cherenkov_glow')]
    for name, tex in extra.items():
        mp._tex_cache['preview:' + name] = tex
        design.textures[name] = 'preview:' + name
    design.textures['bubble_p'] = 'ryzergen:' + FOLDER + 'bubble'
    design.emissive |= {'ck_core', 'ck_mid', 'ck_out', 'hk_core'}


class Lantern(Design):
    """The picture: the solid parts, then the see-through layers back to front (the far wall's
    glass, the far water, the water, bubbles, the near glass), then the glow added as light."""
    BLEND = ('glass_inner', 'water_far', 'water', 'bubbles', 'glass', 'steam')
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
                        elif a >= 128:
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
def design(state='export'):
    """The machine. Exported at rest (state 'export'); a picture places the moving parts for its state."""
    d = Lantern(8, textures(), EMISSIVE, concept_only={'water', 'water_far', 'bubbles', 'light', 'steam'})
    picture = state != 'export'

    def group(name):
        d.group = name

    drives = [(8 + DRIVE_R * math.cos(math.radians(a)), 8 + DRIVE_R * math.sin(math.radians(a))) for a in DRIVE_ANGLES]

    # ---- Base plate: graphite, a pixel thick, tread plate on top.
    group('static')
    d.box('trim', 0, 0, 0, 16, 1, 32, top='plinth')

    # ---- The jar.
    # Pedestal: a gunmetal drum with hazard stripes round its foot, a light ring, a bolted flange
    # under the glass, the floor inside.
    d.revolve(None, O, UP, [(1, 7.6), (3, 7.6), (6, 7.6), (6, 0)], n=24, mats=['pedestal_hazard', 'pedestal', 'pedestal'])
    d.revolve(None, O, UP, [(7, 0), (7, 7.85), (8, 7.85), (8, JAR_R)], n=24, mats=['bolted', 'bolted', 'bolted'])
    # The floor inside, under the water: a plate in rings (a dark rim, a groove round the core's seat),
    # round like the jar, so its edge meets the glass all the way round.
    d.revolve(None, (8, FLOOR + 0.01, 8), UP, [(0, JAR_R), (0, 6.4), (0, 4.4), (0, 3.9), (0, 0)], n=24,
              mats=['floor_rim', 'floor_plate', 'floor_groove', 'floor_plate'])
    # Top flange, the lid's drum, and the rod drives standing on the lid.
    d.revolve(None, O, UP, [(25, JAR_R), (25, 7.85), (26, 7.85), (26, 0)], n=24, mats=['bolted', 'bolted', 'bolted'])
    # The lid: its drum, and its face as part of the same round shape (a rim ring, then the plate),
    # so the face's edge is the drum's edge all the way round.
    d.revolve(None, O, UP, [(27, 7.6), (LID, 7.6), (LID, 6.9), (LID, 0)], n=24, mats=['lid_band', 'lid_rim', 'lid_plate'])
    # Eight bolts round the rim, a plug in the middle with a lifting lug, and a collar for each drive.
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        d.rod('steel', (8 + 7.25 * math.cos(a), LID, 8 + 7.25 * math.sin(a)), UP, 0.3, 0.32, n=6)
    d.revolve(None, (8, LID, 8), UP, [(0, 2.8), (0.4, 2.8), (0.4, 0)], n=16, mats=['steel_dark', 'lid_band'])
    d.box('accent', 7.2, LID + 0.4, 7.7, 8.8, LID + 0.8, 8.3)
    for x, z in drives:
        d.revolve(None, (x, LID, z), UP, [(0, 1.5), (0.2, 1.5), (0.2, 0)], n=12, mats=['steel_dark', 'steel_dark'])
        d.rod('steel_dark', (x, LID + 0.2, z), UP, 1.2, 1.05, n=10)
        # The drive shaft the absorber slides on, from the lid down into the water.
        d.rod('steel', (x, 10, z), UP, 17, 0.22, n=6)
    # Tie rods from flange to flange.
    for k in range(4):
        phi = math.radians(45 + 90 * k)
        d.rod('steel', (8 + 7.5 * math.cos(phi), 7, 8 + 7.5 * math.sin(phi)), UP, 19, 0.45, n=8)
    # The status strip's bezel on the pedestal's front.
    d.box('trim', 4.5, 3.6, 0.5, 11.5, 5.6, 1.4)

    # Light strips: round the pedestal and the top flange, ringing each drive.
    group('strips')
    d.revolve('light_strip', O, UP, [(6, 7.65), (7, 7.65)], n=24)
    d.revolve('light_strip', O, UP, [(26, 7.65), (27, 7.65)], n=24)
    for x, z in drives:
        d.revolve('light_strip', (x, 0, z), UP, [(LID + 0.9, 1.1), (LID + 1.15, 1.1)], n=10)
    group('status')
    d.box('status', 5, 4.1, 0.3, 11, 5.1, 0.6, decals={'north': 'status'}, skip=('south',))

    # The sealed fuel core: a hex bundle of seven rods between two fittings, a lifting bail on top.
    cy = CORE_Y
    group('core_frame')
    d.revolve(None, (8, cy, 8), UP, [(0, 0), (0, 3.2), (0.8, 3.2), (0.8, 0)], n=6, phase=math.pi / 6,
              mats=['steel_dark', 'steel_dark', 'steel_dark'])
    d.revolve('steel', (8, cy, 8), UP, [(4.0, 3.25), (4.5, 3.25)], n=6, phase=math.pi / 6)
    d.revolve(None, (8, cy, 8), UP, [(7.6, 0), (7.6, 3.2), (8.4, 3.2), (8.4, 0)], n=6, phase=math.pi / 6,
              mats=['steel_dark', 'steel_dark', 'steel_dark'])
    d.pipe('accent', [(6.4, cy + 8.4, 8), (6.4, cy + 9.5, 8), (9.6, cy + 9.5, 8), (9.6, cy + 8.4, 8)], 0.3, n=6)
    for name in ('core_rods', 'core_rods_spent', 'core_rods_hot'):
        if picture and name != ('core_rods_hot' if state == 'loss' else 'core_rods'):
            continue
        group(name)
        tex = {'core_rods': 'clad', 'core_rods_spent': 'clad_spent', 'core_rods_hot': 'clad_hot'}[name]
        for k in range(7):
            x, z = (8, 8) if k == 6 else (8 + 2.25 * math.cos(math.radians(60 * k)), 8 + 2.25 * math.sin(math.radians(60 * k)))
            d.rod(tex, (x, cy + 0.8, z), UP, 6.8, 0.92, n=8)

    # Control rods' absorbers, fully in; lifted for the picture's state.
    group('rods')
    lift = ROD_LIFT.get(state, 0) if picture else 0
    for x, z in drives:
        d.rod('absorber', (x, ROD_TIP + lift, z), UP, 8, 0.55, n=8, caps='steel_dark')

    # The glass: the outside, and the far wall's inside, so the far side tints the view too.
    group('glass')
    d.revolve('glass', O, UP, [(FLOOR, JAR_R), (25, JAR_R)], n=24)
    group('glass_inner')
    d.revolve('glass', O, UP, [(25, JAR_R - 0.05), (FLOOR, JAR_R - 0.05)], n=24)

    # ---- The back block, the plant, laid out evenly about its middle so every port sits centred
    # in its panel: the base plate (1), the lower cabinet (14, centred on its ports), a waist band
    # (2, its light strip on the line between the blocks), the upper cabinet (14) and a cap (1).
    # Both cabinets are 14 x 14 on every face, so each 10 x 10 port flange, centred on its block face
    # and flush with it, has an even 2-pixel frame of casing round it.
    group('static')
    cab = {face: 'cab' for face in ('north', 'south', 'east', 'west')}
    # Lower: the steam generator and turbine. Water in on the east (your left), steam out on the
    # west (your right), energy out on the back.
    d.box('cab', 1, 1, 17, 15, 15, 31, decals=cab, skip=('up', 'down'))
    d.box('steel_dark', 15, 3, 19, 16, 13, 29, decals={'east': 'port_coolant'}, skip=('west',))
    d.box('steel_dark', 0, 3, 19, 1, 13, 29, decals={'west': 'port_steam'}, skip=('east',))
    d.box('steel_dark', 3, 3, 31, 13, 13, 32, decals={'south': 'port_energy'}, skip=('north',))
    # The waist between the halves, and the cap.
    d.box('trim', 0.5, 15, 16.5, 15.5, 17, 31.5)
    d.box('trim', 0.5, 31, 16.5, 15.5, 32, 31.5, top='roof')
    # Upper: the fuel bay. Fuel in east, spent cores out west, the generator's fan on the back.
    d.box('cab', 1, 17, 17, 15, 31, 31, decals=cab, skip=('up', 'down'))
    d.box('steel_dark', 15, 19, 19, 16, 29, 29, decals={'east': 'port_fuel'}, skip=('west',))
    d.box('steel_dark', 0, 19, 19, 1, 29, 29, decals={'west': 'port_fuel'}, skip=('east',))
    # The fan's shroud, the size of a port flange and centred like one; the fan itself turns.
    d.revolve(None, (8, 24, 31), Z, [(0, 5.0), (0.7, 5.0), (0.7, 4.3), (0.3, 4.3), (0.3, 0)], n=20,
              mats=['steel_dark', 'steel_dark', 'steel_dark', 'steel_dark'])
    # Hot legs from the lid's drum into the bay, cold legs from the pedestal into the plant. Each
    # stays within the height of the drum it leaves, with a collar where it leaves and a flange
    # where it meets the cabinet.
    for x in (3.6, 12.4):
        surface = 8 + math.sqrt(7.6 ** 2 - (x - 8) ** 2)
        for y, r, cabinet in ((27.5, 0.42, 17), (4.5, 0.55, 17)):
            d.rod('pipe', (x, y, surface - 0.5), Z, cabinet - surface + 0.7, r, n=10)
            d.rod('steel_dark', (x, y, surface - 0.1), Z, 0.4, r + 0.08 if y > 20 else r + 0.2, n=10)
            f = r + 0.35
            d.box('steel_dark', x - f, y - f, cabinet - 0.3, x + f, y + f, cabinet, skip=('south',))
    group('strips')
    d.box('light_strip', 0.4, 15.75, 16.4, 15.6, 16.25, 31.6)
    d.box('light_strip', 0.4, 31.35, 16.4, 15.6, 31.65, 31.6)

    group('fan')
    d.plate('fan', (8, 24, 31.35), Z, 8.6, 8.6)

    if picture:
        picture_layers(d, state, cy)
    return d


def picture_layers(d, state, cy):
    """Water, bubbles, glow and smoke for the picture; the game draws these from the machine's state."""
    level = 12.5 if state == 'loss' else FULL

    def group(name):
        d.group = name

    group('water_far')
    d.revolve('water_far', O, UP, [(level, 7.1), (FLOOR, 7.1)], n=24)
    group('water')
    d.revolve('water', O, UP, [(FLOOR, 7.15), (level, 7.15)], n=24)
    d.revolve('water_top', O, UP, [(level, 7.15), (level, 0)], n=24)
    group('bubbles')
    count = {'idle': 0, 'running': 7, 'overdrive': 18, 'loss': 14}[state]
    for i in range(count):
        a = i * 2.39996
        r = 1.0 + (i * 0.618 % 1) * 4.6
        y0 = cy + 8.5 if state != 'loss' else 9.5
        y = y0 + (i * 0.37 % 1) * (level - y0 - 0.6)
        d.ball('bubble_p', (8 + r * math.cos(a), y, 8 + r * math.sin(a)), 0.32 + 0.11 * (i % 3), n=6)
    group('light')
    if state in ('running', 'overdrive'):
        top = cy + 13 if state == 'running' else cy + 14.5
        d.revolve('ck_core', O, UP, [(cy + 0.5, 3.6), (top - 2, 3.6)], n=12)
        d.revolve('ck_mid', O, UP, [(cy + 0.2, 5.0), (top, 5.0)], n=16)
        d.revolve('ck_out', O, UP, [(FLOOR + 0.2, 6.6), (min(top + 1.5, level - 0.2), 6.6)], n=20)
        if state == 'overdrive':
            d.revolve('ck_mid', O, UP, [(cy + 0.2, 4.3), (top + 1, 4.3)], n=16)
    elif state == 'loss':
        d.revolve('hk_core', O, UP, [(cy + 0.5, 4.0), (cy + 9, 4.0)], n=12)
    if state == 'loss':
        group('steam')
        for x, y, z in ((5, 30.5, 6), (10.5, 31.2, 9), (7.5, 32.2, 4.5)):
            d.ball('smoke', (x, y, z), 1.6, n=8)
    group('static')


def picture(out, state, close=False, back=False):
    d = design(state)
    preview_textures(d, state)
    view = (-1, -1) if back else (1, 1)
    views = [(view, 22, 0.35)] if close else [((1, 1), 11, 0.5), ((-1, -1), 11, 0.5)]
    d.save_png(out, views)


def main():
    args = sys.argv[1:]
    state = 'running'
    if '--state' in args:
        i = args.index('--state')
        state = args[i + 1]
        del args[i:i + 2]
    close, back = '--close' in args, '--back' in args
    args = [a for a in args if not a.startswith('--')]
    publish_textures()
    design().export(GAME_DATA)
    out = args[0] if args else os.path.join(mp.ROOT, 'art', 'concepts', 'microreactor.png')
    picture(out, state, close, back)


if __name__ == '__main__':
    main()
