package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.processing.ProcessingBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Works the Monazite Processor's extraction columns while it runs: the lit solvent texture, full
 * bright, over each column from its foot up to a level that surges up and falls back in short
 * pulses, the two columns half a beat apart. Real basis: pulsed extraction columns, where the liquid
 * is driven up and down to mix the two phases. Nothing is drawn while it is idle, when the columns
 * show unlit in the block model.
 */
public class MonaziteProcessorRenderer implements BlockEntityRenderer<ProcessingBlockEntity> {
    private static final ResourceLocation LIT = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/machine/monazite_processor/column_on");
    /** The columns in the model, facing north, in pixels: their west edges (2 wide), front and back, foot and top. */
    private static final float[] WEST_EDGES = {1.5F, 12.5F};
    private static final float FRONT = 2;
    private static final float BACK = 4;
    private static final float FOOT = 9;
    private static final float TOP = 15;
    /** A hair proud of the column's faces, so the glow sits on them. */
    private static final float PROUD = 0.02F;
    /** One pulse, in ticks of work: a second and a half. */
    private static final float CYCLE = 30;

    public MonaziteProcessorRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The level through one pulse, 0 to 1: a quick surge up, a short hold, a slower fall back. */
    private static float pulse(float p) {
        if (p < 0.25F) {
            float f = p / 0.25F;
            return f * (2 - f);
        }
        if (p < 0.4F) {
            return 1;
        }
        float f = (p - 0.4F) / 0.6F;
        return 1 - f * f * (3 - 2 * f);
    }

    @Override
    public void render(ProcessingBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float spin = machine.spin();
        if (spin < 0.02F) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LIT);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Direction facing = machine.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(((int) facing.toYRot() + 180) % 360)));
        pose.translate(-0.5, 0, -0.5);
        pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
        PoseStack.Pose matrix = pose.last();
        float beat = machine.beat(partialTick);
        for (int c = 0; c < WEST_EDGES.length; c++) {
            float level = spin * (0.3F + 0.7F * pulse((beat / CYCLE + c * 0.5F) % 1));
            float top = FOOT + (TOP - FOOT) * level;
            float x = WEST_EDGES[c];
            face(buffer, matrix, sprite, Direction.WEST, x - PROUD, FRONT, BACK, top);
            face(buffer, matrix, sprite, Direction.EAST, x + 2 + PROUD, FRONT, BACK, top);
            face(buffer, matrix, sprite, Direction.NORTH, FRONT - PROUD, x, x + 2, top);
            face(buffer, matrix, sprite, Direction.SOUTH, BACK + PROUD, x, x + 2, top);
        }
        pose.popPose();
    }

    /**
     * One face of a column from its foot to {@code top}: on the plane {@code at} (x for west and
     * east, z for north and south), spanning {@code a1} to {@code a2} along the face. The texture maps
     * by position, as the block model's does.
     */
    private static void face(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, Direction face,
                             float at, float a1, float a2, float top) {
        float v1 = sprite.getV((16 - top) / 16);
        float v2 = sprite.getV((16 - FOOT) / 16);
        float u1 = sprite.getU(a1 / 16);
        float u2 = sprite.getU(a2 / 16);
        float[][] corners = switch (face) {
            case WEST -> new float[][] {{at, top, a1}, {at, top, a2}, {at, FOOT, a2}, {at, FOOT, a1}};
            case EAST -> new float[][] {{at, top, a2}, {at, top, a1}, {at, FOOT, a1}, {at, FOOT, a2}};
            case NORTH -> new float[][] {{a2, top, at}, {a1, top, at}, {a1, FOOT, at}, {a2, FOOT, at}};
            default -> new float[][] {{a1, top, at}, {a2, top, at}, {a2, FOOT, at}, {a1, FOOT, at}};
        };
        float[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, corners[i][0], corners[i][1], corners[i][2])
                    .setColor(0xFFFFFFFF)
                    .setUv(uvs[i][0], uvs[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }
}
