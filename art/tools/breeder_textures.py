"""The breeder reactor's own textures (block/breeder/), for its concept (breeder_concept.py).

Drawn to the house style, and even: every texture is drawn for the shape it goes on, so each
surface shows it whole, never a plate cut at an edge or squeezed out of shape.
- Big faces (the sphere, the belt, the deck, the plinth) are one framed plate per panel: lit
  along the top and left, shaded at the foot and right. The concept sizes those panels to match:
  each of the sphere's latitude bands is one plate high, and every ring is cut into equal widths.
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
    """The sphere's plates: light casing, as spheres like it are painted, a framed plate per
    panel with a shallow inset, lit along the top and left."""
    t = plate('F', 'A', 'E', 'I', 'J')
    t.rect(3, 3, 12, 3, 'I')
    t.rect(3, 3, 3, 12, 'I')
    return t


def deck():
    """The platform's deck plates: graphite, with a row of grating slots across the middle."""
    t = plate('S', 'M', 'b', 'T', 'U')
    for x in range(4, 12, 2):
        t.rect(x, 6, x, 9, 'U')
    return t


def plinth():
    """The plinth's top plates: graphite floor, framed."""
    return plate('S', 'M', 'b', 'T', 'U')


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
    'deck': deck,
    'plinth': plinth,
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
