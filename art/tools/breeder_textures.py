"""The breeder reactor's own textures (block/breeder/), for its concept (breeder_concept.py).

Drawn to the house style: every panel is one framed plate per 16-pixel cell, lit along the top and
left and shaded at the foot and right, features centred on whole pixels, and fills given the soft
surface finish (pixelart.publish_finished) so no big face is flat colour. Lit textures stay crisp.
The breeder is the sealed, industrial twin of the station: a gunmetal reactor vessel, graphite
trim and decks, clean copper for the sodium loops, orange for small hardware.

Run from the repo root:
    python art/tools/breeder_textures.py
"""
from pixelart import *  # noqa: F401,F403

FOLDER = 'block/breeder/'


def plate(fill, lit, lit2, shade, shade2, inset=None):
    """One framed plate: a two-step bevel (lit top and left, shaded foot and right) round a fill,
    with a shallow inset line two pixels in if `inset` is given."""
    t = Tex()
    t.rect(0, 0, 15, 15, fill)
    t.rect(0, 0, 15, 0, lit)
    t.rect(0, 0, 0, 15, lit)
    t.rect(1, 1, 14, 1, lit2)
    t.rect(1, 1, 1, 14, lit2)
    t.rect(0, 15, 15, 15, shade2)
    t.rect(15, 0, 15, 15, shade2)
    t.rect(1, 14, 14, 14, shade)
    t.rect(14, 1, 14, 14, shade)
    if inset:
        t.rect(3, 3, 12, 3, inset)
        t.rect(3, 3, 3, 12, inset)
    return t


def vessel():
    """The guard vessel's plates: mid gunmetal, the reactor vessel colour of the house style,
    with a bolt head in each corner."""
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
    """The rotating plugs' top plates: lead grey, plain, so the lifting lugs and drives on them
    read."""
    return plate('p', 'Q', 'q', 'P', 'D')


def plug_side():
    """The plugs' edges: a thin graphite rim with a seam every panel, lit on top."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'b')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(15, 0, 15, 15, 'T')
    t.rect(0, 15, 15, 15, 'U')
    return t


def drive():
    """A control rod drive housing: bright steel, lit on its left, a seam per block of height."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 0, 3, 15, 'A')
    t.rect(12, 0, 15, 15, 'I')
    t.rect(0, 0, 15, 0, 'J')
    return t


def shaft():
    """The drive shaft that rises out of a drive as its rod is withdrawn: polished, with a mark
    every four pixels, so its movement reads."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'H')
    t.rect(0, 0, 5, 15, 'A')
    for y in range(0, 16, 4):
        t.rect(0, y, 15, y, 'L')
    return t


def pump():
    """A primary pump's motor casing: graphite, with upright cooling fins (lit and shaded edges
    every four pixels), as a big motor has."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    for x in range(0, 16, 4):
        t.rect(x, 0, x, 15, 'M')
        t.rect(x + 1, 0, x + 1, 15, 'b')
        t.rect(x + 3, 0, x + 3, 15, 'U')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 15, 15, 15, 'U')
    return t


def fan():
    """The pump motor's fan grille, seen from above (drawn whole on its top): a dark well behind
    four blades and a hub, turned by the renderer while sodium flows."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'U')
    t.rect(1, 1, 14, 14, 'K')
    for i in range(1, 7):
        t.set(8 + i, 8 - i // 2, 'b')
        t.set(7 - i, 7 + i // 2, 'b')
        t.set(8 + i // 2, 8 + i, 'b')
        t.set(7 - i // 2, 7 - i, 'b')
    t.rect(6, 6, 9, 9, 'S')
    t.rect(7, 7, 8, 8, 'X')
    return t


def exchanger():
    """An intermediate heat exchanger's shell: light casing plates, the lighter colour marking
    it apart from the pumps, with a flange band at the foot."""
    t = plate('F', 'A', 'E', 'I', 'J', inset='I')
    t.rect(1, 12, 14, 12, 'b')
    t.rect(1, 13, 14, 13, 'S')
    return t


def cap():
    """Tops of the drives, pumps and exchangers: gunmetal, lit on top."""
    t = plate('u', 's', 's', 'z', 'T')
    return t


def pipe():
    """The secondary sodium loops: clean copper, lit along one side, with a flange mark every
    block of length."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'R')
    t.rect(0, 0, 3, 15, 'e')
    t.rect(12, 0, 15, 15, 'r')
    t.rect(0, 0, 15, 1, 'O')
    return t


def plinth():
    """The plinth's band: graphite, lit along its lip, a groove, a seam per panel."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 1, 15, 1, 'b')
    t.rect(0, 8, 15, 8, 'U')
    t.rect(15, 0, 15, 15, 'T')
    t.rect(0, 15, 15, 15, 'U')
    return t


def lug():
    """Small orange hardware (lifting lugs): lit on top, a darker foot."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'X')
    t.rect(0, 0, 15, 0, 'e')
    t.rect(0, 15, 15, 15, 'Z')
    return t


def trefoil():
    """The radiation trefoil, 14 x 14 on a yellow plaque with a black rim: three blades round a
    disc, drawn by the standard's proportions (a centre disc of radius R, blades from 1.5R to 5R,
    60 degrees wide, one pointing down). Multiblocks have room for an authentic one."""
    import math
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


TEXTURES = {
    'vessel': vessel,
    'deck': deck,
    'plug': plug,
    'plug_side': plug_side,
    'drive': drive,
    'shaft': shaft,
    'pump': pump,
    'fan': fan,
    'exchanger': exchanger,
    'cap': cap,
    'pipe': pipe,
    'plinth': plinth,
    'lug': lug,
    'trefoil': trefoil,
}


def main():
    for name, fn in TEXTURES.items():
        publish_finished(FOLDER + name, fn())


if __name__ == '__main__':
    main()
