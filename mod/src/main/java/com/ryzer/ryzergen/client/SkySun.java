package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.sky.SunSwarmClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Function;

/**
 * The sun and its Dyson swarm as seen in the overworld's sky (drawn by SkySwarmRenderer). Units:
 * the sun's radius is 1, and the ground is straight down the local -y axis.
 *
 * <ul>
 *   <li><b>Sun:</b> a flat disc facing the ground, white-hot in the middle and warmer towards the
 *   edge (limb darkening), with a soft corona. Seen from this far off a star is a disc.</li>
 *   <li><b>Swarm:</b> 640 small panels on 16 orbits, spread in tilt all round the sun, inner orbits
 *   faster (Kepler's third law). They launch in turn from the sun's surface. Their cells face the
 *   sun, so from the ground you mostly see their graphite backs against the sky.</li>
 *   <li><b>Shell:</b> every panel also has a tile of the shell, a sphere laid out round the line to
 *   the ground. While it closes, each panel flies from its orbit into its tile, turning and growing
 *   to fit, the far side first and the side facing the ground last, so the last gap shuts like an
 *   iris. Nothing else is drawn: when the last panel lands, the shell is whole.</li>
 *   <li><b>Shut:</b> the shell glows a dull red over the side facing the ground. Real basis: a Dyson
 *   sphere cannot hide its star's waste heat, so it glows in the infrared.</li>
 * </ul>
 */
public final class SkySun {
    private static final int RINGS = 16;
    private static final int BANDS = SunSwarmClient.SHELL_BANDS;
    private static final int ROUND = 40;
    private static final int PANELS = BANDS * ROUND;
    private static final int PER_RING = PANELS / RINGS;
    /** The shell's radius, just outside the sun. */
    private static final float SHELL = 1.25F;
    /** Half a panel's width in orbit. */
    private static final float PANEL = 0.075F;
    /** The inner orbit's turn, radians per second. */
    private static final float ORBIT_SPEED = 0.35F;

    private static final ResourceLocation CELL = texture("swarm_cell");
    private static final ResourceLocation BACK = texture("swarm_back");
    private static final ResourceLocation WHITE = texture("white");

    /** Each orbit's radius, and the two directions across its plane. */
    private static final float[] RADIUS = new float[RINGS];
    private static final Vector3f[] ACROSS = new Vector3f[RINGS];
    private static final Vector3f[] ALONG = new Vector3f[RINGS];
    private static final Vector3f[] NORMAL = new Vector3f[RINGS];

    static {
        for (int k = 0; k < RINGS; k++) {
            RADIUS[k] = 1.45F + 1.05F * k / (RINGS - 1);
            // Orbit poles spread evenly over the sphere: even steps in height, golden-angle steps round.
            double tilt = Math.acos(1 - 2 * (k + 0.5) / RINGS);
            double node = k * 2.39996;
            Vector3f n = new Vector3f((float) (Math.sin(tilt) * Math.cos(node)), (float) Math.cos(tilt), (float) (Math.sin(tilt) * Math.sin(node)));
            Vector3f a = new Vector3f(n).cross(Math.abs(n.y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0)).normalize();
            NORMAL[k] = n;
            ACROSS[k] = a;
            ALONG[k] = new Vector3f(n).cross(a).normalize();
        }
    }

