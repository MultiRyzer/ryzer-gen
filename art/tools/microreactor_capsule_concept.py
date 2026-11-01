"""Capsule microreactor: a fresh horizontal design, exported only as a concept.

Run: python art/tools/microreactor_capsule_concept.py
All geometry and textures are original to this concept. The existing game's
four-block footprint and port centres are preserved, but the moving parts need
different renderer transforms. Nothing is installed or changed in the mod.
"""
import json
import math
import os

import model_preview as mp
import pixelart as px
from quad_design import Design, smooth_glass

OUT = os.path.join(mp.ROOT, 'art', 'concepts', 'microreactor_capsule')
PREFIX = 'ryzergen:block/microreactor_capsule/'
UP, FRONT, BACK, EAST = (0, 1, 0), (0, 0, -1), (0, 0, 1), (1, 0, 0)
AXIS = (8, 16, 0)
COLOURS = {'idle': 'f', 'running': 'i', 'overdrive': 'a', 'loss': 'x'}


def material(fill, edge=None):
    t = px.Tex()
    t.rect(0, 0, 15, 15, fill)
    if edge:
        t.rect(0, 0, 15, 0, edge)
    return px.finish(t, ord(fill) * 113)


def panel(w, h, fill='F'):
    t = px.Tex()
    t.rect(0, 0, 15, 15, fill)
    t.rect(0, 0, w - 1, 0, 'A')
    t.rect(0, 0, 0, h - 1, 'A')
    t.rect(0, h - 1, w - 1, h - 1, 'J')
    t.rect(w - 1, 0, w - 1, h - 1, 'J')
    if w > 5 and h > 5:
        for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
            t.set(x, y, 'S')
    return px.finish(t, w * 131 + h)


def port(colour):
    t = px.Tex()
    t.rect(0, 0, 15, 15, 'T')
    t.rect(0, 0, 9, 9, colour)
    t.rect(1, 1, 8, 8, 'U')
    t.rect(2, 2, 7, 2, 'b')
    t.rect(2, 3, 2, 7, 'b')
    t.rect(3, 7, 7, 7, 'T')
    for x, y in ((0, 0), (9, 0), (0, 9), (9, 9)):
        t.set(x, y, 'H')
    return px.finish(t, ord(colour))


def label():
    t = px.Tex()
    t.rect(0, 0, 15, 15, 'U')
    # Readable physical diagram, no tiny pretend lettering.
    t.rect(1, 1, 2, 6, 'i')
    t.rect(4, 1, 10, 1, 'E')
    t.rect(4, 3, 8, 3, 's')
    t.rect(4, 5, 11, 5, 's')
    t.rect(13, 1, 14, 2, 'n')
    return [[t.rgba(x, y) for x in range(16)] for y in range(16)]


def screen():
    t = px.Tex()
    t.rect(0, 0, 15, 15, 'U')
    t.rect(1, 1, 14, 1, 'f')
    for x, h in ((2, 3), (4, 6), (6, 8), (8, 5), (10, 9), (12, 7)):
        t.rect(x, 12 - h, x, 12, 'i')
    t.rect(2, 14, 12, 14, 'f')
    return [[t.rgba(x, y) for x in range(16)] for y in range(16)]


def textures():
    data = {
        'casing': material('F'), 'rim': material('E'),
        'vessel': material('u'), 'graphite': material('T'),
        'steel': material('H'), 'copper': material('R'),
        'fin': material('r', 'e'), 'orange': material('X'),
        'black': material('U'), 'panel14': panel(14, 14),
        'panel10': panel(10, 10), 'label': label(), 'screen': screen(),
        'glass': smooth_glass(),
        'energy': port('x'), 'water': port('G'), 'steam': port('E'),
        'fuel': port('X'), 'rod': material('s', 'H'),
    }
    for state, colour in COLOURS.items():
        data['light_' + state] = material(colour)
    os.makedirs(os.path.join(OUT, 'textures'), exist_ok=True)
    for name, rows in data.items():
        mp._tex_cache[PREFIX + name] = rows
        mp.write_png(os.path.join(OUT, 'textures', name + '.png'), rows)
    return {name: PREFIX + name for name in data}


def ring(d, mat, z, radius, width, length=0.7):
    d.revolve(mat, AXIS, BACK,
              [(z, radius - width), (z, radius), (z + length, radius),
               (z + length, radius - width)], n=24)


