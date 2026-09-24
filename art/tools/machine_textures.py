"""Block and item textures for the smaller machines, cables and tools, in the modern house style.
Machines are 3D box models built in datagen; this draws their own textures, and the item icons.

Run from the repo root:
    python art/tools/machine_textures.py
"""
from pixelart import *  # noqa: F401,F403


# ---------------------------------------------------------------- electric alloy smelter
#
# A 3D model (see electricSmelterModel in datagen): graphite body in a light steel frame. These are
# its own textures; the frame, trim, grille and port reuse the microreactor's.

def machine_body():
    """Graphite body panel with vertical ribs every four pixels, mapped by position."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    for x in (3, 7, 11, 15):
        t.rect(x, 0, x, 15, 'T')
        t.rect(x - 1, 0, x - 1, 15, 'b')
    return t


COIL = [
    '........',
    '.#...#..',
    '#.#.#.#.',
    '...#...#',
]


def smelter_window(lit):
    """10 x 8 window onto the heating coil: dark when idle, glowing orange while working."""
    t = Tex()
    t.rect(0, 0, 9, 7, 'T')
    t.rect(0, 0, 9, 0, 'b')
    t.rect(0, 0, 0, 7, 'b')
    t.rect(1, 1, 8, 6, 'Z' if lit else 'U')
    if lit:
        t.rect(1, 4, 8, 6, 'X')
    for y, row in enumerate(COIL):
        for x, c in enumerate(row):
            if c == '#':
                t.set(1 + x, 2 + y, 'e' if lit else 'b')
    t.set(8, 1, 'b' if not lit else 'a')
    return t


def lamp(lit):
    """8 x 1 status strip under the window."""
    t = Tex()
    t.rect(0, 0, 7, 0, 'n' if lit else 'T')
    if lit:
        t.set(0, 0, 'N')
    return t


# ---------------------------------------------------------------- shape tools
#
# Items are drawn as filled shapes, then outlined and shaded automatically the way vanilla items
# are: a one-pixel dark outline, lit top-left edges and shaded bottom-right edges.

def shape_item(filled, body, light, dark, outline='U', highlight=None):
    """filled: set of (x, y). Returns a Tex with the shape outlined and bevel-shaded."""
    t = Tex()
    inside = lambda x, y: (x, y) in filled
    for x, y in filled:
        lit = not inside(x - 1, y) or not inside(x, y - 1)
        shade = not inside(x + 1, y) or not inside(x, y + 1)
        k = light if lit and not shade else dark if shade and not lit else body
        if highlight and not inside(x - 1, y) and not inside(x, y - 1):
            k = highlight
        t.set(x, y, k)
    for x in range(16):
        for y in range(16):
            if not inside(x, y) and any(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                t.set(x, y, outline)
    return t


def disc(cx, cy, r):
    return {(x, y) for x in range(16) for y in range(16) if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r}


def ellipse(cx, cy, rx, ry):
    return {(x, y) for x in range(16) for y in range(16) if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1}


def thick_line(x0, y0, x1, y1, width):
    pts = set()
    steps = 64
    for i in range(steps + 1):
        x = x0 + (x1 - x0) * i / steps
        y = y0 + (y1 - y0) * i / steps
        reach = int(width + 1)
        for dx in range(-reach, reach + 1):
            for dy in range(-reach, reach + 1):
                if dx * dx + dy * dy <= width * width * 0.6:
                    pts.add((int(round(x + dx)), int(round(y + dy))))
    return {(x, y) for x, y in pts if 0 <= x < 16 and 0 <= y < 16}


# ---------------------------------------------------------------- cables and wrench

def cable_block():
    """Insulated cable. Arms map rows 5 to 10 along their length (see the cable model), so the red
    core stripe runs unbroken from end to end."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 5, 15, 5, 'b')
    t.rect(0, 6, 15, 6, 'S')
    t.rect(0, 7, 15, 7, 'x')
    t.rect(0, 8, 15, 8, '8')
    t.rect(0, 9, 15, 9, 'S')
    t.rect(0, 10, 15, 10, 'T')
    return t


def cable_core():
    """The junction box where arms meet: graphite with a bevel and a small red terminal."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(5, 5, 10, 5, 'b')
    t.rect(5, 5, 5, 10, 'b')
    t.rect(5, 10, 10, 10, 'T')
    t.rect(10, 5, 10, 10, 'T')
    t.rect(7, 7, 8, 8, '8')
    t.set(7, 7, 'x')
    return t


def cable_flange():
    """Extract flange: an 8 x 8 graphite plate with the red energy ring, like a port."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(4, 4, 11, 11, 'U')
    t.rect(5, 5, 10, 10, 'x')
    t.rect(6, 6, 9, 9, 'T')
    t.rect(7, 7, 8, 8, 'U')
    t.rect(4, 4, 11, 4, 'b')
    t.rect(4, 4, 4, 11, 'b')
    return t


