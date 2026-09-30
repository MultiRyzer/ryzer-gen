"""Concept design of the breeder reactor (design section 9), before the real multiblock exists.

A sodium-cooled fast reactor of the pool type, as most real ones are built: the core sits deep in
a big round tank of liquid sodium under a thick roof. Sodium is opaque, so unlike the fission
station there is nothing to see through: this is the sealed, industrial reactor, the station's
utility twin. For now only the base unit is drawn; what goes on the roof comes next. Drawn as
smooth quads (quad_design.py) on its own textures (breeder_textures.py). Run:
    python art/tools/breeder_concept.py [OUT.png]

Sizes in blocks: 9 across (centred on the middle of block 4, 4), 5.5 high.

Even textures: every surface shows its texture whole. Panels are sized to their texture (the
tank's 16 plates meet under its 16 ribs; rings are cut into equal widths), heights are whole
blocks or have a band texture drawn at their height, and decals (the trefoil, its plaque, the
bezel) are drawn once at their exact size.

What it shows, bottom to top:
- A graphite plinth with a hazard band, and the console on the front (north), proud of it: as you
  face it, inputs on your left (fuel in, liquid sodium in), outputs on your right (the output port
  for spent fuel and what the blanket bred, energy out), the control core's screen in the middle.
- The tank (guard vessel): gunmetal plates between ribs, a graphite band, a cyan light strip, and
  a radiation trefoil on the front.
- The roof slab: a graphite band round its edge and a deck of plates across its top.
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
TANK_R = 54            # the tank (guard vessel) outside
TANK_TOP = 5 * B       # 80: the tank is four blocks of plates
ROOF_TOP = TANK_TOP + 8
ROOF_R = 58
RIBS = 16
PHASE = math.radians(360 / RIBS / 2)   # tank seams and ribs sit here, so the front (north) is mid-plate

TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in (
    'hazard', 'glow', 'screen', 'port_energy', 'port_fuel', 'port_coolant')})
TEXTURES.update({
    'console': 'ryzergen:block/machine/console',
    'console_top': 'ryzergen:block/machine/console_top',
})
d = Design(C, TEXTURES, {'glow', 'screen'})


def band(y, h, r, n=None):
    """A graphite band `h` high (a lip, a rim), on its own band texture."""
    d.cylinder(f'breeder_band_{h}', y, y + h, r, n=n, v0=0)


def plates(mat, y1, y2, r, n=RIBS, phase=PHASE, v0=None):
    """Like a cylinder, but with `n` panels starting at `phase`: the tank's plates, and the bands
    that go round it, meet under its ribs."""
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

# ---------------------------------------------------------------- the tank: a guard vessel round the sodium pool
for y in range(16, TANK_TOP, B):
    plates('breeder_vessel', y, y + B, TANK_R)
# The ribs stand on the plates' seams; the front gap (north) holds the trefoil.
for k in range(RIBS):
    d.post('breeder_rib', PHASE + math.radians(k * 360 / RIBS), TANK_R + 1.5, 16, TANK_TOP, 1.5, 2)
plates('breeder_band_4', 44, 48, TANK_R + 3.2, v0=0)
d.annulus('breeder_fitting', 48, TANK_R, TANK_R + 3.2)
d.annulus('breeder_fitting', 44, TANK_R, TANK_R + 3.2, up=False)
plates('glow', 70, 71, TANK_R + 0.3)
# The trefoil: a 14 x 14 plaque on a 16 x 16 graphite backing, in the front gap.
PZ = C - TANK_R - 1
d.box('breeder_fitting', C - 8, 24, PZ, C + 8, 40, C - TANK_R + 2, decals={'north': 'breeder_plaque'}, skip=('south',))
d.box('breeder_fitting', C - 7, 25, PZ - 0.5, C + 7, 39, PZ, decals={'north': 'breeder_trefoil'}, skip=('south',))

# ---------------------------------------------------------------- the roof slab
plates('breeder_band_8', TANK_TOP, ROOF_TOP, ROOF_R, v0=0)
d.annulus('breeder_deck', ROOF_TOP, 0, ROOF_R, step=ROOF_R / 4)
d.annulus('breeder_fitting', TANK_TOP, TANK_R, ROOF_R, up=False)


# ---------------------------------------------------------------- picture
def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'breeder.png')
    big_machine_textures.publish_all()
    breeder_textures.main()
    d.save_png(out, [((1, 1), 3, 0.5), ((-1, 0.4), 3, 0.35)])


if __name__ == '__main__':
    main()
