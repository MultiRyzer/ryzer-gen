package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.machine.fusion.FusionPreviewBlock;
import com.ryzer.ryzergen.machine.fusion.FusionPreviewBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;

/**
 * Draws the fusion reactor's concept design (art/tools/tokamak_concept.py) round its preview block.
 * The solid body goes the fission station's way: a GPU mesh built once, or the plain draw while a
 * shader pack is on. Every frame it adds the plasma ({@link FusionPlasma}: flowing glow shells and
 * lightning arcs) and the viewports (a smooth tint, not cut-out pixels). The block sits in the
 * middle of the design's footprint.
 */
public class FusionPreviewRenderer implements BlockEntityRenderer<FusionPreviewBlockEntity> {
    /** Half the design's footprint, in blocks (it is 19 across, centred on the middle block). */
    private static final float HALF = 9.5F;
    private static final float HEIGHT = 10;

    public FusionPreviewRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FusionPreviewBlockEntity preview, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = preview.getBlockState().getValue(FusionPreviewBlock.FACING);
        var body = StationGeometry.group(StationGeometry.FUSION, "static");
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
        if (StationMesh.shadersInUse()) {
            StationRenderer.draw(buffers.getBuffer(Sheets.cutoutBlockSheet()), pose, body, light);
        }
        if (preview.getLevel() != null) {
            long time = preview.getLevel().getGameTime();
            FusionPlasma.drawArcs(buffers.getBuffer(Sheets.translucentCullBlockSheet()), pose, time, preview.getBlockPos().asLong());
            // The glow adds light (its dark parts are clear) and writes no depth, so it shows
            // through itself and through the glass whatever order the buffers are drawn in.
            FusionPlasma.drawGlow(buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS)), pose, (time + partialTick) / 20F);
        }
        // The viewports: translucent without writing depth, so they never hide the glow behind them.
        StationRenderer.draw(buffers.getBuffer(RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS)), pose,
                StationGeometry.group(StationGeometry.FUSION, "glass"), light);
        pose.popPose();
    }

    /** The design faces north (ports at its low z); turn it about the block to the block's facing. */
    private static void place(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-HALF, 0, -HALF);
    }

    @Override
    public AABB getRenderBoundingBox(FusionPreviewBlockEntity preview) {
        var pos = preview.getBlockPos();
        return new AABB(pos.getX() + 0.5 - HALF, pos.getY(), pos.getZ() + 0.5 - HALF,
                pos.getX() + 0.5 + HALF, pos.getY() + HEIGHT, pos.getZ() + 0.5 + HALF);
    }

    @Override
    public boolean shouldRenderOffScreen(FusionPreviewBlockEntity preview) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
