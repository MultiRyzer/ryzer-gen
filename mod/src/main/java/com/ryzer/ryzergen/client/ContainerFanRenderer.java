package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.container.BatteryControllerBlockEntity;
import com.ryzer.ryzergen.battery.container.ContainerPartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;

/**
 * Spins the Container Battery's fan while it works. The blades (art/tools/container_concept.py,
 * 40 x 40 on a 48 canvas) turn about the hub in the fan unit's end, behind the guard; the rest of
 * the unit is the block model. The blades ease up to speed and down again.
 */
public class ContainerFanRenderer implements BlockEntityRenderer<BatteryControllerBlockEntity> {
    private static final ResourceLocation BLADES = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/container/blades");
    /** The design's fan: the blades' plane (x), the hub's centre (y, z) and half their span, in pixels. */
    private static final float BLADES_X = 121.5F;
    private static final float HUB_Y = 24;
    private static final float HUB_Z = 24;
    private static final float HALF = 20;
    /** The controller's block corner in the design (its cell, 3 across and 1 up, times 16). */
    private static final float CONTROLLER_X = 48;
    private static final float CONTROLLER_Y = 16;

    public ContainerFanRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BatteryControllerBlockEntity battery, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!battery.isFormed()) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(BLADES);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Direction facing = battery.getBlockState().getValue(ContainerPartBlock.FACING);
        pose.pushPose();
        // The design faces north; turn it as the blockstate turns the models, about the block's centre.
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(((int) facing.toYRot() + 180) % 360)));
        pose.translate(-0.5, 0, -0.5);
        pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
        pose.translate(-CONTROLLER_X, -CONTROLLER_Y, 0);
        // To the hub, turned by the fan's angle about the design's x axis.
        pose.translate(BLADES_X, HUB_Y, HUB_Z);
        pose.mulPose(Axis.XP.rotationDegrees(battery.fanAngle(partialTick)));
        PoseStack.Pose matrix = pose.last();
        float u1 = sprite.getU(0);
        float v1 = sprite.getV(0);
        float u2 = sprite.getU(40 / 48F);
        float v2 = sprite.getV(40 / 48F);
        // One quad facing out of the east end (the texture as the design draws it), seen from both sides.
        float[][] corners = {{0, HALF, HALF}, {0, HALF, -HALF}, {0, -HALF, -HALF}, {0, -HALF, HALF}};
        float[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(matrix, corners[i][0], corners[i][1], corners[i][2])
                    .setColor(0xFFFFFFFF)
                    .setUv(uvs[i][0], uvs[i][1])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(matrix, 1, 0, 0);
        }
        pose.popPose();
    }

    /** The whole container, so the fan still turns when the controller is off screen. */
    @Override
    public AABB getRenderBoundingBox(BatteryControllerBlockEntity battery) {
        return new AABB(battery.getBlockPos()).inflate(6, 2, 6);
    }
}
