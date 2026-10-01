"""Concept design of the breeder reactor (design section 9), before the real multiblock exists.

A sodium-cooled fast reactor in a steel sphere on legs, as the Dounreay Fast Reactor was housed:
the core sits in a pool of liquid sodium inside, sealed from the air (sodium burns in it). The
shape is the refinery's Horton sphere: a sphere of welded plates standing on a ring of legs with
cross bracing, a girder belt at the equator where the legs meet it, and a railed platform on top.
Drawn as smooth quads (quad_design.py) on its own textures (breeder_textures.py). Run:
    python art/tools/breeder_concept.py [OUT.png]

Sizes in blocks: 9 across (centred on the middle of block 4, 4), about 8 high. The first design
(a sphere on a flat plinth) is kept as the git tag breeder-concept-v1, render art/concepts/breeder_v1.png.

Even textures: every surface shows its texture whole. The sphere's radius makes each latitude band
exactly one plate high; the bands have 16 plates, halving to 8 nearest the poles, so every plate is
near 16 pixels wide and every corner meets its neighbours'. The belt's panels meet at the legs,
heights are whole blocks, and decals (the trefoil, its plaque, the bezel, the fans) are drawn at
their exact size.

What it shows, bottom to top:
- No plinth: the legs stand straight on the ground, inside a compound: a slab-high
  bund wall with hazard stripes and a short fence joins the three consoles, and under the sphere
  a pipe runs from the back of each port to a square hub, from which a thick flanged pipe rises
  into the sphere. Real sodium plants are a maze of loops like this.
- Three consoles on the ground, a block deep, six of the legs standing on their tops: the
  station's front panel with the control core's screen, between hazard-striped cheeks; as you
  face it, the inputs on your left side (fuel in, liquid sodium in) and the outputs on your right
  (the output port for spent fuel and what the blanket bred, energy out), each side under a
  sloping hood with a lit tag above each port and hazard-striped cheeks, as the station's are.
- The sphere: light casing plates in latitude bands, a graphite belt at the equator with a cyan
  light strip and the radiation trefoil, a grated walkway round it reached by a ladder that climbs
  beside a leg, bracketed to it.
- The lantern: a ring of glass round the upper sphere, framed by mullions, showing the machine hall
  inside, as real sodium reactors keep their refuelling machinery under a dome: a floor, the
  rotating plugs with the control rod drives standing in the middle of the hall, lit bands round
  them, and the fuel handling cask on the large plug, under a dark lined dome.
- The platform on top: a yellow handrail, a cyan light strip and an amber beacon.

Moving parts, each drawn in its own group so a renderer can move it (design section 9): the beacon,
turning while the reactor runs; and inside the lantern the rotating plugs (plug_large, and
plug_small with the drives), which will turn while fuel goes in or out.
"""
import contextlib
import math
import os
import sys

import model_preview as mp
import big_machine_textures
import breeder_textures
from quad_design import Design

B = 16
C = 4.5 * B            # centre of the footprint, the middle of block (4, 4)
PLINTH_R = 62          # the plinth; its lip (2 more) stays behind the console's front
BANDS = 10             # latitude bands, pole to pole
R = BANDS * B / math.pi   # the sphere's radius: each band's arc is exactly one plate (16 pixels)
EQUATOR = 72           # the equator's height, in the middle of the belt
BELT = (EQUATOR - 8, EQUATOR + 8)      # the girder belt, a block high
LEGS = 12
BELT_R = (R + 1) / math.cos(math.pi / LEGS)   # the belt's corners: flat panels whose middles clear the sphere
LEG_R = BELT_R + 2     # the legs' circle, on the belt's corners
PHASE = math.radians(360 / LEGS / 2)   # legs sit on the belt's seams, so the front (north) is mid-panel

TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in (
    'hazard', 'glow', 'screen', 'port_energy', 'port_fuel', 'port_coolant')})
TEXTURES.update({name: 'ryzergen:block/station/' + name for name in (
    'housing', 'housing_slope', 'housing_cheek', 'hazard_upright', 'tag_coolant', 'tag_fuel', 'tag_energy')})
TEXTURES['glass'] = 'ryzergen:block/breeder/glass'
TEXTURES['breeder_hazard'] = 'ryzergen:block/microreactor/hazard'
TEXTURES['front_panel'] = 'ryzergen:block/breeder/front_panel'
d = Design(C, TEXTURES, {'glow', 'screen', 'breeder_amber', 'tag_coolant', 'tag_fuel', 'tag_energy'})


@contextlib.contextmanager
def centred(x, z):
    """Draws round a centre other than the machine's (for the pipes and the beacon)."""
    whole = d.at
    d.at = lambda phi, rad, y: (x + rad * math.cos(phi), y, z + rad * math.sin(phi))
    try:
        yield
    finally:
        d.at = whole


@contextlib.contextmanager
def group(name):
    d.group = name
    try:
        yield
    finally:
        d.group = 'static'


def surface_r(y):
    """The sphere's radius from the axis at height y (0 above or below it)."""
    dy = y - EQUATOR
    return math.sqrt(max(0.0, R * R - dy * dy))


def rail(points, closed=False):
    """A yellow handrail along `points`, a pixel thick."""
    d.sweep('breeder_rail', points, 0.5, 0.5, lambda i: (0, 1, 0), closed=closed, caps=not closed)


