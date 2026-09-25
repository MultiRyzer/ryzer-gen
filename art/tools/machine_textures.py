"""Block and item textures for the smaller machines, cables and tools, in the modern house style.
Machines are 3D box models built in datagen; this draws their own textures, and the item icons.

Run from the repo root:
    python art/tools/machine_textures.py
"""
import math

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

# A pipe family shares one shape; the body and band colours say what it carries, as the port rings
# do: orange items, blue liquids, white gases. Keys: body (fill, light, shade, dark) and band (lit, dim).
PIPES = {
    # Energy cable: an insulated graphite body with the red energy band.
    'energy_cable': (('S', 'b', 'T', 'U'), ('x', '8')),
    'item_pipe': (('F', 'A', 'I', 'J'), ('X', 'Z')),
    'fluid_pipe': (('F', 'A', 'I', 'J'), ('i', 'f')),
    # Steam lines are pressure-rated steel: the reactor vessel's gunmetal, with a white band.
    'gas_pipe': (('u', 's', 'z', 'T'), ('A', 'I')),
}


def pipe_block(name):
    """A pipe: the body lit along the top edge, with the band running its length. Arms map rows 5
    to 10, as on the cable."""
    (fill, light, shade, dark), (lit, dim) = PIPES[name]
    t = Tex()
    t.rect(0, 0, 15, 15, fill)
    t.rect(0, 5, 15, 5, light)
    t.rect(0, 6, 15, 6, 'E' if fill == 'F' else light)
    t.rect(0, 7, 15, 7, lit)
    t.rect(0, 8, 15, 8, dim)
    t.rect(0, 9, 15, 9, shade)
    t.rect(0, 10, 15, 10, dark)
    return t


def pipe_core(name):
    """The junction: a box with a crisp bevel and a small hub in the band colour."""
    (fill, light, shade, dark), (lit, dim) = PIPES[name]
    t = Tex()
    t.rect(0, 0, 15, 15, fill)
    t.rect(5, 5, 10, 5, light)
    t.rect(5, 5, 5, 10, light)
    t.rect(5, 10, 10, 10, dark)
    t.rect(10, 5, 10, 10, dark)
    t.rect(7, 7, 8, 8, dim)
    t.set(7, 7, lit)
    return t


def pipe_flange(name):
    """Extract flange: the cable's graphite plate with the pipe's ring colour."""
    lit = PIPES[name][1][0]
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(4, 4, 11, 11, 'U')
    t.rect(5, 5, 10, 10, lit)
    t.rect(6, 6, 9, 9, 'T')
    t.rect(7, 7, 8, 8, 'U')
    t.rect(4, 4, 11, 4, 'b')
    t.rect(4, 4, 4, 11, 'b')
    return t



def lit(nx, ny):
    """How much a surface facing (nx, ny) catches the light, which comes from the top left: 1 facing
    it, -1 facing away."""
    n = math.hypot(nx, ny) or 1
    return (-nx - ny) / (n * math.sqrt(2))


