"""Concept design of the breeder reactor (design section 9), before the real multiblock exists.

A sodium-cooled fast reactor in a steel sphere on legs, as the Dounreay Fast Reactor was housed:
the core sits in a pool of liquid sodium inside, sealed from the air (sodium burns in it). The
shape is the refinery's Horton sphere: a sphere of welded plates standing on a ring of legs with
cross bracing, a girder belt at the equator where the legs meet it, and a railed platform on top.
Sodium is opaque, so unlike the fission station there is nothing to see through: this is the
sealed, industrial reactor, the station's utility twin. Drawn as smooth quads (quad_design.py) on
its own textures (breeder_textures.py). Run:
    python art/tools/breeder_concept.py [OUT.png]

Sizes in blocks: 9 across (centred on the middle of block 4, 4), about 8 high.

Even textures: every surface shows its texture whole. The sphere's radius makes each latitude band
exactly one plate high; the bands have 16 plates, halving to 8 nearest the poles, so every plate is
near 16 pixels wide and every corner meets its neighbours'. The belt's panels meet at the legs, heights
are whole blocks, and decals (the trefoil, its plaque, the bezel) are drawn at their exact size.

What it shows, bottom to top:
- A graphite plinth with a hazard band, and the console on the front (north), proud of it: as you
  face it, inputs on your left (fuel in, liquid sodium in), outputs on your right (the output port
  for spent fuel and what the blanket bred, energy out), the control core's screen in the middle.
- Twelve legs from the plinth to the equator on footings, braced in every bay but the front; a
  drain line from the sphere's foot, and two copper sodium lines from its underside, into the
  plinth.
- The sphere: light casing plates in ten latitude bands, and at its equator a graphite belt with a
  cyan light strip and the radiation trefoil on the front.
- A grated walkway round the belt with a yellow handrail, reached by a caged ladder up the back.
- The platform: a yellow handrail, a cyan light strip round its edge, the fuel hatch, and on a mast
  at its front an amber beacon.

The one moving part, drawn in its own group so a renderer can move it (design section 9): the
beacon, turning while the reactor runs, so a running breeder shows from across a base.
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
TEXTURES.update({
    'console': 'ryzergen:block/machine/console',
    'console_top': 'ryzergen:block/machine/console_top',
})
d = Design(C, TEXTURES, {'glow', 'screen', 'breeder_amber'})


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


def arc(rad, y, a0, a1, steps):
    """Points round the machine's axis at radius `rad` and height `y`, from angle a0 to a1."""
    return [d.at(a0 + (a1 - a0) * k / steps, rad, y) for k in range(steps + 1)]


def band(y, h, r, n=None):
    """A graphite band `h` high (the plinth's lip), on its own band texture."""
    d.cylinder(f'breeder_band_{h}', y, y + h, r, n=n, v0=0)


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


# ---------------------------------------------------------------- plinth, with the console proud of its front
d.annulus('breeder_plinth', 16, 0, PLINTH_R, step=PLINTH_R / 4)
d.cylinder('hazard', 5, 16, PLINTH_R)
band(0, 5, PLINTH_R + 2)
d.annulus('breeder_fitting', 5, PLINTH_R, PLINTH_R + 2)
# The console (north), in front of the plinth's lip (z 8): five cells centred on the middle one.
# As you face it (looking south), your left is east (larger x): inputs there, outputs to the west.
FRONT_Z = 4
d.box('console', 2 * B - 4, 0, FRONT_Z, 7 * B + 4, 15, 2 * B, top='console_top', skip=('south', 'down'))
PORTS = [('port_energy', 2), ('port_fuel', 3), ('core', 4), ('port_fuel', 5), ('port_coolant', 6)]
for kind, cell in PORTS:
    x = cell * B
    if kind == 'core':
        d.box('breeder_fitting', x + 1, 2, FRONT_Z - 1, x + 15, 14, FRONT_Z, decals={'north': 'breeder_bezel'}, skip=('south',))
        d.box('breeder_fitting', x + 3, 5, FRONT_Z - 1.5, x + 13, 11, FRONT_Z - 1, decals={'north': 'screen'}, skip=('south',))
    else:
        d.box('breeder_fitting', x + 3, 3, FRONT_Z - 1, x + 13, 13, FRONT_Z, decals={'north': kind}, skip=('south',))

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


def sphere_band(lo, hi, n, n_lo, n_hi):
    """One latitude band of `n` plates between latitudes lo and hi, its lower edge cut into n_lo
    pieces and its upper into n_hi (each n or 2n), so it meets its neighbours corner to corner."""
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
            d.quad('breeder_shell', pts, [(u0, 0), (u0, 16), (u1, 16), (u1, 0)], outward)


for k in range(1, BANDS - 1):          # the two polar caps are drawn apart
    lo, hi = -math.pi / 2 + k * STEP, -math.pi / 2 + (k + 1) * STEP
    sphere_band(lo, hi, PLATES[k - 1], EDGES[k - 1], EDGES[k])
