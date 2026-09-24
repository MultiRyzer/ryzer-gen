"""Generates Ryzer Gen machine GUIs in the house style, matching the formed machines: a clean light
casing panel, a graphite machine bay with gauge wells and a readout screen, a cyan light line, then
the player inventory. Flat colours and crisp bevels only: no noise, rivets or grime.

Run from the repo root:
    python art/tools/gui_textures.py

Layouts here must match the screen classes in the mod. Button sprites sit to the right of the
panel, from x 176. Gauge fills are not pixel art: the screen draws them as lit gradients, and
fluids with the fluid's own animated texture, like Mekanism does.
"""
from pixelart import *  # noqa: F401,F403

SIZE = 256
PANEL_W, PANEL_H = 176, 190

# Microreactor layout. Keep in step with MicroreactorScreen.
FUEL_SLOT = (7, 20)
FUEL_BAR = (7, 40, 18, 6)
DUMP_BUTTON = (27, 21)
POWER_BUTTON = (8, 49)
REDSTONE_BUTTON = (26, 49)
SAFETY_SWITCH = (8, 67, 34, 14)
TEMP_WELL = (44, 20, 10, 62)
SCREEN = (58, 20, 80, 62)
ENERGY_WELL = (142, 20, 10, 62)
COOLANT_WELL = (156, 20, 10, 62)
BAY = (4, 16, 171, 86)
INVENTORY_Y = 106
HOTBAR_Y = 164

# Sprite sheet positions. Keep in step with MicroreactorScreen. Gauge fills are drawn in code
# (lit gradients, and real fluid textures), so only buttons live here.
SPRITE_BUTTON = (176, 64)
SPRITE_BUTTON_HOVER = (192, 64)
# Power button, whole keys: on, on hovered, off, off hovered.
SPRITE_POWER_KEYS = (176, 128)
SPRITE_FOLLOW_LOAD = (224, 80)
SPRITE_DUMP = (240, 80)
SPRITE_SAFETY_ARMED = (176, 96)
SPRITE_SAFETY_DISARMED = (176, 110)


def panel(t, w, h):
    """Light casing panel: dark outline with cut corners, crisp bevel lit from the top left."""
    t.rect(0, 0, w - 1, h - 1, 'F')
    t.rect(0, 0, w - 1, 0, 'U')
    t.rect(0, h - 1, w - 1, h - 1, 'U')
    t.rect(0, 0, 0, h - 1, 'U')
    t.rect(w - 1, 0, w - 1, h - 1, 'U')
    t.rect(1, 1, w - 2, 1, 'A')
    t.rect(1, 1, 1, h - 2, 'A')
    t.rect(2, 2, w - 3, 2, 'E')
    t.rect(2, 2, 2, h - 3, 'E')
    t.rect(1, h - 2, w - 2, h - 2, 'J')
    t.rect(w - 2, 1, w - 2, h - 2, 'J')
    t.rect(2, h - 3, w - 3, h - 3, 'I')
    t.rect(w - 3, 2, w - 3, h - 3, 'I')
    for x, y in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        t.px[y][x] = None
    for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
        t.set(x, y, 'U')


def recess(t, x, y, w, h, fill='S', shadow='U', light='b'):
    """A sunken area: shadow on the top and left, light on the bottom and right."""
    t.rect(x, y, x + w - 1, y + h - 1, fill)
    t.rect(x, y, x + w - 1, y, shadow)
    t.rect(x, y, x, y + h - 1, shadow)
    t.rect(x, y + h - 1, x + w - 1, y + h - 1, light)
    t.rect(x + w - 1, y, x + w - 1, y + h - 1, light)


def well(t, x, y, w, h, ticks=True):
    """A gauge well: near black inside a sunken frame, with faint scale ticks down the right."""
    recess(t, x, y, w, h, 'U', 'U', 'b')
    if ticks:
        for ty in range(y + 1 + 5, y + h - 1, 6):
            t.set(x + w - 2, ty, 'T' if (ty - y) % 12 else 'S')


