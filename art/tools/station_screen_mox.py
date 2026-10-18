"""The station screen's MOX tab, mocked: the MOX puzzle's best core (a control rod in the middle, graphite
on the edge middles, rods in a cross), the core tabs on the core box, the load-out racks that replace the
components rows, missing parts marked on the core, and the warning a switch of tab gives while the core
is loaded. Drawn on the screen's 960 x 540 half-pixel grid like station_screen_v3.py.

    python art/tools/station_screen_mox.py
writes art/concepts/station_screen/screen_mox.png: the MOX tab at the screen's real size (2x), and under
it the switch warning and close-ups of the new cells at 4x.
"""
import math
import os

import core_cells_mock as cc
import terminal_font as tf
from projection_mock import Canvas, hexrgb, heat_colour, write_png, ROOT
from station_screen_v3 import (box, row, level, blocks, big_echo, key, GROUND, BOX, TITLE, LABEL, TEXT, DIM, UNLIT, GREEN,
                               YELLOW, AMBER, RED, CYAN)

OUT = os.path.join(ROOT, 'art', 'concepts', 'station_screen')
W, H = 960, 540
GOLD = hexrgb('ffd27a')
SILVER = hexrgb('c2c9d2')
MOX_CORNER = hexrgb('ff7a3d')
cc.S = 30

# The MOX puzzle's best (47 heat), and the parts not yet in the core for the mock.
PLAN = ['FWMWF', 'WFWFW', 'MWCWM', 'WFWFW', 'FWMWF']
MISSING = {(4, 4), (2, 4)}   # a rod and a graphite planned but not loaded
CORNERS = (('nw', (-1, -1)), ('ne', (-1, 1)), ('sw', (1, -1)), ('se', (1, 1)))
SIDES = (('n', (-1, 0)), ('s', (1, 0)), ('w', (0, -1)), ('e', (0, 1)))


def at(r, c):
    return PLAN[r][c] if 0 <= r < 5 and 0 <= c < 5 else None


def mox_heat(r, c):
    """3 heat, +2 per graphite corner, +1 per MOX rod corner; over 4 needs a control corner; (heat, tamed, ok)."""
    g = sum(at(r + dr, c + dc) == 'M' for _, (dr, dc) in CORNERS)
    f = sum(at(r + dr, c + dc) == 'F' for _, (dr, dc) in CORNERS)
    ctl = sum(at(r + dr, c + dc) == 'C' for _, (dr, dc) in CORNERS)
    heat = 3 + 2 * g + f
    return heat, ctl > 0, heat <= 4 or ctl > 0


def mox_fuel(cv, x0, y0, r, c, t=0.4):
    """A MOX rod: as a uranium rod, its pellet hotter, a two-digit heat, and its corner marks by part."""
    heat, tamed, ok = mox_heat(r, c)
    sides = {s: at(r + dr, c + dc) == 'W' for s, (dr, dc) in SIDES if at(r + dr, c + dc) is not None}
    cc.fuel(cv, x0, y0, min(1.0, (heat - 3) / 9), sides, [], all(sides.values()) and ok, t)
    a, b, n = x0 + 2, y0 + 2, cc.S - 4
    mx, my = a + n // 2, b + n // 2
    face = heat_colour(0.45 + 0.55 * min(1.0, (heat - 3) / 9))
    cv.rect(mx - 6, my - 4, mx + 5, my + 4, cc.mul(face, 0.9))
    tf.draw(cv, mx, my - 3, str(heat), hexrgb('1a0f08'), align='centre')
    for corner, (dr, dc) in CORNERS:
        part = at(r + dr, c + dc)
        colour = {'M': cc.AMBER, 'F': MOX_CORNER, 'C': SILVER}.get(part)
        if not colour:
            continue
        ox = a if corner[1] == 'w' else a + n - 1
        oy = b if corner[0] == 'n' else b + n - 1
        sx = 1 if corner[1] == 'w' else -1
        sy = 1 if corner[0] == 'n' else -1
        for k in range(5 if part != 'F' else 3):
            cv.set(ox + k * sx, oy, colour)
            cv.set(ox, oy + k * sy, colour)


