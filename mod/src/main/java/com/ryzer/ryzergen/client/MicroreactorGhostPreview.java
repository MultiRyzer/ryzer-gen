package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorSlot;
import com.ryzer.ryzergen.machine.microreactor.ReactorHeartBlockEntity;
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
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Placement guide for the microreactor. While the player holds a microreactor part, every unformed
 * reactor heart nearby shows where the other three blocks go: an outline per space with the
 * suggested part floating inside. The heart's own front shows the way the machine will face.
 * Filled spaces turn green, blocked ones red.
 *
 * <p>The suggestion puts the heart at the lower front, machine units above and behind it, and the
 * coolant jacket at the upper back under the coolant intake. Any layout with the right parts works.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class MicroreactorGhostPreview {
    private static final double RANGE = 24;
    private static final Set<ReactorHeartBlockEntity> HEARTS = Collections.newSetFromMap(new WeakHashMap<>());
    /** The other three spaces, given the heart at the lower front. */
    private static final MicroreactorSlot[] OTHERS = {MicroreactorSlot.UPPER_FRONT, MicroreactorSlot.LOWER_BACK, MicroreactorSlot.UPPER_BACK};

    private MicroreactorGhostPreview() {}

    /** Called from the heart's client ticker, so the renderer knows which hearts are loaded. */
    public static void track(ReactorHeartBlockEntity heart) {
        HEARTS.add(heart);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        // Not in a shader pack's shadow pass: an overlay should not cast shadows.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || StationMesh.renderingShadows()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || !holdingPart(player)) {
            return;
        }
        HEARTS.removeIf(heart -> heart.isRemoved() || heart.getLevel() != minecraft.level);
        if (HEARTS.isEmpty()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float pulse = 0.55F + 0.25F * (float) Math.sin(Util.getMillis() / 300.0);

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (ReactorHeartBlockEntity heart : HEARTS) {
            BlockState state = heart.getBlockState();
            if (state.getValue(MicroreactorPartBlock.SLOT).isFormed()
                    || heart.getBlockPos().distToCenterSqr(player.position()) > RANGE * RANGE) {
                continue;
            }
            drawGuide(pose, buffers, heart.getBlockPos(), state.getValue(MicroreactorPartBlock.FACING), pulse);
        }
        pose.popPose();
        buffers.endBatch();
    }

    private static boolean holdingPart(Player player) {
        for (ItemStack stack : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.is(ModItems.REACTOR_HEART.get()) || stack.is(ModItems.REACTOR_MACHINE_UNIT.get())
                    || stack.is(ModItems.COOLANT_JACKET.get())) {
                return true;
            }
        }
        return false;
    }

    private static void drawGuide(PoseStack pose, MultiBufferSource.BufferSource buffers, BlockPos heart, Direction facing, float pulse) {
        Minecraft minecraft = Minecraft.getInstance();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (MicroreactorSlot slot : OTHERS) {
            BlockPos pos = slot.fromOrigin(heart, facing);
            BlockState there = minecraft.level.getBlockState(pos);
            boolean filled = there.getBlock() instanceof MicroreactorPartBlock;
            boolean blocked = !filled && !there.canBeReplaced();
            AABB box = new AABB(pos).deflate(0.02);
            if (filled) {
                LevelRenderer.renderLineBox(pose, lines, box, 0.27F, 0.84F, 0.37F, 0.9F);
            } else if (blocked) {
                LevelRenderer.renderLineBox(pose, lines, box, 0.82F, 0.24F, 0.24F, 0.9F);
            } else {
                LevelRenderer.renderLineBox(pose, lines, box, 0.21F, 0.78F, 0.96F, pulse);
                floatingItem(pose, buffers, pos, suggestedPart(slot));
                lines = buffers.getBuffer(RenderType.lines());
            }
        }
    }

    private static ItemStack suggestedPart(MicroreactorSlot slot) {
        return new ItemStack(slot == MicroreactorSlot.UPPER_BACK ? ModItems.COOLANT_JACKET.get() : ModItems.REACTOR_MACHINE_UNIT.get());
    }

    /** The suggested part, small and slowly turning, in the middle of its space. */
    private static void floatingItem(PoseStack pose, MultiBufferSource buffers, BlockPos pos, ItemStack stack) {
        pose.pushPose();
        pose.translate(pos.getX() + 0.5, pos.getY() + 0.35 + 0.05 * Math.sin(Util.getMillis() / 500.0), pos.getZ() + 0.5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((Util.getMillis() / 20L) % 360));
        pose.scale(0.9F, 0.9F, 0.9F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buffers, Minecraft.getInstance().level, 0);
        pose.popPose();
    }
}