def slot(t, x, y):
    """A vanilla-sized 18 x 18 item slot, sunk into the light casing."""
    recess(t, x, y, 18, 18, 'J', 'b', 'A')
    t.rect(x + 1, y + 1, x + 16, y + 1, 'I')
    t.rect(x + 1, y + 1, x + 1, y + 16, 'I')


def screen(t, x, y, w, h):
    """The readout: flat dark glass in a bezel."""
    recess(t, x, y, w, h, 'U', 'U', 'b')


def icon(t, x, y, rows):
    t.stamp(x, y, rows)


# Icons are drawn in 'c' and 'G'; stamped in the screen they become the cyan accent.
LIGHTNING = [
    '...cc.',
    '..cc..',
    '.cccc.',
    '..cc..',
    '.cc...',
    'c.....',
]
THERMOMETER = [
    '..c...',
    '..c...',
    '..c...',
    '..c...',
    '.ccc..',
    '.ccc..',
]
DIAL = [
    '.cccc.',
    'c....c',
    'c..c.c',
    'c.c..c',
    'c....c',
    '.cccc.',
]
CORE = [
    '.cccc.',
    'cGGGGc',
    'cGccGc',
    'cGccGc',
    'cGGGGc',
    '.cccc.',
]
def blend(t, x, y, key, alpha):
    """Mixes a palette colour over what is already drawn, for soft glows."""
    under = t.px[y][x]
    over = PAL[key]
    mixed = ''.join('%02x' % round(int(under[i:i + 2], 16) * (1 - alpha) + int(over[i:i + 2], 16) * alpha)
                    for i in (0, 2, 4))
    t.px[y][x] = mixed


# A keycap seen slightly from above: lit top edge, face shading down to a dark front lip.
KEY_ROWS = ['I', 'J', 'J', 'J', 'b', 'b', 'b', 'b', 'b', 'b', 'S', 'T', 'T', 'U']
LIGHTER = {'A': 'A', 'E': 'A', 'I': 'E', 'J': 'I', 'b': 'J', 'S': 'b', 'T': 'S', 'U': 'T'}
DARKER = {'A': 'E', 'E': 'I', 'I': 'J', 'J': 'b', 'b': 'S', 'S': 'T', 'T': 'U', 'U': 'U'}


def keycap(t, x, y, hover):
    t.rect(x, y, x + 15, y + 15, 'U')
    for i, k in enumerate(KEY_ROWS):
        k = LIGHTER[k] if hover else k
        t.rect(x + 1, y + 1 + i, x + 14, y + 1 + i, k)
        t.set(x + 1, y + 1 + i, LIGHTER[k])
        t.set(x + 14, y + 1 + i, DARKER[k])
    for cx, cy in ((x, y), (x + 15, y), (x, y + 15), (x + 15, y + 15)):
        t.px[cy][cx] = None


POWER_SYMBOL = [
    '...##...',
    '.#.##.#.',
    '#..##..#',
    '#..##..#',
    '#......#',
    '#......#',
    '.#....#.',
    '..####..',
]


def power_key(t, x, y, on, hover):
    """Power key: lit green symbol with a soft halo when on, a dim red standby light when off."""
    keycap(t, x, y, hover)
    sx, sy = x + 4, y + 3
    lit = {(sx + i, sy + j) for j, row in enumerate(POWER_SYMBOL) for i, c in enumerate(row) if c == '#'}
    if on:
        for px in range(x + 1, x + 15):
            for py in range(y + 1, y + 12):
                if (px, py) in lit:
                    continue
                d = min(max(abs(px - lx), abs(py - ly)) for lx, ly in lit)
                if d == 1:
                    blend(t, px, py, 'n', 0.45)
                elif d == 2:
                    blend(t, px, py, 'n', 0.18)
        for px, py in lit:
            t.set(px, py, 'N' if py - sy <= 3 and 3 <= px - sx <= 4 else 'n')
    else:
        for px, py in lit:
            blend(t, px, py, 'x', 0.75)


