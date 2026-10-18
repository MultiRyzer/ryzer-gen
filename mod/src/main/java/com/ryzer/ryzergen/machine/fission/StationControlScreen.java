package com.ryzer.ryzergen.machine.fission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ryzer.ryzergen.client.terminal.ConsoleSounds;
import com.ryzer.ryzergen.client.terminal.CoreArt;
import com.ryzer.ryzergen.client.terminal.TerminalText;
import com.ryzer.ryzergen.machine.SoundSwitch;
import com.ryzer.ryzergen.machine.fission.StationReactor.Channel;
import com.ryzer.ryzergen.machine.fission.StationReactor.Core;
import com.ryzer.ryzergen.machine.fission.StationRunner.Command;
import com.ryzer.ryzergen.machine.fission.StationRunner.Status;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The fission station's one control screen (design section 8, "The core puzzle"): a full-screen
 * terminal after the approved mocks (art/tools/station_screen_v3.py, station_screen_mox.py) and the
 * playable references (art/concepts/station_screen/core-puzzle.html, core-puzzle-mox.html). Laid out
 * on a 960 x 540 grid of half GUI pixels, so at Auto GUI scale on a 1080p screen small text is 2
 * screen pixels a font pixel and the core's pixel art one GUI pixel a pixel; bigger windows scale it
 * up by whole steps, smaller ones down.
 *
 * <p>Left: the core's heat against its best, its stars, a meter, the verdict, then the load-out: a
 * rack per part (each slot one planned part: lit in the core, amber at the input port, red missing),
 * the water and the ports, and the inject and eject commands. Middle: the core as pixel art under
 * its two tabs (uranium and MOX, each its own plan; switching while the core holds parts warns, then
 * ejects them), planned with its parts and a hover that shows what a part would do. Right: the
 * running core's physics with a flux history, and a two-minute forecast of its temperature. A status
 * bar types its log along the foot. The top bar holds the switches: power, redstone, the safeties
 * (shift-click to disarm) and the sound.
 */
public class StationControlScreen extends AbstractContainerScreen<StationControlMenu> {
    /** The layout's grid: half GUI pixels. */
    private static final int W = 960;
    private static final int H = 540;
    private static final int GROUND = 0xFF0B0D11;
    private static final int BAR = 0xFF0F141A;
    private static final int BOX = 0xFF1E3A4A;
    private static final int TITLE = 0xFF35C8F5;
    private static final int LABEL = 0xFF5F7C8C;
    private static final int RULE = 0xFF8AA3B2;
    private static final int TEXT = 0xFFD6E4EC;
    private static final int DIM = 0xFF2A4656;
    private static final int UNLIT = 0xFF141C24;
    private static final int GREEN = 0xFF44D65E;
    private static final int YELLOW = 0xFFE2C13A;
    private static final int AMBER = 0xFFF0A030;
    private static final int RED = 0xFFE0503C;
    private static final int GOLD = 0xFFFFD27A;
    private static final int CYAN = 0xFF35C8F5;
    private static final int SILVER = 0xFFC2C9D2;
    private static final int MOX_MARK = 0xFFFF7A3D;
    private static final int SCRAM_LINE = 0xFF7A5418;
    private static final int RACK_RED = 0xFF5A2A22;
    /** Where the core's pixel art sits, drawn 2 units a pixel. */
    private static final int ART_X = 322;
    private static final int ART_Y = 76;
    private static final Channel[] URANIUM_TOOLS = {Channel.FUEL, Channel.MODERATOR, Channel.COOLANT, Channel.EMPTY};
    private static final Channel[] MOX_TOOLS = {Channel.FUEL, Channel.MODERATOR, Channel.COOLANT, Channel.CONTROL, Channel.EMPTY};
    /** The forecast: two minutes ahead, a sample every half second. */
    private static final int FORECAST_TICKS = 2400;
    private static final int FORECAST_EVERY = 10;
    private static final int HISTORY = 52;
    /** How long after opening the screen takes what the server sends as the starting state. */
    private static final int SETTLE_MS = 500;
    /** How long a port shows a command's error. */
    private static final int ERROR_MS = 6000;

    private final CoreArt art = new CoreArt();
    private final ConsoleSounds sounds = new ConsoleSounds();
    private final long opened = System.currentTimeMillis();
    /** The layout's transform: its top left on screen and GUI units per grid unit. */
    private float originX;
    private float originY;
    private float unit = 0.5F;
    private Channel tool = Channel.FUEL;
    private int hover = -1;
    /** What the plan last scored, to sound and log a change once the server has it. */
    private int lastScore = -1;
    private int lastStars;
    private int lastBad;
    private Core lastCore;
    private boolean won;
    /** The last cell clicked and the part, for the log line once the change arrives. */
    private int clicked = -1;
    private Channel clickedPart = Channel.EMPTY;
    private final long[] stamped = new long[3];
    /** The log line: its text and when it started typing. */
    private String log = "";
    private long logAt;
    /** Messages still to type, in turn, while the screen is idle. */
    private final List<String> queuedLogs = new ArrayList<>();
    private String hoverLog = "";
    private int seenCommand = -1;
    private long commandAt;
    /** The switch warning, while it is up. */
    private boolean warning;
    private final float[] history = new float[HISTORY];
    private long lastSample;
    /** The core's temperature a second apart, for k-effective's trend. */
    private float previousTemperature = -1;
    private float trend;
    private int lastTickCell = -1;
    /** The clickable parts drawn this frame: x0, y0, x1, y1 and what they do. */
    private final List<int[]> hits = new ArrayList<>();
    private static final int HIT_POWER = 1;
    private static final int HIT_REDSTONE = 2;
    private static final int HIT_SAFETY = 3;
    private static final int HIT_SOUND = 4;
    private static final int HIT_INJECT = 5;
    private static final int HIT_EJECT = 6;
    private static final int HIT_TAB = 7;
    private static final int HIT_WARN_GO = 8;
    private static final int HIT_WARN_CANCEL = 9;
    private static final int HIT_TOOL = 10;

