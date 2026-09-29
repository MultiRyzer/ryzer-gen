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
 * Lights the Lithium Extractor's columns while it works: the lit sorbent bed texture, full bright,
 * over each column from its foot up to a level that works like a piston stroke, as brine is pumped
 * up through the beds and pressed back out: a slow rise to the top, a hold, a quicker squeeze back
 * down, and a short rest before the next. Nothing is drawn while it is idle, when the columns show
 * unlit in the block model.
 */
public class LithiumExtractorRenderer implements BlockEntityRenderer<ProcessingBlockEntity> {
    private static final ResourceLocation LIT = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/machine/lithium_extractor/column_on");
    /** The columns in the model, facing north, in pixels: their centres across, foot and top. */
    private static final float[] CENTRES = {5, 11};
    private static final float FOOT = 5;
    private static final float TOP = 14;
    /** A hair proud of the column's faces, so the glow sits on them. */
    private static final float PROUD = 0.02F;

    /** One stroke, in ticks of work: about four seconds. */
    private static final float CYCLE = 80;

    public LithiumExtractorRenderer(BlockEntityRendererProvider.Context context) {
    }

    /**
     * The level through one stroke, 0 to 1, for {@code p} from 0 to 1: a slow rise (half the
     * stroke), a hold at the top, a squeeze back down (quicker, starting slow as if against
     * pressure), and a rest at the bottom. It ends where it starts, so strokes run on smoothly.
     */
    private static float stroke(float p) {
        if (p < 0.5F) {
            float f = p / 0.5F;
            return f * f * (3 - 2 * f);
        }
        if (p < 0.65F) {
            return 1;
        }
        if (p < 0.85F) {
            float f = (p - 0.65F) / 0.2F;
            return 1 - f * f;
        }
        return 0;
    }

    @Override
    public void render(ProcessingBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float spin = machine.spin();
        if (spin < 0.02F) {
            return;
        }
        float level = spin * stroke(machine.beat(partialTick) % CYCLE / CYCLE);
        float top = FOOT + (TOP - FOOT) * Math.max(0.08F, level);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LIT);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Direction facing = machine.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(((int) facing.toYRot() + 180) % 360)));
        pose.translate(-0.5, 0, -0.5);
        pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
        PoseStack.Pose matrix = pose.last();
        for (float x : CENTRES) {
            // The column is two boxes: its flanks on the wide one, its front and back on the narrow one.
            face(buffer, matrix, sprite, Direction.WEST, x - 2 - PROUD, 7, 11, top);
            face(buffer, matrix, sprite, Direction.EAST, x + 2 + PROUD, 7, 11, top);
            face(buffer, matrix, sprite, Direction.NORTH, 6.5F - PROUD, x - 1.5F, x + 1.5F, top);
            face(buffer, matrix, sprite, Direction.SOUTH, 11.5F + PROUD, x - 1.5F, x + 1.5F, top);
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
