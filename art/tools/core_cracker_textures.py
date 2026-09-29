"""The Core Cracker's own textures, for its model (coreCrackerModel in datagen).

A jaw crusher, as used to crack hard ceramics: a heavy ribbed frame, a feed hopper on top, a big
flywheel either side (the flywheel stores the energy that drives the jaws through each stroke), and
a discharge chute at the front where the cracked kernels drop out. Frame bands, chute and fittings
use the machine family's textures (machine_family.py).

Run from the repo root:
    python art/tools/core_cracker_textures.py
"""
import math

from pixelart import *  # noqa: F401,F403
import machine_family

FOLDER = 'block/machine/core_cracker/'


def crusher():
    """The crusher's frame: heavy gunmetal cast with vertical stiffening ribs every four pixels."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    for x in range(1, 16, 4):
        t.rect(x, 0, x, 15, 's')
        t.rect(x + 1, 0, x + 1, 15, 'z')
    t.rect(0, 0, 15, 0, 's')
    t.rect(0, 15, 15, 15, 'z')
    return t


def frame_side():
    """A side of the crusher's cast frame, 10 x 9, drawn at its size: a dark outline, a lit bevel
    inside it, a stiffening rib down the middle and a bolt in each corner."""
    t = Tex()
    w, h = 10, 9
    t.rect(0, 0, w - 1, h - 1, 'u')
    t.rect(0, 0, w - 1, 0, 'z')
    t.rect(0, 0, 0, h - 1, 'z')
    t.rect(w - 1, 0, w - 1, h - 1, 'T')
    t.rect(0, h - 1, w - 1, h - 1, 'T')
    t.rect(1, 1, w - 2, 1, 's')
    t.rect(1, 1, 1, h - 2, 's')
    t.rect(w - 2, 2, w - 2, h - 2, 'z')
    t.rect(2, h - 2, w - 2, h - 2, 'z')
    t.rect(4, 2, 4, h - 3, 's')
    t.rect(5, 2, 5, h - 3, 'z')
    for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
        t.set(x, y, 'L')
    return t


def chute():
    """The discharge chute's front, 6 x 3: a lit steel lip round the dark slot the kernels drop from."""
    t = Tex()
    t.rect(0, 0, 5, 2, 'u')
    t.rect(0, 0, 5, 0, 's')
    t.rect(1, 1, 4, 2, 'U')
    t.rect(1, 1, 4, 1, 'T')
    return t


def flywheel():
    """A flywheel seen side on. Its faces map by position, and the wheel spans z 3 to 13 and y 2.5
    to 12.5, so it is drawn round (8, 8.5) there: a gunmetal rim with eight bolts, four spokes lit
    along their top edges, dark between them, and an orange hub round a dark socket. It turns with
    the wheel (CoreCrackerRenderer)."""
    t = Tex()
    cx, cy = 8, 8.5
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if r > 3.9:
                k = 's' if dy < -1 else 'u'
            elif r < 0.8:
                k = 'U'
            elif r < 1.6:
                k = 'X'
            elif abs(dx) < 0.7:
                k = 's' if dx < 0 else 'u'
            elif abs(dy) < 0.7:
                k = 's' if dy < 0 else 'u'
            else:
                k = 'T'
            t.set(x, y, k)
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        t.set(int(cx + 4.4 * math.cos(a)), int(cy + 4.4 * math.sin(a)), 'L')
    return t


def hopper_mouth():
    """The feed hopper's opening, seen from above: dark inside a lit steel lip."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    t.rect(4, 4, 11, 11, 'U')
    t.rect(4, 4, 11, 4, 'T')
    return t


TEXTURES = {
    'crusher': crusher,
    'frame_side': frame_side,
    'chute': chute,
    'flywheel': flywheel,
    'hopper_mouth': hopper_mouth,
}


def main():
    machine_family.main()
    for name, fn in TEXTURES.items():
        publish_finished(FOLDER + name, fn())


if __name__ == '__main__':
    main()
