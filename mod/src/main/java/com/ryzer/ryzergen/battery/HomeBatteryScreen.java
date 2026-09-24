package com.ryzer.ryzergen.battery;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.GuiGauges;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Home battery readout. Positions match art/tools/gui_textures.py (home_battery). The six bays are
 * drawn top to bottom as the cabinet stacks them: the highest bay is module 6.
 */
public class HomeBatteryScreen extends AbstractContainerScreen<HomeBatteryMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/gui/home_battery.png");
    private static final int LABEL = 0xFF2B3036;
    private static final int VALUE = 0xFF35C8F5;
    private static final int DIM = 0xFF2A7CD6;
    private static final int GOOD = 0xFF44D65E;
    private static final int WARN = 0xFFF0A030;
    private static final int GAUGE_X = 11;
    private static final int GAUGE_Y = 22;
    private static final int GAUGE_H = 78;
    private static final int READOUT_X = 34;
    private static final int BAY_X = 121;
    private static final int BAY_Y = 22;
    private static final int BAY_STEP = 13;

    public HomeBatteryScreen(HomeBatteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 112;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        float fraction = menu.capacity() == 0 ? 0 : menu.energy() / (float) menu.capacity();
        GuiGauges.glow(graphics, leftPos + GAUGE_X, topPos + GAUGE_Y, 12, GAUGE_H, fraction, GuiGauges.ENERGY);
        GuiGauges.glass(graphics, leftPos + GAUGE_X, topPos + GAUGE_Y, 12, GAUGE_H);
        for (int bay = 0; bay < HomeBatteryBlockEntity.MAX_MODULES; bay++) {
            if (menu.module(bay) == BatteryChemistry.LEAD_ACID) {
                // A lead-acid module's front: lead grey case, orange terminal, a charge lamp.
                int x = leftPos + BAY_X;
                int y = topPos + bayY(bay);
                graphics.fillGradient(x, y, x + 44, y + 9, 0xFF6A7280, 0xFF454B56);
                graphics.fill(x, y, x + 44, y + 1, 0xFF868F9A);
                graphics.fill(x + 2, y + 3, x + 5, y + 6, 0xFFFF8A1E);
                graphics.fill(x + 39, y + 3, x + 41, y + 6, fraction > 0 ? 0xFF44D65E : 0xFF2B3036);
            }
        }
    }

    /** Bay 0 (the first module) sits at the bottom, like the cabinet. */
    private static int bayY(int bay) {
        return BAY_Y + (HomeBatteryBlockEntity.MAX_MODULES - 1 - bay) * BAY_STEP;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.battery.stored", compact(menu.energy())), READOUT_X, 25, VALUE, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.battery.capacity", compact(menu.capacity())), READOUT_X, 37, DIM, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.battery.in", compact(menu.in())), READOUT_X, 55, GOOD, false);
        graphics.drawString(font, Component.translatable("gui.ryzergen.battery.out", compact(menu.out())), READOUT_X, 67, WARN, false);
        for (int bay = 0; bay < HomeBatteryBlockEntity.MAX_MODULES; bay++) {
            BatteryChemistry module = menu.module(bay);
            Component name = Component.translatable("gui.ryzergen.battery.chemistry." + module.getSerializedName());
            graphics.drawString(font, name, BAY_X + 7, bayY(bay) + 1, module == BatteryChemistry.EMPTY ? 0xFF4D545D : 0xFF1D2126, false);
        }
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
        if (mx >= GAUGE_X - 1 && mx < GAUGE_X + 13 && my >= GAUGE_Y - 1 && my < GAUGE_Y + GAUGE_H + 1) {
            graphics.renderTooltip(font, Component.translatable("gui.ryzergen.energy",
                    String.format("%,d", menu.energy()), String.format("%,d", menu.capacity())), mouseX, mouseY);
        } else if (mx >= BAY_X - 1 && mx < BAY_X + 46 && my >= BAY_Y - 1 && my < bayY(0) + 11) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.ryzergen.battery.modules_hint"),
                    Component.translatable("gui.ryzergen.battery.remove_hint").withStyle(style -> style.withColor(0xFF9AA3AE))),
                    mouseX, mouseY);
        }
    }
}
