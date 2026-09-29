package com.ryzer.ryzergen.machine.pool;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The controller's screen before the pool forms: the parts store on the left, and a readout of
 * what is placed, what is in stock and what is in the way. It shares the fission station's build
 * panel (art/tools/gui_textures.py, station_core) and its words.
 */
public class PoolBuildScreen extends AbstractContainerScreen<PoolBuildMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/station_core.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF9AA3AE;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int BAD = 0xFFE0503C;
    private static final int SCREEN_X = 66;
    private static final int ROW_Y = 31;
    private static final int ROW_STEP = 11;
    private static final int BAR_Y = 64;
    private static final PoolPart[] BUILT = {PoolPart.LINER, PoolPart.CRANE};

    public PoolBuildScreen(PoolBuildMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleLabelY = 4;
        inventoryLabelY = 73;
    }

    private static int total(PoolPart part) {
        return part == PoolPart.CRANE ? 1 : PoolLayout.CELLS - 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int placed = 0;
        int all = 0;
        for (int i = 0; i < BUILT.length; i++) {
            PoolPart part = BUILT[i];
            placed += total(part) - menu.missing(part);
            all += total(part);
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos + SCREEN_X + 4, topPos + ROW_Y + i * ROW_STEP - 1, 0);
            graphics.pose().scale(0.625F, 0.625F, 1);
            graphics.renderItem(new ItemStack(part.block()), 0, 0);
            graphics.pose().popPose();
        }
        int barX = leftPos + SCREEN_X + 4;
        int barW = 94;
        graphics.fill(barX, topPos + BAR_Y, barX + barW, topPos + BAR_Y + 4, 0xFF2B3036);
        GuiGauges.glowHorizontal(graphics, barX, topPos + BAR_Y, barW, 4, all == 0 ? 0 : placed / (float) all, VALUE);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        Component status;
        int colour;
        boolean shortOfParts = false;
        for (PoolPart part : BUILT) {
            shortOfParts |= menu.stock(part) < menu.missing(part);
        }
        if (menu.formed()) {
            status = Component.translatable("gui.ryzergen.station_core.formed");
            colour = GOOD;
        } else if (menu.blocked() > 0) {
            status = Component.translatable("gui.ryzergen.station_core.blocked", menu.blocked());
            colour = BAD;
        } else if (shortOfParts) {
            status = Component.translatable("gui.ryzergen.station_core.needs_parts");
            colour = WARN;
        } else {
            status = Component.translatable("gui.ryzergen.station_core.building");
            colour = GOOD;
        }
        graphics.drawString(font, status, SCREEN_X + 4, 20, colour, false);
        for (int i = 0; i < BUILT.length; i++) {
            PoolPart part = BUILT[i];
            int placed = total(part) - menu.missing(part);
            graphics.drawString(font, Component.translatable("gui.ryzergen.station_core.count", placed, total(part)),
                    SCREEN_X + 16, ROW_Y + i * ROW_STEP, placed >= total(part) ? GOOD : VALUE, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        for (int i = 0; i < BUILT.length; i++) {
            int y = ROW_Y + i * ROW_STEP - 1;
            if (mx >= SCREEN_X + 2 && mx < SCREEN_X + 100 && my >= y && my < y + ROW_STEP) {
                PoolPart part = BUILT[i];
                graphics.renderComponentTooltip(font, List.of(part.block().getName(),
                        Component.translatable("gui.ryzergen.station_core.stock", menu.stock(part), menu.missing(part))
                                .withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
            }
        }
    }
}
