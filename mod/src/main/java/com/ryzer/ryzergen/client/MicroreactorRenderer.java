package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorSlot;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Draws a formed microreactor, the Lantern (art/tools/microreactor_concept.py, exported to
 * assets/ryzergen/microreactor/microreactor.json): a pool reactor in a glass jar, its plant behind it.
 * The body comes from a GPU mesh, as the fission station's does (or the plain draw while a shader pack
 * is on). Everything that moves or follows the reactor is drawn every frame:
 * <ul>
 * <li>the water in the jar, as high as the coolant tank is full, with bubbles rising off the core
 * (more in overdrive, boiling on a coolant loss);</li>
 * <li>the Cherenkov glow while the core runs under water: the water round it glowing blue, as a real
 * pool reactor's does, with a flash as it goes critical;</li>
 * <li>the core: fresh, spent, or glowing with its heat past 500°C (steel starts to glow at about
 * 525°C), as it does running dry or running away. Now and then while it runs it turns in its seat,
 * a sixth or a third of a turn, overshooting a little before it settles: fuel is turned so it burns
 * evenly (real reactors turn and shuffle assemblies between runs; here it happens on the go);</li>
 * <li>the control rods, drawn out while it runs and further in overdrive, dropped when it stops;</li>
 * <li>the light strips and the status strip: cyan running, amber in overdrive, flashing red on a
 * coolant loss, dark when stopped;</li>
 * <li>the generator's fan, turning with the reactor.</li>
 * </ul>
 * The design is drawn facing north from the lower front block, then turned to the machine's facing.
 */
public class MicroreactorRenderer implements BlockEntityRenderer<ReactorHeartBlockEntity> {
    private static final ResourceLocation DATA = rl("microreactor/microreactor.json");
    private static final ResourceLocation GLOW = rl("block/microreactor/cherenkov_glow");
    private static final ResourceLocation BUBBLE = rl("block/microreactor/bubble");
    private static final ResourceLocation WATER = rl("block/microreactor/water");

    // The design's numbers, in pixels (microreactor_concept.py).
    private static final float AXIS = 8;
    private static final float FLOOR = 8;
    private static final float FULL = 23.5F;
    private static final float WATER_R = 7.15F;
    private static final float CORE_Y = 8.6F;
    private static final float CORE_HEIGHT = 9.5F;
    private static final float RODS_RUNNING = 3.1F;
    private static final float RODS_OVERDRIVE = 7.6F;
    private static final float FAN_X = 8;
    private static final float FAN_Y = 24;
    /** How long the core takes to turn, and the quiet between turns while it runs (ticks). */
    private static final int TURN_TICKS = 30;
    private static final int TURN_GAP_MIN = 300;
    private static final int TURN_GAP_SPREAD = 500;

    private static final int CHERENKOV = 0x46AFFF;
    private static final int CYAN = 0x35C8F5;
    private static final int AMBER = 0xF0A030;
    private static final int RED = 0xE8402F;
    private static final int STANDBY = 0x2A8FB0;
    private static final int DARK = 0x1B5A73;
    private static final float[] UP = {0, 1, 0};

