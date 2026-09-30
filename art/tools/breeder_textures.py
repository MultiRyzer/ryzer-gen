"""The breeder reactor's own textures (block/breeder/), for its concept (breeder_concept.py).

Drawn to the house style, and even: every texture is drawn for the shape it goes on, so each
surface shows it whole, never a plate cut at an edge or squeezed out of shape.
- Big faces (the tank, the deck, the plugs, the plinth) are one framed plate per panel: lit along
  the top and left, shaded at the foot and right. The concept sizes those panels to match: the
  tank's seams sit under its ribs, and every ring is cut into equal widths.
- Small round parts (drives, pumps, exchangers) have panels only a few pixels wide, so their
  textures change only down their height (rings and seams), or repeat once per panel (a pump's
  fins), and read the same however narrow the panel.
- Bands shorter than a block (rims, flanges) have a texture drawn at their height, band_<h>.
- Decals (the fan, the trefoil, the plaque, the bezel) are drawn at their exact size.
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


def plug():
    """The rotating plugs' top plates: lead grey, plain, so the lugs and drives on them read."""
    return plate('p', 'Q', 'q', 'P', 'D')


def plinth():
    """The plinth's top plates: graphite floor, framed."""
    return plate('S', 'M', 'b', 'T', 'U')


# ---------------------------------------------------------------- small round parts: even however narrow the panel
def rib():
    """An upright rib on the tank, a few pixels wide: lit edge, face, shadowed edge across it (so
    each face of the rib reads the same), and a joint at each block of height."""
    t = Tex()
    for x in range(16):
        t.rect(x, 0, x, 15, 'M' if x < 5 else 'b' if x < 11 else 'T')
    t.rect(0, 0, 15, 0, 'U')
    return t


def drive():
    """A control rod drive housing: bright steel, a seam ring at its top and foot, and a
    maker's band a third of the way up."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 1, 15, 1, 'J')
    t.rect(0, 10, 15, 10, 'I')
    t.rect(0, 11, 15, 11, 'A')
    t.rect(0, 15, 15, 15, 'J')
    return t


def shaft():
    """The drive shaft that rises as its rod is withdrawn: polished, with a mark every two
    pixels of height, so its movement reads."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'H')
    for y in range(0, 16, 2):
        t.rect(0, y, 15, y, 'L')
    return t


def pump():
    """A primary pump's motor casing: graphite with one cooling fin per panel (a lit edge, the
    fin's face and its shadow), a lit rim at the top and a dark foot."""
    t = Tex()
    cols = ['U', 'U', 'M', 'M', 'M', 'b', 'b', 'b', 'b', 'b', 'b', 'S', 'S', 'S', 'U', 'U']
    for x, k in enumerate(cols):
        t.rect(x, 0, x, 15, k)
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 15, 15, 15, 'U')
    return t


def exchanger():
    """An intermediate heat exchanger's shell, a block high per band: light casing (lighter than
    the pumps, to tell them apart), lit at its top seam and shaded at its foot."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 1, 15, 1, 'E')
    t.rect(0, 14, 15, 14, 'I')
    t.rect(0, 15, 15, 15, 'J')
    return t


def fitting():
    """Flanges, collars, caps and the lugs' bases: plain gunmetal, the finish its only pattern,
    so any size of box or disc shows it evenly."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    return t


def pipe():
    """The secondary sodium loops: clean copper, plain, so a round pipe of any length and any bend
    shows it evenly; its roundness comes from the shading."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'R')
    return t


def graphite():
    """The pads that seat the roof's machinery, and the plinth's pipe pads: plain graphite, the
    deck's colour, so they read as part of the roof however small a wedge they are cut into."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    return t


def lug():
    """Small orange hardware (lifting lugs, the handling machine's frame): plain orange."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'X')
    return t


def band(h):
    """A graphite band `h` pixels high (a rim, a plug's edge, a flange): lit along the top,
    shaded at the foot, the same all round. Drawn in rows 0 to h - 1."""
    t = Tex()
    t.rect(0, 0, 15, h - 1, 'b')
    t.rect(0, 0, 15, 0, 'M')
    if h > 2:
        t.rect(0, h - 1, 15, h - 1, 'T')
    return t


# ---------------------------------------------------------------- decals, at their exact size
def fan():
    """A pump motor's fan, 12 x 12, drawn once across the motor's top: a dark well, four swept
    blades and an orange hub. Turned by the renderer while sodium flows."""
    t = Tex()
    for y in range(12):
        for x in range(12):
            dx, dy = x + 0.5 - 6, y + 0.5 - 6
            r = math.hypot(dx, dy)
            if r > 6:
                continue
            ang = math.degrees(math.atan2(dy, dx)) + r * 9       # blades sweep back as they go out
            k = 'K' if r > 5.2 else 'U'
            if 1.5 < r <= 5.2 and ang % 90 < 28:
                k = 'b' if ang % 90 < 20 else 'S'
            if r <= 1.8:
                k = 'X'
            t.set(x, y, k)
    return t


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


BANDS = (2, 4, 5, 8)

TEXTURES = {
    'vessel': vessel,
    'deck': deck,
    'plug': plug,
    'plinth': plinth,
    'rib': rib,
    'drive': drive,
    'shaft': shaft,
    'pump': pump,
    'exchanger': exchanger,
    'fitting': fitting,
    'pipe': pipe,
    'graphite': graphite,
    'lug': lug,
    'fan': fan,
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
