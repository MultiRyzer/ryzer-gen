package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.machine.sun.SunGatePreviewBlock;
import com.ryzer.ryzergen.machine.sun.SunGatePreviewBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws the sun gate's concept design (art/tools/sun_gate_concept.py) round its preview block. The
 * platform and arms go the fusion preview's way: a GPU mesh built once, or the plain draw while a
 * shader pack is on. Every frame it adds the sun and its swarm ({@link SunGateSun}), at the
 * coverage set on the block. The block sits in the middle of the design's footprint.
 */
public class SunGateRenderer implements BlockEntityRenderer<SunGatePreviewBlockEntity> {
    /** Half the design's footprint, in blocks (it is 11 across, centred on the middle block). */
    private static final float HALF = 5.5F;
    private static final float HEIGHT = 11;

    public SunGateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SunGatePreviewBlockEntity gate, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = gate.getBlockState().getValue(SunGatePreviewBlock.FACING);
        var body = StationGeometry.group(StationGeometry.SUN_GATE, "static");
        if (!StationMesh.shadersInUse()) {
            StationMesh mesh = gate.clientMesh instanceof StationMesh cached && cached.matches(light, facing) ? cached : null;
            if (mesh == null) {
                if (gate.clientMesh instanceof StationMesh old) {
                    old.close();
                }
                PoseStack local = new PoseStack();
                place(local, facing);
                mesh = StationMesh.build(body, local, light, facing);
                gate.clientMesh = mesh;
            }
            mesh.draw(pose.last().pose());
        } else {
            pose.pushPose();
            place(pose, facing);
            StationRenderer.draw(buffers.getBuffer(Sheets.cutoutBlockSheet()), pose, body, light);
            pose.popPose();
        }
        if (gate.getLevel() == null) {
            return;
        }
        float seconds = (gate.getLevel().getGameTime() % 72000 + partialTick) / 20F;
        float coverage = gate.getBlockState().getValue(SunGatePreviewBlock.COVERAGE) / 10F;
        var pos = gate.getBlockPos();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vector3f eye = new Vector3f((float) (camera.x - pos.getX() - 0.5), (float) (camera.y - pos.getY() - SunGateSun.Y),
                (float) (camera.z - pos.getZ() - 0.5));
        pose.pushPose();
        pose.translate(0.5, SunGateSun.Y, 0.5);
        SunGateSun.draw(buffers, pose, eye, seconds, coverage, light);
        pose.popPose();
    }

    /** The design faces north (the console at its low z); turn it about the block to the block's facing. */
    private static void place(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-HALF, 0, -HALF);
    }

    @Override
    public AABB getRenderBoundingBox(SunGatePreviewBlockEntity gate) {
        var pos = gate.getBlockPos();
        return new AABB(pos.getX() + 0.5 - HALF, pos.getY(), pos.getZ() + 0.5 - HALF,
                pos.getX() + 0.5 + HALF, pos.getY() + HEIGHT, pos.getZ() + 0.5 + HALF);
    }

    @Override
    public boolean shouldRenderOffScreen(SunGatePreviewBlockEntity gate) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
