package com.ryzer.ryzergen.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Gauge fills shared by every Ryzer Gen GUI. Never pixel art: lit gradients for energy and heat, and
 * fluids drawn with the fluid's own animated texture, in the spirit of Mekanism. Positions are in
 * screen pixels, and the gauge fills from the bottom.
 */
public final class GuiGauges {
    /** Standard colours. */
    public static final int ENERGY = 0xFFFF4D3D;

    private GuiGauges() {}

    /** A lit gauge: bright top to dark base, a bright surface line and a soft band of light drifting up. */
    public static void glow(GuiGraphics graphics, int x, int y, int w, int h, float fraction, int colour) {
        int fill = Math.round(Mth.clamp(fraction, 0, 1) * h);
        if (fill <= 0) {
            return;
        }
        int bottom = y + h;
        int top = bottom - fill;
        graphics.fillGradient(x, top, x + w, bottom, colour, ARGB.darken(colour, 0.45F));
        graphics.enableScissor(x, top, x + w, bottom);
        int band = bottom - (int) (Util.getMillis() / 45 % (h + 16)) + 8;
        graphics.fillGradient(x, band - 8, x + w, band, 0x00FFFFFF, 0x38FFFFFF);
        graphics.fillGradient(x, band, x + w, band + 8, 0x38FFFFFF, 0x00FFFFFF);
        graphics.disableScissor();
        graphics.fill(x, top, x + w, top + 1, ARGB.lighten(colour, 0.55F));
    }

    /** A fluid gauge drawn with the fluid's own animated texture and tint. */
    public static void fluid(GuiGraphics graphics, int x, int y, int w, int h, float fraction, Fluid fluid) {
        int fill = Math.round(Mth.clamp(fraction, 0, 1) * h);
        if (fill <= 0) {
            return;
        }
        IClientFluidTypeExtensions fluidClient = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(fluidClient.getStillTexture());
        int tint = fluidClient.getTintColor();
        float r = (tint >> 16 & 0xFF) / 255F;
        float g = (tint >> 8 & 0xFF) / 255F;
        float b = (tint & 0xFF) / 255F;
        int bottom = y + h;
        graphics.enableScissor(x, bottom - fill, x + w, bottom);
        for (int ty = bottom - w; ty > bottom - fill - w; ty -= w) {
            graphics.blit(x, ty, 0, w, w, sprite, r, g, b, 1F);
        }
        graphics.disableScissor();
        graphics.fill(x, bottom - fill, x + w, bottom - fill + 1, 0x60FFFFFF);
    }

    /** Glass over a gauge: a highlight down the left and scale ticks every 6 pixels on the right. */
    public static void glass(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + 1, y + h, 0x28FFFFFF);
        for (int ty = y + 5; ty < y + h; ty += 6) {
            graphics.fill(x + w - 2, ty, x + w, ty + 1, (ty - y + 1) % 12 == 0 ? 0x70000000 : 0x40000000);
        }
    }
}