def buttons(t):
    for (bx, by), hover in ((SPRITE_BUTTON, False), (SPRITE_BUTTON_HOVER, True)):
        keycap(t, bx, by, hover)
    kx, ky = SPRITE_POWER_KEYS
    for i, (on, hover) in enumerate(((True, False), (True, True), (False, False), (False, True))):
        power_key(t, kx + i * 16, ky, on, hover)
    # Follow load: a battery, topping up.
    x, y = SPRITE_FOLLOW_LOAD
    t.stamp(x + 4, y + 2, [
        '...UU...',
        '.UUEEUU.',
        '.UTTTTU.',
        '.UTTTTU.',
        '.UTTTTU.',
        '.UnnnnU.',
        '.UNnnnU.',
        '.UnnnnU.',
        '.UnnnnU.',
        '.UUUUUU.',
    ])
    # Dump: a glowing resistor coil with heat rising off it.
    x, y = SPRITE_DUMP
    t.stamp(x + 2, y + 2, [
        '...x...x....',
        '..x...x...x.',
        '...x...x.x..',
        '............',
        'U.X...X...XU',
        'UX.X.X.X.X.U',
        'U...X...X..U',
        '............',
        'UUUUUUUUUUUU',
    ])


def safety_switch(t):
    """Wide safety interlock switch. Armed: graphite plate, guard cover closed, green lamp.
    Disarmed: hazard stripes, cover flipped up, lever thrown, red lamp."""
    w, h = SAFETY_SWITCH[2], SAFETY_SWITCH[3]
    for (x, y), armed in ((SPRITE_SAFETY_ARMED, True), (SPRITE_SAFETY_DISARMED, False)):
        t.rect(x, y, x + w - 1, y + h - 1, 'U')
        if armed:
            t.rect(x + 1, y + 1, x + w - 2, y + h - 2, 'b')
        else:
            for yy in range(y + 1, y + h - 1):
                for xx in range(x + 1, x + w - 1):
                    t.set(xx, yy, 'Y' if (xx + yy) % 6 < 3 else 'T')
        t.rect(x + 1, y + 1, x + w - 2, y + 1, 'J')
        t.rect(x + 1, y + 1, x + 1, y + h - 2, 'J')
        t.rect(x + 1, y + h - 2, x + w - 2, y + h - 2, 'T')
        t.rect(x + w - 2, y + 1, x + w - 2, y + h - 2, 'T')
        # Lamp.
        lamp, glint = ('n', 'N') if armed else ('x', 'e')
        t.rect(x + 4, y + 4, x + 6, y + 9, 'U')
        t.rect(x + 5, y + 5, x + 5, y + 8, lamp)
        t.set(x + 5, y + 5, glint)
        if armed:
            # Closed guard cover over the lever.
            t.rect(x + 10, y + 3, x + 29, y + 10, 'U')
            t.rect(x + 11, y + 4, x + 28, y + 9, 'I')
            t.rect(x + 11, y + 4, x + 28, y + 4, 'A')
            t.rect(x + 11, y + 9, x + 28, y + 9, 'J')
            t.rect(x + 19, y + 6, x + 20, y + 8, 'X')
        else:
            # Cover flipped up out of the way, lever thrown up with its red knob.
            t.rect(x + 10, y + 2, x + 29, y + 3, 'U')
            t.rect(x + 11, y + 2, x + 28, y + 2, 'I')
            t.rect(x + 18, y + 5, x + 21, y + 11, 'U')
            t.rect(x + 19, y + 6, x + 20, y + 10, 'A')
            t.rect(x + 19, y + 5, x + 20, y + 6, 'x')


def cyan(rows):
    return [row.replace('c', 'i').replace('G', 'f') for row in rows]


