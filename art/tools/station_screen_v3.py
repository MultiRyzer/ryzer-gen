"""The fission station's single screen, third mock: clean and dense, small text with the important
things large (after tiling-desktop system monitors and sci-fi game menus).

Drawn on a half-GUI-pixel grid: 960 x 540 units for a 480 x 270 GUI (Auto GUI scale at 1080p), so
small text is half a GUI pixel per font pixel (a scaled-down string in game, 2 screen pixels per
font pixel at that scale) while the core's pixel art stays at a whole GUI pixel. Saved at 1920 x
1080, the screen's real size.

    python art/tools/station_screen_v3.py
writes art/concepts/station_screen/screen_v3.png
"""
import math
import os

import terminal_font as tf
from projection_mock import Canvas, hexrgb, heat_colour, write_png, ROOT
from station_screen_mock import PLAN, BEST, N, total, core, star, GRAPHITE_LIT, WATER_LIT

OUT = os.path.join(ROOT, 'art', 'concepts', 'station_screen')
W, H = 960, 540
GROUND = hexrgb('0b0d11')
BOX = hexrgb('1e3a4a')
TITLE = hexrgb('35c8f5')
LABEL = hexrgb('5f7c8c')
TEXT = hexrgb('d6e4ec')
DIM = hexrgb('2a4656')
UNLIT = hexrgb('141c24')
GREEN = hexrgb('44d65e')
YELLOW = hexrgb('e2c13a')
AMBER = hexrgb('f0a030')
RED = hexrgb('e0503c')
CYAN = hexrgb('35c8f5')


def box(cv, x0, y0, x1, y1, title, note=None):
    """A thin box with its title cut into the top line, and an optional note on the right."""
    cv.frame(x0, y0, x1, y1, BOX)
    tw = tf.width(title)
    cv.rect(x0 + 8, y0, x0 + 8 + tw + 7, y0, GROUND)
    tf.draw(cv, x0 + 12, y0 - 3, title, TITLE)
    if note:
        nw = tf.width(note)
        cv.rect(x1 - 12 - nw - 4, y0, x1 - 8, y0, GROUND)
        tf.draw(cv, x1 - 10, y0 - 3, note, LABEL, align='right')


def row(cv, x0, x1, y, label, value, colour=TEXT):
    tf.draw(cv, x0, y, label, LABEL)
    tf.draw(cv, x1, y, value, colour, align='right')


def level(f):
    """A value's colour by how far up its range it sits, green through yellow to red."""
    return GREEN if f < 0.6 else YELLOW if f < 0.8 else AMBER if f < 0.92 else RED


def blocks(cv, x0, y, n, lit, colour, w=4, h=5, gap=1):
    for i in range(n):
        x = x0 + i * (w + gap)
        cv.rect(x, y, x + w - 1, y + h - 1, colour if i < lit else UNLIT)


def big_echo(cv, x, y, s, colour, scale):
    """Large blocky digits with an offset echo behind them, like a terminal banner."""
    tf.draw(cv, x + 3, y + 3, s, UNLIT, scale=scale)
    tf.draw(cv, x, y, s, colour, scale=scale)


