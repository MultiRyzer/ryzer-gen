package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ryzer.ryzergen.storage.PressureTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What is inside a tank, seen through the sight glass. A gas fills the whole tank rather than sitting
 * at a level, so in a pressure tank the haze fills the whole inside and grows thicker as the pressure
 * rises (clearly visible even when nearly empty). A liquid settles, so in a fluid tank it fills from
 * the floor up to its level, nearly opaque, and glows if it gives off light (lava). Drawn once per
 * tank, by its controller, as one box tiled a block at a time.
 */
public class PressureTankRenderer implements BlockEntityRenderer<PressureTankBlockEntity> {
    /** Inset from the outside, clear of the walls. */
    private static final float WALL = 1.5F / 16;
    /** The tower's floor and roof are thicker than its walls. */
    private static final float FLOOR = 2.5F / 16;
    private static final float ROOF = 2.5F / 16;

    public PressureTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PressureTankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!tank.isController()) {
            return;
        }
        FluidStack gas = tank.contents();
        if (gas.isEmpty() || tank.capacity() <= 0) {
            return;
        }
        float fill = Mth.clamp(gas.getAmount() / (float) tank.capacity(), 0, 1);
        IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(gas.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(look.getStillTexture(gas));
        int tint = look.getTintColor(gas);
        float opacity = tank.holdsGas() ? 0.35F + 0.5F * Mth.sqrt(fill) : 0.9F;
        int colour = (int) (opacity * 255) << 24 | (tint & 0xFFFFFF);
        if (gas.getFluid().getFluidType().getLightLevel(gas) > 0) {
            light = LightTexture.FULL_BRIGHT;
        }

        int width = tank.isTower() ? 2 : 1;
        int height = tank.isTower() ? tank.height() : 1;
        float x1 = WALL;
        float z1 = WALL;
        float x2 = width - WALL;
        float z2 = width - WALL;
        float y1 = tank.isTower() ? FLOOR : WALL;
        float y2 = height - (tank.isTower() ? ROOF : WALL);
        if (!tank.holdsGas()) {
            y2 = y1 + (y2 - y1) * fill;
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose matrix = pose.last();
        for (Direction face : Direction.values()) {
            box(buffer, matrix, sprite, colour, light, overlay, face, x1, y1, z1, x2, y2, z2);
        }
    }

    /** One face of the haze box, split into block-sized tiles so the texture keeps its scale. */
    private static void box(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, int colour, int light, int overlay,
                            Direction face, float x1, float y1, float z1, float x2, float y2, float z2) {
        // The face's two axes (a, b) and its fixed coordinate.
        float aMin, aMax, bMin, bMax, fixed;
        switch (face.getAxis()) {
            case X -> { aMin = z1; aMax = z2; bMin = y1; bMax = y2; fixed = face == Direction.EAST ? x2 : x1; }
            case Y -> { aMin = x1; aMax = x2; bMin = z1; bMax = z2; fixed = face == Direction.UP ? y2 : y1; }
            default -> { aMin = x1; aMax = x2; bMin = y1; bMax = y2; fixed = face == Direction.SOUTH ? z2 : z1; }
        }
        for (int ia = (int) Math.floor(aMin); ia < aMax; ia++) {
            float a1 = Math.max(aMin, ia);
            float a2 = Math.min(aMax, ia + 1);
            for (int ib = (int) Math.floor(bMin); ib < bMax; ib++) {
                float b1 = Math.max(bMin, ib);
                float b2 = Math.min(bMax, ib + 1);
                if (a2 > a1 && b2 > b1) {
                    quad(buffer, pose, colour, light, overlay, face, fixed, a1, a2, b1, b2,
                            sprite.getU(a1 - ia), sprite.getU(a2 - ia), sprite.getV(1 - (b2 - ib)), sprite.getV(1 - (b1 - ib)));
                }
            }
        }
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int colour, int light, int overlay, Direction face,
                             float fixed, float a1, float a2, float b1, float b2, float u1, float u2, float v1, float v2) {
        float[][] corners = new float[4][];
        // Wound so each face points outward.
        switch (face) {
            case NORTH -> corners = new float[][] {{a2, b1, fixed}, {a1, b1, fixed}, {a1, b2, fixed}, {a2, b2, fixed}};
            case SOUTH -> corners = new float[][] {{a1, b1, fixed}, {a2, b1, fixed}, {a2, b2, fixed}, {a1, b2, fixed}};
            case WEST -> corners = new float[][] {{fixed, b1, a1}, {fixed, b1, a2}, {fixed, b2, a2}, {fixed, b2, a1}};
            case EAST -> corners = new float[][] {{fixed, b1, a2}, {fixed, b1, a1}, {fixed, b2, a1}, {fixed, b2, a2}};
            case UP -> corners = new float[][] {{a1, fixed, b1}, {a1, fixed, b2}, {a2, fixed, b2}, {a2, fixed, b1}};
            case DOWN -> corners = new float[][] {{a1, fixed, b2}, {a1, fixed, b1}, {a2, fixed, b1}, {a2, fixed, b2}};
        }
        float[][] uvs = {{u2, v2}, {u1, v2}, {u1, v1}, {u2, v1}};
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, corners[i][0], corners[i][1], corners[i][2])
                    .setColor(colour)
                    .setUv(uvs[i][0], uvs[i][1])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }

    @Override
    public AABB getRenderBoundingBox(PressureTankBlockEntity tank) {
        int width = tank.isTower() ? 2 : 1;
        int height = tank.isTower() ? tank.height() : 1;
        return new AABB(tank.getBlockPos()).expandTowards(width - 1, height - 1, width - 1);
    }
}
