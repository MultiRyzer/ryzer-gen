"""The mega drill's concept (design section 11c, the static option), drawn as smooth quads, not yet
in the game. Run:
    python art/tools/mega_drill_concept.py [OUT.png]

One big machine that never moves, built round a glowing molten pit (the user's reference, 4 Oct
2026). It stands over the middle of a chunk on 9 x 9 blocks and mines the whole chunk below it,
starting well under its base, so it always stands on solid ground.
- The foundation: an eight-sided graphite plinth with hazard stripes, grated on top.
- Four armoured corner pylons, light casing with an orange stripe and a cyan light line, leaning
  in a little, the heavy frame that holds the drill.
- The pit at its heart: a ring of grating round a molten core glowing amber, sunk into the plinth,
  where the beam melts its way down the bore.
- The drill column held over the pit by eight hydraulic rams from the pylons: its head a graphite
  collar and a cone down to a tip glowing where it meets the melt, then a brushed-steel column
  banded in graphite, an orange collar, a ring of cooling fins, and a domed cap with a beacon.
- Pipes up its sides (copper for the coolant, steel for what comes up), two exhaust stacks at the
  back venting steam, and the controller on the front: the station's front panel and screen.
Real basis: blind-shaft boring rigs, which stand over the hole they bore and lift the cuttings up
through it, here melting the rock with the beam the other drills use (thermal spallation).
"""
import math
import os
import sys

import model_preview as mp
import breeder_textures
import drill_textures
from quad_design import Design

B = 16
SIZE = 9 * B
C = SIZE / 2
TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({'drill_' + name: 'ryzergen:block/drill/' + name for name in drill_textures.TEXTURES})
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in ('hazard', 'glow', 'screen')})
TEXTURES['front_panel'] = 'ryzergen:block/breeder/front_panel'
d = Design(C, TEXTURES, {'glow', 'screen', 'breeder_amber'})
OCT = math.pi / 8           # eight-sided parts turned so a flat faces each side


def centred(x, z, draw):
    whole = d.at
    d.at = lambda phi, rad, y: (x + rad * math.cos(phi), y, z + rad * math.sin(phi))
    try:
        draw()
    finally:
        d.at = whole


def octagonal(draw):
    whole = d.at
    d.at = lambda phi, rad, y: (C + rad * math.cos(phi + OCT), y, C + rad * math.sin(phi + OCT))
    try:
        draw()
    finally:
        d.at = whole


# ---------------------------------------------------------------- the foundation and the pit
PLINTH = 10
PIT_R = 46
COL_R = 18
COL = (PLINTH + 26, 100)
octagonal(lambda: (
    d.cylinder('drill_skirt', 0, PLINTH, 70, n=8, v0=6),
    d.cylinder('hazard', 0, 4, 70.3, n=8),
    d.annulus('breeder_grate', PLINTH, PIT_R + 6, 70, n=8),
    # The pit: a stepped graphite ring down to the molten core.
    d.cylinder('drill_frame', PLINTH - 4, PLINTH, PIT_R + 6, n=16, inward=True, v0=12),
    d.annulus('breeder_grate', PLINTH - 4, PIT_R, PIT_R + 6, n=16),
    d.cylinder('breeder_graphite', 2, PLINTH - 4, PIT_R, n=16, inward=True),
))
centred(C, C, lambda: (
    d.disc('breeder_amber', 1, 2, PIT_R, n=16),
    d.cylinder('breeder_amber', 2, 3, PIT_R - 0.2, n=16, inward=True),
))

# ---------------------------------------------------------------- the corner pylons
PYLON = 18
for k in range(4):
    phi = math.radians(45 + 90 * k)
    out = (math.cos(phi), math.sin(phi))
    base = (C + out[0] * 52, C + out[1] * 52)
    top = (C + out[0] * 44, C + out[1] * 44)
    across = (-out[1], 0, out[0])
    # A heavy leaning pylon: a swept block, light casing, orange stripe, light line.
    d.sweep('drill_casing', [(base[0], PLINTH, base[1]), (base[0], PLINTH + 14, base[1]), (top[0], 76, top[1])],
            PYLON / 2, PYLON / 2, lambda i, v=across: v, closed=False, caps=True)
    d.sweep('breeder_amber' if False else 'breeder_rail', [(base[0] + out[0] * (PYLON / 2 + 0.3), PLINTH + 30, base[1] + out[1] * (PYLON / 2 + 0.3)),
                                                           (top[0] + out[0] * (PYLON / 2 + 0.3), 58, top[1] + out[1] * (PYLON / 2 + 0.3))],
            0.3, 3, lambda i, v=across: v, closed=False)
    d.box('glow', top[0] - PYLON / 2 - 0.3, 70, top[1] - PYLON / 2 - 0.3, top[0] + PYLON / 2 + 0.3, 71, top[1] + PYLON / 2 + 0.3,
          skip=('up', 'down'))
    # A graphite cap on each pylon.
    d.box('drill_band', top[0] - PYLON / 2 - 1, 76, top[1] - PYLON / 2 - 1, top[0] + PYLON / 2 + 1, 80, top[1] + PYLON / 2 + 1,
          top='breeder_graphite')
    # A hydraulic ram from the pylon's head to the column's collar, and one lower down.
    for (y0, y1) in ((72, 60), (34, 30)):
        p0 = (top[0] - out[0] * PYLON / 2, y0, top[1] - out[1] * PYLON / 2)
        p1 = (C + out[0] * (COL_R + 3), y1 + 8, C + out[1] * (COL_R + 3))
        mid = tuple(p0[i] + (p1[i] - p0[i]) * 0.5 for i in range(3))
        d.sweep('breeder_graphite', [p0, mid], 2.2, 2.2, lambda i: (0, 1, 0), closed=False, caps=True)
        d.sweep('breeder_fitting', [mid, p1], 1.4, 1.4, lambda i: (0, 1, 0), closed=False, caps=True)

