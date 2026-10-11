"""The icons set into the mod's text (client/text/Term), drawn as a font: one 8 x 8 cell each in
textures/font/icons.png, mapped to private-use characters by font/icons.json. Run:
    python art/tools/font_icons.py

Each icon is drawn white with grey shading and no colour of its own: the game tints a font glyph by
its text's colour, so an icon takes its term's colour wherever it is drawn (the bolt red beside
Power, the drop blue beside Coolant). They sit on the text's cap height, 7 pixels tall in rows 0 to
6, so they line up with the letters beside them. Keep them to clean shapes a pixel thick: at this
size a detail shorter than 2 pixels does not read.
"""
import json
import os

from pixelart import ROOT, write_png

ASSETS = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen')
FIRST = 0xE000

# In the order of Term's characters (U+E000 on). '#' white, '+' grey (a darker shade of the tint).
ICONS = {
    'power': [          # a lightning bolt
        '...##',
        '..##.',
        '.##..',
        '#####',
        '..##.',
        '.##..',
        '##...',
    ],
    'heat': [           # a flame
        '..#..',
        '.##..',
        '.###.',
        '####.',
        '##+##',
        '#+++#',
        '.###.',
    ],
    'coolant': [        # a drop
        '..#..',
        '..#..',
        '.###.',
        '##+##',
        '#+###',
        '#####',
        '.###.',
    ],
    'steam': [          # two wisps rising
        '.#..#.',
        '#..#..',
        '#..#..',
        '.#..#.',
        '..#..#',
        '..#..#',
        '.#..#.',
    ],
    'fuel': [           # a fuel rod, banded
        '.###.',
        '#####',
        '#+++#',
        '#####',
        '#####',
        '#+++#',
        '.###.',
    ],
    'moderator': [      # a graphite block
        '######',
        '#++++#',
        '#+##+#',
        '#+##+#',
        '#++++#',
        '######',
        '......',
    ],
    'control': [        # a control rod on its drive
        '#####',
        '..#..',
        '.###.',
        '.###.',
        '.###.',
        '.###.',
        '.###.',
    ],
    'radiation': [      # a trefoil
        '###.###',
        '###.###',
        '.##.##.',
        '...#...',
        '..###..',
        '.#####.',
        '.#####.',
    ],
    'speed': [          # a double chevron
        '#..#...',
        '.#..#..',
        '..#..#.',
        '...#..#',
        '..#..#.',
        '.#..#..',
        '#..#...',
    ],
    'breeding': [       # a spark: something new being made
        '...#...',
        '...#...',
        '..###..',
        '#######',
        '..###..',
        '...#...',
        '...#...',
    ],
    'time': [           # a clock
        '.#####.',
        '#.....#',
        '#..#..#',
        '#..##.#',
        '#.....#',
        '#.....#',
        '.#####.',
    ],
}

COLOURS = {'#': (255, 255, 255, 255), '+': (170, 170, 170, 255), '.': (0, 0, 0, 0)}


def main():
    cells = list(ICONS.values())
    rows = [[COLOURS['.']] * (8 * len(cells)) for _ in range(8)]
    for n, icon in enumerate(cells):
        assert len(icon) == 7 and all(len(r) <= 7 for r in icon), list(ICONS)[n]
        for y, line in enumerate(icon):
            for x, c in enumerate(line):
                rows[y][8 * n + x] = COLOURS[c]
    png = os.path.join(ASSETS, 'textures', 'font', 'icons.png')
    os.makedirs(os.path.dirname(png), exist_ok=True)
    write_png(png, rows)
    chars = ''.join(chr(FIRST + n) for n in range(len(cells)))
    font = {'providers': [{'type': 'bitmap', 'file': 'ryzergen:font/icons.png', 'ascent': 7, 'height': 8, 'chars': [chars]}]}
    path = os.path.join(ASSETS, 'font', 'icons.json')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(font, f, indent=2, ensure_ascii=True)
        f.write('\n')
    print('icons ->', png, '(' + ', '.join(ICONS) + ')')


if __name__ == '__main__':
    main()