def decal_disc(mat, cx, cz, y, r, size, n=16):
    """A flat round top with a decal drawn once across it at its exact size (`size` pixels,
    centred), not once per wedge."""
    half = size / 2
    for k in range(n):
        p0, p1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        pts = [(cx, y, cz), (cx, y, cz), (cx + r * math.cos(p1), y, cz + r * math.sin(p1)), (cx + r * math.cos(p0), y, cz + r * math.sin(p0))]
        d.quad(mat, pts, [(half + (q[0] - cx), half + (q[2] - cz)) for q in pts], (0, 1, 0))


def arc(rad, y, a0, a1, steps):
    """Points round the machine's axis at radius `rad` and height `y`, from angle a0 to a1."""
    return [d.at(a0 + (a1 - a0) * k / steps, rad, y) for k in range(steps + 1)]


def band(y, h, r, n=None):
    """A graphite band `h` high (the plinth's lip), on its own band texture."""
    d.cylinder(f'breeder_band_{h}', y, y + h, r, n=n, v0=0)


def flat_ring(mat, y, r1, r2, n, n_in):
    """A flat ring of `n` panels facing up, from r1 to r2, its inner edge cut into n_in pieces (n,
    or n / 2 where the ring inside has half as many panels). Where n_in is half, every other inner
    corner sits at the middle of the inner ring's straight edge, so the rings join with no gap."""
    step = 2 * math.pi / n
    for k in range(n):
        a0, a1 = k * step, (k + 1) * step

        def inner(angle, index):
            if n_in == n or index % 2 == 0 or r1 == 0:
                return d.at(angle, r1, y)
            before, after = d.at(angle - step, r1, y), d.at(angle + step, r1, y)
            return tuple((before[i] + after[i]) / 2 for i in range(3))

        pts = [inner(a0, k), d.at(a0, r2, y), d.at(a1, r2, y), inner(a1, k + 1)]
        d.quad(mat, pts, [(0, 16), (0, 0), (16, 0), (16, 16)], (0, 1, 0))


def plates(mat, y1, y2, r, n=LEGS, phase=PHASE, v0=None):
    """Like a cylinder, but with `n` panels starting at `phase`: the belt's panels meet at the
    legs."""
    h = y2 - y1
    top = 16 - min(16, h) if v0 is None else v0
    for k in range(n):
        p0, p1 = phase + 2 * math.pi * k / n, phase + 2 * math.pi * (k + 1) / n
        pm = (p0 + p1) / 2
        d.quad(mat, [d.at(p0, r, y2), d.at(p0, r, y1), d.at(p1, r, y1), d.at(p1, r, y2)],
               [(0, top), (0, top + min(16, h)), (16, top + min(16, h)), (16, top)], (math.cos(pm), 0, math.sin(pm)))


# ---------------------------------------------------------------- consoles, standing on the ground
# No plinth: the legs reach the ground, between and behind the consoles. As the
# station has them, the front (north) carries the control core's screen, and the ports go on
# consoles on the two sides. As you face the front (looking south), your left is east (larger x):
# the inputs go there (fuel in, liquid sodium in), and the outputs on the west (the output port for
# spent fuel and what the blanket bred, energy out), never on one shared port. Each side console is
# three cells on the grid, so its panels show whole.
FACE = 1.5                      # each console's face, as the station's: just inside the block's edge,
                                # its port flanges flush with the edge, where a pipe meets them
DEPTH = 14                      # shallow, so the legs behind them reach the ground
FRONT_DEPTH = 13
CELLS = (3 * B, 6 * B)          # the middle three cells along each side
TAGS = {'port_coolant': 'tag_coolant', 'port_fuel': 'tag_fuel', 'port_energy': 'tag_energy'}
HOOD_BACK, HOOD_TOP = FACE + 5, 20     # the hood slopes from the face's top edge back to here,
                                       # clear of the legs that stand behind it


# Each console is a profile (depth in from its face, height) drawn along its length: the face, a
# short top, then its back sloping down to a low back wall, so it reads as a desk, not a block. The
# slope is a louvred vent; the back wall a framed plate per block. The cheeks either end follow the
# profile, standing proud of it, each side one plate drawn at its shape (breeder/cheek_*).
SIDE_PROFILE = [(0, 0), (0, 15), (HOOD_BACK - FACE, HOOD_TOP), (DEPTH, 12), (DEPTH, 0)]
SIDE_CHEEK = [(-1, 0), (-1, HOOD_TOP + 2), (HOOD_BACK - FACE + 1, HOOD_TOP + 2), (DEPTH, 14), (DEPTH, 0)]
FRONT_PROFILE = [(0, 0), (0, 15), (4, 15), (FRONT_DEPTH, 9), (FRONT_DEPTH, 0)]
FRONT_CHEEK = [(-1, 0), (-1, 15), (4, 15), (FRONT_DEPTH, 9), (FRONT_DEPTH, 0)]


