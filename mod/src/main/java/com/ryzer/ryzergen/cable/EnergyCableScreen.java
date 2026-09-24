package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Cable panel: one row per extract side, with the block it pulls from, a bar of the current rate
 * against the cable's limit, and a footer with the limit and how many machines it feeds. The panel
 * grows with the rows: the texture (art/tools/gui_textures.py, cable) is drawn for six, and the
 * screen shows its top part and then its bottom edge.
 */
public class EnergyCableScreen extends AbstractContainerScreen<EnergyCableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/cable.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int NAME = 0xFFB3BBC4;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF4D545D;
    private static final int WELL = 0xFF1D2126;
    private static final int WELL_LIGHT = 0xFF4D545D;
    private static final int TEXTURE_H = 194;
    private static final int BOTTOM_H = 8;
    private static final int ROW_X = 10;
    private static final int ROW_Y = 20;
    private static final int ROW_W = 156;
    private static final int ROW_H = 22;
    private static final int ROW_STEP = 24;
    private static final int BAR_X = 22;
    private static final int BAR_Y = 13;
    private static final int BAR_H = 5;
    /** Rates are averaged over a second, so round-robin delivery does not make the numbers flicker. */
    private static final int SAMPLES = 20;

    private final int[][] samples = new int[6][SAMPLES];
    private int sample;

    public EnergyCableScreen(EnergyCableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = TEXTURE_H;
    }

    private List<Direction> extractSides() {
        List<Direction> sides = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (menu.mode(dir) == CableSide.EXTRACT) {
                sides.add(dir);
            }
        }
        return sides;
    }

    /** Height of the panel for this many rows, plus the footer. */
    private static int panelHeight(int rows) {
        return ROW_Y + (Math.max(rows, 1) + 1) * ROW_STEP + 6;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (Direction dir : Direction.values()) {
            samples[dir.get3DDataValue()][sample] = menu.moved(dir);
        }
        sample = (sample + 1) % SAMPLES;
        int height = panelHeight(extractSides().size());
        if (height != imageHeight) {
            imageHeight = height;
            topPos = (this.height - imageHeight) / 2;
        }
    }

    @Override
    protected void init() {
        imageHeight = panelHeight(extractSides().size());
        super.init();
        for (Direction dir : Direction.values()) {
            java.util.Arrays.fill(samples[dir.get3DDataValue()], menu.moved(dir));
        }
    }

    private int average(Direction dir) {
        long total = 0;
        for (int value : samples[dir.get3DDataValue()]) {
            total += value;
        }
        return (int) Math.round(total / (double) SAMPLES);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int top = imageHeight - BOTTOM_H;
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, top);
        graphics.blit(TEXTURE, leftPos, topPos + top, 0, TEXTURE_H - BOTTOM_H, imageWidth, BOTTOM_H);

        List<Direction> sides = extractSides();
        int max = menu.maxRate();
        for (int row = 0; row <= sides.size(); row++) {
            well(graphics, leftPos + ROW_X, topPos + rowY(row), ROW_W, ROW_H);
        }
        for (int row = 0; row < sides.size(); row++) {
            int x = leftPos + ROW_X;
            int y = topPos + rowY(row);
            ItemStack icon = sourceIcon(sides.get(row));
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, x + 3, y + 3);
            }
            int barX = x + BAR_X;
            int barW = ROW_W - BAR_X - 4;
            graphics.fill(barX, y + BAR_Y, barX + barW, y + BAR_Y + BAR_H, 0xFF2B3036);
            float fraction = max == 0 ? 0 : average(sides.get(row)) / (float) max;
            GuiGauges.glowHorizontal(graphics, barX, y + BAR_Y, barW, BAR_H, fraction, GuiGauges.ENERGY);
            for (int tick = 1; tick < 4; tick++) {
                graphics.fill(barX + barW * tick / 4, y + BAR_Y, barX + barW * tick / 4 + 1, y + BAR_Y + BAR_H, 0x50000000);
            }
        }
    }

    private static int rowY(int row) {
        return ROW_Y + row * ROW_STEP;
    }

    /** A sunken readout, the same as the texture's screens: dark glass, a lit lower and right edge. */
    private static void well(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, WELL);
        graphics.fill(x, y + h - 1, x + w, y + h, WELL_LIGHT);
        graphics.fill(x + w - 1, y, x + w, y + h, WELL_LIGHT);
    }

    private BlockState source(Direction dir) {
        Level level = minecraft == null ? null : minecraft.level;
        return level == null ? null : level.getBlockState(menu.pos().relative(dir));
    }

    private ItemStack sourceIcon(Direction dir) {
        BlockState state = source(dir);
        return state == null ? ItemStack.EMPTY : new ItemStack(state.getBlock());
    }

    private Component sourceName(Direction dir) {
        BlockState state = source(dir);
        return state == null ? Component.empty() : state.getBlock().getName();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        List<Direction> sides = extractSides();
        int max = menu.maxRate();
        for (int row = 0; row < sides.size(); row++) {
            Direction dir = sides.get(row);
            int y = rowY(row);
            int current = average(dir);
            String value = Component.translatable("gui.ryzergen.cable.rate", String.format("%,d", current), String.format("%,d", max)).getString();
            int valueX = ROW_X + ROW_W - 4 - font.width(value);
            graphics.drawString(font, value, valueX, y + 3, current > 0 ? VALUE : DIM, false);
            String name = font.plainSubstrByWidth(sourceName(dir).getString(), valueX - ROW_X - BAR_X - 4);
            graphics.drawString(font, name, ROW_X + BAR_X, y + 3, NAME, false);
        }
        int footer = rowY(sides.size());
        graphics.drawString(font, Component.translatable("gui.ryzergen.cable.max", String.format("%,d", max)), ROW_X + 4, footer + 3, VALUE, false);
        int receivers = menu.receivers();
        graphics.drawString(font, Component.translatable(receivers == 1 ? "gui.ryzergen.cable.receivers.one" : "gui.ryzergen.cable.receivers", receivers),
                ROW_X + 4, footer + 12, receivers > 0 ? NAME : 0xFFF0A030, false);
        int buffers = menu.buffers();
        if (buffers > 0) {
            Component stored = Component.translatable(buffers == 1 ? "gui.ryzergen.cable.buffers.one" : "gui.ryzergen.cable.buffers", buffers);
            graphics.drawString(font, stored, ROW_X + ROW_W - 4 - font.width(stored), footer + 12, 0xFF44D65E, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        List<Direction> sides = extractSides();
        for (int row = 0; row < sides.size(); row++) {
            int y = rowY(row);
            if (mx >= ROW_X && mx < ROW_X + ROW_W && my >= y && my < y + ROW_H) {
                Direction dir = sides.get(row);
                graphics.renderComponentTooltip(font, List.of(
                        sourceName(dir),
                        Component.translatable("gui.ryzergen.cable.side", Component.translatable("gui.ryzergen.direction." + dir.getName()))
                                .withStyle(style -> style.withColor(0xFF9AA3AE)),
                        Component.translatable("gui.ryzergen.cable.average").withStyle(style -> style.withColor(0xFF9AA3AE))),
                        mouseX, mouseY);
            }
        }
    }
}
