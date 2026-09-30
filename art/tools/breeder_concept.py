"""Concept design of the breeder reactor (design section 9), before the real multiblock exists.

A sodium-cooled fast reactor of the pool type, as most real ones are built: the core sits deep in
a big round tank of liquid sodium, and everything that works it comes in through the thick roof.
Sodium is opaque, so unlike the fission station there is nothing to see through: this is the
sealed, industrial reactor, the station's utility twin. Drawn as smooth quads (quad_design.py) on
its own textures (breeder_textures.py), one framed plate per 16-pixel panel. Run:
    python art/tools/breeder_concept.py [OUT.png]

Sizes in blocks: 9 across (centred on the middle of block 4, 4), 7 high.

What it shows, bottom to top:
- A graphite plinth with a hazard band, and the console on the front (north), proud of it: as you
  face it, inputs on your left (fuel in, liquid sodium in), outputs on your right (the output port
  for spent fuel and what the blanket bred, energy out), the control core's screen in the middle.
- The tank (guard vessel): gunmetal plates, ribs, a graphite band, a cyan light strip, and a
  radiation trefoil on the front.
- The roof slab, and in it the two rotating plugs: a large one, and a small one set off its middle.
- On the small plug, the control rod drives and the fuel handling machine; through the deck, two
  primary pumps (finned motors with a fan on top) and two intermediate heat exchangers (tall, light
  casing), and the secondary sodium loops carried from the exchangers down the tank to the plinth.

Moving parts, each drawn in its own group so a renderer can move it (design section 9):
- fan: the pump motors' fans, turning while sodium flows.
- shafts: the drive shafts above the control rod drives, raised while the reactor runs (the rods
  withdrawn) and dropping all at once on a SCRAM.
- plug_large, plug_small (with the drives and handling machine on it): turning to bring the
  handling machine over a core position while fuel goes in or out, as real rotating plugs do.
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
TANK_R = 54            # the tank (guard vessel) outside
TANK_TOP = 5 * B       # where the roof sits
ROOF_TOP = TANK_TOP + 6
PLUG_R = 36            # the large rotating plug
SMALL_R = 18           # the small rotating plug
SMALL_OFF = 12         # the small plug's centre, this far behind the large one's
PLUG_TOP = ROOF_TOP + 4
SMALL_TOP = PLUG_TOP + 4
TOP = 7 * B            # the drives' caps: the top of the machine

TEXTURES = {'breeder_' + name: 'ryzergen:block/breeder/' + name for name in breeder_textures.TEXTURES}
TEXTURES.update({name: 'ryzergen:block/microreactor/' + name for name in (
    'hazard', 'glow', 'screen', 'port_energy', 'port_fuel', 'port_coolant')})
TEXTURES.update({
    'console': 'ryzergen:block/machine/console',
    'console_top': 'ryzergen:block/machine/console_top',
})
d = Design(C, TEXTURES, {'glow', 'screen'})


@contextlib.contextmanager
def centred(x, z):
    """Draws round a centre other than the machine's (for the plugs, pumps and exchangers)."""
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


