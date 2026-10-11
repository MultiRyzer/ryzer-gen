"""Generates the ore blocks, raw drops, gems and ingots.

Built on vanilla textures, as most mods do, so they sit naturally in today's game:
- Ores: vanilla stone or deepslate, with each mineral painted in from crystal pieces of its own real
  habit (pitchblende bubbles crusted with autunite, stepped galena cubes, wire silver, bauxite
  pebbles, fluorite cubes, halite cubes, wolframite blades with scheelite, monazite prisms), placed in
  clumps that wrap across the edges, the rock round them tinted by the host (11 Oct 2026, after
  vanilla's modern ores; the earlier round blobs read as stickers).
- Ingots, raw drops and gems: the vanilla shape and shading, gradient-mapped onto each material's own
  colour ramp. Vanilla's grey levels are sorted by brightness and each one takes the matching tone.

Vanilla textures are read from the Minecraft jar at run time (see pixelart.vanilla), never copied in.
Run from the repo root after one Gradle build:
    python art/tools/material_textures.py
"""
from pixelart import *  # noqa: F401,F403

# The ores are painted from crystal pieces, each drawn once for a mineral habit and lit from the top
# left: ' ' leaves what is below, '1' to '5' the mineral from its darkest to its glint, 'a' 'b' 'c' a
# second mineral (dark to light). Each ore places its pieces by hand into a few clumps of mixed
# sizes, all inside the block's outer ring of pixels: that ring stays plain rock, as on vanilla's
# ores, so an ore block melts into the stone round it. The rock round every crystal takes its host's
# hue at its own brightness (a ring a pixel out, and half as much a ring further), and the rock just
# below and right of a crystal is shaded (ORE_SHADOW), as if the crystal stood proud of it.
ORE_SHADOW = 0.3
PIECES = {
    # Rounded lumps, lit from the top left, a glint on the light side.
    'ball4': [' 43 ', '4532', '3321', ' 21 '],
    'ball3': ['54 ', '432', ' 21'],
    'ball2': ['43', '21'],
    'dot': ['3'],
    'glint': ['4'],
    # Cubes: a lit top face, the left face in body, the right in shade.
    'cube4': ['5444', '4333', '4322', '3211'],
    'cube3': ['544', '432', '321'],
    'cube2': ['54', '31'],
    # Galena's stepped cube: two cubes grown together.
    'step': ['544  ', '4332 ', '43544', '32432', ' 1321'],
    # Bladed crystals, two pixels thick, lit along the top edge.
    'blade6': ['544443', '322221'],
    'blade4': ['5443', '3221'],
    'blade5d': ['54   ', '3443 ', ' 3244', '  321'],
    'bladev': ['54', '43', '43', '32', '21'],
    # Stubby prisms, a pointed end.
    'prism': [' 5 ', '544', '433', '432', '321', ' 1 '],
    'prism_s': ['54', '43', '32', '21'],
    'prism_l': ['  54', ' 543', '5432', '4321', '321 '],
    # Crust plates of the second mineral.
    'crust3': ['cb ', 'bba', ' a '],
    'crust2': ['cb', 'ba'],
    'crust1': ['c'],
    'flake': ['b'],
}


