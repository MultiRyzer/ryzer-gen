"""The terminal font: a 5 x 7 pixel monospace in capitals, for the machines' control screens.

Every glyph sits in a 5 x 7 cell and advances 6 pixels (one of spacing), so columns of readouts line
up digit under digit. Lower case draws as capitals. The mocks draw them straight from here, and the
game takes them as the bitmap font ryzergen:terminal (client/TerminalText), as it takes the term
icons. Publish it with
    python art/tools/terminal_font.py
"""

GLYPHS = {
    'A': ['.###.', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'],
    'B': ['####.', '#...#', '#...#', '####.', '#...#', '#...#', '####.'],
    'C': ['.####', '#....', '#....', '#....', '#....', '#....', '.####'],
    'D': ['####.', '#...#', '#...#', '#...#', '#...#', '#...#', '####.'],
    'E': ['#####', '#....', '#....', '####.', '#....', '#....', '#####'],
    'F': ['#####', '#....', '#....', '####.', '#....', '#....', '#....'],
    'G': ['.####', '#....', '#....', '#..##', '#...#', '#...#', '.###.'],
    'H': ['#...#', '#...#', '#...#', '#####', '#...#', '#...#', '#...#'],
    'I': ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '#####'],
    'J': ['..###', '...#.', '...#.', '...#.', '...#.', '#..#.', '.##..'],
    'K': ['#...#', '#..#.', '#.#..', '##...', '#.#..', '#..#.', '#...#'],
    'L': ['#....', '#....', '#....', '#....', '#....', '#....', '#####'],
    'M': ['#...#', '##.##', '#.#.#', '#.#.#', '#...#', '#...#', '#...#'],
    'N': ['#...#', '##..#', '#.#.#', '#..##', '#...#', '#...#', '#...#'],
    'O': ['.###.', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
    'P': ['####.', '#...#', '#...#', '####.', '#....', '#....', '#....'],
    'Q': ['.###.', '#...#', '#...#', '#...#', '#.#.#', '#..#.', '.##.#'],
    'R': ['####.', '#...#', '#...#', '####.', '#.#..', '#..#.', '#...#'],
    'S': ['.####', '#....', '#....', '.###.', '....#', '....#', '####.'],
    'T': ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '..#..'],
    'U': ['#...#', '#...#', '#...#', '#...#', '#...#', '#...#', '.###.'],
    'V': ['#...#', '#...#', '#...#', '#...#', '#...#', '.#.#.', '..#..'],
    'W': ['#...#', '#...#', '#...#', '#.#.#', '#.#.#', '##.##', '#...#'],
    'X': ['#...#', '#...#', '.#.#.', '..#..', '.#.#.', '#...#', '#...#'],
    'Y': ['#...#', '#...#', '.#.#.', '..#..', '..#..', '..#..', '..#..'],
    'Z': ['#####', '....#', '...#.', '..#..', '.#...', '#....', '#####'],
    '0': ['.###.', '#...#', '#..##', '#.#.#', '##..#', '#...#', '.###.'],
    '1': ['..#..', '.##..', '..#..', '..#..', '..#..', '..#..', '.###.'],
    '2': ['.###.', '#...#', '....#', '...#.', '..#..', '.#...', '#####'],
    '3': ['####.', '....#', '....#', '.###.', '....#', '....#', '####.'],
    '4': ['...#.', '..##.', '.#.#.', '#..#.', '#####', '...#.', '...#.'],
    '5': ['#####', '#....', '####.', '....#', '....#', '#...#', '.###.'],
    '6': ['.###.', '#....', '#....', '####.', '#...#', '#...#', '.###.'],
    '7': ['#####', '....#', '...#.', '..#..', '..#..', '..#..', '..#..'],
    '8': ['.###.', '#...#', '#...#', '.###.', '#...#', '#...#', '.###.'],
    '9': ['.###.', '#...#', '#...#', '.####', '....#', '....#', '.###.'],
    '.': ['.....', '.....', '.....', '.....', '.....', '.....', '..#..'],
    ',': ['.....', '.....', '.....', '.....', '.....', '..#..', '.#...'],
    ':': ['.....', '..#..', '.....', '.....', '.....', '..#..', '.....'],
    '/': ['....#', '....#', '...#.', '..#..', '.#...', '#....', '#....'],
    '%': ['##..#', '##..#', '...#.', '..#..', '.#...', '#..##', '#..##'],
    '+': ['.....', '..#..', '..#..', '#####', '..#..', '..#..', '.....'],
    '-': ['.....', '.....', '.....', '#####', '.....', '.....', '.....'],
    '=': ['.....', '.....', '#####', '.....', '#####', '.....', '.....'],
    '_': ['.....', '.....', '.....', '.....', '.....', '.....', '#####'],
    '>': ['#....', '.#...', '..#..', '...#.', '..#..', '.#...', '#....'],
    '<': ['....#', '...#.', '..#..', '.#...', '..#..', '...#.', '....#'],
    '[': ['.###.', '.#...', '.#...', '.#...', '.#...', '.#...', '.###.'],
    ']': ['.###.', '...#.', '...#.', '...#.', '...#.', '...#.', '.###.'],
    '(': ['...#.', '..#..', '.#...', '.#...', '.#...', '..#..', '...#.'],
    ')': ['.#...', '..#..', '...#.', '...#.', '...#.', '..#..', '.#...'],
    '!': ['..#..', '..#..', '..#..', '..#..', '..#..', '.....', '..#..'],
    '?': ['.###.', '#...#', '....#', '...#.', '..#..', '.....', '..#..'],
    "'": ['..#..', '..#..', '.....', '.....', '.....', '.....', '.....'],
    '*': ['.....', '#.#.#', '.###.', '#####', '.###.', '#.#.#', '.....'],
    '#': ['.#.#.', '#####', '.#.#.', '.#.#.', '.#.#.', '#####', '.#.#.'],
    '|': ['..#..', '..#..', '..#..', '..#..', '..#..', '..#..', '..#..'],
    '°': ['.##..', '#..#.', '.##..', '.....', '.....', '.....', '.....'],
    ' ': ['.....'] * 7,
}
ADVANCE = 6
HEIGHT = 7