    private SkySun() {}

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/sun/" + name);
    }

    /**
     * Draws it all, with {@code pose} at the sun's middle. {@code dim} is how much of the light is
     * cut off, which darkens the panels' backs along with the sky behind them.
     */
    public static void draw(MultiBufferSource buffers, PoseStack pose, float seconds, float coverage, float closing, float dim) {
        Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        PoseStack.Pose last = pose.last();
        TextureAtlasSprite white = atlas.apply(WHITE);
        float open = SunSwarmClient.open(closing);
        VertexConsumer solid = buffers.getBuffer(Sheets.cutoutBlockSheet());
        // The disc is drawn solid, so it hides what is behind it (vanilla's square sun, the far
        // orbits), then its light is added over it. Not as a beacon beam: shader packs fade those out
        // beyond the render distance, and the sun is always beyond it.
        if (open > 0.001F) {
            disc(solid, last, white, false);
        }
        panels(solid, last, seconds, coverage, closing, dim, atlas.apply(CELL), atlas.apply(BACK));
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS));
        if (open > 0.001F) {
            disc(glow, last, white, true);
        }
        corona(glow, last, open * (1 - 0.3F * coverage), white);
        float heat = SunSwarmClient.smooth(0.9F, 1, closing);
        if (heat > 0) {
            infrared(glow, last, heat, 0xFF8A1E0C, white);
        }
    }

    /** Where the shell caves in first, as directions from the sun's middle: the collapse spreads out from these. */
    private static final Vector3f[] DENTS = {
            new Vector3f(0.3F, -0.8F, 0.5F).normalize(), new Vector3f(-0.7F, 0.2F, -0.6F).normalize(), new Vector3f(0.6F, 0.5F, -0.4F).normalize()};

    /**
     * The shut shell crushing in, {@code t} from 0 to 1. It caves in from a few dents and spreads
     * from there, so it crumples rather than shrinks. Each panel falls inwards faster and faster, as
     * under gravity, keeping its size until it is well in; it tumbles, stretches along its fall like
     * a streak, and heats from the dull red of waste heat to white as it goes. A bright core builds
     * in the middle as everything condenses into it.
     */
    public static void collapse(MultiBufferSource buffers, PoseStack pose, float t, float dim) {
        Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        PoseStack.Pose last = pose.last();
        TextureAtlasSprite back = atlas.apply(BACK), white = atlas.apply(WHITE);
        VertexConsumer solid = buffers.getBuffer(Sheets.cutoutBlockSheet());
        int backColour = scale(0xFFA4ABB4, 1 - 0.85F * dim);
        Vector3f[] tile = new Vector3f[4], at = new Vector3f[4];
        // The panels are all in by the time the flash comes (0.62 of the way).
        float fallen = Math.min(1, t / 0.6F);
        float[] heat = new float[PANELS];
        Vector3f[][] placed = new Vector3f[PANELS][];
        for (int i = 0; i < PANELS; i++) {
            int band = i / ROUND, round = i % ROUND;
            double t0 = Math.PI * band / BANDS, t1 = Math.PI * (band + 1) / BANDS;
            double p0 = Math.PI * 2 * round / ROUND, p1 = Math.PI * 2 * (round + 1) / ROUND;
            tile[0] = onShell(t0, p0, SHELL);
            tile[1] = onShell(t1, p0, SHELL);
            tile[2] = onShell(t1, p1, SHELL);
            tile[3] = onShell(t0, p1, SHELL);
            Vector3f middle = new Vector3f(tile[0]).add(tile[1]).add(tile[2]).add(tile[3]).mul(0.25F);
            Vector3f out = new Vector3f(middle).normalize();
            // When this panel goes: soon near a dent, later far from one, with a little jitter.
            float nearest = 2;
            for (Vector3f dent : DENTS) {
                nearest = Math.min(nearest, 1 - out.dot(dent));
            }
            float start = 0.3F * nearest / 2 + 0.08F * hashed(i, 1);
            float u = SunSwarmClient.smooth(start, start + 0.55F, fallen);
            if (u >= 0.999F) {
                continue;
            }
            float fall = u * u;
            float size = 1 - SunSwarmClient.smooth(0.55F, 1, u);
            float stretch = 1 + 2.2F * u * (1 - u);
            Quaternionf tumble = new Quaternionf().rotateAxis(u * u * (2 + 5 * hashed(i, 2)),
                    new Vector3f(hashed(i, 3) - 0.5F, hashed(i, 4) - 0.5F, hashed(i, 5) - 0.5F).normalize());
            Vector3f centre = new Vector3f(middle).mul(1 - fall);
            for (int k = 0; k < 4; k++) {
                Vector3f offset = new Vector3f(tile[k]).sub(middle);
                float along = offset.dot(out);
                offset.sub(new Vector3f(out).mul(along)).mul(size).add(new Vector3f(out).mul(along * size * stretch));
                at[k] = tumble.transform(offset).add(centre);
            }
            placed[i] = new Vector3f[] {new Vector3f(at[0]), new Vector3f(at[1]), new Vector3f(at[2]), new Vector3f(at[3])};
            heat[i] = u;
            quad(solid, last, at, back, back, mix(backColour, 0xFF5A1408, SunSwarmClient.smooth(0, 0.4F, u)), band == 0);
        }
        // Their heat, added over them: dull red at first, white-hot as they reach the middle.
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS));
        int base = 0xFF8A1E0C;
        for (int i = 0; i < PANELS; i++) {
            if (placed[i] == null) {
                continue;
            }
            int colour = scale(mix(base, 0xFFFFE8B0, heat[i]), 0.35F + 0.65F * heat[i]);
            for (int flip = 0; flip < 2; flip++) {
                for (int v = 0; v < 4; v++) {
                    int k = flip == 0 ? v : 3 - v;
                    vertex(glow, last, placed[i][k], colour, white, k, LightTexture.FULL_BRIGHT, 0, -1, 0);
                }
            }
        }
        float core = SunSwarmClient.smooth(0.15F, 0.7F, t);
        if (core > 0) {
            ring(glow, last, new float[] {0, 0.12F + 0.25F * core, 0.5F + 0.9F * core},
                    new int[] {scale(0xFFFFFFFF, core), scale(0xFFFFC880, core * 0.6F), 0xFF000000}, white, 32, GLOW_LIFT * 3);
        }
    }

    /** A steady 0 to 1 for panel {@code i}, one per {@code salt}. */
    private static float hashed(int i, int salt) {
        double v = Math.sin(i * 12.9898 + salt * 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    /** The pop's shockwave: a thin bright ring racing out, {@code radius} across, fading with {@code strength}. */
    public static void shockwave(MultiBufferSource buffers, PoseStack pose, float strength, float radius) {
        TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
        ring(buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS)), pose.last(), new float[] {radius * 0.82F, radius * 0.96F, radius},
                new int[] {0xFF000000, scale(0xFFD8E8FF, strength), 0xFF000000}, white, 64, GLOW_LIFT * 2);
    }

    /** The collapse's flash: a white glow facing the ground, {@code radius} across. */
    public static void flash(MultiBufferSource buffers, PoseStack pose, float strength, float radius) {
        TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
        float[] radii = {0, radius * 0.25F, radius};
        int[] colours = {scale(0xFFFFFFFF, strength), scale(0xFFFFE0B0, strength * 0.7F), 0xFF000000};
        ring(buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS)), pose.last(), radii, colours, white, 48, GLOW_LIFT);
    }

    /**
     * The wormhole as a picture, for when a shader pack is on and the real one (a lensing pass over
     * the frame, WormholeRenderer) cannot run: the far side, painted the way the lensing shader paints
     * it (WormholeFarSide), turning slowly, with the photon ring round it. No bent sky round it, but
     * the same view through. Drawn as a beacon beam is, which shader packs light by itself rather
     * than by the sky. {@code open} grows it from nothing.
     */
    public static void wormholeFallback(MultiBufferSource buffers, PoseStack pose, float open, float seconds) {
        if (open <= 0) {
            return;
        }
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotation(seconds * 0.02F));
        VertexConsumer far = buffers.getBuffer(RenderType.beaconBeam(WormholeFarSide.texture(), false));
        PoseStack.Pose turned = pose.last();
        int steps = 64;
        for (int k = 0; k < steps; k++) {
            double a0 = Math.PI * 2 * k / steps, a1 = Math.PI * 2 * (k + 1) / steps;
            Vector3f[] pts = {new Vector3f(), onDisc(a0, open, 0), onDisc(a1, open, 0), new Vector3f()};
            for (int flip = 0; flip < 2; flip++) {
                for (int v = 0; v < 4; v++) {
                    Vector3f p = pts[flip == 0 ? v : 3 - v];
                    far.addVertex(turned, p.x, p.y, p.z)
                            .setColor(0xFFFFFFFF)
                            .setUv(0.5F + p.x / (2 * open), 0.5F + p.z / (2 * open))
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(LightTexture.FULL_BRIGHT)
                            .setNormal(turned, 0, -1, 0);
                }
            }
        }
        pose.popPose();
        TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
        ring(buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS)), pose.last(), new float[] {open * 0.96F, open * 1.02F, open * 1.12F},
                new int[] {0xFF000000, 0xFFC0DCFF, 0xFF000000}, white, 64, GLOW_LIFT);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    // ---------------------------------------------------------------- sun

    /**
     * How far the glows sit in front of the solid disc, towards the ground. In the same plane they
     * would fight it for depth and drop out in patches.
     */
    private static final float GLOW_LIFT = -0.03F;

    /** A point on the disc facing the ground (in the x-z plane at {@code y}), {@code r} out at {@code angle}. */
    private static Vector3f onDisc(double angle, float r, float y) {
        return new Vector3f((float) Math.cos(angle) * r, y, (float) Math.sin(angle) * r);
    }

    /** The disc, solid ({@code glow} false) or as light added in front of it. */
    private static void disc(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite white, boolean glow) {
        float[] radii = {0, 0.55F, 0.85F, 1};
        int[] colours = {0xFFFFFCF0, 0xFFFFF2CE, 0xFFFFDC96, 0xFFFFB866};
        ring(buffer, pose, radii, colours, white, 48, glow ? GLOW_LIFT : 0);
    }

    /** A soft glow round the disc, fading out to twice the sun's radius. */
    private static void corona(VertexConsumer buffer, PoseStack.Pose pose, float strength, TextureAtlasSprite white) {
        if (strength <= 0.001F) {
            return;
        }
        float[] radii = {0.95F, 1.15F, 1.5F, 2.3F};
        int[] colours = {scale(0xFFFFE6B0, strength), scale(0xFFE89A4A, strength * 0.6F), scale(0xFF6A3A12, strength * 0.3F), 0xFF000000};
        ring(buffer, pose, radii, colours, white, 48, GLOW_LIFT);
    }

    /** Rings of quads in the disc's plane between the given radii, shaded between the given colours, both ways round. */
    private static void ring(VertexConsumer buffer, PoseStack.Pose pose, float[] radii, int[] colours, TextureAtlasSprite white, int steps, float y) {
        for (int b = 0; b < radii.length - 1; b++) {
            for (int k = 0; k < steps; k++) {
                double a0 = Math.PI * 2 * k / steps, a1 = Math.PI * 2 * (k + 1) / steps;
                Vector3f[] pts = {onDisc(a0, radii[b], y), onDisc(a1, radii[b], y), onDisc(a1, radii[b + 1], y), onDisc(a0, radii[b + 1], y)};
                int[] cols = {colours[b], colours[b], colours[b + 1], colours[b + 1]};
                for (int flip = 0; flip < 2; flip++) {
                    for (int v = 0; v < 4; v++) {
                        int i = flip == 0 ? v : 3 - v;
                        vertex(buffer, pose, pts[i], cols[i], white, i, LightTexture.FULL_BRIGHT, 0, -1, 0);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- swarm and shell

    /** A point on the shell, {@code theta} round from the side facing the ground (the local -y). */
    private static Vector3f onShell(double theta, double phi, float r) {
        float st = (float) Math.sin(theta);
        return new Vector3f(st * (float) Math.cos(phi) * r, -(float) Math.cos(theta) * r, st * (float) Math.sin(phi) * r);
    }

    private static void panels(VertexConsumer buffer, PoseStack.Pose pose, float seconds, float coverage, float closing, float dim,
                               TextureAtlasSprite cell, TextureAtlasSprite back) {
        float shown = coverage * PANELS;
        // The backs darken with the sky, lit only by what light is left.
        int backColour = scale(0xFFA4ABB4, 1 - 0.85F * dim);
        float spiral = closing * closing * 6;
        Vector3f[] orbit = new Vector3f[4], tile = new Vector3f[4], at = new Vector3f[4];
        for (int t = 0; t < PANELS; t++) {
            // Launch order: spread over all the orbits rather than one after another.
            int rank = (t * 7919) % PANELS;
            float launched = Math.min(1, (shown - rank) / 24F);
            if (launched <= 0) {
                continue;
            }
            int band = t / ROUND, round = t % ROUND;
            double t0 = Math.PI * band / BANDS, t1 = Math.PI * (band + 1) / BANDS;
            double p0 = Math.PI * 2 * round / ROUND, p1 = Math.PI * 2 * (round + 1) / ROUND;
            float land = SunSwarmClient.landed((t0 + t1) / 2, closing);

            int ring = t % RINGS, slot = t / RINGS;
            float r = RADIUS[ring];
            float speed = ORBIT_SPEED * (float) Math.pow(RADIUS[0] / r, 1.5);
            double angle = Math.PI * 2 * slot / PER_RING + ring * 0.9 + seconds * speed + spiral;
            // Launching panels rise from the sun's surface to their orbit.
            float height = 1 + (r - 1) * SunSwarmClient.smooth(0, 1, launched);
            float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
            Vector3f centre = new Vector3f(ACROSS[ring]).mul(c * height).add(new Vector3f(ALONG[ring]).mul(s * height));
            Vector3f along = new Vector3f(ACROSS[ring]).mul(-s).add(new Vector3f(ALONG[ring]).mul(c));
            Vector3f axis = NORMAL[ring];
            float half = PANEL * launched;
            orbit[0] = corner(centre, along, axis, -half, -half);
            orbit[1] = corner(centre, along, axis, -half, half);
            orbit[2] = corner(centre, along, axis, half, half);
            orbit[3] = corner(centre, along, axis, half, -half);
            if (land > 0) {
                tile[0] = onShell(t0, p0, SHELL);
                tile[1] = onShell(t1, p0, SHELL);
                tile[2] = onShell(t1, p1, SHELL);
                tile[3] = onShell(t0, p1, SHELL);
                for (int k = 0; k < 4; k++) {
                    at[k] = new Vector3f(orbit[k]).lerp(tile[k], land);
                }
            } else {
                System.arraycopy(orbit, 0, at, 0, 4);
            }
            quad(buffer, pose, at, cell, back, backColour, band == 0);
        }
    }

    private static Vector3f corner(Vector3f centre, Vector3f along, Vector3f axis, float a, float b) {
        return new Vector3f(centre).add(new Vector3f(along).mul(a)).add(new Vector3f(axis).mul(b));
    }

    /**
     * One panel: its back facing out from the sun, its cells facing in. The panels round the pole
     * facing the ground are triangles (two corners meet at the pole), so their facing comes from the
     * diagonals, and they show only the plain middle of the texture: forty frames meeting in one
     * spot would shimmer.
     */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, Vector3f[] p, TextureAtlasSprite cell, TextureAtlasSprite back,
                             int backColour, boolean pole) {
        Vector3f normal = new Vector3f(p[2]).sub(p[0]).cross(new Vector3f(p[3]).sub(p[1]));
        Vector3f middle = new Vector3f(p[0]).add(p[1]).add(p[2]).add(p[3]).mul(0.25F);
        boolean outward = normal.dot(middle) > 0;
        normal.normalize();
        if (!outward) {
            normal.negate();
        }
        for (int side = 0; side < 2; side++) {
            boolean isBack = side == 0;
            TextureAtlasSprite sprite = isBack ? back : cell;
            int colour = isBack ? backColour : 0xFFFFFFFF;
            // Counter-clockwise seen from the side it faces.
            boolean forwards = isBack == outward;
            float nx = isBack ? normal.x : -normal.x, ny = isBack ? normal.y : -normal.y, nz = isBack ? normal.z : -normal.z;
            float inset = pole ? 0.35F : 0;
            float u0 = sprite.getU(inset), u1 = sprite.getU(1 - inset), v0 = sprite.getV(inset), v1 = sprite.getV(1 - inset);
            for (int v = 0; v < 4; v++) {
                int i = forwards ? v : 3 - v;
                buffer.addVertex(pose, p[i].x, p[i].y, p[i].z)
                        .setColor(colour)
                        .setUv(i == 0 || i == 1 ? u0 : u1, i == 0 || i == 3 ? v0 : v1)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, nx, ny, nz);
            }
        }
    }

    /** The shut shell's waste heat: a dull red glow over the side facing the ground, brightest face on. */
    private static void infrared(VertexConsumer buffer, PoseStack.Pose pose, float heat, int colour, TextureAtlasSprite white) {
        int lat = 8;
        float r = SHELL * 1.004F;
        for (int j = 0; j < lat; j++) {
            double t0 = Math.PI / 2 * j / lat, t1 = Math.PI / 2 * (j + 1) / lat;
            int c0 = scale(colour, heat * (float) Math.cos(t0)), c1 = scale(colour, heat * (float) Math.cos(t1));
            for (int i = 0; i < ROUND; i++) {
                double p0 = Math.PI * 2 * i / ROUND, p1 = Math.PI * 2 * (i + 1) / ROUND;
                Vector3f[] pts = {onShell(t0, p0, r), onShell(t1, p0, r), onShell(t1, p1, r), onShell(t0, p1, r)};
                int[] cols = {c0, c1, c1, c0};
                for (int flip = 0; flip < 2; flip++) {
                    for (int v = 0; v < 4; v++) {
                        int k = flip == 0 ? v : 3 - v;
                        vertex(buffer, pose, pts[k], cols[k], white, k, LightTexture.FULL_BRIGHT, 0, -1, 0);
                    }
                }
            }
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3f p, int colour, TextureAtlasSprite sprite, int corner,
                               int light, float nx, float ny, float nz) {
        buffer.addVertex(pose, p.x, p.y, p.z)
                .setColor(colour)
                .setUv(corner == 0 || corner == 3 ? sprite.getU0() : sprite.getU1(), corner < 2 ? sprite.getV0() : sprite.getV1())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static int scale(int argb, float f) {
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((argb & 0xFF) * f));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