# Each ore: its five mineral tones (dark to glint), a second mineral's three if it has one, the
# host rock's tint and how strongly it takes, its pieces (name, x, y) and, for wire silver, wires
# (paths of points, lit along them with tarnish beneath and right).
ORES = {
    # Pitchblende (black, greasy, in bubbly masses) crusted with autunite (lime-yellow plates).
    'uranium': {
        'tones': ['0c0e09', '1b2015', '2d3322', '4a5338', '9aa878'],
        'second': ['5a8412', 'a6d630', 'eeff8a'],
        'halo': '1e2a12', 'halo_amount': 0.25,
        'pieces': [
            ('ball4', 2, 3), ('ball3', 5, 5), ('ball2', 1, 7), ('crust3', 1, 2), ('crust2', 5, 3),
            ('crust1', 7, 6), ('crust2', 4, 8), ('flake', 1, 6), ('ball4', 10, 10), ('ball3', 8, 12),
            ('crust2', 9, 9), ('crust1', 13, 10), ('crust3', 12, 12), ('ball2', 11, 2),
            ('crust1', 13, 3), ('crust1', 4, 13), ('dot', 5, 12),
        ],
    },
    # Galena: stepped cubes, metallic blue-grey, bright glints on the lit corners.
    'lead': {
        'tones': ['191c25', '363e50', '5b667c', '96a2b8', 'eef3fa'],
        'second': None,
        'halo': '1c2130', 'halo_amount': 0.34,
        'pieces': [
            ('step', 1, 2), ('cube2', 5, 5), ('cube2', 1, 8), ('cube4', 10, 9), ('cube3', 12, 12),
            ('cube2', 8, 12), ('cube3', 11, 1), ('cube2', 13, 4), ('cube2', 4, 12), ('glint', 6, 13),
        ],
    },
    # Native silver: wires branching through the rock, bright where lit, black tarnish beneath.
    'silver': {
        'tones': ['262320', '68625a', 'a8a299', 'e0dbd2', 'ffffff'],
        'second': None,
        'halo': '2a2622', 'halo_amount': 0.26,
        'wires': [
            [(1, 4), (2, 4), (3, 5), (4, 5), (5, 6), (6, 6), (7, 7), (7, 8), (8, 9)],
            [(4, 5), (4, 4), (5, 3), (5, 2), (6, 1)],
            [(6, 6), (7, 5), (8, 5)],
            [(9, 13), (10, 12), (11, 12), (12, 11), (12, 10), (13, 9)],
            [(12, 11), (13, 12), (13, 13)],
            [(10, 2), (11, 3), (12, 3)],
            [(2, 13), (3, 12), (4, 12), (5, 11)],
        ],
        'pieces': [('ball2', 7, 8), ('glint', 9, 9)],
    },
    # Bauxite: round red-brown pisoliths, many sizes, in rusty rock.
    'aluminium': {
        'tones': ['2a1008', '58220e', '8a3c1a', 'b45e2e', 'e2986c'],
        'second': None,
        'halo': '6a2c14', 'halo_amount': 0.12,
        'pieces': [
            ('ball4', 1, 2), ('ball3', 5, 4), ('ball2', 2, 6), ('ball2', 6, 1), ('dot', 4, 7),
            ('ball4', 10, 9), ('ball3', 12, 12), ('ball2', 8, 13), ('ball2', 13, 7), ('dot', 11, 14),
            ('ball3', 11, 2), ('dot', 14, 4), ('ball2', 3, 11), ('dot', 6, 11),
        ],
    },
    # Fluorite: purple cubes grown together, a cyan glint where it fluoresces.
    'fluorite': {
        'tones': ['1d0a35', '461e7c', '7638c0', 'a874ee', 'e8d8ff'],
        'second': ['2a7f9c', '54cae8', 'bdf6ff'],
        'halo': '2a1046', 'halo_amount': 0.15,
        'pieces': [
            ('cube4', 1, 2), ('cube3', 4, 5), ('cube2', 1, 6), ('crust1', 1, 2), ('crust1', 4, 5),
            ('cube4', 10, 9), ('cube2', 8, 12), ('cube3', 12, 12), ('crust1', 10, 9), ('crust1', 12, 12),
            ('cube2', 12, 2), ('crust1', 12, 2), ('cube2', 5, 12),
        ],
    },
    # Halite: small pink-white cubes, clean, in loose groups.
    'salt': {
        'tones': ['7a4652', 'b27584', 'e0b0ba', 'f6dce2', 'ffffff'],
        'second': None,
        'halo': '8a5060', 'halo_amount': 0.2,
        'pieces': [
            ('cube3', 1, 1), ('cube2', 4, 3), ('cube2', 2, 5), ('glint', 5, 1), ('cube3', 10, 7),
            ('cube2', 13, 6), ('cube2', 12, 10), ('cube2', 9, 11), ('cube2', 3, 11), ('cube3', 5, 12),
            ('glint', 2, 14), ('cube2', 13, 13), ('glint', 14, 2),
        ],
    },
    # Wolframite: black bladed crystals with a metallic edge, crossing; specks of pale scheelite.
    'tungsten': {
        'tones': ['0a0a0a', '1a1918', '33302d', '6c6762', 'e6e2dc'],
        'second': ['a89a78', 'e2d6b0', 'fffaea'],
        'halo': '141210', 'halo_amount': 0.32,
        'pieces': [
            ('blade6', 1, 3), ('blade5d', 3, 5), ('bladev', 7, 1), ('crust2', 6, 8), ('crust1', 1, 6),
            ('blade6', 8, 11), ('blade4', 10, 8), ('bladev', 13, 10), ('crust3', 11, 12),
            ('crust1', 9, 10), ('blade4', 11, 2), ('crust2', 2, 12), ('crust1', 4, 14),
            ('crust1', 14, 6),
        ],
    },
    # Monazite: honey-brown stubby prisms with a resinous shine, in small sprays.
    'monazite': {
        'tones': ['2e1a08', '583410', '885618', 'b47e2c', 'ecc278'],
        'second': None,
        'halo': '3a2410', 'halo_amount': 0.32,
        'pieces': [
            ('prism', 2, 2), ('prism_l', 4, 4), ('prism_s', 1, 6), ('prism', 11, 8), ('prism_s', 13, 10),
            ('prism_l', 8, 10), ('prism_s', 12, 1), ('dot', 14, 3), ('prism_s', 3, 11), ('dot', 6, 14),
        ],
    },
}