def control(cv, x0, y0, tames):
    """A control rod seen from above: a cruciform absorber blade in brushed steel round a drive hub,
    its absorber tubes as dots along the arms; a silver glow towards the rods it tames."""
    cc.socket(cv, x0, y0)
    a, b, n = x0 + 2, y0 + 2, cc.S - 4
    cv.rect(a, b, a + n - 1, b + n - 1, hexrgb('17191d'))
    for k in range(0, n, 4):
        for m in range(0, n, 4):
            cv.set(a + k, b + m, hexrgb('1f2328'))
    m = n // 2
    for corner in tames:
        ox = a if corner[1] == 'w' else a + n - 1
        oy = b if corner[0] == 'n' else b + n - 1
        for j in range(n):
            for i in range(n):
                d = math.hypot(a + i - ox, b + j - oy)
                if d < 8:
                    cv.blend(a + i, b + j, SILVER, 0.35 * cc.bands((1 - d / 8) ** 1.5, 3))
    for i in range(2, n - 2):
        for w in range(-2, 2):
            lit = 0.25 + 0.75 * (1 if w < 0 else 0.3)
            cv.set(a + i, b + m + w, cc.mix(cc.CLAD_DARK, cc.CLAD_LIT, lit))
            cv.set(a + m + w, b + i, cc.mix(cc.CLAD_DARK, cc.CLAD_LIT, lit))
    for i in range(4, n - 4, 3):
        if abs(i - m) > 3:
            cv.set(a + i, b + m, cc.CLAD_DARK)
            cv.set(a + m, b + i, cc.CLAD_DARK)
    cv.rect(a + m - 4, b + m - 4, a + m + 3, b + m + 3, cc.RIM_DARK)
    cv.rect(a + m - 3, b + m - 3, a + m + 2, b + m + 2, hexrgb('3a3f47'))
    cv.rect(a + m - 3, b + m - 3, a + m + 2, b + m - 3, cc.CLAD)
    cv.rect(a + m - 1, b + m - 1, a + m, b + m, hexrgb('7ad06a'))


def missing_mark(cv, x0, y0):
    """A planned part not in the core: an amber dashed frame and a warning tab in the top right."""
    a, b, n = x0 + 1, y0 + 1, cc.S - 3
    for k in range(0, n, 4):
        for d in range(2):
            for x, y in ((a + k + d, b), (a + k + d, b + n - 1), (a, b + k + d), (a + n - 1, b + k + d)):
                cv.set(x, y, AMBER)
    cv.rect(a + n - 9, b, a + n - 1, b + 8, AMBER)
    cv.rect(a + n - 5, b + 2, a + n - 5, b + 4, GROUND)
    cv.set(a + n - 5, b + 6, GROUND)


def core(cv, x0, y0):
    cv.rect(x0 - 3, y0 - 3, x0 + 5 * cc.S + 1, y0 + 5 * cc.S + 1, hexrgb('0b0d10'))
    cv.frame(x0 - 3, y0 - 3, x0 + 5 * cc.S + 1, y0 + 5 * cc.S + 1, hexrgb('2a2e35'))
    for r in range(5):
        for c in range(5):
            x, y = x0 + c * cc.S, y0 + r * cc.S
            kind = PLAN[r][c]
            if kind == 'W':
                touches = {s: 0.8 for s, (dr, dc) in SIDES if at(r + dr, c + dc) == 'F' and (r + dr, c + dc) not in MISSING}
                cc.water(cv, x, y, touches, 0.4 + (r * 5 + c) * 0.37)
            elif kind == 'M':
                cc.graphite(cv, x, y, [k for k, (dr, dc) in CORNERS if at(r + dr, c + dc) == 'F'], 0.4)
            elif kind == 'C':
                control(cv, x, y, [k for k, (dr, dc) in CORNERS if at(r + dr, c + dc) == 'F'])
            else:
                mox_fuel(cv, x, y, r, c)
            if (r, c) in MISSING:
                for yy in range(y + 2, y + cc.S - 2):
                    for xx in range(x + 2, x + cc.S - 2):
                        cv.set(xx, yy, cc.mul(cv.px[yy][xx], 0.4))
                missing_mark(cv, x, y)


def rack_rod(cv, x, y, state):
    """One fuel slot in the load-out rack, 10 x 26 units: a rod standing in it, lit when in the core."""
    cv.frame(x, y, x + 9, y + 25, BOX if state != 'missing' else hexrgb('5a2a22'))
    if state == 'in':
        cv.rect(x + 3, y + 3, x + 6, y + 22, SILVER)
        cv.rect(x + 3, y + 3, x + 3, y + 22, hexrgb('e6ebf0'))
        cv.rect(x + 3, y + 3, x + 6, y + 7, hexrgb('ff8a3d'))
    elif state == 'input':
        for yy in range(y + 3, y + 23, 3):
            cv.rect(x + 3, yy, x + 6, yy, AMBER)
    else:
        cv.rect(x + 3, y + 21, x + 6, y + 22, RED)


