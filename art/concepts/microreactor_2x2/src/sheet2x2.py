import os, sys
sys.path.insert(0, r'C:/Users/Quintinimus10k/Documents/Ryzer Gen/art/tools')
import model_preview as mp
HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, 'renders2x2')
FONT = {'1': ['010', '110', '010', '010', '111'], '2': ['111', '001', '111', '100', '111'], '3': ['111', '001', '111', '001', '111'],
        '4': ['101', '101', '111', '001', '001'], '5': ['111', '100', '111', '001', '111'], '6': ['111', '100', '111', '101', '111']}
BG = (40, 44, 52, 255)
tiles = []
for f in sorted(os.listdir(R)):
    if not f[0].isdigit():
        continue
    img = [list(r) for r in mp.read_png(os.path.join(R, f))]
    for r_, line in enumerate(FONT[f[0]]):
        for c, bit in enumerate(line):
            if bit == '1':
                for yy in range(5):
                    for xx in range(5):
                        img[8 + r_ * 5 + yy][10 + c * 5 + xx] = (53, 200, 245, 255)
    tiles.append(img)
out = []
for row in (tiles[:3], tiles[3:]):
    h = max(len(t) for t in row)
    for y in range(h):
        line = []
        for t in row:
            line += t[y] if y < len(t) else [BG] * len(t[0])
        out.append(line)
W = max(len(r) for r in out)
out = [r + [BG] * (W - len(r)) for r in out]
mp.write_png(os.path.join(R, '00_sheet.png'), out)
print(W, len(out))
