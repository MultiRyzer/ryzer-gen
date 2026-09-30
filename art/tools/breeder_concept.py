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
exactly one plate high, and each band has as many plates as keep them near 16 pixels wide, so the
seams stagger band to band as a welded sphere's do. The belt's panels meet at the legs, heights
are whole blocks, and decals (the trefoil, its plaque, the bezel) are drawn at their exact size.

What it shows, bottom to top:
- A graphite plinth with a hazard band, and the console on the front (north), proud of it: as you
  face it, inputs on your left (fuel in, liquid sodium in), outputs on your right (the output port
  for spent fuel and what the blanket bred, energy out), the control core's screen in the middle.
- Twelve legs from the plinth to the equator, braced in every bay but the front, and a drain line
  from the sphere's foot to the plinth.
- The sphere: light casing plates in ten latitude bands, and at its equator a graphite belt with a
  cyan light strip and the radiation trefoil on the front.
- A platform on top with a yellow handrail.
"""
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
d = Design(C, TEXTURES, {'glow', 'screen'})


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
for k in range(1, BANDS - 1):          # the two polar caps are drawn apart
    lo, hi = -math.pi / 2 + k * STEP, -math.pi / 2 + (k + 1) * STEP
    (y1, r1), (y2, r2) = lat_point(lo), lat_point(hi)
    n = max(6, round(2 * math.pi * (r1 + r2) / 2 / B))
    d.lathe('breeder_shell', [(y1, r1), (y2, r2)], n=n)
# The caps: under the platform on top, and round the drain at the foot.
TOP_Y, TOP_R = lat_point(math.pi / 2 - STEP)
FOOT_Y, FOOT_R = lat_point(-math.pi / 2 + STEP)
d.lathe('breeder_fitting', [(TOP_Y, TOP_R), (EQUATOR + R, 0)], n=12)
d.lathe('breeder_fitting', [(EQUATOR - R, 0), (FOOT_Y, FOOT_R)], n=12)

# ---------------------------------------------------------------- the girder belt round the equator
belt_r = BELT_R
plates('breeder_plinth', BELT[0], BELT[1], belt_r, n=LEGS)
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
# The drain line from the sphere's foot to the plinth.
d.cylinder('breeder_fitting', 16, EQUATOR - R + 1, 3, n=8)

# ---------------------------------------------------------------- the platform on top, with a handrail
PLAT_R = 16
PLAT_Y = EQUATOR + R
d.disc('breeder_deck', PLAT_Y - 2, PLAT_Y, PLAT_R, sides='breeder_fitting', n=8)
for k in range(8):
    phi = 2 * math.pi * (k + 0.5) / 8
    d.post('breeder_rail', phi, PLAT_R - 1, PLAT_Y, PLAT_Y + 7, 0.5, 0.5)
ring = [d.at(2 * math.pi * k / 16, PLAT_R - 1, PLAT_Y + 7) for k in range(16)]
d.sweep('breeder_rail', ring, 0.5, 0.5, lambda i: (0, 1, 0))


# ---------------------------------------------------------------- picture
def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'breeder.png')
    big_machine_textures.publish_all()
    breeder_textures.main()
    d.save_png(out, [((1, 1), 3, 0.5), ((-1, 0.4), 3, 0.35)])


if __name__ == '__main__':
    main()