def banded(mat, y1, y2, r, n=None):
    """A cylinder's side cut into bands on the 16-pixel grid, so its texture keeps its scale: one
    framed plate per panel, however tall the cylinder."""
    edges = [y1] + [y for y in range(int(y1 // B + 1) * B, int(y2), B) if y > y1] + [y2]
    for a, b in zip(edges, edges[1:]):
        d.cylinder(mat, a, b, r, n=n)


def on_circle(phi, rad):
    return C + rad * math.cos(phi), C + rad * math.sin(phi)


# ---------------------------------------------------------------- plinth, with the console proud of its front
d.annulus('breeder_plinth', 16, 0, PLINTH_R)
d.cylinder('hazard', 5, 16, PLINTH_R)
d.cylinder('breeder_plinth', 0, 5, PLINTH_R + 2)
d.annulus('breeder_plinth', 5, PLINTH_R, PLINTH_R + 2)
# The console (north), in front of the plinth's lip (z 8): five cells centred on the middle one.
# As you face it (looking south), your left is east (larger x): inputs there, outputs to the west.
FRONT_Z = 4
d.box('console', 2 * B - 4, 0, FRONT_Z, 7 * B + 4, 15, 2 * B, top='console_top', skip=('south', 'down'))
PORTS = [('port_energy', 2), ('port_fuel', 3), ('core', 4), ('port_fuel', 5), ('port_coolant', 6)]
for kind, cell in PORTS:
    x = cell * B
    if kind == 'core':
        d.box('breeder_cap', x + 1, 2, FRONT_Z - 1, x + 15, 14, FRONT_Z, skip=('south',))
        d.box('breeder_cap', x + 3, 5, FRONT_Z - 1.5, x + 13, 11, FRONT_Z - 1, decals={'north': 'screen'}, skip=('south',))
    else:
        d.box('breeder_cap', x + 3, 3, FRONT_Z - 1, x + 13, 13, FRONT_Z, decals={'north': kind}, skip=('south',))

# ---------------------------------------------------------------- the tank: a guard vessel round the sodium pool
banded('breeder_vessel', 16, TANK_TOP, TANK_R)
# Stiffening ribs, in pairs of gaps: the front gap (north, 270 degrees) holds the trefoil, and the
# gaps at 135 and 315 carry the secondary loops down.
for k in range(16):
    d.post('breeder_plug_side', math.radians(k * 22.5 + 11.25), TANK_R + 1.5, 16, TANK_TOP, 1.5, 2)
d.cylinder('breeder_plug_side', 44, 48, TANK_R + 3.2)
d.annulus('breeder_plug_side', 48, TANK_R, TANK_R + 3.2)
d.annulus('breeder_plug_side', 44, TANK_R, TANK_R + 3.2, up=False)
d.cylinder('glow', 70, 71, TANK_R + 0.3)
# The trefoil: a flat 14 x 14 plaque on a graphite backing, set in the front gap between two ribs.
PZ = C - TANK_R - 1
d.box('breeder_plug_side', C - 8, 22, PZ, C + 8, 38, C - TANK_R + 2, skip=('south',))
d.box('breeder_cap', C - 7, 23, PZ - 0.5, C + 7, 37, PZ, decals={'north': 'breeder_trefoil'}, skip=('south',))

# ---------------------------------------------------------------- the roof slab
d.cylinder('breeder_plug_side', TANK_TOP, ROOF_TOP, TANK_R + 4)
d.annulus('breeder_deck', ROOF_TOP, PLUG_R, TANK_R + 4)
d.annulus('breeder_plug_side', TANK_TOP, TANK_R, TANK_R + 4, up=False)
# A cyan light line round the large plug's seat.
d.annulus('glow', ROOF_TOP + 0.02, PLUG_R, PLUG_R + 1.5)

# ---------------------------------------------------------------- the rotating plugs, the drives and the handling machine
SX, SZ = C, C + SMALL_OFF
with group('plug_large'):
    d.cylinder('breeder_plug_side', ROOF_TOP - 2, PLUG_TOP, PLUG_R)
    d.annulus('breeder_plug', PLUG_TOP, 0, PLUG_R)
    # Three lifting lugs round the large plug's rim, orange hardware, clear of the small plug.
    for deg in (150, 270, 30):
        lx, lz = on_circle(math.radians(deg), PLUG_R - 6)
        d.box('breeder_cap', lx - 2, PLUG_TOP, lz - 2, lx + 2, PLUG_TOP + 1, lz + 2)
        d.box('breeder_lug', lx - 1, PLUG_TOP + 1, lz - 1, lx + 1, PLUG_TOP + 3, lz + 1)
with group('plug_small'):
    with centred(SX, SZ):
        d.cylinder('breeder_plug_side', PLUG_TOP, SMALL_TOP, SMALL_R)
        d.annulus('breeder_plug', SMALL_TOP, 0, SMALL_R)
    # Seven control rod drives: tall housings with a cap and a cyan light near the top.
    DRIVES = [(0, 0)] + [(9 * math.cos(math.radians(a)), 9 * math.sin(math.radians(a))) for a in range(30, 390, 60)]
    for dx, dz in DRIVES:
        with centred(SX + dx, SZ + dz):
            banded('breeder_drive', SMALL_TOP, TOP - 8, 2.5, n=8)
            d.cylinder('glow', TOP - 14, TOP - 12, 2.7, n=8)
            d.disc('breeder_cap', TOP - 8, TOP - 5, 3.2, n=8)
    # The fuel handling machine at the small plug's front edge: a squat cask over a port, and an
    # orange lifting frame.
    FX, FZ = SX, SZ - 13
    with centred(FX, FZ):
        d.disc('breeder_exchanger', SMALL_TOP, SMALL_TOP + 10, 4.5, top='breeder_cap', bottom=False, n=12)
    d.box('breeder_cap', FX - 5.5, SMALL_TOP + 10, FZ - 1, FX + 5.5, SMALL_TOP + 11, FZ + 1)
with group('shafts'):
    # The drive shafts, drawn raised (reactor running): polished rods out of the caps.
    for dx, dz in DRIVES:
        with centred(SX + dx, SZ + dz):
            d.disc('breeder_shaft', TOP - 5, TOP, 1, n=6, bottom=False)

# ---------------------------------------------------------------- pumps and heat exchangers through the deck
PR = TANK_R - 12       # their circle, clear of the large plug
# Primary pumps (front left and back right as you face it): a short neck, a finned motor, the fan.
PUMPS = [math.radians(a) for a in (225, 45)]
for phi in PUMPS:
    x, z = on_circle(phi, PR)
    with centred(x, z):
        d.cylinder('breeder_cap', ROOF_TOP, ROOF_TOP + 6, 6)
        d.disc('breeder_cap', ROOF_TOP + 6, ROOF_TOP + 8, 9.5, bottom=True)
        banded('breeder_pump', ROOF_TOP + 8, ROOF_TOP + 22, 8.5)
        d.cylinder('glow', ROOF_TOP + 18, ROOF_TOP + 19, 8.7)
        d.annulus('breeder_cap', ROOF_TOP + 22, 6, 8.5)
    with group('fan'):
        with centred(x, z):
            d.annulus('breeder_fan', ROOF_TOP + 21.5, 0, 6, n=8)
# Intermediate heat exchangers (front right and back left): tall light columns with two flanges
# and a domed head; the secondary loop leaves from their heads.
EXCHANGERS = [math.radians(a) for a in (315, 135)]
EX_TOP = ROOF_TOP + 30
for phi in EXCHANGERS:
    x, z = on_circle(phi, PR)
    with centred(x, z):
        banded('breeder_exchanger', ROOF_TOP, EX_TOP, 7)
        for y in (ROOF_TOP + 10, ROOF_TOP + 22):
            d.disc('breeder_cap', y, y + 2, 8.5)
        d.lathe('breeder_cap', [(EX_TOP, 7), (EX_TOP + 3, 6), (EX_TOP + 5, 3.5), (EX_TOP + 6, 0)])

# ---------------------------------------------------------------- the secondary loops: from each exchanger's head, over the edge, down the tank
# Hot and cold legs side by side, leaving the head sideways, over the roof's edge and down the
# rib gap to a flanged penetration on the plinth.
PIPE = 2.5
for phi in EXCHANGERS:
    out = (math.cos(phi), 0, math.sin(phi))
    across = (-math.sin(phi), 0, math.cos(phi))
    for side in (-1, 1):
        off = side * 4
        y = ROOF_TOP + 26 - (side + 1) * 3
        r_down = TANK_R + 5
        pts = [(C + (PR + 6) * out[0] + off * across[0], y, C + (PR + 6) * out[2] + off * across[2]),
               (C + r_down * out[0] + off * across[0], y, C + r_down * out[2] + off * across[2]),
               (C + r_down * out[0] + off * across[0], 18, C + r_down * out[2] + off * across[2])]
        d.sweep('breeder_pipe', pts, PIPE, PIPE, lambda i: across, closed=False, caps=True, along_v=True)
        # Flanges: at the head, at the bend, and the penetration box on the plinth.
        for p, half in ((pts[0], PIPE + 1), (pts[1], PIPE + 1)):
            d.box('breeder_cap', p[0] - half, p[1] - half, p[2] - half, p[0] + half, p[1] + half, p[2] + half)
        f = pts[2]
        d.box('breeder_cap', f[0] - 4, 16, f[2] - 4, f[0] + 4, 20, f[2] + 4)


# ---------------------------------------------------------------- the moving parts, posed
SHAFT_DROP = 5         # a SCRAM drops the shafts back into their caps


def turn(p, cx, cz, a):
    c, s = math.cos(a), math.sin(a)
    return (cx + (p[0] - cx) * c - (p[2] - cz) * s, p[1], cz + (p[0] - cx) * s + (p[2] - cz) * c)


def posed(large_deg, small_deg, dropped):
    """A copy of the design with its moving parts moved as the renderer will move them: the small
    plug (with the drives, handling machine and shafts) turned on its own axis, then both plugs
    turned together on the large plug's; the shafts lowered when `dropped` (a SCRAM)."""
    moved = Design(C, TEXTURES, d.emissive)
    a, b = math.radians(large_deg), math.radians(small_deg)
    for tex, verts, n, grp in d.quads:
        def place(x, y, z):
            p = (x, y, z)
            if grp in ('plug_small', 'shafts'):
                p = turn(p, SX, SZ, b)
            if grp in ('plug_large', 'plug_small', 'shafts'):
                p = turn(p, C, C, a)
            if grp == 'shafts' and dropped:
                p = (p[0], p[1] - SHAFT_DROP, p[2])
            return p
        new_verts = [(*place(*v[:3]), *v[3:]) for v in verts]
        new_n = n
        if grp in ('plug_large', 'plug_small', 'shafts'):
            new_n = turn((n[0], n[1], n[2]), 0, 0, (b if grp != 'plug_large' else 0) + a)
        moved.quads.append((tex, new_verts, new_n, grp))
    return moved


# ---------------------------------------------------------------- picture
def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(mp.ROOT, 'art', 'concepts', 'breeder.png')
    big_machine_textures.publish_all()
    breeder_textures.main()
    d.save_png(out, [((1, 1), 3, 0.5), ((-1, 0.4), 3, 0.35)])
    # The same machine mid refuel (plugs turned) and after a SCRAM (shafts down), to check that
    # every moving part clears what is round it.
    posed(70, -120, True).save_png(out.replace('.png', '_moving.png'), [((1, 1), 3, 0.5), ((-1, 0.4), 3, 0.35)])


if __name__ == '__main__':
    main()
