package com.ryzer.ryzergen.compat.accessories;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.client.rendering.ModelTransformUtils;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The dosimeter ring as worn: a thin green band wrapped round the hand, a little up from the end
 * of the arm, with its purple fluorite chip set on the outside. Accessories' default draws the
 * flat item sprite against the side of the hand instead. Even slots are the right hand, odd the
 * left, as Accessories places rings; more rings sit further up the hand.
 *
 * <p>Drawn as plain boxes, tinted, on vanilla's white texture, so they are lit and shaded like the
 * player model. Only ever loaded when Accessories is (see AccessoriesClient).
 */
public class DosimeterRingRenderer implements AccessoryRenderer {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final int BAND = 0xFF3F9160;
    private static final int BAND_EDGE = 0xFF2A6B45;
    private static final int CHIP = 0xFF8A4CCC;
    private static final float PX = 1 / 16F;
    /** Half the arm's width (4 pixels), and how far the band stands off it. */
    private static final float ARM = 2;
    private static final float GAP = 0.15F;
    private static final float THICK = 0.35F;

    @Override
    public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack pose, EntityModel<M> model,
                                                MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
                                                float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(model instanceof HumanoidModel<?>)) {
            return;
        }
        boolean right = reference.slot() % 2 == 0;
        int stackIndex = reference.slot() / 2;
        pose.pushPose();
        // The end of the arm, centred: the same anchor Accessories uses for rings.
        ModelTransformUtils.transformToModelPart(pose, reference.entity(), model, right ? "right_arm" : "left_arm", 0, -1, 0);
        pose.translate(0, (1.2F + 1.4F * stackIndex) * PX, 0);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        float in = ARM + GAP;
        float out = in + THICK;
        // Four sides of the band, 1 pixel wide, round the arm.
        box(buffer, pose, -out, 0, -out, out, 1, -in, BAND, light);
        box(buffer, pose, -out, 0, in, out, 1, out, BAND, light);
        box(buffer, pose, -out, 0, -in, -in, 1, in, BAND_EDGE, light);
        box(buffer, pose, in, 0, -in, out, 1, in, BAND_EDGE, light);
        // The fluorite chip on the outside of the hand.
        float side = right ? 1 : -1;
        float x1 = side * out;
        float x2 = side * (out + 0.6F);
        box(buffer, pose, Math.min(x1, x2), -0.1F, -0.5F, Math.max(x1, x2), 1.1F, 0.5F, CHIP, light);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderInFirstPerson(net.minecraft.world.entity.HumanoidArm arm, ItemStack stack, SlotReference reference) {
        return true;
    }

    /** A box in pixels, each face tinted and lit, facing out. */
    private static void box(VertexConsumer buffer, PoseStack pose, float x1, float y1, float z1, float x2, float y2, float z2,
                            int colour, int light) {
        PoseStack.Pose last = pose.last();
        float a = x1 * PX, b = y1 * PX, c = z1 * PX, d = x2 * PX, e = y2 * PX, f = z2 * PX;
        face(buffer, last, colour, light, Direction.DOWN, a, b, c, d, b, c, d, b, f, a, b, f);
        face(buffer, last, colour, light, Direction.UP, a, e, f, d, e, f, d, e, c, a, e, c);
        face(buffer, last, colour, light, Direction.NORTH, a, e, c, d, e, c, d, b, c, a, b, c);
        face(buffer, last, colour, light, Direction.SOUTH, a, b, f, d, b, f, d, e, f, a, e, f);
        face(buffer, last, colour, light, Direction.WEST, a, b, c, a, b, f, a, e, f, a, e, c);
        face(buffer, last, colour, light, Direction.EAST, d, e, c, d, e, f, d, b, f, d, b, c);
    }

    private static void face(VertexConsumer buffer, PoseStack.Pose pose, int colour, int light, Direction normal, float... xyz) {
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2])
                    .setColor(colour)
                    .setUv(i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
        }
    }
}
