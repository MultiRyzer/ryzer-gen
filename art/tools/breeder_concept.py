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


PAD_H = 4


def pad(x, z, y, r_in, r, bolts=8, n=16):
    """A raised pad seating a machine in the roof (or a pipe in the plinth), as real roof
    penetrations are: graphite in the deck's colour, a band round its edge, and a ring of bolts,
    so the machine rises out of the roof rather than standing on it."""
    with centred(x, z):
        band(y, PAD_H, r, n=n)
        d.annulus('breeder_graphite', y + PAD_H, r_in, r, n=n)
    for k in range(bolts):
        bx, bz = x + (r - 1.6) * math.cos(2 * math.pi * (k + 0.5) / bolts), z + (r - 1.6) * math.sin(2 * math.pi * (k + 0.5) / bolts)
        d.box('breeder_fitting', bx - 0.5, y + PAD_H, bz - 0.5, bx + 0.5, y + PAD_H + 0.5, bz + 0.5, skip=('down',))


def norm3(v):
    length = math.sqrt(sum(c * c for c in v))
    return tuple(c / length for c in v)


def tube(mat, path, r, across, sides=8, caps=False):
    """A round pipe along `path` (points), turning in the plane square to `across`: rings of
    `sides` round each point, the texture wrapped once round the pipe and laid along it a pixel
    per pixel, split so no piece is longer than a block."""
    pts = [path[0]]
    for a, b in zip(path, path[1:]):
        length = math.dist(a, b)
        pieces = max(1, math.ceil(length / B))
        pts += [tuple(a[i] + (b[i] - a[i]) * (k + 1) / pieces for i in range(3)) for k in range(pieces)]
    tangents = []
    for i in range(len(pts)):
        a, b = pts[max(0, i - 1)], pts[min(len(pts) - 1, i + 1)]
        tangents.append(norm3(tuple(b[k] - a[k] for k in range(3))))

    def ring(i):
        t = tangents[i]
        up = norm3((across[1] * t[2] - across[2] * t[1], across[2] * t[0] - across[0] * t[2], across[0] * t[1] - across[1] * t[0]))
        out = []
        for k in range(sides + 1):
            th = 2 * math.pi * k / sides
            dirv = tuple(math.cos(th) * across[j] + math.sin(th) * up[j] for j in range(3))
            out.append((tuple(pts[i][j] + r * dirv[j] for j in range(3)), dirv))
        return out

    rings = [ring(i) for i in range(len(pts))]
    for i in range(len(pts) - 1):
        v = min(16, math.dist(pts[i], pts[i + 1]))
        for k in range(sides):
            (p0, n0), (p1, n1) = rings[i][k], rings[i][k + 1]
            (q0, _), (q1, _) = rings[i + 1][k], rings[i + 1][k + 1]
            u0, u1 = 16 * k / sides, 16 * (k + 1) / sides
            d.quad(mat, [p0, q0, q1, p1], [(u0, 0), (u0, v), (u1, v), (u1, 0)], norm3(tuple(n0[j] + n1[j] for j in range(3))))
    if caps:
        for i, sign in ((0, -1), (len(pts) - 1, 1)):
            t = tangents[i]
            c = pts[i]
            for k in range(sides):
                (p0, _), (p1, _) = rings[i][k], rings[i][k + 1]
                d.quad(mat, [c, c, p1, p0], [(8, 8), (8, 8), (16, 16), (16, 0)], tuple(sign * t[j] for j in range(3)))


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
    pad(FX, FZ, PLUG_TOP, 4.5, 7, bolts=6, n=12)
    with centred(FX, FZ):
        d.cylinder('breeder_exchanger', PLUG_TOP + PAD_H, PLUG_TOP + PAD_H + B, 4.5, n=12)
        d.annulus('breeder_fitting', PLUG_TOP + PAD_H + B, 0, 4.5, n=12)
    d.box('breeder_lug', FX - 5.5, PLUG_TOP + PAD_H + B, FZ - 1, FX + 5.5, PLUG_TOP + PAD_H + B + 1, FZ + 1)
with group('plug_small'):
    with centred(SX, SZ):
        band(PLUG_TOP, 4, SMALL_R)
        d.annulus('breeder_plug', SMALL_TOP, 0, SMALL_R)
    # Seven control rod drives: housings a block high, a cyan light, a cap.
    DRIVES = [(0, 0)] + [(9 * math.cos(math.radians(a)), 9 * math.sin(math.radians(a))) for a in range(30, 390, 60)]
    for dx, dz in DRIVES:
        with centred(SX + dx, SZ + dz):
            band(SMALL_TOP, 2, 3.6, n=8)
            d.annulus('breeder_fitting', SMALL_TOP + 2, 2.5, 3.6, n=8)
            blocks('breeder_drive', SMALL_TOP, DRIVE_TOP, 2.5, n=8)
            d.cylinder('glow', SMALL_TOP + 8, SMALL_TOP + 9, 2.7, n=8)
            d.disc('breeder_fitting', DRIVE_TOP, DRIVE_TOP + 3, 3.2, n=8)
