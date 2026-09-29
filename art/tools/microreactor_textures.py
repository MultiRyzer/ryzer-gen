"""Generates the microreactor textures: the loose parts, and the materials and decals
used by the assembled 3D model. Preview the result with model_preview.py.

Pure Python, no PIL needed. Run from the repo root:
    python art/tools/microreactor_textures.py

The loose parts share a graphite frame with orange corner clips, in the same modern style as the
formed machine.
"""
import random

from pixelart import *  # noqa: F401,F403 (palette, canvas and house-style helpers)

# ---------------------------------------------------------------- loose parts
#
# The three blocks before the machine is assembled, in the same modern style as the formed machine.
# They share one frame: a graphite border with orange corner clips, like the clips on the formed
# skid. Each face then shows the part's role, reusing the formed machine's own panels: the heart is
# gunmetal with the porthole, the machine unit is light casing with the console, the coolant jacket
# has the sight glass, the copper fins and the coolant intake port.

def part_frame(fill, light, dark):
    t = Tex()
    t.rect(0, 0, 15, 15, fill)
    t.rect(1, 1, 14, 1, light)
    t.rect(1, 1, 1, 14, light)
    t.rect(1, 14, 14, 14, dark)
    t.rect(14, 1, 14, 14, dark)
    t.rect(0, 0, 15, 0, 'b')
    t.rect(0, 0, 0, 15, 'b')
    t.rect(0, 15, 15, 15, 'U')
    t.rect(15, 0, 15, 15, 'U')
    for cx, cy in ((0, 0), (14, 0), (0, 14), (14, 14)):
        t.set(cx, cy, 'e')
        t.set(cx + 1, cy, 'X')
        t.set(cx, cy + 1, 'X')
        t.set(cx + 1, cy + 1, 'Z')
    return t


def paste(t, src, x0, y0, w, h):
    """Copies the top-left w x h of another texture (a decal) onto this one."""
    for y in range(h):
        for x in range(w):
            if src.px[y][x]:
                t.px[y0 + y][x0 + x] = src.px[y][x]


def casing_part():
    return part_frame('F', 'A', 'I')


def vessel_part():
    return part_frame('s', 'J', 'u')


def heart_front():
    """The porthole onto the core, with a dim cyan status strip below."""
    t = vessel_part()
    paste(t, decal_porthole(), 3, 3, 10, 10)
    t.rect(5, 13, 10, 13, 'f')
    return t


def heart_side():
    """A pressure gauge above a graphite clamp band with its unlit cyan strip."""
    t = vessel_part()
    paste(t, decal_gauge(), 4, 2, 7, 7)
    t.rect(1, 10, 14, 10, 'b')
    t.rect(1, 11, 14, 11, 'S')
    t.rect(3, 11, 12, 11, 'f')
    t.rect(1, 12, 14, 12, 'T')
    return t


def heart_top():
    """The domed lid over the core, with its orange lifting lug."""
    t = vessel_part()
    t.stamp(3, 3, [
        '.JJJJJJJJ.',
        'JIIIIIIIIu',
        'JIssssssuu',
        'JIssssssuu',
        'JIsseXssuu',
        'JIssXZssuu',
        'JIssssssuu',
        'JIssssssuu',
        'Juuuuuuuuu',
        '.uuuuuuuu.',
    ])
    return t


def unit_front():
    """The console display above a vent grille."""
    t = casing_part()
    paste(t, decal_screen(), 3, 2, 10, 6)
    paste(t, decal_grille(), 4, 8, 8, 6)
    return t


def unit_side():
    """An inset access panel with a vent grille."""
    t = casing_part()
    t.rect(3, 3, 12, 3, 'I')
    t.rect(3, 3, 3, 12, 'I')
    t.rect(3, 12, 12, 12, 'E')
    t.rect(12, 3, 12, 12, 'E')
    paste(t, decal_grille(), 4, 5, 8, 6)
    return t


