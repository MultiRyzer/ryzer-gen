"""Shieldcell: an alternative microreactor, kept entirely in the concept folder.

Run from the repo root: python art/tools/microreactor_shieldcell_concept.py
Uses the existing renderer's coordinates and animation groups. Nothing is installed.
The viewing chamber is a gameplay cutaway of a shielded reactor, not a claim that
a small water tank provides real reactor shielding. The rear cabinet houses the
secondary heat exchanger and generator. Primary coolant stays in the front vessel.
"""
import math
import os

import microreactor_concept as base
import model_preview as mp
from pixelart import Tex, finish

OUT = os.path.join(mp.ROOT, 'art', 'concepts', 'microreactor_shieldcell')
PREFIX = 'ryzergen:block/microreactor_shieldcell/'


def panel(w, h, dark=False):
    t = Tex()
    t.rect(0, 0, 15, 15, 'b' if dark else 'F')
    t.rect(0, 0, w - 1, h - 1, 's' if dark else 'F')
    t.rect(0, 0, w - 1, 0, 'u' if dark else 'A')
    t.rect(0, 0, 0, h - 1, 'u' if dark else 'A')
    t.rect(w - 1, 0, w - 1, h - 1, 'b' if dark else 'J')
    t.rect(0, h - 1, w - 1, h - 1, 'b' if dark else 'J')
    if w >= 6 and h >= 6:
        for x, y in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
            t.set(x, y, 'J')
    return finish(t, w * 31 + h)


def own_textures():
    tex = {
        'shield': panel(2, 16), 'brow': panel(12, 3),
        'sill': panel(12, 4), 'side': panel(2, 14),
        'cabinet': panel(14, 14), 'lid': panel(14, 14, True),
    }
    # Reuse the Lantern's procedural materials without publishing into the mod.
    tex.update({name: fn() for name, fn in base.PUBLISHED.items()})
    for name, rows in tex.items():
        mp._tex_cache[PREFIX + name] = rows[:len(rows[0])]
        mp.write_png(os.path.join(OUT, 'textures', name + '.png'), rows)
    return {name: PREFIX + name for name in tex}


