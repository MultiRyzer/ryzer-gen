"""The breeder reactor's own textures (block/breeder/), for its concept (breeder_concept.py).

Drawn to the house style, and even: every texture is drawn for the shape it goes on, so each
surface shows it whole, never a plate cut at an edge or squeezed out of shape.
- Big faces (the tank, the deck, the plinth) are one framed plate per panel: lit along the top
  and left, shaded at the foot and right. The concept sizes those panels to match: the tank's
  seams sit under its ribs, and every ring is cut into equal widths.
- Narrow parts (the ribs) have textures that read the same however narrow the face.
- Bands shorter than a block (the plinth's lip, the roof's rim) have a texture drawn at their
  height, band_<h>.
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
def vessel():
    """The guard vessel's plates, one per rib gap: mid gunmetal, the reactor vessel colour of the
    house style, with a bolt head in each corner."""
    t = plate('s', 'h', 'L', 'u', 'z')
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        t.set(x, y, 'H')
    return t


def deck():
    """The roof slab's deck plates: graphite, with a row of grating slots across the middle."""
    t = plate('S', 'M', 'b', 'T', 'U')
    for x in range(4, 12, 2):
        t.rect(x, 6, x, 9, 'U')
    return t


def plinth():
    """The plinth's top plates: graphite floor, framed."""
    return plate('S', 'M', 'b', 'T', 'U')


# ---------------------------------------------------------------- narrow parts and bands
def rib():
    """An upright rib on the tank, a few pixels wide: lit edge, face, shadowed edge across it (so
    each face of the rib reads the same), and a joint at each block of height."""
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


BANDS = (4, 5, 8)

TEXTURES = {
    'vessel': vessel,
    'deck': deck,
    'plinth': plinth,
    'rib': rib,
    'fitting': fitting,
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