def design(tex, state='running'):
    light = 'light_' + state
    d = Design(8, dict(tex), {light, 'screen'})
    # Twin skid rails and two thick saddles, visibly carrying the drum.
    for x in (0, 13):
        d.box('graphite', x, 0, 0, x + 3, 2, 32)
        for z in (2, 27):
            d.box('orange', x + 0.5, 1.5, z, x + 2.5, 2.5, z + 2)
    for z in (5, 19):
        d.box('vessel', 2, 2, z, 14, 6, z + 3)
        d.box('graphite', 0.5, 6, z, 3, 12, z + 3)
        d.box('graphite', 13, 6, z, 15.5, 12, z + 3)
    # A low secondary exchanger, separated from the sealed primary vessel.
    d.revolve('copper', (8, 6, 3), BACK,
              [(0, 0), (0, 2.6), (1, 3), (19, 3), (20, 2.6), (20, 0)], n=16)
    for z in (4, 9, 14, 19):
        d.revolve('graphite', (8, 6, 0), BACK,
                  [(z, 3), (z + 0.7, 3)], n=16)
    # Horizontal pressure shell, with dished shoulders and a hollow inspection head.
    d.revolve(None, AXIS, BACK,
              [(2, 3.6), (2, 5.3), (4, 6.4), (12, 6.4),
               (13, 6.4), (21, 6.4), (24, 5.2), (25, 0)], n=24,
              mats=['rim', 'casing', 'casing', 'graphite', 'casing', 'casing', 'vessel'])
    # Inside the inspection head, only a small sealed fuel bundle is visible.
    d.revolve('black', AXIS, BACK, [(2, 3.6), (7, 3.6), (7, 0)], n=24)
    d.group = 'core'
    for k in range(7):
        a = k * math.pi / 3
        x, y = (8, 16) if k == 6 else (8 + 1.7 * math.cos(a), 16 + 1.7 * math.sin(a))
        d.rod('rod', (x, y, 3), BACK, 3.8, 0.6, n=8)
    d.group = 'static'
    ring(d, 'graphite', 0.8, 5.15, 1.7, 1.15)
    ring(d, 'copper', 1.2, 5.3, 0.4, 0.6)
    # Latch dogs and bolts around a recognisable sealed front hatch.
    for k in range(8):
        a = 2 * math.pi * k / 8
        x, y = 8 + 4.5 * math.cos(a), 16 + 4.5 * math.sin(a)
        d.rod('steel', (x, y, 0.7), BACK, 0.45, 0.28, n=6)
    for x in (2.2, 12.6):
        d.box('orange', x, 15, 0.5, x + 1.2, 17, 1.8)
    # Two long straps bind the pressure body into its cradle.
    for z in (5, 19):
        ring(d, 'graphite', z, 6.8, 0.45, 1)
    d.group = 'status'
    ring(d, light, 0.6, 3.6, 0.18, 0.15)
    ring(d, light, 12, 6.48, 0.08, 0.4)
    d.group = 'glass'
    d.revolve('glass', AXIS, BACK, [(0.9, 3.35), (0.9, 0)], n=24)
    d.group = 'static'
    # An asymmetric radiator cassette: solid copper fins, a graphite guard and white cap.
    d.box('graphite', 0.8, 22, 5, 5.2, 24, 18)
    d.pipe('copper', [(2, 20.8, 7), (2, 29.5, 7), (2, 29.5, 16), (2, 21, 16)], 0.55, n=8)
    for z in range(6, 18, 2):
        d.box('fin', 1.1, 24, z, 4.9, 29.5, z + 0.65)
    d.box('rim', 0.5, 29.5, 4.5, 5.5, 30.5, 18.5)
    for z in (5, 18):
        d.box('graphite', 0.5, 23.5, z, 1.1, 29.5, z + 0.5)
    # Offset instruments over the opposite shoulder, with an angled emissive screen.
    d.box('vessel', 10, 21, 17, 14, 25, 23)
    d.box('casing', 9, 25, 17, 15, 29, 27)
    d.box('graphite', 8.5, 29, 16.5, 15.5, 30, 27.5)
    d.quad('screen', [(9.5, 29, 17), (9.5, 26, 16.5), (14.5, 26, 16.5), (14.5, 29, 17)],
           [(0, 0), (0, 16), (16, 16), (16, 0)], (0, 0.16, -1))
    d.box('orange', 13, 30, 22, 14, 31, 23)
    # Rear service spine, with exactly the existing lower and upper port centres.
    d.box('graphite', 0.5, 15, 16.5, 15.5, 17, 31.5)
    d.box('panel14', 1, 17, 17, 15, 31, 31,
          decals={'east': 'panel14', 'west': 'panel14', 'south': 'panel14'}, skip=('north',))
    d.box('panel14', 1, 1, 17, 15, 15, 31,
          decals={'east': 'panel14', 'west': 'panel14', 'south': 'panel14'}, skip=('north', 'up'))
    d.box('graphite', 1, 31, 17, 15, 32, 31, top='panel14')
    for x1, x2, face, mat, y in ((15, 16, 'east', 'water', 3),
                                (0, 1, 'west', 'steam', 3),
                                (15, 16, 'east', 'fuel', 19),
                                (0, 1, 'west', 'fuel', 19)):
        d.box('graphite', x1, y, 19, x2, y + 10, 29, decals={face: mat})
    d.box('graphite', 3, 3, 31, 13, 13, 32, decals={'south': 'energy'})
    # Generator exhaust vanes with a separately exported fan rotor.
    d.revolve('graphite', (8, 24, 31), BACK,
              [(0, 3.8), (0.8, 3.8), (0.8, 3.1), (0, 3.1)], n=16)
    d.group = 'fan'
    d.rod('steel', (8, 24, 31.1), BACK, 0.3, 0.6, n=8)
    for k in range(5):
        a = k * 2 * math.pi / 5
        p = [(8 + r * math.cos(a + shift), 24 + r * math.sin(a + shift), 31.3)
             for r, shift in ((0.5, 0), (2.9, 0.15), (2.9, 0.52), (0.5, 0.8))]
        d.quad('vessel', p, [(0, 0), (0, 16), (16, 16), (16, 0)], BACK)
    d.group = 'static'
    # External primary loop and a visible valve, separate from the radiator's secondary circuit.
    d.pipe('copper', [(14, 14, 5), (15, 14, 5), (15, 10, 5), (15, 10, 17)], 0.48, n=8)
    d.rod('graphite', (15, 10, 12), UP, 1.4, 0.3, n=8)
    d.revolve('orange', (15, 11.4, 12), UP, [(0, 0.6), (0, 1), (0.4, 1), (0.4, 0.6)], n=12)
    # Small identification plate and a guarded coolant sight tube.
    d.box('graphite', 4, 3.5, 2.1, 12, 6.5, 2.6)
    d.box('label', 4, 3.5, 2, 12, 6.5, 2.1, decals={'north': 'label'}, skip=('south',))
    d.box('graphite', 14.5, 12, 13, 15.3, 20, 15)
    d.group = 'coolant_level'
    level = 14 if state == 'loss' else 19
    d.box('light_running', 15.31, 12.5, 13.5, 15.5, level, 14.5)
    d.group = 'status'
    d.box(light, 1, 31, 23, 15, 31.5, 24)
    return d


