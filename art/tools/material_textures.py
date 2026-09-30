"""Generates the ore blocks, raw drops, gems and ingots.

Built on vanilla textures, as most mods do, so they sit naturally in today's game:
- Ores: vanilla stone or deepslate, with each mineral's own cluster shape drawn on top and shaded the
  modern vanilla way (lit top and left edges, dark bottom and right, a highlight on top-left corners,
  and a soft shadow cast onto the rock).
- Ingots, raw drops and gems: the vanilla shape and shading, gradient-mapped onto each material's own
  colour ramp. Vanilla's grey levels are sorted by brightness and each one takes the matching tone.

Vanilla textures are read from the Minecraft jar at run time (see pixelart.vanilla), never copied in.
Run from the repo root after one Gradle build:
    python art/tools/material_textures.py
"""
from pixelart import *  # noqa: F401,F403

# Five tones per ore, dark to light: outline, shade, body, lit edge, highlight.
ORE_TONES = {
    # Pitchblende with its bright yellow-green secondary minerals.
    'uranium': ['1c2a10', '3d6b1f', '6aa82e', 'a8e04a', 'e8ff9a'],
    # Galena: heavy, dull blue-grey.
    'lead': ['1f2130', '3d4260', '5d6590', '8a92b8', 'c5cae0'],
    # Native silver: bright and warm white.
    'silver': ['4a433b', '8c847a', 'c2bbb1', 'ece7e0', 'ffffff'],
    # Bauxite, the aluminium ore: rusty red-brown nodules.
    'aluminium': ['3d1a0e', '7a3a1c', 'b0602e', 'd98e52', 'f5c08a'],
    # Fluorite: purple cubes that fluoresce cyan at the edges.
    'fluorite': ['2a1247', '5a2a8f', '8a4ccc', 'b98af0', 'a8f0ff'],
    # Halite: small pinkish-white cubes.
    'salt': ['8a4f5c', 'c9828f', 'eab3bc', 'f8d9de', 'fff4f6'],
    # Wolframite: near-black tabular crystals with a metallic sheen.
    'tungsten': ['0f0d0c', '2b2522', '4a403a', '8a7c72', 'e0d6ce'],
    # Monazite: small red-brown prisms.
    'monazite': ['3a120a', '7a2a14', 'b04a22', 'dd7a3c', 'ffb070'],
}

