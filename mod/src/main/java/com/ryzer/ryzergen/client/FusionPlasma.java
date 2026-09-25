package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector3f;

import java.util.Random;

/**
 * The plasma inside the fusion reactor's vessel, drawn fresh every frame. Geometry follows
 * art/tools/tokamak_concept.py.
 *
 * <ul>
 *   <li><b>Glow:</b> three nested shells of wispy filaments (block/fusion/plasma_wisp, animated),
 *   drawn additively so their dark parts are clear and only the light shows. Each turns round the
 *   ring at its own speed, some one way and some the other, so the plasma flows in layers.</li>
 *   <li><b>Arcs:</b> violet streamers that leap from the plasma to the wall and fork on the way,
 *   like a Tesla coil's. New every couple of ticks, so they flicker.</li>
 * </ul>
 *
 * <p>Real basis, loosely: a tokamak plasma is a ring current of millions of amps, flowing round the
 * machine, and its edge throws off filaments. This is the look of that, not a simulation.
 */
public final class FusionPlasma {
    // The design's torus, in pixels: centre, ring radius, tube middle height, and the inside of the wall.
    private static final float C = 152;
    private static final float R = 88;
    private static final float YC = 76;
    private static final float WALL = 33;

    // ---------------------------------------------------------------- glow shells
    private static final float[] SHELLS = {9, 16, 24};
    private static final int[] TINTS = {0xFFD89CE0, 0xFFB050D8, 0xFF6A2CB0};
    /** Degrees per second each shell turns round the ring. */
    private static final float[] SPEEDS = {18, -27, 40};
    private static final int ROUND = 48;
    private static final int ACROSS = 12;
    private static final ResourceLocation WISP = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/fusion/plasma_wisp");
    /** Each shell's corners, built once: [shell][i][j] = x, y, z in blocks. */
    private static float[][][][] points;

    // ---------------------------------------------------------------- arcs
    private static final float ARC_FROM = 24;
    private static final int BOLTS = 14;
    private static final int STEPS = 9;
    private static final int CORE = 0xFFFFE8FF;
    private static final int GLOW = 0x88C060FF;
    private static final ResourceLocation ARC = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/fusion/arc");

    private FusionPlasma() {}

    /** The glow, into an additive buffer (RenderType.eyes on the block atlas). */
    public static void drawGlow(VertexConsumer buffer, PoseStack pose, float seconds) {
        if (points == null) {
            points = buildShells();
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WISP);
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        for (int k = 0; k < SHELLS.length; k++) {
            pose.pushPose();
            pose.translate(C / 16, 0, C / 16);
            pose.mulPose(Axis.YP.rotationDegrees(seconds * SPEEDS[k]));
            pose.translate(-C / 16, 0, -C / 16);
            PoseStack.Pose last = pose.last();
            // A slow breathing, out of step between the shells.
            float pulse = 0.75F + 0.25F * (float) Math.sin(seconds * 2.1F + k * 2.0F);
            int colour = scale(TINTS[k], pulse);
            float[][][] shell = points[k];
            for (int i = 0; i < ROUND; i++) {
                for (int j = 0; j < ACROSS; j++) {
                    float[] a = shell[i][j], b = shell[i + 1][j], c = shell[i + 1][j + 1], d = shell[i][j + 1];
                    // Both ways round: the far side of each shell shows through the near side.
                    quad(buffer, last, a, b, c, d, u0, v0, u1, v1, colour);
                    quad(buffer, last, d, c, b, a, u1, v0, u0, v1, colour);
                }
            }
            pose.popPose();
        }
    }

    private static float[][][][] buildShells() {
        float[][][][] out = new float[SHELLS.length][ROUND + 1][ACROSS + 1][];
        for (int k = 0; k < SHELLS.length; k++) {
            for (int i = 0; i <= ROUND; i++) {
                double phi = Math.PI * 2 * i / ROUND;
                for (int j = 0; j <= ACROSS; j++) {
                    double theta = Math.PI * 2 * j / ACROSS;
                    Vector3f p = point((float) phi, (float) theta, SHELLS[k]);
                    out[k][i][j] = new float[] {p.x, p.y, p.z};
                }
            }
        }
        return out;
    }