def microreactor():
    t = Tex(SIZE)
    panel(t, PANEL_W, PANEL_H)
    bay_w, bay_h = BAY[2] - BAY[0] + 1, BAY[3] - BAY[1] + 1
    recess(t, BAY[0], BAY[1], bay_w, bay_h, 'S', 'U', 'A')

    # Fuel core: a slot with an orange accent rim and a faint core outline, its fuel bar below.
    x, y = FUEL_SLOT
    slot(t, x, y)
    t.rect(x - 1, y - 1, x + 18, y - 1, 'X')
    t.rect(x - 1, y - 1, x - 1, y + 18, 'X')
    t.rect(x - 1, y + 18, x + 18, y + 18, 'Z')
    t.rect(x + 18, y - 1, x + 18, y + 18, 'Z')
    t.stamp(x + 6, y + 6, [row.replace('c', 'I').replace('G', 'J') for row in CORE])
    bx, by, bw, bh = FUEL_BAR
    well(t, bx, by, bw, bh, ticks=False)
    sx0, sy0, sw, sh = SAFETY_SWITCH
    recess(t, sx0 - 1, sy0 - 1, sw + 2, sh + 2, 'U', 'U', 'b')

    well(t, *TEMP_WELL)
    well(t, *ENERGY_WELL)
    well(t, *COOLANT_WELL)
    screen(t, *SCREEN)

    # Row icons inside the screen, beside the output, temperature, fuel and efficiency readouts.
    sx, sy = SCREEN[0] + 4, SCREEN[1]
    t.rect(SCREEN[0] + 3, sy + 13, SCREEN[0] + SCREEN[2] - 4, sy + 13, 'f')
    icon(t, sx, sy + 17, cyan(LIGHTNING))
    icon(t, sx, sy + 29, cyan(THERMOMETER))
    icon(t, sx, sy + 41, cyan(CORE))
    icon(t, sx, sy + 53, cyan(DIAL))

    # A cyan light line between the machine and the inventory, like the strips on the machine.
    line_y = BAY[3] + 4
    t.rect(6, line_y - 1, PANEL_W - 7, line_y - 1, 'J')
    t.rect(6, line_y, PANEL_W - 7, line_y, 'i')
    t.rect(6, line_y + 1, PANEL_W - 7, line_y + 1, 'A')

    for row in range(3):
        for col in range(9):
            slot(t, 7 + col * 18, INVENTORY_Y - 1 + row * 18)
    for col in range(9):
        slot(t, 7 + col * 18, HOTBAR_Y - 1)

    buttons(t)
    safety_switch(t)
    return t


# Alloy smelter layout. Keep in step with AlloySmelterMenu and AlloySmelterScreen.
SMELTER_H = 166
SMELTER_BAY = (4, 13, 171, 71)
SMELTER_INPUTS = ((37, 16), (55, 16))
SMELTER_FUEL = (46, 52)
SMELTER_OUTPUT = (111, 30)
SMELTER_FLAME = (47, 36)
SMELTER_ARROW = (79, 34)
SPRITE_FLAME = (176, 0)
SPRITE_ARROW = (176, 14)

FLAME = [
    '......#.......',
    '......##......',
    '.....###......',
    '.....####.....',
    '....#####.....',
    '....######....',
    '...#######....',
    '...########...',
    '..#########...',
    '..##########..',
    '..##########..',
    '...########...',
    '....######....',
    '.....####.....',
]


def flame(t, x0, y0, lit):
    inside = lambda x, y: 0 <= y < 14 and 0 <= x < 14 and FLAME[y][x] == '#'
    for y in range(14):
        for x in range(14):
            if not inside(x, y):
                continue
            edge = not (inside(x - 1, y) and inside(x + 1, y) and inside(x, y - 1) and inside(x, y + 1))
            if lit:
                k = 'X' if edge else 'e' if y >= 8 and inside(x - 2, y) and inside(x + 2, y) else 'a'
            else:
                k = 'b' if edge else 'S'
            t.set(x0 + x, y0 + y, k)


def arrow(t, x0, y0, lit):
    """A 24 x 17 progress arrow: a shaft and a head, filled left to right by the screen."""
    inside = lambda x, y: 0 <= x < 24 and 0 <= y < 17 and ((x <= 14 and 6 <= y <= 10) or (x >= 14 and abs(y - 8) <= 23 - x))
    for y in range(17):
        for x in range(24):
            if not inside(x, y):
                continue
            edge = not (inside(x, y - 1) and inside(x, y + 1) and inside(x + 1, y))
            t.set(x0 + x, y0 + y, ('j' if edge else 'i') if lit else ('b' if edge else 'T'))