# Cluster shapes, one per mineral: '#' is ore, '.' is rock.
ORE_SHAPES = {
    # Metal nuggets.
    'lead': [
        '................',
        '...##...........',
        '..####......##..',
        '..###......####.',
        '...#.......###..',
        '.............#..',
        '......###.......',
        '.....#####......',
        '.....####.....#.',
        '......##.....###',
        '.............##.',
        '..##............',
        '.####......##...',
        '.###......####..',
        '..#........##...',
        '................',
    ],
    'silver': [
        '................',
        '..#.............',
        '.###.....##.....',
        '.##.....####....',
        '.........##.....',
        '....##..........',
        '...####.......#.',
        '...###.......###',
        '....#.........#.',
        '..........##....',
        '.........####...',
        '.##......###....',
        '####............',
        '.##.......#.....',
        '.........###....',
        '..........#.....',
    ],
    # Tabular crystals.
    'tungsten': [
        '................',
        '.####...........',
        '.####.....###...',
        '..........###...',
        '...........##...',
        '.....##.........',
        '....####........',
        '....####....##..',
        '.....##....####.',
        '...........####.',
        '..##............',
        '.####....###....',
        '.####....####...',
        '..##......##....',
        '................',
        '................',
    ],
    # Angular shards.
    'uranium': [
        '................',
        '..#.........#...',
        '..##.......##...',
        '...##.....##....',
        '...###...##.....',
        '....##..........',
        '................',
        '.......#........',
        '......###.......',
        '.....#####......',
        '......###..##...',
        '.......#..###...',
        '..##.......##...',
        '.###............',
        '..##............',
        '................',
    ],
    # Cubes.
    'fluorite': [
        '................',
        '.###............',
        '.###.....##.....',
        '.###....####....',
        '........####....',
        '.........##.....',
        '...##...........',
        '..####......###.',
        '..####......###.',
        '...##.......###.',
        '.........#......',
        '........###.....',
        '.##.....###.....',
        '.##......#......',
        '................',
        '................',
    ],
    # Many small cubes.
    'salt': [
        '................',
        '..##.....##.....',
        '..##.....##..#..',
        '.............#..',
        '.....##.........',
        '.....##....##...',
        '...........##...',
        '.##.............',
        '.##....##.......',
        '.......##...##..',
        '............##..',
        '....##..........',
        '....##....##....',
        '..........##....',
        '.##.............',
        '.##.............',
    ],
    # Round nodules.
    'aluminium': [
        '................',
        '..###...........',
        '.#####......##..',
        '.#####.....####.',
        '..###......####.',
        '............##..',
        '.....###........',
        '....#####.......',
        '....#####...###.',
        '.....###...#####',
        '...........#####',
        '.##.........###.',
        '####............',
        '####....###.....',
        '.##....#####....',
        '........###.....',
    ],
    # Small prisms.
    'monazite': [
        '................',
        '.##.............',
        '..##......#.....',
        '...#.....###....',
        '.........##.....',
        '.....#..........',
        '....###.....##..',
        '.....###...###..',
        '......#.....#...',
        '................',
        '.##.........#...',
        '.###.......##...',
        '..##......##....',
        '..........#.....',
        '....##..........',
        '.....##.........',
    ],
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
}
NUGGET_RAMPS = {
    'plutonium_nugget': INGOT_RAMPS['plutonium_ingot'],
}
RAW_RAMPS = {
    'raw_uranium': ['141a10', '24321a', '3b5226', '5b7a33', '86a84a', 'b8d86a', 'e2f5a0'],
    'raw_lead': ['181a22', '2a2d3a', '41465a', '5d637d', '7f86a3', 'a7adc6', 'd0d4e6'],
    'raw_silver': ['2e2a25', '4f4943', '766f67', '9d958b', 'c3bcb2', 'e0dbd3', 'f8f5f0'],
    'raw_aluminium': ['2b140c', '4a2414', '6e3a1f', '93552d', 'b9744a', 'd69a6c', 'efc39c'],
    'raw_tungsten': ['120f0d', '231d19', '3a302a', '54463c', '716054', '917d6d', 'b39e8d'],
    'raw_monazite': ['2e120a', '521f10', '7a3218', 'a44a22', 'c96a32', 'e39152', 'f5bd84'],
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


def ore(base, mineral):
    """Draws a mineral's clusters over vanilla rock, with modern vanilla-style shading."""
    t = vanilla(base)
    shape = ORE_SHAPES[mineral]
    assert len(shape) == 16 and all(len(row) == 16 for row in shape), mineral
    tones = ORE_TONES[mineral]
    inside = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and shape[y][x] == '#'
    # Soft shadow cast onto the rock below and to the right of each cluster.
    for y in range(16):
        for x in range(16):
            if not inside(x, y) and (inside(x - 1, y) or inside(x, y - 1)):
                t.px[y][x] = darken(t.px[y][x], 0.35)
    for y in range(16):
        for x in range(16):
            if not inside(x, y):
                continue
            up, left = not inside(x, y - 1), not inside(x - 1, y)
            down, right = not inside(x, y + 1), not inside(x + 1, y)
            if up and left:
                tone = 4
            elif down and right:
                tone = 0
            elif up or left:
                tone = 3
            elif down or right:
                tone = 1
            else:
                tone = 2
            t.px[y][x] = tones[tone]
    return t


def main():
    for mineral in ORE_SHAPES:
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