# ---------------------------------------------------------------- the drill column
centred(C, C, lambda: (
    # The drill's head hanging over the pit: a graphite collar, a cone down to a tip glowing white
    # hot where it meets the melt, with the beam's lens.
    d.cylinder('drill_ring', COL[0] - 6, COL[0], COL_R + 4, n=16, v0=8),
    d.annulus('drill_ring', COL[0] - 6, 0, COL_R + 4, up=False, n=16),
    d.annulus('drill_ring', COL[0], COL_R, COL_R + 4, n=16),
    d.cylinder('glow', COL[0] - 3.5, COL[0] - 2.5, COL_R + 4.2, n=16),
    d.lathe('breeder_graphite', [(PLINTH - 2, 3), (COL[0] - 6, COL_R)], n=16),
    d.cylinder('breeder_amber', PLINTH - 4, PLINTH - 2, 3, n=8),
    # The column in brushed steel, banded in graphite, an orange collar, a ring of fins.
    d.cylinder('drill_pot', COL[0], COL[1], COL_R, n=16),
    d.cylinder('drill_band', COL[0] + 20, COL[0] + 24, COL_R + 0.3, n=16, v0=12),
    d.cylinder('breeder_rail', 58, 63, COL_R + 0.4, n=16),
    d.cylinder('glow', 64, 65, COL_R + 0.4, n=16),
    d.cylinder('drill_band', COL[1] - 6, COL[1], COL_R + 2, n=16, v0=10),
))
for k in range(16):
    phi = 2 * math.pi * k / 16
    centred(C, C, lambda phi=phi: d.post('drill_frame', phi, COL_R + 3, 70, 88, 3, 0.8))
# The domed cap and its beacon.
dome = [(COL[1] + 12 * math.sin(t * math.pi / 2), (COL_R + 1) * math.cos(t * math.pi / 2)) for t in [k / 6 for k in range(7)]]
centred(C, C, lambda: (
    d.lathe('drill_pot', dome, n=16),
    d.cylinder('breeder_graphite', COL[1] + 11, COL[1] + 16, 5, n=8),
    d.cylinder('breeder_amber', COL[1] + 16, COL[1] + 19, 2.5, n=8),
))

# ---------------------------------------------------------------- pipes, stacks and the controller
# Pipes up the column's sides: copper for the coolant, steel for what the bore brings up.
for k, (deg, mat, r) in enumerate(((150, 'breeder_pipe', 2.5), (210, 'breeder_steel_pipe', 3.5), (330, 'breeder_pipe', 2.5), (30, 'breeder_steel_pipe', 3.5))):
    phi = math.radians(deg)
    px, pz = C + (COL_R + r + 1) * math.cos(phi), C + (COL_R + r + 1) * math.sin(phi)
    centred(px, pz, lambda mat=mat, r=r: d.cylinder(mat, COL[0], COL[1] - 8, r, n=8))
    # Clamps up the pipe.
    for y in range(COL[0] + 8, COL[1] - 8, 20):
        centred(px, pz, lambda y=y, r=r: d.cylinder('breeder_rail', y, y + 2, r + 0.6, n=8))
# Two exhaust stacks at the back, venting steam.
for sx in (-1, 1):
    ex, ez = C + sx * 30, C + 52
    centred(ex, ez, lambda: (
        d.cylinder('breeder_graphite', PLINTH, 100, 6, n=10),
        d.cylinder('drill_band', 100, 106, 7, n=10, v0=10),
        d.cylinder('breeder_graphite', 98, 106, 5, n=10, inward=True),
        d.cylinder('breeder_rail', 80, 83, 6.3, n=10),
    ))
# The controller on the front of the plinth: the station's front panel with its screen.
FACE = C - 70 * math.cos(OCT) - 2
d.box('breeder_graphite', C - 18, PLINTH, FACE, C + 18, PLINTH + 18, FACE + 10, skip=('down',))
d.quad('front_panel', [(C + B, PLINTH + 16, FACE - 0.1), (C - B, PLINTH + 16, FACE - 0.1), (C - B, PLINTH + 1, FACE - 0.1), (C + B, PLINTH + 1, FACE - 0.1)],
       [(0, 1), (16, 1), (16, 16), (0, 16)], (0, 0, -1))
d.box('breeder_fitting', C - 5, PLINTH + 6, FACE - 1.1, C + 5, PLINTH + 12, FACE - 0.6, decals={'north': 'screen'}, skip=('south',))


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'mega_drill.png')
    breeder_textures.main()
    drill_textures.main()
    d.save_png(out, [((1, 1), 2.4, 0.5), ((-1, 0.6), 2.4, 0.25)])


if __name__ == '__main__':
    main()
