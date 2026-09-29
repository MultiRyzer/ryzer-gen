package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.pool.PoolControllerBlockEntity;
import com.ryzer.ryzergen.machine.pool.PoolLayout;
import com.ryzer.ryzergen.machine.pool.PoolPart;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Placement guide for the Spent Fuel Pool, like the station's. While the player holds a pool part,
 * every unformed controller nearby outlines its 5 x 3 x 3: blue where a part is missing (with the
 * part floating in it), green where the right part is in place, red where something is in the way.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class PoolGhostPreview {
    private static final double RANGE = 32;
    private static final Set<PoolControllerBlockEntity> CONTROLLERS = Collections.newSetFromMap(new WeakHashMap<>());

    private PoolGhostPreview() {}

    public static void track(PoolControllerBlockEntity controller) {
        CONTROLLERS.add(controller);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || StationMesh.renderingShadows()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || !holdingPart(player)) {
            return;
        }
        CONTROLLERS.removeIf(controller -> controller.isRemoved() || controller.getLevel() != minecraft.level);
        if (CONTROLLERS.isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float pulse = 0.5F + 0.25F * (float) Math.sin(Util.getMillis() / 300.0);
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (PoolControllerBlockEntity controller : CONTROLLERS) {
            if (!controller.isFormed() && controller.getBlockPos().distToCenterSqr(player.position()) <= RANGE * RANGE) {
                drawGuide(pose, buffers, controller, pulse);
            }
        }
        pose.popPose();
        buffers.endBatch();
    }

    private static boolean holdingPart(Player player) {
        for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.is(ModItems.POOL_LINER.get()) || stack.is(ModItems.POOL_CRANE.get()) || stack.is(ModItems.POOL_CONTROLLER.get())) {
                return true;
            }
        }
        return false;
    }

    private static void drawGuide(PoseStack pose, MultiBufferSource.BufferSource buffers, PoolControllerBlockEntity controller, float pulse) {
        Minecraft minecraft = Minecraft.getInstance();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        boolean raised = controller.raised();
        for (int index = 1; index <= PoolLayout.CELLS; index++) {
            BlockPos cell = PoolLayout.cell(index);
            PoolPart part = PoolLayout.partAt(cell, raised);
            if (part == PoolPart.CONTROLLER) {
                continue;
            }
            BlockPos pos = controller.cellPos(cell);
            BlockState there = minecraft.level.getBlockState(pos);
            AABB box = new AABB(pos).deflate(0.03);
            if (there.is(part.block())) {
                LevelRenderer.renderLineBox(pose, lines, box, 0.27F, 0.84F, 0.37F, 0.5F);
            } else if (!there.canBeReplaced()) {
                LevelRenderer.renderLineBox(pose, lines, box, 0.82F, 0.24F, 0.24F, 0.9F);
            } else {
                LevelRenderer.renderLineBox(pose, lines, box, 0.21F, 0.78F, 0.96F, pulse);
                floatingItem(pose, buffers, pos, new ItemStack(part.block()));
                lines = buffers.getBuffer(RenderType.lines());
            }
        }
    }

    /** The suggested part, small and slowly turning, in the middle of its space. */
    private static void floatingItem(PoseStack pose, MultiBufferSource buffers, BlockPos pos, ItemStack stack) {
        pose.pushPose();
        pose.translate(pos.getX() + 0.5, pos.getY() + 0.35 + 0.05 * Math.sin(Util.getMillis() / 500.0), pos.getZ() + 0.5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((Util.getMillis() / 20L) % 360));
        pose.scale(0.7F, 0.7F, 0.7F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers, Minecraft.getInstance().level, 0);
        pose.popPose();
    }
}
