"""Concept design of the fusion reactor (design section 10b), before the real multiblock exists.

A deuterium-tritium tokamak: a doughnut-shaped vacuum vessel with the plasma ring inside it, held
by D-shaped magnet coils round the tube, a central solenoid up the hole in the middle, and two
flat field coils. A band of smooth, see-through viewports round the top of the vessel shows the
plasma glowing. Drawn as smooth quads (quad_design.py) and exported for the game's preview block
(FusionPreviewRenderer). Run:
    python art/tools/tokamak_concept.py [OUT.png]

Sizes in blocks: 19 across (centred on the middle of block 9, 9), about 9.5 high. The vessel's tube
is 5 thick, its middle 5.5 from the centre of the machine.
"""
import math
import os
import sys

import model_preview as mp
import big_machine_textures
from quad_design import Design

B = 16
C = 9.5 * B            # centre of the footprint, the middle of block (9, 9)
R = 5.5 * B            # centre of the machine to the middle of the tube (major radius)
A = 2.5 * B            # tube radius to the vessel's outside (minor radius)
WALL = 6               # vessel wall thickness
YC = B + 8 + A + 12    # height of the tube's middle
BASE_R = 9 * B - 4     # plinth radius
NU = 64                # steps round the machine
NV = 24                # steps round the tube (15 degrees each)
GLASS = (15, 75)       # viewport band, degrees round the tube from the outside (90 is the top)

TEXTURES = {name: 'ryzergen:block/microreactor/' + name for name in (
    'steel', 'steel_dark', 'copper', 'lead', 'hazard', 'glow', 'screen', 'port_energy', 'port_steam', 'port_coolant')}
TEXTURES.update({
    'glass': 'ryzergen:block/machine/smooth_glass',
    'console': 'ryzergen:block/machine/console',
    'console_top': 'ryzergen:block/machine/console_top',
    'plasma_wisp': 'ryzergen:block/fusion/plasma_wisp',
})
d = Design(C, TEXTURES, {'glow', 'screen', 'plasma_wisp'}, concept_only={'plasma'})


# ---------------------------------------------------------------- the torus (vessel and plasma)
def tube_point(phi, theta, rad):
    """A point `rad` from the tube's middle, `theta` round the tube (0 outward, 90 up)."""
    dd = R + rad * math.cos(theta)
    return (C + dd * math.cos(phi), YC + rad * math.sin(theta), C + dd * math.sin(phi))


def tube_normal(phi, theta):
    return (math.cos(theta) * math.cos(phi), math.sin(theta), math.cos(theta) * math.sin(phi))


def torus(mat, rad, t1, t2, nv, inward=False, nu=NU):
    """Panels of a torus of tube radius `rad`, from t1 to t2 degrees round the tube."""
    for i in range(nu):
        p0, p1 = 2 * math.pi * i / nu, 2 * math.pi * (i + 1) / nu
        for j in range(nv):
            a0 = math.radians(t1 + (t2 - t1) * j / nv)
            a1 = math.radians(t1 + (t2 - t1) * (j + 1) / nv)
            n = tube_normal((p0 + p1) / 2, (a0 + a1) / 2)
            if inward:
                n = (-n[0], -n[1], -n[2])
            d.quad(mat, [tube_point(p0, a0, rad), tube_point(p1, a0, rad), tube_point(p1, a1, rad), tube_point(p0, a1, rad)],
                   [(0, 0), (0, 16), (16, 16), (16, 0)], n)


# ---------------------------------------------------------------- base: a round plinth with a flat console on the front
d.annulus('steel_dark', 16, 0, BASE_R)
d.cylinder('hazard', 5, 16, BASE_R)
d.cylinder('steel_dark', 0, 5, BASE_R + 2)
d.annulus('steel_dark', 5, BASE_R, BASE_R + 2)
# The console: a flat front (north) for the ports. Five cells centred on the middle one (the
# control core's screen): energy out, deuterium in, core, tritium in, liquid nitrogen in. Gases have
# white rings, liquids blue, energy red. Each port is the usual 10 x 10 flange, drawn whole.
FRONT_Z = 8
d.box('console', 7 * B - 4, 0, FRONT_Z, 12 * B + 4, 15, 2 * B, top='console_top', skip=('south', 'down'))
PORTS = [('port_energy', 7), ('port_steam', 8), ('core', 9), ('port_steam', 10), ('port_coolant', 11)]
for kind, cell in PORTS:
    x = cell * B
    if kind == 'core':
        d.box('steel_dark', x + 1, 2, FRONT_Z - 1, x + 15, 14, FRONT_Z, skip=('south',))
        d.box('steel_dark', x + 3, 5, FRONT_Z - 1.5, x + 13, 11, FRONT_Z - 1, decals={'north': 'screen'}, skip=('south',))
    else:
        d.box('steel_dark', x + 3, 3, FRONT_Z - 1, x + 13, 13, FRONT_Z, decals={'north': kind}, skip=('south',))
# Stands under the vessel, between the coils.
for k in range(8):
    d.post('steel_dark', math.radians(k * 45), R, 16, YC - A + 4, 5, 5)