# Colour ramps, dark to light, for gradient-mapping vanilla items.
INGOT_RAMPS = {
    'uranium_ingot': ['1d2b12', '2f4a1c', '4f7a2a', '78a83a', 'a6d34a', 'd7f57a', 'f2ffc0'],
    'lead_ingot': ['1c1d2b', '2e3044', '474a66', '646888', '8a8eab', 'b3b6cc', 'd9dbe8'],
    'silver_ingot': ['3a3530', '66605a', '928b84', 'bcb6ae', 'dcd7d0', 'f1eee9', 'ffffff'],
    'aluminium_ingot': ['5a6470', '7d8894', 'a2adb8', 'bcc6cf', 'cfd7de', 'dde3e9', 'e9eef2'],
    'tungsten_ingot': ['141414', '262526', '3b393a', '545152', '716c6c', '938d8b', 'b8b1ae'],
    'steel_ingot': ['1b1f24', '2e343c', '47505b', '65707d', '8791a0', 'a9b3c1', 'cfd6e0'],
    # Graphite is tagged c:ingots/graphite like other reactor mods: a near-black bar with a soft sheen.
    'graphite': ['0c0d0f', '17191c', '23262a', '32363b', '464b52', '62686f', '8a9198'],
    # Plutonium: a steel-blue metal with a violet cast, bright at the edges, special among the
    # ingots. Real basis: plutonium in solution is famously blue-violet.
    'plutonium_ingot': ['0d1026', '1a1f45', '2a3370', '3d4f9e', '5a73c8', '8aa4ec', 'c8d8ff'],
    # Sodium: a soft, silvery white metal, faintly warm where it has begun to tarnish.
    'sodium_ingot': ['38362f', '5a5850', '807d73', 'a5a297', 'c5c2b7', 'dedbd1', 'f6f4ec'],
    # Transuranic metal: plutonium, americium and curium together, as the Electrorefiner plates them
    # out. A dark metal warming to amber at its edges, from the decay heat of the americium and curium.
    'transuranic_metal': ['120d10', '241a20', '3a2a32', '56404a', '7a5c62', 'a88478', 'e0b894'],
    # Thorium: a dull grey metal that tarnishes dark, with a faint olive cast from its oxide.
    'thorium_ingot': ['141612', '262a22', '3c4236', '575e4e', '767e6a', '9aa38c', 'c5ccb5'],
    # Yttrium: a bright silver with a warm, pale gold sheen.
    'yttrium_ingot': ['2f2a1f', '4f4834', '756c50', '9c9270', 'c2b994', 'e0d9bb', 'f8f4e2'],
    # Uranium-233: uranium's green turned to teal, so it never reads as plain uranium.
    'uranium_233_ingot': ['0f2a24', '184a3e', '24705c', '36a083', '56c9a4', '90e8c8', 'd0fff0'],
}
NUGGET_RAMPS = {
    'plutonium_nugget': INGOT_RAMPS['plutonium_ingot'],
}
RAW_RAMPS = {
    'raw_uranium': ['0c0e09', '1b2015', '2d3322', '4a5338', '6f8a2a', 'a6d630', 'eeff8a'],
    'raw_lead': ['181a22', '2a2d3a', '41465a', '5d637d', '7f86a3', 'a7adc6', 'd0d4e6'],
    'raw_silver': ['2e2a25', '4f4943', '766f67', '9d958b', 'c3bcb2', 'e0dbd3', 'f8f5f0'],
    'raw_aluminium': ['2a1008', '421a0b', '58220e', '8a3c1a', 'b45e2e', 'cc7a4c', 'e2986c'],
    'raw_tungsten': ['0a0a0a', '141413', '1a1918', '33302d', '4e4a46', '6c6762', 'b8b2aa'],
    'raw_monazite': ['2e1a08', '42260c', '583410', '885618', 'b47e2c', 'd09e50', 'ecc278'],
}
GEM_RAMPS = {
    # Fluorite on the emerald's cut shape; salt on sugar's grains.
    'fluorite': ('item/emerald', ['1f0f33', '3a1c5c', '5a2d8a', '7e46b5', 'a36bd9', 'c99af0', 'ecd7ff']),
    'salt': ('item/sugar', ['7a4a55', '9e6a75', 'c48f99', 'dcb1b9', 'ecced3', 'f7e6e9', 'fff7f8']),
    # Silicon carbide (moissanite): near-black crystals with a blue-green sheen, on the amethyst shard.
    'silicon_carbide': ('item/amethyst_shard', ['0d1414', '152426', '1f3a3d', '2a5a5c', '3b8580', '62b8a8', 'b8f0e0']),
    # TRISO pellets: glossy black carbon-coated grains, on the beetroot seeds' little cluster.
    'triso_pellets': ('item/beetroot_seeds', ['0b0b0d', '17181b', '25272b', '363a40', '50555d', '7a8089', 'c4cad2']),
    # Silicon: a blue-grey metalloid with a bright sheen, on quartz's crystal shape.
    'silicon': ('item/quartz', ['10141b', '1d2530', '2e3948', '445366', '627590', '8da0bb', 'c9d6ea']),
    # Spent kernels: the same little cluster as TRISO, its coating burnt and oxidised to rust brown.
    'spent_kernels': ('item/beetroot_seeds', ['0e0b09', '1d1613', '2f231c', '463427', '634935', '86644a', 'b08d6c']),
    # Lithium: a soft, pale silver metal, on glowstone dust's grains.
    'lithium_dust': ('item/glowstone_dust', ['23222b', '3b3a47', '5a5869', '7f7d91', 'a8a6ba', 'd1cfe0', 'f4f3fa']),
    # Fission waste: fission products set in glass (vitrified), a dark amber lump.
    'fission_waste': ('item/magma_cream', ['0a0604', '160c06', '24130a', '361d0e', '4f2c14', '7a4a20', 'd59a50']),
}


