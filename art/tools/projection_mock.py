"""A still mock of the fission station's projection screen, for approval before any Java.

The projection is a full-screen view behind the station's control screen: a live pixel-art picture
of the core and its readouts. This draws one frame of it from the station's own sums (a port of
StationReactor.analyse), in Minecraft's font read from the game jar, at the size the screen fills at
GUI scale 3 on a 1080p display (640 x 360 GUI pixels), saved at twice that for viewing.

The core is drawn top down in art pixels of two GUI pixels: fuel rods coloured by their heat
(blackbody, red to white), coolant channels with their load, graphite, control rods (violet, as the
mod draws them), and neutrons as dots whose density follows the flux round the fuel. Two frames:
a balanced core, and the same core with one fuel channel's rod missing, the station's flux tilt,
read out as a quadrant tilt (hottest quadrant over the average).

    python art/tools/projection_mock.py
writes art/concepts/fission_projection/{balanced,tilt}.png
"""
import json
import math
import os
import random
import zipfile

import pixelart
from pixelart import PAL, read_png, write_png

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, 'art', 'concepts', 'fission_projection')
W, H = 640, 360
VIEW = 2  # saved at twice the GUI size


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def k(name):
    return hexrgb(PAL[name])


# The house palette for a dark screen: graphite grounds, cyan for what is live, status colours.
BG = hexrgb('101216')
PANEL = k('K')
EDGE = k('D')
RULE = k('d')
LABEL = k('J')
TEXT = k('A')
CYAN = k('i')
CYAN_DIM = k('f')
GREEN, AMBER, ORANGE, RED = k('n'), k('Y'), k('X'), k('x')
VIOLET, VIOLET_DARK = hexrgb('9b6bff'), hexrgb('4a2f8c')
WATER, WATER_DEEP = hexrgb('16365e'), hexrgb('0c1d36')

# ---------------------------------------------------------------- the core, as StationReactor works it out
FUEL, COOL, MOD, CTRL, EMPTY = 'F', 'W', 'M', 'C', '.'
LAYOUT = ['FWFWF',
          'MFWFM',
          'FWFWF',
          'MFWFM',
          'FWFWF']
HEAT, CAPACITY = 1000, 3000
BONUS = {MOD: 0.4, FUEL: 0.2, CTRL: -0.35}


def neighbours(x, z):
    return [(x + dx, z + dz) for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)) if 0 <= x + dx < 5 and 0 <= z + dz < 5]


def analyse(types, missing=()):
    """Heat per fuel rod and load per coolant channel, as StationReactor.analyse. A fuel channel in
    {missing} is planned but holds no rod: it makes nothing and helps nothing."""
    live = lambda x, z: types[z][x] != FUEL or (x, z) not in missing
    heat = {}
    for z in range(5):
        for x in range(5):
            if types[z][x] != FUEL or not live(x, z):
                continue
            f = 1 + sum(BONUS.get(types[nz][nx], 0) for nx, nz in neighbours(x, z) if live(nx, nz))
            heat[(x, z)] = HEAT * max(0.1, f)
    load, stranded = {}, 0
    for (x, z), h in heat.items():
        cool = [(nx, nz) for nx, nz in neighbours(x, z) if types[nz][nx] == COOL]
        if not cool:
            stranded += h
        for c in cool:
            load[c] = load.get(c, 0) + h / len(cool)
    stranded += sum(max(0, l - CAPACITY) for l in load.values())
    generation = sum(heat.values())
    fraction = min(1, generation / (len(load) * CAPACITY)) if load else 0
    temperature = 150 + 450 * fraction
    output = generation * (0.25 + 0.15 * min(1, (temperature - 150) / 450)) if stranded <= 0.5 else 0
    return heat, load, generation, temperature, output, stranded


def quadrants(heat):
    """Power per quadrant (NW, NE, SW, SE); rods on the centre lines count half to each side."""
    q = [0.0] * 4
    for (x, z), h in heat.items():
        xs = [0] if x < 2 else [1] if x > 2 else [0, 1]
        zs = [0] if z < 2 else [1] if z > 2 else [0, 1]
        for qx in xs:
            for qz in zs:
                q[qz * 2 + qx] += h / (len(xs) * len(zs))
    return q