def cable_item():
    """A thick coil of insulated cable with its red core showing along the loop and a bare copper end."""
    loop = ellipse(7.5, 7.5, 6.6, 5.6) - ellipse(7.5, 7.5, 3.6, 2.8)
    tail = {(x, y) for x, y in thick_line(11.5, 11.5, 14.5, 14.5, 1)}
    filled = loop | tail
    t = shape_item(filled, 'S', 'b', 'T')
    # The red core: a line round the middle of the band.
    for x, y in loop:
        r = ((x + 0.5 - 7.5) / 5.1) ** 2 + ((y + 0.5 - 7.5) / 4.2) ** 2
        if 0.86 <= r <= 1.12:
            t.set(x, y, 'x' if y < 8 else '8')
    # Bare copper conductor at the end of the tail.
    for x, y in ((14, 14), (15, 15), (14, 15), (15, 14)):
        if (x, y) in filled:
            t.set(x, y, 'R' if (x + y) % 2 else 'e')
    return t


def wrench_item():
    """An open-ended spanner. A three-pixel shaft runs diagonally into a round head, and the jaw is
    cut along the same line, so it opens towards the top right. The lower shaft has an orange grip."""
    band = lambda x, y: 14 <= x + y <= 16
    shaft = {(x, y) for x in range(1, 11) for y in range(16) if band(x, y)}
    head = disc(11.5, 3.5, 3.6)
    slot = {(x, y) for x in range(12, 16) for y in range(16) if band(x, y)}
    filled = {(x, y) for x, y in (shaft | head) - slot if 0 <= x < 16 and 0 <= y < 16}
    t = shape_item(filled, 'J', 'E', 'b', highlight='A')
    for x, y in filled:
        if x <= 5:
            lit = (x - 1, y) not in filled or (x, y - 1) not in filled
            shade = (x + 1, y) not in filled or (x, y + 1) not in filled
            t.set(x, y, 'e' if lit and not shade else 'Z' if shade and not lit else 'X')
    return t


def dosimeter_ring():
    """A steel ring seen at an angle, with a faceted fluorite crystal set on top (the dosimeter chip)."""
    band = ellipse(8, 10.5, 6.5, 4.2) - ellipse(8, 10.8, 4.3, 2.3)
    t = shape_item(band, 'J', 'E', 'b', highlight='A')
    # The far side of the band sits in shadow.
    for x, y in band:
        if y < 10 and 4 < x < 12 and t.px[y][x] == PAL['J']:
            t.set(x, y, 'I')
    # Setting and crystal: a small faceted gem in purple with a fluorescent cyan edge.
    t.stamp(5, 1, [
        '..UUUU..',
        '.U3443U.',
        'U234432U',
        'U123321U',
        '.U1221U.',
        '.UJUUJU.',
    ])
    t.set(8, 2, 'j')
    t.set(7, 3, 'i')
    return t


# ---------------------------------------------------------------- home battery

def module_front():
    """9 x 3 front plate of a lead-acid module: orange terminal, lead-grey case, a green charge lamp."""
    t = Tex()
    t.rect(0, 0, 15, 15, 's')
    t.stamp(0, 0, [
        'XeIIIIIII',
        'ZXssssssn',
        'ZZuuuuuuu',
    ])
    return t


def module_case():
    """The module's sides and top: plain lead grey."""
    t = Tex()
    t.rect(0, 0, 15, 15, 's')
    t.rect(0, 15, 15, 15, 'u')
    return t


def lead_acid_module_item():
    """A lead-acid battery: gunmetal case with an orange + terminal and a graphite - terminal."""
    case = {(x, y) for x in range(2, 14) for y in range(5, 14)}
    terminals = {(x, y) for x in (4, 5, 10, 11) for y in (3, 4)}
    t = shape_item(case | terminals, 's', 'J', 'u', highlight='I')
    for x, y in terminals:
        t.set(x, y, 'X' if x < 8 else 'T')
    t.set(4, 3, 'e')
    t.rect(3, 8, 12, 9, 'z')
    t.rect(3, 8, 5, 9, 'X')
    return t


def home_battery_item():
    """A tall cabinet with its bay column, three modules fitted."""
    body = {(x, y) for x in range(4, 12) for y in range(1, 15)}
    t = shape_item(body, 'F', 'A', 'J', highlight='A')
    t.rect(4, 1, 11, 1, 'S')
    t.rect(4, 14, 11, 14, 'S')
    t.rect(5, 3, 10, 12, 'T')
    for y in (4, 6, 8, 10, 12):
        t.rect(5, y, 10, y, 'U')
    for y in (7, 9, 11):
        t.rect(6, y, 9, y, 's')
        t.set(6, y, 'X')
    return t


TEXTURES = {
    'block/machine/body': machine_body,
    'block/machine/smelter_window': lambda: smelter_window(False),
    'block/machine/smelter_window_on': lambda: smelter_window(True),
    'block/machine/lamp_off': lambda: lamp(False),
    'block/machine/lamp_on': lambda: lamp(True),
    'block/energy_cable': cable_block,
    'block/energy_cable_flange': cable_flange,
    'block/energy_cable_core': cable_core,
    'item/energy_cable': cable_item,
    'item/wrench': wrench_item,
    'item/dosimeter_ring': dosimeter_ring,
    'block/machine/module_lead_acid': module_front,
    'block/machine/module_case': module_case,
    'item/lead_acid_module': lead_acid_module_item,
    'item/home_battery': home_battery_item,
}


def main():
    for name, fn in TEXTURES.items():
        publish(name, fn())


if __name__ == '__main__':
    main()