def mix(a, b, t):
    ca, cb = rgb(a), rgb(b)
    return '%02x%02x%02x' % tuple(round(ca[i] + (cb[i] - ca[i]) * t) for i in range(3))


def darken(c, amount):
    return mix(c, '000000', amount)


def gradient_map(tex, ramp):
    """Recolours a texture: its distinct colours, by brightness, take the matching tone of the ramp."""
    colours = sorted({c for row in tex.px for c in row if c}, key=luminance)
    mapping = {}
    for i, c in enumerate(colours):
        t = i / max(1, len(colours) - 1) * (len(ramp) - 1)
        lo = min(int(t), len(ramp) - 2)
        mapping[c] = mix(ramp[lo], ramp[lo + 1], t - lo)
    tex.px = [[mapping.get(c) if c else None for c in row] for row in tex.px]
    return tex


def ore_layer(mineral):
    """The mineral as a 16 x 16 grid of piece characters (None for rock), wrapping at the edges."""
    spec = ORES[mineral]
    grid = [[None] * 16 for _ in range(16)]
    for name, x0, y0 in spec.get('pieces', []):
        for dy, row in enumerate(PIECES[name]):
            for dx, ch in enumerate(row):
                if ch != ' ':
                    grid[(y0 + dy) % 16][(x0 + dx) % 16] = ch
    for path in spec.get('wires', []):
        for i, (x, y) in enumerate(path):
            tip = i == 0 or i == len(path) - 1
            grid[y % 16][x % 16] = '5' if i == 0 else '3' if tip else '4'
            if grid[(y + 1) % 16][(x + 1) % 16] is None:
                grid[(y + 1) % 16][(x + 1) % 16] = '1'
    return grid


