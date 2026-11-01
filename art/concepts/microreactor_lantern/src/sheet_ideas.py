"""Puts each idea's front view side by side, numbered, with the current design first."""
import os
import sys
sys.path.insert(0, r'C:/Users/Quintinimus10k/Documents/Ryzer Gen/art/tools')
import model_preview as mp
import importlib

HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, 'renders')
FONT = {
    '0': ['111', '101', '101', '101', '111'], '1': ['010', '110', '010', '010', '111'], '2': ['111', '001', '111', '100', '111'],
    '3': ['111', '001', '111', '001', '111'], '4': ['101', '101', '111', '001', '001'], '5': ['111', '100', '111', '001', '111'],
    '6': ['111', '100', '111', '101', '111'], 'N': ['101', '111', '111', '111', '101'], 'O': ['111', '101', '101', '101', '111'],
    'W': ['101', '101', '111', '111', '101'],
}
BG = (40, 44, 52, 255)


def left_half(img):
    # Each render is two views side by side; keep the first (front-left) view.
    w = len(img[0])
    cols = [x for x in range(w) if any(img[y][x][:3] != BG[:3] for y in range(len(img)))]
    # find the gap between the two views
    gap = None
    for x in range(cols[0], cols[-1]):
        if x not in cols and x - 1 in cols:
            if all(c not in cols for c in range(x, min(x + 6, w))):
                gap = x
                break
    end = gap or w // 2
    return [row[:end + 8] for row in img]


def label(img, text, scale=6):
    x0, y0 = 14, 14
    for i, ch in enumerate(text):
        for r, line in enumerate(FONT[ch]):
            for c, bit in enumerate(line):
                if bit == '1':
                    for yy in range(scale):
                        for xx in range(scale):
                            img[y0 + r * scale + yy][x0 + (i * 4 + c) * scale + xx] = (53, 200, 245, 255)
    return img


names = [('NOW', 'current.png'), ('1', '01_hourglass.png'), ('2', '02_lantern.png'), ('3', '03_crown.png'),
         ('4', '04_jet.png'), ('5', '05_mule.png'), ('6', '06_atomic.png')]
tiles = []
for tag, f in names:
    path = os.path.join(R, f) if f != 'current.png' else os.path.join(HERE, f)
    img = left_half(mp.read_png(path))
    tiles.append(label([list(r) for r in img], tag))
# Two rows: current + 1-3, then 4-6.
rows_of = [tiles[:4], tiles[4:]]
out = []
for row in rows_of:
    h = max(len(t) for t in row)
    for y in range(h):
        line = []
        for t in row:
            line += t[y] if y < len(t) else [BG] * len(t[0])
        out.append(line)
W = max(len(r) for r in out)
out = [r + [BG] * (W - len(r)) for r in out]
mp.write_png(os.path.join(R, '00_sheet.png'), out)
print('sheet', W, len(out))
