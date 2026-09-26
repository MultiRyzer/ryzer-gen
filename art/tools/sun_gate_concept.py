"""Concept design of the sun gate (design section 11), before the real machine exists.

The way into the sun dimension: a small sun held above a round platform by three curved emitter
arms. It is also the swarm's orrery: solar shades circle it on six tilted orbits, and each orbit
fills in as the real swarm's coverage rises, so the gate shows your progress at a glance. Empty
slots show as faint outlines.

Only the platform is exported (SunGateRenderer draws it as the fusion preview draws its body). The
sun, its corona and the shades move, so the game draws them every frame (SunGateSun); here they are
drawn for the picture only, at half coverage. Run:
    python art/tools/sun_gate_concept.py [OUT.png]

Sizes in blocks: 11 across (centred on the middle of block 5, 5), about 10 high. The sun is 3 blocks
across, its middle 6.5 blocks up. Keep the numbers below in step with SunGateSun.java.
"""
import math
import os
import sys

import model_preview as mp
import big_machine_textures
from quad_design import Design

B = 16
C = 5.5 * B            # centre of the footprint, the middle of block (5, 5)
BASE_R = 84            # plinth radius
SY = 104               # height of the sun's middle
RS = 24                # the sun's radius
# The orbits: radius, tilt and the direction the tilt leans (degrees). Inner orbits turn faster.
ORBITS = [(36, 8, 0), (41, 58, 20), (46, -58, 80), (51, 30, 140), (56, -30, 200), (61, 75, 260)]
SLOTS = 24             # shades per orbit
SHADE = 3.5            # half the width of a shade
COVERAGE = 0.5         # for the picture

TEXTURES = {name: 'ryzergen:block/microreactor/' + name for name in (
    'steel', 'steel_dark', 'copper', 'hazard', 'glow', 'screen', 'port_energy')}
TEXTURES.update({
    'console': 'ryzergen:block/machine/console',
    'console_top': 'ryzergen:block/machine/console_top',
    'photosphere': 'ryzergen:block/sun/photosphere',
    'shade': 'ryzergen:block/sun/shade',
    'shade_back': 'ryzergen:block/sun/shade_back',
})
d = Design(C, TEXTURES, {'glow', 'screen', 'photosphere'}, concept_only={'sun', 'shades'})


# ---------------------------------------------------------------- platform: plinth, two steps and the emitter dish
d.annulus('steel', 16, 0, BASE_R)
d.annulus('glow', 16.02, BASE_R - 3, BASE_R - 2)
d.cylinder('hazard', 5, 16, BASE_R)
d.cylinder('steel_dark', 0, 5, BASE_R + 2)
d.annulus('steel_dark', 5, BASE_R, BASE_R + 2)
d.disc('steel_dark', 16, 22, 56, top='steel', bottom=False)
d.disc('steel_dark', 22, 28, 42, top='steel', bottom=False)
d.annulus('glow', 28.02, 40, 41)
# The emitter: a flared cup whose glowing dish looks up at the sun.
d.lathe('steel_dark', [(28, 30), (32, 33), (36, 36)])
d.annulus('steel_dark', 36, 26, 36)
d.cylinder('steel_dark', 33, 36, 26, inward=True)
d.annulus('glow', 33, 0, 26)

# ---------------------------------------------------------------- console on the front (north): energy in, and a screen
FRONT_Z = 2
d.box('console', C - 24, 0, FRONT_Z, C + 24, 15, 2 * B, top='console_top', skip=('south', 'down'))
x = C - 16
d.box('steel_dark', x + 3, 3, FRONT_Z - 1, x + 13, 13, FRONT_Z, decals={'north': 'port_energy'}, skip=('south',))
x = C
d.box('steel_dark', x + 1, 2, FRONT_Z - 1, x + 15, 14, FRONT_Z, skip=('south',))
d.box('steel_dark', x + 3, 5, FRONT_Z - 1.5, x + 13, 11, FRONT_Z - 1, decals={'north': 'screen'}, skip=('south',))

