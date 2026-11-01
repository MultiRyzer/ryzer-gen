"""Square microreactor concept. Run from the repo with Python, preview only.

The 32 x 32 pixel footprint is two blocks wide and two blocks deep.
Geometry and textures are exported beside the previews, never into the mod.
"""
import json
import math
import os

import microreactor_capsule_concept as materials
import model_preview as mp
from quad_design import Design

OUT = os.path.join(mp.ROOT, 'art', 'concepts', 'microreactor_square')
UP = (0, 1, 0)
FRONT = (0, 0, -1)


def design(tex, state='running'):
    light = 'light_' + state
    d = Design(16, tex, {light, 'screen'})
    # Four individual deck plates make the square footprint legible.
    d.box('graphite', 0, 0, 0, 32, 2, 32)
    for x in (1, 17):
        for z in (1, 17):
            d.box('casing', x, 2, z, x + 14, 3, z + 14,
                  decals={'up': 'panel14'})
    for x in (1, 28):
        for z in (1, 28):
            d.box('graphite', x, 3, z, x + 3, 5, z + 3)
            d.box('orange', x + 0.5, 5, z + 0.5, x + 2.5, 5.5, z + 2.5)
    # A wide pressure vessel nested in a shallow shielding bowl.
    d.lathe('graphite', [(3, 0), (3, 10), (5, 12), (7, 12)], n=32)
    d.lathe('vessel', [(6, 10.5), (8, 11), (20, 11), (23, 9)], n=32)
    d.lathe('casing', [(5, 12), (7, 13), (10, 13), (11, 12)], n=32)
    d.annulus('rim', 11, 11, 12, n=32)
    # Four structural ribs support the bolted lid, leaving the dark vessel visible.
    for x in (6, 24):
        for z in (7, 23):
            d.box('graphite', x - 1, 10, z - 1, x + 1, 24, z + 1)
            d.box('casing', x - 1.5, 12, z - 1.5, x + 1.5, 22, z + 1.5)
            d.box('orange', x - 1.5, 22, z - 1.5, x + 1.5, 23, z + 1.5)
    # Broad stepped lid with recessed central fuel-loading hatch.
    d.lathe('graphite', [(23, 9), (23, 12.5), (25, 12.5)], n=32)
    d.lathe('lid', [(25, 12.5), (27, 11), (27, 5.5)], n=32)
    d.lathe('graphite', [(27, 5.5), (26, 5.5), (26, 0)], n=24)
    d.lathe('lid', [(26, 0), (26, 4.5), (27, 4.5), (28, 3.5), (28, 0)], n=24)
    d.group = 'status'
    d.cylinder(light, 24.7, 25.1, 12.55, n=32)
    d.group = 'static'
    for k in range(12):
        a = k * math.tau / 12
        d.rod('steel', (16 + 10.5 * math.cos(a), 27, 16 + 10.5 * math.sin(a)),
              UP, 0.6, 0.45, n=6)
    # Low bridge handle on the removable hatch.
    for x in (12, 19):
        d.box('graphite', x, 28, 15, x + 1, 30, 17)
    d.box('copper', 12, 30, 15, 20, 31, 17)
    # A small protected instrument window, rather than a glass reactor wall.
    axis = (16, 17, 0)
    d.revolve('graphite', axis, FRONT,
              [(-6, 0), (-6, 4), (-3.5, 4), (-3.5, 2.8), (-5, 2.8)], n=24)
    d.group = 'status'
    d.revolve(light, axis, FRONT, [(-3.45, 2.8), (-3.45, 2.5)], n=24)
    d.group = 'core'
    for x in (14.5, 16, 17.5):
        d.rod('rod', (x, 15.5, 4), UP, 3, 0.4, n=8)
    d.group = 'glass'
    d.revolve('glass', axis, FRONT, [(-3.4, 2.5), (-3.4, 0)], n=24)
    d.group = 'static'
    for x in (12, 19):
        d.box('orange', x, 16, 3.1, x + 1, 18, 4.2)
    # Twin cooling cassettes flank the vessel. Copper fin packs remain exposed.
    for x in (1, 27):
        d.box('graphite', x, 6, 9, x + 4, 8, 23)
        d.box('graphite', x, 20, 9, x + 4, 22, 23)
        for y in range(9, 20, 2):
            d.box('fin', x, y, 10, x + 4, y + 0.7, 22)
        d.pipe('copper', [(x + 2, 8, 10), (x + 2, 23, 10),
                          (x + 2, 23, 22), (x + 2, 8, 22)], 0.6, n=8)
    # Low front control console leaves the vessel and lid in clear view.
    d.box('graphite', 3, 3, 1, 11, 10, 6)
    d.box('casing', 2, 9, 1, 12, 10, 6)
    d.quad('screen', [(3, 12, 5), (3, 10, 0.8), (11, 10, 0.8), (11, 12, 5)],
           [(0, 0), (0, 16), (16, 16), (16, 0)], (0, 1, -0.5))
    d.box('orange', 10, 12, 4, 11, 12.7, 5)
    # Flush connections occupy separate left input and right output blocks.
    for x, mat in ((3, 'water'), (19, 'steam')):
        d.box('graphite', x - 1, 3, 25, x + 11, 15, 31)
        d.box('graphite', x, 3, 31, x + 10, 13, 32, decals={'south': mat})
    for x, mat in ((3, 'fuel'), (19, 'fuel')):
        d.box('casing', x - 1, 17, 26, x + 11, 29, 31)
        d.box('graphite', x, 19, 31, x + 10, 29, 32, decals={'south': mat})
    d.box('graphite', 31, 3, 3, 32, 13, 13, decals={'east': 'energy'})
    # Heavy side cable conduit links the generator section to its energy socket.
    d.pipe('graphite', [(30, 8, 8), (30, 5, 8), (24, 5, 8)], 0.8, n=8)
    return d


def main():
    materials.OUT = OUT
    materials.PREFIX = 'ryzergen:block/microreactor_square/'
    tex = materials.textures()
    # A quiet surface avoids stretching conspicuous grain across narrow lid wedges.
    rows = materials.material('F')
    base = rows[8][8]
    rows = [[tuple(round(base[i] + (p[i] - base[i]) * 0.15) for i in range(3)) + (255,)
             for p in row] for row in rows]
    tex['lid'] = materials.PREFIX + 'lid'
    mp._tex_cache[tex['lid']] = rows
    mp.write_png(os.path.join(OUT, 'textures', 'lid.png'), rows)
    d = design(tex)
    for mat, verts, normal, group in d.quads:
        assert mat in tex
        assert all(math.isfinite(v) for p in verts for v in p)
        assert all(0 <= p[i] <= 32 for p in verts for i in range(3)), (group, verts)
        assert abs(sum(v * v for v in normal) - 1) < 0.001
    d.export(os.path.join(OUT, 'square.json'))
    d.save_png(os.path.join(OUT, 'front.png'), [((1, 1), 20, 0.55)])
    d.save_png(os.path.join(OUT, 'front_and_back.png'),
               [((1, 1), 15, 0.55), ((-1, -1), 15, 0.55)])
    d.save_png(os.path.join(OUT, 'footprint.png'), [((1, 1), 17, 0.92)])
    with open(os.path.join(OUT, 'layout.json'), 'w') as f:
        json.dump({'bounds_pixels': [32, 32, 32], 'footprint_blocks': [2, 2],
                   'height_blocks': 2, 'preview_only': True,
                   'integration': 'Requires a new formation layout and port mapping.'}, f, indent=2)
    print('Verified finite geometry, unit normals, texture references and 2x2x2 bounds.')


if __name__ == '__main__':
    main()
