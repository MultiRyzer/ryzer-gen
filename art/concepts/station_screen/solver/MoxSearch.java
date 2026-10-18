import java.util.ArrayList;
import java.util.List;

/**
 * Exact best layouts of a full 5 x 5 core with four parts (F fuel, M graphite, W water, C control
 * rod), for trying MOX puzzle rules. Dynamic programming down the rows over every 4^25 layout,
 * counting every layout that ties for best; rows where a rod lacks water beside it in the row are
 * dropped first, which keeps it fast. Then a "hover climb": from an empty core, keep making the one
 * change the hover would show as the biggest gain, and see how close that gets to the best.
 *
 * Rules (as the uranium puzzle, plus control rods): a fuel rod needs water on every side it has; it
 * makes BASE heat, +G for each graphite on its corners, -CUT for each control rod on its corners;
 * a rod whose heat (before or after the cut) is over LIMIT must have a control rod on a corner, or
 * it runs away and the layout fails.
 *
 *     java MoxSearch.java
 */
public class MoxSearch {
    static final int N = 5, F = 0, M = 1, W = 2, C = 3, E = 4, OUT = 5;
    static final int NEG = -1_000_000;

    record Rule(String name, int base, int g, int fd, int cut, int limit, boolean capAfterCut, int ccost) {}

    /** Heat of the rod at the centre of a 3 x 3 neighbourhood (row-major, 4 the centre), or NEG if it fails. */
    static int rod(Rule r, int[] nb) {
        for (int s : new int[] {1, 3, 5, 7}) {
            if (nb[s] != W && nb[s] != OUT) {
                return NEG;
            }
        }
        int g = 0, c = 0, f = 0;
        for (int k : new int[] {0, 2, 6, 8}) {
            g += nb[k] == M ? 1 : 0;
            c += nb[k] == C ? 1 : 0;
            f += nb[k] == F ? 1 : 0;
        }
        int raw = r.base + r.g * g + r.fd * f;
        int heat = raw - r.cut * c;
        if ((r.capAfterCut ? heat : raw) > r.limit && c == 0) {
            return NEG;
        }
        return Math.max(0, heat);
    }

    static int[][] rows;

    static int cellAt(int row, int x) {
        return row == rows.length ? OUT : x < 0 || x >= N ? OUT : rows[row][x];
    }

    /** Score of the middle row b, between a above and c below (rows.length stands for outside). */
    static int middle(Rule r, int a, int b, int c) {
        int total = 0;
        int[] nb = new int[9];
        for (int x = 0; x < N; x++) {
            if (rows[b][x] == C) {
                total -= r.ccost;
            }
            if (rows[b][x] != F) {
                continue;
            }
            for (int dy = -1; dy <= 1; dy++) {
                int row = dy < 0 ? a : dy == 0 ? b : c;
                for (int dx = -1; dx <= 1; dx++) {
                    nb[(dy + 1) * 3 + dx + 1] = cellAt(row, x + dx);
                }
            }
            int h = rod(r, nb);
            if (h == NEG) {
                return NEG;
            }
            total += h;
        }
        return total;
    }

    record Result(int best, long ties, String layout) {}

    static final List<int[][]> dps = new ArrayList<>();
    static int[][][] lastS;
    static int lastN;

    /** Every layout scoring {@code best}, from the last solve. */
    static List<String> all(int best) {
        List<String> out = new ArrayList<>();
        int n = lastN;
        int[][] last = dps.get(N - 1);
        for (int a = 0; a <= n; a++) {
            for (int b = 0; b < n; b++) {
                if (last[a][b] != NEG && lastS[a][b][n] != NEG && last[a][b] + lastS[a][b][n] == best) {
                    walk(N - 1, a, b, new int[] {b}, out);
                }
            }
        }
        return out;
    }