with group('shafts'):
    # The drive shafts, drawn raised (reactor running): polished rods out of the caps.
    for dx, dz in DRIVES:
        with centred(SX + dx, SZ + dz):
            d.disc('breeder_shaft', DRIVE_TOP + 3, DRIVE_TOP + 8, 1, n=6, bottom=False)

# ---------------------------------------------------------------- pumps and heat exchangers, seated in the roof
PR = TANK_R - 10       # their circle, clear of the large plug and inside the roof's edge
SEAT = ROOF_TOP + PAD_H
# Primary pumps (front left and back right as you face it): a finned motor a block high with one
# fin per panel, rising out of its pad, and the fan across its top.
PUMPS = [math.radians(a) for a in (225, 45)]
for phi in PUMPS:
    x, z = on_circle(phi, PR)
    pad(x, z, ROOF_TOP, 8.5, 12)
    with centred(x, z):
        blocks('breeder_pump', SEAT, SEAT + B, 8.5, n=12)
        d.cylinder('glow', SEAT + 12, SEAT + 13, 8.7, n=12)
        d.annulus('breeder_fitting', SEAT + B, 6, 8.5, n=12)
    with group('fan'):
        decal_disc('breeder_fan', x, z, SEAT + B - 0.5, 6, 12)
# Intermediate heat exchangers (front right and back left): light columns two blocks high, rising
# out of their pads, with two flanges and a domed head; the secondary loop leaves from a nozzle
# near the head.
EXCHANGERS = [math.radians(a) for a in (315, 135)]
EX_R = 7
EX_TOP = SEAT + 2 * B
for phi in EXCHANGERS:
    x, z = on_circle(phi, PR)
    pad(x, z, ROOF_TOP, EX_R, 11)
    with centred(x, z):
        blocks('breeder_exchanger', SEAT, EX_TOP, EX_R, n=12)
        for y in (SEAT + 10, SEAT + 22):
            band(y, 2, EX_R + 1.5, n=12)
            d.annulus('breeder_fitting', y + 2, EX_R, EX_R + 1.5, n=12)
            d.annulus('breeder_fitting', y, EX_R, EX_R + 1.5, up=False, n=12)
        d.lathe('breeder_fitting', [(EX_TOP, EX_R), (EX_TOP + 3, 6), (EX_TOP + 5, 3.5), (EX_TOP + 6, 0)], n=12)

# ---------------------------------------------------------------- the secondary loops: round pipes from each exchanger, over the edge, down the tank
# Hot and cold legs side by side. Each leaves the exchanger through a flanged nozzle, runs out over
# the roof's edge, turns down on a smooth bend, runs down the rib gap and enters the plinth through
# a collar in a pad.
PIPE = 2.5
BEND = 6
R_DOWN = ROOF_R + 3
FOOT = 16 + PAD_H
for phi in EXCHANGERS:
    out = (math.cos(phi), 0, math.sin(phi))
    across = (-math.sin(phi), 0, math.cos(phi))

    def at(rad, y, off):
        return (C + rad * out[0] + off * across[0], y, C + rad * out[2] + off * across[2])

    for side in (-1, 1):
        off = side * 3.5
        y = EX_TOP - 5 if side < 0 else EX_TOP - 13
        start = PR + EX_R - 0.5
        path = [at(start, y, off), at(R_DOWN - BEND, y, off)]
        for k in range(1, 7):
            a = math.radians(90 * k / 6)
            path.append(at(R_DOWN - BEND + BEND * math.sin(a), y - BEND + BEND * math.cos(a), off))
        path.append(at(R_DOWN, FOOT, off))
        tube('breeder_pipe', path, PIPE, across)
        # The nozzle's flange on the exchanger, a collar where the pipe enters the plinth, and
        # that collar's pad.
        tube('breeder_fitting', [at(start + 1, y, off), at(start + 2.5, y, off)], PIPE + 1, across, caps=True)
        tube('breeder_fitting', [at(R_DOWN, FOOT, off), at(R_DOWN, FOOT + 2, off)], PIPE + 1, across, caps=True)
    fx, _, fz = at(R_DOWN, 0, 0)
    pad(fx, fz, 16, 0, 8, bolts=6, n=12)


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
