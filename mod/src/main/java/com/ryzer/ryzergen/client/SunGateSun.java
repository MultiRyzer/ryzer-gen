package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * The sun gate's moving parts, drawn fresh every frame round the sun's middle. Geometry follows
 * art/tools/sun_gate_concept.py.
 *
 * <ul>
 *   <li><b>Sun:</b> a sphere of boiling granulation (block/sun/photosphere, animated), turning
 *   slowly, darker and redder towards its edge as seen from the camera. Real basis: limb darkening.
 *   Near the edge you look through the cooler top of the photosphere, so it is dimmer and redder.</li>
 *   <li><b>Corona and prominences:</b> a soft glow round the edge that dims as the swarm grows
 *   (the shades catch the light), and a few flickering loops standing off the surface.</li>
 *   <li><b>Shades:</b> six tilted orbits of 24 slots. Filled slots hold a solar shade facing the
 *   sun (cells towards it, gold foil out); empty ones show as faint outlines, so progress reads at
 *   a glance. Inner orbits turn faster, by Kepler's third law (the period goes as the radius to the
 *   power 1.5).</li>
 * </ul>
 */
public final class SunGateSun {
    /** Height of the sun's middle above the block, in blocks. */
    public static final float Y = 104 / 16F;
    private static final float RS = 24 / 16F;
    // Orbits: radius (pixels), tilt and the way the tilt leans (degrees). Keep in step with the concept.
    private static final float[][] ORBITS = {{36, 8, 0}, {41, 58, 20}, {46, -58, 80}, {51, 30, 140}, {56, -30, 200}, {61, 75, 260}};
    private static final int SLOTS = 24;
    public static final int TOTAL = ORBITS.length * SLOTS;
    private static final float SHADE = 3.5F / 16;
    /** The inner orbit's turn, radians per second (about 21 seconds round). */
    private static final float ORBIT_SPEED = 0.3F;
    /** The sun's own turn, radians per second. */
    private static final float SUN_SPEED = 0.06F;
    private static final int LON = 24;
    private static final int LAT = 12;

    private static final ResourceLocation PHOTOSPHERE = texture("photosphere");
    private static final ResourceLocation SHADE_FRONT = texture("shade");
    private static final ResourceLocation SHADE_BACK = texture("shade_back");
    private static final ResourceLocation GHOST = texture("shade_ghost");
    private static final ResourceLocation WHITE = texture("white");

    private static final int GHOST_TINT = 0xFF1E5A68;
    private static final int ORBIT_TINT = 0xFF0A2630;
    /** Each slot's place in the fill order (see the concept's fill_order). */
    private static int[][] rank;

