"""Concept design of the breeder reactor (design section 9), before the real multiblock exists.

A sodium-cooled fast reactor of the pool type, as most real ones are built: the core sits deep in
a big round tank of liquid sodium, and everything that works it comes in through the thick roof.
Sodium is opaque, so unlike the fission station there is nothing to see through: this is the
sealed, industrial reactor, the station's utility twin. Drawn as smooth quads (quad_design.py) on
its own textures (breeder_textures.py). Run:
    python art/tools/breeder_concept.py [OUT.png]

Sizes in blocks: 9 across (centred on the middle of block 4, 4), 7.5 high.

Even textures: every surface shows its texture whole. Panels are sized to their texture (the
tank's 16 plates meet under its 16 ribs; rings are cut into equal widths), heights are whole
multiples of a block or have a band texture drawn at their height, small round parts use textures
that read the same however narrow a panel is, and decals (the fan, the trefoil, the bezel) are
drawn once at their exact size.

What it shows, bottom to top:
- A graphite plinth with a hazard band, and the console on the front (north), proud of it: as you
  face it, inputs on your left (fuel in, liquid sodium in), outputs on your right (the output port
  for spent fuel and what the blanket bred, energy out), the control core's screen in the middle.
- The tank (guard vessel): gunmetal plates between ribs, a graphite band, a cyan light strip, and
  a radiation trefoil on the front.
- The roof slab, and in it the two rotating plugs: a large one, and a small one set off its middle.
- On the small plug, the control rod drives; on the large plug, lifting lugs and the fuel handling
  machine; through the deck, two primary pumps (finned motors with a fan on top) and two
  intermediate heat exchangers (tall, light casing), and the secondary sodium loops carried from
  the exchangers down the tank to the plinth.

Moving parts, each drawn in its own group so a renderer can move it (design section 9):
- fan: the pump motors' fans, turning while sodium flows.
- shafts: the drive shafts above the control rod drives, raised while the reactor runs (the rods
  withdrawn) and dropping all at once on a SCRAM.
- plug_large (with the lugs and the handling machine), plug_small (with the drives): turning to
  bring the handling machine over a core position while fuel goes in or out, as real rotating
  plugs do.
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
TANK_TOP = 5 * B       # 80: the tank is four blocks of plates
ROOF_TOP = TANK_TOP + 8
ROOF_R = 58
PLUG_R = 32            # the large rotating plug: two rings of plates
SMALL_R = 16           # the small rotating plug: one ring
SMALL_OFF = 12         # the small plug's centre, this far behind the large one's
PLUG_TOP = ROOF_TOP + 4
SMALL_TOP = PLUG_TOP + 4
DRIVE_TOP = SMALL_TOP + B   # the drives: one block of housing
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


def blocks(mat, y1, y2, r, n=None):
    """A cylinder's side in bands a block high from its own foot, so each shows its texture whole.
    The height must be whole blocks."""
    assert (y2 - y1) % B == 0, (mat, y1, y2)
    for y in range(int(y1), int(y2), B):
        d.cylinder(mat, y, y + B, r, n=n)


def band(y, h, r, n=None):
    """A graphite band `h` high (a rim, a plug's edge, a flange's side), on its own band texture."""
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


def decal_disc(mat, cx, cz, y, r, size, n=16):
    """A flat round top with a decal drawn once across it at its exact size (`size` pixels,
    centred), not once per wedge."""
    half = size / 2
    for k in range(n):
        p0, p1 = 2 * math.pi * k / n, 2 * math.pi * (k + 1) / n
        pts = [(cx, y, cz), (cx, y, cz), (cx + r * math.cos(p1), y, cz + r * math.sin(p1)), (cx + r * math.cos(p0), y, cz + r * math.sin(p0))]
        uvs = [(half + (p[0] - cx), half + (p[2] - cz)) for p in pts]
        d.quad(mat, pts, uvs, (0, 1, 0))


def on_circle(phi, rad):
    return C + rad * math.cos(phi), C + rad * math.sin(phi)


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
# The ribs stand on the plates' seams. The front gap (north) holds the trefoil, and the gaps at 135
# and 315 degrees carry the secondary loops down.
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
d.annulus('breeder_deck', ROOF_TOP, PLUG_R, ROOF_R, step=(ROOF_R - PLUG_R) / 2)
d.annulus('breeder_fitting', TANK_TOP, TANK_R, ROOF_R, up=False)
# A cyan light line round the large plug's seat.
d.annulus('glow', ROOF_TOP + 0.02, PLUG_R, PLUG_R + 1.5)

# ---------------------------------------------------------------- the rotating plugs, the drives and the handling machine
SX, SZ = C, C + SMALL_OFF
with group('plug_large'):
    band(ROOF_TOP, 4, PLUG_R)
    d.annulus('breeder_plug', PLUG_TOP, 0, PLUG_R)
    # Three lifting lugs round the rim, clear of the small plug.
    for deg in (150, 270, 30):
        lx, lz = on_circle(math.radians(deg), PLUG_R - 6)
        d.box('breeder_fitting', lx - 2, PLUG_TOP, lz - 2, lx + 2, PLUG_TOP + 1, lz + 2)
        d.box('breeder_lug', lx - 1, PLUG_TOP + 1, lz - 1, lx + 1, PLUG_TOP + 3, lz + 1)
    # The fuel handling machine, front left of the large plug: a cask a block high over a port,
    # and an orange lifting frame.
    FX, FZ = on_circle(math.radians(215), 22)
    with centred(FX, FZ):
        d.cylinder('breeder_exchanger', PLUG_TOP, PLUG_TOP + B, 4.5, n=12)
        d.annulus('breeder_fitting', PLUG_TOP + B, 0, 4.5, n=12)
    d.box('breeder_lug', FX - 5.5, PLUG_TOP + B, FZ - 1, FX + 5.5, PLUG_TOP + B + 1, FZ + 1)
with group('plug_small'):
    with centred(SX, SZ):
        band(PLUG_TOP, 4, SMALL_R)
        d.annulus('breeder_plug', SMALL_TOP, 0, SMALL_R)
    # Seven control rod drives: housings a block high, a cyan light, a cap.
    DRIVES = [(0, 0)] + [(9 * math.cos(math.radians(a)), 9 * math.sin(math.radians(a))) for a in range(30, 390, 60)]
    for dx, dz in DRIVES:
        with centred(SX + dx, SZ + dz):
            blocks('breeder_drive', SMALL_TOP, DRIVE_TOP, 2.5, n=8)
            d.cylinder('glow', SMALL_TOP + 8, SMALL_TOP + 9, 2.7, n=8)
            d.disc('breeder_fitting', DRIVE_TOP, DRIVE_TOP + 3, 3.2, n=8)
with group('shafts'):
    # The drive shafts, drawn raised (reactor running): polished rods out of the caps.
    for dx, dz in DRIVES:
        with centred(SX + dx, SZ + dz):
            d.disc('breeder_shaft', DRIVE_TOP + 3, DRIVE_TOP + 8, 1, n=6, bottom=False)

# ---------------------------------------------------------------- pumps and heat exchangers through the deck
PR = TANK_R - 10       # their circle, clear of the large plug and inside the roof's edge
# Primary pumps (front left and back right as you face it): a neck, a flange, a finned motor a
# block high with one fin per panel, and the fan across its top.
PUMPS = [math.radians(a) for a in (225, 45)]
MOTOR = ROOF_TOP + 6
for phi in PUMPS:
    x, z = on_circle(phi, PR)
    with centred(x, z):
        d.cylinder('breeder_fitting', ROOF_TOP, ROOF_TOP + 4, 6)
        band(ROOF_TOP + 4, 2, 9.5)
        d.annulus('breeder_fitting', ROOF_TOP + 6, 8.5, 9.5)
        d.annulus('breeder_fitting', ROOF_TOP + 4, 6, 9.5, up=False)
        blocks('breeder_pump', MOTOR, MOTOR + B, 8.5, n=12)
        d.cylinder('glow', MOTOR + 12, MOTOR + 13, 8.7, n=12)
        d.annulus('breeder_fitting', MOTOR + B, 6, 8.5, n=12)
    with group('fan'):
        decal_disc('breeder_fan', x, z, MOTOR + B - 0.5, 6, 12)
# Intermediate heat exchangers (front right and back left): light columns two blocks high with
# two flanges and a domed head; the secondary loop leaves from their heads.
EXCHANGERS = [math.radians(a) for a in (315, 135)]
EX_TOP = ROOF_TOP + 2 * B
for phi in EXCHANGERS:
    x, z = on_circle(phi, PR)
    with centred(x, z):
        blocks('breeder_exchanger', ROOF_TOP, EX_TOP, 7, n=12)
        for y in (ROOF_TOP + 10, ROOF_TOP + 22):
            band(y, 2, 8.5, n=12)
            d.annulus('breeder_fitting', y + 2, 7, 8.5, n=12)
            d.annulus('breeder_fitting', y, 7, 8.5, up=False, n=12)
        d.lathe('breeder_fitting', [(EX_TOP, 7), (EX_TOP + 3, 6), (EX_TOP + 5, 3.5), (EX_TOP + 6, 0)], n=12)

# ---------------------------------------------------------------- the secondary loops: from each exchanger's head, over the edge, down the tank
# Hot and cold legs side by side, leaving the head sideways, over the roof's edge and down the
# rib gap to a flanged penetration on the plinth.
PIPE = 2.5
R_DOWN = ROOF_R + 2
for phi in EXCHANGERS:
    out = (math.cos(phi), 0, math.sin(phi))
    across = (-math.sin(phi), 0, math.cos(phi))
    for side in (-1, 1):
        off = side * 4
        y = EX_TOP - 4 - (side + 1) * 3
        pts = [(C + (PR + 7) * out[0] + off * across[0], y, C + (PR + 7) * out[2] + off * across[2]),
               (C + R_DOWN * out[0] + off * across[0], y, C + R_DOWN * out[2] + off * across[2]),
               (C + R_DOWN * out[0] + off * across[0], 20, C + R_DOWN * out[2] + off * across[2])]
        d.sweep('breeder_pipe', pts, PIPE, PIPE, lambda i: across, closed=False, caps=True, along_v=True)
        # Flanges at the head and the bend, and the penetration on the plinth.
        for p in pts[:2]:
            h = PIPE + 1
            d.box('breeder_fitting', p[0] - h, p[1] - h, p[2] - h, p[0] + h, p[1] + h, p[2] + h)
        f = pts[2]
        d.box('breeder_fitting', f[0] - 4, 16, f[2] - 4, f[0] + 4, 20, f[2] + 4)


# ---------------------------------------------------------------- the moving parts, posed
SHAFT_DROP = 5         # a SCRAM drops the shafts back into their caps


def turn(p, cx, cz, a):
    c, s = math.cos(a), math.sin(a)
    return (cx + (p[0] - cx) * c - (p[2] - cz) * s, p[1], cz + (p[0] - cx) * s + (p[2] - cz) * c)


def posed(large_deg, small_deg, dropped):
    """A copy of the design with its moving parts moved as the renderer will move them: the small
    plug (with the drives and shafts) turned on its own axis, then both plugs turned together on
    the large plug's; the shafts lowered when `dropped` (a SCRAM)."""
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
