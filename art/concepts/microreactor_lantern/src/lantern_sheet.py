"""The Lantern's states side by side, front views, labelled."""
import os
import sys
sys.path.insert(0, r'C:/Users/Quintinimus10k/Documents/Ryzer Gen/art/tools')
import model_preview as mp
from sheet_ideas import left_half

HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, 'lantern')
F = {'I': ['111', '010', '010', '010', '111'], 'D': ['110', '101', '101', '101', '110'], 'L': ['100', '100', '100', '100', '111'],
     'E': ['111', '100', '110', '100', '111'], 'R': ['110', '101', '110', '101', '101'], 'U': ['101', '101', '101', '101', '111'],
     'N': ['110', '101', '101', '101', '101'], 'G': ['111', '100', '101', '101', '111'], 'O': ['111', '101', '101', '101', '111'],
     'V': ['101', '101', '101', '101', '010'], 'C': ['111', '100', '100', '100', '111'], 'A': ['010', '101', '111', '101', '101'],
     'T': ['111', '010', '010', '010', '010'], 'S': ['111', '100', '111', '001', '111'], 'F': ['111', '100', '110', '100', '100'],
     ' ': ['000'] * 5}
BG = (40, 44, 52, 255)
COL = {'idle': (90, 110, 120), 'running': (53, 200, 245), 'overdrive': (240, 160, 48), 'loss': (225, 70, 60), 'refuel': (80, 220, 100)}
NAMES = {'idle': 'IDLE', 'running': 'RUNNING', 'overdrive': 'OVERDRIVE', 'loss': 'COOLANT LOSS', 'refuel': 'REFUEL'}


def label(img, text, col, scale=4):
    for i, ch in enumerate(text):
        for r, line in enumerate(F[ch]):
            for c, bit in enumerate(line):
                if bit == '1':
                    for yy in range(scale):
                        for xx in range(scale):
                            img[10 + r * scale + yy][14 + (i * 4 + c) * scale + xx] = (*col, 255)
    return img


tiles = []
for s in ('idle', 'running', 'overdrive', 'loss', 'refuel'):
    img = [list(r) for r in left_half(mp.read_png(os.path.join(R, s + '.png')))]
    tiles.append(label(img, NAMES[s], COL[s]))
h = max(len(t) for t in tiles)
out = []
for y in range(h):
    line = []
    for t in tiles:
        line += t[y] if y < len(t) else [BG] * len(t[0])
    out.append(line)
mp.write_png(os.path.join(R, '00_states.png'), out)
print(len(out[0]), len(out))
