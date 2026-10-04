"""The mega drill's concept (design section 11c, option B), drawn as smooth quads, not yet in the
game. Run:
    python art/tools/mega_drill_concept.py [OUT.png]

One big square machine that never moves, built round a glowing molten pit (the user's reference,
4 Oct 2026; made square on 4 Oct, as players have asked for fewer round shapes). It stands over the
middle of a chunk on 9 x 9 blocks and mines the whole chunk below it, starting well under its base,
so it always stands on solid ground. Everything sits on the block grid, heights in whole blocks.
- The plinth: a square graphite base with hazard stripes, grated on top, and in its middle the
  square molten pit glowing amber, stepped down inside a gunmetal rim.
- Four square corner pylons, two blocks a side and five high, light casing with an orange stripe
  down their outer faces and a cyan light line under their graphite caps.
- The gantry: girders joining the pylons round the top, and a cross of girders the drill hangs
  from, braced to the pylons by four hydraulic rams.
- The drill: a square column two blocks a side in light casing, vents on each face, graphite bands
  and a light line, its foot a heavy collar and a pyramid tip glowing where it meets the melt; a
  stepped cap above the gantry with a beacon.
- Square ducts up the column (coolant in, spoil up), two square exhaust stacks at the back, and the
  controller on the front of the plinth: the station's front panel and screen.
Real basis: blind-shaft boring rigs, which stand over the hole they bore and lift the cuttings up
through it, here melting the rock with a beam (thermal spallation).
"""
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
ALL = ('north', 'south', 'east', 'west', 'up', 'down')
UV = [(0, 0), (16, 0), (16, 16), (0, 16)]


def face_only(face):
    return tuple(f for f in ALL if f != face)


def outline(x1, y1, z1, x2, y2, z2, mat, t=0.3):
    """A thin overlay round a box's four sides (a light line or a hazard band)."""
    d.box(mat, x1 - t, y1, z1 - t, x2 + t, y2, z2 + t, skip=('up', 'down'))


# ---------------------------------------------------------------- the plinth and the pit
PLINTH = B
PIT = 3 * B            # the pit's half width: a 6 x 6 pit in the 9 x 9 plinth
for x1, z1, x2, z2 in ((0, 0, SIZE, C - PIT), (0, C + PIT, SIZE, SIZE), (0, C - PIT, C - PIT, C + PIT), (C + PIT, C - PIT, SIZE, C + PIT)):
    d.box('drill_skirt', x1, 0, z1, x2, PLINTH, z2, top='breeder_grate', skip=('down',))
outline(0, 0, 0, SIZE, 4, SIZE, 'hazard')
# The pit: a gunmetal rim stepped in, graphite walls, the melt glowing at the bottom.
RIM = 4
for x1, z1, x2, z2 in ((C - PIT, C - PIT, C + PIT, C - PIT + RIM), (C - PIT, C + PIT - RIM, C + PIT, C + PIT),
                       (C - PIT, C - PIT + RIM, C - PIT + RIM, C + PIT - RIM), (C + PIT - RIM, C - PIT + RIM, C + PIT, C + PIT - RIM)):
    d.box('drill_frame', x1, PLINTH - 4, z1, x2, PLINTH, z2, top='breeder_grate', skip=('down',))