# ---------------------------------------------------------------- vessel, viewports and plasma
per = 360 / NV
g1, g2 = GLASS
steel_steps = round((360 - (g2 - g1)) / per)
# Steel all round except the viewport band, outside and in (the inside keeps the view through the
# band on the plasma, rather than out through the far wall).
torus('steel', A, g2 - 360, g1, steel_steps)
torus('steel_dark', A - WALL, g2 - 360, g1, steel_steps, inward=True)
d.group = 'glass'
torus('glass', A, g1, g2, round((g2 - g1) / per))
d.group = 'static'
# Where the band meets the steel, the wall's thickness shows as a dark rim.
for theta, facing in ((g1, 1), (g2, -1)):
    th = math.radians(theta)
    for i in range(NU):
        p0, p1 = 2 * math.pi * i / NU, 2 * math.pi * (i + 1) / NU
        pm = (p0 + p1) / 2
        along = (-math.sin(th) * math.cos(pm), math.cos(th), -math.sin(th) * math.sin(pm))
        d.quad('steel_dark', [tube_point(p0, th, A - WALL), tube_point(p1, th, A - WALL), tube_point(p1, th, A), tube_point(p0, th, A)],
               [(0, 0), (16, 0), (16, WALL), (0, WALL)], tuple(facing * v for v in along))
# A cyan light strip round the outside, just below the viewports.
torus('glow', A + 0.4, -3, 3, 1)
# The plasma is drawn by the game every frame (FusionPlasma): three see-through shells of flowing
# filaments, turning round the ring at different speeds, and lightning arcs to the wall. Here the
# shells are drawn for the picture only.
d.group = 'plasma'
for radius in (9, 16, 24):
    torus('plasma_wisp', radius, 0, 360, 12, nu=48)
d.group = 'static'

# ---------------------------------------------------------------- toroidal field coils
# 16 D-shaped coils round the tube, set between the stands and clear of the heating duct.
# Real basis: they make the main field, the long way round the ring; the D takes the load best.
COILS = 16
RC = A + 7             # outer half: an ellipse this far out
HC = RC * 1.12         # half height
INNER = A + 5          # the flat inner side, this far in


def d_path(steps=72):
    """The coil's centreline in its own plane (outward from the tube's middle, up): an ellipse on
    the outside and a superellipse (nearly flat, round cornered) on the inside."""
    pts = []
    for k in range(steps):
        th = 2 * math.pi * k / steps
        c, s = math.cos(th), math.sin(th)
        if c >= 0:
            pts.append((RC * c, HC * s))
        else:
            pts.append((-INNER * abs(c) ** (1 / 3), HC * math.copysign(abs(s) ** (1 / 3), s)))
    return pts


D = d_path()
for k in range(COILS):
    phi = math.radians((k + 0.5) * 360 / COILS)
    toroidal = (-math.sin(phi), 0, math.cos(phi))
    path = [(C + (R + dd) * math.cos(phi), YC + dy, C + (R + dd) * math.sin(phi)) for dd, dy in D]
    d.sweep('copper', path, 3.5, 3.5, lambda i: toroidal)
    # Dark clamps round the coil at the top and the outside: short, wider sections of the same bar.
    for centre in (18, 0):
        idx = [(centre + o) % len(D) for o in range(-2, 3)]
        d.sweep('steel_dark', [path[i] for i in idx], 5, 5, lambda i: toroidal, closed=False, caps=True)

# ---------------------------------------------------------------- poloidal field coils: flat rings resting on the coils
for y, rad in ((YC + 45, R + 30), (19, R + 30)):
    d.sweep('copper', [d.at(2 * math.pi * k / NU, rad, y) for k in range(NU)], 4, 3, lambda i: (0, 1, 0))

# ---------------------------------------------------------------- central solenoid
SOL = 27
TOP = 9 * B
d.cylinder('lead', 16, TOP, SOL, n=32)
for y in range(32, TOP - 8, 20):
    d.cylinder('copper', y, y + 4, SOL + 1.5, n=32)
    d.annulus('copper', y + 4, SOL, SOL + 1.5, n=32)
    d.annulus('copper', y, SOL, SOL + 1.5, up=False, n=32)
d.disc('steel_dark', TOP, TOP + 4, SOL + 3, n=32)
d.annulus('glow', TOP + 4.02, SOL - 2, SOL + 1, n=32)

# ---------------------------------------------------------------- heating: a neutral beam injector on the west side
# Real basis: fast neutral atoms fired into the plasma to heat it. A straight duct on the middle
# row (z of the machine's centre), powered through the energy port on its end.
ZC = C
YB = 72
d.box('steel', 4, YB - 8, ZC - 8, 30, YB + 8, ZC + 8, skip=('east',))
d.box('steel_dark', 26, YB - 10, ZC - 10, 32, YB + 10, ZC + 10, skip=('east',))
d.box('steel_dark', 2, YB - 10, ZC - 10, 6, YB + 10, ZC + 10)
d.box('steel_dark', 1, YB - 5, ZC - 5, 2, YB + 5, ZC + 5, decals={'west': 'port_energy'}, skip=('east',))
d.box('steel_dark', 14, 16, ZC - 4, 22, YB - 8, ZC + 4, skip=('down', 'up'))


# ---------------------------------------------------------------- export and picture
GAME_DATA = os.path.join(mp.ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen', 'fusion', 'fusion_reactor.json')


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'fusion_reactor.png')
    big_machine_textures.publish_all()
    d.export(GAME_DATA)
    d.save_png(out, [((1, 1), 2, 0.5), ((1, 0.35), 2, 0.3)])


if __name__ == '__main__':
    main()
