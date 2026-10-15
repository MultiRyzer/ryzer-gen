package com.ryzer.ryzergen.client.handbook;

import com.ryzer.ryzergen.client.progression.ProgressionKeys;
import com.ryzer.ryzergen.client.progression.ProgressionScreen;
import com.ryzer.ryzergen.client.text.RichText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Operator's Handbook: a ring-bound manual open on a two-page spread, light casing pages either
 * side of a graphite spine with the mod's cyan light line, and a chapter tab for each spread down its
 * right edge. Its first spread teaches the progression map's key, with a button that opens the map;
 * the rest cover the basics the map does not (how to read the mod, power, heat, radiation, where to
 * get help). Every word is in the lang file: {@code handbook.ryzergen.<chapter>.<left|right>.title}
 * and {@code .1}, {@code .2} and on, in the mod's markup. A paragraph that starts "1. " is a step,
 * drawn with its number in a badge.
 */
public class HandbookScreen extends Screen {
    private static final int W = 300;
    private static final int H = 196;
    private static final int HALF = W / 2;
    private static final int TEXT_W = HALF - 22;
    private static final int TAB_W = 60;
    private static final int TAB_H = 18;
    private static final int BUTTON_H = 18;

    private static final int PAGE = 0xFFE1E6EB;
    private static final int PAGE_EDGE = 0xFFB3BBC4;
    private static final int BINDER = 0xFF2A2E35;
    private static final int OUTLINE = 0xFF15171B;
    private static final int SPINE = 0xFF373C44;
    private static final int CYAN = 0xFF35C8F5;
    private static final int DEEP_CYAN = 0xFF0A86B0;
    private static final int INK = 0xFF1E2226;
    private static final int BODY = 0xFF3A4047;
    private static final int DIM = 0xFF9AA3AE;
    private static final Pattern STEP = Pattern.compile("^(\\d+)\\. (.*)$");

    /** The chapters, one spread each, with the colour on their tab. */
    private enum Chapter {
        START(0xFF0A86B0), BASICS(0xFF1E9632), POWER(0xFFC0281C), HEAT(0xFFC05A10), RADIATION(0xFF6A9200), HELP(0xFF50565E);

        final int colour;

        Chapter(int colour) {
            this.colour = colour;
        }

