package com.ryzer.ryzergen.client.terminal;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * A screen's pixel art: ARGB pixels drawn in code, sent to a texture and drawn one texel per GUI
 * pixel, the same grid as the font. Made for one size; make a new one when the screen changes size.
 */
public final class PixelCanvas implements AutoCloseable {
    public final int w;
    public final int h;
    public final int[] px;
    private final DynamicTexture texture;
    private final ResourceLocation id;

    public PixelCanvas(int w, int h, String name) {
        this.w = w;
        this.h = h;
        this.px = new int[w * h];
        this.texture = new DynamicTexture(new NativeImage(w, h, false));
        this.id = Minecraft.getInstance().getTextureManager().register("ryzergen_" + name, texture);
    }

    public int get(int x, int y) {
        return px[y * w + x];
    }

    public void set(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < w && y < h) {
            px[y * w + x] = c;
        }
    }

    /** Light added over a pixel. */
    public void glow(int x, int y, int c, float f) {
        if (x >= 0 && y >= 0 && x < w && y < h && f > 0) {
            px[y * w + x] = Pixels.add(px[y * w + x], c, f);
        }
    }

    public void rect(int x0, int y0, int x1, int y1, int c) {
        for (int y = Math.max(0, y0); y <= Math.min(h - 1, y1); y++) {
            for (int x = Math.max(0, x0); x <= Math.min(w - 1, x1); x++) {
                px[y * w + x] = c;
            }
        }
    }

    public void copyFrom(int[] source) {
        System.arraycopy(source, 0, px, 0, px.length);
    }

    /** Sends the pixels to the texture. */
    public void upload() {
        NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = px[y * w + x];
                // NativeImage holds ABGR.
                image.setPixelRGBA(x, y, (c & 0xFF00FF00) | (c & 0xFF) << 16 | (c >> 16 & 0xFF));
            }
        }
        texture.upload();
    }

    /** Draws the picture with its top left at (x, y), {@code scale} screen units a pixel. */
    public void draw(GuiGraphics graphics, int x, int y, int scale) {
        graphics.blit(id, x, y, w * scale, h * scale, 0, 0, w, h, w, h);
    }

    /** Draws the picture with its top left at (x, y). */
    public void draw(GuiGraphics graphics, int x, int y) {
        graphics.blit(id, x, y, 0, 0, w, h, w, h);
    }

    @Override
    public void close() {
        Minecraft.getInstance().getTextureManager().release(id);
    }
}