# ---------------------------------------------------------------- three emitter arms
# They stand outside the orbits and bend in over the sun, their tips aimed at it. Real basis, loosely:
# the gate holds its sun by fields, as a tokamak holds plasma, and the arms carry the field coils.
ARM = [(74, 16), (74, 32), (74, 48), (74, 64), (74, 80), (74, 96), (73, 110), (71, 122), (68, 133), (64, 142), (59, 149)]
for phi_deg in (90, 210, 330):
    phi = math.radians(phi_deg)
    round_dir = (-math.sin(phi), 0, math.cos(phi))
    along = lambda i: round_dir

    def pts(profile):
        return [d.at(phi, r, y) for r, y in profile]

    d.sweep('steel_dark', pts([(74, 16), (74, 26)]), 6, 7, along, closed=False, caps=True)
    d.sweep('steel', pts(ARM), 4, 5, along, closed=False, caps=True)
    for y in (40, 58):
        d.sweep('copper', pts([(74, y), (74, y + 8)]), 5, 6, along, closed=False, caps=True)
    # A light strip up the inside face, and the emitter head at the tip.
    d.sweep('glow', pts([(69.7, 28), (69.7, 96)]), 0.3, 1.5, along, closed=False)
    d.sweep('steel_dark', pts([(62.5, 145), (58, 150.5)]), 5.5, 6.5, along, closed=False, caps=True)
    d.sweep('glow', pts([(58, 150.5), (56.5, 152)]), 3.5, 4.5, along, closed=False, caps=True)

# ---------------------------------------------------------------- the sun and the shades (drawn in game by SunGateSun)
d.group = 'sun'
LAT, LON = 12, 24


def sphere_point(i, j):
    lon = 2 * math.pi * i / LON
    lat = math.pi * j / LAT - math.pi / 2
    return (math.cos(lat) * math.cos(lon), math.sin(lat), math.cos(lat) * math.sin(lon))


for i in range(LON):
    for j in range(LAT):
        corners = [sphere_point(i, j), sphere_point(i + 1, j), sphere_point(i + 1, j + 1), sphere_point(i, j + 1)]
        # Three panels to a tile each way, so the granules keep about one texel per pixel.
        u0, v0 = (i % 3) * 16 / 3, (j % 3) * 16 / 3
        uv = [(u0, v0), (u0 + 16 / 3, v0), (u0 + 16 / 3, v0 + 16 / 3), (u0, v0 + 16 / 3)]
        mid = [sum(c[k] for c in corners) / 4 for k in range(3)]
        d.quad('photosphere', [(C + RS * c[0], SY + RS * c[1], C + RS * c[2]) for c in corners], uv, mid)


def fill_order():
    """Which slot fills next: bit-reversed counting, so each orbit fills evenly all round, and all
    six orbits together."""
    order = [int(f'{m:05b}'[::-1], 2) for m in range(32)]
    order = [s for s in order if s < SLOTS]
    return [(ring, slot) for slot in order for ring in range(len(ORBITS))]


def orbit_frame(ring, theta):
    """Where a slot is (relative to the sun), which way the orbit runs there, and the orbit's axis."""
    r, tilt, lean = ORBITS[ring]
    i, o = math.radians(tilt), math.radians(lean)

    def turn(v):
        x, y, z = v
        y, z = y * math.cos(i) - z * math.sin(i), y * math.sin(i) + z * math.cos(i)
        return (x * math.cos(o) + z * math.sin(o), y, -x * math.sin(o) + z * math.cos(o))

    pos = turn((r * math.cos(theta), 0, r * math.sin(theta)))
    return pos, turn((-math.sin(theta), 0, math.cos(theta))), turn((0, 1, 0))


d.group = 'shades'
for ring, slot in fill_order()[:round(COVERAGE * len(ORBITS) * SLOTS)]:
    pos, t, a = orbit_frame(ring, 2 * math.pi * slot / SLOTS + ring * 0.7)
    corners = [tuple(C + pos[k] + SHADE * (su * t[k] + sa * a[k]) if k != 1 else SY + pos[k] + SHADE * (su * t[k] + sa * a[k])
                     for k in range(3)) for su, sa in ((-1, -1), (1, -1), (1, 1), (-1, 1))]
    uv = [(0, 16), (16, 16), (16, 0), (0, 0)]
    d.quad('shade', corners, uv, tuple(-v for v in pos))
    d.quad('shade_back', corners, uv, pos)
d.group = 'static'


# ---------------------------------------------------------------- export and picture
GAME_DATA = os.path.join(mp.ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'sun', 'sun_gate.json')


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'sun_gate.png')
    big_machine_textures.publish_all()
    d.export(GAME_DATA)
    d.save_png(out, [((1, 1), 3, 0.35), ((1, -0.3), 3, 0.15)])


if __name__ == '__main__':
    main()