    public MicroreactorRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, path);
    }

    private static List<StationGeometry.Quad> group(String name) {
        return StationGeometry.group(DATA, name);
    }

    // ================================================================ animation, a tick at a time

    /** What the renderer shows, eased a tick at a time so it moves smoothly; kept on the heart. */
    private static final class Animation {
        float rods, rodsPrev;
        float water = -1, waterPrev;
        float glow, glowPrev;
        float heat, heatPrev;
        float fan, fanPrev, fanSpeed;
        /** The core's settled angle, and the turn under way (from, to, ticks into it; -1 when still). */
        float coreAngle, turnFrom, turnTo;
        int turnTicks = -1;
        int nextTurn = TURN_GAP_MIN;
        boolean wasRunning;
        MicroreactorSlot meshSlot;
    }

    private static Animation animation(ReactorHeartBlockEntity heart) {
        if (heart.clientAnimation instanceof Animation a) {
            return a;
        }
        Animation a = new Animation();
        heart.clientAnimation = a;
        return a;
    }

    /** Called from the heart's client ticker while the machine is formed. */
    public static void tick(ReactorHeartBlockEntity heart) {
        Animation a = animation(heart);
        Status status = heart.status();
        boolean running = status.running();
        boolean overdrive = status.overdrive();
        a.rodsPrev = a.rods;
        a.waterPrev = a.water;
        a.glowPrev = a.glow;
        a.heatPrev = a.heat;
        a.fanPrev = a.fan;

        // The rods come out slowly while it runs, further in overdrive, and drop fast when it stops.
        float rods = !running ? 0 : overdrive ? RODS_OVERDRIVE : RODS_RUNNING;
        a.rods = rods > a.rods ? Math.min(rods, a.rods + 0.15F) : Math.max(rods, a.rods - 1.2F);
        // The water follows the tank (from where it stands at first sight).
        float water = heart.clientWater();
        if (a.water < 0) {
            a.water = a.waterPrev = water;
        }
        a.water += (water - a.water) * 0.15F;

        // The water glows while a core runs under it, with a flash as it goes critical.
        float glow = running && heart.clientFuel() != 0 && water > 0 ? (overdrive ? 1.0F : 0.7F) : 0;
        if (running && !a.wasRunning && glow > 0) {
            a.glow = 1.5F;
        }
        a.wasRunning = running;
        a.glow += (glow - a.glow) * (a.glow > glow ? 0.06F : 0.05F);
        // The core's own heat: from 500°C (dull red) to 1000°C (white hot).
        a.heat += (Mth.clamp((heart.temperature() - 500) / 500F, 0, 1) - a.heat) * 0.1F;
        // Now and then while it runs, the core turns a sixth or a third of the way round, either way.
        if (a.turnTicks >= 0) {
            if (++a.turnTicks >= TURN_TICKS) {
                a.coreAngle = a.turnTo % 360;
                a.turnTicks = -1;
                a.nextTurn = TURN_GAP_MIN + RANDOM.nextInt(TURN_GAP_SPREAD);
            }
        } else if (running && heart.clientFuel() != 0 && --a.nextTurn <= 0) {
            a.turnFrom = a.coreAngle;
            a.turnTo = a.coreAngle + (RANDOM.nextBoolean() ? 60 : 120) * (RANDOM.nextBoolean() ? 1 : -1);
            a.turnTicks = 0;
        }
        // The fan winds up and down with the reactor.
        float speed = running ? (overdrive ? 30 : 20) : 0;
        a.fanSpeed += Mth.clamp(speed - a.fanSpeed, -0.4F, 0.4F);
        a.fan += a.fanSpeed;
        if (a.fan > 3600) {
            a.fan -= 3600;
            a.fanPrev -= 3600;
        }
    }

    private static final java.util.Random RANDOM = new java.util.Random();

    /** The core's angle now: during a turn it swings past the new position a little and settles back. */
    private static float coreAngle(Animation a, float partialTick) {
        if (a.turnTicks < 0) {
            return a.coreAngle;
        }
        float t = Mth.clamp((a.turnTicks + partialTick) / TURN_TICKS, 0, 1);
        // Ease out with an overshoot (about a tenth of the turn past, then back).
        float c1 = 1.9F, c3 = c1 + 1, u = t - 1;
        float f = 1 + c3 * u * u * u + c1 * u * u;
        return a.turnFrom + (a.turnTo - a.turnFrom) * f;
    }

    // ================================================================ drawing

    @Override
    public void render(ReactorHeartBlockEntity heart, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = heart.getBlockState();
        MicroreactorSlot slot = state.getValue(MicroreactorPartBlock.SLOT);
        if (!slot.isFormed() || heart.getLevel() == null) {
            return;
        }
        Direction facing = state.getValue(MicroreactorPartBlock.FACING);
        Animation a = animation(heart);
        boolean upper = slot == MicroreactorSlot.UPPER_FRONT || slot == MicroreactorSlot.UPPER_BACK;
        light = StationRenderer.openLight(heart, upper ? 1 : 2, light);

        // The body: from the GPU mesh when we can, otherwise sent like everything else.
        List<StationGeometry.Quad> body = group("static");
        if (!StationMesh.shadersInUse()) {
            StationMesh mesh = heart.clientMesh instanceof StationMesh cached && cached.matches(light, facing) && a.meshSlot == slot
                    ? cached : null;
            if (mesh == null) {
                if (heart.clientMesh instanceof StationMesh old) {
                    old.close();
                }
                PoseStack local = new PoseStack();
                place(local, slot, facing);
                mesh = StationMesh.build(body, local, light, facing);
                heart.clientMesh = mesh;
                a.meshSlot = slot;
            }
            mesh.draw(pose.last().pose());
        }

        long time = heart.getLevel().getGameTime();
        Status status = heart.status();
        VertexConsumer solid = buffers.getBuffer(Sheets.cutoutBlockSheet());
        pose.pushPose();
        place(pose, slot, facing);
        if (StationMesh.shadersInUse()) {
            StationRenderer.draw(solid, pose, body, light);
        }

        // Light strips and the status strip, in the state's colour.
        int lights = stripColour(status);
        boolean lit = stripLit(status, time);
        drawAs(solid, pose, group("strips"), lit ? LightTexture.FULL_BRIGHT : light, lights);
        drawAs(solid, pose, group("status"), lit ? LightTexture.FULL_BRIGHT : light, lights);

        // The control rods, drawn out as far as the reactor has them.
        float rods = Mth.lerp(partialTick, a.rodsPrev, a.rods);
        pose.pushPose();
        pose.translate(0, rods / 16, 0);
        StationRenderer.draw(solid, pose, group("rods"), light);
        pose.popPose();

        // The core on the jar's floor: fresh, spent, or glowing with its heat.
        int coreKind = heart.clientFuel();
        float heat = Mth.lerp(partialTick, a.heatPrev, a.heat);
        if (coreKind > 0) {
            pose.pushPose();
            pose.translate(AXIS / 16, 0, AXIS / 16);
            pose.mulPose(Axis.YP.rotationDegrees(coreAngle(a, partialTick)));
            pose.translate(-AXIS / 16, 0, -AXIS / 16);
            StationRenderer.draw(solid, pose, group("core_frame"), light);
            if (heat > 0.05F) {
                drawAs(solid, pose, group("core_rods_hot"), LightTexture.FULL_BRIGHT, heatColour(heat));
            } else {
                StationRenderer.draw(solid, pose, group(coreKind == 1 ? "core_rods" : "core_rods_spent"), light);
            }
            pose.popPose();
        }

        // The generator's fan.
        pose.pushPose();
        pose.translate(FAN_X / 16, FAN_Y / 16, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, a.fanPrev, a.fan)));
        pose.translate(-FAN_X / 16, -FAN_Y / 16, 0);
        StationRenderer.draw(solid, pose, group("fan"), light);
        pose.popPose();

        // The see-through layers, sorted together: the water, its glow and bubbles, the glass.
        VertexConsumer clear = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        float water = Mth.lerp(partialTick, a.waterPrev, Math.max(0, a.water));
        float level = FLOOR + (FULL - FLOOR) * Mth.clamp(water, 0, 1);
        if (level > FLOOR + 0.3F) {
            water(clear, pose, level, light);
            float glow = Mth.lerp(partialTick, a.glowPrev, a.glow);
            if (glow > 0.02F) {
                glow(clear, pose, glow, level);
            }
            bubbles(clear, pose, status, coreKind > 0, level, time + partialTick, light);
        }
        StationRenderer.draw(clear, pose, group("glass_inner"), light);
        StationRenderer.draw(clear, pose, group("glass"), light);
        pose.popPose();
    }

    /** From the heart's block to the lower front block, then turned about its middle to the facing. */
    private static void place(PoseStack pose, MicroreactorSlot slot, Direction facing) {
        BlockPos origin = slot.toOrigin(BlockPos.ZERO, facing);
        pose.translate(origin.getX(), origin.getY(), origin.getZ());
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
    }

    private static int stripColour(Status status) {
        return 0xFF000000 | switch (status) {
            case ONLINE, DUMPING -> CYAN;
            case OVERDRIVE -> AMBER;
            case COOLANT_LOSS -> RED;
            case STANDBY -> STANDBY;
            default -> DARK;
        };
    }

    /** Lit while it runs or stands by; a coolant loss flashes. */
    private static boolean stripLit(Status status, long time) {
        return switch (status) {
            case ONLINE, DUMPING, OVERDRIVE, STANDBY -> true;
            case COOLANT_LOSS -> (time / 5) % 2 == 0;
            default -> false;
        };
    }

    /** Dull red at 500°C, orange by about 800°C, nearly white at 1000°C. */
    private static int heatColour(float heat) {
        int r, g, b;
        if (heat < 0.6F) {
            float t = heat / 0.6F;
            r = Mth.lerpInt(t, 0x7A, 0xFF);
            g = Mth.lerpInt(t, 0x1A, 0x8A);
            b = Mth.lerpInt(t, 0x0A, 0x3C);
        } else {
            float t = (heat - 0.6F) / 0.4F;
            r = 0xFF;
            g = Mth.lerpInt(t, 0x8A, 0xE6);
            b = Mth.lerpInt(t, 0x3C, 0xB0);
        }
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Draws quads with one light and tint for all of them (lit faces shaded flat, as the station's are). */
    private static void drawAs(VertexConsumer buffer, PoseStack pose, List<StationGeometry.Quad> quads, int light, int colour) {
        PoseStack.Pose last = pose.last();
        boolean flat = light == LightTexture.FULL_BRIGHT;
        for (StationGeometry.Quad quad : quads) {
            float[] p = quad.xyz();
            float[] uv = quad.uv();
            float[] n = flat ? UP : quad.normal();
            for (int i = 0; i < 4; i++) {
                buffer.addVertex(last, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                        .setColor(colour)
                        .setUv(uv[i * 2], uv[i * 2 + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(last, n[0], n[1], n[2]);
            }
        }
    }

    // ---------------------------------------------------------------- the water and what is in it

    private static final int SIDES = 24;

    /**
     * The water: a column the jar's width up to {@code level} (pixels), in its own moving deep-blue
     * texture (block/microreactor/water: its colour and see-through are in the texture, so it reads
     * the same with a shader pack). Its near side and the far side's inside are both drawn, so the
     * core is seen through one wall of water and what is behind it through two.
     */
    private static void water(VertexConsumer buffer, PoseStack pose, float level, int light) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER);
        int colour = 0xFFFFFFFF;
        cylinder(buffer, pose, sprite, WATER_R, FLOOR, level, colour, light, false, true);
        // The surface.
        PoseStack.Pose last = pose.last();
        for (int k = 0; k < SIDES; k++) {
            double p0 = 2 * Math.PI * k / SIDES;
            double p1 = 2 * Math.PI * (k + 1) / SIDES;
            float[][] pts = {
                    {AXIS, level, AXIS},
                    {AXIS + WATER_R * (float) Math.cos(p0), level, AXIS + WATER_R * (float) Math.sin(p0)},
                    {AXIS + WATER_R * (float) Math.cos(p1), level, AXIS + WATER_R * (float) Math.sin(p1)},
                    {AXIS, level, AXIS},
            };
            float[][] uv = new float[4][];
            for (int i = 0; i < 4; i++) {
                uv[i] = new float[] {sprite.getU(pts[i][0] / 16), sprite.getV(pts[i][2] / 16)};
            }
            quad(buffer, last, pts, uv, UP, colour, light);
        }
    }

    /**
     * The Cherenkov glow: two shells of light close round the core, brightest low down and fading up
     * (the texture fades), stopping at the surface. Drawn lit and see-through with their insides too,
     * so the glow reads from every side.
     */
    private static void glow(VertexConsumer buffer, PoseStack pose, float strength, float level) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(GLOW);
        float[][] shells = {{3.7F, CORE_Y + 0.5F, CORE_Y + 10, 1.0F}, {5.0F, CORE_Y + 0.2F, CORE_Y + 12, 0.55F}};
        for (float[] shell : shells) {
            float top = Math.min(shell[2], level - 0.2F);
            if (top <= shell[1] + 0.5F) {
                continue;
            }
            int alpha = Math.round(Mth.clamp(strength * shell[3] * 0.6F, 0, 1) * 255);
            cylinder(buffer, pose, sprite, shell[0], shell[1], top, alpha << 24 | CHERENKOV, LightTexture.FULL_BRIGHT, true, true);
        }
    }

    /**
     * Bubbles rising off the core to the surface: a few while it runs, more in overdrive, and a
     * boil from the floor on a coolant loss.
     */
    private static void bubbles(VertexConsumer buffer, PoseStack pose, Status status, boolean core, float level, float time, int light) {
        int count = status == Status.COOLANT_LOSS ? 16 : status == Status.OVERDRIVE ? 14 : status.running() ? 6 : 0;
        float from = status == Status.COOLANT_LOSS || !core ? FLOOR + 1 : CORE_Y + CORE_HEIGHT - 0.5F;
        if (count == 0 || from >= level - 1) {
            return;
        }
        for (int i = 0; i < count; i++) {
            float period = 36 + (i % 5) * 6;
            float phase = ((time + i * 17.3F) % period) / period;
            float y = from + phase * (level - from - 0.5F);
            float r = 1 + ((i * 0.618F) % 1) * 4.4F;
            float angle = i * 2.39996F + 0.3F * Mth.sin(time * 0.05F + i);
            float x = AXIS + r * Mth.cos(angle);
            float z = AXIS + r * Mth.sin(angle);
            float s = 0.18F + 0.05F * (i % 3);
            StationRenderer.draw(buffer, pose, StationGeometry.box(x - s, y - s, z - s, x + s, y + s, z + s, BUBBLE, false), light);
        }
    }

    /**
     * The side of an upright cylinder round the jar's axis, radius {@code r}, from {@code y1} to
     * {@code y2} (pixels), each panel taking its share of the texture across. {@code outside} and
     * {@code inside} pick which ways it faces.
     */
    private static void cylinder(VertexConsumer buffer, PoseStack pose, TextureAtlasSprite sprite, float r, float y1, float y2,
                                 int colour, int light, boolean outside, boolean inside) {
        PoseStack.Pose last = pose.last();
        float width = (float) (2 * Math.PI * r / SIDES);
        float height = Math.min(16, y2 - y1);
        for (int k = 0; k < SIDES; k++) {
            double p0 = 2 * Math.PI * k / SIDES;
            double p1 = 2 * Math.PI * (k + 1) / SIDES;
            double pm = (p0 + p1) / 2;
            float c0 = (float) Math.cos(p0), s0 = (float) Math.sin(p0), c1 = (float) Math.cos(p1), s1 = (float) Math.sin(p1);
            float[][] pts = {
                    {AXIS + r * c0, y2, AXIS + r * s0},
                    {AXIS + r * c0, y1, AXIS + r * s0},
                    {AXIS + r * c1, y1, AXIS + r * s1},
                    {AXIS + r * c1, y2, AXIS + r * s1},
            };
            float u0 = (k * width) % 16;
            if (u0 + width > 16) {
                u0 = 16 - width;
            }
            float[][] uv = {
                    {sprite.getU(u0 / 16), sprite.getV((16 - height) / 16)},
                    {sprite.getU(u0 / 16), sprite.getV(1)},
                    {sprite.getU((u0 + width) / 16), sprite.getV(1)},
                    {sprite.getU((u0 + width) / 16), sprite.getV((16 - height) / 16)},
            };
            float[] out = {(float) Math.cos(pm), 0, (float) Math.sin(pm)};
            if (inside) {
                quad(buffer, last, pts, uv, new float[] {-out[0], 0, -out[2]}, colour, light);
            }
            if (outside) {
                quad(buffer, last, pts, uv, out, colour, light);
            }
        }
    }

    /**
     * One face from corners in pixels, wound so it faces {@code facing} (the back is culled). Lit
     * faces get an upward normal, so the shader shades them flat.
     */
    private static void quad(VertexConsumer buffer, PoseStack.Pose last, float[][] pts, float[][] uv, float[] facing, int colour, int light) {
        // Newell's method, so a face with two corners together (a triangle) still finds its way.
        float nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < 4; i++) {
            float[] p = pts[i], q = pts[(i + 1) % 4];
            nx += (p[1] - q[1]) * (p[2] + q[2]);
            ny += (p[2] - q[2]) * (p[0] + q[0]);
            nz += (p[0] - q[0]) * (p[1] + q[1]);
        }
        boolean flip = nx * facing[0] + ny * facing[1] + nz * facing[2] < 0;
        float[] n = light == LightTexture.FULL_BRIGHT ? UP : facing;
        for (int j = 0; j < 4; j++) {
            int i = flip ? 3 - j : j;
            buffer.addVertex(last, pts[i][0] / 16, pts[i][1] / 16, pts[i][2] / 16)
                    .setColor(colour)
                    .setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(last, n[0], n[1], n[2]);
        }
    }

    @Override
    public AABB getRenderBoundingBox(ReactorHeartBlockEntity heart) {
        BlockState state = heart.getBlockState();
        MicroreactorSlot slot = state.getValue(MicroreactorPartBlock.SLOT);
        if (!slot.isFormed()) {
            return new AABB(heart.getBlockPos());
        }
        Direction facing = state.getValue(MicroreactorPartBlock.FACING);
        BlockPos origin = slot.toOrigin(heart.getBlockPos(), facing);
        BlockPos far = MicroreactorSlot.UPPER_BACK.fromOrigin(origin, facing);
        return new AABB(origin).minmax(new AABB(far)).inflate(1);
    }

    @Override
    public boolean shouldRenderOffScreen(ReactorHeartBlockEntity heart) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
