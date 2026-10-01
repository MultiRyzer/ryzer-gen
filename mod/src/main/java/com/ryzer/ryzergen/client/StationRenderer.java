package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.fission.StationLayout;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.machine.fission.StationRunner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Draws a formed fission station from {@link StationGeometry}: the static body (from a GPU mesh,
 * see {@link StationMesh}), the rods, and the rotor turning about the station's centre. The geometry is drawn facing north from the station's
 * north-west corner, then turned about the footprint's centre to the core's facing.
 */
public class StationRenderer implements BlockEntityRenderer<StationCoreBlockEntity> {
    private static final float HALF = StationLayout.SIZE / 2F;

    public StationRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StationCoreBlockEntity core, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!core.isFormed()) {
            return;
        }
        light = openLight(core, StationLayout.HEIGHT + 1, light);
        Direction facing = core.facing();
        // The static body: from the GPU mesh when we can (built once, redrawn each frame in one
        // call), otherwise sent like everything else.
        if (!StationMesh.shadersInUse()) {
            StationMesh mesh = core.clientMesh instanceof StationMesh cached && cached.matches(light, facing) ? cached : null;
            if (mesh == null) {
                if (core.clientMesh instanceof StationMesh old) {
                    old.close();
                }
                PoseStack local = new PoseStack();
                place(local, facing);
                mesh = StationMesh.build(StationGeometry.group("static"), local, light, facing);
                core.clientMesh = mesh;
            }
            mesh.draw(pose.last().pose());
        }
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        pose.pushPose();
        place(pose, facing);
        if (StationMesh.shadersInUse()) {
            draw(buffer, pose, StationGeometry.group("static"), light);
        }
        draw(buffer, pose, rods(core, partialTick), light);
        pose.pushPose();
        float angle = core.rotorAngle(partialTick);
        pose.translate(HALF, 0, HALF);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-HALF, 0, -HALF);
        draw(buffer, pose, StationGeometry.group("rotor"), light);
        pose.popPose();
        // Steam, then the chamber glass last, both translucent, so they tint what is behind them rather
        // than cutting it out. The steam fades in with the turbine and is gone when the station stops.
        VertexConsumer translucent = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        int steam = Math.round(Math.min(1, core.spin() * 1.2F) * 255);
        if (steam > 4) {
            int colour = steam << 24 | 0xFFFFFF;
            draw(translucent, pose, StationGeometry.group("steam"), light, colour);
            draw(translucent, pose, plumes(core), light, colour);
        }
        draw(translucent, pose, StationGeometry.group("glass"), light);
        pose.popPose();
    }

    /** From the core's corner to the design's north-west corner, then turned about the centre to the facing. */
    private static void place(PoseStack pose, Direction facing) {
        BlockPos coreCell = StationLayout.turn(StationLayout.CORE, facing);
        pose.translate(-coreCell.getX() + HALF, 0, -coreCell.getZ() + HALF);
        pose.mulPose(Axis.YP.rotationDegrees(StationLayout.yRotation(facing)));
        pose.translate(-HALF, 0, -HALF);
    }

    private static final ResourceLocation DARK = texture("microreactor/steel_dark");
    private static final ResourceLocation COPPER = texture("microreactor/copper");
    private static final ResourceLocation LEAD = texture("microreactor/lead");
    private static final ResourceLocation FUEL = texture("station/fuel_pin");
    private static final ResourceLocation FUEL_OFF = texture("station/fuel_pin_off");
    private static final ResourceLocation SPACER = texture("station/spacer");
    private static final ResourceLocation NOZZLE = texture("station/nozzle");
    private static final ResourceLocation CHERENKOV = texture("station/cherenkov");
    private static final ResourceLocation MODERATOR = texture("station/moderator");
    private static final ResourceLocation CONTROL = texture("station/control");
    private static final ResourceLocation STEAM = texture("station/steam");

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/" + name);
    }

    private static float channelX(int i) {
        return HALF * 16 + (i % StationReactor.GRID - 2) * 20;
    }

    private static float channelZ(int i) {
        return HALF * 16 + (i / StationReactor.GRID - 2) * 20;
    }

    /**
     * The core's 25 channels as the player has loaded them, where the concept design puts them
     * (fission_concept.py): columns 20 pixels apart round the centre, standing on the pedestal up to
     * the reactor head. Each kind has its own look, so a layout reads at a glance:
     * <ul>
     * <li>fuel: a bundle of four pins held by spacer grids, as real fuel assemblies are built. The
     * pins glow green while the station runs, dim when it stops, and go dark once spent.</li>
     * <li>coolant: a column of water in copper hoops, glowing Cherenkov blue while the station runs
     * (the real glow of a reactor's water).</li>
     * <li>moderator: a graphite column with bore holes; control: a steel rod on a drive shaft, part
     * way in; lithium target: lead.</li>
     * </ul>
     * Empty channels show just a collar.
     */
    private static List<StationGeometry.Quad> rods(StationCoreBlockEntity core, float partialTick) {
        List<StationGeometry.Quad> quads = new java.util.ArrayList<>();
        StationRunner runner = core.runner();
        boolean running = core.isRunning();
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            float x = channelX(i);
            float z = channelZ(i);
            quads.addAll(StationGeometry.box(x - 5, 24, z - 5, x + 5, 27, z + 5, DARK, false));
            ItemStack item = runner.item(i);
            // Rods stand fully up while the station runs and sink halfway (into the pedestal) when
            // it stops: the whole rod shifts down, all but the collar.
            List<StationGeometry.Quad> rod = new java.util.ArrayList<>();
            float shift = (core.lift(i, partialTick) - 1) * ROD_LENGTH;
            switch (runner.type(i)) {
                case FUEL -> {
                    boolean fresh = StationReactor.isFreshFuel(item);
                    if (!fresh && !StationReactor.isSpent(item)) {
                        break;
                    }
                    ResourceLocation pins = fresh ? (running ? FUEL : FUEL_OFF) : DARK;
                    rod.addAll(StationGeometry.box(x - 4.5F, 27 + shift, z - 4.5F, x + 4.5F, 30 + shift, z + 4.5F, NOZZLE, false));
                    for (float dx : new float[] {-3.5F, 0.5F}) {
                        for (float dz : new float[] {-3.5F, 0.5F}) {
                            rod.addAll(StationGeometry.box(x + dx, 30 + shift, z + dz, x + dx + 3, 76 + shift, z + dz + 3, pins,
                                    pins == FUEL));
                        }
                    }
                    for (int y : new int[] {41, 55, 69}) {
                        rod.addAll(StationGeometry.box(x - 4.5F, y + shift, z - 4.5F, x + 4.5F, y + 1.5F + shift, z + 4.5F, SPACER, false));
                    }
                    rod.addAll(StationGeometry.box(x - 4.5F, 76 + shift, z - 4.5F, x + 4.5F, 80 + shift, z + 4.5F, NOZZLE, false));
                }
                case COOLANT -> {
                    quads.addAll(StationGeometry.box(x - 2.5F, 27, z - 2.5F, x + 2.5F, 80, z + 2.5F, CHERENKOV, running));
                    for (int y = 29; y < 79; y += 8) {
                        quads.addAll(StationGeometry.box(x - 3.5F, y, z - 3.5F, x + 3.5F, y + 2, z + 3.5F, COPPER, false));
                    }
                }
                case MODERATOR -> {
                    if (!item.isEmpty()) {
                        quads.addAll(StationGeometry.box(x - 4, 27, z - 4, x + 4, 80, z + 4, MODERATOR, false));
                    }
                }
                case CONTROL -> {
                    if (!item.isEmpty()) {
                        rod.addAll(StationGeometry.box(x - 4.5F, 27 + shift, z - 4.5F, x + 4.5F, 31 + shift, z + 4.5F, NOZZLE, false));
                        rod.addAll(StationGeometry.box(x - 3, 44 + shift, z - 3, x + 3, 72 + shift, z + 3, CONTROL, false));
                        rod.addAll(StationGeometry.box(x - 1, 72 + shift, z - 1, x + 1, 80 + shift, z + 1, SPACER, false));
                    }
                }
                case TARGET -> {
                    if (!item.isEmpty()) {
                        rod.addAll(StationGeometry.box(x - 4, 27 + shift, z - 4, x + 4, 80 + shift, z + 4, LEAD, false));
                    }
                }
                case EMPTY -> {
                }
            }
            quads.addAll(rod);
        }
        return quads;
    }

    /** A rod's length from the pedestal to the reactor head, in pixels: half of it sinks when lowered. */
    private static final float ROD_LENGTH = 53;

    /**
     * Steam rising off each coolant channel: two crossed sheets of the animated steam texture, each
     * with a face both ways (like vanilla fire), from just above the pedestal to the head.
     */
    private static List<StationGeometry.Quad> plumes(StationCoreBlockEntity core) {
        List<StationGeometry.Quad> quads = new java.util.ArrayList<>();
        StationRunner runner = core.runner();
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(STEAM);
        float[] uv = {sprite.getU0(), sprite.getV0(), sprite.getU0(), sprite.getV1(), sprite.getU1(), sprite.getV1(),
                sprite.getU1(), sprite.getV0()};
        float[] uvBack = {uv[6], uv[7], uv[4], uv[5], uv[2], uv[3], uv[0], uv[1]};
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            if (runner.type(i) != StationReactor.Channel.COOLANT) {
                continue;
            }
            float x = channelX(i) / 16F;
            float z = channelZ(i) / 16F;
            float top = 80 / 16F, bottom = 30 / 16F, w = 7 / 16F;
            for (int axis = 0; axis < 2; axis++) {
                float sx = axis == 0 ? w : 0, sz = axis == 0 ? 0 : w;
                float[] front = {x - sx, top, z - sz, x - sx, bottom, z - sz, x + sx, bottom, z + sz, x + sx, top, z + sz};
                float[] back = {x + sx, top, z + sz, x + sx, bottom, z + sz, x - sx, bottom, z - sz, x - sx, top, z - sz};
                float[] normal = {sz / w, 0, -sx / w};
                quads.add(new StationGeometry.Quad(front, uv, normal, false));
                quads.add(new StationGeometry.Quad(back, uvBack, new float[] {-normal[0], 0, -normal[2]}, false));
            }
        }
        return quads;
    }

    private static final float[] UP = {0, 1, 0};

    /**
     * The light to draw a whole multiblock with: its own block light, and the sky light of the open
     * air just above it, {@code above} blocks over the footprint's middle. The core sits inside its
     * own structure, where sky light is low (and stays low in worlds lit before formed parts let
     * light through), and shader packs read that sky light to decide how much sun a surface gets.
     */
    static int openLight(net.minecraft.world.level.block.entity.BlockEntity core, int above, int light) {
        net.minecraft.world.level.Level level = core.getLevel();
        if (level == null) {
            return light;
        }
        int top = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, core.getBlockPos().above(above));
        return LightTexture.pack(Math.max(LightTexture.block(light), LightTexture.block(top)),
                Math.max(LightTexture.sky(light), LightTexture.sky(top)));
    }

    static void draw(VertexConsumer buffer, PoseStack pose, List<StationGeometry.Quad> quads, int light) {
        draw(buffer, pose, quads, light, 0xFFFFFFFF);
    }

    /** Draws quads tinted by {@code colour} (ARGB; its alpha fades translucent ones). */
    static void draw(VertexConsumer buffer, PoseStack pose, List<StationGeometry.Quad> quads, int light, int colour) {
        PoseStack.Pose last = pose.last();
        for (StationGeometry.Quad quad : quads) {
            int lit = quad.emissive() ? LightTexture.FULL_BRIGHT : light;
            float[] p = quad.xyz();
            float[] uv = quad.uv();
            // The entity shader shades each face by its normal, which would dim the sides of a
            // light strip to half. Emissive faces point their normal up, where that shading is full.
            float[] n = quad.emissive() ? UP : quad.normal();
            float nx = n[0], ny = n[1], nz = n[2];
            for (int i = 0; i < 4; i++) {
                buffer.addVertex(last, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                        .setColor(colour)
                        .setUv(uv[i * 2], uv[i * 2 + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(lit)
                        .setNormal(last, nx, ny, nz);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(StationCoreBlockEntity core) {
        BlockPos a = StationLayout.toWorld(core.getBlockPos(), core.facing(), BlockPos.ZERO);
        BlockPos b = StationLayout.toWorld(core.getBlockPos(), core.facing(),
                new BlockPos(StationLayout.SIZE - 1, StationLayout.HEIGHT, StationLayout.SIZE - 1));
        return new AABB(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ()).inflate(1);
    }

    @Override
    public boolean shouldRenderOffScreen(StationCoreBlockEntity core) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
