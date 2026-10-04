package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.machine.drill.MegaDrillPreviewBlock;
import com.ryzer.ryzergen.machine.drill.MegaDrillPreviewBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * Draws the mega drill's concept design (art/tools/mega_drill_concept.py) round its preview block.
 * The body goes the fission station's way: a GPU mesh built once, or the plain draw while a shader
 * pack is on. What changes when it runs (the melt, the beam, the lamps, the light lines) is drawn
 * every frame from the design's 'on' or 'off' group, by the block's redstone signal. The block sits
 * in the middle of the pit.
 */
public class MegaDrillPreviewRenderer implements BlockEntityRenderer<MegaDrillPreviewBlockEntity> {
    /** Half the design's footprint, in blocks (it is 9 across, centred on the middle block). */
    private static final float HALF = 4.5F;
    private static final float HEIGHT = 16;

    public MegaDrillPreviewRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MegaDrillPreviewBlockEntity preview, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = preview.getBlockState().getValue(MegaDrillPreviewBlock.FACING);
        boolean running = preview.getBlockState().getValue(MegaDrillPreviewBlock.POWERED);
        var body = StationGeometry.group(StationGeometry.MEGA_DRILL, "static");
        if (!StationMesh.shadersInUse()) {
            StationMesh mesh = preview.clientMesh instanceof StationMesh cached && cached.matches(light, facing) ? cached : null;
            if (mesh == null) {
                if (preview.clientMesh instanceof StationMesh old) {
                    old.close();
                }
                PoseStack local = new PoseStack();
                place(local, facing);
                mesh = StationMesh.build(body, local, light, facing);
                preview.clientMesh = mesh;
            }
            mesh.draw(pose.last().pose());
        }
        pose.pushPose();
        place(pose, facing);
        var cutout = buffers.getBuffer(Sheets.cutoutBlockSheet());
        if (StationMesh.shadersInUse()) {
            StationRenderer.draw(cutout, pose, body, light);
        }
        StationRenderer.draw(cutout, pose, StationGeometry.group(StationGeometry.MEGA_DRILL, running ? "on" : "off"), light);
        pose.popPose();
    }

    /** The design faces north (the ladder at its low z); turn it about the block to the block's facing. */
    private static void place(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-HALF, 0, -HALF);
    }

    @Override
    public AABB getRenderBoundingBox(MegaDrillPreviewBlockEntity preview) {
        var pos = preview.getBlockPos();
        return new AABB(pos.getX() + 0.5 - HALF, pos.getY(), pos.getZ() + 0.5 - HALF,
                pos.getX() + 0.5 + HALF, pos.getY() + HEIGHT, pos.getZ() + 0.5 + HALF);
    }

    @Override
    public boolean shouldRenderOffScreen(MegaDrillPreviewBlockEntity preview) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
