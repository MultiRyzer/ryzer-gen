"""The Alloy Smelter's own textures, for its model (alloySmelterModel in datagen).

Tier 1, before any power: a small fuel-fired furnace, as early blast furnaces were. A firebrick
hearth bound with steel bands, a fire door that glows while it burns, and a copper hood and flue on
top. The only brick machine, so it reads as the earliest. Bands, hood and flue use the machine
family's gunmetal and copper (machine_family.py).

Run from the repo root:
    python art/tools/alloy_smelter_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/alloy_smelter/'


def firebrick():
    """Firebrick, laid in courses four pixels high, each brick eight long and offset course to
    course, with pale mortar joints. Lit along each brick's top edge, clean and even."""
    t = Tex()
    for y in range(16):
        course = y // 4
        for x in range(16):
            joint_x = (x + (4 if course % 2 else 0)) % 8 == 7
            if y % 4 == 3 or joint_x:
                k = 'J'
            elif y % 4 == 0:
                k = 'R'
            else:
                k = 'r'
            t.set(x, y, k)
    return t


def fire_door(lit):
    """The fire door, 7 x 4: a gunmetal frame round the firebox opening. Burning, the fire shows
    through it, orange to white at its heart; out, the firebox is dark."""
    t = Tex()
    t.rect(0, 0, 6, 3, 'u')
    t.rect(0, 0, 6, 0, 's')
    if lit:
        t.rect(1, 1, 5, 2, 'X')
        t.rect(2, 2, 4, 2, 'a')
        t.set(3, 2, 'e')
    else:
        t.rect(1, 1, 5, 2, 'U')
    return t


TEXTURES = {
    'firebrick': firebrick,
    'fire_door': lambda: fire_door(False),
    'fire_door_on': lambda: fire_door(True),
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        # Lit textures stay crisp; the rest get the surface finish (pixelart.finish).
        (publish if name.endswith('_on') else publish_finished)(FOLDER + name, fn())


if __name__ == '__main__':
    main()