def unit_top():
    """A cooling fan behind a graphite grille."""
    t = casing_part()
    t.stamp(3, 3, [
        '..UUUUUU..',
        '.UTTTTTTU.',
        'UTTbTTbTTU',
        'UTTTbbTTTU',
        'UTbTJJTbTU',
        'UTbTJJTbTU',
        'UTTTbbTTTU',
        'UTTbTTbTTU',
        '.UTTTTTTU.',
        '..UUUUUU..',
    ])
    return t


def coolant_front():
    """A sight glass showing the water level, with graduation marks."""
    t = casing_part()
    t.rect(6, 2, 9, 13, 'U')
    t.rect(7, 3, 8, 6, 'T')
    t.rect(7, 7, 8, 12, 'v')
    t.rect(7, 7, 8, 7, 'V')
    t.rect(8, 8, 8, 12, 'w')
    for y in (4, 7, 10):
        t.set(10, y, 'J')
    t.rect(3, 3, 4, 3, 'i')
    return t


def coolant_side():
    """Copper radiator fins."""
    t = casing_part()
    for y in (3, 6, 9):
        t.rect(2, y, 13, y, 'e')
        t.rect(2, y + 1, 13, y + 1, 'R')
        t.rect(2, y + 2, 13, y + 2, 'O')
    return t


def coolant_top():
    """The coolant intake port, as on the formed machine."""
    t = casing_part()
    paste(t, decal_port_coolant(), 3, 3, 10, 10)
    return t


# ---------------------------------------------------------------- assembled microreactor
#
# The formed machine is a 3D box model (see MicroreactorModel in datagen). Its surfaces use small
# tiling materials, mapped by position so they run on across block joins, plus a few decals that
# sit in the top left of their texture at their real pixel size.
#
# Style: clean and modern (think Applied Energistics or Oritech, not Tekkit). Flat, noise-free
# panels with crisp one-pixel bevels, light casing against dark graphite trim, copper and orange
# accents, and cyan light strips that glow while the reactor runs. Each tile is one panel, so every
# block face reads as a neat plate with a fine seam at the block joins.

def panel(fill, light, dark, top=None, left=None):
    """A flat panel: one fill colour, light top and left edge, dark bottom and right edge."""
    t = Tex()
    t.rect(0, 0, 15, 15, fill)
    t.rect(0, 0, 15, 0, top or light)
    t.rect(0, 0, 0, 15, left or light)
    t.rect(0, 15, 15, 15, dark)
    t.rect(15, 0, 15, 15, dark)
    return t


def mat_steel():
    """Casing: light grey panel with a shallow recessed inset, like a machine housing."""
    t = panel('F', 'E', 'I', top='A')
    t.rect(3, 3, 12, 3, 'I')
    t.rect(3, 3, 3, 12, 'I')
    t.rect(3, 12, 12, 12, 'E')
    t.rect(12, 3, 12, 12, 'E')
    return t


def mat_steel_dark():
    """Trim: dark graphite for the skid, clips, clamp rings and port frames."""
    t = panel('S', 'b', 'U')
    t.rect(1, 1, 14, 1, 'T')
    return t


def mat_lead():
    """Vessel: mid gunmetal, a step lighter than the graphite trim so the flanges stand out, clad
    in vertical staves (a welded seam every four pixels, lit on one side), as a pressure vessel's
    plates are. Maps by position, so the staves run on round the column."""
    t = panel('s', 'J', 'u')
    for x in range(1, 16, 4):
        t.rect(x, 1, x, 14, 'z')
        t.rect(x + 1, 1, x + 1, 14, 'J')
    return t


def mat_copper():
    """Copper accents: clean and bright, with a soft highlight line."""
    t = panel('R', 'e', 'O')
    t.rect(1, 4, 14, 4, 'e')
    return t


def mat_accent():
    """Orange accent for small hardware: corner clips, the lifting lug."""
    t = panel('X', 'e', 'Z')
    t.rect(1, 1, 14, 1, 'a')
    return t


