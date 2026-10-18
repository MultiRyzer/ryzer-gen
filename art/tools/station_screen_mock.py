"""A still mock of the fission station's single screen, for approval before any Java.

One screen replaces the control panel and the projection. The core puzzle is the centrepiece: a
5 x 5 core planned by clicking, each rod showing its heat, its watered sides and the graphite on its
corners, with the picked part's preview under the mouse. On the left, the few numbers that matter
(heat against the best, stars, whether it holds) and the Inject and Eject keys, which move parts
through the station's ports. On the right, the technical breakdown, for flavour.

Drawn at 480 x 270 GUI pixels (Auto GUI scale at 1080p, the smallest it must fit), in Minecraft's
font, saved at three times that for viewing.

    python art/tools/station_screen_mock.py
writes art/concepts/station_screen/screen.png
"""
import math
import os

from projection_mock import (Canvas, FONT, text, text_width, heat_colour, hexrgb, k, BG, PANEL, EDGE, RULE, LABEL,
                             TEXT, CYAN, CYAN_DIM, GREEN, AMBER, RED, ROOT, write_png)

OUT = os.path.join(ROOT, 'art', 'concepts', 'station_screen')
W, H = 480, 270
VIEW = 3
N = 5
# The plan shown: the best lattice with one graphite still to place (where the mouse is).
PLAN = ['FWFWF',
        'WMWMW',
        'FWFWF',
        'WMW.W',
        'FWFWF']
HOVER = (3, 3)           # row, column
BEST = 34
WATER, WATER_LIT, WATER_DEEP = hexrgb('1f5a9c'), hexrgb('3f86cf'), hexrgb('0c1d36')
GRAPHITE, GRAPHITE_LIT = k('S'), k('l')


def cell(r, c, plan=PLAN):
    return plan[r][c] if 0 <= r < N and 0 <= c < N else None


def rod(r, c, plan=PLAN):
    """(heat, cooled) for the rod at r, c: water on every side it has; 2 heat, +1 per graphite corner."""
    cooled = all(cell(r + dr, c + dc, plan) in (None, 'W') for dr, dc in ((-1, 0), (1, 0), (0, -1), (0, 1)))
    heat = 2 + sum(1 for dr in (-1, 1) for dc in (-1, 1) if cell(r + dr, c + dc, plan) == 'M')
    return heat, cooled


def total(plan):
    return sum(rod(r, c, plan)[0] for r in range(N) for c in range(N) if plan[r][c] == 'F' and rod(r, c, plan)[1])


def big(cv, x, y, s, colour, scale):
    """Text drawn at a whole-number scale, as the game draws a scaled string."""
    for ch in s:
        adv, pts = FONT.get(ch, (6, []))
        for px, py in pts:
            cv.rect(x + px * scale, y + py * scale, x + px * scale + scale - 1, y + py * scale + scale - 1, colour)
        x += adv * scale
    return x


STAR = ['..#..', '..#..', '#####', '.###.', '##.##']


def star(cv, x, y, on):
    for dy, row in enumerate(STAR):
        for dx, ch in enumerate(row):
            if ch == '#':
                cv.set(x + dx, y + dy, AMBER if on else hexrgb('2b3036'))


def panel(cv, x0, y0, x1, y1):
    cv.rect(x0, y0, x1, y1, PANEL)
    cv.frame(x0, y0, x1, y1, EDGE)
    cv.rect(x0, y0, x0 + 24, y0, CYAN)