def tint(c, hue, amount):
    """c moved towards hue's colour at c's own brightness, so a tint shifts the rock without darkening it."""
    scale = luminance(c) / max(1.0, luminance(hue))
    target = '%02x%02x%02x' % tuple(max(0, min(255, round(v * scale))) for v in rgb(hue))
    return mix(c, target, amount)


def ore(base, mineral):
    """Paints a mineral's crystals into vanilla rock. The rock round them takes its host's hue and a
    shadow below and right of each crystal, so they sit proud of the stone in the top-left light.
    The outer ring of pixels stays plain rock, as vanilla's ores do, so a block blends into the
    stone around it and a wall never shows its grid."""
    spec = ORES[mineral]
    t = vanilla(base)
    grid = ore_layer(mineral)
    for y in range(16):
        for x in range(16):
            ch = grid[y][x]
            if ch is not None:
                t.px[y][x] = spec['tones'][int(ch) - 1] if ch in '12345' else spec['second']['abc'.index(ch)]
            elif 0 < x < 15 and 0 < y < 15:
                near = min((max(abs(dx), abs(dy)) for dy in range(-2, 3) for dx in range(-2, 3)
                            if grid[(y + dy) % 16][(x + dx) % 16] is not None), default=9)
                amount = spec['halo_amount'] * (1 if near == 1 else 0.45 if near == 2 else 0)
                if amount:
                    t.px[y][x] = tint(t.px[y][x], spec['halo'], amount)
                if grid[y - 1][x] is not None or grid[y][x - 1] is not None:
                    t.px[y][x] = darken(t.px[y][x], ORE_SHADOW)
                elif grid[y - 1][x - 1] is not None:
                    t.px[y][x] = darken(t.px[y][x], ORE_SHADOW / 2)
    return t


def main():
    for mineral in ORES:
        publish(f'block/{mineral}_ore', ore('block/stone', mineral))
        publish(f'block/deepslate_{mineral}_ore', ore('block/deepslate', mineral))
    for name, ramp in INGOT_RAMPS.items():
        publish(f'item/{name}', gradient_map(vanilla('item/iron_ingot'), ramp))
    for name, ramp in RAW_RAMPS.items():
        publish(f'item/{name}', gradient_map(vanilla('item/raw_iron'), ramp))
    # Graphite block: coal block's shape in graphite's near-black with a soft sheen.
    publish('block/graphite_block', gradient_map(vanilla('block/coal_block'), INGOT_RAMPS['graphite']))
    for name, ramp in NUGGET_RAMPS.items():
        publish(f'item/{name}', gradient_map(vanilla('item/iron_nugget'), ramp))
    for name, (base, ramp) in GEM_RAMPS.items():
        publish(f'item/{name}', gradient_map(vanilla(base), ramp))


if __name__ == '__main__':
    main()
