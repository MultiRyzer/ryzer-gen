package com.ryzer.ryzergen.battery.container;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

import static com.ryzer.ryzergen.battery.container.BatteryControllerBlockEntity.COOLANT_CAPACITY;

/**
 * Container Battery readout: the charge and coolant gauges, stored charge, capacity, in and out,
 * the cooling status, and a map of the front's 20 slots laid out as you see them from the front,
 * lit where a rack is installed. Positions match art/tools/gui_textures.py (container_battery).
 */
public class ContainerScreen extends AbstractContainerScreen<ContainerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/container_battery.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF2A7CD6;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int HINT = 0xFF9AA3AE;
    private static final int CHARGE_X = 11;
    private static final int COOLANT_X = 29;
    private static final int GAUGE_Y = 22;
    private static final int GAUGE_H = 78;
    private static final int READOUT_X = 46;
    private static final int MAP_X = 126;
    private static final int MAP_Y = 23;
    private static final int CELL_W = 5;
    private static final int CELL_H = 9;

    public ContainerScreen(ContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 112;
    }

    /** Where a cell of the front shows on the map: as seen from the front, so west is on the right. */
    private static int[] mapCell(BlockPos cell) {
        int column = ContainerLayout.LONG - 2 - cell.getX();
        int row = ContainerLayout.HIGH - 1 - cell.getY();
        return new int[] {MAP_X + column * (CELL_W + 1), MAP_Y + row * (CELL_H + 1)};
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        float fraction = menu.capacity() == 0 ? 0 : menu.energy() / (float) menu.capacity();
        GuiGauges.glow(graphics, leftPos + CHARGE_X, topPos + GAUGE_Y, 12, GAUGE_H, fraction, GuiGauges.ENERGY);
        GuiGauges.glass(graphics, leftPos + CHARGE_X, topPos + GAUGE_Y, 12, GAUGE_H);
        GuiGauges.fluid(graphics, leftPos + COOLANT_X, topPos + GAUGE_Y, 8, GAUGE_H, menu.coolant() / (float) COOLANT_CAPACITY, Fluids.WATER);
        for (int slot = 0; slot < BatteryControllerBlockEntity.SLOTS; slot++) {
            int[] at = mapCell(ContainerLayout.SLOTS.get(slot));
            int x = leftPos + at[0];
            int y = topPos + at[1];
            if (menu.installed(slot)) {
                graphics.fillGradient(x, y, x + CELL_W, y + CELL_H, 0xFF4D545D, 0xFF2B3036);
                graphics.fill(x + 1, y + 2, x + CELL_W - 1, y + 3, VALUE);
                graphics.fill(x + 1, y + 5, x + CELL_W - 1, y + 6, fraction > 0 ? VALUE : 0xFF1B5A73);
            } else {
                graphics.fill(x, y, x + CELL_W, y + CELL_H, 0xFF9EA7B1);
                graphics.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, 0xFFC9D0D7);
            }
        }
        // The console's cell.
        int[] console = mapCell(ContainerLayout.CONTROLLER);
        graphics.fill(leftPos + console[0], topPos + console[1], leftPos + console[0] + CELL_W, topPos + console[1] + CELL_H, 0xFF1D2126);
        graphics.fill(leftPos + console[0] + 1, topPos + console[1] + 3, leftPos + console[0] + CELL_W - 1, topPos + console[1] + 4, GOOD);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.battery.stored", compact(menu.energy())), READOUT_X, 26, VALUE, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.battery.capacity", compact(menu.capacity())), READOUT_X, 37, DIM, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.battery.in", compact(menu.in())), READOUT_X, 52, GOOD, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.battery.out", compact(menu.out())), READOUT_X, 63, WARN, false);
        boolean cooled = menu.coolant() > 0;
        graphics.drawString(font, Component.translatable(cooled ? "gui.ryzergen.container.cooled" : "gui.ryzergen.container.no_coolant"),
                READOUT_X, 78, cooled ? GOOD : WARN, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.container.racks", menu.racks(), BatteryControllerBlockEntity.SLOTS),
                MAP_X, 59, LABEL, false);
    }

    /** 950, 12.5k, 1.20M. */
    private static String compact(int value) {
        if (value < 10_000) {
            return String.format("%,d", value);
        }
        if (value < 1_000_000) {
            return String.format("%.1fk", value / 1_000.0);
        }
        return String.format("%.2fM", value / 1_000_000.0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= CHARGE_X - 1 && mx < CHARGE_X + 13 && my >= GAUGE_Y - 1 && my < GAUGE_Y + GAUGE_H + 1) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.energy",
                    String.format("%,d", menu.energy()), String.format("%,d", menu.capacity())), mouseX, mouseY);
        } else if (mx >= COOLANT_X - 1 && mx < COOLANT_X + 9 && my >= GAUGE_Y - 1 && my < GAUGE_Y + GAUGE_H + 1) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.container.coolant", String.format("%,d", menu.coolant()), String.format("%,d", COOLANT_CAPACITY)),
                    Component.translatable("gui.ryzergen.container.coolant_hint").withStyle(style -> style.withColor(HINT))), mouseX, mouseY);
        } else if (mx >= MAP_X - 2 && mx < MAP_X + 43 && my >= MAP_Y - 2 && my < MAP_Y + 31) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.container.racks_hint"),
                    Component.translatable("gui.ryzergen.container.remove_hint").withStyle(style -> style.withColor(HINT))), mouseX, mouseY);
        }
    }
}