def rack_block(cv, x, y, state, part):
    """One graphite or control rod slot, 18 x 18 units."""
    cv.frame(x, y, x + 17, y + 17, BOX if state != 'missing' else hexrgb('5a2a22'))
    if state == 'in' and part == 'M':
        cv.rect(x + 3, y + 3, x + 14, y + 14, hexrgb('30343a'))
        cv.rect(x + 3, y + 3, x + 14, y + 3, hexrgb('5a6068'))
        cv.rect(x + 7, y + 7, x + 10, y + 10, hexrgb('101215'))
    elif state == 'in':
        cv.rect(x + 3, y + 8, x + 14, y + 9, SILVER)
        cv.rect(x + 8, y + 3, x + 9, y + 14, SILVER)
        cv.rect(x + 7, y + 7, x + 10, y + 10, hexrgb('3a3f47'))
    elif state == 'input':
        for k in range(3, 15, 3):
            cv.set(x + k, y + 3, AMBER)
            cv.set(x + k, y + 14, AMBER)
            cv.set(x + 3, y + k, AMBER)
            cv.set(x + 14, y + k, AMBER)
    else:
        cv.rect(x + 7, y + 12, x + 10, y + 13, RED)


def loadout(cv):
    """The load-out box: a rack per part, each slot one planned part, and the count large."""
    box(cv, 12, 252, 300, 500, 'LOAD-OUT', 'MOX CORE')
    groups = [('FUEL RODS', 'MOX', ['in'] * 7 + ['input'], 'F'), ('GRAPHITE', '', ['in'] * 3 + ['missing'], 'M'),
              ('CONTROL RODS', '', ['in'], 'C')]
    y = 266
    for label, note, slots, part in groups:
        loaded = slots.count('in')
        tf.draw(cv, 26, y + 2, label, LABEL)
        if note:
            tf.draw(cv, 26 + tf.width(label) + 8, y + 2, note, hexrgb('ff8a3d'))
        colour = GREEN if loaded == len(slots) else AMBER if 'missing' not in slots else RED
        tf.draw(cv, 286, y - 2, '%d/%d' % (loaded, len(slots)), colour, align='right', scale=2)
        y += 16
        x = 26
        for state in slots:
            if part == 'F':
                rack_rod(cv, x, y, state)
                x += 13
            else:
                rack_block(cv, x, y, state, part)
                x += 21
        y += (26 if part == 'F' else 18) + 10
    # The water and the two ports, one line each.
    tf.draw(cv, 26, y, 'WATER', LABEL)
    cv.rect(66, y + 1, 230, y + 5, UNLIT)
    cv.rect(66, y + 1, 66 + int(164 * 0.78), y + 5, hexrgb('2a74c0'))
    tf.draw(cv, 286, y, '12,400 MB', GREEN, align='right')
    y += 14
    tf.draw(cv, 26, y, 'IN', LABEL)
    tf.draw(cv, 44, y, 'CHEST, 1 ROD', TEXT)
    tf.draw(cv, 150, y, 'OUT', LABEL)
    tf.draw(cv, 174, y, 'CHEST, 21 FREE', TEXT)
    y += 16
    y = key(cv, 26, y, 286, 'INJECT COMPONENTS', lit=True)
    key(cv, 26, y + 6, 286, 'EJECT COMPONENTS')


def tabs(cv, x0, y0):
    """The core tabs, cut into the core box's top line like its title."""
    x = x0 + 8
    for label, score, picked in (('URANIUM', '34', False), ('MOX', '47', True)):
        text = '%s  %s' % (label, score)
        w = tf.width(text) + 16
        cv.rect(x, y0 - 8, x + w, y0 + 6, GROUND)
        if picked:
            cv.rect(x, y0 - 8, x + w, y0 + 6, CYAN)
            tf.draw(cv, x + 8, y0 - 4, text, GROUND)
        else:
            cv.frame(x, y0 - 8, x + w, y0 + 6, BOX)
            tf.draw(cv, x + 8, y0 - 4, text, LABEL)
        x += w + 6


