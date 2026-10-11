package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.drill.MegaDrillBlockEntity;
import com.ryzer.ryzergen.machine.drill.MegaDrillLayout;
import com.ryzer.ryzergen.registry.ModBlocks;
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
 * Placement guide for the mega drill, like the pool's. While the player holds drill frame or a
 * controller, every unformed controller nearby outlines the drill's cells: blue where frame is
 * missing, green where it is in place, red where something is in the way. The next few missing
 * cells, lowest first (the order the controller builds in), show the frame floating in them; the
 * pit (the 7 x 7 under the base's opening) is outlined on the ground, so the player sees what it will mine.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class MegaDrillGhostPreview {
    private static final double RANGE = 40;
    /** Floating frame in this many missing cells at most; the outlines show the rest. */
    private static final int FLOATING = 12;
    private static final Set<MegaDrillBlockEntity> CONTROLLERS = Collections.newSetFromMap(new WeakHashMap<>());

    private MegaDrillGhostPreview() {}

    public static void track(MegaDrillBlockEntity controller) {
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
        for (MegaDrillBlockEntity controller : CONTROLLERS) {
            if (!controller.isFormed() && controller.getBlockPos().distToCenterSqr(player.position()) <= RANGE * RANGE) {
                drawGuide(pose, buffers, controller, pulse);
            }
        }
        pose.popPose();
        buffers.endBatch();
    }

    private static boolean holdingPart(Player player) {
        for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.is(ModItems.DRILL_FRAME.get()) || stack.is(ModItems.MEGA_DRILL_CONTROLLER.get())) {
                return true;
            }
        }
        return false;
    }

    private static void drawGuide(PoseStack pose, MultiBufferSource.BufferSource buffers, MegaDrillBlockEntity controller, float pulse) {
        Minecraft minecraft = Minecraft.getInstance();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        // The pit it will mine, outlined on the ground under the base.
        BlockPos centre = controller.centre();
        AABB pit = new AABB(centre.getX() + MegaDrillLayout.LOW, centre.getY() - 0.98, centre.getZ() + MegaDrillLayout.LOW,
                centre.getX() + MegaDrillLayout.HIGH + 1, centre.getY() + 0.02, centre.getZ() + MegaDrillLayout.HIGH + 1);
        LevelRenderer.renderLineBox(pose, lines, pit, 0.94F, 0.63F, 0.19F, 0.6F);
        int floating = 0;
        for (MegaDrillLayout.Cell cell : MegaDrillLayout.CELL_LIST) {
            if (cell.pos().equals(MegaDrillLayout.CONTROLLER)) {
                continue;
            }
            BlockPos pos = MegaDrillLayout.toWorld(controller.getBlockPos(), controller.facing(), cell.pos());
            BlockState there = minecraft.level.getBlockState(pos);
            AABB box = new AABB(pos).deflate(0.03);
            if (there.is(ModBlocks.DRILL_FRAME.get())) {
                LevelRenderer.renderLineBox(pose, lines, box, 0.27F, 0.84F, 0.37F, 0.5F);
            } else if (!there.canBeReplaced()) {
                LevelRenderer.renderLineBox(pose, lines, box, 0.82F, 0.24F, 0.24F, 0.9F);
            } else {
                LevelRenderer.renderLineBox(pose, lines, box, 0.21F, 0.78F, 0.96F, pulse);
                // The cells list runs from the base up, near enough the order the build goes in.
                if (floating++ < FLOATING) {
                    floatingItem(pose, buffers, pos, new ItemStack(ModBlocks.DRILL_FRAME.get()));
                    lines = buffers.getBuffer(RenderType.lines());
                }
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