    public StationControlScreen(StationControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private static String tr(String key, Object... args) {
        return Component.translatable("gui.ryzergen.console." + key, args).getString();
    }

    @Override
    protected void init() {
        // The whole window is the screen: nothing outside it to click through to.
        imageWidth = width;
        imageHeight = height;
        super.init();
        float fit = Math.min(width / (float) W, height / (float) H) * 2;
        float scale = fit >= 1 ? (float) Math.floor(fit) : fit;
        unit = scale / 2;
        originX = (width - W * unit) / 2;
        originY = (height - H * unit) / 2;
        if (log.isEmpty()) {
            queuedLogs.add(tr("log.ready"));
        }
    }

    @Override
    public void removed() {
        super.removed();
        art.close();
    }

    // ---------------------------------------------------------------- what the screen shows

    private Core core() {
        return menu.core();
    }

    private Channel[] tools() {
        return core() == Core.MOX ? MOX_TOOLS : URANIUM_TOOLS;
    }

    /** The plan in screen order: the player at the front sees the core turned round (front row at the bottom). */
    private Channel[] screenPlan() {
        Channel[] plan = new Channel[StationReactor.CHANNELS];
        for (int i = 0; i < plan.length; i++) {
            plan[i] = menu.plan(channel(i));
        }
        return plan;
    }

    /** The channel a screen cell shows. */
    private static int channel(int cell) {
        return StationReactor.CHANNELS - 1 - cell;
    }

    private int[] screenItems() {
        int[] items = new int[StationReactor.CHANNELS];
        for (int i = 0; i < items.length; i++) {
            items[i] = menu.item(channel(i));
        }
        return items;
    }

    private ItemStack[] rodsInScreenOrder() {
        ItemStack[] rods = menu.rods();
        ItemStack[] out = new ItemStack[rods.length];
        for (int i = 0; i < rods.length; i++) {
            out[i] = rods[channel(i)];
        }
        return out;
    }

    /** Parts in the core, of any kind. */
    private int partsIn() {
        int n = 0;
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            n += menu.item(i) != StationControlMenu.ITEM_NONE ? 1 : 0;
        }
        return n;
    }

    private boolean running() {
        return switch (menu.status()) {
            case WARMING, ONLINE, NO_WATER, OVERHEAT, OVERDRIVE, FLUX_TILT, UNSTABLE, RUNAWAY, INJECTING -> true;
            default -> false;
        };
    }

    private static String cellName(int cell) {
        return "" + "ABCDE".charAt(cell % CoreArt.N) + (cell / CoreArt.N + 1);
    }

    private static String coreName(Core core) {
        return tr("tab." + core.name().toLowerCase(Locale.ROOT));
    }

    private static String partKey(Channel part) {
        return switch (part) {
            case FUEL -> "fuel";
            case MODERATOR -> "graphite";
            case COOLANT -> "water";
            case CONTROL -> "control";
            default -> "clear";
        };
    }

    private static int toolColour(Channel part) {
        return switch (part) {
            case FUEL -> 0xFFF08A24;
            case MODERATOR -> 0xFF5A6068;
            case COOLANT -> 0xFF2A74C0;
            case CONTROL -> SILVER;
            default -> 0xFF2B3036;
        };
    }

    private static String pad(int value, int digits) {
        return String.format("%0" + digits + "d", Math.max(0, value));
    }

    private static String number(float value) {
        return String.format("%,d", Math.round(value));
    }

    /** A value's colour by how far up its range it sits, green through yellow to red. */
    private static int level(float f) {
        return f < 0.6F ? GREEN : f < 0.8F ? YELLOW : f < 0.92F ? AMBER : RED;
    }

