package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.client.MachineMotion.Clock;
import com.ryzer.ryzergen.client.MachineMotion.Element;
import com.ryzer.ryzergen.client.MachineMotion.Glow;
import com.ryzer.ryzergen.client.MachineMotion.Part;
import com.ryzer.ryzergen.machine.AnimatedMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

/**
 * Draws a machine's moving parts and lit faces, as its MachineMotion lists them, turned
 * with the block. The body stays a baked block model; only these are drawn each frame, so a base
 * full of machines stays cheap.
 */
public class MachinePartsRenderer<T extends BlockEntity & AnimatedMachine> implements BlockEntityRenderer<T> {
    /** A hair proud of the faces under a glow, so it sits on them. */
    private static final float PROUD = 0.02F;

    public MachinePartsRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        List<Element> elements = MachineMotion.of(machine.motionId());
        if (elements.isEmpty()) {
            return;
        }
        Clock clock = Clock.of(machine, partialTick, machine.getLevel());
        Direction facing = machine.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        pose.pushPose();
        // The designs face north; turn them as the blockstate turns the models, about the block's centre.
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(((int) facing.toYRot() + 180) % 360)));
        pose.translate(-0.5, 0, -0.5);
        for (Element element : elements) {
            switch (element) {
                case Part part -> part(machine, part, clock, pose, buffers, light, overlay);
                case Glow glow -> glow(glow, clock, pose, buffers);
            }
        }
        pose.popPose();
    }

    /** A part model, moved; a warm one lit and tinted by the machine's heat, as if by the glow beside it. */
    private static void part(BlockEntity machine, Part part, Clock clock, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        float g = 1;
        float b = 1;
        if (part.warm() && clock.heat() > 0) {
            int block = Math.max(LightTexture.block(light), Math.round(clock.heat() * 13));
            light = LightTexture.pack(block, LightTexture.sky(light));
            g = 1 - 0.12F * clock.heat();
            b = 1 - 0.3F * clock.heat();
        }
        pose.pushPose();
        part.move().apply(pose, clock);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
                machine.getBlockState(), minecraft.getModelManager().getModel(part.model()), 1, g, b, light, overlay,
                ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }

    private static void glow(Glow glow, Clock clock, PoseStack pose, MultiBufferSource buffers) {
        int argb = glow.shine().argb(clock);
        if ((argb >>> 24) == 0) {
            return;
        }
        float top = glow.y1() + (glow.y2() - glow.y1()) * Mth.clamp(glow.fill().to(clock), 0, 1);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(glow.texture());
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose matrix = pose.last();
        float x1 = glow.x1();
        float x2 = glow.x2();
        float y1 = glow.y1();
        float z1 = glow.z1();
        float z2 = glow.z2();
        for (Direction face : glow.faces()) {
            float[][] corners = switch (face) {
                case NORTH -> new float[][] {{x2, top, z1 - PROUD}, {x1, top, z1 - PROUD}, {x1, y1, z1 - PROUD}, {x2, y1, z1 - PROUD}};
                case SOUTH -> new float[][] {{x1, top, z2 + PROUD}, {x2, top, z2 + PROUD}, {x2, y1, z2 + PROUD}, {x1, y1, z2 + PROUD}};
                case WEST -> new float[][] {{x1 - PROUD, top, z1}, {x1 - PROUD, top, z2}, {x1 - PROUD, y1, z2}, {x1 - PROUD, y1, z1}};
                case EAST -> new float[][] {{x2 + PROUD, top, z2}, {x2 + PROUD, top, z1}, {x2 + PROUD, y1, z1}, {x2 + PROUD, y1, z2}};
                case UP -> new float[][] {{x1, top + PROUD, z1}, {x1, top + PROUD, z2}, {x2, top + PROUD, z2}, {x2, top + PROUD, z1}};
                case DOWN -> new float[][] {{x1, y1 - PROUD, z2}, {x1, y1 - PROUD, z1}, {x2, y1 - PROUD, z1}, {x2, y1 - PROUD, z2}};
            };
            for (float[] corner : corners) {
                float[] uv = uv(face, corner[0], corner[1], corner[2]);
                buffer.addVertex(matrix, corner[0] / 16, corner[1] / 16, corner[2] / 16)
                        .setColor(argb)
                        .setUv(sprite.getU(uv[0] / 16), sprite.getV(uv[1] / 16))
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(matrix, face.getStepX(), face.getStepY(), face.getStepZ());
            }
        }
    }

    /** Where a point on a face samples its texture, as a block model maps a face with no UVs of its own. */
    private static float[] uv(Direction face, float x, float y, float z) {
        float[] uv = switch (face) {
            case NORTH -> new float[] {16 - x, 16 - y};
            case SOUTH -> new float[] {x, 16 - y};
            case WEST -> new float[] {z, 16 - y};
            case EAST -> new float[] {16 - z, 16 - y};
            case UP -> new float[] {x, z};
            case DOWN -> new float[] {x, 16 - z};
        };
        uv[0] = Mth.clamp(uv[0], 0, 16);
        uv[1] = Mth.clamp(uv[1], 0, 16);
        return uv;
    }
}
