package com.ryzer.ryzergen.machine.pool;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

import static com.ryzer.ryzergen.machine.pool.PoolControllerBlockEntity.*;

/**
 * The formed pool's screen: each rack slot fills from below in cyan as its item cools (green when
 * it is ready to lift out), the water gauge beside them, the output slots on the right, and a
 * status beside the title. Positions match art/tools/gui_textures.py (spent_fuel_pool).
 */
public class PoolScreen extends AbstractContainerScreen<PoolMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/spent_fuel_pool.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int DIM = 0xFF9AA3AE;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int COOLING_FILL = 0x7035C8F5;
    private static final int READY_FILL = 0x7044D65E;
    private static final int WELL_X = 117;
    private static final int WELL_Y = 17;
    private static final int WELL_W = 12;
    private static final int WELL_H = 54;
    /** Where the status starts: the right-hand end of the title row. */
    private static final int STATUS_RIGHT = 169;

    public PoolScreen(PoolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleLabelY = 4;
        inventoryLabelY = 73;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int rack = 0; rack < RACKS; rack++) {
            if (!menu.getSlot(rack).hasItem()) {
                continue;
            }
            float progress = menu.progress(rack);
            int x = leftPos + 8 + (rack % 6) * 18;
            int y = topPos + 18 + (rack / 6) * 18;
            int fill = Math.max(1, Math.round(16 * progress));
            graphics.fill(x, y + 16 - fill, x + 16, y + 16, progress >= 1 ? READY_FILL : COOLING_FILL);
        }
        GuiGauges.fluid(graphics, leftPos + WELL_X + 1, topPos + WELL_Y + 1, WELL_W - 2, WELL_H - 2,
                menu.water() / (float) CAPACITY, Fluids.WATER);
        // The line the water must stay above to cover the racks.
        int line = topPos + WELL_Y + 1 + (WELL_H - 2) / 2;
        graphics.fill(leftPos + WELL_X + 1, line, leftPos + WELL_X + WELL_W - 1, line + 1, 0xA0FFFFFF);
    }

    private boolean anyFuel() {
        for (int rack = 0; rack < RACKS; rack++) {
            if (menu.getSlot(rack).hasItem()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        Component status;
        int colour;
        if (menu.water() < COVERED) {
            status = Component.translatable("gui.ryzergen.pool.low_water");
            colour = WARN;
        } else if (anyFuel()) {
            status = Component.translatable("gui.ryzergen.pool.cooling");
            colour = GOOD;
        } else {
            status = Component.translatable("gui.ryzergen.pool.idle");
            colour = DIM;
        }
        graphics.drawString(font, status, STATUS_RIGHT - font.width(status), titleLabelY, colour, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= WELL_X && mx < WELL_X + WELL_W && my >= WELL_Y && my < WELL_Y + WELL_H) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.pool.water", String.format("%,d", menu.water()), String.format("%,d", CAPACITY)),
                    Component.translatable("gui.ryzergen.pool.water_hint").withStyle(style -> style.withColor(DIM))),
                    mouseX, mouseY);
        }
    }

    /** A rack's tooltip adds how long its item still has to cool. */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = super.getTooltipFromContainerItem(stack);
        if (hoveredSlot != null && hoveredSlot.index < RACKS) {
            float progress = menu.progress(hoveredSlot.index);
            int seconds = Math.round((1 - progress) * HotFuel.coolingTicks(stack) / 20F);
            lines = new java.util.ArrayList<>(lines);
            lines.add(progress >= 1
                    ? Component.translatable("gui.ryzergen.pool.ready").withStyle(style -> style.withColor(GOOD))
                    : Component.translatable("gui.ryzergen.pool.time_left", seconds / 60, String.format("%02d", seconds % 60))
                    .withStyle(style -> style.withColor(DIM)));
        }
        return lines;
    }
}
