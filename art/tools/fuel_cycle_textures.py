"""Item textures for the fuel cycle (tier 2) that are drawn rather than mapped from vanilla: the
advanced control board and the fission fuel rods. Materials (silicon, plutonium, spent kernels,
fission waste) are in material_textures.py. Run: python art/tools/fuel_cycle_textures.py
"""
import math

from pixelart import *  # noqa: F401,F403


def advanced_control_board():
    """Tier 2 circuit: the basic board's layout on a blue board, with gold traces, a silicon chip
    and a cyan status light."""
    t = Tex()
    t.rect(1, 3, 14, 12, 'U')
    t.rect(2, 4, 13, 11, 'v')
    t.rect(2, 4, 13, 4, 'V')
    t.rect(2, 4, 2, 11, 'V')
    t.rect(2, 11, 13, 11, 'w')
    t.rect(13, 4, 13, 11, 'w')
    # Gold traces into the chip.
    t.rect(3, 7, 5, 7, 'a')
    t.rect(3, 6, 3, 9, 'a')
    t.rect(11, 8, 12, 8, 'a')
    t.rect(12, 8, 12, 10, 'a')
    t.rect(4, 10, 6, 10, 'a')
    # The silicon chip: blue-grey with a bright bevel, and its pins.
    t.rect(6, 6, 10, 9, 's')
    t.rect(6, 6, 10, 6, 'I')
    t.rect(6, 6, 6, 9, 'I')
    t.rect(7, 7, 9, 8, 'u')
    for x in (7, 9):
        t.set(x, 5, 'J')
        t.set(x, 10, 'J')
    # The status light and gold edge contacts.
    t.set(12, 5, 'i')
    for x in (4, 6, 8, 10):
        t.set(x, 12, 'a')
    return t


def fuel_rod(band_lit, band_dim, half=1.6):
    """A fission fuel rod, corner to corner: steel cladding lit from the top left, graphite end
    caps and two coloured rings that say which fuel is inside. `half` is half its thickness: the
    breeder's fuel assembly is a fatter steel duct round its pins."""
    x0, y0, x1, y1 = 2.5, 13.5, 13.5, 2.5
    length = math.hypot(x1 - x0, y1 - y0)
    dx, dy = (x1 - x0) / length, (y1 - y0) / length
    cells = {}
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - x0, y + 0.5 - y0
            along = (px * dx + py * dy) / length
            # Across the rod: negative on the top-left (lit) side.
            across = py * dx - px * dy
            if not -0.02 <= along <= 1.02 or abs(across) > half:
                continue
            if along < 0.1 or along > 0.9:
                key = 'b' if across < -0.5 * half / 1.6 else 'S' if across < 0.6 * half / 1.6 else 'T'
            elif 0.3 <= along < 0.37 or 0.63 <= along < 0.7:
                key = band_lit if across < 0.2 else band_dim
            else:
                k = half / 1.6
                key = 'A' if across < -0.9 * k else 'E' if across < -0.2 * k else 'F' if across < 0.5 * k else 'I'
            cells[(x, y)] = key
    t = Tex()
    for (x, y), key in cells.items():
        t.set(x, y, key)
    for y in range(16):
        for x in range(16):
            if (x, y) not in cells and any((x + ox, y + oy) in cells for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                t.set(x, y, 'U')
    return t


TEXTURES = {
    'item/advanced_control_board': advanced_control_board,
    # Uranium: the uranium green. MOX: the amber of plutonium's tarnish.
    'item/uranium_fuel_rod': lambda: fuel_rod('n', '6'),
    'item/mox_fuel_rod': lambda: fuel_rod('X', 'Z'),
    # Spent rods: the same rings, burnt dull.
    'item/spent_uranium_rod': lambda: fuel_rod('6', '5'),
    'item/spent_mox_rod': lambda: fuel_rod('Z', '8'),
    # Lithium target rod: pale lithium rings; bred, they glow violet with tritium.
    'item/lithium_target_rod': lambda: fuel_rod('A', 'I'),
    'item/irradiated_target_rod': lambda: fuel_rod('4', '3'),
    # Control rod: the brake, ringed in warning red.
    'item/control_rod': lambda: fuel_rod('x', '8'),
    # Breeder fuel: a fat fast reactor assembly, ringed in the breeder's cyan.
    'item/breeder_fuel': lambda: fuel_rod('i', 'f', half=2.3),
    'item/spent_breeder_fuel': lambda: fuel_rod('f', 'D', half=2.3),
    # The breeder's uranium blanket: uranium green on the fat assembly; bred, plutonium blue.
    'item/uranium_blanket': lambda: fuel_rod('n', '6', half=2.3),
    'item/bred_uranium_blanket': lambda: fuel_rod('G', 'g', half=2.3),
}


def main():
    for name, fn in TEXTURES.items():
        publish(name, fn())


if __name__ == '__main__':
    main()