    private static String clock(long seconds) {
        return String.format("T+%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }

    private static String minutes(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void say(String text) {
        log = text;
        logAt = System.currentTimeMillis();
        queuedLogs.clear();
    }

    /** Queues a core's rules, to type one after another. */
    private void queueRules(Core core) {
        String prefix = core == Core.MOX ? "log.mox.rule" : "log.rule";
        for (int n = 1; n <= (core == Core.MOX ? 3 : 2); n++) {
            queuedLogs.add(tr(prefix + n));
        }
    }

    // ---------------------------------------------------------------- watching for changes

    /** Sounds and logs a change in the plan once it arrives from the server, as the reference page does. */
    private void watch(StationReactor.Analysis plan, Channel[] channels) {
        int stars = plan.stars();
        // The plan arrives from the server just after the screen opens, and a new core's after a
        // switch: take it as it is, without a fanfare.
        if (lastScore < 0 || lastCore != core() || System.currentTimeMillis() - opened < SETTLE_MS) {
            if (lastCore == null && System.currentTimeMillis() - opened >= SETTLE_MS) {
                queueRules(core());
            }
            lastCore = System.currentTimeMillis() - opened < SETTLE_MS ? null : core();
            lastScore = plan.score();
            lastStars = stars;
            lastBad = plan.failing();
            won = stars == 3;
            return;
        }
        if (plan.score() == lastScore && stars == lastStars && plan.failing() == lastBad) {
            return;
        }
        int delta = plan.score() - lastScore;
        if (delta > 0) {
            sounds.note(Math.min(14, plan.score() / (core() == Core.MOX ? 4 : 3)), 0.6F, 60);
        } else if (delta < 0) {
            sounds.down();
        }
        if (plan.failing() > lastBad) {
            sounds.buzz();
        }
        if (stars > lastStars && stars < 3) {
            sounds.star(stars);
            stamped[stars - 1] = System.currentTimeMillis();
        }
        if (stars == 3 && lastStars < 3) {
            celebrate(channels);
        }
        if (stars < 3) {
            won = false;
        }
        if (stars != 3 && clicked >= 0) {
            String line = clickedPart == Channel.EMPTY
                    ? tr("log.cleared", cellName(clicked), pad(plan.score(), 2))
                    : tr("log.placed", cellName(clicked), tr("part." + partKey(clickedPart)), pad(plan.score(), 2));
            if (plan.uncooled() > 0) {
                line += tr("log.dry", plan.uncooled());
            }
            if (plan.runaway() > 0) {
                line += tr("log.wild", plan.runaway());
            }
            say(line);
        }
        lastScore = plan.score();
        lastStars = stars;
        lastBad = plan.failing();
    }

    private void celebrate(Channel[] channels) {
        won = true;
        art.celebrate(channels, k -> sounds.note(k + 6, 0.5F, 0));
        sounds.fanfare();
        long now = System.currentTimeMillis();
        for (int k = 0; k < 3; k++) {
            stamped[k] = now + 1200 + k * 150;
        }
        log = tr("log.won", core().best);
        logAt = now + 1300;
        queuedLogs.clear();
    }

    /** The result of an inject, eject or switch, once the server has answered. */
    private void watchCommands() {
        int count = menu.commandCount();
        if (seenCommand < 0 || System.currentTimeMillis() - opened < SETTLE_MS) {
            seenCommand = count;
            return;
        }
        if (count == seenCommand) {
            return;
        }
        seenCommand = count;
        commandAt = System.currentTimeMillis();
        Command outcome = menu.commandOutcome();
        if (outcome == Command.INJECTED || outcome == Command.INJECTED_PART) {
            sounds.inject();
        } else if (outcome == Command.EJECTED || outcome == Command.DRAINING || outcome == Command.SWITCHED && menu.commandMoved() > 0) {
            sounds.eject();
        } else if (outcome.error()) {
            sounds.buzz();
        }
        say(outcome == Command.SWITCHED
                ? tr("result.switched.log", coreName(core()), menu.commandMoved())
                : tr("result." + key(outcome) + ".log", menu.commandMoved(), menu.commandMissing()));
        if (outcome == Command.SWITCHED) {
            // The new core's rules follow its selection.
            queueRules(core());
            tool = Channel.FUEL;
        }
    }

    private static String key(Command command) {
        return command.name().toLowerCase(Locale.ROOT);
    }

    /** Whether the last command, within the last few seconds, failed in one of these ways. */
    private boolean recentError(Command... kinds) {
        if (commandAt <= 0 || System.currentTimeMillis() - commandAt > ERROR_MS) {
            return false;
        }
        Command outcome = menu.commandOutcome();
        for (Command kind : kinds) {
            if (outcome == kind) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        sounds.mute(menu.muted());
        sounds.update();
        Core core = core();
        Channel[] plan = screenPlan();
        int[] items = screenItems();
        StationReactor.Analysis planned = StationReactor.plan(plan, core);
        boolean overdrive = !menu.safeties();
        StationReactor.Analysis live = StationReactor.analyse(plan, rodsInScreenOrder(), overdrive, core);
        watch(planned, plan);
        watchCommands();
        sample(live);
        if (!List.of(tools()).contains(tool)) {
            tool = Channel.FUEL;
        }
        float ux = (mouseX - originX) / unit;
        float uy = (mouseY - originY) / unit;
        int over = warning ? -1 : CoreArt.cellAt((ux - ART_X) / 2, (uy - ART_Y) / 2);
        if (over != hover) {
            hover = over;
            if (over >= 0 && over != lastTickCell) {
                sounds.tick();
            }
            lastTickCell = over;
        }
        hoverLog = hover >= 0 && plan[hover] != tool ? hoverLine(plan, planned) : "";

        graphics.fill(0, 0, width, height, GROUND);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(originX, originY, 0);
        pose.scale(unit, unit, 1);
        hits.clear();
        float hx = warning ? -1 : ux;
        float hy = warning ? -1 : uy;
        topBar(graphics, hx, hy);
        output(graphics, planned, live);
        loadout(graphics, plan, items, hx, hy);
        core(graphics, plan, items, planned, hx, hy);
        physics(graphics, live, overdrive);
        forecast(graphics, live, overdrive);
        statusBar(graphics, planned, live, overdrive);
        if (warning) {
            hits.clear();
            warning(graphics, ux, uy);
        }
        pose.popPose();
    }

    /** Samples the flux history once a second, and the temperature's trend for k-effective. */
    private void sample(StationReactor.Analysis live) {
        long now = System.currentTimeMillis();
        if (now - lastSample < 1000) {
            return;
        }
        lastSample = now;
        System.arraycopy(history, 1, history, 0, HISTORY - 1);
        float full = core().best * core().unit * live.heatScale();
        history[HISTORY - 1] = running() ? Math.min(1, live.generation() / full * (0.9F + 0.1F * (float) Math.random())) : 0;
        float t = menu.temperature();
        trend = previousTemperature < 0 ? 0 : t - previousTemperature;
        previousTemperature = t;
    }

    /** A thin box with its title cut into the top line, and a note on the right. */
    private static void box(GuiGraphics graphics, int x0, int y0, int x1, int y1, String title, String note, int edge, int noteColour) {
        graphics.renderOutline(x0, y0, x1 - x0 + 1, y1 - y0 + 1, edge);
        if (!title.isEmpty()) {
            graphics.fill(x0 + 8, y0, x0 + 8 + TerminalText.width(title) + 8, y0 + 1, GROUND);
            TerminalText.draw(graphics, title, x0 + 12, y0 - 3, TITLE, 1);
        }
        if (!note.isEmpty()) {
            int nw = TerminalText.width(note);
            graphics.fill(x1 - 12 - nw - 4, y0, x1 - 7, y0 + 1, GROUND);
            TerminalText.right(graphics, note, x1 - 10, y0 - 3, noteColour, 1);
        }
    }

    private static void box(GuiGraphics graphics, int x0, int y0, int x1, int y1, String title, String note, int edge) {
        box(graphics, x0, y0, x1, y1, title, note, edge, LABEL);
    }

    private static void row(GuiGraphics graphics, int x0, int x1, int y, String label, String value, int colour) {
        TerminalText.draw(graphics, label, x0, y, LABEL, 1);
        TerminalText.right(graphics, value, x1 + 1, y, colour, 1);
    }

    private static boolean inside(float x, float y, int[] r) {
        return x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3];
    }

    private void topBar(GuiGraphics graphics, float ux, float uy) {
        float x = TerminalText.draw(graphics, tr("crumbs.root"), 14, 11, CYAN, 1);
        TerminalText.draw(graphics, tr("crumbs.path"), x + 8, 11, LABEL, 1);
        Status status = menu.status();
        String pill = switch (status) {
            case FLUX_TILT -> tr("status.flux_tilt", minutes(menu.tiltSeconds()));
            case INJECTING -> tr("status.injecting", minutes(menu.reserveSeconds()));
            default -> tr("status." + status.name().toLowerCase(Locale.ROOT));
        };
        int tone = switch (status) {
            case ONLINE -> GREEN;
            case OVERDRIVE, WARMING -> AMBER;
            case OFFLINE, NO_FUEL -> LABEL;
            default -> RED;
        };
        if (won && tone == GREEN) {
            tone = GOLD;
        }
        boolean blink = status.alarm() && System.currentTimeMillis() / 400 % 2 == 0;
        int pw = TerminalText.width(pill) * 2 + 16;
        int px0 = 946 - pw;
        graphics.fill(px0, 6, 947, 25, blink ? tone : (tone & 0x00FFFFFF) | 0x26000000);
        graphics.renderOutline(px0, 6, pw + 1, 19, tone);
        TerminalText.centre(graphics, pill, px0 + pw / 2F + 1, 9, blink ? GROUND : tone, 2);
        // The switches, right to left from the pill.
        String[] labels = {
                tr(menu.enabled() ? "power.on" : "power.off"),
                tr("redstone." + menu.redstoneMode().name().toLowerCase(Locale.ROOT)),
                tr(menu.safeties() ? "safeties.on" : "safeties.off"),
                tr(menu.muted() ? "sound.off" : "sound.on")};
        int[] colours = {menu.enabled() ? GREEN : RED, LABEL, menu.safeties() ? GREEN : RED, menu.muted() ? LABEL : CYAN};
        int[] ids = {HIT_POWER, HIT_REDSTONE, HIT_SAFETY, HIT_SOUND};
        int right = px0 - 16;
        int[][] areas = new int[4][];
        for (int i = 3; i >= 0; i--) {
            int w = TerminalText.width(labels[i]);
            areas[i] = new int[] {right - w - 3, 7, right + 3, 22, ids[i]};
            right -= w + 22;
        }
        for (int i = 0; i < 4; i++) {
            int[] a = areas[i];
            if (inside(ux, uy, a)) {
                graphics.renderOutline(a[0], a[1], a[2] - a[0] + 1, a[3] - a[1] + 1, BOX);
            }
            TerminalText.draw(graphics, labels[i], a[0] + 3, 11, colours[i], 1);
            hits.add(a);
        }
        graphics.fill(0, 31, W, 32, BOX);
    }

    private void output(GuiGraphics graphics, StationReactor.Analysis plan, StationReactor.Analysis live) {
        Core core = core();
        box(graphics, 12, 46, 300, 236, tr("output"), tr("output.note"), won ? GOLD : BOX);
        String heat = pad(plan.score(), 2);
        TerminalText.draw(graphics, heat, 29, 69, UNLIT, 8);
        TerminalText.draw(graphics, heat, 26, 66, TEXT, 8);
        TerminalText.draw(graphics, "/" + core.best, 126, 106, LABEL, 3);
        int stars = plan.stars();
        String[] shape = {"..#..", "..#..", "#####", ".###.", "##.##"};
        long now = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            boolean on = i < stars && now >= stamped[i];
            float age = (now - stamped[i]) / 450F;
            float scale = on && age >= 0 && age < 1 ? 1 + 1.4F * (1 - age) * (1 - age) : 1;
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(210 + i * 28, 82, 0);
            pose.scale(scale, scale, 1);
            for (int dy = 0; dy < 5; dy++) {
                for (int dx = 0; dx < 5; dx++) {
                    if (shape[dy].charAt(dx) == '#') {
                        graphics.fill(dx * 4 - 10, dy * 4 - 10, dx * 4 - 6, dy * 4 - 6, on ? AMBER : UNLIT);
                    }
                }
            }
            pose.popPose();
        }
        TerminalText.draw(graphics, tr("stars", stars), 200, 98, LABEL, 1);
        int segments = (core.best + 1) / 2;
        int width = 262 / segments - 2;
        int lit = plan.score() / 2;
        for (int i = 0; i < segments; i++) {
            int x = 26 + i * (width + 2);
            graphics.fill(x, 140, x + width, 146, i < lit ? (won ? GOLD : CYAN) : UNLIT);
        }
        String verdict;
        int tone;
        if (won) {
            verdict = tr("verdict.won");
            tone = GOLD;
        } else if (plan.rods() == 0) {
            verdict = tr("verdict.empty");
            tone = AMBER;
        } else if (plan.uncooled() > 0) {
            verdict = plan.uncooled() == 1 ? tr("verdict.dry_one") : tr("verdict.dry", plan.uncooled());
            tone = RED;
        } else if (plan.runaway() > 0) {
            verdict = plan.runaway() == 1 ? tr("verdict.wild_one") : tr("verdict.wild", plan.runaway());
            tone = RED;
        } else {
            verdict = tr("verdict.steady");
            tone = GREEN;
        }
        TerminalText.draw(graphics, verdict, 26, 154, tone, 2);
        int toBest = core.best - plan.score();
        row(graphics, 26, 286, 178, tr("row.best"), tr("unit.heat", toBest <= 0 ? "+00" : "-" + pad(toBest, 2)),
                toBest <= 0 ? GREEN : toBest <= 6 ? AMBER : TEXT);
        row(graphics, 26, 286, 190, tr("row.rods"), pad(plan.rods() - plan.uncooled(), 2) + " / " + pad(plan.rods(), 2),
                plan.uncooled() > 0 ? RED : GREEN);
        if (core == Core.MOX) {
            row(graphics, 26, 286, 202, tr("row.tamed"), pad(plan.hot() - plan.runaway(), 2) + " / " + pad(plan.hot(), 2),
                    plan.runaway() > 0 ? RED : GREEN);
        } else {
            row(graphics, 26, 286, 202, tr("row.graphite"), pad(plan.graphite(), 2), TEXT);
        }
        // What the plan makes once filled: with the fuel in the core, or the core's own if there is none yet.
        float scale = live.rods() > 0 ? live.heatScale() : 1;
        float power = plan.failing() > 0 ? 0 : plan.generation() * scale * StationReactor.efficiency(plan.settledTemperature(), !menu.safeties())
                * (menu.safeties() ? 1 : StationReactor.OVERDRIVE_HEAT);
        row(graphics, 26, 286, 214, tr("row.power"), tr("unit.fet", number(power)), TEXT);
    }

    /** The load-out: a rack per part, each slot one planned part, the count large; then the water, the ports and the commands. */
    private void loadout(GuiGraphics graphics, Channel[] plan, int[] items, float ux, float uy) {
        Core core = core();
        String id = core.name().toLowerCase(Locale.ROOT);
        boolean paused = !menu.feeding();
        box(graphics, 12, 252, 300, 500, tr("loadout"), paused ? tr("loadout.paused") : tr("loadout." + id), BOX, paused ? AMBER : LABEL);
        int y = 266;
        y = rack(graphics, y, plan, items, Channel.FUEL, menu.inputFuel(), tr("rack.fuel"), tr("rack.note." + id));
        y = rack(graphics, y, plan, items, Channel.MODERATOR, menu.inputGraphite(), tr("rack.graphite"), "");
        if (core == Core.MOX) {
            rack(graphics, y, plan, items, Channel.CONTROL, menu.inputControl(), tr("rack.control"), "");
        }
        int water = menu.water();
        TerminalText.draw(graphics, tr("row.water"), 26, 406, LABEL, 1);
        graphics.fill(66, 407, 231, 412, UNLIT);
        graphics.fill(66, 407, 66 + Math.round(165 * Math.min(1, water / (float) StationRunner.WATER_CAPACITY)), 412, 0xFF2A74C0);
        TerminalText.right(graphics, water > 0 ? tr("unit.mb", number(water)) : tr("water.none"), 287, 406,
                water > StationRunner.WATER_CAPACITY / 4 ? GREEN : water > 0 ? AMBER : RED, 1);
        boolean inError = recentError(Command.NO_INPUT);
        boolean outError = recentError(Command.NO_OUTPUT, Command.EJECTED_PART);
        float x = TerminalText.draw(graphics, tr("port.in"), 26, 420, LABEL, 1);
        TerminalText.draw(graphics, inError ? tr("port.err.input") : port(menu.inputKind(), -1), x + 6, 420,
                inError ? RED : menu.inputKind() == 0 ? AMBER : TEXT, 1);
        String out = outError ? tr(menu.commandOutcome() == Command.EJECTED_PART ? "port.err.full" : "port.err.output")
                : port(menu.outputKind(), menu.outputFree());
        float ox = 287 - TerminalText.width(out);
        TerminalText.draw(graphics, out, ox, 420, outError ? RED : menu.outputKind() == 0 ? AMBER : TEXT, 1);
        TerminalText.right(graphics, tr("port.out"), ox - 6, 420, LABEL, 1);
        int[] inject = {26, 436, 286, 460, HIT_INJECT};
        int[] eject = {26, 466, 286, 490, HIT_EJECT};
        key(graphics, inject, tr("inject"), inside(ux, uy, inject), paused && System.currentTimeMillis() / 600 % 2 == 0 ? AMBER : CYAN);
        key(graphics, eject, tr("eject"), inside(ux, uy, eject), CYAN);
        hits.add(inject);
        hits.add(eject);
    }

    /**
     * One part's rack: its label, the count large (in the core against planned), and a slot per
     * planned part, sorted lit (in the core), amber (waiting at the input port) and red (missing).
     * Returns the y of the next rack.
     */
    private int rack(GuiGraphics graphics, int y, Channel[] plan, int[] items, Channel part, int available, String label, String note) {
        int planned = 0;
        int in = 0;
        for (int i = 0; i < plan.length; i++) {
            if (plan[i] != part) {
                continue;
            }
            planned++;
            int item = items[i];
            in += switch (part) {
                case FUEL -> item >= StationControlMenu.ITEM_URANIUM && item <= StationControlMenu.ITEM_U233 ? 1 : 0;
                case MODERATOR -> item == StationControlMenu.ITEM_GRAPHITE ? 1 : 0;
                default -> item == StationControlMenu.ITEM_CONTROL ? 1 : 0;
            };
        }
        // A pipe on the input port brings what it has as it comes: its slots wait rather than miss.
        int waiting = Math.min(planned - in, menu.inputKind() == 2 ? planned - in : menu.inputKind() == 1 ? available : 0);
        float x = TerminalText.draw(graphics, label, 26, y + 2, LABEL, 1);
        if (!note.isEmpty()) {
            TerminalText.draw(graphics, note, x + 8, y + 2, core() == Core.MOX ? MOX_MARK : 0xFFFF8A3D, 1);
        }
        int colour = planned == 0 ? LABEL : in == planned ? GREEN : in + waiting == planned ? AMBER : RED;
        TerminalText.right(graphics, in + "/" + planned, 287, y - 2, colour, 2);
        boolean rod = part == Channel.FUEL;
        int pitch = rod ? 13 : 21;
        int fit = 260 / pitch;
        int top = y + 16;
        if (planned == 0) {
            TerminalText.draw(graphics, tr("rack.none"), 26, top + (rod ? 9 : 5), DIM, 1);
        }
        for (int k = 0; k < Math.min(planned, fit); k++) {
            if (k == fit - 1 && planned > fit) {
                TerminalText.draw(graphics, "+" + (planned - fit + 1), 26 + k * pitch, top + 5, LABEL, 1);
                break;
            }
            int state = k < in ? 0 : k < in + waiting ? 1 : 2;
            int sx = 26 + k * pitch;
            if (rod) {
                slotRod(graphics, sx, top, state);
            } else {
                slotBlock(graphics, sx, top, state, part);
            }
        }
        return top + (rod ? 26 : 18) + 10;
    }

    /** A fuel slot, 10 x 26: a rod standing in it when it is in the core. */
    private void slotRod(GuiGraphics graphics, int x, int y, int state) {
        graphics.renderOutline(x, y, 10, 26, state == 2 ? RACK_RED : BOX);
        if (state == 0) {
            graphics.fill(x + 3, y + 3, x + 7, y + 23, SILVER);
            graphics.fill(x + 3, y + 3, x + 4, y + 23, 0xFFE6EBF0);
            graphics.fill(x + 3, y + 3, x + 7, y + 8, core() == Core.MOX ? GOLD : 0xFFFF8A3D);
        } else if (state == 1) {
            for (int yy = y + 3; yy < y + 23; yy += 3) {
                graphics.fill(x + 3, yy, x + 7, yy + 1, AMBER);
            }
        } else {
            graphics.fill(x + 3, y + 21, x + 7, y + 23, RED);
        }
    }

    /** A graphite or control rod slot, 18 x 18. */
    private static void slotBlock(GuiGraphics graphics, int x, int y, int state, Channel part) {
        graphics.renderOutline(x, y, 18, 18, state == 2 ? RACK_RED : BOX);
        if (state == 0 && part == Channel.MODERATOR) {
            graphics.fill(x + 3, y + 3, x + 15, y + 15, 0xFF30343A);
            graphics.fill(x + 3, y + 3, x + 15, y + 4, 0xFF5A6068);
            graphics.fill(x + 7, y + 7, x + 11, y + 11, 0xFF101215);
        } else if (state == 0) {
            graphics.fill(x + 3, y + 8, x + 15, y + 10, SILVER);
            graphics.fill(x + 8, y + 3, x + 10, y + 15, SILVER);
            graphics.fill(x + 7, y + 7, x + 11, y + 11, 0xFF3A3F47);
        } else if (state == 1) {
            for (int k = 3; k < 15; k += 3) {
                graphics.fill(x + k, y + 3, x + k + 1, y + 4, AMBER);
                graphics.fill(x + k, y + 14, x + k + 1, y + 15, AMBER);
                graphics.fill(x + 3, y + k, x + 4, y + k + 1, AMBER);
                graphics.fill(x + 14, y + k, x + 15, y + k + 1, AMBER);
            }
        } else {
            graphics.fill(x + 7, y + 12, x + 11, y + 14, RED);
        }
    }

    private static String port(int kind, int free) {
        return switch (kind) {
            case 1 -> free >= 0 ? tr("port.chest_free", free) : tr("port.chest");
            case 2 -> tr("port.pipe");
            default -> tr("port.none");
        };
    }

    /** A command: a thin outline, filled while the mouse is on it. */
    private static void key(GuiGraphics graphics, int[] a, String label, boolean lit, int colour) {
        if (lit) {
            graphics.fill(a[0], a[1], a[2] + 1, a[3] + 1, colour);
        } else {
            graphics.renderOutline(a[0], a[1], a[2] - a[0] + 1, a[3] - a[1] + 1, colour);
        }
        TerminalText.centre(graphics, label, (a[0] + a[2]) / 2F + 1, a[1] + 5, lit ? GROUND : colour, 2);
    }

    private void core(GuiGraphics graphics, Channel[] plan, int[] items, StationReactor.Analysis planned, float ux, float uy) {
        Core core = core();
        box(graphics, 312, 46, 648, 500, "", tr(running() ? "core.live" : "core.plan"), won ? GOLD : BOX);
        tabs(graphics, ux, uy);
        art.render(new CoreArt.View(core, plan, items, planned, running(), hover, tool, won));
        art.draw(graphics, ART_X, ART_Y, 2);
        int gx0 = ART_X + 2 * CoreArt.PAD;
        int gy0 = ART_Y + 2 * CoreArt.PAD;
        for (int i = 0; i < CoreArt.N; i++) {
            TerminalText.draw(graphics, String.valueOf("ABCDE".charAt(i)), gx0 + i * 60 + 27, gy0 - 14, LABEL, 1);
            TerminalText.draw(graphics, String.valueOf(i + 1), gx0 - 13, gy0 + i * 60 + 26, LABEL, 1);
        }
        Channel[] tools = tools();
        int width = tools.length == 5 ? 62 : 75;
        int pitch = tools.length == 5 ? 66 : 80;
        int start = tools.length == 5 ? 317 : 326;
        for (int i = 0; i < tools.length; i++) {
            int x0 = start + i * pitch;
            int[] area = {x0, 420, x0 + width - 1, 438, HIT_TOOL + i};
            boolean picked = tools[i] == tool;
            if (picked) {
                graphics.fill(x0, 420, x0 + width, 439, CYAN);
            } else {
                graphics.renderOutline(x0, 420, width, 19, inside(ux, uy, area) ? CYAN : BOX);
            }
            graphics.fill(x0 + 4, 426, x0 + 9, 432, toolColour(tools[i]));
            TerminalText.draw(graphics, tr("tool." + partKey(tools[i])), x0 + 12, 426, picked ? GROUND : TEXT, 1);
            hits.add(area);
        }
        TerminalText.centre(graphics, tr("hint", tools.length), 480, 448, LABEL, 1);
        // The legend, centred on one line; the MOX core adds its own marks.
        List<Object[]> legend = new ArrayList<>(List.of(new Object[] {tr("legend.water"), CYAN}, new Object[] {tr("legend.dry"), RED},
                new Object[] {tr("legend.graphite"), GOLD}));
        if (core == Core.MOX) {
            legend.add(new Object[] {tr("legend.mox"), MOX_MARK});
            legend.add(new Object[] {tr("legend.control"), SILVER});
        }
        legend.add(new Object[] {tr("legend.unloaded"), AMBER});
        int total = 0;
        for (Object[] entry : legend) {
            total += TerminalText.width((String) entry[0]) + 13 + 10;
        }
        int x = 480 - (total - 10) / 2;
        for (Object[] entry : legend) {
            graphics.fill(x, 465, x + 9, 468, (Integer) entry[1]);
            x = (int) TerminalText.draw(graphics, (String) entry[0], x + 13, 462, LABEL, 1) + 10;
        }
        String rules = core == Core.MOX ? "rules.mox." : "rules.uranium.";
        TerminalText.centre(graphics, tr(rules + "1"), 480, 476, RULE, 1);
        TerminalText.centre(graphics, tr(rules + "2"), 480, 488, RULE, 1);
    }

    /** The core tabs, cut into the core box's top line like a title: each core's name and its plan's score. */
    private void tabs(GuiGraphics graphics, float ux, float uy) {
        int x = 320;
        Core picked = core();
        Channel[] other = menu.otherPlan();
        for (Core tab : Core.values()) {
            int score = StationReactor.plan(tab == picked ? menu.plan() : other, tab).score();
            String text = coreName(tab) + "  " + pad(score, 2);
            int w = TerminalText.width(text) + 16;
            int[] area = {x, 38, x + w, 52, HIT_TAB};
            boolean lit = tab != picked && inside(ux, uy, area);
            graphics.fill(x, 38, x + w + 1, 53, tab == picked ? CYAN : GROUND);
            if (tab != picked) {
                graphics.renderOutline(x, 38, w + 1, 15, lit ? CYAN : BOX);
                hits.add(area);
            }
            TerminalText.draw(graphics, text, x + 8, 42, tab == picked ? GROUND : lit ? TEXT : LABEL, 1);
            x += w + 6;
        }
    }

    /** The warning a switch of core gives while the core holds parts: confirming ejects them. */
    private void warning(GuiGraphics graphics, float ux, float uy) {
        graphics.fill(0, 0, W, H, 0xA60B0D11);
        int x0 = 270;
        int y0 = 190;
        int x1 = 690;
        int y1 = 350;
        Core to = Core.byId(1 - core().ordinal());
        graphics.fill(x0, y0, x1 + 1, y1 + 1, GROUND);
        graphics.renderOutline(x0, y0, x1 - x0 + 1, y1 - y0 + 1, AMBER);
        graphics.fill(x0, y0, x1 + 1, y0 + 15, AMBER);
        TerminalText.draw(graphics, tr("warn.head", coreName(to)), x0 + 8, y0 + 4, GROUND, 1);
        int fuel = 0;
        int graphite = 0;
        int control = 0;
        int other = 0;
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            switch (menu.item(i)) {
                case StationControlMenu.ITEM_NONE -> { }
                case StationControlMenu.ITEM_GRAPHITE -> graphite++;
                case StationControlMenu.ITEM_CONTROL -> control++;
                case StationControlMenu.ITEM_URANIUM, StationControlMenu.ITEM_MOX, StationControlMenu.ITEM_U233 -> fuel++;
                default -> other++;
            }
        }
        List<String> parts = new ArrayList<>();
        if (fuel > 0) parts.add(tr("warn.fuel", fuel));
        if (graphite > 0) parts.add(tr("warn.graphite", graphite));
        if (control > 0) parts.add(tr("warn.control", control));
        if (other > 0) parts.add(tr("warn.other", other));
        TerminalText.draw(graphics, tr("warn.big", fuel + graphite + control + other), x0 + 14, y0 + 30, AMBER, 2);
        TerminalText.draw(graphics, tr("warn.1", coreName(core()), String.join(", ", parts)), x0 + 14, y0 + 58, TEXT, 1);
        TerminalText.draw(graphics, tr("warn.2"), x0 + 14, y0 + 72, TEXT, 1);
        TerminalText.draw(graphics, tr("warn.3", port(menu.outputKind(), menu.outputFree()), coreName(core())), x0 + 14, y0 + 86, LABEL, 1);
        int[] go = {x0 + 14, y0 + 112, x0 + 260, y0 + 136, HIT_WARN_GO};
        int[] cancel = {x0 + 272, y0 + 112, x1 - 14, y0 + 136, HIT_WARN_CANCEL};
        key(graphics, go, tr("warn.go"), true, inside(ux, uy, go) ? GOLD : AMBER);
        key(graphics, cancel, tr("warn.cancel"), inside(ux, uy, cancel), LABEL);
        hits.add(go);
        hits.add(cancel);
    }

    /** What a hovered part would do, for the log line. */
    private String hoverLine(Channel[] plan, StationReactor.Analysis now) {
        Channel[] after = plan.clone();
        after[hover] = tool;
        StationReactor.Analysis then = StationReactor.plan(after, core());
        int delta = then.score() - now.score();
        String line = tr("log.hover", cellName(hover), tr("part." + partKey(tool)), delta > 0 ? "+" + delta : String.valueOf(delta));
        if (tool == Channel.FUEL && !then.cooled()[hover]) {
            int dry = 0;
            for (int n : StationReactor.neighbours(hover)) {
                dry += after[n] == Channel.COOLANT ? 0 : 1;
            }
            line += tr("log.hover.needs", dry);
        } else if (tool == Channel.FUEL && then.wild()[hover]) {
            line += tr("log.hover.control");
        } else if (then.failing() > now.failing()) {
            line += tr("log.hover.dries", then.failing() - now.failing());
        } else if (then.failing() < now.failing()) {
            line += tr("log.hover.cools", now.failing() - then.failing());
        }
        return line;
    }

    private void physics(GuiGraphics graphics, StationReactor.Analysis live, boolean overdrive) {
        box(graphics, 660, 46, 948, 300, tr("physics"), clock((System.currentTimeMillis() - opened) / 1000), BOX);
        boolean on = running();
        float full = core().best * core().unit * live.heatScale();
        float thermal = on ? live.generation() : 0;
        float load = full <= 0 ? 0 : thermal / full;
        float temperature = menu.temperature();
        float k = live.rods() == 0 ? 0 : !on ? 0.95F : 1 + Math.max(-0.0099F, Math.min(0.0099F, trend * 0.0005F));
        float pcm = k <= 0 ? 0 : (k - 1) / k * 100_000;
        float water = on ? (live.generation() - live.stranded()) / StationRunner.heatPerWater(overdrive) : 0;
        float burnup = 0;
        int rods = 0;
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            int item = menu.item(i);
            if (item >= StationControlMenu.ITEM_URANIUM && item <= StationControlMenu.ITEM_U233) {
                burnup += 1 - menu.fuelLeft(i);
                rods++;
            }
        }
        burnup = rods == 0 ? 0 : burnup / rods;
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] {"thermal", tr("unit.fet", number(thermal)), load});
        rows.add(new Object[] {"efficiency", tr("unit.percent", String.format("%.1f", menu.efficiencyPermille() / 10F)),
                menu.efficiencyPermille() / 1000F});
        rows.add(new Object[] {"temperature", tr("unit.c", pad(Math.round(temperature), 4)), temperature / StationRunner.SCRAM_TEMPERATURE});
        rows.add(new Object[] {"flux", tr("unit.flux", String.format("%.2f", thermal / StationReactor.URANIUM_HEAT * 0.08F)), load * 0.8F});
        rows.add(new Object[] {"k", String.format("%.4f", k), on ? Math.abs(k - 1) * 40 + 0.2F : 0.1F});
        rows.add(new Object[] {"reactivity", tr("unit.pcm", (pcm >= 0 ? "+" : "") + Math.round(pcm)), on ? Math.abs(pcm) / 100 : 0.1F});
        if (core() == Core.MOX) {
            rows.add(new Object[] {"controls", pad(live.controls(), 2), 0.2F});
        }
        rows.add(new Object[] {"channels", pad(live.working(), 2) + " / " + pad(live.coolants(), 2), live.coolants() == 0 ? 0F : 0.3F});
        rows.add(new Object[] {"draw", tr("unit.mbt", number(water)), load * 0.9F});
        rows.add(new Object[] {"steam", tr("unit.mbt", number(water)), load * 0.9F});
        rows.add(new Object[] {"burnup", tr("unit.percent", String.format("%.1f", burnup * 100)), burnup});
        rows.add(new Object[] {"tilt", on ? String.format("%.2f", tilt(live)) : "-", on ? (tilt(live) - 1) * 4 : 0F});
        rows.add(new Object[] {"steady", tr("steady.value", String.format("%.2f", 1 + menu.bonus() / 1000F),
                String.format("%.1f", menu.streak() / 10F)), 0F});
        if (menu.reserveBays() > 0) {
            rows.add(new Object[] {"reserve", tr("reserve.value", menu.reserveBays(), minutes(menu.reserveSeconds())), 0F});
        }
        int step = rows.size() > 12 ? 11 : 12;
        for (int i = 0; i < rows.size(); i++) {
            Object[] r = rows.get(i);
            row(graphics, 674, 934, 62 + i * step, tr("row." + r[0]), (String) r[1], level((Float) r[2]));
        }
        TerminalText.draw(graphics, tr("history"), 674, 216, LABEL, 1);
        for (int i = 0; i < HISTORY; i++) {
            int x = 674 + i * 5;
            int h = Math.max(1, Math.round(history[i] * 64));
            graphics.fill(x, 286 - h, x + 4, 286, CYAN);
            if (h > 38) {
                graphics.fill(x, 286 - h, x + 4, 286 - 38, 0xFF7FDCFF);
            }
            if (h > 54) {
                graphics.fill(x, 286 - h, x + 4, 286 - 54, TEXT);
            }
        }
        graphics.fill(674, 287, 935, 288, BOX);
    }

    /** The hottest quadrant of the core against the average, from the heat of each rod (the middle row and column split). */
    private static float tilt(StationReactor.Analysis live) {
        float[] q = new float[4];
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            int r = i / CoreArt.N;
            int c = i % CoreArt.N;
            float heat = live.heat()[i];
            for (int k = 0; k < 4; k++) {
                float wr = r == 2 ? 0.5F : (r < 2) == (k < 2) ? 1 : 0;
                float wc = c == 2 ? 0.5F : (c < 2) == (k % 2 == 0) ? 1 : 0;
                q[k] += heat * wr * wc;
            }
        }
        float mean = (q[0] + q[1] + q[2] + q[3]) / 4;
        return mean <= 0 ? 1 : Math.max(Math.max(q[0], q[1]), Math.max(q[2], q[3])) / mean;
    }

    private void forecast(GuiGraphics graphics, StationReactor.Analysis live, boolean overdrive) {
        box(graphics, 660, 316, 948, 500, tr("forecast"), tr("forecast.note"), BOX);
        Status status = menu.status();
        int unstableIn = status == Status.UNSTABLE ? 0 : status == Status.FLUX_TILT ? menu.tiltSeconds() * 20 : -1;
        StationRunner.Forecast f = StationRunner.forecast(live, menu.temperature(), running(), overdrive, unstableIn,
                menu.reserveSeconds() * 20, FORECAST_TICKS, FORECAST_EVERY);
        float[] samples = f.samples();
        float end = samples[samples.length - 1];
        String verdict;
        int tone;
        if (f.outcome() == StationRunner.Outcome.MELTDOWN) {
            verdict = tr("forecast.meltdown", minutes(f.at() / 20));
            tone = RED;
        } else if (f.outcome() == StationRunner.Outcome.SCRAM) {
            verdict = tr("forecast.scram", minutes(f.at() / 20));
            tone = AMBER;
        } else if (!running()) {
            verdict = tr("forecast.cools", Math.round(end));
            tone = LABEL;
        } else if (Math.abs(end - samples[samples.length - 13]) > 2) {
            verdict = tr("forecast.rising", Math.round(end));
            tone = AMBER;
        } else {
            verdict = tr("forecast.settles", Math.round(end));
            tone = GREEN;
        }
        TerminalText.draw(graphics, verdict, 674, 332, tone, 2);
        int gx0 = 674;
        int gx1 = 934;
        int gy0 = 360;
        int gy1 = 474;
        for (int x = gx0; x <= gx1; x += 8) {
            for (int y = gy0; y <= gy1; y += 8) {
                graphics.fill(x, y, x + 1, y + 1, UNLIT);
            }
        }
        graphics.renderOutline(gx0, gy0, gx1 - gx0 + 1, gy1 - gy0 + 1, BOX);
        float limit = overdrive ? StationRunner.MELTDOWN_TEMPERATURE : StationRunner.SCRAM_TEMPERATURE;
        int ly = y(limit, gy1);
        for (int x = gx0; x < gx1; x += 4) {
            graphics.fill(x, ly, x + 2, ly + 1, overdrive ? RED : SCRAM_LINE);
        }
        TerminalText.draw(graphics, tr(overdrive ? "forecast.line.meltdown" : "forecast.line.scram", Math.round(limit)), gx0 + 4, ly - 10,
                overdrive ? RED : SCRAM_LINE, 1);
        int line = f.outcome() == StationRunner.Outcome.MELTDOWN ? RED : f.outcome() == StationRunner.Outcome.SCRAM ? AMBER : CYAN;
        for (int x = gx0 + 1; x < gx1; x++) {
            float at = (x - gx0 - 1) / (float) (gx1 - gx0 - 2) * (samples.length - 1);
            int i = Math.min(samples.length - 2, (int) at);
            float t = samples[i] + (samples[i + 1] - samples[i]) * (at - i);
            int y = y(t, gy1);
            graphics.fill(x, y, x + 1, y + 2, line);
            graphics.fill(x, y + 2, x + 1, gy1, (line & 0x00FFFFFF) | 0x10000000);
        }
        TerminalText.draw(graphics, "0", gx0, gy1 + 6, LABEL, 1);
        TerminalText.centre(graphics, tr("forecast.mid"), (gx0 + gx1) / 2F, gy1 + 6, LABEL, 1);
        TerminalText.right(graphics, tr("forecast.end"), gx1 + 1, gy1 + 6, LABEL, 1);
    }

    /** The forecast graph's height for a temperature: 900°C sits near the top, 1000°C at it. */
    private static int y(float temperature, int bottom) {
        return Math.max(bottom - 112, Math.round(bottom - 4 - Math.min(1040, temperature) / 1031F * 110));
    }

    private void statusBar(GuiGraphics graphics, StationReactor.Analysis plan, StationReactor.Analysis live, boolean overdrive) {
        graphics.fill(0, 512, W, H, BAR);
        graphics.fill(0, 512, W, 513, BOX);
        long now = System.currentTimeMillis();
        if (!queuedLogs.isEmpty() && (log.isEmpty() || now - logAt > 3500)) {
            log = queuedLogs.remove(0);
            logAt = now;
        }
        String text;
        if (!hoverLog.isEmpty()) {
            text = "> " + hoverLog;
        } else {
            int typed = (int) Math.max(0, Math.min(log.length(), (now - logAt) / 18));
            text = "> " + log.substring(0, typed);
        }
        float x = TerminalText.draw(graphics, text, 14, 522, CYAN, 1);
        if (now / 500 % 2 == 0) {
            graphics.fill((int) x + 4, 521, (int) x + 9, 529, CYAN);
        }
        boolean on = running();
        float water = on ? (live.generation() - live.stranded()) / StationRunner.heatPerWater(overdrive) : 0;
        float temperature = menu.temperature();
        String[] segments = {
                tr("bar.plan", pad(plan.score(), 2), core().best),
                tr("bar.water", number(water)),
                tr("bar.steam", number(water)),
                tr("bar.core", pad(Math.round(temperature), 4))};
        int[] colours = {won ? GOLD : TEXT, LABEL, LABEL, level(temperature / StationRunner.SCRAM_TEMPERATURE)};
        float right = 946;
        for (int i = segments.length - 1; i >= 0; i--) {
            TerminalText.right(graphics, segments[i], right, 522, colours[i], 1);
            right -= TerminalText.width(segments[i]) + 14;
            if (i > 0) {
                TerminalText.right(graphics, "|", right + 1, 522, DIM, 1);
                right -= 14;
            }
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float ux = (float) ((mouseX - originX) / unit);
        float uy = (float) ((mouseY - originY) / unit);
        if (warning) {
            for (int[] hit : hits) {
                if (button == 0 && inside(ux, uy, hit)) {
                    warning = false;
                    if (hit[4] == HIT_WARN_GO) {
                        send(StationControlMenu.BUTTON_SWITCH_CORE);
                    } else {
                        sounds.tick();
                        say(tr("log.switch_cancelled"));
                    }
                    break;
                }
            }
            return true;
        }
        int cell = CoreArt.cellAt((ux - ART_X) / 2, (uy - ART_Y) / 2);
        if (cell >= 0 && (button == 0 || button == 1)) {
            Channel part = button == 1 ? Channel.EMPTY : tool;
            if (menu.plan(channel(cell)) != part) {
                clicked = cell;
                clickedPart = part;
                sounds.place(part);
                send(StationControlMenu.BUTTON_PLAN + channel(cell) * 8 + part.ordinal());
            }
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (int[] hit : hits) {
            if (!inside(ux, uy, hit)) {
                continue;
            }
            int id = hit[4];
            if (id >= HIT_TOOL) {
                tool = tools()[id - HIT_TOOL];
                sounds.tick();
            } else if (id == HIT_TAB) {
                // A core holding parts warns first: switching ejects them.
                if (partsIn() > 0) {
                    warning = true;
                    sounds.buzz();
                } else {
                    send(StationControlMenu.BUTTON_SWITCH_CORE);
                    sounds.tick();
                }
            } else if (id == HIT_SAFETY) {
                // Disarming takes a shift-click, so it is never an accident; re-arming SCRAMs the core.
                if (menu.safeties() && !Screen.hasShiftDown()) {
                    say(tr("log.safeties"));
                    sounds.buzz();
                } else {
                    send(StationControlMenu.BUTTON_SAFETY);
                    sounds.tick();
                }
            } else {
                send(switch (id) {
                    case HIT_POWER -> StationControlMenu.BUTTON_POWER;
                    case HIT_REDSTONE -> StationControlMenu.BUTTON_REDSTONE;
                    case HIT_SOUND -> SoundSwitch.BUTTON;
                    case HIT_INJECT -> StationControlMenu.BUTTON_INJECT;
                    default -> StationControlMenu.BUTTON_EJECT;
                });
                if (id != HIT_INJECT && id != HIT_EJECT) {
                    sounds.tick();
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (warning) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                warning = false;
                sounds.tick();
                say(tr("log.switch_cancelled"));
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                warning = false;
                send(StationControlMenu.BUTTON_SWITCH_CORE);
            }
            return true;
        }
        Channel[] tools = tools();
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode < GLFW.GLFW_KEY_1 + tools.length) {
            tool = tools[keyCode - GLFW.GLFW_KEY_1];
            sounds.tick();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