def design(materials, state='export'):
    # Retain the animated core, absorbers and water at the existing renderer's axes.
    d = base.design(state)
    retained = {'core_frame', 'core_rods', 'core_rods_spent', 'core_rods_hot',
                'rods', 'glass', 'glass_inner', 'water', 'water_far', 'bubbles', 'light'}
    d.quads = [q for q in d.quads if q[3] in retained]
    d.textures.update(materials)
    d.group = 'static'
    # Four-block footprint, with a framed shield enclosure around the vessel.
    d.box('trim', 0, 0, 0, 16, 1, 32, top='plinth')
    d.revolve(None, base.O, base.UP,
              [(1, 0), (1, 7.8), (7, 7.8), (8, 7.25), (8, 0)], n=24,
              mats=['pedestal', 'pedestal', 'floor_rim', 'floor_plate'])
    # Whole-pixel posts leave a clear ten-pixel window on the front.
    for x in (0, 14):
        d.box('shield', x, 8, 0, x + 2, 24, 2,
              decals={'north': 'shield', 'south': 'shield'})
        d.box('side', x, 8, 14, x + 2, 24, 16,
              decals={'north': 'shield', 'south': 'shield'})
    d.box('brow', 2, 24, 0, 14, 27, 2, decals={'north': 'brow'})
    d.box('sill', 2, 4, 0, 14, 8, 2, decals={'north': 'sill'})
    # Vessel head, recessed inside a square lifting frame.
    d.revolve(None, base.O, base.UP,
              [(25, 7.25), (26, 7.7), (27, 7.7), (28, 6.6), (28, 0)], n=24,
              mats=['lid_band', 'bolted', 'lid_rim', 'lid_plate'])
    for x in (0, 14):
        d.box('side', x, 24, 2, x + 2, 28, 16)
    # Two graphite carry handles, each with orange fixing lugs.
    for x in (1, 15):
        d.pipe('steel_dark', [(x, 28, 3), (x, 30, 3), (x, 30, 12), (x, 28, 12)], 0.45, n=8)
        for z in (3, 12):
            d.box('accent', x - 0.65, 27.8, z - 0.65, x + 0.65, 28.6, z + 0.65)
    for a in base.DRIVE_ANGLES:
        x = 8 + base.DRIVE_R * math.cos(math.radians(a))
        z = 8 + base.DRIVE_R * math.sin(math.radians(a))
        d.rod('steel_dark', (x, 28, z), base.UP, 2, 0.9, n=8)
        d.rod('steel', (x, 10, z), base.UP, 17, 0.22, n=6)
    # Rear plant: exactly centred flanges on each existing capability face.
    faces = {f: 'cabinet' for f in ('north', 'south', 'east', 'west')}
    for y in (1, 17):
        d.box('cabinet', 1, y, 17, 15, y + 14, 31, decals=faces, skip=('up', 'down'))
    for y in (15, 31):
        d.box('trim', 0.5, y, 16.5, 15.5, min(y + 2, 32), 31.5, top='lid')
    for x1, x2, side, material, y in (
        (15, 16, 'east', 'port_coolant', 3), (0, 1, 'west', 'port_steam', 3),
        (15, 16, 'east', 'port_fuel', 19), (0, 1, 'west', 'port_fuel', 19)):
        d.box('steel_dark', x1, y, 19, x2, y + 10, 29, decals={side: material})
    d.box('steel_dark', 3, 3, 31, 13, 13, 32, decals={'south': 'port_energy'})
    d.revolve('steel_dark', (8, 24, 31), base.Z,
              [(0, 5), (0.7, 5), (0.7, 4.3), (0.3, 4.3)], n=20)
    # Visible copper primary loop between vessel and heat exchanger.
    for x in (4, 12):
        d.pipe('pipe', [(x, 5, 14), (x, 5, 17)], 0.6, n=8)
        d.pipe('pipe', [(x, 26, 14), (x, 26, 17)], 0.45, n=8)
    # Framed status display and slim state-coloured strips.
    d.box('steel_dark', 5, 4.5, 0, 11, 6.5, 0.5)
    d.group = 'status'
    d.box('status', 5, 5, -0.01, 11, 6, 0.1, decals={'north': 'status'}, skip=('south',))
    d.group = 'strips'
    d.box('light_strip', 2, 23.6, 0.1, 14, 24, 0.5)
    d.box('light_strip', 2, 7.6, 0.1, 14, 8, 0.5)
    d.box('light_strip', 0.4, 15.8, 16.4, 15.6, 16.2, 31.6)
    d.group = 'fan'
    d.plate('fan', (8, 24, 31.35), base.Z, 8.6, 8.6)
    return d


def verify(d):
    required = {'static', 'strips', 'status', 'glass', 'glass_inner', 'core_frame',
                'core_rods', 'core_rods_spent', 'core_rods_hot', 'rods', 'fan'}
    assert required <= {q[3] for q in d.quads}
    for tex, vertices, normal, group in d.quads:
        assert tex in d.textures, tex
        mp.texture(d.textures[tex])
        assert all(math.isfinite(c) for v in vertices for c in v)
        assert all(0 <= v[0] <= 16 and 0 <= v[1] <= 32 and -0.02 <= v[2] <= 32 for v in vertices)
        assert abs(sum(c * c for c in normal) - 1) < 0.001
    print('Verified renderer groups, texture references, finite geometry and four-block bounds.')


def main():
    os.makedirs(os.path.join(OUT, 'textures'), exist_ok=True)
    materials = own_textures()
    exported = design(materials)
    verify(exported)
    exported.export(os.path.join(OUT, 'shieldcell.json'))
    cards = []
    for state in ('idle', 'running', 'overdrive', 'loss'):
        d = design(materials, state)
        base.preview_textures(d, state)
        front = d.render((1, 1), 12, 0.35)
        cards.append(front)
        mp.write_png(os.path.join(OUT, state + '.png'), front)
        if state == 'running':
            d.save_png(os.path.join(OUT, 'front_and_back.png'), [((1, 1), 18, 0.35), ((-1, -1), 18, 0.35)])
    # All states side by side, for comparing the visual feedback.
    h = max(len(c) for c in cards)
    widths = [len(c[0]) for c in cards]
    board = [[(40, 44, 52, 255)] * (sum(widths) + 48) for _ in range(h + 24)]
    left = 0
    for card, w in zip(cards, widths):
        for y, row in enumerate(card):
            board[y + 12][left:left + w] = row
        left += w + 16
    mp.write_png(os.path.join(OUT, 'states.png'), board)
    print('Preview only. Existing game resources and the Lantern generator are unchanged.')


if __name__ == '__main__':
    main()