# ---------------------------------------------------------------- drawing
class Canvas:
    def __init__(self, w, h, fill):
        self.w, self.h = w, h
        self.px = [[fill] * w for _ in range(h)]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def blend(self, x, y, c, a):
        if 0 <= x < self.w and 0 <= y < self.h:
            o = self.px[y][x]
            self.px[y][x] = tuple(round(o[i] * (1 - a) + c[i] * a) for i in range(3))

    def rect(self, x0, y0, x1, y1, c):
        for y in range(max(0, y0), min(self.h, y1 + 1)):
            for x in range(max(0, x0), min(self.w, x1 + 1)):
                self.px[y][x] = c

    def frame(self, x0, y0, x1, y1, c):
        self.rect(x0, y0, x1, y0, c)
        self.rect(x0, y1, x1, y1, c)
        self.rect(x0, y0, x0, y1, c)
        self.rect(x1, y0, x1, y1, c)

    def blit(self, other, x0, y0, scale):
        for y in range(other.h):
            for x in range(other.w):
                self.rect(x0 + x * scale, y0 + y * scale, x0 + x * scale + scale - 1, y0 + y * scale + scale - 1, other.px[y][x])


def load_font():
    """Minecraft's default font glyphs from the jar: char -> (advance, inked pixel list)."""
    glyphs = {}
    with zipfile.ZipFile(pixelart.VANILLA_JAR) as jar:
        font = json.loads(jar.read('assets/minecraft/font/include/default.json'))
        for provider in font['providers']:
            if provider.get('type') != 'bitmap':
                continue
            rows = read_png(jar.read('assets/minecraft/textures/' + provider['file'].split(':')[-1]))
            lines = provider['chars']
            cw, ch = len(rows[0]) // len(lines[0]), len(rows) // len(lines)
            if ch != 8:
                continue
            ascent = provider.get('ascent', 7)
            for r, line in enumerate(lines):
                for c, char in enumerate(line):
                    if char in glyphs or char == '\u0000':
                        continue
                    pts, inked = [], 0
                    for y in range(ch):
                        for x in range(cw):
                            if rows[r * ch + y][c * cw + x][3]:
                                pts.append((x, y - ascent + 7))
                                inked = max(inked, x + 1)
                    glyphs[char] = (inked + 1, pts)
    glyphs[' '] = (4, [])
    return glyphs


FONT = load_font()


def text_width(s):
    return sum(FONT.get(ch, (6, []))[0] for ch in s)


