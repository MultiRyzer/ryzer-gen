"""The core puzzle's cells, redrawn: a still of the solved core and a close-up of each kind of cell.

Each cell is pixel art at one GUI pixel per pixel, 32 pixels a cell, lit from the top left:
  water     depth shading, caustic lines, a soft highlight and bubbles; Cherenkov blue where it
            touches a rod, so a rod's watered side shows in the water too
  graphite  a machined block, chamfered, in brick courses round a central bore; its corners glow
            amber towards the rods it warms
  fuel      a silver cladding ring round a glowing pellet face, red to white with its heat, the heat
            in the middle; cyan bars on its watered sides, red on a missing one
  empty     a dark socket in the core plate
In game the caustics, bubbles and glows move; the still shows one moment (phase t).

    python art/tools/core_cells_mock.py
writes art/concepts/station_screen/core_cells.png
"""
import math
import os

import terminal_font as tf
from projection_mock import Canvas, hexrgb, heat_colour, write_png, ROOT

OUT = os.path.join(ROOT, 'art', 'concepts', 'station_screen')
S = 32                     # a cell, with its 1-pixel seam
LX, LY = -0.7071, -0.7071  # the house light


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def add(a, b, f):
    return tuple(min(255, round(a[i] + b[i] * f)) for i in range(3))


def mul(a, f):
    return tuple(max(0, min(255, round(v * f))) for v in a)


def bands(v, n):
    return math.floor(max(0.0, min(0.999, v)) * n) / (n - 1) if n > 1 else v


DEEP, MID, SHALLOW = hexrgb('0a2448'), hexrgb('134a86'), hexrgb('2a74c0')
CAUSTIC, GLINT = hexrgb('6cbcf4'), hexrgb('d4f0ff')
CHERENKOV = hexrgb('3c8cff')
RIM_DARK, RIM, RIM_LIT = hexrgb('1b1e22'), hexrgb('2b3036'), hexrgb('646c77')
GRAPHITE, GRAPHITE_LINE, GRAPHITE_LIT = hexrgb('30343a'), hexrgb('23272b'), hexrgb('5a6068')
BORE = hexrgb('101215')
CLAD_LIT, CLAD, CLAD_DARK = hexrgb('c2c9d2'), hexrgb('8b939e'), hexrgb('454b54')
AMBER = hexrgb('ffb84d')
CYAN, RED = hexrgb('35c8f5'), hexrgb('e0503c')
SOCKET = hexrgb('14171b')


def socket(cv, x0, y0):
    """The channel's socket in the core plate: a dark rim, lit on its lower right lip."""
    cv.rect(x0, y0, x0 + S - 2, y0 + S - 2, RIM_DARK)
    cv.rect(x0 + 1, y0 + S - 3, x0 + S - 2, y0 + S - 3, RIM)
    cv.rect(x0 + S - 3, y0 + 1, x0 + S - 3, y0 + S - 3, RIM)


def water(cv, x0, y0, touches, t=0.0):
    """Water in a channel. touches: {side: rod heat 0..1} for the rods beside it ('n', 's', 'w', 'e')."""
    socket(cv, x0, y0)
    n = S - 4
    for j in range(n):
        for i in range(n):
            u, v = (i + 0.5) / n, (j + 0.5) / n
            # Deeper towards the rim; the rim shades the top and left inside edges.
            edge = min(u, v, 1 - u, 1 - v)
            c = mix(DEEP, MID, edge * 3.2)
            c = mix(c, SHALLOW, max(0.0, edge * 3 - 0.75))
            if u < 0.08 or v < 0.08:
                c = mul(c, 0.72)
            # Caustics: where three waves cancel, light gathers in thin lines.
            a = math.sin(i * 0.55 + t * 1.3) + math.sin(j * 0.48 - t) + math.sin((i + j) * 0.31 + t * 0.7)
            line = abs(a)
            if line < 0.32:
                c = add(c, CAUSTIC, 0.45 if line < 0.14 else 0.22)
            # Cherenkov blue from the rods beside it, strongest at their side.
            for side, heat in touches.items():
                d = {'n': v, 's': 1 - v, 'w': u, 'e': 1 - u}[side]
                g = max(0.0, 1 - d / 0.45) ** 2 * (0.35 + 0.65 * heat)
                c = add(c, CHERENKOV, 0.55 * bands(g, 4))
            # A soft highlight across the top left.
            if abs(u + v - 0.55) < 0.06 and u < 0.45:
                c = add(c, GLINT, 0.18)
            cv.set(x0 + 2 + i, y0 + 2 + j, c)
    for bx, by in ((0.3, 0.62), (0.68, 0.3), (0.55, 0.78)):
        cv.set(x0 + 2 + int(bx * n), y0 + 2 + int((by - (t * 0.07) % 0.2) * n), GLINT)


