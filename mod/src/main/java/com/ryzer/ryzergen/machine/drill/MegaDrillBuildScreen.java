package com.ryzer.ryzergen.machine.drill;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import com.ryzer.ryzergen.registry.ModBlocks;
import com.ryzer.ryzergen.client.text.RichText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The controller's screen before the drill forms: the parts store on the left, and a readout of
 * how much frame is placed, in stock and in the way. It shares the fission station's build panel
 * (art/tools/gui_textures.py, station_core) and its words.
 */
public class MegaDrillBuildScreen extends AbstractContainerScreen<MegaDrillBuildMenu> {
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
    /** Frame to place: every cell but the controller's own. */
    private static final int TOTAL = MegaDrillLayout.CELLS - 1;

    public MegaDrillBuildScreen(MegaDrillBuildMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleLabelY = 4;
        inventoryLabelY = 73;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + SCREEN_X + 4, topPos + ROW_Y - 1, 0);
        graphics.pose().scale(0.625F, 0.625F, 1);
        graphics.renderItem(new ItemStack(ModBlocks.DRILL_FRAME.get()), 0, 0);
        graphics.pose().popPose();
        int barX = leftPos + SCREEN_X + 4;
        int barW = 94;
        graphics.fill(barX, topPos + BAR_Y, barX + barW, topPos + BAR_Y + 4, 0xFF2B3036);
        GuiGauges.glowHorizontal(graphics, barX, topPos + BAR_Y, barW, 4, (TOTAL - menu.missing()) / (float) TOTAL, VALUE);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
        Component status;
        int colour;
        if (menu.formed()) {
            status = RichText.tr("gui.ryzergen.station_core.formed");
            colour = GOOD;
        } else if (menu.blocked() > 0) {
            status = RichText.tr("gui.ryzergen.station_core.blocked", menu.blocked());
            colour = BAD;
        } else if (menu.stock() < menu.missing()) {
            status = RichText.tr("gui.ryzergen.station_core.needs_parts");
            colour = WARN;
        } else {
            status = RichText.tr("gui.ryzergen.station_core.building");
            colour = GOOD;
        }
        graphics.drawString(font, status, SCREEN_X + 4, 20, colour, false);
        int placed = TOTAL - menu.missing();
        graphics.drawString(font, RichText.tr("gui.ryzergen.station_core.count", placed, TOTAL),
                SCREEN_X + 16, ROW_Y, placed >= TOTAL ? GOOD : VALUE, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= SCREEN_X + 2 && mx < SCREEN_X + 100 && my >= ROW_Y - 1 && my < ROW_Y - 1 + ROW_STEP) {
            graphics.renderComponentTooltip(font, List.of(ModBlocks.DRILL_FRAME.get().getName(),
                    RichText.tr("gui.ryzergen.station_core.stock", menu.stock(), menu.missing())
                            .withStyle(style -> style.withColor(DIM))), mouseX, mouseY);
        }
    }
}