def text(cv, x, y, s, colour, align='left', shadow=False):
    if align == 'right':
        x -= text_width(s) - 1
    elif align == 'centre':
        x -= text_width(s) // 2
    for ch in s:
        adv, pts = FONT.get(ch, (6, []))
        for px, py in pts:
            if shadow:
                cv.set(x + px + 1, y + py + 1, tuple(v // 4 for v in colour))
            cv.set(x + px, y + py, colour)
        x += adv
    return x


def heat_colour(t):
    """Blackbody, from dull red through orange to white hot, t from 0 to 1."""
    stops = [(0, hexrgb('5a1f12')), (0.35, hexrgb('b2431c')), (0.65, hexrgb('f08a24')), (0.85, hexrgb('ffd27a')), (1, hexrgb('fff6e0'))]
    for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0)
            return tuple(round(c0[i] + (c1[i] - c0[i]) * f) for i in range(3))
    return stops[-1][1]


def load_colour(f):
    return GREEN if f < 0.8 else AMBER if f <= 1 else RED


def fmt(n):
    return f'{n:,.0f}'


# ---------------------------------------------------------------- the core view (art pixels)
CELL, CORE0 = 17, 24          # a channel's cell and the core's corner, in art pixels
ART = 133                     # the view's size in art pixels (266 GUI pixels)
CENTRE = CORE0 + CELL * 5 / 2
RING = 64
MID = CELL // 2               # a cell's middle
ROD = CELL * 0.33             # a rod's radius


# Ordered dithering (4 x 4 Bayer): glows step in bands, so the picture stays pixel art.
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
LIGHT = (-0.7071, -0.7071)  # the house light, from the top left
CHERENKOV = hexrgb('3c8cff')
STEEL = k('l')


def banded(v, x, y, steps=4):
    """v (0 to 1) stepped into bands, dithered between them."""
    v = max(0.0, min(1.0, v)) * steps
    base = math.floor(v)
    return (base + (1 if v - base > (BAYER[y % 4][x % 4] + 0.5) / 16 else 0)) / steps


def mul(c, f):
    return tuple(round(min(255, max(0, c[i] * f))) for i in range(3))


def add(a, b, f=1.0):
    return tuple(round(min(255, a[i] + b[i] * f)) for i in range(3))


def facing(nx, ny):
    """How much a surface with outward normal (nx, ny) faces the light: 0 to 1."""
    return max(0.0, nx * LIGHT[0] + ny * LIGHT[1])


def disc(cv, cx, cy, radius, paint):
    """Calls paint(px, py, d, nx, ny) for every pixel of a disc round (cx, cy), returning its colour."""
    r = int(radius) + 2
    for py in range(-r, r + 1):
        for px in range(-r, r + 1):
            d = math.hypot(px + 0.5, py + 0.5)
            if d <= radius:
                c = paint(px, py, d, (px + 0.5) / max(d, 0.01), (py + 0.5) / max(d, 0.01))
                if c is not None:
                    cv.set(int(cx + px), int(cy + py), c)


def core_view(heat, load, missing, seed):
    """The core top down, lit by its own rods: the water and plate first, then the rods' warm bloom
    and the water's Cherenkov glow over them, banded and dithered."""
    hottest = max(heat.values()) if heat else 1
    centres = {(x, z): (CORE0 + x * CELL + MID + 0.5, CORE0 + z * CELL + MID + 0.5) for z in range(5) for x in range(5)}

    def flux(px, py):
        return sum(h * math.exp(-math.hypot(px - centres[c][0], py - centres[c][1]) / 10) for c, h in heat.items())
    peak = max(flux(*centres[c]) for c in centres) or 1

    def warmth(px, py):
        return sum((h / hottest) * max(0.0, 1 - math.hypot(px - centres[c][0], py - centres[c][1]) / 22) ** 2
                   for c, h in heat.items())

    cv = Canvas(ART, ART, PANEL)
    for y in range(ART):
        for x in range(ART):
            px, py = x + 0.5, y + 0.5
            dx, dy = px - CENTRE, py - CENTRE
            d = math.hypot(dx, dy)
            if d > RING + 1.5:
                continue
            if d > RING - 3:
                # The vessel wall: steel lit on the top left, the core's glow on its inner lip.
                c = mul(STEEL, 0.5 + 0.5 * facing(dx / d, dy / d))
                if d < RING - 1.5:
                    c = add(c, CHERENKOV, 0.25 * banded(flux(px, py) / peak, x, y))
                cv.set(x, y, k('T') if d > RING + 0.5 else c)
                continue
            f = flux(px, py) / peak
            # Clean bands, no dither: the pool reads calm, the light in steps.
            c = add(WATER_DEEP, CHERENKOV, 0.55 * math.floor(f ** 1.6 * 6) / 6)
            c = add(c, hexrgb('ff9a40'), 0.10 * math.floor(min(1.0, warmth(px, py)) * 3) / 3)
            cv.set(x, y, c)

    for (x, z), (cx, cy) in centres.items():
        kind = LAYOUT[z][x]
        if kind in (FUEL, COOL, CTRL):
            # Its socket in the core plate: a dark rim, the lower right lip catching the light.
            disc(cv, cx, cy, ROD + 1.6, lambda px, py, d, nx, ny:
                 (k('S') if facing(-nx, -ny) > 0.3 else k('U')) if d > ROD + 0.4 else None)
        if kind == FUEL and (x, z) in missing:
            disc(cv, cx, cy, ROD + 0.9, lambda px, py, d, nx, ny:
                 hexrgb('0a0c10') if d <= ROD else RED if (int(math.degrees(math.atan2(py, px)) + 360) // 30) % 2 == 0 else None)
            cv.rect(int(cx) - 1, int(cy) - 3, int(cx), int(cy), RED)
            cv.rect(int(cx) - 1, int(cy) + 2, int(cx), int(cy) + 3, RED)
        elif kind == FUEL:
            t = heat[(x, z)] / hottest
            core = heat_colour(0.45 + 0.55 * t)

            def rod(px, py, d, nx, ny, core=core):
                if d > ROD - 0.7:
                    return mul(core, 0.45)
                # The rod's end, lit as a dome, its rim falling into shadow, with a glint.
                c = mul(core, 0.75 + 0.4 * facing(nx, ny) - 0.25 * (d / ROD) ** 2)
                return add(c, (255, 255, 255), 0.45) if -2 <= px <= -1 and -2 <= py <= -1 else c
            disc(cv, cx, cy, ROD, rod)
            # Bloom: its heat glowing out into the water, in dithered bands.
            for py in range(-12, 13):
                for px in range(-12, 13):
                    d = math.hypot(px + 0.5, py + 0.5)
                    X, Y = int(cx + px), int(cy + py)
                    if ROD + 1.7 < d < ROD + 7 and 0 <= X < ART and 0 <= Y < ART:
                        g = (1 - (d - ROD - 1.7) / 5.3) ** 2 * (0.35 + 0.65 * t)
                        cv.px[Y][X] = add(cv.px[Y][X], heat_colour(0.7), 0.35 * banded(g, X, Y))
        elif kind == COOL:
            f = load.get((x, z), 0) / CAPACITY
            arc = load_colour(f)

            def pipe(px, py, d, nx, ny):
                if d <= ROD - 1.2:
                    # The water in the pipe, a ripple ring and a glint.
                    if -2 <= px <= -1 and py == -2:
                        return hexrgb('b8e4ff')
                    return hexrgb('3f86cf') if abs(d - 2.2) < 0.5 else hexrgb('1f5a9c')
                return mul(STEEL, 0.6 + 0.5 * facing(nx, ny))
            disc(cv, cx, cy, ROD, pipe)
            # Its load: a ring round from the top, in its colour.
            disc(cv, cx, cy, ROD + 1.6, lambda px, py, d, nx, ny:
                 arc if d > ROD + 0.6 and (math.degrees(math.atan2(px + 0.5, -(py + 0.5))) + 360) % 360 <= 360 * min(1, f) else None)
        elif kind == MOD:
            x0, y0, e = int(cx) - MID + 3, int(cy) - MID + 3, CELL - 7
            cv.rect(x0, y0, x0 + e, y0 + e, k('S'))
            cv.rect(x0, y0, x0 + e, y0, k('l'))
            cv.rect(x0, y0, x0, y0 + e, k('m'))
            cv.rect(x0 + 1, y0 + e, x0 + e, y0 + e, k('T'))
            cv.rect(x0 + e, y0 + 1, x0 + e, y0 + e, k('T'))
            cv.rect(int(cx) - 1, int(cy) - 1, int(cx), int(cy), k('U'))
        elif kind == CTRL:
            disc(cv, cx, cy, ROD, lambda px, py, d, nx, ny:
                 VIOLET_DARK if d > ROD - 0.7 or ((px == 0 or py == 0) and d < ROD - 1)
                 else mul(VIOLET, 0.7 + 0.4 * facing(nx, ny)))
    # Neutrons: a few sparks where the flux runs strongest, in the water between the rods.
    rng = random.Random(seed)
    tries = 0
    while tries < 1400:
        tries += 1
        x, y = rng.uniform(CENTRE - RING, CENTRE + RING), rng.uniform(CENTRE - RING, CENTRE + RING)
        if math.hypot(x - CENTRE, y - CENTRE) > RING - 5:
            continue
        if min(math.hypot(x - cx, y - cy) for cx, cy in centres.values()) < ROD + 2:
            continue
        f = flux(x, y) / peak
        if rng.random() < f ** 4:
            cv.set(int(x), int(y), hexrgb('e6f8ff') if f > 0.8 else hexrgb('8fd8ff'))
    return cv


# ---------------------------------------------------------------- the whole screen
def screen(name, missing, tilt_seconds=None):
    heat, load, generation, temperature, output, stranded = analyse(LAYOUT, missing)
    q = quadrants(heat)
    mean = sum(q) / 4
    qptr = max(q) / mean if mean else 1
    cv = Canvas(W, H, BG)

    # Header.
    text(cv, 10, 8, 'FISSION STATION  /  CORE PROJECTION', LABEL)
    text(cv, 10, 19, 'Uranium core, safeties on', TEXT)
    text(cv, W - 10, 8, 'Esc  back to the controls', LABEL, align='right')
    cv.rect(0, 0, 90, 1, CYAN)

    # The core view.
    cv.rect(8, 32, 400, 300, PANEL)
    cv.frame(8, 32, 400, 300, EDGE)
    vx, vy = 8 + (392 - ART * VIEW) // 2 + 1, 33
    cv.blit(core_view(heat, load, missing, 7), vx, vy, VIEW)
    text(cv, 14, 37, 'CORE, TOP DOWN', LABEL)
    text(cv, 394, 37, 'LIVE', CYAN, align='right')
    # Quadrant marks round the core, outside its ring.
    half = ART * VIEW // 2
    for label, (dx, dy) in zip(('NW', 'NE', 'SW', 'SE'), ((-1, -1), (1, -1), (-1, 1), (1, 1))):
        text(cv, vx + half + dx * (half - 14), vy + half + dy * (half - 18) - 4, label, CYAN_DIM, align='centre')

    # Readouts.
    x0, x1 = 410, 632
    cv.rect(x0, 32, x1, 300, PANEL)
    cv.frame(x0, 32, x1, 300, EDGE)
    cv.rect(x0, 32, x0 + 40, 32, CYAN)
    status, colour = ('FLUX TILT', RED) if missing else ('ONLINE', GREEN)
    text(cv, x0 + 8, 40, status, colour)
    if missing:
        text(cv, x1 - 8, 40, f'unstable in {tilt_seconds} s', RED, align='right')
    rows = [('Output', f'{fmt(output)} FE/t' if output else 'none'),
            ('Core', f'{fmt(temperature)}°C'),
            ('Coolant load', f'{fmt(100 * generation / (len(load) * CAPACITY))}%'),
            ('Fuel rods', f'{len(heat)} of {sum(r.count(FUEL) for r in LAYOUT)}')]
    y = 56
    for label, value in rows:
        text(cv, x0 + 8, y, label, LABEL)
        text(cv, x1 - 8, y, value, TEXT, align='right')
        y += 12
    cv.rect(x0 + 8, y + 2, x1 - 8, y + 2, RULE)

    # Quadrant tilt: the ratio, a bar against its limit, and the four quadrants.
    y += 10
    text(cv, x0 + 8, y, 'QUADRANT TILT', CYAN)
    tilt_colour = GREEN if qptr <= 1.02 else AMBER if qptr <= 1.09 else RED
    text(cv, x1 - 8, y, f'{qptr:.2f}', tilt_colour, align='right')
    y += 12
    text(cv, x0 + 8, y, 'Hottest quadrant over the mean', LABEL)
    y += 14
    gx = x0 + 8
    for i, p in enumerate(q):
        qx, qy = gx + (i % 2) * 52, y + (i // 2) * 26
        dev = p / mean - 1 if mean else 0
        c = GREEN if abs(dev) <= 0.02 else AMBER if abs(dev) <= 0.09 else RED
        cv.rect(qx, qy, qx + 49, qy + 23, k('U'))
        cv.frame(qx, qy, qx + 49, qy + 23, c)
        text(cv, qx + 25, qy + 4, ('NW', 'NE', 'SW', 'SE')[i], LABEL, align='centre')
        text(cv, qx + 25, qy + 13, f'{dev:+.0%}', c, align='centre')
    # A note on what the reading means, beside the grid.
    note = ['A gap in the fuel', 'skews the flux.', 'Refuel it before', 'it runs away.'] if missing else \
           ['Even flux: every', 'rod pulls its', 'weight.', '']
    for i, line in enumerate(note):
        text(cv, gx + 112, y + 2 + i * 11, line, LABEL)
    y += 60
    cv.rect(x0 + 8, y, x1 - 8, y, RULE)

    # Legend.
    y += 8
    legend = [('Fuel, by heat', heat_colour(0.8)), ('Coolant, load ring', hexrgb('1f5a9c')),
              ('Graphite', k('S')), ('Control rod', VIOLET), ('Neutrons', CYAN)]
    for i, (label, c) in enumerate(legend):
        lx, ly = x0 + 8 + (i % 2) * 110, y + (i // 2) * 12
        cv.rect(lx, ly + 1, lx + 5, ly + 6, c)
        text(cv, lx + 9, ly, label, LABEL)

    # The what-if rail: the core's temperature from cold, scrubbed along the bottom.
    ry = 306
    text(cv, 10, ry, 'WHAT-IF RUN', CYAN)
    settle = 'never settles: heats on' if missing else f'settles at {fmt(temperature)}°C in 40 s'
    text(cv, W - 10, ry, settle, RED if missing else TEXT, align='right')
    rx0, rx1, rail = 10, W - 10, ry + 30
    cv.rect(rx0, rail, rx1, rail + 1, k('T'))
    for x in range(rx0, rx1 + 1):
        s = (x - rx0) / (rx1 - rx0)
        t = (1 - math.exp(-s * 4)) if not missing else min(1.3, s * 1.3)
        cv.set(x, rail - round(t * 10), ORANGE if missing and t > 1 else CYAN)
    hx = rx0 + round((rx1 - rx0) * 0.62)
    cv.rect(hx - 1, rail - 12, hx + 1, rail + 4, TEXT)
    text(cv, 10, rail + 8, 'Drag to scrub a layout before you build it', LABEL)
    return cv


def save(cv, path):
    rows = []
    for row in cv.px:
        line = []
        for c in row:
            line += [(*c, 255)] * VIEW
        rows += [line] * VIEW
    write_png(path, rows)


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, missing in (('balanced', ()), ('tilt', ((1, 1),))):
        heat, load, generation, temperature, output, stranded = analyse(LAYOUT, missing)
        print(f'{name}: {len(heat)} rods, {fmt(generation)} heat, {fmt(temperature)}C, {fmt(output)} FE/t, '
              f'loads {[round(v) for v in load.values()]}, stranded {fmt(stranded)}')
        save(screen(name, missing, tilt_seconds=8), os.path.join(OUT, name + '.png'))
    print(OUT)


if __name__ == '__main__':
    main()
