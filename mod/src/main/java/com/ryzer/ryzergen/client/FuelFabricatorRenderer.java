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
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Works the Fuel Fabricator's press: its ram and platen (a model of their own, datagen
 * fuel_fabricator_ram, drawn at the bottom of the stroke) rest raised, and press down onto the bed
 * and lift again in a steady stroke while it works, easing in and out of it.
 */
public class FuelFabricatorRenderer implements BlockEntityRenderer<ProcessingBlockEntity> {
    public static final ModelResourceLocation RAM =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/fuel_fabricator_ram"));
    /** How far the platen rests above the bed, in pixels. */
    private static final float STROKE = 1.5F;

    public FuelFabricatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ProcessingBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        Direction facing = machine.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        // 0 at rest, 1 on the bed: a full stroke each turn of the machine's angle.
        float press = machine.spin() * (1 - Mth.cos(machine.angle(partialTick) * Mth.DEG_TO_RAD)) / 2;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(((int) facing.toYRot() + 180) % 360)));
        pose.translate(-0.5, STROKE * (1 - press) / 16, -0.5);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.solidBlockSheet()),
                machine.getBlockState(), minecraft.getModelManager().getModel(RAM), 1, 1, 1, light, overlay, ModelData.EMPTY,
                RenderType.solid());
        pose.popPose();
    }
}
