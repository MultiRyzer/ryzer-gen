"""The formed microreactor's design (design section 6), as the pool's and the container's are made.

One wide, two deep and two high, facing north: x 0 to 16 west to east, y 0 to 32 up, z 0 to 32
front to back. The reactor vessel stands at the front on a hazard skid, with its porthole and
console; the coolant section is behind it, its radiator fins on both flanks and the intake chute
on top. The ports stay where the game has them (MicroreactorPort): energy out on the back, coolant
in on top, steam out on the right, and fuel in and out on the vessel's flanks.

This script is the microreactor's one source: it exports the design in two states (idle and
running: the light strips and the porthole lit) to art/designs/microreactor.json for datagen
(DesignModel, one model per block and state), publishes the new textures it draws into
block/microreactor/, and renders a preview.

Run from the repo root:
    python art/tools/microreactor_concept.py [OUT.png] [--scale N] [--state idle|running]
"""
import sys

from pixelart import Tex, write_png
import model_preview as mp
import pool_concept as pc
from pool_concept import Box

LONG, HIGH, DEEP = 16, 32, 32
FOLDER = 'block/microreactor/'
STATES = ('idle', 'running')

# The textures the game already has for the microreactor (microreactor_textures.py).
EXISTING = {name: FOLDER + name for name in (
    'steel', 'steel_dark', 'lead', 'copper', 'hazard', 'glow', 'glow_off', 'accent', 'screen', 'porthole',
    'porthole_on', 'grille', 'port_energy', 'port_coolant', 'port_steam', 'port_fuel')}


def flange(w, h):
    """A bolted flange's edge at size: lit along the top with a bolt head every three pixels, and
    shaded below, where the clamp ring meets the vessel."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, 0, 'b')
    t.rect(0, 1, w - 1, h - 1, 'T')
    for x in range(1, w - 1, 3):
        t.set(x, 0, 'L')
        t.set(x, 1, 'U')
    return pc.rows(t)


def radiator(w, h):
    """A radiator at size: a graphite frame round a core of vertical copper fins, lit and shaded in
    turn. An odd number of fin columns, so both ends match."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'S')
    t.rect(0, 0, w - 1, 0, 'b')
    t.rect(0, h - 1, w - 1, h - 1, 'U')
    for x in range(1, w - 1):
        t.rect(x, 1, x, h - 2, 'e' if x % 2 else 'O')
    t.rect(1, 1, w - 2, 1, 'r')
    return pc.rows(t)