    private SunGateSun() {}

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/sun/" + name);
    }

    /**
     * Draws it all, with {@code pose} at the sun's middle (not turned with the block: it looks the
     * same from every side). {@code eye} is the camera, relative to the sun's middle, in blocks.
     */
    public static void draw(MultiBufferSource buffers, PoseStack pose, Vector3f eye, float seconds, float coverage, int light) {
        Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        PoseStack.Pose last = pose.last();
        // One buffer at a time: asking for the next ends a shared one, which must be finished by then.
        sphere(buffers.getBuffer(RenderType.beaconBeam(InventoryMenu.BLOCK_ATLAS, false)), last, eye, seconds, atlas.apply(PHOTOSPHERE));
        int shown = Math.round(coverage * TOTAL);
        shades(buffers.getBuffer(Sheets.cutoutBlockSheet()), last, seconds, shown, light, atlas.apply(SHADE_FRONT), atlas.apply(SHADE_BACK), null);
        // Light added, with no depth written: the outlines, orbit lines, corona and prominences.
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS));
        shades(glow, last, seconds, shown, light, null, null, atlas.apply(GHOST));
        TextureAtlasSprite white = atlas.apply(WHITE);
        orbitLines(glow, last, white);
        corona(glow, last, eye, 1 - 0.55F * coverage, white);
        prominences(glow, last, eye, seconds, white);
    }

    // ---------------------------------------------------------------- sun
    private static void sphere(VertexConsumer buffer, PoseStack.Pose pose, Vector3f eye, float seconds, TextureAtlasSprite sprite) {
        float spin = seconds * SUN_SPEED;
        float[][][] p = new float[LON + 1][LAT + 1][];
        for (int i = 0; i <= LON; i++) {
            double lon = Math.PI * 2 * i / LON + spin;
            for (int j = 0; j <= LAT; j++) {
                double lat = Math.PI * j / LAT - Math.PI / 2;
                float x = (float) (Math.cos(lat) * Math.cos(lon)), y = (float) Math.sin(lat), z = (float) (Math.cos(lat) * Math.sin(lon));
                p[i][j] = new float[] {x, y, z, limb(x, y, z, eye)};
            }
        }
        float du = (sprite.getU1() - sprite.getU0()) / 3, dv = (sprite.getV1() - sprite.getV0()) / 3;
        for (int i = 0; i < LON; i++) {
            for (int j = 0; j < LAT; j++) {
                // Three panels to a tile each way, as in the concept.
                float u0 = sprite.getU0() + du * (i % 3), v0 = sprite.getV0() + dv * (j % 3);
                sunVertex(buffer, pose, p[i][j], u0, v0);
                sunVertex(buffer, pose, p[i][j + 1], u0, v0 + dv);
                sunVertex(buffer, pose, p[i + 1][j + 1], u0 + du, v0 + dv);
                sunVertex(buffer, pose, p[i + 1][j], u0 + du, v0);
            }
        }
    }

    /** Limb darkening: the brightness seen at this point of the surface, towards the camera. */
    private static float limb(float x, float y, float z, Vector3f eye) {
        Vector3f toEye = new Vector3f(eye).sub(x * RS, y * RS, z * RS).normalize();
        float mu = Math.max(0, x * toEye.x + y * toEye.y + z * toEye.z);
        // The linear law with u = 0.6, near the real sun's in visible light.
        return 1 - 0.6F * (1 - mu);
    }

    private static void sunVertex(VertexConsumer buffer, PoseStack.Pose pose, float[] p, float u, float v) {
        float b = p[3];
        // Redder towards the edge: blue fades fastest, then green.
        int colour = 0xFF000000 | channel(b, 0.8) << 16 | channel(b, 1.2) << 8 | channel(b, 1.8);
        buffer.addVertex(pose, p[0] * RS, p[1] * RS, p[2] * RS)
                .setColor(colour)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, p[0], p[1], p[2]);
    }

    private static int channel(float brightness, double power) {
        return (int) Math.round(255 * Math.pow(brightness, power));
    }

    // ---------------------------------------------------------------- shades
    /**
     * Filled slots as solid shades (when {@code front} is given), or empty ones as outlines (when
     * {@code ghost} is given).
     */
    private static void shades(VertexConsumer buffer, PoseStack.Pose pose, float seconds, int shown, int light,
                               TextureAtlasSprite front, TextureAtlasSprite back, TextureAtlasSprite ghost) {
        if (rank == null) {
            rank = fillOrder();
        }
        Vector3f pos = new Vector3f(), along = new Vector3f(), axis = new Vector3f();
        for (int ring = 0; ring < ORBITS.length; ring++) {
            float speed = ORBIT_SPEED * (float) Math.pow(ORBITS[0][0] / ORBITS[ring][0], 1.5);
            for (int slot = 0; slot < SLOTS; slot++) {
                boolean filled = rank[ring][slot] < shown;
                if (filled != (ghost == null)) {
                    continue;
                }
                float theta = (float) (Math.PI * 2 * slot / SLOTS) + ring * 0.7F + seconds * speed;
                frame(ring, theta, pos, along, axis);
                if (ghost != null) {
                    panel(buffer, pose, pos, along, axis, ghost, GHOST_TINT, LightTexture.FULL_BRIGHT, false);
                    panel(buffer, pose, pos, along, axis, ghost, GHOST_TINT, LightTexture.FULL_BRIGHT, true);
                } else {
                    // The cells face the sun and are lit by it; the foil faces out, lit by the world.
                    panel(buffer, pose, pos, along, axis, front, 0xFFFFFFFF, LightTexture.FULL_BRIGHT, false);
                    panel(buffer, pose, pos, along, axis, back, 0xFFFFFFFF, light, true);
                }
            }
        }
    }

    /** A slot's place (from the sun's middle, in blocks), the way its orbit runs there, and the orbit's axis. */
    private static void frame(int ring, float theta, Vector3f pos, Vector3f along, Vector3f axis) {
        float r = ORBITS[ring][0] / 16;
        float c = (float) Math.cos(theta), s = (float) Math.sin(theta);
        turn(ring, r * c, 0, r * s, pos);
        turn(ring, -s, 0, c, along);
        turn(ring, 0, 1, 0, axis);
    }

    /** From the orbit's own plane to the world: tilt about x, then lean about y. */
    private static void turn(int ring, float x, float y, float z, Vector3f out) {
        double i = Math.toRadians(ORBITS[ring][1]), o = Math.toRadians(ORBITS[ring][2]);
        float ty = (float) (y * Math.cos(i) - z * Math.sin(i));
        float tz = (float) (y * Math.sin(i) + z * Math.cos(i));
        out.set((float) (x * Math.cos(o) + tz * Math.sin(o)), ty, (float) (-x * Math.sin(o) + tz * Math.cos(o)));
    }

    /** One side of a shade: the side facing the sun, or (with {@code outward}) the side facing away. */
    private static void panel(VertexConsumer buffer, PoseStack.Pose pose, Vector3f pos, Vector3f along, Vector3f axis,
                              TextureAtlasSprite sprite, int colour, int light, boolean outward) {
        float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        float n = outward ? 1 : -1;
        Vector3f normal = new Vector3f(pos).normalize().mul(n);
        for (int k = 0; k < 4; k++) {
            float[] c = corners[outward ? 3 - k : k];
            float x = pos.x + SHADE * (c[0] * along.x + c[1] * axis.x);
            float y = pos.y + SHADE * (c[0] * along.y + c[1] * axis.y);
            float z = pos.z + SHADE * (c[0] * along.z + c[1] * axis.z);
            buffer.addVertex(pose, x, y, z)
                    .setColor(colour)
                    .setUv(c[0] < 0 ? sprite.getU0() : sprite.getU1(), c[1] < 0 ? sprite.getV1() : sprite.getV0())
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, normal.x, normal.y, normal.z);
        }
    }

    /**
     * Bit-reversed counting over the slots, all six orbits together: each orbit fills evenly all
     * round rather than from one end.
     */
    private static int[][] fillOrder() {
        int[][] out = new int[ORBITS.length][SLOTS];
        int next = 0;
        for (int m = 0; m < 32; m++) {
            int slot = Integer.reverse(m) >>> 27;
            if (slot >= SLOTS) {
                continue;
            }
            for (int ring = 0; ring < ORBITS.length; ring++) {
                out[ring][slot] = next++;
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- glows
    /** Each orbit as a faint line: two thin ribbons, one in its plane and one across it. */
    private static void orbitLines(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite white) {
        int steps = 64;
        float half = 0.25F / 16;
        Vector3f a = new Vector3f(), b = new Vector3f(), t = new Vector3f(), axis = new Vector3f(), unused = new Vector3f();
        for (int ring = 0; ring < ORBITS.length; ring++) {
            for (int k = 0; k < steps; k++) {
                frame(ring, (float) (Math.PI * 2 * k / steps), a, t, axis);
                frame(ring, (float) (Math.PI * 2 * (k + 1) / steps), b, unused, unused);
                Vector3f out = new Vector3f(a).normalize();
                ribbon(buffer, pose, a, b, axis.mul(half), ORBIT_TINT, ORBIT_TINT, white);
                ribbon(buffer, pose, a, b, out.mul(half), ORBIT_TINT, ORBIT_TINT, white);
            }
        }
    }

    /**
     * A soft glow round the sun's edge, square to the camera: bright at the limb, fading to nothing
     * well out. Its middle is hidden behind the sun.
     */
    private static void corona(VertexConsumer buffer, PoseStack.Pose pose, Vector3f eye, float strength, TextureAtlasSprite white) {
        Vector3f toEye = new Vector3f(eye).normalize();
        Vector3f right = new Vector3f(toEye).cross(0, 1, 0);
        if (right.lengthSquared() < 1e-4F) {
            right.set(1, 0, 0);
        }
        right.normalize();
        Vector3f up = new Vector3f(right).cross(toEye).normalize();
        float[] radii = {0.9F * RS, 1.06F * RS, 1.5F * RS, 2.5F * RS};
        int[] colours = {scale(0xFFFFD890, strength), scale(0xFFD88A38, strength * 0.7F), scale(0xFF6A340E, strength * 0.4F), 0xFF000000};
        int steps = 48;
        for (int ring = 0; ring < radii.length - 1; ring++) {
            for (int k = 0; k < steps; k++) {
                double a0 = Math.PI * 2 * k / steps, a1 = Math.PI * 2 * (k + 1) / steps;
                float[][] pts = {
                        disc(right, up, a0, radii[ring]), disc(right, up, a1, radii[ring]),
                        disc(right, up, a1, radii[ring + 1]), disc(right, up, a0, radii[ring + 1])};
                int[] cols = {colours[ring], colours[ring], colours[ring + 1], colours[ring + 1]};
                for (int flip = 0; flip < 2; flip++) {
                    for (int v = 0; v < 4; v++) {
                        int idx = flip == 0 ? v : 3 - v;
                        glowVertex(buffer, pose, pts[idx][0], pts[idx][1], pts[idx][2], cols[idx], white, idx);
                    }
                }
            }
        }
    }

    private static float[] disc(Vector3f right, Vector3f up, double angle, float radius) {
        float c = (float) Math.cos(angle) * radius, s = (float) Math.sin(angle) * radius;
        return new float[] {right.x * c + up.x * s, right.y * c + up.y * s, right.z * c + up.z * s};
    }

    /** A few loops of glowing gas standing off the surface, turning with the sun and flickering. */
    private static void prominences(VertexConsumer buffer, PoseStack.Pose pose, Vector3f eye, float seconds, TextureAtlasSprite white) {
        float spin = seconds * SUN_SPEED;
        float[][] feet = {{0.3F, 0.25F}, {2.2F, -0.3F}, {3.6F, 0.1F}, {5.1F, -0.15F}};
        int steps = 10;
        for (int k = 0; k < feet.length; k++) {
            float lon = feet[k][0] + spin, lat = feet[k][1];
            Vector3f from = surface(lon, lat), to = surface(lon + 0.38F, lat + 0.06F);
            float rise = 0.42F + 0.08F * (float) Math.sin(seconds * 1.3F + k * 2.1F);
            float flicker = 0.75F + 0.25F * (float) Math.sin(seconds * 5.7F + k * 1.9F);
            Vector3f prev = null;
            for (int s = 0; s <= steps; s++) {
                float t = (float) s / steps;
                Vector3f p = new Vector3f(from).lerp(to, t).normalize()
                        .mul(RS * (1 + rise * (float) Math.sin(Math.PI * t)));
                if (prev != null) {
                    Vector3f side = new Vector3f(p).sub(prev).cross(new Vector3f(eye).sub(p)).normalize();
                    ribbon(buffer, pose, prev, p, new Vector3f(side).mul(1.4F / 16), scale(0xFFFF6A1E, flicker), scale(0xFFFF6A1E, flicker), white);
                    ribbon(buffer, pose, prev, p, side.mul(0.5F / 16), scale(0xFFFFD0A0, flicker), scale(0xFFFFD0A0, flicker), white);
                }
                prev = p;
            }
        }
    }

    private static Vector3f surface(float lon, float lat) {
        return new Vector3f((float) (Math.cos(lat) * Math.cos(lon)), (float) Math.sin(lat), (float) (Math.cos(lat) * Math.sin(lon)));
    }

    /** A flat strip from a to b, `side` either way across, drawn both ways round. */
    private static void ribbon(VertexConsumer buffer, PoseStack.Pose pose, Vector3f a, Vector3f b, Vector3f side,
                               int colourA, int colourB, TextureAtlasSprite white) {
        float[][] pts = {
                {a.x - side.x, a.y - side.y, a.z - side.z}, {b.x - side.x, b.y - side.y, b.z - side.z},
                {b.x + side.x, b.y + side.y, b.z + side.z}, {a.x + side.x, a.y + side.y, a.z + side.z}};
        int[] cols = {colourA, colourB, colourB, colourA};
        for (int flip = 0; flip < 2; flip++) {
            for (int v = 0; v < 4; v++) {
                int idx = flip == 0 ? v : 3 - v;
                glowVertex(buffer, pose, pts[idx][0], pts[idx][1], pts[idx][2], cols[idx], white, idx);
            }
        }
    }

    private static void glowVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int colour,
                                   TextureAtlasSprite sprite, int corner) {
        buffer.addVertex(pose, x, y, z)
                .setColor(colour)
                .setUv(corner == 0 || corner == 3 ? sprite.getU0() : sprite.getU1(), corner < 2 ? sprite.getV0() : sprite.getV1())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }

    private static int scale(int argb, float f) {
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((argb & 0xFF) * f));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
