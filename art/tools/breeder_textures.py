"""The breeder reactor's own textures (block/breeder/), for its concept (breeder_concept.py).

Drawn to the house style, and even: every texture is drawn for the shape it goes on, so each
surface shows it whole, never a plate cut at an edge or squeezed out of shape.
- Big faces are drawn for their panels: the sphere's welded plates (one latitude band high),
  the belt's girder (one panel between two legs), and the plinth's tread plate, a small repeating
  pattern that reads the same on every piece of its round floor.
- Narrow and round parts (the legs, the bracing, the handrail, the pipes) have textures that
  change only down their length, so they read the same however narrow the face.
- Bands shorter than a block (the plinth's lip) have a texture drawn at their height, band_<h>.
- Decals (the trefoil, the plaque, the bezel) are drawn at their exact size.
Fills get the soft surface finish (pixelart.publish_finished); lit textures stay crisp.

Run from the repo root:
    python art/tools/breeder_textures.py
"""
import math

from pixelart import *  # noqa: F401,F403
from quad_design import write_texture

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
def graphite():
    """The console's top: plain graphite, the finish its only pattern."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'S')
    return t


def fitting():
    """Small boxes and the undersides of rims: plain gunmetal, the finish its only pattern, so any
    size shows it evenly."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'u')
    return t


def concrete():
    """The footings and pads the reactor stands on: pale concrete, plain, so a pier or pad of any
    size shows it evenly (its lighting comes from the shading)."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    return t


def pump():
    """A sodium pump's motor casing: graphite with one cooling fin per panel (a lit edge, the fin's
    face and its shadow), a lit rim at the top and a dark foot."""
    t = Tex()
    cols = ['U', 'U', 'M', 'M', 'M', 'b', 'b', 'b', 'b', 'b', 'b', 'S', 'S', 'S', 'U', 'U']
    for x, k in enumerate(cols):
        t.rect(x, 0, x, 15, k)
    t.rect(0, 0, 15, 0, 'M')
    t.rect(0, 15, 15, 15, 'U')
    return t


def exchanger():
    """A heat exchanger's shell, a block high: light casing (lighter than the pumps, to tell them
    apart), lit at its top seam and shaded at its foot, the same all round."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'F')
    t.rect(0, 0, 15, 0, 'A')
    t.rect(0, 1, 15, 1, 'E')
    t.rect(0, 14, 15, 14, 'I')
    t.rect(0, 15, 15, 15, 'J')
    return t


def fan():
    """A pump motor's fan, 12 x 12, drawn once across the motor's top: a dark well, four swept
    blades and an orange hub."""
    t = Tex()
    for y in range(12):
        for x in range(12):
            dx, dy = x + 0.5 - 6, y + 0.5 - 6
            r = math.hypot(dx, dy)
            if r > 6:
                continue
            ang = math.degrees(math.atan2(dy, dx)) + r * 9
            k = 'K' if r > 5.2 else 'U'
            if 1.5 < r <= 5.2 and ang % 90 < 28:
                k = 'b' if ang % 90 < 20 else 'S'
            if r <= 1.8:
                k = 'X'
            t.set(x, y, k)
    return t


def plug():
    """The rotating plugs' top plates, seen through the lantern: lead grey, framed."""
    return plate('p', 'Q', 'q', 'P', 'D')


def lining():
    """The inside of the dome above the lantern: dark graphite plates, framed, so the room inside
    reads as a lit machine hall rather than a hollow shell."""
    return plate('T', 'b', 'S', 'U', 'K')


def hall_floor():
    """The lantern hall's floor, one plate per panel of its rings: dark graphite, plain, with a seam
    down one side and across the outer edge, so the rings read as clean radial plates rather than
    a busy grating squeezed into wedges."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'T')
    t.rect(0, 0, 0, 15, 'U')
    t.rect(1, 0, 1, 15, 'S')
    t.rect(0, 0, 15, 0, 'U')
    t.rect(0, 1, 15, 1, 'S')
    return t


def glass():
    """The lantern's glass: a faint, even cool tint with no sheen, so nothing draws streaks across
    the hall behind it. Written straight as colour and alpha (it is translucent)."""
    return [[(170, 215, 235, 56)] * 16 for _ in range(16)]


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
    """The sodium lines: clean copper, clamped every four pixels (a dark ring with a bright edge
    above it), the same all round, so a round pipe shows it evenly; its roundness comes from the
    shading."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'R')
    for y in range(0, 16, 4):
        t.rect(0, y, 15, y, 'r')
        t.rect(0, (y + 3) % 16, 15, (y + 3) % 16, 'e')
    return t


def leg():
    """A leg's upper part, a block high: painted steel, off-white to match the sphere, with a weld
    ring at the block's foot, the same all round."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    t.rect(0, 14, 15, 14, 'A')
    for x in range(16):
        t.set(x, 15, 'J' if x % 2 else 'I')
    return t


def fireproofing():
    """A leg's lower part, a block high: the grey fireproofing jacket real sphere legs carry near
    the ground (a concrete casing), lit at a chamfered joint at its top."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'L')
    t.rect(0, 0, 15, 0, 'h')
    t.rect(0, 1, 15, 1, 'M')
    return t


def louvre():
    """A console's sloping back: a louvred vent cover, graphite slats lit on top with dark gaps
    between, every 3 pixels down the slope and the same across, so a slope of any size shows it."""
    t = Tex()
    for y in range(16):
        t.rect(0, y, 15, y, ('M', 'S', 'U')[y % 3])
    return t


