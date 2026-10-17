"""The Alloy Smelter's own textures, for its model (alloySmelterModel in datagen).

Tier 1, before any power: a small fuel-fired furnace, as early blast furnaces were. It is a plain
block, as vanilla's furnaces are, since nothing about it is high tech: firebrick bound in a
gunmetal frame, with a riveted steel band round its middle. Each face has one job:
  front    the fire door in a steel surround, with a draft damper above it;
  sides    banded brickwork (the back too: pipes meet any face);
  top      a steel plate with the copper flue ring and its grate;
  bottom   plain graphite.
The fire door sits a pixel back in its own box (the firebox), so only it glows while the fire
burns, not the brick round it.

Every face is 16 x 16, symmetric about its centre: the frame is one pixel, the courses three
(two of brick, one of mortar), two above the band and two below.

Run from the repo root:
    python art/tools/alloy_smelter_textures.py
"""
from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/alloy_smelter/'
# The door's opening on the front, in texture pixels: 8 x 5, centred across, below the band.
DOOR_X, DOOR_Y = 4, 8


def frame(t):
    """The gunmetal frame round a face: lit along its top and left, shaded along its foot and right."""
    t.rect(0, 0, 15, 0, 's')
    t.rect(0, 0, 0, 15, 's')
    t.rect(0, 15, 15, 15, 'z')
    t.rect(15, 0, 15, 15, 'z')
    t.set(15, 0, 'u')
    t.set(0, 15, 'u')


# Where the vertical joints fall across the 14-pixel field, mirrored about its centre where a
# running bond allows: a whole brick between two halves, then two bricks meeting in the middle.
JOINTS = [(4, 11), (7,)]


def bricks(t, y0, courses):
    """Courses of firebrick from row {y0}, three rows each (two of brick, one of dark mortar), the
    joints alternating course to course. Each brick takes one of three tones (fired brick varies),
    lit along its top."""
    tones = [('R', 'r'), ('r', 'O'), ('R', 'O')]   # lit top, body
    for c in range(courses):
        joints = JOINTS[c % 2]
        for row in range(3):
            y = y0 + c * 3 + row
            for x in range(1, 15):
                if row == 2 or x in joints:
                    t.set(x, y, 'o')
                    continue
                brick = sum(1 for j in joints if x > j)
                lit, body = tones[(brick + c * 2) % len(tones)]
                t.set(x, y, lit if row == 0 else body)


def band(t):
    """The steel band round the middle, rows 7 and 8, with an orange rivet each side."""
    t.rect(1, 7, 14, 7, 's')
    t.rect(1, 8, 14, 8, 'u')
    for x in (3, 12):
        t.set(x, 7, 'X')
        t.set(x, 8, 'Z')


def side():
    t = Tex()
    bricks(t, 1, 2)
    band(t)
    bricks(t, 9, 2)
    frame(t)
    return t


def front():
    t = side()
    # The draft damper above the door: a steel plate with its slot.
    t.rect(5, 2, 10, 4, 'u')
    t.rect(5, 2, 10, 2, 's')
    t.rect(5, 4, 10, 4, 'z')
    t.rect(6, 3, 9, 3, 'U')
    # The door's surround: a steel frame a pixel round the opening, over the band.
    x0, y0, x1, y1 = DOOR_X - 1, DOOR_Y - 1, DOOR_X + 8, DOOR_Y + 5
    t.rect(x0, y0, x1, y1, 'u')
    t.rect(x0, y0, x1, y0, 's')
    t.rect(x0, y0, x0, y1, 's')
    t.rect(x0, y1, x1, y1, 'z')
    t.rect(x1, y0 + 1, x1, y1, 'z')
    # The latch, an orange lug on the surround's head.
    t.rect(7, y0, 8, y0, 'X')
    # The opening itself is the firebox's face, a pixel back; here it is only dark.
    t.rect(DOOR_X, DOOR_Y, DOOR_X + 7, DOOR_Y + 4, 'U')
    return t


def fire_door(lit, frame_no=0):
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
            if (x * 3 + frame_no * 5) % 7 < 2:
                t.set(x, 1, 'a')
        t.set(1 + (frame_no * 3) % 6, 3, 'W')
    else:
        t.set(3, 3, 'O')
    for bar in (2, 5):
        t.rect(bar, 1, bar, 3, 'u')
    return t


def firebox(lit, frame_no=0):
    """The firebox's face, seen only through the door's opening: the fire door where the opening is."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'U')
    door = fire_door(lit, frame_no)
    for y in range(5):
        for x in range(8):
            t.px[DOOR_Y + y][DOOR_X + x] = door.px[y][x]
    return t


def top():
    t = Tex()
    t.rect(1, 1, 14, 14, 'u')
    # A shallow inset round the plate, as the house casing has.
    t.rect(2, 2, 13, 2, 'z')
    t.rect(2, 2, 2, 13, 'z')
    t.rect(2, 13, 13, 13, 's')
    t.rect(13, 3, 13, 13, 's')
    # The copper flue ring, 6 x 6 in the middle, lit top left, with its grate: two bars over the dark.
    t.rect(5, 5, 10, 10, 'r')
    t.rect(5, 5, 10, 5, 'R')
    t.rect(5, 5, 5, 10, 'R')
    t.rect(5, 10, 10, 10, 'O')
    t.rect(10, 6, 10, 10, 'O')
    t.rect(6, 6, 9, 9, 'U')
    t.rect(6, 7, 9, 8, 'T')
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.set(x, y, 'X')
    frame(t)
    return t


def bottom():
    t = Tex()
    t.rect(1, 1, 14, 14, 'T')
    frame(t)
    return t


TEXTURES = {
    'front': front,
    'side': side,
    'top': top,
    'bottom': bottom,
    'firebox': lambda: firebox(False),
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        publish_finished(FOLDER + name, fn())
    # Lit while it burns, and flickering; lit textures stay crisp.
    publish_frames(FOLDER + 'firebox_on', [firebox(True, i) for i in range(7)], 2, True)


if __name__ == '__main__':
    main()
