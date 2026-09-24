"""Textures for the fission station's parts, as they look before the station forms (then the core
draws the whole station from the design in fission_concept.py). Run:
    python art/tools/station_textures.py
"""
import math

from pixelart import *  # noqa: F401,F403


def panel(t, fill='F', light='A', shade='I', rim='S'):
    """A clean casing panel: a graphite rim, then a crisp one-pixel bevel round a flat face."""
    t.rect(0, 0, 15, 15, rim)
    t.rect(1, 1, 14, 14, fill)
    t.rect(1, 1, 14, 1, light)
    t.rect(1, 1, 1, 14, light)
    t.rect(1, 14, 14, 14, shade)
    t.rect(14, 1, 14, 14, shade)
    return t


def station_casing():
    """Light casing with a graphite rim; a small orange clip in one corner marks it as station kit."""
    t = panel(Tex())
    t.rect(3, 3, 12, 3, 'E')
    t.rect(3, 12, 12, 12, 'J')
    t.set(12, 12, 'X')
    t.set(11, 12, 'Z')
    return t


def station_glass():
    """Reinforced glass: a graphite frame round a clear pane with glare streaks (drawn as cutout)."""
    t = Tex()
    t.rect(0, 0, 15, 0, 'b')
    t.rect(0, 0, 0, 15, 'b')
    t.rect(0, 15, 15, 15, 'T')
    t.rect(15, 0, 15, 15, 'T')
    t.rect(1, 1, 14, 1, 'S')
    t.rect(1, 1, 1, 14, 'S')
    t.rect(1, 14, 14, 14, 'S')
    t.rect(14, 1, 14, 14, 'S')
    for y in range(2, 14):
        for x in range(2, 14):
            if (x + y) % 11 in (0, 1):
                t.set(x, y, 'j' if (x + y) % 11 == 0 else 'A')
    return t


def turbine_rotor_top():
    """The rotor seen from above: a gunmetal hub and eight pitched steel blades on a dark ring."""
    t = panel(Tex(), fill='T', light='S', shade='U', rim='S')
    for k in range(8):
        a = math.radians(k * 45 + 10)
        for step in range(2, 7):
            x = int(round(7.5 + step * math.cos(a)))
            y = int(round(7.5 + step * math.sin(a)))
            t.set(x, y, 'E' if step < 5 else 'F')
            t.set(int(round(7.5 + step * math.cos(a + 0.2))), int(round(7.5 + step * math.sin(a + 0.2))), 'I')
    t.rect(6, 6, 9, 9, 'u')
    t.rect(6, 6, 9, 6, 's')
    t.set(7, 7, 'j')
    return t


def turbine_rotor_side():
    """The rotor's housing: graphite, with a copper generator band round the middle."""
    t = panel(Tex(), fill='S', light='b', shade='T', rim='T')
    t.rect(1, 6, 14, 6, 'e')
    t.rect(1, 7, 14, 8, 'R')
    t.rect(1, 9, 14, 9, 'Z')
    return t


def station_core_front():
    """The control core's face: a readout screen in a gunmetal bezel, a status light below."""
    t = panel(Tex())
    t.rect(3, 3, 12, 10, 'z')
    t.rect(4, 4, 11, 9, 'U')
    for y, width in ((5, 6), (7, 4)):
        t.rect(5, y, 4 + width, y, 'i')
    t.set(10, 8, 'j')
    t.rect(4, 12, 5, 12, 'n')
    t.rect(7, 12, 11, 12, 'J')
    return t


def creative_top():
    """The creative blocks' top: dark, with a magenta diamond, the usual mark of creative-only kit."""
    t = panel(Tex(), fill='T', light='S', shade='U', rim='U')
    for i in range(5):
        t.rect(7 - i, 3 + i, 8 + i, 3 + i, '3')
        t.rect(7 - i, 12 - i, 8 + i, 12 - i, '3')
    t.rect(6, 6, 9, 9, '4')
    return t


def creative_side(port):
    """A creative block's side: dark casing, a magenta band, and the port ring of what it gives."""
    t = panel(Tex(), fill='S', light='b', shade='T', rim='U')
    t.rect(1, 2, 14, 2, '3')
    t.rect(1, 13, 14, 13, '3')
    t.rect(4, 4, 11, 11, 'U')
    t.rect(5, 5, 10, 10, port)
    t.rect(6, 6, 9, 9, 'T')
    return t


TEXTURES = {
    'block/station_casing': station_casing,
    'block/station_glass': station_glass,
    'block/turbine_rotor_top': turbine_rotor_top,
    'block/turbine_rotor_side': turbine_rotor_side,
    'block/station_core_front': station_core_front,
    'block/creative_top': creative_top,
    'block/creative_battery_side': lambda: creative_side('x'),
    'block/creative_water_tank_side': lambda: creative_side('i'),
}


def main():
    for name, fn in TEXTURES.items():
        publish(name, fn())


if __name__ == '__main__':
    main()
