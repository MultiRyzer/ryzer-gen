package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.fission.StationLayout;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.machine.fission.StationRunner;
import net.minecraft.resources.ResourceLocation;
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
        draw(buffer, pose, rods(core), light);
        pose.pushPose();
        float angle = core.rotorAngle(partialTick);
        pose.translate(HALF, 0, HALF);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-HALF, 0, -HALF);
        draw(buffer, pose, StationGeometry.group("rotor"), light);
        pose.popPose();
        pose.popPose();
    }

    /** From the core's corner to the design's north-west corner, then turned about the centre to the facing. */
    private static void place(PoseStack pose, Direction facing) {
        BlockPos coreCell = StationLayout.turn(StationLayout.CORE, facing);
        pose.translate(-coreCell.getX() + HALF, 0, -coreCell.getZ() + HALF);
        pose.mulPose(Axis.YP.rotationDegrees(StationLayout.yRotation(facing)));
        pose.translate(-HALF, 0, -HALF);
    }

    private static final ResourceLocation GLOW = texture("glow");
    private static final ResourceLocation GLOW_OFF = texture("glow_off");
    private static final ResourceLocation DARK = texture("steel_dark");
    private static final ResourceLocation STEEL = texture("steel");
    private static final ResourceLocation COPPER = texture("copper");
    private static final ResourceLocation LEAD = texture("lead");

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/microreactor/" + name);
    }

    /**
     * The core's 25 channels as the player has loaded them, where the concept design puts them:
     * columns 20 pixels apart round the centre, standing on the pedestal up to the reactor head.
     * Coolant channels are copper pipes; fuel glows while the station runs (dim when stopped, dark
     * once spent); moderators are graphite, control rods steel, lithium target rods gunmetal. Empty channels show just a collar.
     */
    private static List<StationGeometry.Quad> rods(StationCoreBlockEntity core) {
        List<StationGeometry.Quad> quads = new java.util.ArrayList<>();
        StationRunner runner = core.runner();
        boolean running = core.isRunning();
        for (int i = 0; i < StationReactor.CHANNELS; i++) {
            float x = HALF * 16 + (i % StationReactor.GRID - 2) * 20;
            float z = HALF * 16 + (i / StationReactor.GRID - 2) * 20;
            quads.addAll(StationGeometry.box(x - 5, 24, z - 5, x + 5, 27, z + 5, DARK, false));
            ItemStack item = runner.item(i);
            ResourceLocation texture = switch (runner.type(i)) {
                case COOLANT -> COPPER;
                case FUEL -> StationReactor.isFreshFuel(item) ? (running ? GLOW : GLOW_OFF) : StationReactor.isSpent(item) ? DARK : null;
                case MODERATOR -> item.isEmpty() ? null : DARK;
                case CONTROL -> item.isEmpty() ? null : STEEL;
                case TARGET -> item.isEmpty() ? null : LEAD;
                case EMPTY -> null;
            };
            if (texture != null) {
                quads.addAll(StationGeometry.box(x - 4, 27, z - 4, x + 4, 80, z + 4, texture, texture == GLOW));
            }
        }
        return quads;
    }

    static void draw(VertexConsumer buffer, PoseStack pose, List<StationGeometry.Quad> quads, int light) {
        PoseStack.Pose last = pose.last();
        for (StationGeometry.Quad quad : quads) {
            int lit = quad.emissive() ? LightTexture.FULL_BRIGHT : light;
            float[] p = quad.xyz();
            float[] uv = quad.uv();
            // The entity shader shades each face by its normal, which would dim the sides of a
            // light strip to half. Emissive faces point their normal up, where that shading is full.
            Direction normal = quad.emissive() ? Direction.UP : quad.face();
            float nx = normal.getStepX(), ny = normal.getStepY(), nz = normal.getStepZ();
            for (int i = 0; i < 4; i++) {
                buffer.addVertex(last, p[i * 3], p[i * 3 + 1], p[i * 3 + 2])
                        .setColor(0xFFFFFFFF)
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
