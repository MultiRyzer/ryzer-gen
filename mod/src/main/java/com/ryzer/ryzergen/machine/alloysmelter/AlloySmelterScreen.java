package com.ryzer.ryzergen.machine.alloysmelter;

import com.ryzer.ryzergen.client.SoundTab;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class AlloySmelterScreen extends AbstractContainerScreen<AlloySmelterMenu> implements SoundTab.Host {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/alloy_smelter.png");
    private static final int LABEL = 0xFF2B3036;

    public AlloySmelterScreen(AlloySmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // The machine bay starts at y 13, so the title sits higher than the default, and the
        // inventory label clears the bay's bottom edge (y 71).
        titleLabelY = 4;
        inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        SoundTab.render(graphics, leftPos, topPos, imageWidth, soundTabY(), menu.muted(), mouseX, mouseY);
        if (menu.isLit()) {
            // The flame burns down from the top as fuel runs out.
            int flame = Mth.ceil(menu.litProgress() * 13);
            graphics.blit(TEXTURE, leftPos + 47, topPos + 36 + 13 - flame, 176, 13 - flame, 14, flame + 1);
        }
        int arrow = Mth.ceil(menu.cookProgress() * 24);
        graphics.blit(TEXTURE, leftPos + 79, topPos + 34, 176, 14, arrow, 17);
    }

    /** Dark graphite labels on the light casing panel, as on every Ryzer Gen GUI. */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        SoundTab.tooltip(graphics, font, leftPos, topPos, imageWidth, soundTabY(), menu.muted(), mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** The sound switch's tab on the panel's right edge. */
    @Override
    public int soundTabY() {
        return SoundTab.TOP;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && SoundTab.click(menu, leftPos, topPos, imageWidth, soundTabY(), mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