        String key() {
            return "handbook.ryzergen." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A button drawn on a page, and what it does. */
    private record PageButton(Chapter chapter, boolean right, String key, Runnable action) {}

    private final List<PageButton> buttons = new ArrayList<>();
    private Chapter chapter = Chapter.START;
    private int x0;
    private int y0;

    public HandbookScreen() {
        super(Component.translatable("handbook.ryzergen.title"));
        buttons.add(new PageButton(Chapter.START, false, "handbook.ryzergen.button.map", this::openMap));
        buttons.add(new PageButton(Chapter.HELP, false, "handbook.ryzergen.button.discord", () -> Links.open(this, Links.DISCORD)));
        buttons.add(new PageButton(Chapter.HELP, true, "handbook.ryzergen.button.bug", () -> Links.open(this, Links.BUGS)));
    }

    /** Opens the handbook; called when the book is used. */
    public static void open() {
        Minecraft.getInstance().setScreen(new HandbookScreen());
    }

    @Override
    protected void init() {
        x0 = (width - W - TAB_W) / 2;
        y0 = Math.max(4, (height - H) / 2);
    }

    private static String keyName() {
        return ProgressionKeys.OPEN_MAP.getTranslatedKeyMessage().getString();
    }

    private void openMap() {
        if (minecraft != null) {
            minecraft.setScreen(new ProgressionScreen());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderBook(graphics);
        renderTabs(graphics, mouseX, mouseY);
        int sx = x0 + HALF;
        renderPage(graphics, x0 + 10, false);
        renderPage(graphics, sx + 10, true);
        for (PageButton button : buttons) {
            if (button.chapter() == chapter) {
                renderButton(graphics, button, mouseX, mouseY);
            }
        }
        // Page number and arrows on the right page's foot.
        int n = chapter.ordinal() + 1;
        String page = n + " / " + Chapter.values().length;
        graphics.drawString(font, page, sx + HALF - 10 - font.width(page), y0 + 10, DIM, false);
        boolean first = chapter.ordinal() == 0;
        boolean last = chapter.ordinal() == Chapter.values().length - 1;
        graphics.drawString(font, "<", sx + 12, y0 + H - 14, first ? 0xFFC9D0D7 : overPrev(mouseX, mouseY) ? DEEP_CYAN : INK, false);
        graphics.drawString(font, ">", sx + HALF - 16, y0 + H - 14, last ? 0xFFC9D0D7 : overNext(mouseX, mouseY) ? DEEP_CYAN : INK, false);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
    }

    /** The binder, the two pages and the ring-bound spine. */
    private void renderBook(GuiGraphics graphics) {
        graphics.fill(x0 - 4, y0 - 4, x0 + W + 4, y0 + H + 4, OUTLINE);
        graphics.fill(x0 - 3, y0 - 3, x0 + W + 3, y0 + H + 3, BINDER);
        graphics.fill(x0, y0, x0 + W, y0 + H, PAGE);
        graphics.fill(x0, y0 + H - 1, x0 + W, y0 + H, PAGE_EDGE);
        int sx = x0 + HALF;
        graphics.fill(sx - 3, y0, sx + 3, y0 + H, SPINE);
        graphics.fill(sx - 1, y0, sx + 1, y0 + H, CYAN);
        for (int ry = y0 + 8; ry < y0 + H - 4; ry += 24) {
            graphics.fill(sx - 4, ry, sx + 4, ry + 3, 0xFF646C77);
            graphics.renderOutline(sx - 4, ry, 8, 3, OUTLINE);
        }
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Chapter c : Chapter.values()) {
            int ty = tabY(c);
            boolean on = c == chapter;
            boolean hover = overTab(c, mouseX, mouseY);
            int tw = on || hover ? TAB_W : TAB_W - 4;
            int tx = x0 + W + 3;
            graphics.fill(tx, ty, tx + tw, ty + TAB_H, on ? PAGE : 0xFFC9D0D7);
            graphics.fill(tx + tw - 3, ty, tx + tw, ty + TAB_H, c.colour);
            graphics.renderOutline(tx - 1, ty, tw + 1, TAB_H, OUTLINE);
            String label = font.plainSubstrByWidth(Component.translatable(c.key() + ".tab").getString(), tw - 9);
            graphics.drawString(font, label, tx + 4, ty + 5, on ? INK : 0xFF50565E, false);
        }
    }

    private int tabY(Chapter c) {
        return y0 + 6 + c.ordinal() * (TAB_H + 4);
    }

    /** One page: the title, then its paragraphs; the first spread's left page also shows the map's key. */
    private void renderPage(GuiGraphics graphics, int x, boolean right) {
        String base = chapter.key() + (right ? ".right" : ".left");
        int y = y0 + 10;
        if (!right) {
            graphics.drawString(font, Component.translatable("handbook.ryzergen.header"), x, y, DEEP_CYAN, false);
            graphics.fill(x, y + 10, x + TEXT_W, y + 11, CYAN);
            y += 18;
        }
        graphics.drawString(font, Component.translatable(base + ".title").withStyle(style -> style.withBold(true)), x, y, INK, false);
        y += 14;
        if (chapter == Chapter.START && !right) {
            y = renderKey(graphics, x, y);
        }
        Language language = Language.getInstance();
        for (int n = 1; language.has(base + "." + n); n++) {
            String raw = language.getOrDefault(base + "." + n);
            Matcher step = STEP.matcher(raw);
            if (step.matches()) {
                graphics.fill(x, y - 1, x + 9, y + 8, 0xFF2B3036);
                graphics.drawString(font, step.group(1), x + 2, y, CYAN, false);
                y = paragraph(graphics, RichText.parse(String.format(step.group(2), keyName()), true), x + 13, y, TEXT_W - 13) + 3;
            } else {
                y = paragraph(graphics, RichText.trLight(base + "." + n, keyName()), x, y, TEXT_W) + 4;
            }
        }
    }

    /** The map's key as a big keycap, with what it opens beside it. */
    private int renderKey(GuiGraphics graphics, int x, int y) {
        String key = keyName();
        boolean big = font.width(key) <= 9;
        int capW = Math.max(22, (big ? font.width(key) * 2 : font.width(key)) + 8);
        graphics.fill(x, y + 2, x + capW, y + 24, 0xFF1D2126);
        graphics.fill(x, y, x + capW, y + 22, 0xFF4D545D);
        graphics.fill(x + 2, y + 2, x + capW - 2, y + 18, 0xFF5A626C);
        graphics.renderOutline(x, y, capW, 22, OUTLINE);
        graphics.pose().pushPose();
        if (big) {
            graphics.pose().translate(x + (capW - font.width(key) * 2) / 2F + 1, y + 3, 0);
            graphics.pose().scale(2, 2, 1);
            graphics.drawString(font, key, 0, 0, 0xFFFFFFFF, false);
        } else {
            graphics.drawString(font, key, x + 4, y + 7, 0xFFFFFFFF, false);
        }
        graphics.pose().popPose();
        int tx = x + capW + 6;
        graphics.drawString(font, Component.translatable("handbook.ryzergen.start.key").withStyle(style -> style.withBold(true)), tx, y + 3, DEEP_CYAN, false);
        graphics.drawString(font, Component.translatable("handbook.ryzergen.start.key.sub"), tx, y + 13, 0xFF50565E, false);
        return y + 32;
    }

    private int paragraph(GuiGraphics graphics, Component text, int x, int y, int w) {
        for (FormattedCharSequence line : font.split(text, w)) {
            graphics.drawString(font, line, x, y, BODY, false);
            y += 10;
        }
        return y;
    }

    private int buttonX(PageButton button) {
        return x0 + (button.right() ? HALF : 0) + 10;
    }

    private int buttonY() {
        return y0 + H - 38;
    }

    private void renderButton(GuiGraphics graphics, PageButton button, int mouseX, int mouseY) {
        int x = buttonX(button);
        int y = buttonY();
        boolean hover = over(mouseX, mouseY, x, y, TEXT_W, BUTTON_H);
        graphics.fill(x, y, x + TEXT_W, y + BUTTON_H, OUTLINE);
        graphics.fill(x + 1, y + 1, x + TEXT_W - 1, y + BUTTON_H - 1, hover ? 0xFF3A4047 : 0xFF2B3036);
        graphics.fill(x + 1, y + 1, x + TEXT_W - 1, y + 2, CYAN);
        Component label = Component.translatable(button.key(), keyName());
        graphics.drawString(font, label, x + (TEXT_W - font.width(label)) / 2, y + 6, hover ? 0xFFFFFFFF : 0xFFE1E6EB, false);
    }

    private static boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private boolean overTab(Chapter c, double mouseX, double mouseY) {
        return over(mouseX, mouseY, x0 + W + 3, tabY(c), TAB_W, TAB_H);
    }

    private boolean overPrev(double mouseX, double mouseY) {
        return over(mouseX, mouseY, x0 + HALF + 8, y0 + H - 18, 14, 14);
    }

    private boolean overNext(double mouseX, double mouseY) {
        return over(mouseX, mouseY, x0 + W - 20, y0 + H - 18, 14, 14);
    }

    private void turnTo(Chapter to) {
        if (to != chapter) {
            chapter = to;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            }
        }
    }

    private void turn(int by) {
        int to = chapter.ordinal() + by;
        if (to >= 0 && to < Chapter.values().length) {
            turnTo(Chapter.values()[to]);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Chapter c : Chapter.values()) {
                if (overTab(c, mouseX, mouseY)) {
                    turnTo(c);
                    return true;
                }
            }
            if (overPrev(mouseX, mouseY)) {
                turn(-1);
                return true;
            }
            if (overNext(mouseX, mouseY)) {
                turn(1);
                return true;
            }
            for (PageButton b : buttons) {
                if (b.chapter() == chapter && over(mouseX, mouseY, buttonX(b), buttonY(), TEXT_W, BUTTON_H)) {
                    if (minecraft != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    b.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        turn(scrollY > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        // The map's own key opens the map from here: the handbook's whole point.
        if (ProgressionKeys.OPEN_MAP.matches(keyCode, scanCode)) {
            openMap();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