def wrench_item():
    """A compact open-ended spanner, drawn diagonally in the tech-mod way and kept slim like
    Mekanism's tools: a steel head with a clear jaw opening to the top right and a lit rim, a
    two-pixel shaft, an orange collar with a small cyan status light, a wrapped graphite grip and a
    steel end cap."""
    x0, y0, x1, y1 = 2.6, 13.4, 8.6, 7.4
    cx, cy, r = 10.9, 5.1, 2.95
    along = lambda x, y: ((x - x0) * (x1 - x0) + (y - y0) * (y1 - y0)) / ((x1 - x0) ** 2 + (y1 - y0) ** 2)
    cells = set()
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            a = min(1, max(0, along(px, py)))
            qx, qy = x0 + (x1 - x0) * a, y0 + (y1 - y0) * a
            on_shaft = math.hypot(px - qx, py - qy) <= 0.8
            on_head = math.hypot(px - cx, py - cy) <= r
            # The jaw: a slot from the middle of the head out to the top right.
            ux, uy = (px - cx) / 1.4142, (cy - py) / 1.4142
            jaw = (ux + uy) > -0.2 and abs(ux - uy) <= 1.1
            if (on_shaft or on_head) and not (on_head and jaw):
                cells.add((x, y))
    t = shape_item(cells, 'F', 'E', 'J')
    for x, y in cells:
        a = along(x + 0.5, y + 0.5)
        edge_lit = (x - 1, y) not in cells or (x, y - 1) not in cells
        edge_shade = (x + 1, y) not in cells or (x, y + 1) not in cells
        head = math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r
        if a < 0.08:
            # Steel end cap.
            t.set(x, y, 'E' if edge_lit else 'I')
        elif a < 0.5:
            # Grip, wrapped: lighter turns every other step along it.
            wrap = int(a * 11) % 2 == 0
            t.set(x, y, 'b' if wrap else 'T')
        elif a < 0.66:
            t.set(x, y, 'e' if edge_lit and not edge_shade else 'Z' if edge_shade and not edge_lit else 'X')
        elif head and edge_lit and not edge_shade:
            t.set(x, y, 'A')
    # The bite stays open: no outline inside the jaw, so it reads as a gap, not a hole.
    for y in range(16):
        for x in range(16):
            ux, uy = (x + 0.5 - cx) / 1.4142, (cy - y - 0.5) / 1.4142
            if (x, y) not in cells and (ux + uy) > 0.3 and abs(ux - uy) <= 1.5:
                t.px[y][x] = None
    # The status light on the collar: its middle pixel on the lit side.
    collar = sorted(((x, y) for x, y in cells if 0.5 <= along(x + 0.5, y + 0.5) < 0.66), key=lambda p: p[0] - p[1])
    if collar:
        x, y = collar[len(collar) // 2]
        t.set(x, y, 'i')
    return t


def dosimeter_ring():
    """A green band seen at a tilt, as magic mods draw their rings: two pixels thick, lit from the
    top left with no black outline (its darkest green is the edge), and a fluorite crystal (the
    dosimeter chip) in a small steel setting breaking the silhouette at the top left."""
    cx, cy = 8.3, 8.7
    tilt = math.radians(-38)
    cos, sin = math.cos(tilt), math.sin(tilt)
    t = Tex()
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - cx, y + 0.5 - cy
            u = px * cos + py * sin
            v = -px * sin + py * cos
            outer = math.sqrt((u / 6.6) ** 2 + (v / 5.4) ** 2)
            inner = math.sqrt((u / 4.3) ** 2 + (v / 3.1) ** 2)
            if outer > 1 or inner <= 1:
                continue
            # Near the inner edge the band's surface faces the hole; near the outer edge, away from it.
            facing = -1 if inner - 1 < 1 - outer else 1
            light = lit(px * facing, py * facing)
            t.set(x, y, 'n' if light > 0.55 else '7' if light > 0.05 else '6' if light > -0.5 else '5')
    t.set(3, 10, 'N')
    t.set(12, 7, 'N')
    # The setting and the fluorite chip.
    t.stamp(2, 2, [
        '.A3.',
        '3442',
        '3422',
        'J21J',
        '.JJ.',
    ])
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


def geiger_counter_item():
    """A handheld Geiger counter: light casing with a graphite carry handle, a dark readout with a
    cyan needle over its scale, a speaker grille, and the orange tube cap on the side."""
    body = {(x, y) for x in range(2, 14) for y in range(5, 14)}
    handle = {(x, y) for x in range(4, 12) for y in (2, 3)} | {(x, y) for x in (4, 5, 10, 11) for y in (3, 4)}
    t = shape_item(body, 'F', 'A', 'J', highlight='A')
    for x, y in handle:
        t.set(x, y, 'S')
    for x in range(4, 12):
        t.set(x, 2, 'b')
    for x in range(3, 13):
        t.set(x, 1, 'U')
    for x, y in ((3, 2), (12, 2), (3, 3), (12, 3), (3, 4), (12, 4), (6, 3), (7, 3), (8, 3), (9, 3)):
        t.set(x, y, 'U')
    # Readout: a dark window, the scale across the top in dim cyan, the needle lit.
    t.rect(4, 6, 11, 9, 'U')
    t.rect(5, 7, 10, 7, 'f')
    for x, y in ((6, 9), (7, 8), (8, 8)):
        t.set(x, y, 'i')
    t.set(8, 7, 'j')
    # Speaker grille and the tube cap.
    for x in (5, 7, 9):
        t.set(x, 11, 'T')
        t.set(x, 12, 'S')
    t.set(12, 11, 'X')
    t.set(12, 12, 'Z')
    return t


def speed_module_item():
    """A speed module: a graphite circuit card with a double cyan chevron and gold edge contacts."""
    card = {(x, y) for x in range(2, 14) for y in range(3, 12)}
    pins = {(x, 12) for x in (4, 6, 8, 10)}
    t = shape_item(card | pins, 'S', 'b', 'T', highlight='J')
    for x, y in pins:
        t.set(x, y, 'a')
    # Two chevrons pointing right, two pixels thick, bright at the tips.
    for ox in (4, 8):
        for dx in (0, 1):
            for i, dy in enumerate((-2, -1, 0, 1, 2)):
                t.set(ox + dx + (2 - abs(dy)), 7 + dy, 'i')
        t.set(ox + 3, 7, 'j')
    return t


# ---------------------------------------------------------------- cable fittings
# One look per tier, the same on every kind of cable and pipe (see CableUpgrade). Block textures
# tile across the fitting's faces; item textures show the fitting on its own.

def fitting_block(tier):
    """Clean banded metal: silver plating, heavy copper, or frosted cryogenic jacket."""
    fill, light, shade, line = {
        'silver': ('H', 'A', 'h', 'L'),
        'busbar': ('R', 'e', 'r', 'O'),
        'cryogenic': ('A', 'A', 'E', 'j'),
    }[tier]
    t = Tex()
    t.rect(0, 0, 15, 15, fill)
    for y in (0, 8):
        t.rect(0, y, 15, y, light)
        t.rect(0, y + 6, 15, y + 6, shade)
        t.rect(0, y + 7, 15, y + 7, line)
    if tier == 'busbar':
        # Bolt heads on the clamp.
        for x, y in ((3, 3), (12, 3), (3, 11), (12, 11)):
            t.set(x, y, 'X')
            t.set(x + 1, y + 1, 'Z')
    return t


def fitting_item(tier):
    """A fitting, drawn as a card like the Speed Module so upgrades read alike in a chest: a graphite
    card with gold contacts, one to three tier pips along the top, and the tier's symbol. Silver: a
    bright conductor bar. Busbar: two thick copper bars. Cryogenic: a cyan snowflake."""
    card = {(x, y) for x in range(2, 14) for y in range(3, 12)}
    pins = {(x, 12) for x in (4, 6, 8, 10)}
    t = shape_item(card | pins, 'S', 'b', 'T', highlight='J')
    for x, y in pins:
        t.set(x, y, 'a')
    level = {'silver': 1, 'busbar': 2, 'cryogenic': 3}[tier]
    for i in range(3):
        t.set(10 + i, 4, 'n' if i < level else 'T')
    if tier == 'silver':
        t.rect(4, 7, 11, 8, 'H')
        t.rect(4, 7, 11, 7, 'A')
        t.rect(4, 9, 11, 9, 'h')
    elif tier == 'busbar':
        for y in (6, 9):
            t.rect(4, y, 11, y + 1, 'R')
            t.rect(4, y, 11, y, 'e')
            t.set(5, y + 1, 'X')
            t.set(10, y + 1, 'X')
    else:
        for i in range(-2, 3):
            t.set(7 + i, 8, 'i')
            t.set(7, 8 + i, 'i')
        for d in (-2, 2):
            t.set(7 + d, 8 + d, 'i')
            t.set(7 + d, 8 - d, 'i')
        t.set(7, 8, 'j')
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
    'item/wrench': wrench_item,
    'item/dosimeter_ring': dosimeter_ring,
    'block/machine/module_lead_acid': module_front,
    'block/machine/module_case': module_case,
    'item/lead_acid_module': lead_acid_module_item,
    'item/home_battery': home_battery_item,
    'item/speed_module': speed_module_item,
    'item/geiger_counter': geiger_counter_item,
    'block/fitting_silver': lambda: fitting_block('silver'),
    'block/fitting_busbar': lambda: fitting_block('busbar'),
    'block/fitting_cryogenic': lambda: fitting_block('cryogenic'),
    'item/silver_fittings': lambda: fitting_item('silver'),
    'item/busbar_fittings': lambda: fitting_item('busbar'),
    'item/cryogenic_fittings': lambda: fitting_item('cryogenic'),
}

# Every pipe, energy cable included, from the one family. Their items show the block itself.
for _name in PIPES:
    TEXTURES['block/' + _name] = lambda n=_name: pipe_block(n)
    TEXTURES['block/' + _name + '_core'] = lambda n=_name: pipe_core(n)
    TEXTURES['block/' + _name + '_flange'] = lambda n=_name: pipe_flange(n)


def main():
    for name, fn in TEXTURES.items():
        publish(name, fn())


if __name__ == '__main__':
    main()