def alloy_smelter():
    t = Tex(SIZE)
    panel(t, PANEL_W, SMELTER_H)
    bx0, by0, bx1, by1 = SMELTER_BAY
    recess(t, bx0, by0, bx1 - bx0 + 1, by1 - by0 + 1, 'S', 'U', 'A')
    for x, y in SMELTER_INPUTS:
        slot(t, x, y)
    slot(t, *SMELTER_FUEL)
    # The large output slot.
    ox, oy = SMELTER_OUTPUT
    recess(t, ox, oy, 26, 26, 'J', 'b', 'A')
    t.rect(ox + 1, oy + 1, ox + 24, oy + 1, 'I')
    t.rect(ox + 1, oy + 1, ox + 1, oy + 24, 'I')
    t.rect(ox - 1, oy - 1, ox + 26, oy - 1, 'X')
    t.rect(ox - 1, oy - 1, ox - 1, oy + 26, 'X')
    t.rect(ox - 1, oy + 26, ox + 26, oy + 26, 'Z')
    t.rect(ox + 26, oy - 1, ox + 26, oy + 26, 'Z')
    flame(t, *SMELTER_FLAME, lit=False)
    arrow(t, *SMELTER_ARROW, lit=False)
    for row in range(3):
        for col in range(9):
            slot(t, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(t, 7 + col * 18, 141)
    # Lit sprites, drawn over the unlit ones by the screen as the smelter works.
    flame(t, *SPRITE_FLAME, lit=True)
    arrow(t, *SPRITE_ARROW, lit=True)
    return t


def electric_alloy_smelter():
    """Like the fuel smelter, without the fuel slot, with an energy gauge on the right."""
    t = Tex(SIZE)
    panel(t, PANEL_W, SMELTER_H)
    bx0, by0, bx1, by1 = SMELTER_BAY
    recess(t, bx0, by0, bx1 - bx0 + 1, by1 - by0 + 1, 'S', 'U', 'A')
    slot(t, 37, 34)
    slot(t, 55, 34)
    ox, oy = SMELTER_OUTPUT
    recess(t, ox, oy, 26, 26, 'J', 'b', 'A')
    t.rect(ox + 1, oy + 1, ox + 24, oy + 1, 'I')
    t.rect(ox + 1, oy + 1, ox + 1, oy + 24, 'I')
    t.rect(ox - 1, oy - 1, ox + 26, oy - 1, 'X')
    t.rect(ox - 1, oy - 1, ox - 1, oy + 26, 'X')
    t.rect(ox - 1, oy + 26, ox + 26, oy + 26, 'Z')
    t.rect(ox + 26, oy - 1, ox + 26, oy + 26, 'Z')
    arrow(t, *SMELTER_ARROW, lit=False)
    well(t, 152, 17, 10, 52)
    for row in range(3):
        for col in range(9):
            slot(t, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(t, 7 + col * 18, 141)
    arrow(t, 176, 0, lit=True)
    return t


def home_battery():
    """Readout only (modules go in by hand): a charge gauge, the readout screen, six module bays."""
    t = Tex(SIZE)
    panel(t, PANEL_W, 112)
    recess(t, 4, 16, 168, 92, 'S', 'U', 'A')
    well(t, 10, 21, 14, 80)
    screen(t, 30, 21, 84, 60)
    t.rect(33, 49, 110, 49, 'f')
    for bay in range(6):
        recess(t, 120, 21 + bay * 13, 46, 11, 'U', 'U', 'b')
    return t


def cable():
    """Cable panel, drawn for six extract rows plus the footer. The rows are drawn in code, and the
    screen shows only as many as the cable has, then this texture's bottom edge."""
    t = Tex(SIZE)
    h = 194
    panel(t, PANEL_W, h)
    recess(t, 4, 16, 168, h - 20, 'S', 'U', 'A')
    return t


def main():
    publish('gui/microreactor', microreactor())
    publish('gui/alloy_smelter', alloy_smelter())
    publish('gui/electric_alloy_smelter', electric_alloy_smelter())
    publish('gui/home_battery', home_battery())
    publish('gui/cable', cable())


if __name__ == '__main__':
    main()