    /** Back from step k, state (a above b), listing every way down to the first row. */
    private static void walk(int k, int a, int b, int[] tail, List<String> out) {
        if (k == 0) {
            StringBuilder l = new StringBuilder();
            for (int row : tail) for (int x = 0; x < N; x++) l.append("FMWC".charAt(rows[row][x]));
            out.add(l.toString());
            return;
        }
        int[][] prev = dps.get(k - 1);
        for (int z = 0; z <= lastN; z++) {
            if (prev[z][a] != NEG && lastS[z][a][b] != NEG && prev[z][a] + lastS[z][a][b] == dps.get(k)[a][b]) {
                int[] t = new int[tail.length + 1];
                t[0] = a;
                System.arraycopy(tail, 0, t, 1, tail.length);
                walk(k - 1, z, a, t, out);
            }
        }
    }

    static Result solve(Rule r) {
        List<int[]> keep = new ArrayList<>();
        for (int v = 0; v < 1024; v++) {
            int[] d = new int[N];
            int t = v;
            for (int x = 0; x < N; x++) {
                d[x] = t % 4;
                t /= 4;
            }
            boolean ok = true;
            for (int x = 0; x < N; x++) {
                if (d[x] == F && ((x > 0 && d[x - 1] != W) || (x < N - 1 && d[x + 1] != W))) {
                    ok = false;
                }
            }
            if (ok) {
                keep.add(d);
            }
        }
        rows = keep.toArray(new int[0][]);
        int n = rows.length;
        int out = n;
        int[][][] s = new int[n + 1][n][n + 1];
        for (int a = 0; a <= n; a++) {
            for (int b = 0; b < n; b++) {
                for (int c = 0; c <= n; c++) {
                    s[a][b][c] = middle(r, a, b, c);
                }
            }
        }
        // dp[a][b]: best score of the rows before b, given the row above b is a (out above the first).
        int[][] dp = new int[n + 1][n];
        long[][] cnt = new long[n + 1][n];
        for (int[] row : dp) java.util.Arrays.fill(row, NEG);
        for (int b = 0; b < n; b++) {
            dp[out][b] = 0;
            cnt[out][b] = 1;
        }
        List<int[][]> back = new ArrayList<>();
        dps.clear();
        dps.add(dp);
        lastS = s;
        lastN = n;
        for (int step = 1; step < N; step++) {
            int[][] nd = new int[n + 1][n];
            long[][] nc = new long[n + 1][n];
            int[][] arg = new int[n + 1][n];
            for (int[] row : nd) java.util.Arrays.fill(row, NEG);
            for (int a = 0; a <= n; a++) {
                for (int b = 0; b < n; b++) {
                    if (dp[a][b] == NEG) continue;
                    for (int c = 0; c < n; c++) {
                        int g = s[a][b][c];
                        if (g == NEG) continue;
                        int v = dp[a][b] + g;
                        if (v > nd[b][c]) {
                            nd[b][c] = v;
                            nc[b][c] = cnt[a][b];
                            arg[b][c] = a;
                        } else if (v == nd[b][c]) {
                            nc[b][c] += cnt[a][b];
                        }
                    }
                }
            }
            back.add(arg);
            dp = nd;
            cnt = nc;
            dps.add(dp);
        }
        int best = NEG;
        long ties = 0;
        int ba = -1, bb = -1;
        for (int a = 0; a <= n; a++) {
            for (int b = 0; b < n; b++) {
                if (dp[a][b] == NEG || s[a][b][out] == NEG) continue;
                int v = dp[a][b] + s[a][b][out];
                if (v > best) {
                    best = v;
                    ties = cnt[a][b];
                    ba = a;
                    bb = b;
                } else if (v == best) {
                    ties += cnt[a][b];
                }
            }
        }
        int[] order = new int[N];
        order[N - 1] = bb;
        order[N - 2] = ba;
        int a = ba, b = bb;
        for (int step = N - 2; step >= 1; step--) {
            int prev = back.get(step)[a][b];
            order[step - 1] = prev;
            b = a;
            a = prev;
        }
        StringBuilder layout = new StringBuilder();
        for (int row : order) {
            for (int x = 0; x < N; x++) layout.append("FMWC".charAt(rows[row][x]));
        }
        return new Result(best, ties, layout.toString());
    }

