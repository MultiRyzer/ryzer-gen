package com.ryzer.ryzergen.client.terminal;

import com.ryzer.ryzergen.machine.fission.StationControlMenu;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The fission station's core as pixel art, the centrepiece of its control screen (design section 8,
 * "The core puzzle"; cells per art/tools/core_cells_mock.py, ported from the playable reference
 * art/concepts/station_screen/core-puzzle.html). A 5 x 5 grid of 30-pixel cells drawn one texel per
 * GUI pixel: water with depth, moving caustics, bubbles and Cherenkov blue beside lit rods; graphite
 * as a machined block whose corners glow amber towards the rods it warms; fuel as a cladding ring
 * round a pellet glowing red to white with its heat, its heat number on it, cyan bars on watered
 * sides and red on missing ones. In the MOX core a control rod is a cruciform blade round its drive hub, glowing silver towards
 * the rods it tames; a rod over its limit with no control rod flashes red. A part planned but not yet
 * in place draws dim inside a blinking amber frame with a warning tab.
 *
 * <p>Everything here is in screen order (row by row from the top left, as the player at the
 * station's front sees the core); the screen turns the channels round to match.
 */
public final class CoreArt implements AutoCloseable {
    public static final int N = StationReactor.GRID;
    public static final int CELL = 30;
    public static final int PAD = 4;
    public static final int SIZE = N * CELL + 2 * PAD;
    /** A cell's floor inside its socket. */
    private static final int INNER = CELL - 4;

    private static final int GROUND = 0xFF0B0D11;
    private static final int FRAME = 0xFF2A2E35;
    private static final int DEEP = 0xFF0A2448;
    private static final int MID = 0xFF134A86;
    private static final int SHALLOW = 0xFF2A74C0;
    private static final int CAUSTIC = 0xFF6CBCF4;
    private static final int GLINT = 0xFFD4F0FF;
    private static final int CHERENKOV = 0xFF3C8CFF;
    private static final int RIM_DARK = 0xFF1B1E22;
    private static final int RIM = 0xFF2B3036;
    private static final int GRAPHITE = 0xFF30343A;
    private static final int GRAPHITE_LINE = 0xFF23272B;
    private static final int GRAPHITE_LIT = 0xFF5A6068;
    private static final int GRAPHITE_SIDE = 0xFF474D55;
    private static final int BORE = 0xFF101215;
    private static final int CLAD_LIT = 0xFFC2C9D2;
    private static final int CLAD_DARK = 0xFF454B54;
    private static final int AMBER = 0xFFFFB84D;
    private static final int CYAN = 0xFF35C8F5;
    private static final int RED = 0xFFE0503C;
    private static final int SOCKET = 0xFF14171B;
    private static final int PLATE = 0xFF1A1E23;
    private static final int CROSS = 0xFF23282E;
    private static final int INK = 0xFF1A0F08;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF44D65E;
    private static final int GOLD = 0xFFFFD27A;
    private static final int DEAD = 0xFF4A1A10;
    private static final int SPENT = 0xFF3A3E44;
    private static final int ORANGE = 0xFFF08A24;
    private static final int MOX_MARK = 0xFFFF7A3D;
    private static final int STEEL = 0xFF17191D;
    private static final int STEEL_DOT = 0xFF1F2328;
    private static final int HUB = 0xFF3A3F47;
    private static final int CLAD = 0xFF8B939E;
    private static final int LAMP = 0xFF7AD06A;
    private static final int LAMP_IDLE = 0xFF4A3A1A;
    private static final int WARN = 0xFFF0A030;

    /** The terminal font's digits, for the heat on each rod and the hover's change. */
    private static final String[][] DIGITS = {
            {".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."}, {"..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."},
            {".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"}, {"####.", "....#", "....#", ".###.", "....#", "....#", "####."},
            {"...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."}, {"#####", "#....", "####.", "....#", "....#", "#...#", ".###."},
            {".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."}, {"#####", "....#", "...#.", "..#..", "..#..", "..#..", "..#.."},
            {".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."}, {".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."}};
    /** Sides and corners as (row, column) steps: north, south, west, east; then the corners. */
    private static final int[][] SIDES = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] CORNERS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    /**
     * What to draw: the plan and the items in its channels (StationControlMenu's ITEM codes), the
     * plan worked out, whether the station runs, the cell under the mouse (or -1) with the part
     * picked, and whether the core is solved (its frame turns gold).
     */
    public record View(StationReactor.Core core, Channel[] plan, int[] items, StationReactor.Analysis analysis, boolean running,
                       int hover, Channel tool, boolean won) {}

    private final PixelCanvas c = new PixelCanvas(SIZE, SIZE, "station_core");
    private final long start = System.currentTimeMillis();
    private final RandomSource random = RandomSource.create();
    /** Cherenkov rings: x, y, start (ms after the picture was made). */
    private final List<float[]> rings = new ArrayList<>();
    /** Sparks: x, y, vx, vy, start (ms after the picture was made), life (s), gold (0 or 1). */
    private final List<float[]> sparks = new ArrayList<>();
    private long celebration = -1;
    private int[] order = new int[0];
    private boolean[] fired = new boolean[0];
    private IntConsumer onIgnite = k -> {};

    /** The three-star sequence: rods ignite from the centre outward, each calling {@code onIgnite} with its place in line. */
    public void celebrate(Channel[] plan, IntConsumer onIgnite) {
        List<Integer> rods = new ArrayList<>();
        for (int i = 0; i < plan.length; i++) {
            if (plan[i] == Channel.FUEL) {
                rods.add(i);
            }
        }
        rods.sort((a, b) -> Double.compare(Math.hypot(a % N - 2, a / N - 2), Math.hypot(b % N - 2, b / N - 2)));
        order = rods.stream().mapToInt(Integer::intValue).toArray();
        fired = new boolean[order.length];
        celebration = System.currentTimeMillis();
        this.onIgnite = onIgnite;
    }

    public boolean celebrating() {
        return celebration >= 0;
    }

    public static int cellX(int i) {
        return PAD + i % N * CELL;
    }

    public static int cellY(int i) {
        return PAD + i / N * CELL;
    }

    /** The cell at a point on the picture, or -1. */
    public static int cellAt(double x, double y) {
        double gx = x - PAD;
        double gy = y - PAD;
        if (gx < 0 || gy < 0 || gx >= N * CELL || gy >= N * CELL) {
            return -1;
        }
        return (int) (gy / CELL) * N + (int) (gx / CELL);
    }

    /** Draws the core and sends it to its texture. */
    public void render(View view) {
        long now = System.currentTimeMillis();
        float t = (now - start) / 1000F;
        float[] flash = new float[N * N];
        float graphiteBoost = 0;
        float chase = -1;
        float scan = -1;
        if (celebration >= 0) {
            float ct = (now - celebration) / 1000F;
            if (ct < 0.4F) {
                scan = ct / 0.4F;
            }
            for (int k = 0; k < order.length; k++) {
                float at = 0.35F + k * 0.08F;
                if (ct >= at && ct < at + 0.3F) {
                    flash[order[k]] = 1 - (ct - at) / 0.3F;
                }
                if (ct >= at && !fired[k]) {
                    fired[k] = true;
                    ignite(order[k], k);
                }
            }
            if (ct > 0.9F && ct < 1.5F) {
                graphiteBoost = (float) Math.sin((ct - 0.9F) / 0.6F * Math.PI) * 1.2F;
            }
            if (ct > 1.0F && ct < 2.2F) {
                chase = (ct - 1.0F) / 1.2F;
            }
            if (ct > 2.6F) {
                celebration = -1;
            }
        }
        frame(view.won(), chase);
        for (int i = 0; i < N * N; i++) {
            drawCell(view, i, t + i * 0.37F, flash[i], graphiteBoost);
        }
        for (int k = rings.size() - 1; k >= 0; k--) {
            float[] ring = rings.get(k);
            float age = (now - start - ring[2]) / 1000F;
            if (age > 0.6F) {
                rings.remove(k);
                continue;
            }
            float rad = 4 + age * 70;
            float a = (1 - age / 0.6F) * 0.55F;
            for (int s = 0; s < 120; s++) {
                double ang = s / 120.0 * Math.PI * 2;
                int x = (int) Math.round(ring[0] + Math.cos(ang) * rad);
                int y = (int) Math.round(ring[1] + Math.sin(ang) * rad);
                if (x > 2 && y > 2 && x < SIZE - 3 && y < SIZE - 3) {
                    c.glow(x, y, CHERENKOV, a);
                }
            }
        }
        for (int k = sparks.size() - 1; k >= 0; k--) {
            float[] s = sparks.get(k);
            float age = (now - start - s[4]) / 1000F;
            if (age > s[5]) {
                sparks.remove(k);
                continue;
            }
            float x = s[0] + s[2] * age;
            float y = s[1] + s[3] * age + 30 * age * age;
            c.glow(Math.round(x), Math.round(y), s[6] > 0 ? GOLD : GLINT, 1 - age / s[5]);
        }
        if (scan >= 0) {
            int y = (int) (scan * SIZE);
            for (int x = 0; x < SIZE; x++) {
                c.glow(x, y, CYAN, 0.9F);
                c.glow(x, y - 1, CYAN, 0.3F);
            }
        }
        if (view.hover() >= 0 && view.plan()[view.hover()] != view.tool() && celebration < 0) {
            hover(view);
        }
        c.upload();
    }

    /** Draws the picture at (x, y), {@code scale} screen units a pixel. */
    public void draw(GuiGraphics graphics, int x, int y, int scale) {
        c.draw(graphics, x, y, scale);
    }

    @Override
    public void close() {
        c.close();
    }

    // ---------------------------------------------------------------- pixels

    private void put(int x, int y, int colour) {
        c.set(x, y, colour);
    }

    private void blend(int x, int y, int colour, float a) {
        if (x >= 0 && y >= 0 && x < SIZE && y < SIZE && a > 0) {
            c.set(x, y, Pixels.lerp(c.get(x, y), colour, Math.min(1, a)));
        }
    }

    private void rect(int x0, int y0, int x1, int y1, int colour) {
        c.rect(x0, y0, x1, y1, colour);
    }

    private static int mix(int a, int b, float t) {
        return Pixels.lerp(a, b, Math.max(0, Math.min(1, t)));
    }

    private static float bands(float v, int n) {
        return (float) Math.floor(Math.max(0, Math.min(0.999F, v)) * n) / (n - 1);
    }

    private void digit(int x, int y, int d, int colour) {
        String[] rows = DIGITS[Math.max(0, Math.min(9, d))];
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                if (rows[j].charAt(i) == '#') {
                    put(x + i, y + j, colour);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the cells

    private static Channel at(Channel[] plan, int r, int col) {
        return r < 0 || col < 0 || r >= N || col >= N ? null : plan[r * N + col];
    }

    private static boolean live(int item) {
        return item >= StationControlMenu.ITEM_URANIUM && item <= StationControlMenu.ITEM_U233;
    }

    private void socket(int x0, int y0) {
        rect(x0, y0, x0 + CELL - 2, y0 + CELL - 2, RIM_DARK);
        rect(x0 + 1, y0 + CELL - 3, x0 + CELL - 2, y0 + CELL - 3, RIM);
        rect(x0 + CELL - 3, y0 + 1, x0 + CELL - 3, y0 + CELL - 3, RIM);
    }

    private void floor(int a, int b) {
        rect(a, b, a + INNER - 1, b + INNER - 1, SOCKET);
        for (int k = 0; k < INNER; k += 4) {
            for (int m = 0; m < INNER; m += 4) {
                put(a + k, b + m, PLATE);
            }
        }
    }

    private void drawCell(View view, int i, float t, float flash, float graphiteBoost) {
        int x = cellX(i);
        int y = cellY(i);
        int r = i / N;
        int col = i % N;
        Channel kind = view.plan()[i];
        StationReactor.Analysis e = view.analysis();
        if (kind == Channel.COOLANT) {
            // The heat of each lit, cooled rod beside it, for the Cherenkov glow on that side.
            float[] touches = {-1, -1, -1, -1};
            if (view.running()) {
                for (int s = 0; s < 4; s++) {
                    int j = (r + SIDES[s][0]) * N + col + SIDES[s][1];
                    if (at(view.plan(), r + SIDES[s][0], col + SIDES[s][1]) == Channel.FUEL && e.units()[j] > 0 && live(view.items()[j])) {
                        touches[s] = e.units()[j] / (view.core() == StationReactor.Core.MOX ? 12F : 6F);
                    }
                }
            }
            water(x, y, touches, t);
        } else if (kind == Channel.MODERATOR) {
            boolean[] warms = new boolean[4];
            for (int s = 0; s < 4; s++) {
                warms[s] = at(view.plan(), r + CORNERS[s][0], col + CORNERS[s][1]) == Channel.FUEL;
            }
            graphite(x, y, warms, t, graphiteBoost);
        } else if (kind == Channel.CONTROL) {
            boolean[] tames = new boolean[4];
            for (int s = 0; s < 4; s++) {
                int r2 = r + CORNERS[s][0];
                int c2 = col + CORNERS[s][1];
                tames[s] = at(view.plan(), r2, c2) == Channel.FUEL && e.rodHeat()[r2 * N + c2] > view.core().limit;
            }
            control(x, y, tames, t);
        } else if (kind == Channel.FUEL) {
            fuel(view, i, x, y, t, flash);
        } else {
            socket(x, y);
            floor(x + 2, y + 2);
            int a = x + 2;
            int b = y + 2;
            int h = INNER >> 1;
            rect(a + h - 3, b + h, a + h + 2, b + h, CROSS);
            rect(a + h, b + h - 3, a + h, b + h + 2, CROSS);
        }
        if (!loaded(kind, view.items()[i])) {
            notLoaded(x, y, t);
        }
        // Something the plan no longer takes, waiting for the output port: an orange mark.
        if (view.items()[i] == StationControlMenu.ITEM_OTHER) {
            rect(x + 3, y + 3, x + 5, y + 5, ORANGE);
        }
    }

    /** Whether a planned part is in its channel (water and empty channels need none). */
    private static boolean loaded(Channel kind, int item) {
        return switch (kind) {
            case FUEL -> live(item) || item == StationControlMenu.ITEM_SPENT;
            case MODERATOR -> item == StationControlMenu.ITEM_GRAPHITE;
            case CONTROL -> item == StationControlMenu.ITEM_CONTROL;
            default -> true;
        };
    }

    /** A planned part not in the core: its cell dimmed, inside a blinking amber dashed frame with a warning tab. */
    private void notLoaded(int x0, int y0, float t) {
        for (int y = y0 + 2; y < y0 + 2 + INNER; y++) {
            for (int x = x0 + 2; x < x0 + 2 + INNER; x++) {
                put(x, y, Pixels.mul(c.get(x, y), 0.4F));
            }
        }
        if (Math.sin(t * 4) < -0.3) {
            return;
        }
        int a = x0 + 1;
        int b = y0 + 1;
        int n = CELL - 3;
        for (int k = 0; k < n; k += 4) {
            for (int d = 0; d < 2 && k + d < n; d++) {
                put(a + k + d, b, WARN);
                put(a + k + d, b + n - 1, WARN);
                put(a, b + k + d, WARN);
                put(a + n - 1, b + k + d, WARN);
            }
        }
        rect(a + n - 9, b, a + n - 1, b + 8, WARN);
        rect(a + n - 5, b + 2, a + n - 5, b + 4, GROUND);
        put(a + n - 5, b + 6, GROUND);
    }

    /**
     * A control rod seen from above: a cruciform absorber blade in brushed steel round its drive hub,
     * absorber tubes as dots along the arms, a silver glow towards each rod it tames, and a lamp on the
     * hub, green while it tames one and amber while idle.
     */
    private void control(int x0, int y0, boolean[] tames, float t) {
        socket(x0, y0);
        int n = INNER;
        int a = x0 + 2;
        int b = y0 + 2;
        int m = n >> 1;
        rect(a, b, a + n - 1, b + n - 1, STEEL);
        for (int k = 0; k < n; k += 4) {
            for (int q = 0; q < n; q += 4) {
                put(a + k, b + q, STEEL_DOT);
            }
        }
        float pulse = (float) (0.85 + 0.15 * Math.sin(t * 3));
        boolean any = false;
        for (int s = 0; s < 4; s++) {
            if (!tames[s]) {
                continue;
            }
            any = true;
            int ox = CORNERS[s][1] < 0 ? a : a + n - 1;
            int oy = CORNERS[s][0] < 0 ? b : b + n - 1;
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    double d = Math.hypot(a + i - ox, b + j - oy);
                    if (d < 8.4) {
                        blend(a + i, b + j, CLAD_LIT, 0.4F * bands((float) Math.pow(1 - d / 8.4, 1.5) * pulse, 3));
                    }
                }
            }
        }
        for (int i = 2; i < n - 2; i++) {
            for (int w = -2; w < 2; w++) {
                int colour = mix(CLAD_DARK, CLAD_LIT, w < 0 ? 1 : 0.45F);
                put(a + i, b + m + w, colour);
                put(a + m + w, b + i, colour);
            }
        }
        for (int i = 4; i < n - 4; i += 3) {
            if (Math.abs(i - m) > 4) {
                put(a + i, b + m, CLAD_DARK);
                put(a + m, b + i, CLAD_DARK);
            }
        }
        rect(a + m - 4, b + m - 4, a + m + 3, b + m + 3, RIM_DARK);
        rect(a + m - 3, b + m - 3, a + m + 2, b + m + 2, HUB);
        rect(a + m - 3, b + m - 3, a + m + 2, b + m - 3, CLAD);
        rect(a + m - 1, b + m - 1, a + m, b + m, any ? LAMP : LAMP_IDLE);
    }

    /** A number in the terminal font's digits, centred on {@code cx}. */
    private void number(int cx, int y, int value, int colour) {
        String text = String.valueOf(Math.max(0, value));
        int x = cx - (text.length() * 6 - 1) / 2;
        for (int k = 0; k < text.length(); k++) {
            digit(x + k * 6, y, text.charAt(k) - '0', colour);
        }
    }

    private void water(int x0, int y0, float[] touches, float t) {
        socket(x0, y0);
        int n = INNER;
        int a = x0 + 2;
        int b = y0 + 2;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float u = (i + 0.5F) / n;
                float v = (j + 0.5F) / n;
                float edge = Math.min(Math.min(u, v), Math.min(1 - u, 1 - v));
                int col = mix(DEEP, MID, edge * 3.2F);
                col = mix(col, SHALLOW, Math.max(0, edge * 3 - 0.75F));
                if (u < 0.08F || v < 0.08F) {
                    col = Pixels.mul(col, 0.72F);
                }
                // Caustics: where three waves cancel, light gathers in thin lines.
                float s = (float) Math.abs(Math.sin((x0 + i) * 0.55 + t * 1.3) + Math.sin((y0 + j) * 0.48 - t)
                        + Math.sin((x0 + i + y0 + j) * 0.31 + t * 0.7));
                if (s < 0.32F) {
                    col = mix(col, CAUSTIC, s < 0.14F ? 0.5F : 0.25F);
                }
                for (int side = 0; side < 4; side++) {
                    if (touches[side] < 0) {
                        continue;
                    }
                    float d = side == 0 ? v : side == 1 ? 1 - v : side == 2 ? u : 1 - u;
                    float g = (float) Math.pow(Math.max(0, 1 - d / 0.45F), 2) * (0.35F + 0.65F * touches[side]);
                    col = mix(col, CHERENKOV, 0.5F * bands(g, 4));
                }
                if (Math.abs(u + v - 0.55F) < 0.06F && u < 0.45F) {
                    col = mix(col, GLINT, 0.18F);
                }
                put(a + i, b + j, col);
            }
        }
        float[][] bubbles = {{0.3F, 0.62F}, {0.68F, 0.3F}, {0.55F, 0.78F}};
        for (float[] bubble : bubbles) {
            float rise = (t * 0.18F + bubble[0] * 3) % 1;
            put(a + (int) (bubble[0] * n), b + (int) (((bubble[1] - rise * 0.5F + 0.5F) % 1) * n), GLINT);
        }
    }

    private void graphite(int x0, int y0, boolean[] warms, float t, float boost) {
        socket(x0, y0);
        int n = INNER;
        int a = x0 + 2;
        int b = y0 + 2;
        rect(a, b, a + n - 1, b + n - 1, GRAPHITE);
        int k = 0;
        for (int y = b + 6; y < b + n - 1; y += 7, k++) {
            rect(a + 1, y, a + n - 2, y, GRAPHITE_LINE);
            for (int x = a + (k % 2 == 1 ? 5 : 12); x < a + n - 1; x += 14) {
                rect(x, y - 6, x, y - 1, GRAPHITE_LINE);
            }
        }
        rect(a, b, a + n - 1, b, GRAPHITE_LIT);
        rect(a, b, a, b + n - 1, GRAPHITE_SIDE);
        rect(a + 1, b + n - 1, a + n - 1, b + n - 1, RIM_DARK);
        rect(a + n - 1, b + 1, a + n - 1, b + n - 1, RIM_DARK);
        float cx = a + n / 2F;
        float cy = b + n / 2F;
        for (int j = -6; j <= 6; j++) {
            for (int i = -6; i <= 6; i++) {
                double d = Math.hypot(i + 0.5, j + 0.5);
                if (d <= 4.3) {
                    put((int) Math.floor(cx + i), (int) Math.floor(cy + j), BORE);
                } else if (d <= 5.2) {
                    put((int) Math.floor(cx + i), (int) Math.floor(cy + j), i + j > 0 ? GRAPHITE_LIT : RIM_DARK);
                }
            }
        }
        float pulse = (float) (0.85 + 0.15 * Math.sin(t * 3)) * (1 + boost);
        for (int s = 0; s < 4; s++) {
            if (!warms[s]) {
                continue;
            }
            int ox = CORNERS[s][1] < 0 ? a : a + n - 1;
            int oy = CORNERS[s][0] < 0 ? b : b + n - 1;
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    double d = Math.hypot(a + i - ox, b + j - oy);
                    if (d < 8.4) {
                        blend(a + i, b + j, AMBER, Math.min(0.9F, 0.55F * bands((float) Math.pow(1 - d / 8.4, 1.5) * pulse, 3)));
                    }
                }
            }
        }
    }

    private void fuel(View view, int i, int x0, int y0, float t, float flash) {
        socket(x0, y0);
        int n = INNER;
        int a = x0 + 2;
        int b = y0 + 2;
        floor(a, b);
        int r = i / N;
        int col = i % N;
        Channel[] plan = view.plan();
        StationReactor.Analysis e = view.analysis();
        boolean cooled = e.cooled()[i];
        boolean wild = e.wild()[i];
        boolean ok = cooled && !wild;
        int units = e.rodHeat()[i];
        boolean mox = view.core() == StationReactor.Core.MOX;
        int item = view.items()[i];
        boolean lit = live(item);
        float heat = mox ? Math.min(1, (units - 3) / 9F) : units / 6F;
        float cx = a + n / 2F;
        float cy = b + n / 2F;
        int face = item == StationControlMenu.ITEM_SPENT ? SPENT
                : ok ? Pixels.heat(0.45F + 0.55F * heat)
                : wild ? mix(RED, WHITE, 0.25F + 0.25F * (float) Math.sin(t * 9)) : DEAD;
        float power = lit && !view.running() ? 0.7F : 1;
        float breath = lit ? 1 + 0.05F * (float) Math.sin(t * 2.2) : 1;
        if ((ok || wild) && lit) {
            int bloom = wild ? RED : Pixels.heat(0.7F);
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    double d = Math.hypot(a + k + 0.5 - cx, b + j + 0.5 - cy);
                    if (d > 10.7 && d < 14.9) {
                        c.glow(a + k, b + j, bloom,
                                power * 0.35F * bands((float) Math.pow(1 - (d - 10.7) / 4.2, 2) * (0.3F + 0.7F * heat) * breath, 3));
                    }
                }
            }
        }
        for (int j = -13; j <= 13; j++) {
            for (int k = -13; k <= 13; k++) {
                double d = Math.hypot(k + 0.5, j + 0.5);
                float nx = (float) ((k + 0.5) / Math.max(d, 0.1));
                float ny = (float) ((j + 0.5) / Math.max(d, 0.1));
                int x = (int) Math.floor(cx + k);
                int y = (int) Math.floor(cy + j);
                if (d <= 8.0) {
                    int colour = Pixels.mul(face, (0.62F + 0.45F * bands(1 - (float) (d / 8.0 * d / 8.0) * 0.45F, 5)) * breath * power);
                    if (flash > 0) {
                        colour = mix(colour, WHITE, flash);
                    }
                    put(x, y, colour);
                } else if (d <= 10.6) {
                    int colour = mix(CLAD_DARK, CLAD_LIT, 0.25F + 0.75F * Pixels.facing(nx, ny));
                    // A MOX rod's cladding carries an orange band along its top, to tell it from uranium.
                    if (mox && d > 9.2 && d <= 9.9 && ny < -0.3F) {
                        colour = mix(colour, MOX_MARK, 0.55F);
                    }
                    if (d > 9.9) {
                        colour = Pixels.mul(colour, 0.6F);
                    }
                    put(x, y, colour);
                } else if (d <= 11.3) {
                    put(x, y, RIM_DARK);
                }
            }
        }
        number((int) Math.floor(cx), (int) Math.floor(cy) - 3, cooled ? units : 0, ok ? INK : wild ? WHITE : RED);
        for (int s = 0; s < 4; s++) {
            Channel next = at(plan, r + SIDES[s][0], col + SIDES[s][1]);
            if (next == null) {
                continue;
            }
            int colour = next == Channel.COOLANT ? CYAN : RED;
            switch (s) {
                case 0 -> rect(a + 8, b, a + n - 9, b + 1, colour);
                case 1 -> rect(a + 8, b + n - 2, a + n - 9, b + n - 1, colour);
                case 2 -> rect(a, b + 8, a + 1, b + n - 9, colour);
                default -> rect(a + n - 2, b + 8, a + n - 1, b + n - 9, colour);
            }
        }
        boolean flashOn = Math.sin(t * 9) > 0;
        for (int[] corner : CORNERS) {
            Channel part = at(plan, r + corner[0], col + corner[1]);
            int colour;
            int length = 5;
            if (part == Channel.MODERATOR) {
                colour = AMBER;
            } else if (part == Channel.CONTROL && mox) {
                colour = CLAD_LIT;
            } else if (part == Channel.FUEL && mox) {
                colour = MOX_MARK;
                length = 3;
            } else if (wild && part != null && flashOn) {
                // A runaway rod asks for a control rod on its open corners.
                colour = RED;
            } else {
                continue;
            }
            int ox = corner[1] < 0 ? a : a + n - 1;
            int oy = corner[0] < 0 ? b : b + n - 1;
            int sx = corner[1] < 0 ? 1 : -1;
            int sy = corner[0] < 0 ? 1 : -1;
            for (int k = 0; k < length; k++) {
                put(ox + k * sx, oy, colour);
                put(ox, oy + k * sy, colour);
            }
        }
    }

    private void frame(boolean won, float chase) {
        rect(0, 0, SIZE - 1, SIZE - 1, GROUND);
        // The core's frame: steel, gold once solved, with a chase of light running round it in the celebration.
        int edge = won ? GOLD : FRAME;
        for (int k = 0; k < SIZE; k++) {
            put(k, 1, edge);
            put(k, SIZE - 2, edge);
            put(1, k, edge);
            put(SIZE - 2, k, edge);
        }
        if (chase >= 0) {
            int length = SIZE - 3;
            int per = 4 * length;
            for (int s = 0; s < 40; s++) {
                int p = ((int) Math.floor(chase * per) - s + per) % per;
                int x;
                int y;
                if (p < length) {
                    x = 1 + p;
                    y = 1;
                } else if (p < 2 * length) {
                    x = SIZE - 2;
                    y = 1 + p - length;
                } else if (p < 3 * length) {
                    x = SIZE - 2 - (p - 2 * length);
                    y = SIZE - 2;
                } else {
                    x = 1;
                    y = SIZE - 2 - (p - 3 * length);
                }
                blend(x, y, WHITE, 1 - s / 40F);
            }
        }
    }

    private void outline(int i, int colour) {
        int x = cellX(i);
        int y = cellY(i);
        for (int k = 1; k < CELL - 2; k++) {
            put(x + k, y + 1, colour);
            put(x + k, y + CELL - 3, colour);
            put(x + 1, y + k, colour);
            put(x + CELL - 3, y + k, colour);
        }
    }

    /** The hover: the picked part's ghost, the change in heat, and green or red outlines on the rods it would change. */
    private void hover(View view) {
        int over = view.hover();
        Channel[] after = view.plan().clone();
        after[over] = view.tool();
        StationReactor.Analysis now = view.analysis();
        StationReactor.Analysis then = StationReactor.plan(after, view.core());
        for (int i = 0; i < N * N; i++) {
            if (i == over || view.plan()[i] != Channel.FUEL) {
                continue;
            }
            int d = then.units()[i] - now.units()[i];
            if (d != 0) {
                outline(i, d > 0 ? GREEN : RED);
            }
        }
        int x = cellX(over);
        int y = cellY(over);
        int ghost = switch (view.tool()) {
            case FUEL -> Pixels.heat(0.6F);
            case MODERATOR -> GRAPHITE_LIT;
            case COOLANT -> SHALLOW;
            case CONTROL -> CLAD_LIT;
            default -> SOCKET;
        };
        for (int j = 2; j < CELL - 2; j++) {
            for (int i = 2; i < CELL - 2; i++) {
                blend(x + i, y + j, ghost, 0.4F);
            }
        }
        outline(over, CYAN);
        int delta = then.score() - now.score();
        String label = delta > 0 ? "+" + delta : String.valueOf(delta);
        int w = label.length() * 6 + 2;
        int tone = delta > 0 ? GREEN : delta < 0 ? RED : CROSS;
        rect(x + CELL - 3 - w, y + 2, x + CELL - 3, y + 11, GROUND);
        for (int k = 0; k < label.length(); k++) {
            char ch = label.charAt(k);
            int gx = x + CELL - 1 - w + k * 6;
            if (ch == '+' || ch == '-') {
                rect(gx, y + 6, gx + 4, y + 6, tone);
                if (ch == '+') {
                    rect(gx + 2, y + 4, gx + 2, y + 8, tone);
                }
            } else {
                digit(gx, y + 3, ch - '0', tone);
            }
        }
    }

    private void ignite(int i, int k) {
        float cx = cellX(i) + CELL / 2F - 1;
        float cy = cellY(i) + CELL / 2F - 1;
        float now = System.currentTimeMillis() - start;
        rings.add(new float[] {cx, cy, now});
        for (int s = 0; s < 12; s++) {
            double a = random.nextDouble() * Math.PI * 2;
            double v = 20 + random.nextDouble() * 40;
            sparks.add(new float[] {cx, cy, (float) (Math.cos(a) * v), (float) (Math.sin(a) * v - 20), now,
                    0.5F + random.nextFloat() * 0.5F, s % 3 == 0 ? 1 : 0});
        }
        onIgnite.accept(k);
    }
}
