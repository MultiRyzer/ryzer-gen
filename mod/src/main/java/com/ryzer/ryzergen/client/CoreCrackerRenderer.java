package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Turns the Core Cracker's flywheels while it works: they are a model of their own (datagen,
 * core_cracker_flywheel), drawn here about their axle and easing up to speed and down again.
 */
public class CoreCrackerRenderer implements BlockEntityRenderer<ProcessingBlockEntity> {
    public static final ModelResourceLocation FLYWHEEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/core_cracker_flywheel"));
    /** The flywheels' axle in the block, facing north, in pixels. */
    private static final float AXLE_Y = 7.5F;
    private static final float AXLE_Z = 8;

    public CoreCrackerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ProcessingBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(FLYWHEEL);
        Direction facing = machine.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        pose.pushPose();
        // Turned as the blockstate turns the machine, about the block's centre.
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(((int) facing.toYRot() + 180) % 360)));
        pose.translate(-0.5, 0, -0.5);
        // About the axle, which runs east to west through both wheels.
        pose.translate(0, AXLE_Y / 16, AXLE_Z / 16);
        pose.mulPose(Axis.XP.rotationDegrees(machine.angle(partialTick)));
        pose.translate(0, -AXLE_Y / 16, -AXLE_Z / 16);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.solidBlockSheet()),
                machine.getBlockState(), model, 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }
}