def graphite(cv, x0, y0, warms, t=0.0):
    """A graphite block. warms: the corners ('nw', 'ne', 'sw', 'se') with a rod it warms."""
    socket(cv, x0, y0)
    a, b = x0 + 2, y0 + 2
    n = S - 4
    cv.rect(a, b, a + n - 1, b + n - 1, GRAPHITE)
    # Brick courses, joints offset course to course.
    for k, y in enumerate(range(b + 6, b + n - 1, 7)):
        cv.rect(a + 1, y, a + n - 2, y, GRAPHITE_LINE)
        jx = a + (5 if k % 2 else 12)
        for x in range(jx, a + n - 1, 14):
            cv.rect(x, y - 6, x, y - 1, GRAPHITE_LINE)
    # Chamfered edges: lit top and left, dark bottom and right.
    cv.rect(a, b, a + n - 1, b, GRAPHITE_LIT)
    cv.rect(a, b, a, b + n - 1, hexrgb('474d55'))
    cv.rect(a + 1, b + n - 1, a + n - 1, b + n - 1, RIM_DARK)
    cv.rect(a + n - 1, b + 1, a + n - 1, b + n - 1, RIM_DARK)
    # The bore through its middle: dark, its lower right rim catching the light.
    cx, cy = a + n / 2, b + n / 2
    for j in range(-6, 7):
        for i in range(-6, 7):
            d = math.hypot(i + 0.5, j + 0.5)
            if d <= 4.6:
                cv.set(int(cx + i), int(cy + j), BORE)
            elif d <= 5.6:
                lit = (i + j) > 0
                cv.set(int(cx + i), int(cy + j), GRAPHITE_LIT if lit else RIM_DARK)
    # Amber at the corners it warms, breathing a little.
    pulse = 0.85 + 0.15 * math.sin(t * 3)
    for corner in warms:
        ox = a if corner[1] == 'w' else a + n - 1
        oy = b if corner[0] == 'n' else b + n - 1
        for j in range(n):
            for i in range(n):
                d = math.hypot(a + i - ox, b + j - oy)
                if d < 9:
                    g = (1 - d / 9) ** 1.5 * pulse
                    cv.blend(a + i, b + j, AMBER, 0.55 * bands(g, 3))