lo, hi = C - PIT + RIM, C + PIT - RIM
steps = [lo + k * B for k in range(int((hi - lo) // B))] + [hi]
for xa, xb in zip(steps, steps[1:]):
    d.quad('breeder_graphite', [(xa, PLINTH - 4, lo), (xb, PLINTH - 4, lo), (xb, 2, lo), (xa, 2, lo)], UV, (0, 0, 1))
    d.quad('breeder_graphite', [(xb, PLINTH - 4, hi), (xa, PLINTH - 4, hi), (xa, 2, hi), (xb, 2, hi)], UV, (0, 0, -1))
    d.quad('breeder_graphite', [(lo, PLINTH - 4, xb), (lo, PLINTH - 4, xa), (lo, 2, xa), (lo, 2, xb)], UV, (1, 0, 0))
    d.quad('breeder_graphite', [(hi, PLINTH - 4, xa), (hi, PLINTH - 4, xb), (hi, 2, xb), (hi, 2, xa)], UV, (-1, 0, 0))
    for za, zb in zip(steps, steps[1:]):
        d.quad('breeder_amber', [(xa, 2, za), (xb, 2, za), (xb, 2, zb), (xa, 2, zb)], UV, (0, 1, 0))

# ---------------------------------------------------------------- the corner pylons and the gantry
P = 2 * B
TOP = PLINTH + 5 * B
for px, pz, sx, sz in ((0, 0, -1, -1), (SIZE - P, 0, 1, -1), (0, SIZE - P, -1, 1), (SIZE - P, SIZE - P, 1, 1)):
    d.box('drill_casing', px, PLINTH, pz, px + P, TOP, pz + P, skip=('down',))
    d.box('drill_band', px - 1, TOP, pz - 1, px + P + 1, TOP + 4, pz + P + 1, top='breeder_grate')
    outline(px, TOP - 4, pz, px + P, TOP - 3, pz + P, 'glow')
    # An orange stripe down each outer face.
    ox = px if sx < 0 else px + P
    oz = pz if sz < 0 else pz + P
    d.box('breeder_rail', ox - (0.3 if sx < 0 else 0), PLINTH + 8, pz + P / 2 - 3, ox + (0 if sx < 0 else 0.3), TOP - 10, pz + P / 2 + 3,
          skip=face_only('west' if sx < 0 else 'east'))
    d.box('breeder_rail', px + P / 2 - 3, PLINTH + 8, oz - (0.3 if sz < 0 else 0), px + P / 2 + 3, TOP - 10, oz + (0 if sz < 0 else 0.3),
          skip=face_only('north' if sz < 0 else 'south'))
# Girders joining the pylons round the top, and a cross of girders over the middle for the drill.
G = 6
GY = TOP - B
for x1, z1, x2, z2 in ((P, C - G, SIZE - P, C + G), (C - G, P, C + G, SIZE - P),
                       (P, P / 2 - G, SIZE - P, P / 2 + G), (P, SIZE - P / 2 - G, SIZE - P, SIZE - P / 2 + G),
                       (P / 2 - G, P, P / 2 + G, SIZE - P), (SIZE - P / 2 - G, P, SIZE - P / 2 + G, SIZE - P)):
    d.box('breeder_girder', x1, GY, z1, x2, GY + 12, z2, top='breeder_grate')

# ---------------------------------------------------------------- the drill
D = B                  # the column's half width: 2 x 2 blocks
COL = (PLINTH + 2 * B, TOP + 2 * B)
d.box('drill_casing', C - D, COL[0], C - D, C + D, COL[1], C + D, skip=('down',))
for face, box in (('north', (C - 8, COL[0] + B, C - D - 0.3, C + 8, COL[0] + 2 * B, C - D)),
                  ('south', (C - 8, COL[0] + B, C + D, C + 8, COL[0] + 2 * B, C + D + 0.3)),
                  ('west', (C - D - 0.3, COL[0] + B, C - 8, C - D, COL[0] + 2 * B, C + 8)),
                  ('east', (C + D, COL[0] + B, C - 8, C + D + 0.3, COL[0] + 2 * B, C + 8))):
    d.box('drill_vent', *box, skip=face_only(face))
for y in (COL[0] + 3 * B, COL[1] - 4):
    d.box('drill_band', C - D - 1, y, C - D - 1, C + D + 1, y + 4, C + D + 1)
outline(C - D, COL[0] + 3 * B + 5, C - D, C + D, COL[0] + 3 * B + 6, C + D, 'glow')
# The collar at its foot, and the pyramid tip down to the melt.
COLLAR = 22
d.box('drill_band', C - COLLAR, COL[0] - 8, C - COLLAR, C + COLLAR, COL[0], C + COLLAR)
outline(C - COLLAR, COL[0] - 5, C - COLLAR, C + COLLAR, COL[0] - 4, C + COLLAR, 'glow')
tip = (C, PLINTH - 2, C)
corners = [(C - COLLAR + 2, COL[0] - 8, C - COLLAR + 2), (C + COLLAR - 2, COL[0] - 8, C - COLLAR + 2),
           (C + COLLAR - 2, COL[0] - 8, C + COLLAR - 2), (C - COLLAR + 2, COL[0] - 8, C + COLLAR - 2)]
for k in range(4):
    a, b = corners[k], corners[(k + 1) % 4]
    mid = ((a[0] + b[0]) / 2 - C, 0, (a[2] + b[2]) / 2 - C)
    d.quad('breeder_graphite', [a, b, tip, tip], [(0, 0), (16, 0), (8, 16), (8, 16)], (mid[0], -0.6 * COLLAR, mid[2]))
d.box('breeder_amber', C - 3, PLINTH - 5, C - 3, C + 3, PLINTH, C + 3)
# Four hydraulic rams from the pylons' inner corners to the collar.
for sx, sz in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
    p0 = (C + sx * (C - P), TOP - 2 * B, C + sz * (C - P))
    p1 = (C + sx * COLLAR, COL[0] - 4, C + sz * COLLAR)
    mid = tuple(p0[i] + (p1[i] - p0[i]) * 0.55 for i in range(3))
    d.sweep('breeder_graphite', [p0, mid], 2.4, 2.4, lambda i: (0, 1, 0), closed=False, caps=True)
    d.sweep('breeder_fitting', [mid, p1], 1.5, 1.5, lambda i: (0, 1, 0), closed=False, caps=True)
# The stepped cap and its beacon.
d.box('drill_band', C - D - 3, COL[1], C - D - 3, C + D + 3, COL[1] + 4, C + D + 3, top='breeder_graphite')
d.box('drill_casing', C - 10, COL[1] + 4, C - 10, C + 10, COL[1] + 12, C + 10, top='breeder_grate')
d.box('breeder_graphite', C - 3, COL[1] + 12, C - 3, C + 3, COL[1] + 16, C + 3, skip=('down',))
d.box('breeder_amber', C - 2, COL[1] + 16, C - 2, C + 2, COL[1] + 19, C + 2, skip=('down',))

# ---------------------------------------------------------------- ducts, stacks and the controller
# Square ducts up the column's front: coolant in on the left (copper), spoil up on the right.
for x1, mat in ((C - D - 7, 'breeder_pipe'), (C + D + 1, 'breeder_graphite')):
    d.box(mat, x1, COL[0], C - D - 6, x1 + 6, COL[1] - 6, C - D)
    for y in range(int(COL[0]) + 10, int(COL[1]) - 8, 16):
        d.box('breeder_rail', x1 - 0.6, y, C - D - 6.6, x1 + 6.6, y + 2, C - D)
# Two square exhaust stacks at the back, between the pylons.
for sx in (-1, 1):
    ex = C + sx * 30
    ez = SIZE - P / 2
    d.box('breeder_graphite', ex - 6, PLINTH, ez - 6, ex + 6, TOP + 2 * B, ez + 6)
    d.box('drill_band', ex - 7, TOP + 2 * B - 4, ez - 7, ex + 7, TOP + 2 * B, ez + 7, skip=('up',))
    d.box('breeder_rail', ex - 6.4, TOP, ez - 6.4, ex + 6.4, TOP + 3, ez + 6.4, skip=('up', 'down'))
# The controller on the front of the plinth: the station's front panel with its screen.
FACE = -2
d.box('breeder_graphite', C - B - 2, 0, FACE, C + B + 2, PLINTH + B, 0, skip=('south',))
d.quad('front_panel', [(C + B, PLINTH + B - 2, FACE - 0.1), (C - B, PLINTH + B - 2, FACE - 0.1), (C - B, PLINTH + 1, FACE - 0.1),
                       (C + B, PLINTH + 1, FACE - 0.1)], [(0, 1), (16, 1), (16, 16), (0, 16)], (0, 0, -1))
d.box('breeder_fitting', C - 5, PLINTH + 6, FACE - 1.1, C + 5, PLINTH + 12, FACE - 0.6, decals={'north': 'screen'}, skip=('south',))


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'mega_drill.png')
    breeder_textures.main()
    drill_textures.main()
    d.save_png(out, [((1, 1), 2.2, 0.5), ((-1, 0.6), 2.2, 0.25)])


if __name__ == '__main__':
    main()
