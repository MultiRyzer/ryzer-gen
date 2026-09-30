"""The breeder reactor's own textures (block/breeder/), for its concept (breeder_concept.py).

Drawn to the house style, and even: every texture is drawn for the shape it goes on, so each
surface shows it whole, never a plate cut at an edge or squeezed out of shape.
- Big faces are drawn for their panels: the sphere's welded plates (one latitude band high),
  the belt's girder (one panel between two legs), and the plinth's tread plate, a small repeating
  pattern that reads the same on every piece of its round floor.
- Narrow parts (the legs, the bracing, the handrail) have textures that read the same however
  narrow the face.
- Bands shorter than a block (the plinth's lip) have a texture drawn at their height, band_<h>.
- Decals (the trefoil, the plaque, the bezel) are drawn at their exact size.
Fills get the soft surface finish (pixelart.publish_finished); lit textures stay crisp.

Run from the repo root:
    python art/tools/breeder_textures.py
"""
import math

from pixelart import *  # noqa: F401,F403

FOLDER = 'block/breeder/'


def plate(fill, lit, lit2, shade, shade2, w=16, h=16):
    """One framed plate, w x h: a two-step bevel (lit top and left, shaded foot and right) round a
    fill."""
    t = Tex()
    t.rect(0, 0, w - 1, h - 1, fill)
    t.rect(0, 0, w - 1, 0, lit)
    t.rect(0, 0, 0, h - 1, lit)
    t.rect(1, 1, w - 2, 1, lit2)
    t.rect(1, 1, 1, h - 2, lit2)
    t.rect(0, h - 1, w - 1, h - 1, shade2)
    t.rect(w - 1, 0, w - 1, h - 1, shade2)
    t.rect(1, h - 2, w - 2, h - 2, shade)
    t.rect(w - 2, 1, w - 2, h - 2, shade)
    return t


# ---------------------------------------------------------------- big faces: one plate per panel
def shell():
    """The sphere's plates: smooth painted steel, off-white as such spheres are painted, not the
    framed panel the machines use, so the sphere reads as one welded vessel. Each plate draws only
    its top and left seam, so every joint shows as one line: a weld bead (a ripple of darker
    pixels) with a highlight beside it where the bead catches the light."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    for i in range(16):
        bead = 'J' if i % 2 else 'I'
        t.set(i, 0, bead)
        t.set(0, i, bead)
    t.rect(1, 1, 15, 1, 'A')
    t.rect(1, 1, 1, 15, 'A')
    return t


def plinth():
    """The plinth's floor: steel tread plate, gunmetal with short raised bars in the alternating
    pattern industrial floors use, each lit on its upper edge and shadowed below. The pattern
    repeats every 8 pixels, so every piece of the round floor shows it evenly."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    for cy in range(4):
        for cx in range(4):
            x0, y0 = cx * 4, cy * 4
            if (cx + cy) % 2 == 0:
                t.set(x0 + 1, y0 + 2, 's')
                t.set(x0 + 2, y0 + 1, 's')
                t.set(x0 + 2, y0 + 2, 'z')
            else:
                t.set(x0 + 1, y0 + 1, 's')
                t.set(x0 + 2, y0 + 2, 's')
                t.set(x0 + 1, y0 + 2, 'z')
    return t


def girder():
    """The equator belt, a girder the legs hang from, drawn for one panel between two legs: a lit
    top flange and a shaded bottom one, each with a row of bolts, a darker web between, and a
    stiffener down the middle."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 0, 15, 2, 'b')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 13, 15, 15, 'b')
    t.rect(0, 15, 15, 15, 'U')
    t.rect(0, 3, 15, 3, 'T')
    t.rect(7, 3, 8, 12, 'b')
    t.rect(7, 3, 7, 12, 'M')
    for x in range(2, 16, 4):
        t.set(x, 1, 'L')
        t.set(x, 14, 'M')
    return t


# ---------------------------------------------------------------- narrow parts and bands
def rib():
    """A leg, a few pixels wide: lit edge, face, shadowed edge across it (so each face of the leg
    reads the same), and a joint at each block of height."""
    t = Tex()
    for x in range(16):
        t.rect(x, 0, x, 15, 'M' if x < 5 else 'b' if x < 11 else 'T')
    t.rect(0, 0, 15, 0, 'U')
    return t


def fitting():
    """Small boxes and the undersides of rims: plain gunmetal, the finish its only pattern, so any
    size shows it evenly."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    return t