def fuel(cv, x0, y0, heat, sides, corners, cooled=True, t=0.0):
    """A fuel rod. heat 0..1; sides: {side: True watered, False missing}; corners: graphite corners."""
    socket(cv, x0, y0)
    a, b = x0 + 2, y0 + 2
    n = S - 4
    cv.rect(a, b, a + n - 1, b + n - 1, SOCKET)
    # The core plate's grid on the socket floor.
    for k in range(0, n, 4):
        for m in range(0, n, 4):
            cv.set(a + k, b + m, hexrgb('1a1e23'))
    cx, cy = a + n / 2, b + n / 2
    face = heat_colour(0.45 + 0.55 * heat) if cooled else hexrgb('4a1a10')
    breath = 1 + 0.04 * math.sin(t * 2.2)
    # Bloom: the rod's heat glowing out into its socket.
    if cooled:
        for j in range(n):
            for i in range(n):
                d = math.hypot(a + i + 0.5 - cx, b + j + 0.5 - cy)
                if 11.5 < d < 16:
                    g = (1 - (d - 11.5) / 4.5) ** 2 * (0.3 + 0.7 * heat)
                    cv.blend(a + i, b + j, heat_colour(0.7), 0.5 * bands(g, 3))
    for j in range(-13, 14):
        for i in range(-13, 14):
            d = math.hypot(i + 0.5, j + 0.5)
            nx, ny = (i + 0.5) / max(d, 0.1), (j + 0.5) / max(d, 0.1)
            facing = max(0.0, nx * LX + ny * LY)
            X, Y = int(cx + i), int(cy + j)
            if d <= 8.6:
                # The pellet face: hottest in the middle, in steps.
                f = 1 - (d / 8.6) ** 2 * 0.45
                c = mul(face, (0.62 + 0.45 * bands(f, 5)) * breath)
                cv.set(X, Y, c)
            elif d <= 11.4:
                # The cladding ring: brushed silver, lit on the top left.
                c = mix(CLAD_DARK, CLAD_LIT, 0.25 + 0.75 * facing)
                if d > 10.6:
                    c = mul(c, 0.6)
                cv.set(X, Y, c)
            elif d <= 12.2:
                cv.set(X, Y, RIM_DARK)
    # The heat, in the middle.
    tf.draw(cv, int(cx) + 1, int(cy) - 3, str(2 + len(corners)) if cooled else '0', hexrgb('1a0f08') if cooled else RED,
            align='centre')
    # Its sides: cyan where watered, red where water is missing.
    for side, ok in sides.items():
        c = CYAN if ok else RED
        if side == 'n':
            cv.rect(a + 9, b, a + n - 10, b + 1, c)
        elif side == 's':
            cv.rect(a + 9, b + n - 2, a + n - 10, b + n - 1, c)
        elif side == 'w':
            cv.rect(a, b + 9, a + 1, b + n - 10, c)
        else:
            cv.rect(a + n - 2, b + 9, a + n - 1, b + n - 10, c)
    # Graphite on its corners: an amber bracket in each.
    for corner in corners:
        ox = a if corner[1] == 'w' else a + n - 1
        oy = b if corner[0] == 'n' else b + n - 1
        sx = 1 if corner[1] == 'w' else -1
        sy = 1 if corner[0] == 'n' else -1
        for k in range(5):
            cv.set(ox + k * sx, oy, AMBER)
            cv.set(ox, oy + k * sy, AMBER)


def empty(cv, x0, y0):
    socket(cv, x0, y0)
    a, b = x0 + 2, y0 + 2
    n = S - 4
    cv.rect(a, b, a + n - 1, b + n - 1, SOCKET)
    for k in range(0, n, 4):
        for m in range(0, n, 4):
            cv.set(a + k, b + m, hexrgb('1a1e23'))
    c = n // 2
    cv.rect(a + c - 3, b + c, a + c + 2, b + c, hexrgb('23282e'))
    cv.rect(a + c, b + c - 3, a + c, b + c + 2, hexrgb('23282e'))


LATTICE = ['FWFWF', 'WMWMW', 'FWFWF', 'WMWMW', 'FWFWF']


def at(plan, r, c):
    return plan[r][c] if 0 <= r < 5 and 0 <= c < 5 else None