def key(cv, x0, y0, x1, label, lit=False, scale=2):
    """A command: thin outline, or filled while it is the one under the mouse."""
    h = 7 * scale + 10
    if lit:
        cv.rect(x0, y0, x1, y0 + h, CYAN)
        tf.draw(cv, (x0 + x1) // 2 + 1, y0 + 5, label, GROUND, align='centre', scale=scale)
    else:
        cv.frame(x0, y0, x1, y0 + h, CYAN)
        tf.draw(cv, (x0 + x1) // 2 + 1, y0 + 5, label, CYAN, align='centre', scale=scale)
    return y0 + h


def screen():
    cv = Canvas(W, H, GROUND)
    heat = total(PLAN)

    # Top bar: where you are, the station's switches, its status.
    tf.draw(cv, 14, 11, 'RYZERGEN', CYAN)
    tf.draw(cv, 66, 11, '>  FISSION STATION 01  >  CORE PLANNER', LABEL)
    for i, (label, colour) in enumerate((('PWR ON', GREEN), ('RS IGNORED', LABEL), ('SAFETIES ARMED', GREEN))):
        x = 560 + [0, 56, 132][i]
        tf.draw(cv, x, 11, label, colour)
    cv.rect(862, 6, 946, 24, hexrgb('0f2a16'))
    cv.frame(862, 6, 946, 24, GREEN)
    tf.draw(cv, 904, 9, 'ONLINE', GREEN, align='centre', scale=2)
    cv.rect(0, 31, W, 31, BOX)

    # Output: the one number that matters, large.
    box(cv, 12, 46, 300, 236, 'OUTPUT', 'HEAT UNITS')
    big_echo(cv, 26, 66, '%02d' % heat, TEXT, 8)
    tf.draw(cv, 126, 106, '/%d' % BEST, LABEL, scale=3)
    for i in range(3):
        sx = 200 + i * 28
        for dy, line in enumerate(['..#..', '..#..', '#####', '.###.', '##.##']):
            for dx, ch in enumerate(line):
                if ch == '#':
                    cv.rect(sx + dx * 4, 72 + dy * 4, sx + dx * 4 + 3, 72 + dy * 4 + 3, AMBER if i < 2 else UNLIT)
    tf.draw(cv, 200, 98, '2 OF 3 STARS', LABEL)
    blocks(cv, 26, 140, BEST // 2, heat // 2, CYAN, w=14, h=6, gap=2)
    tf.draw(cv, 26, 154, 'HOLDS STEADY', GREEN, scale=2)
    row(cv, 26, 286, 178, 'TO THE BEST', '-%02d HEAT' % (BEST - heat), AMBER)
    row(cv, 26, 286, 190, 'RODS COOLED', '09 / 09', GREEN)
    row(cv, 26, 286, 202, 'GRAPHITE PLACED', '03', TEXT)
    row(cv, 26, 286, 214, 'POWER OUT', '7,080 FE/T', TEXT)

    # Components: what the plan needs, and the two commands.
    box(cv, 12, 252, 300, 500, 'COMPONENTS', 'PORTS')
    fuel = sum(r.count('F') for r in PLAN)
    graphite = sum(r.count('M') for r in PLAN)
    for i, (label, need) in enumerate((('FUEL RODS', fuel), ('GRAPHITE', graphite))):
        y = 268 + i * 16
        row(cv, 26, 286, y, label, '%02d / %02d IN INPUT' % (need, need), GREEN)
    row(cv, 26, 286, 300, 'WATER', 'PORT CONNECTED', GREEN)
    row(cv, 26, 286, 312, 'OUTPUT PORT', 'CHEST, 21 FREE', TEXT)
    y = key(cv, 26, 330, 286, 'INJECT COMPONENTS', lit=True)
    key(cv, 26, y + 8, 286, 'EJECT COMPONENTS')
    # The error a command gives with nothing on its port.
    cv.frame(26, 410, 286, 486, RED)
    cv.rect(26, 410, 286, 422, RED)
    tf.draw(cv, 32, 413, 'ERR 02  //  INPUT PORT', GROUND)
    tf.draw(cv, 32, 430, 'NO CHEST OR PIPE FOUND.', RED)
    tf.draw(cv, 32, 442, "ATTACH ONE TO THE STATION'S", LABEL)
    tf.draw(cv, 32, 454, 'INPUT PORT, THEN TRY AGAIN.', LABEL)
    tf.draw(cv, 32, 472, 'CODE 0x02  T+00:14', DIM)

    # The core, the centrepiece: pixel art at a whole GUI pixel, framed by coordinates.
    box(cv, 312, 46, 648, 500, 'CORE MAP', '5 X 5  //  LIVE')
    art = Canvas(158, 158, GROUND)
    core(art, 4, 4, 150)
    ax, ay = 312 + (336 - 316) // 2, 76
    cv.blit(art, ax, ay, 2)
    gx0, gy0 = ax + 8, ay + 8
    for i in range(N):
        tf.draw(cv, gx0 + i * 60 + 27, gy0 - 14, 'ABCDE'[i], LABEL)
        tf.draw(cv, gx0 - 13, gy0 + i * 60 + 26, str(i + 1), LABEL)
    tools = [('FUEL', heat_colour(0.6)), ('GRAPHITE', GRAPHITE_LIT), ('WATER', WATER_LIT), ('CLEAR', hexrgb('2b3036'))]
    for i, (label, colour) in enumerate(tools):
        x0 = 326 + i * 80
        picked = label == 'GRAPHITE'
        if picked:
            cv.rect(x0, 420, x0 + 74, 438, CYAN)
        else:
            cv.frame(x0, 420, x0 + 74, 438, BOX)
        cv.rect(x0 + 6, 426, x0 + 11, 431, colour)
        tf.draw(cv, x0 + 17, 426, label, GROUND if picked else TEXT)
    tf.draw(cv, 480, 452, 'LMB PLACE  //  RMB CLEAR  //  1-4 PICK A PART', LABEL, align='centre')
    for i, (label, colour) in enumerate((('WATERED SIDE', CYAN), ('MISSING WATER', RED), ('GRAPHITE CORNER', hexrgb('ffd27a')))):
        x = 330 + i * 104
        cv.rect(x, 472, x + 8, 474, colour)
        tf.draw(cv, x + 13, 469, label, LABEL)

    # Physics: the technical breakdown, small and dense, each value coloured by its level.
    box(cv, 660, 46, 948, 300, 'REACTOR PHYSICS', 'T+00:41:07')
    rows = [('THERMAL POWER', '17,700 FE/T', 0.52), ('TURBINE EFFICIENCY', '40.0 %', 0.4), ('CORE TEMPERATURE', '0484 °C', 0.54),
            ('NEUTRON FLUX', '2.41E13 N/CM2S', 0.5), ('K-EFFECTIVE', '1.0000', 0.3), ('REACTIVITY', '+0 PCM', 0.1),
            ('COOLANT CHANNELS', '12 / 12', 0.3), ('WATER DRAW', '1,240 MB/T', 0.62), ('STEAM RAISED', '2,480 MB/T', 0.62),
            ('FUEL BURNUP', '18.2 %', 0.18), ('QUADRANT TILT', '1.00', 0.1)]
    for i, (label, value, f) in enumerate(rows):
        y = 62 + i * 13
        row(cv, 674, 934, y, label, value, level(f))
    # A neutron flux history, as a monitor draws CPU load: thin bars, newest on the right.
    tf.draw(cv, 674, 212, 'FLUX HISTORY  60S', LABEL)
    for i in range(52):
        x = 674 + i * 5
        h = int(38 + 14 * math.sin(i * 0.5) * math.exp(-i * 0.02) + 6 * math.sin(i * 1.7))
        h = max(6, min(64, h))
        for yy in range(286 - h, 286):
            f = (286 - yy) / 64
            cv.rect(x, yy, x + 3, yy, CYAN if f < 0.6 else hexrgb('7fdcff') if f < 0.85 else TEXT)
    cv.rect(674, 287, 934, 287, BOX)

    # Forecast: the core's temperature run two minutes ahead.
    box(cv, 660, 316, 948, 500, 'FORECAST', 'T+120 S')
    tf.draw(cv, 674, 332, 'SETTLES AT 484 °C', GREEN, scale=2)
    gx0, gx1, gy0, gy1 = 674, 934, 360, 474
    for x in range(gx0, gx1 + 1, 8):
        for y in range(gy0, gy1 + 1, 8):
            cv.set(x, y, UNLIT)
    cv.frame(gx0, gy0, gx1, gy1, BOX)
    sy = gy0 + 14
    for x in range(gx0, gx1, 4):
        cv.rect(x, sy, x + 1, sy, hexrgb('7a5418'))
    tf.draw(cv, gx0 + 4, sy - 10, 'SCRAM 900 °C', hexrgb('7a5418'))
    for x in range(gx0 + 1, gx1):
        f = (x - gx0) / (gx1 - gx0)
        t = 0.5 + 0.45 * (1 - math.exp(-f * 5))
        y = gy1 - 4 - round(t * (gy1 - gy0 - 30))
        cv.rect(x, y, x, y + 1, CYAN)
        for yy in range(y + 2, gy1):
            cv.blend(x, yy, CYAN, 0.06)
    tf.draw(cv, gx0, gy1 + 6, '0', LABEL)
    tf.draw(cv, (gx0 + gx1) // 2, gy1 + 6, '60 S', LABEL, align='centre')
    tf.draw(cv, gx1, gy1 + 6, '120 S', LABEL, align='right')

    # Status bar along the foot, as a desktop's panel.
    cv.rect(0, 512, W, 540, hexrgb('0f141a'))
    cv.rect(0, 512, W, 512, BOX)
    segments = [('> D4: GRAPHITE WOULD ADD +4 HEAT TO 4 RODS', CYAN), ('PLAN 30 / 34', TEXT), ('WATER 1,240 MB/T', LABEL),
                ('STEAM 2,480 MB/T', LABEL), ('CORE 0484 °C', GREEN)]
    x = 14
    for i, (label, colour) in enumerate(segments):
        if i:
            tf.draw(cv, x, 522, '|', DIM)
            x += 14
        x = tf.draw(cv, x, 522, label, colour) + 14
    cv.rect(x - 8, 521, x - 4, 528, CYAN)
    return cv


def main():
    os.makedirs(OUT, exist_ok=True)
    cv = screen()
    rows = []
    for r in cv.px:
        line = []
        for c in r:
            line += [(*c, 255)] * 2
        rows += [line] * 2
    path = os.path.join(OUT, 'screen_v3.png')
    write_png(path, rows)
    print(path)


if __name__ == '__main__':
    main()