    /** One panel: v runs round the ring (the way it flows), u round the tube. */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float[] a, float[] b, float[] c, float[] d,
                             float ua, float va, float ub, float vb, int colour) {
        vertex(buffer, pose, a, ua, va, colour);
        vertex(buffer, pose, b, ua, vb, colour);
        vertex(buffer, pose, c, ub, vb, colour);
        vertex(buffer, pose, d, ub, va, colour);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float[] p, float u, float v, int colour) {
        buffer.addVertex(pose, p[0], p[1], p[2])
                .setColor(colour)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }

    private static int scale(int argb, float f) {
        int r = Math.round(((argb >> 16) & 0xFF) * f);
        int g = Math.round(((argb >> 8) & 0xFF) * f);
        int b = Math.round((argb & 0xFF) * f);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Draws this frame's arcs; `time` is the game time, `salt` keeps two reactors apart. */
    public static void drawArcs(VertexConsumer buffer, PoseStack pose, long time, long salt) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ARC);
        // A new set every 2 ticks, and each bolt only lives for some of those.
        Random random = new Random((time / 2) * 341873128712L + salt);
        PoseStack.Pose last = pose.last();
        for (int b = 0; b < BOLTS; b++) {
            if (random.nextFloat() < 0.3F) {
                continue;
            }
            float phi = random.nextFloat() * (float) (Math.PI * 2);
            float span = 0.12F + random.nextFloat() * 0.3F;
            float theta = random.nextFloat() * (float) (Math.PI * 2);
            bolt(buffer, last, sprite, random, phi, span, theta, ARC_FROM, STEPS, true);
        }
    }

    /**
     * One streamer: from `r0` out from the tube's middle to the wall, drifting round the ring by
     * `span` and wandering round the tube, with forks.
     */
    private static void bolt(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, Random random,
                             float phi, float span, float theta, float r0, int steps, boolean forks) {
        Vector3f prev = point(phi, theta, r0);
        float r = r0;
        for (int s = 1; s <= steps; s++) {
            phi += span / steps * (0.5F + random.nextFloat());
            theta += (random.nextFloat() - 0.5F) * 0.7F;
            r = s == steps ? WALL : Math.min(WALL, r + (WALL - r0) / steps + (random.nextFloat() - 0.5F) * 4);
            Vector3f next = point(phi, theta, r);
            float fade = 1 - 0.5F * s / steps;
            segment(buffer, pose, sprite, prev, next, 2.6F * fade, GLOW);
            segment(buffer, pose, sprite, prev, next, 0.7F * fade, CORE);
            if (forks && s < steps - 1 && random.nextFloat() < 0.3F) {
                bolt(buffer, pose, sprite, random, phi, span * 0.5F, theta + (random.nextFloat() - 0.5F) * 1.2F, r, 3, false);
            }
            prev = next;
        }
    }

    /** A point on the torus, in blocks (the design's pixels / 16). */
    private static Vector3f point(float phi, float theta, float rad) {
        float d = R + rad * (float) Math.cos(theta);
        return new Vector3f((C + d * (float) Math.cos(phi)) / 16, (YC + rad * (float) Math.sin(theta)) / 16,
                (C + d * (float) Math.sin(phi)) / 16);
    }

    /** A streak from a to b, `width` pixels across: two crossed ribbons, each drawn both ways round. */
    private static void segment(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, Vector3f a, Vector3f b,
                                float width, int colour) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-8F) {
            return;
        }
        Vector3f side = new Vector3f(dir).cross(Math.abs(dir.y) > 0.9F * dir.length() ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0))
                .normalize(width / 32);
        Vector3f other = new Vector3f(dir).cross(side).normalize(width / 32);
        for (Vector3f s : new Vector3f[] {side, other}) {
            ribbon(buffer, pose, sprite, a, b, s, colour, false);
            ribbon(buffer, pose, sprite, a, b, s, colour, true);
        }
    }

    private static void ribbon(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, Vector3f a, Vector3f b,
                               Vector3f side, int colour, boolean flip) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float[][] corners = {
                {a.x - side.x, a.y - side.y, a.z - side.z, u0, v0},
                {b.x - side.x, b.y - side.y, b.z - side.z, u1, v0},
                {b.x + side.x, b.y + side.y, b.z + side.z, u1, v1},
                {a.x + side.x, a.y + side.y, a.z + side.z, u0, v1},
        };
        for (int i = 0; i < 4; i++) {
            float[] c = corners[flip ? 3 - i : i];
            buffer.addVertex(pose, c[0], c[1], c[2])
                    .setColor(colour)
                    .setUv(c[3], c[4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, 0, 1, 0);
        }
    }
}