def key(cv, x0, y0, x1, y1, label, colour=TEXT, lit=False):
    """A keycap: lit top edge, shaded face, dark front lip."""
    cv.rect(x0, y0, x1, y1, hexrgb('2b3036'))
    cv.rect(x0, y0, x1, y0, hexrgb('535a64') if not lit else CYAN)
    cv.rect(x0, y1 - 1, x1, y1, hexrgb('0b0d10'))
    text(cv, (x0 + x1) // 2 + 1, (y0 + y1) // 2 - 4, label, colour, align='centre')


def core(cv, x0, y0, size):
    """The core: lit water, glowing rods with their heat, watered sides, graphite corners, the hover."""
    cs = size // N
    hottest = 6
    after = [row[:] for row in PLAN]
    after[HOVER[0]] = after[HOVER[0]][:HOVER[1]] + 'M' + after[HOVER[0]][HOVER[1] + 1:]
    cv.rect(x0 - 3, y0 - 3, x0 + size + 2, y0 + size + 2, hexrgb('0b0d10'))
    cv.frame(x0 - 3, y0 - 3, x0 + size + 2, y0 + size + 2, EDGE)
    for r in range(N):
        for c in range(N):
            t = PLAN[r][c]
            cx0, cy0 = x0 + c * cs, y0 + r * cs
            cx1, cy1 = cx0 + cs - 3, cy0 + cs - 3
            mx, my = (cx0 + cx1) / 2, (cy0 + cy1) / 2
            if t == 'W':
                # Water: a flat channel, lit along its top, with two ripple lines.
                cv.rect(cx0, cy0, cx1, cy1, WATER)
                cv.rect(cx0, cy0, cx1, cy0, WATER_LIT)
                for fy, fx0, fx1 in ((0.38, 0.2, 0.55), (0.66, 0.45, 0.8)):
                    cv.rect(int(cx0 + fx0 * cs), int(cy0 + fy * cs), int(cx0 + fx1 * cs), int(cy0 + fy * cs), WATER_LIT)
            elif t == 'M':
                cv.rect(cx0, cy0, cx1, cy1, GRAPHITE)
                cv.rect(cx0, cy0, cx1, cy0 + 1, GRAPHITE_LIT)
                cv.rect(cx0, cy0, cx0 + 1, cy1, hexrgb('535a64'))
                cv.rect(cx0 + 2, cy1 - 1, cx1, cy1, k('T'))
                cv.rect(cx1 - 1, cy0 + 2, cx1, cy1, k('T'))
                cv.rect(int(mx) - 2, int(my) - 2, int(mx) + 1, int(my) + 1, k('U'))
            elif t == 'F':
                heat, cooled = rod(r, c)
                cv.rect(cx0, cy0, cx1, cy1, hexrgb('181b20'))
                rr = cs * 0.34
                hot = heat_colour(0.45 + 0.55 * min(1, (heat - 2) / 4))
                for y in range(cy0, cy1 + 1):
                    for x in range(cx0, cx1 + 1):
                        dx, dy = x + 0.5 - mx, y + 0.5 - my
                        d = math.hypot(dx, dy)
                        if d <= rr:
                            shade = 0.78 + 0.35 * max(0.0, (-dx - dy) / (1.414 * max(d, 0.1))) - 0.25 * (d / rr) ** 2
                            col = tuple(min(255, round(v * shade)) for v in hot)
                            cv.set(x, y, col if d < rr - 0.8 else tuple(v // 2 for v in hot))
                        elif d < rr + 5:
                            g = (1 - (d - rr) / 5) ** 2 * 0.35
                            cv.blend(x, y, heat_colour(0.7), g)
                tf.draw(cv, int(mx) + 1, int(my) - 3, str(heat), hexrgb('1a0f08'), align='centre')
                # Its watered sides: cyan bars (an edge side has no bar), and graphite corner marks.
                for dr, dc, bar in ((-1, 0, (cx0 + cs // 3, cy0 + 1, cx1 - cs // 3, cy0 + 2)),
                                    (1, 0, (cx0 + cs // 3, cy1 - 2, cx1 - cs // 3, cy1 - 1)),
                                    (0, -1, (cx0 + 1, cy0 + cs // 3, cx0 + 2, cy1 - cs // 3)),
                                    (0, 1, (cx1 - 2, cy0 + cs // 3, cx1 - 1, cy1 - cs // 3))):
                    n = cell(r + dr, c + dc)
                    if n is not None:
                        cv.rect(*bar, CYAN if n == 'W' else RED)
                for dr, dc, (ax, ay, sx, sy) in ((-1, -1, (cx0 + 1, cy0 + 1, 1, 1)), (-1, 1, (cx1 - 1, cy0 + 1, -1, 1)),
                                                 (1, -1, (cx0 + 1, cy1 - 1, 1, -1)), (1, 1, (cx1 - 1, cy1 - 1, -1, -1))):
                    if cell(r + dr, c + dc) == 'M':
                        for i in range(4):
                            cv.set(ax + i * sx, ay, hexrgb('ffd27a'))
                            cv.set(ax, ay + i * sy, hexrgb('ffd27a'))
                # A rod the hovered graphite would warm: a green outline.
                if (r, c) != HOVER and rod(r, c, after)[0] > heat:
                    cv.frame(cx0 - 1, cy0 - 1, cx1 + 1, cy1 + 1, GREEN)
            else:
                cv.rect(cx0, cy0, cx1, cy1, hexrgb('181b20'))
    # The hover: a ghost of the graphite, and what it would add.
    r, c = HOVER
    cx0, cy0 = x0 + c * cs, y0 + r * cs
    cx1, cy1 = cx0 + cs - 3, cy0 + cs - 3
    for y in range(cy0, cy1 + 1):
        for x in range(cx0, cx1 + 1):
            cv.blend(x, y, GRAPHITE_LIT, 0.45)
    cv.frame(cx0, cy0, cx1, cy1, CYAN)
    gain = total(after) - total(PLAN)
    tag = f'+{gain}'
    cv.rect(cx1 - tf.width(tag) - 3, cy0 + 1, cx1 - 1, cy0 + 10, hexrgb('0b0d10'))
    tf.draw(cv, cx1 - 2, cy0 + 2, tag, GREEN, align='right')


# ---------------------------------------------------------------- the terminal look
import terminal_font as tf

PHOSPHOR = hexrgb('35c8f5')
PHOSPHOR_DIM = hexrgb('1b5a73')
PHOSPHOR_FAINT = hexrgb('123848')
GROUND = hexrgb('0a0c0f')
UNLIT = hexrgb('101820')
SCRAM = hexrgb('7a5418')


def rail(cv, x0, x1, y, tag, title):
    """A panel's header rail: its number and title, then a dotted line running to the panel's edge."""
    x = tf.draw(cv, x0, y, '[' + tag + ']', PHOSPHOR_DIM)
    x = tf.draw(cv, x + 6, y, title, PHOSPHOR, glow=PHOSPHOR)
    for px in range(x + 5, x1, 2):
        cv.set(px, y + 3, PHOSPHOR_FAINT)


def brackets(cv, x0, y0, x1, y1, colour=None):
    """A panel drawn as its four corners, as a terminal frames a window."""
    c = colour or PHOSPHOR_DIM
    n = 6
    for (x, y, sx, sy) in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
        cv.rect(min(x, x + sx * n), y, max(x, x + sx * n), y, c)
        cv.rect(x, min(y, y + sy * n), x, max(y, y + sy * n), c)


SEGMENTS = {'0': 'abcdef', '1': 'bc', '2': 'abged', '3': 'abgcd', '4': 'fgbc', '5': 'afgcd', '6': 'afgedc',
            '7': 'abc', '8': 'abcdefg', '9': 'abcfgd'}


def segments(cv, x, y, digit, colour, w=12, h=22, t=3):
    """A seven-segment digit, its unlit segments faintly ghosted as on a real display."""
    on = SEGMENTS[digit]
    half = h // 2
    seg = {'a': (x + t, y, x + w - t - 1, y + t - 1), 'g': (x + t, y + half - 1, x + w - t - 1, y + half + t - 2),
           'd': (x + t, y + h - t, x + w - t - 1, y + h - 1), 'f': (x, y + t, x + t - 1, y + half - 2),
           'b': (x + w - t, y + t, x + w - 1, y + half - 2), 'e': (x, y + half + t - 1, x + t - 1, y + h - t - 1),
           'c': (x + w - t, y + half + t - 1, x + w - 1, y + h - t - 1)}
    for name, r in seg.items():
        cv.rect(*r, colour if name in on else UNLIT)


def command(cv, x0, y0, x1, label, lit=False):
    """A command key: bracketed text, filled while it is the one under the mouse."""
    if lit:
        cv.rect(x0, y0, x1, y0 + 12, PHOSPHOR)
        tf.draw(cv, (x0 + x1) // 2 + 1, y0 + 3, '[ ' + label + ' ]', GROUND, align='centre')
    else:
        cv.frame(x0, y0, x1, y0 + 12, PHOSPHOR_DIM)
        tf.draw(cv, (x0 + x1) // 2 + 1, y0 + 3, '[ ' + label + ' ]', PHOSPHOR, align='centre')


def row(cv, x0, x1, y, label, value, colour=None):
    tf.draw(cv, x0, y, label, PHOSPHOR_DIM)
    tf.draw(cv, x1, y, value, colour or TEXT, align='right')


def screen():
    cv = Canvas(W, H, GROUND)
    # Header: where you are, the station's switches, its status.
    tf.draw(cv, 6, 5, 'RYZERGEN // STATION 01 // CORE PLANNER', PHOSPHOR, glow=PHOSPHOR)
    tf.draw(cv, 300, 5, '[PWR]', GREEN)
    tf.draw(cv, 334, 5, '[RS:OFF]', PHOSPHOR_DIM)
    tf.draw(cv, 386, 5, '[SAFE]', GREEN)
    cv.rect(425, 3, 474, 13, hexrgb('0f2a16'))
    cv.rect(429, 7, 431, 9, GREEN)
    tf.draw(cv, 436, 5, 'ONLINE', GREEN, glow=GREEN)
    for x in range(6, 475, 2):
        cv.set(x, 17, PHOSPHOR_FAINT)

    # [01] the simple numbers.
    brackets(cv, 6, 22, 128, 252)
    rail(cv, 11, 124, 27, '01', 'OUTPUT')
    tf.draw(cv, 11, 41, 'HEAT', PHOSPHOR_DIM)
    heat = total(PLAN)
    for i, d in enumerate('%02d' % heat):
        segments(cv, 11 + i * 16, 51, d, PHOSPHOR)
    tf.draw(cv, 45, 66, '/%d' % BEST, PHOSPHOR_DIM)
    for i in range(3):
        star(cv, 82 + i * 9, 54, i < 2)
    # A segmented meter: one block for every two heat.
    for i in range(BEST // 2):
        x = 11 + i * 7 if i < 16 else None
        if x is None:
            continue
        cv.rect(x, 79, x + 5, 83, PHOSPHOR if i < heat // 2 else UNLIT)
    tf.draw(cv, 11, 90, 'HOLDS STEADY', GREEN, glow=GREEN)
    row(cv, 11, 123, 101, 'TO BEST', '-%02d' % (BEST - heat), AMBER)

    rail(cv, 11, 124, 117, '02', 'COMPONENTS')
    fuel = sum(r.count('F') for r in PLAN)
    graphite = sum(r.count('M') for r in PLAN)
    row(cv, 11, 123, 130, 'FUEL RODS', '%02d/%02d' % (fuel, fuel), GREEN)
    row(cv, 11, 123, 140, 'GRAPHITE', '%02d/%02d' % (graphite, graphite), GREEN)
    row(cv, 11, 123, 150, 'WATER', 'PORT OK', GREEN)
    command(cv, 11, 164, 123, 'INJECT', lit=True)
    command(cv, 11, 181, 123, 'EJECT')
    # The error a command gives with nothing on its port.
    cv.frame(11, 200, 123, 230, RED)
    cv.rect(11, 200, 123, 209, RED)
    tf.draw(cv, 15, 202, 'ERR 02', GROUND)
    tf.draw(cv, 15, 213, 'NO CHEST OR PIPE', RED)
    tf.draw(cv, 15, 222, 'ON THE INPUT PORT', RED)

    # [03] the core, the centrepiece, its coordinates round it like a plot.
    size = 170
    gx0 = 134 + (206 - size) // 2 + 6
    gy0 = 50
    brackets(cv, 134, 22, 340, 252)
    rail(cv, 139, 336, 27, '03', 'CORE MAP 5X5')
    core(cv, gx0, gy0, size)
    cs = size // N
    for i in range(N):
        tf.draw(cv, gx0 + i * cs + cs // 2 - 3, gy0 - 12, 'ABCDE'[i], PHOSPHOR_DIM)
        tf.draw(cv, gx0 - 13, gy0 + i * cs + cs // 2 - 4, str(i + 1), PHOSPHOR_DIM)
    tools = [('F', 'FUEL', heat_colour(0.6)), ('G', 'GRAPH', GRAPHITE_LIT), ('W', 'WATER', WATER_LIT), ('X', 'CLEAR', hexrgb('2b3036'))]
    for i, (code, label, c) in enumerate(tools):
        x0 = 140 + i * 49
        picked = code == 'G'
        if picked:
            cv.rect(x0, 225, x0 + 45, 236, PHOSPHOR)
        else:
            cv.frame(x0, 225, x0 + 45, 236, PHOSPHOR_DIM)
        cv.rect(x0 + 3, 228, x0 + 7, 233, c)
        tf.draw(cv, x0 + 11, 228, label, GROUND if picked else PHOSPHOR)
    tf.draw(cv, 237, 241, 'RMB CLEARS A CHANNEL', PHOSPHOR_FAINT, align='centre')

    # [04] the physics, and [05] the forecast.
    brackets(cv, 346, 22, 474, 252)
    rail(cv, 351, 470, 27, '04', 'PHYSICS')
    rows = [('THERMAL', '17,700 FE/T'), ('OUTPUT', '7,080 FE/T'), ('TURBINE', '40.0%'), ('CORE', '0484°C'),
            ('FLUX', '2.41E13'), ('K-EFF', '1.0000'), ('REACT.', '+0 PCM'), ('COOLANT', '12 CH'),
            ('WATER', '1,240 MB/T'), ('STEAM', '2,480 MB/T'), ('BURNUP', '18.2%'), ('TILT', '1.00')]
    for i, (label, value) in enumerate(rows):
        row(cv, 351, 469, 40 + i * 10, label, value)
    rail(cv, 351, 470, 165, '05', 'FORECAST')
    tf.draw(cv, 351, 177, 'SETTLES 0484°C', GREEN, glow=GREEN)
    gx0, gx1, gy0, gy1 = 351, 469, 191, 236
    for x in range(gx0, gx1 + 1, 4):
        for y in range(gy0, gy1 + 1, 7):
            cv.set(x, y, UNLIT)
    cv.frame(gx0, gy0, gx1, gy1, PHOSPHOR_FAINT)
    scram_y = gy0 + 12
    for x in range(gx0, gx1, 3):
        cv.set(x, scram_y, SCRAM)
    tf.draw(cv, gx0 + 3, scram_y - 9 + 1, 'SCRAM 900°C', SCRAM)
    for x in range(gx0 + 1, gx1):
        f = (x - gx0) / (gx1 - gx0)
        t = 0.5 + 0.45 * (1 - math.exp(-f * 5))
        y = gy1 - 2 - round(t * (gy1 - gy0 - 14))
        cv.blend(x, y - 1, PHOSPHOR, 0.25)
        cv.blend(x, y + 1, PHOSPHOR, 0.25)
        cv.set(x, y, PHOSPHOR)
    tf.draw(cv, gx0, gy1 + 4, '0', PHOSPHOR_FAINT)
    tf.draw(cv, (gx0 + gx1) // 2, gy1 + 4, '60', PHOSPHOR_FAINT, align='centre')
    tf.draw(cv, gx1, gy1 + 4, '120S', PHOSPHOR_FAINT, align='right')

    # The log line along the foot, its cursor blinking.
    log = '> GRAPHITE AT D4 WOULD ADD +4 HEAT'
    x = tf.draw(cv, 6, H - 9, log, PHOSPHOR, glow=PHOSPHOR)
    cv.rect(x + 2, H - 9, x + 6, H - 3, PHOSPHOR)
    # CRT scanlines: every other row a touch darker.
    for y in range(0, H, 2):
        for x in range(W):
            r_, g_, b_ = cv.px[y][x]
            cv.px[y][x] = (r_ * 9 // 10, g_ * 9 // 10, b_ * 9 // 10)
    return cv


def main():
    os.makedirs(OUT, exist_ok=True)
    cv = screen()
    rows = []
    for row in cv.px:
        line = []
        for c in row:
            line += [(*c, 255)] * VIEW
        rows += [line] * VIEW
    write_png(os.path.join(OUT, 'screen.png'), rows)
    print(os.path.join(OUT, 'screen.png'), 'heat', total(PLAN))


if __name__ == '__main__':
    main()