def head(w, h):
    """The vessel's closure head seen from above, at size: a flat gunmetal plate, bevelled, with a
    ring of bolts a pixel in from its edge."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'u')
    t.rect(0, 0, w - 1, 0, 's')
    t.rect(0, 0, 0, h - 1, 's')
    t.rect(w - 1, 0, w - 1, h - 1, 'z')
    t.rect(0, h - 1, w - 1, h - 1, 'z')
    for x in range(2, w - 2, 3):
        t.set(x, 1, 'L')
        t.set(x, h - 2, 'L')
    for y in range(4, h - 3, 3):
        t.set(1, y, 'L')
        t.set(w - 2, y, 'L')
    return pc.rows(t)


def housing(w, h):
    """The coolant housing's plating at size: a light casing plate with a bevel, a seam across its
    middle where two plates meet, and a bolt in each corner and at each end of the seam."""
    t = Tex(w, h)
    t.rect(0, 0, w - 1, h - 1, 'F')
    t.rect(0, 0, w - 1, 0, 'A')
    t.rect(0, 0, 0, h - 1, 'E')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    mid = h // 2
    t.rect(1, mid, w - 2, mid, 'I')
    t.rect(1, mid + 1, w - 2, mid + 1, 'A')
    for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3), (2, mid - 2), (w - 3, mid - 2)):
        t.set(x, y, 'J')
    return pc.rows(t)


def textures():
    tex = pc.textures()
    for name, path in EXISTING.items():
        tex[name] = mp.texture(path)
    made = {'flange': flange, 'housing': housing, 'radiator': radiator, 'head': head}
    base = type(tex).__missing__

    def missing(self, name):
        kind, _, size = name.partition('_')
        if kind in made:
            w, h = (int(v) for v in size.split('x'))
            self[name] = made[kind](w, h)
            return self[name]
        return base(self, name)

    type(tex).__missing__ = missing
    return tex


def design(state='running'):
    running = state == 'running'
    boxes = []

    def add(m, *c):
        b = Box(m, *c)
        boxes.append(b)
        return b

    def strip(*c):
        return add('glow', *c).glow() if running else add('glow_off', *c)

    def pipe(x1, y1, z1, x2, y2, z2):
        """A copper pipe along its longest side, each long face shaded round at its size."""
        b = add('copper', x1, y1, z1, x2, y2, z2)
        dx, dy, dz = round(x2 - x1), round(y2 - y1), round(z2 - z1)
        if dy >= max(dx, dz):
            for d, w in (('north', dx), ('south', dx), ('west', dz), ('east', dz)):
                b.decal(d, f'pipey_{w}x{dy}')
        else:
            for d, w in (('west', dz), ('east', dz)):
                b.decal(d, f'pipex_{w}x{dy}')
            b.decal('up', f'pipey_{dx}x{dz}')
        return b

    # Skid: hazard-striped base with a deck plate.
    add('steel_dark', 0, 0, 0, 16, 2, 32).sides('hazard')
    add('steel', 1, 2, 1, 15, 3, 31)
    # Corner clips holding the machine to the skid.
    for x in (0, 13):
        for z in (0, 29):
            add('accent', x, 2, z, x + 3, 5, z + 3)

    # Reactor vessel at the front: an octagonal lead column, three overlapping boxes.
    add('lead', 3, 3, 3, 13, 26, 15)
    add('lead', 2, 3, 5, 14, 26, 13)
    add('lead', 5, 3, 2, 11, 26, 16)
    # Bolted flanges clamping the vessel's sections together.
    for y in (9, 20):
        add('steel_dark', 1.5, y, 1.5, 14.5, y + 2, 15.5).decal('north', 'flange_13x2').decal('south', 'flange_13x2') \
            .decal('west', 'flange_14x2').decal('east', 'flange_14x2')
    # Light strips in the clamp rings and under the coolant housing's roof.
    strip(1.4, 9.75, 1.4, 14.6, 10.25, 15.6)
    strip(1.4, 20.75, 1.4, 14.6, 21.25, 15.6)
    strip(1.9, 22.25, 16.4, 14.1, 22.75, 30.1)
    # The closure head: a third bolted flange, a flat bolted plate on it, and in the middle the
    # control drive's housing with a light ring, lit while the reactor runs.
    add('steel_dark', 1.5, 26, 1.5, 14.5, 28, 15.5).decal('north', 'flange_13x2').decal('south', 'flange_13x2')         .decal('west', 'flange_14x2').decal('east', 'flange_14x2')
    add('lead', 2.5, 28, 2.5, 13.5, 29, 14.5).decal('up', 'head_11x12')
    add('steel_dark', 6, 29, 6.5, 10, 30, 10.5)
    strip(5.9, 30, 6.4, 10.1, 30.5, 10.6)
    add('steel_dark', 6, 30.5, 6.5, 10, 31.5, 10.5)
    # Porthole onto the core, and the control console below it.
    porthole = add('steel', 3, 11, 1, 13, 21, 2).decal('north', 'porthole_on' if running else 'porthole')
    if running:
        porthole.glow('north')
    add('steel_dark', 3, 3, 0, 13, 9, 3).decal('north', 'screen').glow('north')

    # Coolant section at the back: the housing, plated and bolted, with a radiator high on each
    # flank (clear of the steam port below it on the right), and the intake chute on top.
    add('steel', 2, 3, 16, 14, 23, 30).decal('west', 'housing_14x20').decal('east', 'housing_14x20') \
        .decal('south', 'housing_12x20')
    add('steel_dark', 1.5, 23, 16.5, 14.5, 24, 30.5)
    add('steel', 4, 16, 30, 12, 22, 30.5).decal('south', 'grille')
    add('steel_dark', 0.75, 14, 16.5, 2, 22, 29.5).decal('west', 'radiator_13x8')
    add('steel_dark', 14, 14, 16.5, 15.25, 22, 29.5).decal('east', 'radiator_13x8')
    # Coolant loop pipes from the housing into the vessel, low on each side.
    pipe(1, 4, 13, 3, 6, 20)
    pipe(13, 4, 13, 15, 6, 20)

    # Ports (see MicroreactorPort): 10 x 10 flanges flush with the block faces round 8 x 8 sockets.
    add('steel_dark', 4, 4, 30, 12, 12, 31)
    add('steel_dark', 3, 3, 31, 13, 13, 32).decal('south', 'port_energy')
    add('steel', 4, 24, 20, 12, 25, 28)
    pipe(5, 25, 21, 11, 31, 27)
    add('steel_dark', 3, 31, 19, 13, 32, 29).decal('up', 'port_coolant')
    add('steel_dark', 14, 4, 20, 15, 12, 28)
    add('steel_dark', 15, 3, 19, 16, 13, 29).decal('east', 'port_steam')
    add('steel_dark', 1, 4, 4, 2, 12, 12)
    add('steel_dark', 0, 3, 3, 1, 13, 13).decal('west', 'port_fuel')
    add('steel_dark', 14, 4, 4, 15, 12, 12)
    add('steel_dark', 15, 3, 3, 16, 13, 13).decal('east', 'port_fuel')
    return boxes


def export(tex):
    pc.export_design('microreactor', FOLDER, {state: design(state) for state in STATES}, tex,
                     (LONG // 16, HIGH // 16, DEEP // 16), existing=EXISTING)


def main():
    args = sys.argv[1:]
    scale, state = 12, 'running'
    for flag in ('--scale', '--state'):
        if flag in args:
            i = args.index(flag)
            if flag == '--scale':
                scale = int(args[i + 1])
            else:
                state = args[i + 1]
            del args[i:i + 2]
    tex = textures()
    export(tex)
    if not args:
        return
    faces = pc.faces_of(design(state), tex)
    views = [mp.render(faces, (1, 1), scale), mp.render(faces, (-1, -1), scale)]
    h = max(len(v) for v in views)
    out = []
    for y in range(h):
        row = []
        for v in views:
            row += v[y] if y < len(v) else [(40, 44, 52, 255)] * len(v[0])
        out.append(row)
    write_png(args[0], out)


if __name__ == '__main__':
    main()