def mat_hazard():
    """Clean hazard stripes, yellow on graphite."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            t.set(x, y, 'Y' if (x + y) % 8 < 4 else 'T')
    return t


def mat_glow():
    """Cyan light strip, drawn emissive while the reactor runs."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'i')
    t.rect(0, 7, 15, 8, 'j')
    return t


def mat_glow_off():
    """The same strip, unlit."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'f')
    t.rect(0, 7, 15, 8, 'S')
    return t


def decal(rows, base='F'):
    t = Tex()
    t.rect(0, 0, 15, 15, base)
    t.stamp(0, 0, rows)
    return t


def decal_screen():
    """Console display: dark glass, a header bar, a trace and a readout."""
    return decal([
        'bbbbbbbbbb',
        'bUffffUUXb',
        'bUiUUiUUUb',
        'bUUiiUjiUb',
        'bUnUiiiiUb',
        'bbbbbbbbbb',
    ])


PORTHOLE = [
    'LbbbbbbbbL',
    'bTTTTTTTTU',
    'bTU1221UTU',
    'bT123321TU',
    'bT234432TU',
    'bT234432TU',
    'bT123321TU',
    'bTU1221UTU',
    'bTTTTTTTTU',
    'LUUUUUUUUL',
]


def decal_porthole():
    """Porthole with the reactor off: dark glass with a glint."""
    glass = str.maketrans({'1': 'U', '2': 'T', '3': 'S', '4': 'T'})
    t = decal([row.translate(glass) for row in PORTHOLE])
    t.set(3, 3, 'b')
    return t


def decal_porthole_on():
    """Porthole with the reactor running: Cherenkov blue. Drawn emissive in the model."""
    glow = str.maketrans({'1': 'f', '2': 'i', '3': 'j', '4': 'W'})
    return decal([row.translate(glow) for row in PORTHOLE])


# ---------------------------------------------------------------- fuel chain items

def item_basic_control_board():
    """Tier 1 circuit: a green board with copper traces, a graphite chip, an LED and gold contacts."""
    t = Tex()
    t.rect(1, 3, 14, 12, 'U')
    t.rect(2, 4, 13, 11, '6')
    t.rect(2, 4, 13, 4, '7')
    t.rect(2, 4, 2, 11, '7')
    t.rect(2, 11, 13, 11, '5')
    t.rect(13, 4, 13, 11, '5')
    # Copper traces into the chip.
    t.rect(3, 7, 5, 7, 'R')
    t.rect(3, 6, 3, 9, 'R')
    t.set(3, 6, 'e')
    t.rect(11, 8, 12, 8, 'R')
    t.rect(12, 8, 12, 10, 'R')
    t.rect(4, 10, 6, 10, 'R')
    # The chip, with its pins.
    t.rect(6, 6, 10, 9, 'T')
    t.rect(6, 6, 10, 6, 'b')
    t.set(6, 6, 'J')
    for x in (7, 9):
        t.set(x, 5, 'J')
        t.set(x, 10, 'J')
    # A status LED and a redstone dot.
    t.set(12, 5, 'n')
    t.set(4, 9, 'x')
    # Gold edge contacts.
    for x in (4, 6, 8, 10):
        t.set(x, 12, 'a')
    return t


CORE_ITEM = [
    '................',
    '.....KKKKKK.....',
    '....KQqqqqpK....',
    '....KPPPPPpK....',
    '...KKKKKKKKKK...',
    '...KhLLLLLLdK...',
    '...KhLlllllmK...',
    '...KhYBYBYBdK...',
    '...KhLlnnllmK...',
    '...KhLlnnllmK...',
    '...KhLlllllmK...',
    '...KhLLLLLLdK...',
    '...KKKKKKKKKK...',
    '....KPPPPPpK....',
    '....KqppppqK....',
    '.....KKKKKK.....',
]


def item_sealed_fuel_core():
    """Steel canister with lead end caps, a hazard band and a green status window."""
    t = Tex()
    t.stamp(0, 0, CORE_ITEM)
    return t


def item_depleted_fuel_core():
    """The same canister, spent: scorched, faded paint, status window dark."""
    swap = str.maketrans({'n': 'D', 'h': 'l', 'L': 'm', 'l': 'm', 'P': 'p', 'Q': 'q', 'Y': 'y'})
    t = Tex()
    t.stamp(0, 0, [row.translate(swap) for row in CORE_ITEM])
    for x, y in ((5, 6), (9, 10), (6, 11), (10, 5)):
        t.set(x, y, 'k')
    return t


def decal_port(ring):
    """A 10 x 10 port: graphite frame, a coloured ring saying what it carries, dark socket."""
    return decal([
        'bbbbbbbbbb',
        'bTTTTTTTTU',
        'bT' + ring * 6 + 'TU',
        'bT' + ring + 'UUUU' + ring + 'TU',
        'bT' + ring + 'USSU' + ring + 'TU',
        'bT' + ring + 'USSU' + ring + 'TU',
        'bT' + ring + 'UUUU' + ring + 'TU',
        'bT' + ring * 6 + 'TU',
        'bTTTTTTTTU',
        'UUUUUUUUUU',
    ])


def decal_port_energy():
    return decal_port('x')


def decal_port_coolant():
    return decal_port('i')


def decal_port_steam():
    return decal_port('A')


def decal_port_fuel():
    """Orange ring: items (fuel cores in and out)."""
    return decal_port('X')


def decal_gauge():
    """Pressure gauge: white face, graphite rim, orange needle. Drawn on the reactor heart's own
    side (the formed microreactor no longer carries gauges)."""
    return decal([
        'FTTTTTF',
        'TAAAAAT',
        'TAAAXAT',
        'TAAXAAT',
        'TAUAAAT',
        'TEAAAET',
        'FTTTTTF',
    ])


def decal_grille():
    """Vent grille: dark slats in a graphite frame."""
    return decal([
        'bbbbbbbb',
        'bUUUUUUT',
        'bSSSSSST',
        'bUUUUUUT',
        'bSSSSSST',
        'TTTTTTTT',
    ])


TEXTURES = {
    'block/reactor_heart_front': heart_front,
    'block/reactor_heart_side': heart_side,
    'block/reactor_heart_top': heart_top,
    'block/reactor_machine_unit_front': unit_front,
    'block/reactor_machine_unit_side': unit_side,
    'block/reactor_machine_unit_top': unit_top,
    'block/coolant_jacket_front': coolant_front,
    'block/coolant_jacket_side': coolant_side,
    'block/coolant_jacket_top': coolant_top,
    'block/microreactor/steel': mat_steel,
    'block/microreactor/steel_dark': mat_steel_dark,
    'block/microreactor/lead': mat_lead,
    'block/microreactor/copper': mat_copper,
    'block/microreactor/hazard': mat_hazard,
    'block/microreactor/glow': mat_glow,
    'block/microreactor/accent': mat_accent,
    'block/microreactor/glow_off': mat_glow_off,
    'block/microreactor/screen': decal_screen,
    'block/microreactor/porthole': decal_porthole,
    'block/microreactor/porthole_on': decal_porthole_on,
    'item/basic_control_board': item_basic_control_board,
    'item/sealed_fuel_core': item_sealed_fuel_core,
    'item/depleted_fuel_core': item_depleted_fuel_core,
    'block/microreactor/port_energy': decal_port_energy,
    'block/microreactor/port_coolant': decal_port_coolant,
    'block/microreactor/port_steam': decal_port_steam,
    'block/microreactor/port_fuel': decal_port_fuel,
    'block/microreactor/grille': decal_grille,
}


# Lit, glass and port textures stay crisp; the materials get the surface finish (pixelart.finish).
FINISHED = {'steel', 'steel_dark', 'lead', 'copper', 'accent'}


def main():
    for name, fn in TEXTURES.items():
        (publish_finished if name.rsplit('/', 1)[-1] in FINISHED and name.startswith('block/microreactor/')
         else publish)(name, fn())


if __name__ == '__main__':
    main()