def publish():
    """Ships the glyphs as the game's bitmap font ryzergen:terminal: 8 x 8 cells, 16 to a row, in
    textures/font/terminal.png, mapped by font/terminal.json. The game measures each bitmap glyph by
    its rightmost lit column, so every cell also carries one pixel at alpha 1 in its fifth column,
    below the glyph: invisible, but it keeps every glyph 5 wide, so the font stays monospace (6 a
    character). Lower case is not in the font: the screens draw in capitals."""
    import json
    import os
    from pixelart import ROOT, write_png
    assets = os.path.join(ROOT, 'mod', 'src', 'main', 'resources', 'assets', 'ryzergen')
    chars = list(GLYPHS)
    per_row = 16
    lines = (len(chars) + per_row - 1) // per_row
    clear = (0, 0, 0, 0)
    rows = [[clear] * (8 * per_row) for _ in range(8 * lines)]
    for n, ch in enumerate(chars):
        x0, y0 = 8 * (n % per_row), 8 * (n // per_row)
        for gy, row in enumerate(GLYPHS[ch]):
            assert len(row) == 5, ch
            for gx, bit in enumerate(row):
                if bit == '#':
                    rows[y0 + gy][x0 + gx] = (255, 255, 255, 255)
        rows[y0 + 7][x0 + 4] = (255, 255, 255, 1)
    png = os.path.join(assets, 'textures', 'font', 'terminal.png')
    write_png(png, rows)
    table = [''.join(chars[i:i + per_row]).ljust(per_row, '\u0000') for i in range(0, len(chars), per_row)]
    font = {'providers': [{'type': 'bitmap', 'file': 'ryzergen:font/terminal.png', 'ascent': 7, 'height': 8, 'chars': table}]}
    with open(os.path.join(assets, 'font', 'terminal.json'), 'w', encoding='utf-8') as f:
        json.dump(font, f, indent=2, ensure_ascii=True)
        f.write('\n')
    print('terminal font ->', png, '(%d glyphs)' % len(chars))


def width(s):
    return len(s) * ADVANCE - 1 if s else 0


def draw(canvas, x, y, s, colour, align='left', scale=1, glow=None):
    """Draws s with its top left at (x, y); align 'right' or 'centre' measures from x. With glow,
    each lit pixel gets a faint halo of that colour first, like a phosphor screen."""
    w = width(s) * scale
    if align == 'right':
        x -= w - 1
    elif align == 'centre':
        x -= w // 2
    lit = []
    for i, ch in enumerate(s.upper()):
        rows = GLYPHS.get(ch, GLYPHS['?'])
        for gy, row in enumerate(rows):
            for gx, bit in enumerate(row):
                if bit == '#':
                    px, py = x + (i * ADVANCE + gx) * scale, y + gy * scale
                    lit.append((px, py))
    if glow is not None:
        for px, py in lit:
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                canvas.blend(px + dx, py + dy, glow, 0.18)
    for px, py in lit:
        canvas.rect(px, py, px + scale - 1, py + scale - 1, colour)
    return x + w


if __name__ == "__main__":
    publish()