def validate(d):
    for mat, verts, normal, group in d.quads:
        assert mat in d.textures
        mp.texture(d.textures[mat])
        assert all(math.isfinite(v) for p in verts for v in p)
        assert all(0 <= p[0] <= 16 and 0 <= p[1] <= 32 and 0 <= p[2] <= 32 for p in verts), (group, verts)
        assert abs(sum(v * v for v in normal) - 1) < 0.001
    print('Geometry, texture references, normals and four-block bounds verified.')


def main():
    tex = textures()
    d = design(tex)
    validate(d)
    d.export(os.path.join(OUT, 'capsule.json'))
    d.save_png(os.path.join(OUT, 'front_and_back.png'), [((1, 1), 19, 0.4), ((-1, -1), 19, 0.4)])
    d.save_png(os.path.join(OUT, 'front.png'), [((0.4, 1), 25, 0.3)])
    # Show one side low enough to read the horizontal vessel silhouette.
    d.save_png(os.path.join(OUT, 'side.png'), [((1, 0.35), 22, 0.25)])
    cards = []
    for state in COLOURS:
        cards.append(design(tex, state).render((1, 1), 10, 0.4))
    h = max(len(c) for c in cards)
    board = [[(40, 44, 52, 255)] * (sum(len(c[0]) for c in cards) + 48) for _ in range(h)]
    left = 0
    for c in cards:
        for y, row in enumerate(c):
            board[y][left:left + len(row)] = row
        left += len(c[0]) + 16
    mp.write_png(os.path.join(OUT, 'states.png'), board)
    with open(os.path.join(OUT, 'animation_layout.json'), 'w') as f:
        json.dump({'units': 'design pixels', 'footprint': [16, 32, 32],
                   'core': {'axis': [0, 0, 1], 'origin': [8, 16, 3]},
                   'fan': {'axis': [0, 0, 1], 'origin': [8, 24, 31.3]},
                   'coolant_level': {'min_y': 12.5, 'max_y': 19},
                   'status': 'cyan running, amber overdrive, red coolant loss, dim idle',
                   'integration': 'New renderer transforms required. Preview only.'}, f, indent=2)


if __name__ == '__main__':
    main()
