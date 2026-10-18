"""Exact check of the lattice rules on a full 5 x 5 core (F fuel, M graphite, W water):
a fuel rod needs water on every side it has; it makes BASE heat, +G per graphite on its corners.
Score = total heat. Counts every layout tying for best."""
import sys
import numpy as np
import exact
from exact import R, OUT, N, digits, NEG, symmetries

F, M, W = 0, 1, 2

def row_scores(base, g):
    isM = (digits == M).astype(np.int32)
    isW = (digits == W).astype(np.int32)
    isF = (digits == F).astype(np.int32)
    real = np.ones(R + 1, dtype=np.int32); real[OUT] = 0
    total = np.zeros((R + 1, R + 1, R + 1), dtype=np.int32)
    bad = np.zeros((R + 1, R + 1, R + 1), dtype=bool)
    for x in range(N):
        sides = real[:, None, None] + real[None, None, :] + (1 if x > 0 else 0) + (1 if x < N - 1 else 0)
        water = isW[:, x][:, None, None] + isW[:, x][None, None, :]
        h = np.zeros(R + 1, dtype=np.int32)
        if x > 0: h += isW[:, x - 1]
        if x < N - 1: h += isW[:, x + 1]
        water = water + h[None, :, None]
        corners = np.zeros((R + 1, R + 1, R + 1), dtype=np.int32)
        for dx in (-1, 1):
            if 0 <= x + dx < N:
                corners += isM[:, x + dx][:, None, None] + isM[:, x + dx][None, None, :]
        fuel = isF[:, x][None, :, None].astype(bool)
        total += np.where(fuel, base + g * corners, 0)
        bad |= fuel & (water < sides)
    total[bad] = NEG
    return total

exact.row_scores = lambda base, cool, g, fb, gd=0: row_scores(base, g)
for base, g in [(2, 1), (3, 1), (1, 1), (4, 1), (3, 2)]:
    best, count, layout = exact.solve(base, 0, g, 0)
    print(f'base {base}, graphite +{g} per corner: best {best}, layouts tying best {count}, symmetric {symmetries(layout) == 1}')
    for r in range(5): print('     ', ' '.join(layout[r * 5:(r + 1) * 5]))
