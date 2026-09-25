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

import static com.ryzer.ryzergen.cable.CableMenu.*;

/**
 * Cable and pipe panel: one row per side joined to a machine, with the block there, a bar of what
 * came in on that side against its limit, and the side's fitting slot; a footer with the base limit
 * and what the network reaches; then the player's inventory. The panel grows with the rows: the
 * texture (art/tools/gui_textures.py, cable) is drawn for six rows, and the screen shows its top
 * part, then its inventory section.
 */
public class CableScreen extends AbstractContainerScreen<CableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/cable.png");
    private static final int TEXTURE_W = 256;
    private static final int TEXTURE_H = 320;
    /** Where the texture's inventory section starts (after six rows), and its height. */
    private static final int INVENTORY_V = CableMenu.inventoryTop(6);
    private static final int INVENTORY_H = 100;
    private static final int LABEL = 0xFF2B3036;
    private static final int NAME = 0xFFB3BBC4;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF4D545D;
    private static final int HINT = 0xFF9AA3AE;
    private static final int WELL = 0xFF1D2126;
    private static final int WELL_LIGHT = 0xFF4D545D;
    private static final int ROW_H = 22;
    /** Rows stop short of the fitting slot at their right end. */
    private static final int ROW_WELL_W = ROW_W - 22;
    private static final int BAR_X = 22;
    private static final int BAR_Y = 13;
    private static final int BAR_H = 5;
    /**
     * A second of samples, so round-robin delivery does not make the numbers flicker. Energy shows
     * their average per tick; items move in bursts, so they show the total per second.
     */
    private static final int SAMPLES = 20;

    private final int[][] samples = new int[6][SAMPLES];
    private int sample;

    public CableScreen(CableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = CableMenu.inventoryTop(menu.sides().size()) + INVENTORY_H;
        inventoryLabelY = CableMenu.inventoryTop(menu.sides().size()) + 7;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (Direction dir : Direction.values()) {
            samples[dir.get3DDataValue()][sample] = menu.moved(dir);
        }
        sample = (sample + 1) % SAMPLES;
    }

    @Override
    protected void init() {
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
        return menu.kind().perSecond() ? (int) total : (int) Math.round(total / (double) SAMPLES);
    }

    private String key(String name) {
        return "gui.ryzergen." + menu.kind().langKey() + "." + name;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int top = CableMenu.inventoryTop(menu.sides().size());
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, top, TEXTURE_W, TEXTURE_H);
        graphics.blit(TEXTURE, leftPos, topPos + top, 0, INVENTORY_V, imageWidth, INVENTORY_H, TEXTURE_W, TEXTURE_H);

        List<Direction> sides = menu.sides();
        for (int row = 0; row < sides.size(); row++) {
            Direction dir = sides.get(row);
            int x = leftPos + ROW_X;
            int y = topPos + rowY(row);
            boolean feeds = menu.feeds(dir);
            well(graphics, x, y, feeds ? ROW_WELL_W : ROW_W, ROW_H);
            ItemStack icon = sourceIcon(dir);
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, x + 3, y + 3);
            }
            if (!feeds) {
                continue;
            }
            slot(graphics, leftPos + FITTING_X - 1, y + FITTING_DY - 1);
            int barX = x + BAR_X;
            int barW = ROW_WELL_W - BAR_X - 4;
            graphics.fill(barX, y + BAR_Y, barX + barW, y + BAR_Y + BAR_H, 0xFF2B3036);
            int max = menu.maxRate(dir);
            float fraction = max == 0 ? 0 : Math.min(1, average(dir) / (float) max);
            GuiGauges.glowHorizontal(graphics, barX, y + BAR_Y, barW, BAR_H, fraction, menu.kind().colour());
            for (int tick = 1; tick < 4; tick++) {
                graphics.fill(barX + barW * tick / 4, y + BAR_Y, barX + barW * tick / 4 + 1, y + BAR_Y + BAR_H, 0x50000000);
            }
        }
        well(graphics, leftPos + ROW_X, topPos + rowY(Math.max(sides.size(), 1)), ROW_W, ROW_H);
    }

    /** A sunken readout, the same as the texture's screens: dark glass, a lit lower and right edge. */
    private static void well(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, WELL);
        graphics.fill(x, y + h - 1, x + w, y + h, WELL_LIGHT);
        graphics.fill(x + w - 1, y, x + w, y + h, WELL_LIGHT);
    }

    /** An 18 x 18 item slot like the texture's: a mid grey well, shaded top and left, lit bottom and right. */
    private static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF868F9A);
        graphics.fill(x, y, x + 18, y + 1, 0xFF4D545D);
        graphics.fill(x, y, x + 1, y + 18, 0xFF4D545D);
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFE1E6EB);
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFE1E6EB);
        graphics.fill(x + 1, y + 1, x + 17, y + 2, 0xFF9EA7B1);
        graphics.fill(x + 1, y + 1, x + 2, y + 17, 0xFF9EA7B1);
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
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        List<Direction> sides = menu.sides();
        for (int row = 0; row < sides.size(); row++) {
            Direction dir = sides.get(row);
            int y = rowY(row);
            if (!menu.feeds(dir)) {
                // A machine that only takes from the network: nothing comes in here, so no fitting.
                String note = Component.translatable("gui.ryzergen.cable.receives_only").getString();
                int noteX = ROW_X + ROW_W - 4 - font.width(note);
                graphics.drawString(font, note, noteX, y + 7, DIM, false);
                graphics.drawString(font, font.plainSubstrByWidth(sourceName(dir).getString(), noteX - ROW_X - BAR_X - 4),
                        ROW_X + BAR_X, y + 7, NAME, false);
                continue;
            }
            int current = average(dir);
            // What came in: the limit is in the tooltip and the bar shows how close it is.
            String value = Component.translatable(key("rate"), String.format("%,d", current)).getString();
            int valueX = ROW_X + ROW_WELL_W - 4 - font.width(value);
            graphics.drawString(font, value, valueX, y + 3, current > 0 ? VALUE : DIM, false);
            String name = font.plainSubstrByWidth(sourceName(dir).getString(), valueX - ROW_X - BAR_X - 4);
            graphics.drawString(font, name, ROW_X + BAR_X, y + 3, NAME, false);
        }
        int footer = rowY(Math.max(sides.size(), 1));
        graphics.drawString(font, Component.translatable(key("max"), String.format("%,d", menu.baseRate())),
                ROW_X + 4, footer + 3, VALUE, false);
        // What the network reaches, amber when it reaches nothing. Batteries only exist on energy networks.
        int receivers = menu.receivers();
        int buffers = menu.buffers();
        Component network = buffers > 0
                ? Component.translatable(key("network_buffers"), receivers, buffers)
                : Component.translatable(key("network"), receivers);
        graphics.drawString(font, network, ROW_X + 4, footer + 12, receivers + buffers > 0 ? NAME : 0xFFF0A030, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        List<Direction> sides = menu.sides();
        for (int row = 0; row < sides.size(); row++) {
            int y = rowY(row);
            Direction dir = sides.get(row);
            String sideName = Component.translatable("gui.ryzergen.direction." + dir.getName()).getString();
            if (!menu.feeds(dir)) {
                if (mx >= ROW_X && mx < ROW_X + ROW_W && my >= y && my < y + ROW_H) {
                    graphics.renderComponentTooltip(font, List.of(sourceName(dir),
                            Component.translatable("gui.ryzergen.cable.receives_only_hint").withStyle(style -> style.withColor(HINT))),
                            mouseX, mouseY);
                }
                continue;
            }
            if (mx >= ROW_X && mx < ROW_X + ROW_WELL_W && my >= y && my < y + ROW_H) {
                List<Component> lines = new ArrayList<>();
                lines.add(sourceName(dir));
                lines.add(Component.translatable(menu.mode(dir) == CableSide.EXTRACT ? "gui.ryzergen.cable.side"
                        : "gui.ryzergen.cable.side_joined", sideName).withStyle(style -> style.withColor(HINT)));
                lines.add(Component.translatable("gui.ryzergen.cable.limit",
                        Component.translatable(key("rate"), String.format("%,d", menu.maxRate(dir))))
                        .withStyle(style -> style.withColor(VALUE)));
                lines.add(Component.translatable(key("average")).withStyle(style -> style.withColor(HINT)));
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            } else if (mx >= FITTING_X - 1 && mx < FITTING_X + 17 && my >= y + FITTING_DY - 1 && my < y + FITTING_DY + 17
                    && menu.upgrade(dir) == CableUpgrade.NONE && menu.getCarried().isEmpty()) {
                graphics.renderComponentTooltip(font, List.of(
                        Component.translatable("gui.ryzergen.cable.fitting_slot", sideName),
                        Component.translatable("gui.ryzergen.cable.fitting_hint").withStyle(style -> style.withColor(HINT))),
                        mouseX, mouseY);
            }
        }
    }
}