# The caps, cut to meet the polar bands: under the platform on top, and round the drain at the foot.
TOP_Y, TOP_R = lat_point(math.pi / 2 - STEP)
FOOT_Y, FOOT_R = lat_point(-math.pi / 2 + STEP)
d.lathe('breeder_fitting', [(TOP_Y, TOP_R), (EQUATOR + R, 0)], n=8)
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
    d.post('breeder_rib', phi, LEG_R, 16, LEG_TOP, 2, 2)
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
# Footings under the legs.
for k in range(LEGS):
    fx, _, fz = d.at(PHASE + 2 * math.pi * k / LEGS, LEG_R, 0)
    d.box('breeder_fitting', fx - 3, 16, fz - 3, fx + 3, 17, fz + 3, skip=('down',))
# The drain line from the sphere's foot to the plinth.
d.cylinder('breeder_fitting', 16, EQUATOR - R + 1, 3, n=8)
# Two sodium lines from the sphere's underside into the plinth, east and west, each with a collar
# where it leaves the sphere and a pad where it meets the plinth.
for phi in (0, math.pi):
    px, _, pz = d.at(phi, 30, 0)
    top = EQUATOR - surface_r(30)
    with centred(px, pz):
        d.cylinder('breeder_pipe', 18, top + 1, 3, n=8)
        d.disc('breeder_fitting', top - 2, top, 4, n=8)
    d.box('breeder_fitting', px - 5, 16, pz - 5, px + 5, 18, pz + 5, skip=('down',))

# ---------------------------------------------------------------- the walkway round the belt, and the ladder up to it
WALK_Y = BELT[1] + 1
WALK_R = LEG_R + 3
d.annulus('breeder_grate', WALK_Y, surface_r(WALK_Y), WALK_R, n=48, step=WALK_R)
d.annulus('breeder_fitting', BELT[1], BELT_R, WALK_R, up=False, n=48)
d.cylinder('breeder_fitting', BELT[1], WALK_Y, WALK_R, n=48)
# The ladder arrives at the back (south, 90 degrees), through a gap in the handrail.
LADDER = math.radians(90)
GAP = math.radians(7)
for k in range(LEGS):
    phi = PHASE + 2 * math.pi * k / LEGS
    d.post('breeder_rail', phi, WALK_R - 1, WALK_Y, WALK_Y + 7, 0.5, 0.5)
for y in (WALK_Y + 3.5, WALK_Y + 7):
    rail(arc(WALK_R - 1, y, LADDER + GAP, LADDER - GAP + 2 * math.pi, 64))
# The caged ladder, outside the legs: two stiles, rungs, and hoops of the cage from head height.
out = (math.cos(LADDER), 0, math.sin(LADDER))
across = (-math.sin(LADDER), 0, math.cos(LADDER))
LAD_R = WALK_R + 1.5


def lad(off, y, outward=0.0):
    return (C + (LAD_R + outward) * out[0] + off * across[0], y, C + (LAD_R + outward) * out[2] + off * across[2])


for off in (-2.5, 2.5):
    d.sweep('breeder_fitting', [lad(off, 16), lad(off, WALK_Y + 7)], 0.5, 0.5, lambda i: across, closed=False, caps=True)
for y in range(20, int(WALK_Y), 4):
    d.sweep('breeder_fitting', [lad(-2.5, y), lad(2.5, y)], 0.3, 0.3, lambda i: out, closed=False, caps=True)
CAGE = 4.5
for y in range(44, int(WALK_Y) + 1, 9):
    hoop = [lad(-CAGE * math.cos(t), y, 2 + CAGE * math.sin(t)) for t in [math.pi * k / 8 for k in range(9)]]
    rail(hoop)
for t in (math.pi / 4, math.pi / 2, 3 * math.pi / 4):
    d.sweep('breeder_rail', [lad(-CAGE * math.cos(t), 44, 2 + CAGE * math.sin(t)),
                             lad(-CAGE * math.cos(t), WALK_Y, 2 + CAGE * math.sin(t))], 0.4, 0.4,
            lambda i: across, closed=False, caps=True)

# ---------------------------------------------------------------- the platform on top
PLAT_R = 16
PLAT_Y = EQUATOR + R
d.disc('breeder_grate', PLAT_Y - 2, PLAT_Y, PLAT_R, sides='breeder_fitting', n=8)
d.cylinder('glow', PLAT_Y - 1.5, PLAT_Y - 1, PLAT_R + 0.1, n=8)
for k in range(8):
    d.post('breeder_rail', 2 * math.pi * (k + 0.5) / 8, PLAT_R - 1, PLAT_Y, PLAT_Y + 7, 0.5, 0.5)
for y in (PLAT_Y + 3.5, PLAT_Y + 7):
    rail(arc(PLAT_R - 1, y, 0, 2 * math.pi, 16)[:-1], closed=True)
# The fuel hatch, over the core: a 10 x 10 lid.
d.box('breeder_fitting', C - 5, PLAT_Y, C - 1, C + 5, PLAT_Y + 1, C + 9, decals={'up': 'breeder_hatch'})
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
def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'breeder.png')
    big_machine_textures.publish_all()
    breeder_textures.main()
    d.save_png(out, [((1, 1), 3, 0.5), ((-1, 0.4), 3, 0.35)])


if __name__ == '__main__':
    main()
