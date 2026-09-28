package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * The view through the wormhole as a picture, for when a shader pack is on and the real wormhole
 * (the lensing pass, shaders/core/wormhole.fsh) cannot run. Painted once, the same way the shader
 * paints its far side: deep space, a faint nebula, stars, and the new star off to one side, bulged
 * like the view through a sphere. Round: clear outside the circle.
 */
public final class WormholeFarSide {
    private static final int SIZE = 256;
    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "dynamic/wormhole_far_side");
    private static boolean made;

    private WormholeFarSide() {}

    /** The picture's texture, painting it the first time. */
    public static ResourceLocation texture() {
        if (!made) {
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    float px = (x + 0.5F) / SIZE * 2 - 1, py = (y + 0.5F) / SIZE * 2 - 1;
                    float l = (float) Math.sqrt(px * px + py * py);
                    if (l > 1) {
                        image.setPixelRGBA(x, y, 0);
                        continue;
                    }
                    float[] c = farSide(px, py, l);
                    image.setPixelRGBA(x, y, FastColor.ABGR32.color(255, channel(c[2]), channel(c[1]), channel(c[0])));
                }
            }
            Minecraft.getInstance().getTextureManager().register(LOCATION, new DynamicTexture(image));
            made = true;
        }
        return LOCATION;
    }

    private static int channel(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255)));
    }

    /** As the shader's farSide, without the twinkle and drift. */
    private static float[] farSide(float px, float py, float l) {
        float bulge = 1 + 1.6F * l * l * l;
        float qx = px * bulge, qy = py * bulge;
        float r = 0.008F, g = 0.01F, b = 0.025F;
        float n = (float) Math.pow(fbm(qx * 1.5F, qy * 1.5F), 2.6);
        r += 0.28F * n;
        g += 0.1F * n;
        b += 0.38F * n;
        float m = (float) Math.pow(fbm(qx * 2.2F + 7, qy * 2.2F + 7), 3.0);
        r += 0.05F * m;
        g += 0.16F * m;
        b += 0.32F * m;
        float s = stars(qx, qy, 34, 0.9F, 0.16F) + stars(qx, qy, 13, 0.95F, 0.12F) * 1.4F;
        r += 0.85F * s;
        g += 0.9F * s;
        b += s;
        float ds = (float) Math.hypot(qx - 0.24F, qy - 0.14F);
        float disc = 1 - smooth(0.13F, 0.15F, ds);
        float corona = (float) Math.exp(-ds * 9) * 0.9F, halo = (float) Math.exp(-ds * 3) * 0.12F;
        r += disc + corona + halo;
        g += 0.97F * disc + 0.72F * corona + 0.85F * halo;
        b += 0.9F * disc + 0.42F * corona + 0.7F * halo;
        float edge = 0.35F + 0.65F * (1 - smooth(0.75F, 1, l));
        return new float[] {r * edge, g * edge, b * edge};
    }

    private static float stars(float qx, float qy, float scale, float chance, float size) {
        float gx = qx * scale, gy = qy * scale;
        float cx = (float) Math.floor(gx), cy = (float) Math.floor(gy);
        float h = hash(cx, cy);
        if (h < chance) {
            return 0;
        }
        float ax = cx + hash(cx + 1.7F, cy + 1.7F) * 0.8F + 0.1F, ay = cy + hash(cx + 3.1F, cy + 3.1F) * 0.8F + 0.1F;
        return 1 - smooth(0, size, (float) Math.hypot(gx - ax, gy - ay));
    }

    private static float hash(float x, float y) {
        double v = Math.sin(x * 127.1 + y * 311.7) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    private static float noise(float x, float y) {
        float ix = (float) Math.floor(x), iy = (float) Math.floor(y);
        float fx = x - ix, fy = y - iy;
        float ux = fx * fx * (3 - 2 * fx), uy = fy * fy * (3 - 2 * fy);
        float a = hash(ix, iy), b = hash(ix + 1, iy), c = hash(ix, iy + 1), d = hash(ix + 1, iy + 1);
        return (a + (b - a) * ux) + ((c + (d - c) * ux) - (a + (b - a) * ux)) * uy;
    }

    private static float fbm(float x, float y) {
        float sum = 0, amp = 0.5F;
        for (int i = 0; i < 4; i++) {
            sum += amp * noise(x, y);
            x *= 2.03F;
            y *= 2.03F;
            amp *= 0.5F;
        }
        return sum;
    }

    private static float smooth(float from, float to, float x) {
        float t = Math.max(0, Math.min(1, (x - from) / (to - from)));
        return t * t * (3 - 2 * t);
    }
}