def rail():
    """The platform's handrail and posts: safety yellow, plain, so any length shows it evenly."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'Y')
    return t


def grate():
    """The walkway's grating: dark graphite bars with light gaps between, the same on any piece
    however it is cut, so the ring of the walkway shows it evenly."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    for i in range(0, 16, 3):
        t.rect(i, 0, i, 15, 'U')
        t.rect(0, i, 15, i, 'U')
    return t


def pipe():
    """The sodium lines: clean copper, plain, so a round pipe of any length shows it evenly; its
    roundness comes from the shading."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'R')
    return t


def amber():
    """The beacon's lamp, lit: warm amber, bright in the middle of each panel."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'X')
    t.rect(5, 0, 10, 15, 'e')
    return t


def hatch():
    """The fuel hatch on the platform, 10 x 10: a gunmetal lid with a lit rim, a seam across it and
    an orange handle."""
    t = plate('s', 'h', 'L', 'u', 'z', 10, 10)
    t.rect(1, 5, 8, 5, 'z')
    t.rect(4, 2, 5, 2, 'X')
    return t


def band(h):
    """A graphite band `h` pixels high (a lip, a rim): lit along the top,
    shaded at the foot, the same all round. Drawn in rows 0 to h - 1."""
    t = Tex()
    t.rect(0, 0, 15, h - 1, 'b')
    t.rect(0, 0, 15, 0, 'M')
    if h > 2:
        t.rect(0, h - 1, 15, h - 1, 'T')
    return t


# ---------------------------------------------------------------- decals, at their exact size
def trefoil():
    """The radiation trefoil, 14 x 14 on a yellow plaque with a black rim: three blades round a
    disc, drawn by the standard's proportions (a centre disc of radius R, blades from 1.5R to 5R,
    60 degrees wide, one pointing down). Multiblocks have room for an authentic one."""
    t = Tex()
    t.rect(0, 0, 13, 13, 'B')
    t.rect(1, 1, 12, 12, 'Y')
    r = 1.1
    for y in range(1, 13):
        for x in range(1, 13):
            dx, dy = x + 0.5 - 7, y + 0.5 - 7
            dist = math.hypot(dx, dy)
            # Degrees round from straight up; the blades are centred on 60, 180 and 300.
            ang = math.degrees(math.atan2(dx, -dy)) % 120
            if dist <= r or (1.5 * r <= dist <= 5 * r and 30 <= ang <= 90):
                t.set(x, y, 'B')
    return t


def plaque():
    """The trefoil's graphite backing, 16 x 16: a framed plate."""
    return plate('S', 'M', 'b', 'T', 'U')


def bezel():
    """The graphite bezel round the control core's screen on the console, 14 x 12."""
    return plate('S', 'M', 'b', 'T', 'U', 14, 12)


BANDS = (5,)

TEXTURES = {
    'shell': shell,
    'plinth': plinth,
    'girder': girder,
    'rib': rib,
    'fitting': fitting,
    'rail': rail,
    'grate': grate,
    'pipe': pipe,
    'amber': amber,
    'hatch': hatch,
    'trefoil': trefoil,
    'plaque': plaque,
    'bezel': bezel,
}
TEXTURES.update({f'band_{h}': (lambda h=h: band(h)) for h in BANDS})


def main():
    for name, fn in TEXTURES.items():
        publish_finished(FOLDER + name, fn())


if __name__ == '__main__':
    main()