def screen(warning=False):
    cv = Canvas(W, H, GROUND)
    heat = 47
    tf.draw(cv, 14, 11, 'RYZERGEN', CYAN)
    tf.draw(cv, 66, 11, '>  FISSION STATION  >  CORE PLANNER', LABEL)
    for x, label, colour in ((560, 'PWR ON', GREEN), (616, 'RS IGNORED', LABEL), (692, 'SAFETIES ARMED', GREEN), (790, 'SOUND ON', CYAN)):
        tf.draw(cv, x, 11, label, colour)
    cv.rect(862, 6, 946, 24, hexrgb('0f2a16'))
    cv.frame(862, 6, 946, 24, GREEN)
    tf.draw(cv, 904, 9, 'ONLINE', GREEN, align='centre', scale=2)
    cv.rect(0, 31, W, 31, BOX)

    box(cv, 12, 46, 300, 236, 'OUTPUT', 'HEAT UNITS')
    big_echo(cv, 26, 66, '%02d' % 43, TEXT, 8)
    tf.draw(cv, 126, 106, '/47', LABEL, scale=3)
    for i in range(3):
        sx = 200 + i * 28
        for dy, line in enumerate(['..#..', '..#..', '#####', '.###.', '##.##']):
            for dx, ch in enumerate(line):
                if ch == '#':
                    cv.rect(sx + dx * 4, 72 + dy * 4, sx + dx * 4 + 3, 72 + dy * 4 + 3, AMBER if i < 2 else UNLIT)
    tf.draw(cv, 200, 98, '2 OF 3 STARS', LABEL)
    n = 24
    bw = (262 // n) - 2
    for i in range(n):
        x = 26 + i * (bw + 2)
        cv.rect(x, 140, x + bw - 1, 145, CYAN if i < 43 // 2 else UNLIT)
    tf.draw(cv, 26, 154, 'HOLDS STEADY', GREEN, scale=2)
    row(cv, 26, 286, 178, 'TO THE BEST', '-04 HEAT', AMBER)
    row(cv, 26, 286, 190, 'RODS COOLED', '08 / 08', GREEN)
    row(cv, 26, 286, 202, 'RODS TAMED', '04 / 04', GREEN)
    row(cv, 26, 286, 214, 'POWER OUT', '14,620 FE/T', TEXT)
    loadout(cv)

    box(cv, 312, 46, 648, 500, '', '5 X 5  //  LIVE')
    tabs(cv, 312, 46)
    art = Canvas(158, 158, GROUND)
    core(art, 4, 4)
    ax, ay = 322, 76
    cv.blit(art, ax, ay, 2)
    for i in range(5):
        tf.draw(cv, ax + 8 + i * 60 + 27, ay - 6, 'ABCDE'[i], LABEL)
        tf.draw(cv, ax - 5, ay + 8 + i * 60 + 26, str(i + 1), LABEL)
    for i, (label, colour) in enumerate((('FUEL', heat_colour(0.6)), ('GRAPHITE', hexrgb('5a6068')), ('WATER', hexrgb('2a74c0')),
                                         ('CONTROL', SILVER), ('CLEAR', hexrgb('2b3036')))):
        x0 = 317 + i * 66
        if label == 'CONTROL':
            cv.rect(x0, 420, x0 + 61, 438, CYAN)
        else:
            cv.frame(x0, 420, x0 + 61, 438, BOX)
        cv.rect(x0 + 4, 426, x0 + 8, 431, colour)
        tf.draw(cv, x0 + 12, 426, label, GROUND if label == 'CONTROL' else TEXT)
    tf.draw(cv, 480, 452, 'LMB PLACE  //  RMB CLEAR  //  1-5 PICK A PART', LABEL, align='centre')
    for i, (label, colour) in enumerate((('WATERED', CYAN), ('GRAPHITE', hexrgb('ffd27a')), ('MOX ROD', MOX_CORNER),
                                         ('CONTROL', SILVER), ('NOT LOADED', AMBER))):
        x = 326 + i * 64
        cv.rect(x, 472, x + 7, 474, colour)
        tf.draw(cv, x + 10, 469, label, LABEL)
    tf.draw(cv, 480, 484, 'MOX: OVER 4 HEAT NEEDS A CONTROL ROD ON A CORNER', LABEL, align='centre')

    box(cv, 660, 46, 948, 300, 'REACTOR PHYSICS', 'T+00:12:40')
    rows = [('THERMAL POWER', '36,550 FE/T', 0.6), ('TURBINE EFFICIENCY', '40.0 %', 0.4), ('CORE TEMPERATURE', '0600 °C', 0.66),
            ('NEUTRON FLUX', '3.44E13 N/CM2S', 0.6), ('K-EFFECTIVE', '1.0000', 0.3), ('REACTIVITY', '+0 PCM', 0.1),
            ('CONTROL RODS IN', '1 / 1', 0.3), ('COOLANT CHANNELS', '12 / 12', 0.3), ('WATER DRAW', '731 MB/T', 0.62),
            ('FUEL BURNUP', '6.4 %', 0.1), ('QUADRANT TILT', '1.03', 0.2), ('STEADY STATE', 'X1.10  //  4.0 LOADS', 0.1)]
    for i, (label, value, f) in enumerate(rows):
        row(cv, 674, 934, 62 + i * 12, label, value, level(f))
    tf.draw(cv, 674, 216, 'FLUX HISTORY  60S', LABEL)
    for i in range(52):
        h = max(6, min(64, int(44 + 6 * math.sin(i * 0.7))))
        cv.rect(674 + i * 5, 286 - h, 677 + i * 5, 285, CYAN)
    box(cv, 660, 316, 948, 500, 'FORECAST', 'T+120 S')
    tf.draw(cv, 674, 332, 'SETTLES AT 600 °C', GREEN, scale=2)
    cv.frame(674, 360, 934, 474, BOX)
    for x in range(675, 934):
        f = (x - 674) / 260
        y = 470 - round((0.4 + 0.18 * (1 - math.exp(-f * 4))) * 106)
        cv.rect(x, y, x, y + 1, CYAN)

    cv.rect(0, 512, W, 540, hexrgb('0f141a'))
    cv.rect(0, 512, W, 512, BOX)
    tf.draw(cv, 14, 522, '> E5: FUEL ROD NOT LOADED // 1 AT THE INPUT PORT // INJECT TO LOAD IT', CYAN)
    tf.draw(cv, 946, 522, 'PLAN 43 / 47  |  CORE 0600 °C', TEXT, align='right')

    if warning:
        for y in range(H):
            for x in range(W):
                cv.px[y][x] = cc.mul(cv.px[y][x], 0.35)
        x0, y0, x1, y1 = 270, 190, 690, 350
        cv.rect(x0, y0, x1, y1, GROUND)
        cv.frame(x0, y0, x1, y1, AMBER)
        cv.rect(x0, y0, x1, y0 + 14, AMBER)
        tf.draw(cv, x0 + 8, y0 + 4, 'WARNING  //  SWITCH TO THE MOX CORE', GROUND)
        tf.draw(cv, x0 + 14, y0 + 30, 'THE CORE HOLDS 13 PARTS', AMBER, scale=2)
        tf.draw(cv, x0 + 14, y0 + 58, 'FOR THE URANIUM PLAN: 9 FUEL RODS, 4 GRAPHITE.', TEXT)
        tf.draw(cv, x0 + 14, y0 + 72, 'SWITCHING EJECTS THEM TO THE OUTPUT PORT', TEXT)
        tf.draw(cv, x0 + 14, y0 + 86, '(CHEST, 21 FREE). THE URANIUM PLAN IS KEPT.', LABEL)
        key(cv, x0 + 14, y0 + 112, x0 + 260, 'EJECT AND SWITCH', lit=True)
        cv.frame(x0 + 272, y0 + 112, x1 - 14, y0 + 136, BOX)
        tf.draw(cv, (x0 + 272 + x1 - 14) // 2 + 1, y0 + 117, 'CANCEL', TEXT, align='centre', scale=2)
    return cv


def main():
    os.makedirs(OUT, exist_ok=True)
    main_cv = screen()
    warn = screen(warning=True)
    # Close-ups: the control rod, a tamed MOX rod, a corner rod, a part not loaded.
    strip = Canvas(4 * 40, 40, GROUND)
    control(strip, 5, 5, ['nw', 'ne', 'sw', 'se'])
    mox_fuel(strip, 45, 5, 1, 1)
    mox_fuel(strip, 85, 5, 0, 0)
    cc.graphite(strip, 125, 5, [], 0.4)
    for yy in range(7, 33):
        for xx in range(127, 153):
            strip.px[yy][xx] = cc.mul(strip.px[yy][xx], 0.4)
    missing_mark(strip, 125, 5)
    # Sheet: the MOX tab at 2x on top; under it the warning (cropped round its box) at 2x and the cells at 4x.
    sheet_w, sheet_h = W * 2, H * 2 + 340
    rows = [[(11, 13, 17, 255)] * sheet_w for _ in range(sheet_h)]

    def paste(canvas, ox, oy, scale, crop=None):
        x0, y0, x1, y1 = crop or (0, 0, canvas.w, canvas.h)
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = (*canvas.px[y][x], 255)
                for dy in range(scale):
                    rr = rows[oy + (y - y0) * scale + dy]
                    for dx in range(scale):
                        rr[ox + (x - x0) * scale + dx] = c

    paste(main_cv, 0, 0, 2)
    paste(warn, 40, H * 2 + 10, 2, (250, 180, 710, 340))
    paste(strip, 1000, H * 2 + 40, 4)
    path = os.path.join(OUT, 'screen_mox.png')
    write_png(path, rows)
    print(path)


if __name__ == '__main__':
    main()
