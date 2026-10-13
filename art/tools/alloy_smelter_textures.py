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
    """Firebrick laid in courses four pixels high, each brick eight long and offset course to course,
    in recessed dark mortar. Each brick takes one of three tones (as fired brick varies), lit along
    its top and shaded along its foot, so the wall reads as built of separate bricks."""
    tones = [('R', 'r', 'O'), ('r', 'O', 'o'), ('R', 'r', 'r')]   # lit top, body, shaded foot
    t = Tex()
    for y in range(16):
        course = y // 4
        shift = 4 if course % 2 else 0
        for x in range(16):
            if y % 4 == 3 or (x + shift) % 8 == 7:
                t.set(x, y, 'o')
                continue
            brick = ((x + shift) // 8 + course * 3) % len(tones)
            lit, body, foot = tones[brick]
            t.set(x, y, lit if y % 4 == 0 else foot if y % 4 == 2 else body)
    return t


def fire_door(lit, frame=0):
    """The fire door, 8 x 5: a gunmetal frame (lit along its top and left, shaded along its foot and
    right) round a grate of two bars. Burning, the fire shows between the bars, orange at the top to
    white hot on the bed; out, the firebox is dark with a last ember or two."""
    t = Tex()
    t.rect(0, 0, 7, 4, 'u')
    t.rect(0, 0, 7, 0, 's')
    t.rect(0, 0, 0, 4, 's')
    t.rect(7, 1, 7, 4, 'z')
    t.rect(1, 4, 7, 4, 'z')
    rows = ['X', 'a', 'e'] if lit else ['U', 'U', 'T']
    for i, colour in enumerate(rows):
        t.rect(1, 1 + i, 6, 1 + i, colour)
    if lit:
        # The fire flickers: flame tips licking up into the top row, and a white-hot spot on the bed.
        for x in range(1, 7):
            if (x * 3 + frame * 5) % 7 < 2:
                t.set(x, 1, 'a')
        t.set(1 + (frame * 3) % 6, 3, 'W')
    if not lit:
        t.set(3, 3, 'O')
    for bar in (2, 5):
        t.rect(bar, 1, bar, 3, 'u')
    return t


TEXTURES = {
    'firebrick': firebrick,
    'fire_door': lambda: fire_door(False),
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        # Lit textures stay crisp; the rest get the surface finish (pixelart.finish).
        (publish if name.endswith('_on') else publish_finished)(FOLDER + name, fn())
    # Lit while it burns, and flickering.
    publish_frames(FOLDER + 'fire_door_on', [fire_door(True, i) for i in range(7)], 2, True)


if __name__ == '__main__':
    main()