def draw_core(cv, x0, y0, plan, t=0.0):
    cv.rect(x0 - 3, y0 - 3, x0 + 5 * S + 1, y0 + 5 * S + 1, hexrgb('0b0d10'))
    cv.frame(x0 - 3, y0 - 3, x0 + 5 * S + 1, y0 + 5 * S + 1, hexrgb('2a2e35'))
    heats = {}
    for r in range(5):
        for c in range(5):
            if plan[r][c] == 'F':
                corners = [k for k, (dr, dc) in (('nw', (-1, -1)), ('ne', (-1, 1)), ('sw', (1, -1)), ('se', (1, 1)))
                           if at(plan, r + dr, c + dc) == 'M']
                heats[(r, c)] = (2 + len(corners)) / 6
    for r in range(5):
        for c in range(5):
            x, y = x0 + c * S, y0 + r * S
            t_ = t + (r * 5 + c) * 0.37
            kind = plan[r][c]
            if kind == 'W':
                touches = {s: heats[(r + dr, c + dc)] for s, (dr, dc) in (('n', (-1, 0)), ('s', (1, 0)), ('w', (0, -1)), ('e', (0, 1)))
                           if (r + dr, c + dc) in heats}
                water(cv, x, y, touches, t_)
            elif kind == 'M':
                warms = [k for k, (dr, dc) in (('nw', (-1, -1)), ('ne', (-1, 1)), ('sw', (1, -1)), ('se', (1, 1)))
                         if at(plan, r + dr, c + dc) == 'F']
                graphite(cv, x, y, warms, t_)
            elif kind == 'F':
                sides = {s: at(plan, r + dr, c + dc) == 'W' for s, (dr, dc) in (('n', (-1, 0)), ('s', (1, 0)), ('w', (0, -1)), ('e', (0, 1)))
                         if at(plan, r + dr, c + dc) is not None}
                corners = [k for k, (dr, dc) in (('nw', (-1, -1)), ('ne', (-1, 1)), ('sw', (1, -1)), ('se', (1, 1)))
                           if at(plan, r + dr, c + dc) == 'M']
                fuel(cv, x, y, heats[(r, c)], sides, corners, all(sides.values()), t_)
            else:
                empty(cv, x, y)


def main():
    os.makedirs(OUT, exist_ok=True)
    zoom = 4
    # The solved core, and beside it a close-up row: water by a hot rod, graphite, rods at 2 and 6
    # heat, a rod missing water, an empty channel.
    core = Canvas(5 * S + 8, 5 * S + 8, hexrgb('0b0d11'))
    draw_core(core, 4, 4, LATTICE, t=0.4)
    strip = Canvas(6 * (S + 26) + 6, S + 30, hexrgb('0b0d11'))
    water(strip, 6, 6, {'w': 1.0, 's': 0.6}, 0.4)
    graphite(strip, 6 + (S + 26), 6, ['nw', 'ne', 'sw', 'se'], 0.4)
    fuel(strip, 6 + 2 * (S + 26), 6, 0.0, {'n': True, 'e': True}, [], True, 0.4)
    fuel(strip, 6 + 3 * (S + 26), 6, 1.0, {'n': True, 's': True, 'e': True, 'w': True}, ['nw', 'ne', 'sw', 'se'], True, 0.4)
    fuel(strip, 6 + 4 * (S + 26), 6, 0.0, {'n': True, 's': False, 'e': True, 'w': True}, [], False, 0.4)
    empty(strip, 6 + 5 * (S + 26), 6)
    for k, label in enumerate(('WATER', 'GRAPHITE', 'ROD 2', 'ROD 6', 'NO WATER', 'EMPTY')):
        tf.draw(strip, 6 + k * (S + 26) + S // 2 - 1, S + 14, label, hexrgb('5f7c8c'), align='centre')
    w = max(core.w, strip.w) * zoom
    h = (core.h + strip.h + 6) * zoom
    rows = [[(11, 13, 17, 255)] * w for _ in range(h)]
    for canvas, oy in ((core, 0), (strip, core.h + 6)):
        ox = (max(core.w, strip.w) - canvas.w) // 2
        for y in range(canvas.h):
            for x in range(canvas.w):
                c = (*canvas.px[y][x], 255)
                for dy in range(zoom):
                    row = rows[(oy + y) * zoom + dy]
                    for dx in range(zoom):
                        row[(ox + x) * zoom + dx] = c
    path = os.path.join(OUT, 'core_cells.png')
    write_png(path, rows)
    print(path)


if __name__ == '__main__':
    main()
