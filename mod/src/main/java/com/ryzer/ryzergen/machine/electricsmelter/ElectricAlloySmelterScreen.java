package com.ryzer.ryzergen.machine.electricsmelter;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import static com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterBlockEntity.*;

/** Positions match art/tools/gui_textures.py (electric_alloy_smelter). */
public class ElectricAlloySmelterScreen extends AbstractContainerScreen<ElectricAlloySmelterMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/electric_alloy_smelter.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int ENERGY_X = 153;
    private static final int ENERGY_Y = 18;
    private static final int ENERGY_H = 50;

    public ElectricAlloySmelterScreen(ElectricAlloySmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int arrow = Mth.ceil(menu.progress() * 24);
        graphics.blit(TEXTURE, leftPos + 79, topPos + 34, 176, 0, arrow, 17);
        GuiGauges.glow(graphics, leftPos + ENERGY_X, topPos + ENERGY_Y, 8, ENERGY_H, menu.energy() / (float) ENERGY_CAPACITY, GuiGauges.ENERGY);
        GuiGauges.glass(graphics, leftPos + ENERGY_X, topPos + ENERGY_Y, 8, ENERGY_H);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= ENERGY_X - 1 && mx < ENERGY_X + 9 && my >= ENERGY_Y - 1 && my < ENERGY_Y + ENERGY_H + 1) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.energy",
                    String.format("%,d", menu.energy()), String.format("%,d", ENERGY_CAPACITY)), mouseX, mouseY);
        }
    }
}
