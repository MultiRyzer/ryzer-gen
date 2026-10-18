"""Exact best layouts of a full 5 x 5 core (every channel fuel F, graphite M or coolant W), by
dynamic programming down the rows, counting every layout that ties for best.
Rules: a fuel rod makes BASE heat, +G per graphite touching it, +FB per fuel touching it; each coolant
touching it gives COOL cooling; heat must not exceed cooling; score = total heat."""
import itertools, sys
import numpy as np

N = 5
R = 3 ** N
OUT = R
digits = np.full((R + 1, N), -1, dtype=np.int8)
for r in range(R):
    v = r
    for x in range(N):
        digits[r, x] = v % 3
        v //= 3
F, M, W = 0, 1, 2
NEG = -10 ** 6

def row_scores(base, cool, g, fb, gd=0):
    isM = (digits == M).astype(np.int32)
    isW = (digits == W).astype(np.int32)
    isF = (digits == F).astype(np.int32)
    total = np.zeros((R + 1, R + 1, R + 1), dtype=np.int32)
    bad = np.zeros((R + 1, R + 1, R + 1), dtype=bool)
    for x in range(N):
        def horiz(arr):
            h = np.zeros(R + 1, dtype=np.int32)
            if x > 0: h += arr[:, x - 1]
            if x < N - 1: h += arr[:, x + 1]
            return h
        m = isM[:, x][:, None, None] + horiz(isM)[None, :, None] + isM[:, x][None, None, :]
        w = isW[:, x][:, None, None] + horiz(isW)[None, :, None] + isW[:, x][None, None, :]
        f = isF[:, x][:, None, None] + horiz(isF)[None, :, None] + isF[:, x][None, None, :]
        # Graphite on the corners (diagonals): in the rows above and below, a column either side.
        d = np.zeros((R + 1, R + 1, R + 1), dtype=np.int32)
        for dx in (-1, 1):
            if 0 <= x + dx < N:
                d += isM[:, x + dx][:, None, None] + isM[:, x + dx][None, None, :]
        heat = base + g * m + gd * d + fb * f
        fuel = isF[:, x][None, :, None].astype(bool)
        total += np.where(fuel, heat, 0)
        bad |= fuel & (heat > cool * w)
    total[bad] = NEG
    total[OUT, :, :] = np.where(total[OUT] > NEG, total[OUT], NEG)
    return total

def solve(base, cool, g, fb, gd=0):
    S = row_scores(base, cool, g, fb, gd)
    S[:, OUT, :] = NEG  # the outside is never a real row
    dp = np.full((R + 1, R + 1), NEG, dtype=np.int64)
    cnt = np.zeros((R + 1, R + 1), dtype=object)
    dp[OUT, :R] = 0
    cnt[OUT, :R] = 1
    back = []
    for step in range(N):
        last = step == N - 1
        cols = [OUT] if last else list(range(R))
        ndp = np.full((R + 1, R + 1), NEG, dtype=np.int64)
        ncnt = np.zeros((R + 1, R + 1), dtype=object)
        arg = np.zeros((R + 1, R + 1), dtype=np.int32)
        for b in range(R):
            col = dp[:, b]
            ok = col > NEG
            if not ok.any():
                continue
            vals = col[:, None] + S[:, b, :][:, cols]
            best = vals.max(axis=0)
            for k, c in enumerate(cols):
                if best[k] <= NEG // 2:
                    continue
                hit = vals[:, k] == best[k]
                ndp[b, c] = best[k]
                ncnt[b, c] = sum(cnt[hit, b])
                arg[b, c] = int(np.argmax(hit))
        back.append(arg)
        dp, cnt = ndp, ncnt
    best = dp[:, OUT].max()
    count = sum(cnt[b, OUT] for b in range(R) if dp[b, OUT] == best)
    # One best layout, traced back.
    b = int(np.argmax(dp[:, OUT]))
    rows, c = [b], OUT
    for step in range(N - 1, 0, -1):
        a = back[step][b, c]
        rows.append(a)
        b, c = a, b
    rows.reverse()
    layout = ''.join('FMW'[digits[r, x]] for r in rows for x in range(N))
    return int(best), count, layout

def symmetries(g):
    grid = [g[r * 5:(r + 1) * 5] for r in range(5)]
    forms = set()
    for _ in range(4):
        grid = [''.join(grid[4 - c][r] for c in range(5)) for r in range(5)]
        forms.add(''.join(grid)); forms.add(''.join(row[::-1] for row in grid))
    return len(forms)

if __name__ == '__main__':
    variants = [tuple(map(int, a.split(','))) for a in sys.argv[1:]]
    for base, cool, g, fb in variants:
        best, count, layout = solve(base, cool, g, fb)
        print(f'base {base} cool {cool} graphite +{g} fuel +{fb}: best {best}, layouts tying best {count}, '
              f'its symmetric copies {symmetries(layout)}')
        if count <= 8:
            for r in range(5): print('     ', ' '.join(layout[r * 5:(r + 1) * 5]))
        sys.stdout.flush()