def back_wall(h):
    """A console's low back wall, `h` pixels high (drawn in rows 16 - h to 15, the rows a wall
    from the ground shows): a graphite plate per block with a lit bevel and a screw in each corner."""
    t = Tex()
    top = 16 - h
    t.rect(0, top, 15, 15, 'S')
    t.rect(0, top, 15, top, 'M')
    t.rect(0, top, 0, 15, 'M')
    t.rect(1, top + 1, 14, top + 1, 'b')
    t.rect(15, top, 15, 15, 'U')
    t.rect(0, 15, 15, 15, 'U')
    t.rect(1, 14, 14, 14, 'T')
    for x, y in ((2, top + 2), (13, top + 2), (2, 13), (13, 13)):
        t.set(x, y, 'l')
    return t


def profile_plate(poly, h):
    """A console's cheek, drawn at its shape: the profile `poly` (depth from -1, height) as a
    graphite plate h pixels high on a 16-wide canvas, a lit bevel along its top and front edges, a
    shaded one along its slope and back, and a screw near each corner."""
    t = Tex(16, h)
    inside = {}
    for py in range(h):
        for px in range(16):
            # A pixel the outline touches at all counts as inside, so the slanted edge has no
            # see-through notches where the face covers part of a pixel.
            inside[px, py] = any(_in_poly(poly, px + fx - 1, h - py - fy)
                                 for fx in (0.02, 0.5, 0.98) for fy in (0.02, 0.5, 0.98))
    for (px, py), on in inside.items():
        if not on:
            continue
        out = lambda qx, qy: not inside.get((qx, qy), False)
        if out(px, py - 1) or out(px - 1, py):
            c = 'M'
        elif out(px, py + 1) or out(px + 1, py):
            c = 'U'
        elif out(px, py - 2) or out(px - 2, py):
            c = 'b'
        elif out(px, py + 2) or out(px + 2, py):
            c = 'T'
        else:
            c = 'S'
        t.set(px, py, c)
    # Screws, kept three pixels in from the outline.
    for px, py in ((3, h - 4), (3, h - 15 if h > 18 else 4), (12, h - 4)):
        if all(inside.get((px + i, py + j), False) for i in (-3, 3) for j in (-3, 3)):
            t.set(px, py, 'l')
    return t


def _in_poly(poly, x, y):
    hit = False
    for (x1, y1), (x2, y2) in zip(poly, poly[1:] + poly[:1]):
        if (y1 > y) != (y2 > y) and x < x1 + (y - y1) * (x2 - x1) / (y2 - y1):
            hit = not hit
    return hit


def bund():
    """The bund wall's inner face, half a block high (drawn in rows 8 to 15, the rows a wall from
    the ground shows): a gunmetal plate per block with a lit coping edge, a seam at each block,
    a bolt either side of it, and a shadowed foot."""
    t = Tex()
    t.rect(0, 8, 15, 15, 'u')
    t.rect(0, 8, 15, 8, 'h')
    t.rect(0, 9, 15, 9, 's')
    t.rect(0, 9, 0, 15, 'z')
    t.rect(1, 10, 1, 14, 's')
    t.rect(0, 15, 15, 15, 'z')
    t.rect(0, 14, 15, 14, 'u')
    for x in (3, 13):
        t.set(x, 11, 'l')
        t.set(x, 12, 'z')
    return t


def hub():
    """The hub's sides, a block per panel: a gunmetal plate with a lit bevel and screws in its
    corners, a recessed band across its middle where the pipes come in."""
    t = plate('u', 'h', 's', 'z', 'D')
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        t.set(x, y, 'h')
        t.set(x + 1, y + 1, 'z')
    t.rect(2, 6, 13, 6, 'z')
    t.rect(2, 9, 13, 9, 's')
    return t


def steel_pipe():
    """The process lines from the consoles to the hub: off-white painted steel like the sphere and
    its legs, plain, so a round pipe shows it evenly; its roundness comes from the shading."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'E')
    return t


def paint(colour):
    """A colour band on a process line, in its port's ring colour (fuel orange, coolant blue,
    energy red): plain, so a band of any width shows it."""
    t = Tex()
    t.rect(0, 0, 15, 15, colour)
    return t


def amber():
    """The beacon's lamp, lit: warm amber, bright in the middle of each panel."""
    t = Tex()
    t.rect(0, 0, 15, 15, 'X')
    t.rect(5, 0, 10, 15, 'e')
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
    'fitting': fitting,
    'graphite': graphite,
    'rail': rail,
    'concrete': concrete,
    'pump': pump,
    'exchanger': exchanger,
    'fan': fan,
    'plug': plug,
    'lining': lining,
    'hall_floor': hall_floor,
    'grate': grate,
    'pipe': pipe,
    'leg': leg,
    'fireproofing': fireproofing,
    'louvre': louvre,
    'back_12': lambda: back_wall(12),
    'back_9': lambda: back_wall(9),
    # The cheeks' profiles, as breeder_concept.py draws them (SIDE_CHEEK and FRONT_CHEEK).
    'cheek_side': lambda: profile_plate([(-1, 0), (-1, 22), (6, 22), (14, 14), (14, 0)], 24),
    'cheek_front': lambda: profile_plate([(-1, 0), (-1, 15), (4, 15), (13, 9), (13, 0)], 16),
    'bund': bund,
    'hub': hub,
    'steel_pipe': steel_pipe,
    'paint_fuel': lambda: paint('X'),
    'paint_coolant': lambda: paint('v'),
    'paint_energy': lambda: paint('x'),
    'amber': amber,
    'trefoil': trefoil,
    'plaque': plaque,
    'bezel': bezel,
}
TEXTURES.update({f'band_{h}': (lambda h=h: band(h)) for h in BANDS})


def main():
    for name, fn in TEXTURES.items():
        publish_finished(FOLDER + name, fn())
    write_texture(FOLDER + 'glass', glass())


if __name__ == '__main__':
    main()
