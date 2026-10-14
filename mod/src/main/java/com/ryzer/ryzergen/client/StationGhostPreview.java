package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.fission.StationCoreBlockEntity;
import com.ryzer.ryzergen.machine.fission.StationLayout;
import com.ryzer.ryzergen.machine.fission.StationPart;
import com.ryzer.ryzergen.machine.fission.StationReserve;
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
import net.minecraft.core.Direction;
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
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Placement guide for the fission station, like the microreactor's. While the player holds a
 * station part, every unformed control core nearby outlines the spaces still to fill: blue where a
 * part is missing, green where the right part is in place, red where something else is in the way.
 * With some 340 spaces, the suggested part floats only in empty spaces close to the player.
 * Holding a Coolant Reserve block beside a formed station outlines the reserve's next ring of bays
 * on its back the same way (it grows from the centre: 1 bay, then 3, then 5).
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class StationGhostPreview {
    private static final double RANGE = 40;
    /** Suggested parts float only in spaces this close to the player. */
    private static final double ITEM_RANGE = 6;
    private static final Set<StationCoreBlockEntity> CORES = Collections.newSetFromMap(new WeakHashMap<>());

    private StationGhostPreview() {}

    public static void track(StationCoreBlockEntity core) {
        CORES.add(core);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        // Not in a shader pack's shadow pass: an overlay should not cast shadows.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || StationMesh.renderingShadows()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        boolean part = player != null && holdingPart(player);
        boolean reserve = player != null && holding(player, ModItems.COOLANT_RESERVE.get().asItem());
        if (player == null || minecraft.level == null || (!part && !reserve)) {
            return;
        }
        CORES.removeIf(core -> core.isRemoved() || core.getLevel() != minecraft.level);
        if (CORES.isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float pulse = 0.5F + 0.25F * (float) Math.sin(Util.getMillis() / 300.0);
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (StationCoreBlockEntity core : CORES) {
            if (core.getBlockPos().distToCenterSqr(player.position()) > RANGE * RANGE) {
                continue;
            }
            if (!core.isFormed() && part) {
                drawGuide(pose, buffers, core, player.position(), pulse);
            } else if (core.isFormed() && reserve) {
                drawReserveGuide(pose, buffers, core, player.position(), pulse);
            }
        }
        pose.popPose();
        buffers.endBatch();
    }

    private static boolean holding(Player player, net.minecraft.world.item.Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    private static boolean holdingPart(Player player) {
        for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.is(ModItems.STATION_CASING.get()) || stack.is(ModItems.STATION_GLASS.get())
                    || stack.is(ModItems.TURBINE_ROTOR.get()) || stack.is(ModItems.STATION_CORE.get())) {
                return true;
            }
        }
        return false;
    }

    private static void drawGuide(PoseStack pose, MultiBufferSource.BufferSource buffers, StationCoreBlockEntity core, Vec3 player, float pulse) {
        Direction facing = core.facing();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Map.Entry<BlockPos, StationPart> entry : StationLayout.PARTS.entrySet()) {
            if (entry.getValue() != StationPart.CORE) {
                lines = space(pose, buffers, lines, StationLayout.toWorld(core.getBlockPos(), facing, entry.getKey()),
                        entry.getValue().block(), player, pulse);
            }
        }
    }

    /** The coolant reserve's next ring of bays on a formed station's back, if it can still grow. */
    private static void drawReserveGuide(PoseStack pose, MultiBufferSource.BufferSource buffers, StationCoreBlockEntity core, Vec3 player, float pulse) {
        Minecraft minecraft = Minecraft.getInstance();
        int rings = StationReserve.rings(minecraft.level, core.getBlockPos(), core.facing());
        if (rings >= StationLayout.RESERVE.size()) {
            return;
        }
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (BlockPos cell : StationLayout.RESERVE.get(rings)) {
            lines = space(pose, buffers, lines, StationLayout.toWorld(core.getBlockPos(), core.facing(), cell),
                    ModBlocks.COOLANT_RESERVE.get(), player, pulse);
        }
    }

    /**
     * One space's outline: green with its part in place, red with something else in the way, a
     * pulsing blue while empty, with the part floating in it when the player is close. Returns the
     * lines buffer to carry on with (drawing an item ends it).
     */
    private static VertexConsumer space(PoseStack pose, MultiBufferSource.BufferSource buffers, VertexConsumer lines, BlockPos pos,
                                        net.minecraft.world.level.block.Block part, Vec3 player, float pulse) {
        BlockState there = Minecraft.getInstance().level.getBlockState(pos);
        AABB box = new AABB(pos).deflate(0.03);
        if (there.is(part)) {
            LevelRenderer.renderLineBox(pose, lines, box, 0.27F, 0.84F, 0.37F, 0.5F);
        } else if (!there.canBeReplaced()) {
            LevelRenderer.renderLineBox(pose, lines, box, 0.82F, 0.24F, 0.24F, 0.9F);
        } else {
            LevelRenderer.renderLineBox(pose, lines, box, 0.21F, 0.78F, 0.96F, pulse);
            if (pos.distToCenterSqr(player) < ITEM_RANGE * ITEM_RANGE) {
                floatingItem(pose, buffers, pos, new ItemStack(part));
                return buffers.getBuffer(RenderType.lines());
            }
        }
        return lines;
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