def prism(poly, a1, a2, place, faces, cap=None, cap_h=16):
    """A profile `poly` (points (depth, y)) drawn from a1 to a2 along a console. `place(d, y, a)`
    gives the 3D point. `faces[i]` is the texture of the edge from point i to i + 1 and how it is
    mapped ('rows' for a wall standing on the ground, v = 16 - y; 'len' for v down the edge), or
    None to leave it out. Faces are cut a block apart from a1, so a panel shows whole on each
    block. `cap` is a decal drawn at the profile's shape on both ends, `cap_h` pixels high."""
    cd = sum(q[0] for q in poly) / len(poly)
    cy = sum(q[1] for q in poly) / len(poly)
    cuts = [a1 + 16 * k for k in range(int((a2 - a1 - 0.01) // 16) + 1)] + [a2]
    for i, spec in enumerate(faces):
        if spec is None:
            continue
        tex, mode = spec
        (d1, y1), (d2, y2) = poly[i], poly[(i + 1) % len(poly)]
        length = math.dist((d1, y1), (d2, y2))
        for u1, u2 in zip(cuts, cuts[1:]):
            am = (u1 + u2) / 2
            mid = place((d1 + d2) / 2, (y1 + y2) / 2, am)
            inner = place(cd, cy, am)
            out = tuple(m - c for m, c in zip(mid, inner))
            if mode == 'rows':
                va, vb = 16 - y1, 16 - y2
            else:
                va, vb = 0, min(16, length)
            d.quad(tex, [place(d1, y1, u1), place(d2, y2, u1), place(d2, y2, u2), place(d1, y1, u2)],
                   [(0, va), (0, vb), (u2 - u1, vb), (u2 - u1, va)], out)
    if cap:
        k = 16 / cap_h
        for at, sign in ((a1, -1), (a2, 1)):
            pts = [place(dd, y, at) for dd, y in poly]
            uvs = [(dd + 1, (cap_h - y) * k) for dd, y in poly]
            out = tuple(sign * (q - r) for q, r in zip(place(0, 0, a2), place(0, 0, a1)))
            for i in range(1, len(poly) - 1, 2):
                idx = [0, i, i + 1, min(i + 2, len(poly) - 1)]
                d.quad(cap, [pts[j] for j in idx], [uvs[j] for j in idx], out)


def console(side, ports):
    """A console on `side` ('east' or 'west'), three cells long; `ports` gives what sits in each
    cell (a port decal, or None for a plain panel). Over it, as over the station's ports, a hood
    slopes back with a lit tag above each port, between cheeks hazard striped on the front; behind
    the hood its back slopes down to a low wall."""
    a1, a2 = CELLS
    east = side == 'east'
    back = 'west' if east else 'east'

    def x(v):
        """A distance in from the footprint's edge on this side, as world x."""
        return 2 * C - v if east else v

    def place(dd, y, a):
        return (x(FACE + dd), y, a)

    prism(SIDE_PROFILE, a1, a2, place,
          [None, None, ('breeder_louvre', 'len'), ('breeder_back_12', 'rows'), None])
    # The face holding the ports is the station's light port housing, so the ports read the same.
    x1, x2 = sorted((x(FACE), x(FACE + DEPTH)))
    d.box('housing', x1, 0, a1, x2, 15, a2, skip=tuple(f for f in ('north', 'south', 'east', 'west', 'up', 'down') if f != side))
    sx = 1 if east else -1
    d.quad('housing_slope', [(x(HOOD_BACK), HOOD_TOP, a2), (x(FACE), 15, a2), (x(FACE), 15, a1), (x(HOOD_BACK), HOOD_TOP, a1)],
           [(0, 0), (0, 16), (16, 16), (16, 0)], (sx * (HOOD_TOP - 15), HOOD_BACK - FACE, 0))
    # Cheeks either end, standing proud and a little taller, hazard striped on the front.
    for z1, z2 in ((a1 - 3, a1), (a2, a2 + 3)):
        prism(SIDE_CHEEK, z1, z2, place, [None, ('breeder_graphite', 'len'), ('breeder_graphite', 'len'),
                                          ('breeder_graphite', 'rows'), None], cap='breeder_cheek_side', cap_h=24)
        sx1, sx2 = sorted((x(FACE - 1), x(FACE - 1.5)))
        d.box('hazard_upright', sx1, 0, z1, sx2, HOOD_TOP + 2, z2, skip=('down', back))
    for cell, kind in enumerate(ports):
        if kind is None:
            continue
        a = a1 + cell * B
        px1, px2 = sorted((x(FACE), x(0)))
        d.box('breeder_fitting', px1, 3, a + 3, px2, 13, a + 13, decals={side: kind}, skip=(back,))
        # A slim light bar on the hood above the port, in its ring colour.
        t0, t1 = 0.25, 0.45
        xa, ya = FACE + (HOOD_BACK - FACE) * t1, 15 + (HOOD_TOP - 15) * t1
        xb, yb = FACE + (HOOD_BACK - FACE) * t0, 15 + (HOOD_TOP - 15) * t0
        lift = 0.3
        d.quad(TAGS[kind], [(x(xa) + sx * lift, ya + lift, a + 14), (x(xb) + sx * lift, yb + lift, a + 14),
                            (x(xb) + sx * lift, yb + lift, a + 2), (x(xa) + sx * lift, ya + lift, a + 2)],
               [(0, 0), (0, 3), (12, 3), (12, 0)], (sx * (HOOD_TOP - 15), HOOD_BACK - FACE, 0))


# The front: the station's front panel, so the two reactors share a face. Two blocks wide, centred,
# its face one texture drawn once across both (breeder/front_panel, the station's without the hazard
# band at its foot, which the compound wall carries; 32 x 16, rows 1 to 15), with the control
# core's screen standing proud of it in the bezel the panel draws. Behind a short top its back
# slopes down like the side consoles'.
def front_place(dd, y, a):
    return (a, y, FACE + dd)


prism(FRONT_PROFILE, C - B, C + B, front_place,
      [None, ('breeder_graphite', 'len'), ('breeder_louvre', 'len'), ('breeder_back_9', 'rows'), None])
# Seen from the front the viewer's left is the east (+x) end, so u runs from x = C + 16 to C - 16.
d.quad('front_panel', [(C + B, 15, FACE), (C - B, 15, FACE), (C - B, 0, FACE), (C + B, 0, FACE)],
       [(0, 1), (16, 1), (16, 16), (0, 16)], (0, 0, -1))
d.box('breeder_fitting', C - 5, 5, FACE - 1, C + 5, 11, FACE - 0.5, decals={'north': 'screen'}, skip=('south',))
d.box('breeder_fitting', C - 6, 4, FACE - 0.5, C + 6, 12, FACE, skip=('south',))
# Cheeks either end of the front panel, following its profile, hazard striped on the front like the
# side consoles' cheeks.
for x1, x2 in ((C - B - 4, C - B), (C + B, C + B + 4)):
    prism(FRONT_CHEEK, x1, x2, front_place, [None, ('breeder_graphite', 'len'), ('breeder_graphite', 'len'),
                                            ('breeder_graphite', 'rows'), None], cap='breeder_cheek_front')
    d.box('hazard_upright', x1, 0, FACE - 1.5, x2, 15, FACE - 1, skip=('down', 'south'))
console('east', ['port_fuel', None, 'port_coolant'])
console('west', ['port_fuel', None, 'port_energy'])

# ---------------------------------------------------------------- the sphere, in latitude bands
def lat_point(lat):
    """(height, radius from the axis) of the sphere at latitude `lat` (radians)."""
    return EQUATOR + R * math.sin(lat), R * math.cos(lat)


STEP = math.pi / BANDS
# Plates per band, pole to pole (the caps apart): 16 round the middle bands and 8 in the band
# nearest each pole, so every plate is within about a fifth of 16 pixels wide. The counts only ever
# halve, so where they do each wide plate is drawn as two halves meeting the narrow plates' corners
# exactly, and no edge is left open.
PLATES = [8, 16, 16, 16, 16, 16, 16, 8]
EDGES = [8] + [max(a, b) for a, b in zip(PLATES, PLATES[1:])] + [8]


def sphere_band(lo, hi, n, n_lo, n_hi, mat='breeder_shell', inward=False):
    """One latitude band of `n` plates between latitudes lo and hi, its lower edge cut into n_lo
    pieces and its upper into n_hi (each n or 2n), so it meets its neighbours corner to corner.
    `inward` draws it facing the sphere's middle (the lantern's glass seen from inside, the dome's
    lining)."""
    (y1, r1), (y2, r2) = lat_point(lo), lat_point(hi)
    for k in range(n):
        a0, a1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        split = 2 if max(n_lo, n_hi) > n else 1
        for part in range(split):
            b0, b1 = a0 + (a1 - a0) * part / split, a0 + (a1 - a0) * (part + 1) / split

            def corner(angle, y, r, pieces):
                # On an edge cut into as many pieces as plates, the half-way point lies on the
                # straight edge between the plate's corners, not out on the circle.
                if pieces == n and split == 2 and part == 0 and angle == b1:
                    return tuple((d.at(a0, r, y)[i] + d.at(a1, r, y)[i]) / 2 for i in range(3))
                if pieces == n and split == 2 and part == 1 and angle == b0:
                    return tuple((d.at(a0, r, y)[i] + d.at(a1, r, y)[i]) / 2 for i in range(3))
                return d.at(angle, r, y)

            pts = [corner(b0, y2, r2, n_hi), corner(b0, y1, r1, n_lo), corner(b1, y1, r1, n_lo), corner(b1, y2, r2, n_hi)]
            u0, u1 = 16 * part / split, 16 * (part + 1) / split
            pm = (b0 + b1) / 2
            dy, dr = y2 - y1, r2 - r1
            outward = (dy * math.cos(pm), -dr, dy * math.sin(pm))
            if inward:
                outward = tuple(-v for v in outward)
            d.quad(mat, pts, [(u0, 0), (u0, 16), (u1, 16), (u1, 0)], outward)


LANTERN = (6, 7)       # the bands from 18 to 54 degrees up: glass, just above the walkway's rail
for k in range(1, BANDS - 1):          # the two polar caps are drawn apart
    lo, hi = -math.pi / 2 + k * STEP, -math.pi / 2 + (k + 1) * STEP
    if k in LANTERN:
        # Glass, drawn translucent, outside and in, so the far side tints the view too.
        with group('glass'):
            sphere_band(lo, hi, PLATES[k - 1], EDGES[k - 1], EDGES[k], mat='glass')
            sphere_band(lo, hi, PLATES[k - 1], EDGES[k - 1], EDGES[k], mat='glass', inward=True)
        continue
    sphere_band(lo, hi, PLATES[k - 1], EDGES[k - 1], EDGES[k])
    if k > LANTERN[-1]:
        # The dome over the lantern, lined inside, so the hall seen through the glass has a ceiling.
        sphere_band(lo, hi, PLATES[k - 1], EDGES[k - 1], EDGES[k], mat='breeder_lining', inward=True)
# The caps, cut to meet the polar bands: under the platform on top, and round the drain at the foot.
TOP_Y, TOP_R = lat_point(math.pi / 2 - STEP)
FOOT_Y, FOOT_R = lat_point(-math.pi / 2 + STEP)
d.lathe('breeder_fitting', [(TOP_Y, TOP_R), (EQUATOR + R, 0)], n=8)
d.lathe('breeder_lining', [(TOP_Y, TOP_R), (EQUATOR + R, 0)], n=8, inward=True)
d.lathe('breeder_fitting', [(EQUATOR - R, 0), (FOOT_Y, FOOT_R)], n=8)

# ---------------------------------------------------------------- the girder belt round the equator
belt_r = BELT_R
plates('breeder_girder', BELT[0], BELT[1], belt_r, n=LEGS)
for y, up in ((BELT[1], True), (BELT[0], False)):
    inner = lat_point(math.asin((y - EQUATOR) / R))[1]
    d.annulus('breeder_fitting', y, inner, belt_r, up=up, n=LEGS * 2)
plates('glow', EQUATOR - 0.5, EQUATOR + 0.5, belt_r + 0.1, n=LEGS)
# The trefoil: a 14 x 14 plaque on a 16 x 16 graphite backing, on the belt's front panel.
PZ = C - belt_r - 1
d.box('breeder_fitting', C - 8, BELT[0], PZ, C + 8, BELT[1], C - belt_r + 3, decals={'north': 'breeder_plaque'}, skip=('south',))
d.box('breeder_fitting', C - 7, BELT[0] + 1, PZ - 0.5, C + 7, BELT[1] - 1, PZ, decals={'north': 'breeder_trefoil'}, skip=('south',))

# ---------------------------------------------------------------- legs and bracing
LEG_TOP = BELT[1]
for k in range(LEGS):
    phi = PHASE + 2 * math.pi * k / LEGS
    # Round legs, as a sphere's are: fireproofing up to the bracing's middle, painted steel above.
    lx, _, lz = d.at(phi, LEG_R, 0)
    with centred(lx, lz):
        for y in range(0, 48, B):
            d.cylinder('breeder_fireproofing', y, y + B, 2.5, n=8)
        for y in range(48, LEG_TOP, B):
            d.cylinder('breeder_leg', y, y + B, 2.2, n=8)
        d.annulus('breeder_fitting', 48, 2.2, 2.5, n=8)
# Cross bracing in each bay below the belt, thin rods corner to corner; the front bay (north, 270
# degrees) is left open, so the console and the trefoil stay clear.
BRACE = (20, BELT[0] - 4)
for k in range(LEGS):
    a, b = PHASE + 2 * math.pi * k / LEGS, PHASE + 2 * math.pi * (k + 1) / LEGS
    if abs(math.degrees((a + b) / 2) % 360 - 270) < 1:
        continue
    mid = (a + b) / 2
    across = (math.cos(mid), 0, math.sin(mid))
    for (ya, yb) in ((BRACE[0], BRACE[1]), (BRACE[1], BRACE[0])):
        path = [d.at(a, LEG_R, ya), d.at(b, LEG_R, yb)]
        d.sweep('breeder_fitting', path, 0.5, 0.5, lambda i, v=across: v, closed=False, caps=True)
# ---------------------------------------------------------------- the compound: a bund wall, ducts and the central riser
# A slab-high wall round the footprint's edge joins the three consoles into one compound, as sodium
# plants bund their equipment (spilled sodium burns, so it is caught). Graphite with hazard stripes
# on its outer face and a short yellow fence on top, whose rail meets the consoles' tops. From the
# back of each port a pipe runs in to a square hub in the middle, and from the hub a thick flanged
# pipe rises into the sphere's foot: what the ports take in and give out goes this way.
WALL_H = 8
WALL_T = 4
E0, E1 = FACE, 2 * C - FACE          # the wall's outer faces, in line with the consoles' faces
FRONT_ENDS = (C - B - 4, C + B + 4)  # the front console with its cheeks
SIDE_ENDS = (CELLS[0] - 3, CELLS[1] + 3)   # the side consoles with their cheeks


def wall(x1, z1, x2, z2):
    """A straight piece of wall. Every face on the compound's edge is hazard striped, round the
    corners too, so the stripe runs all the way round."""
    edge = [f for f, on in (('west', x1 == E0), ('east', x2 == E1), ('north', z1 == E0), ('south', z2 == E1)) if on]
    d.box('breeder_bund', x1, 0, z1, x2, WALL_H, z2, top='breeder_fitting', skip=('down', *edge))
    o = 0.5
    # Each overlay reaches round a corner by its own thickness, so two meeting there close it.
    ex1 = x1 - o if 'west' in edge else x1
    ex2 = x2 + o if 'east' in edge else x2
    ez1 = z1 - o if 'north' in edge else z1
    ez2 = z2 + o if 'south' in edge else z2
    for f in edge:
        box = {'north': (ex1, 0, z1 - o, ex2, WALL_H, z1), 'south': (ex1, 0, z2, ex2, WALL_H, z2 + o),
               'west': (x1 - o, 0, ez1, x1, WALL_H, ez2), 'east': (x2, 0, ez1, x2 + o, WALL_H, ez2)}[f]
        d.box('breeder_hazard', *box, skip=('down',))


def fence(path):
    """The fence along the middle of the wall's top: one rail along `path` (round its corners in
    one piece, so it joins), with posts at each end, each corner and about a block apart between."""
    posts = []
    for (ax, az), (bx, bz) in zip(path, path[1:]):
        steps = max(1, round(math.dist((ax, az), (bx, bz)) / B))
        posts += [(ax + (bx - ax) * k / steps, az + (bz - az) * k / steps) for k in range(steps)]
    posts.append(path[-1])
    for px, pz in posts:
        d.box('breeder_rail', px - 0.5, WALL_H, pz - 0.5, px + 0.5, 14, pz + 0.5, skip=('down',))
    d.sweep('breeder_rail', [(x, 14.5, z) for x, z in path], 0.5, 0.5, lambda i: (0, 1, 0), closed=False, caps=True)


# Front, either side of the front console; the back, whole; and the two sides, before and after
# their consoles. The front pieces reach the corners, so the sides start behind them.
M0, M1 = E0 + WALL_T / 2, E1 - WALL_T / 2       # the wall's middle line, where the fence stands
wall(E0, E0, FRONT_ENDS[0], E0 + WALL_T)
wall(FRONT_ENDS[1], E0, E1, E0 + WALL_T)
wall(E0, E1 - WALL_T, E1, E1)
for x1, x2 in ((E0, E0 + WALL_T), (E1 - WALL_T, E1)):
    wall(x1, E0 + WALL_T, x2, SIDE_ENDS[0])
    wall(x1, SIDE_ENDS[1], x2, E1 - WALL_T)
# Three fences, each from one console's cheek round the corners to the next.
fence([(FRONT_ENDS[0], M0), (M0, M0), (M0, SIDE_ENDS[0])])
fence([(FRONT_ENDS[1], M0), (M1, M0), (M1, SIDE_ENDS[0])])
fence([(M0, SIDE_ENDS[1]), (M0, M1), (M1, M1), (M1, SIDE_ENDS[1])])

# The hub in the middle: a square block, three blocks across and one high, on the grid, so the
# pipes meet its faces flush. From its top a thick flanged pipe rises into the sphere's foot.
HUB = (C - 1.5 * B, C + 1.5 * B)
HUB_H = B
RISER_R = 6
FOOT = EQUATOR - R
d.box('breeder_hub', HUB[0], 0, HUB[0], HUB[1], HUB_H, HUB[1], top='breeder_plinth', skip=('down',))
d.cylinder('breeder_steel_pipe', HUB_H, FOOT + 1, RISER_R, n=12)
d.cylinder('breeder_fitting', HUB_H, HUB_H + 2, RISER_R + 1.5, n=12)
d.annulus('breeder_fitting', HUB_H + 2, RISER_R, RISER_R + 1.5, n=12)


def hpipe(mat, a1, a2, y, c, r, along_x, n=8):
    """A round pipe lying along x (or z), from a1 to a2, its axis at height y and at c across."""
    lo, hi = sorted((a1, a2))
    cuts = [lo + 16 * k for k in range(int((hi - lo - 0.01) // 16) + 1)] + [hi]
    side = 2 * r * math.tan(math.pi / n)
    for k in range(n):
        t0, t1 = 2 * math.pi * (k - 0.5) / n, 2 * math.pi * (k + 0.5) / n
        p0 = (r / math.cos(math.pi / n) * math.cos(t0), r / math.cos(math.pi / n) * math.sin(t0))
        p1 = (r / math.cos(math.pi / n) * math.cos(t1), r / math.cos(math.pi / n) * math.sin(t1))
        mid = (math.cos(2 * math.pi * k / n), math.sin(2 * math.pi * k / n))

        def pt(along, q):
            return (along, y + q[1], c + q[0]) if along_x else (c + q[0], y + q[1], along)

        out = (0, mid[1], mid[0]) if along_x else (mid[0], mid[1], 0)
        for u1, u2 in zip(cuts, cuts[1:]):
            d.quad(mat, [pt(u1, p0), pt(u2, p0), pt(u2, p1), pt(u1, p1)],
                   [(0, 0), (u2 - u1, 0), (u2 - u1, min(16, side)), (0, min(16, side))], out)


def ring(mat, a1, a2, y, c, r, along_x, n=8):
    """A short ring on a pipe (a flange or a band): its round side and both faces."""
    hpipe(mat, a1, a2, y, c, r, along_x, n)
    for at, sign in ((min(a1, a2), -1), (max(a1, a2), 1)):
        rr = r / math.cos(math.pi / n)
        pts = []
        for k in range(n):
            t = 2 * math.pi * (k - 0.5) / n
            q = (rr * math.cos(t), rr * math.sin(t))
            pts.append((at, y + q[1], c + q[0]) if along_x else (c + q[0], y + q[1], at))
        out = (sign, 0, 0) if along_x else (0, 0, sign)
        # A fan of quads from the first corner (an octagon as three quads).
        for k in range(1, n - 2, 2):
            quad = [pts[0], pts[k], pts[k + 1], pts[k + 2]]
            d.quad(mat, quad, [(0, 0), (0, 2 * r), (2 * r, 2 * r), (2 * r, 0)], out)


# The process lines: one big pipe from the middle of each side console's back straight in to the
# hub, carrying both its ports' lines (the console joins them inside), flanged where it leaves the
# console and where it meets the hub, with a band in each port's ring colour. It leaves low under
# the sloping back, between the two legs that stand behind the console. Inputs come in from the
# east, outputs leave to the west, as the ports do. From the front console two thin conduits carry
# the control core's power and signals.
PIPE_Y, PIPE_R = 6, 4.5
PAINT = {'port_fuel': 'breeder_paint_fuel', 'port_coolant': 'breeder_paint_coolant', 'port_energy': 'breeder_paint_energy'}
for side, ports in (('east', ['port_fuel', 'port_coolant']), ('west', ['port_fuel', 'port_energy'])):
    start = 2 * C - FACE - DEPTH if side == 'east' else FACE + DEPTH
    end = HUB[1] if side == 'east' else HUB[0]
    sign = 1 if end > start else -1
    hpipe('breeder_steel_pipe', start, end, PIPE_Y, C, PIPE_R, True)
    for at in (start, end - sign * 2):
        ring('breeder_fitting', at, at + sign * 2, PIPE_Y, C, PIPE_R + 1, True)
    mid = (start + end) / 2
    for k, kind in enumerate(ports):
        at = mid + sign * (k * 4 - 3.5)
        ring(PAINT[kind], at, at + 3, PIPE_Y, C, PIPE_R + 0.25, True)
for x in (C - 8, C + 8):
    hpipe('breeder_steel_pipe', FACE + FRONT_DEPTH, HUB[0], 5, x, 2, False)
    for at in (FACE + FRONT_DEPTH, HUB[0] - 1.5):
        ring('breeder_fitting', at, at + 1.5, 5, x, 3, False)

# ---------------------------------------------------------------- the walkway round the belt, and the ladder up to it
WALK_Y = BELT[1] + 1
WALK_R = LEG_R + 3
d.annulus('breeder_grate', WALK_Y, surface_r(WALK_Y), WALK_R, n=48, step=WALK_R)
d.annulus('breeder_fitting', BELT[1], BELT_R, WALK_R, up=False, n=48)
d.cylinder('breeder_fitting', BELT[1], WALK_Y, WALK_R, n=48)
# The ladder climbs beside the leg at the back left (105 degrees), bracketed to it, and arrives
# through a gap in the handrail.
LADDER = PHASE + 2 * math.pi * 3 / LEGS
GAP = math.radians(7)
for k in range(LEGS):
    phi = PHASE + 2 * math.pi * k / LEGS
    if abs(phi - LADDER) > 1e-6:
        d.post('breeder_rail', phi, WALK_R - 1, WALK_Y, WALK_Y + 7, 0.5, 0.5)
for y in (WALK_Y + 3.5, WALK_Y + 7):
    rail(arc(WALK_R - 1, y, LADDER + GAP, LADDER - GAP + 2 * math.pi, 64))
# The ladder, just outside its leg's pier: two stiles on the ground and rungs.
out = (math.cos(LADDER), 0, math.sin(LADDER))
across = (-math.sin(LADDER), 0, math.cos(LADDER))
LAD_R = LEG_R + 6


def lad(off, y, outward=0.0):
    return (C + (LAD_R + outward) * out[0] + off * across[0], y, C + (LAD_R + outward) * out[2] + off * across[2])


for off in (-2.5, 2.5):
    d.sweep('breeder_fitting', [lad(off, 0), lad(off, WALK_Y - 1)], 0.5, 0.5, lambda i: across, closed=False, caps=True)
for y in range(4, int(WALK_Y), 4):
    d.sweep('breeder_fitting', [lad(-2.5, y), lad(2.5, y)], 0.3, 0.3, lambda i: out, closed=False, caps=True)
# Brackets from the stiles back to the leg, a block apart, and a step plate at the top across the
# gap to the walkway's edge.
for y in range(24, int(WALK_Y), B):
    for off in (-2.5, 2.5):
        d.sweep('breeder_fitting', [lad(off, y), (C + (LEG_R + 1.5) * out[0] + off * 0.4 * across[0], y,
                                                  C + (LEG_R + 1.5) * out[2] + off * 0.4 * across[2])],
                0.4, 0.4, lambda i: (0, 1, 0), closed=False, caps=True)
d.sweep('breeder_fitting', [d.at(LADDER, WALK_R - 1, WALK_Y - 0.5), d.at(LADDER, LAD_R + 0.5, WALK_Y - 0.5)], 0.5, 3,
        lambda i: across, closed=False, caps=True)

# ---------------------------------------------------------------- the lantern: its frame, and the hall inside
LAN_LO, LAN_HI = -math.pi / 2 + LANTERN[0] * STEP, -math.pi / 2 + (LANTERN[-1] + 1) * STEP
(LAN_Y1, LAN_R1), (LAN_Y2, LAN_R2) = lat_point(LAN_LO), lat_point(LAN_HI)
LAN_MID = lat_point(LAN_LO + STEP)
# Mullions on the glass's seams, and a sill, a transom and a head ring round it.
panes = PLATES[LANTERN[0] - 1]
for k in range(panes):
    phi = 2 * math.pi * k / panes
    path = [d.at(phi, lat_point(a)[1] + 0.6, lat_point(a)[0]) for a in (LAN_LO + (LAN_HI - LAN_LO) * t / 4 for t in range(5))]
    d.sweep('breeder_fitting', path, 0.6, 0.6, lambda i, v=(-math.sin(phi), 0, math.cos(phi)): v, closed=False, caps=True)
for y, r in ((LAN_Y1, LAN_R1), LAN_MID, (LAN_Y2, LAN_R2)):
    d.sweep('breeder_fitting', [d.at(2 * math.pi * k / 48, r + 0.8, y) for k in range(48)], 0.8, 0.8, lambda i: (0, 1, 0))
# The hall's floor, level with the glass's sill: dark plates in two rings round the large plug,
# 16 then 32, the outer ring's corners meeting the inner's edges, with a painted yellow ring round
# the plug (keep clear while it turns) and a light line round the floor's edge.
FLOOR_IN, FLOOR_MID, FLOOR_OUT = 20, 34, LAN_R1 - 0.2
flat_ring('breeder_hall_floor', LAN_Y1, FLOOR_IN, FLOOR_MID, 16, 16)
flat_ring('breeder_hall_floor', LAN_Y1, FLOOR_MID, FLOOR_OUT, 32, 16)
d.annulus('breeder_rail', LAN_Y1 + 0.02, FLOOR_IN + 1.5, FLOOR_IN + 2.5, n=32)
d.annulus('glow', LAN_Y1 + 0.02, LAN_R1 - 2.5, LAN_R1 - 1.5, n=32)
# The rotating plugs over the core: a large one on the deck, and a small one in its middle carrying
# the control rod drives, centre stage in the hall.
HALL = LAN_Y1
PLUG_R, SMALL_R, SMALL_OFF = 20, 10, 0
SX, SZ = C, C + SMALL_OFF
with group('plug_large'):
    d.cylinder('breeder_fitting', HALL, HALL + 3, PLUG_R, n=16)
    d.annulus('breeder_plug', HALL + 3, 0, PLUG_R, n=16, step=PLUG_R / 2)
    # The fuel handling cask, front left of the large plug, and its orange lifting frame.
    fx, _, fz = d.at(math.radians(215), 15, 0)
    with centred(fx, fz):
        d.cylinder('breeder_exchanger', HALL + 3, HALL + 3 + B, 3.5, n=8)
        d.annulus('breeder_fitting', HALL + 3 + B, 0, 3.5, n=8)
with group('plug_small'):
    with centred(SX, SZ):
        d.cylinder('breeder_fitting', HALL + 3, HALL + 6, SMALL_R, n=8)
        d.annulus('breeder_plug', HALL + 6, 0, SMALL_R, n=8)
    # Seven control rod drives: housings a block high with a cyan band, capped.
    for dx, dz in [(0, 0)] + [(6 * math.cos(math.radians(a)), 6 * math.sin(math.radians(a))) for a in range(30, 390, 60)]:
        with centred(SX + dx, SZ + dz):
            d.cylinder('breeder_exchanger', HALL + 6, HALL + 6 + B, 1.6, n=6)
            d.cylinder('glow', HALL + 14, HALL + 15, 1.8, n=6)
            d.disc('breeder_fitting', HALL + 6 + B, HALL + 8 + B, 2.2, n=6)

# ---------------------------------------------------------------- the platform on top
PLAT_R = 16
PLAT_Y = EQUATOR + R
d.disc('breeder_grate', PLAT_Y - 2, PLAT_Y, PLAT_R, sides='breeder_fitting', n=8)
d.cylinder('glow', PLAT_Y - 1.5, PLAT_Y - 1, PLAT_R + 0.1, n=8)
for k in range(8):
    d.post('breeder_rail', 2 * math.pi * (k + 0.5) / 8, PLAT_R - 1, PLAT_Y, PLAT_Y + 7, 0.5, 0.5)
for y in (PLAT_Y + 3.5, PLAT_Y + 7):
    rail(arc(PLAT_R - 1, y, 0, 2 * math.pi, 16)[:-1], closed=True)
# The beacon's mast at the platform's front, and its housing.
BX, _, BZ = d.at(math.radians(270), 10, 0)
d.box('breeder_fitting', BX - 1, PLAT_Y, BZ - 1, BX + 1, PLAT_Y + 10, BZ + 1)
with centred(BX, BZ):
    d.disc('breeder_fitting', PLAT_Y + 10, PLAT_Y + 11, 3, n=8)
    d.disc('breeder_fitting', PLAT_Y + 15, PLAT_Y + 16, 3, n=8, bottom=True)
with group('beacon'):
    # The lamp: half lit amber, half its dark reflector, so turning it sweeps the light round.
    for k in range(8):
        p0, p1 = 2 * math.pi * k / 8, 2 * math.pi * (k + 1) / 8
        pm = (p0 + p1) / 2
        with centred(BX, BZ):
            pts = [d.at(p0, 2.2, PLAT_Y + 15), d.at(p0, 2.2, PLAT_Y + 11), d.at(p1, 2.2, PLAT_Y + 11), d.at(p1, 2.2, PLAT_Y + 15)]
        d.quad('breeder_amber' if k < 4 else 'breeder_fitting', pts, [(0, 0), (0, 4), (16, 4), (16, 0)],
               (math.cos(pm), 0, math.sin(pm)))

# ---------------------------------------------------------------- picture
GAME_DATA = os.path.join(mp.ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'breeder', 'breeder.json')


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'breeder.png')
    big_machine_textures.publish_all()
    breeder_textures.main()
    # For the game: the static body and the beacon, drawn by the control core (BreederRenderer).
    d.export(GAME_DATA)
    d.save_png(out, [((1, 1), 3, 0.5), ((-1, 0.4), 3, 0.35)])


if __name__ == '__main__':
    main()