    /** A whole core as the game scores it: a failing rod scores nothing (rather than failing the layout). */
    static int score(Rule r, int[] g) {
        int total = 0;
        int[] nb = new int[9];
        for (int i = 0; i < 25; i++) {
            if (g[i] == C) total -= r.ccost;
            if (g[i] != F) continue;
            int y = i / N, x = i % N;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int yy = y + dy, xx = x + dx;
                    nb[(dy + 1) * 3 + dx + 1] = yy < 0 || yy >= N || xx < 0 || xx >= N ? OUT : g[yy * N + xx];
                }
            }
            int h = rod(r, nb);
            total += h == NEG ? 0 : h;
        }
        return total;
    }

    /**
     * Following the hover from the page's opening position (a rod in the middle with its four
     * waters): always the change with the biggest gain shown, where placing a rod brings the water
     * its sides need with it (the player knows rule 1). Stops when nothing gains.
     */
    static int climb(Rule r) {
        int[] g = new int[25];
        java.util.Arrays.fill(g, E);
        g[12] = F;
        g[7] = g[11] = g[13] = g[17] = W;
        int now = score(r, g);
        while (true) {
            int bestGain = 0;
            int[] bestGrid = null;
            for (int i = 0; i < 25; i++) {
                for (int p = 0; p <= E; p++) {
                    if (g[i] == p) continue;
                    int[] t = g.clone();
                    t[i] = p;
                    if (p == F) {
                        int y = i / N, x = i % N;
                        if (x > 0) t[i - 1] = W;
                        if (x < N - 1) t[i + 1] = W;
                        if (y > 0) t[i - N] = W;
                        if (y < N - 1) t[i + N] = W;
                    }
                    int gain = score(r, t) - now;
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestGrid = t;
                    }
                }
            }
            if (bestGrid == null) return now;
            g = bestGrid;
            now += bestGain;
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            int[] v = java.util.Arrays.stream(args[0].split(",")).mapToInt(Integer::parseInt).toArray();
            Rule r = new Rule("mox", v[0], v[1], v[2], v[3], v[4], v.length > 5 && v[5] == 1, v.length > 6 ? v[6] : 0);
            Result res = solve(r);
            System.out.println(r + " best " + res.best() + " ties " + res.ties() + " climb " + climb(r));
            for (String l : all(res.best())) {
                for (int y = 0; y < N; y++) System.out.println("   " + String.join(" ", l.substring(y * N, y * N + N).split("")));
                System.out.println();
            }
            return;
        }
        main2();
    }

    static void main2() {
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule("uranium (check)", 2, 1, 0, 0, 99, false, 0));
        for (int base = 2; base <= 4; base++) {
            for (int g = 1; g <= 2; g++) {
                for (int fd = 0; fd <= 1; fd++) {
                    for (int cut = 0; cut <= 1; cut++) {
                        for (int cc = 0; cc <= 2; cc++) {
                            for (int limit = base + 1; limit <= base + 4 * g + 2; limit++) {
                                for (boolean after : new boolean[] {false, true}) {
                                    if (cut == 0 && after) continue;
                                    rules.add(new Rule("mox", base, g, fd, cut, limit, after, cc));
                                }
                            }
                        }
                    }
                }
            }
        }
        for (Rule r : rules) {
            long t0 = System.currentTimeMillis();
            Result res = solve(r);
            String l = res.layout();
            long fuel = l.chars().filter(ch -> ch == 'F').count();
            long ctrl = l.chars().filter(ch -> ch == 'C').count();
            long gr = l.chars().filter(ch -> ch == 'M').count();
            if (!r.name().startsWith("uranium") && (ctrl == 0 || gr == 0 || res.ties() > 1)) continue;
            int climb = climb(r);
            
            System.out.printf("%-16s base %d g %d fd %d cut %d cost %d limit %d %-6s best %3d ties %6d climb %3d (%3.0f%%) F%2d M%2d C%2d  %s  %d ms%n",
                    r.name(), r.base(), r.g(), r.fd(), r.cut(), r.ccost(), r.limit(), r.capAfterCut() ? "after" : "before", res.best(), res.ties(), climb,
                    100.0 * climb / res.best(), fuel, gr, ctrl, l, System.currentTimeMillis() - t0);
        }
    }
}
