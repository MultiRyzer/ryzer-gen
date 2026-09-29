package com.ryzer.ryzergen.client;

import java.util.List;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelResourceLocation;
import com.ryzer.ryzergen.machine.pool.PoolLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.pool.PoolControllerBlockEntity;
import com.ryzer.ryzergen.machine.pool.PoolPartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;

/**
 * Draws the fuel in a formed Spent Fuel Pool: a rod standing in the rack's cell for each filled
 * rack slot, laid out as the controller's screen lays them out (6 across, 3 deep, from the front
 * left), so the pool fills as rods go in. A rod fresh in glows Cherenkov blue and lights up, and
 * fades back to plain steel as it cools. It also runs the crane: parked over the middle of the pool
 * until a rod finishes cooling, when it runs to a rack cell, lowers its grab, lifts a rod out and
 * carries it to the output end, sets it down and comes back (the server starts a run at most every
 * ten seconds). The crane's parts are models of their own (pool_concept.py, crane_parts). The rack
 * and everything else is the block model.
 */
public class PoolRenderer implements BlockEntityRenderer<PoolControllerBlockEntity> {
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/pool/rod_side");
    private static final ResourceLocation TOP = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/pool/rod_top");
    /** The rack and rods in the design, in pixels (see PoolLayout). A rod is 4 across. */
    private static final int RACK_X = PoolLayout.RACK_X;
    private static final int RACK_Z = PoolLayout.RACK_Z;
    private static final int CELL = PoolLayout.CELL;
    private static final int COLUMNS = PoolLayout.COLUMNS;
    private static final int ROD_BOTTOM = PoolLayout.ROD_BOTTOM;
    private static final int ROD_TOP = PoolLayout.ROD_TOP;
    /** The controller's block corner in the design (its cell, 2 across and 1 up, times 16). */
    private static final float CONTROLLER_X = 32;
    private static final float CONTROLLER_Y = 16;
    /** Cherenkov blue, the tint of a rod fresh in. */
    private static final int HOT_R = 0x8F;
    private static final int HOT_G = 0xE3;
    private static final int HOT_B = 0xFF;

    public static final ModelResourceLocation BRIDGE = crane("bridge");
    public static final ModelResourceLocation TROLLEY = crane("trolley");
    public static final ModelResourceLocation CABLE = crane("cable");
    public static final ModelResourceLocation GRAB = crane("grab");
    public static final ModelResourceLocation ROD = crane("rod");
    public static final List<ModelResourceLocation> CRANE_PARTS = List.of(BRIDGE, TROLLEY, CABLE, GRAB, ROD);

    /** The crane parked, in the design's pixels: the bridge's x, the trolley's z, and the grab's foot. */
    private static final float PARK_X = 40;
    private static final float PARK_Z = 22;
    private static final float PARK_GRAB = 38;
    /** The crane parts' origin in the design (pool_concept.py, CRANE_ORIGIN): where the park sits. */
    private static final float ORIGIN_X = 40;
    private static final float ORIGIN_Y = 40;
    private static final float ORIGIN_Z = 19;
    /** The trolley's foot, where the cable leaves it, and the grab's height. */
    private static final float TROLLEY_FOOT = 45;
    private static final float GRAB_HEIGHT = 3;
    /**
     * Where the crane drops rods: into the tilted-open chute hatch in the west wall, under the output
     * port (pool_concept.py), the rod's foot down inside the hatch's mouth (the grab 10 pixels above it).
     */
    private static final float OUT_X = 9;
    private static final float OUT_Z = 24;
    private static final float OUT_GRAB = 35.5F;
    /** The run's phases, in ticks from its start. */
    private static final float[] PHASES = {30, 45, 60, 100, 115, 130, 170};

    private static ModelResourceLocation crane(String part) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/pool/crane_" + part));
    }

    public PoolRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Smooth from a to b as f goes 0 to 1. */
    private static float ease(float a, float b, float f) {
        f = Mth.clamp(f, 0, 1);
        return a + (b - a) * f * f * (3 - 2 * f);
    }

    /**
     * Where the crane is {@code t} ticks into a run for rack {@code slot}: the bridge's x, the
     * trolley's z, the grab's foot, and whether it carries a rod.
     */
    private static float[] crane(float t, int slot) {
        float cellX = PoolLayout.RACK_X + (slot % PoolLayout.COLUMNS) * PoolLayout.CELL + 5;
        float cellZ = PoolLayout.RACK_Z + (slot / PoolLayout.COLUMNS) * PoolLayout.CELL + 5;
        float down = PoolLayout.ROD_TOP;
        float[] p = PHASES;
        if (t < 0 || t >= p[6]) {
            return new float[] {PARK_X, PARK_Z, PARK_GRAB, 0};
        }
        if (t < p[0]) {
            float f = t / p[0];
            return new float[] {ease(PARK_X, cellX, f), ease(PARK_Z, cellZ, f), PARK_GRAB, 0};
        }
        if (t < p[1]) {
            return new float[] {cellX, cellZ, ease(PARK_GRAB, down, (t - p[0]) / (p[1] - p[0])), 0};
        }
        if (t < p[2]) {
            return new float[] {cellX, cellZ, ease(down, PARK_GRAB, (t - p[1]) / (p[2] - p[1])), 1};
        }
        if (t < p[3]) {
            float f = (t - p[2]) / (p[3] - p[2]);
            return new float[] {ease(cellX, OUT_X, f), ease(cellZ, OUT_Z, f), PARK_GRAB, 1};
        }
        if (t < p[4]) {
            return new float[] {OUT_X, OUT_Z, ease(PARK_GRAB, OUT_GRAB, (t - p[3]) / (p[4] - p[3])), 1};
        }
        if (t < p[5]) {
            return new float[] {OUT_X, OUT_Z, ease(OUT_GRAB, PARK_GRAB, (t - p[4]) / (p[5] - p[4])), 0};
        }
        float f = (t - p[5]) / (p[6] - p[5]);
        return new float[] {ease(OUT_X, PARK_X, f), ease(OUT_Z, PARK_Z, f), PARK_GRAB, 0};
    }

    /** The crane, placed by its run, in a pose already turned to the pool's facing (block units). */
    private static void drawCrane(PoolControllerBlockEntity pool, float partialTick, PoseStack pose, MultiBufferSource buffers,
                                  int light, int overlay) {
        float[] at = crane(pool.craneTime(partialTick), pool.craneSlot());
        float x = at[0] - ORIGIN_X;
        float z = at[1] - PARK_Z;
        float grab = at[2] - PARK_GRAB;
        part(BRIDGE, pool, pose, buffers, light, overlay, x, 0, 0, 1);
        part(TROLLEY, pool, pose, buffers, light, overlay, x, 0, z, 1);
        part(GRAB, pool, pose, buffers, light, overlay, x, grab, z, 1);
        if (at[3] > 0) {
            part(ROD, pool, pose, buffers, light, overlay, x, grab, z, 1);
        }
        // The cable, stretched from the grab's top up to the trolley.
        float top = at[2] + GRAB_HEIGHT;
        part(CABLE, pool, pose, buffers, light, overlay, x, top - ORIGIN_Y, z, TROLLEY_FOOT - top);
    }

    /** One crane part, its origin moved by (dx, dy, dz) pixels from the park, stretched {@code height} times upward. */
    private static void part(ModelResourceLocation part, PoolControllerBlockEntity pool, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay, float dx, float dy, float dz, float height) {
        Minecraft minecraft = Minecraft.getInstance();
        pose.pushPose();
        pose.translate((ORIGIN_X + dx) / 16, (ORIGIN_Y + dy) / 16, (ORIGIN_Z + dz) / 16);
        pose.scale(1, height, 1);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
                pool.getBlockState(), minecraft.getModelManager().getModel(part), 1, 1, 1, light, overlay, ModelData.EMPTY,
                RenderType.cutout());
        pose.popPose();
    }

    @Override
    public void render(PoolControllerBlockEntity pool, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!pool.isFormed()) {
            return;
        }
        TextureAtlasSprite side = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SIDE);
        TextureAtlasSprite top = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TOP);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        Direction facing = pool.getBlockState().getValue(PoolPartBlock.FACING);
        pose.pushPose();
        // The design faces north; turn it as the blockstate turns the models, about the block's centre.
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(((int) facing.toYRot() + 180) % 360)));
        pose.translate(-0.5, 0, -0.5);
        pose.translate(-CONTROLLER_X / 16, -CONTROLLER_Y / 16, 0);
        drawCrane(pool, partialTick, pose, buffers, light, overlay);
        pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
        PoseStack.Pose matrix = pose.last();
        int block = LightTexture.block(light);
        int sky = LightTexture.sky(light);
        for (int slot = 0; slot < PoolControllerBlockEntity.RACKS; slot++) {
            int cooled = pool.rod(slot);
            if (cooled < 0) {
                continue;
            }
            float heat = 1 - Mth.clamp(cooled / 1000F, 0, 1);
            int colour = 0xFF000000 | Mth.lerpInt(heat, 255, HOT_R) << 16 | Mth.lerpInt(heat, 255, HOT_G) << 8 | Mth.lerpInt(heat, 255, HOT_B);
            int glow = LightTexture.pack(Math.max(block, Math.round(15 * heat)), sky);
            float x = RACK_X + (slot % COLUMNS) * CELL + 3;
            float z = RACK_Z + (slot / COLUMNS) * CELL + 3;
            rod(buffer, matrix, side, top, colour, glow, overlay, x, z);
        }
        pose.popPose();
    }

    /** One rod, 4 x 4 across, its faces mapped from the texture's top left, one texel per pixel. */
    private static void rod(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite side, TextureAtlasSprite top,
                            int colour, int light, int overlay, float x1, float z1) {
        float x2 = x1 + 4;
        float z2 = z1 + 4;
        float y1 = ROD_BOTTOM;
        float y2 = ROD_TOP;
        float h = y2 - y1;
        float u1 = side.getU(0);
        float u2 = side.getU(4 / 16F);
        float v1 = side.getV(0);
        float v2 = side.getV(h / 16F);
        quad(buffer, pose, colour, light, overlay, Direction.NORTH, new float[][] {{x2, y2, z1}, {x1, y2, z1}, {x1, y1, z1}, {x2, y1, z1}}, u1, v1, u2, v2);
        quad(buffer, pose, colour, light, overlay, Direction.SOUTH, new float[][] {{x1, y2, z2}, {x2, y2, z2}, {x2, y1, z2}, {x1, y1, z2}}, u1, v1, u2, v2);
        quad(buffer, pose, colour, light, overlay, Direction.WEST, new float[][] {{x1, y2, z1}, {x1, y2, z2}, {x1, y1, z2}, {x1, y1, z1}}, u1, v1, u2, v2);
        quad(buffer, pose, colour, light, overlay, Direction.EAST, new float[][] {{x2, y2, z2}, {x2, y2, z1}, {x2, y1, z1}, {x2, y1, z2}}, u1, v1, u2, v2);
        quad(buffer, pose, colour, light, overlay, Direction.UP, new float[][] {{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}},
                top.getU(0), top.getV(0), top.getU(4 / 16F), top.getV(4 / 16F));
    }

    /**
     * A face from its four corners, top left first and going round clockwise as seen from outside,
     * so the texture's top left lands on the first corner.
     */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int colour, int light, int overlay, Direction face,
                             float[][] corners, float u1, float v1, float u2, float v2) {
        float[][] uvs = {{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}};
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, corners[i][0], corners[i][1], corners[i][2])
                    .setColor(colour)
                    .setUv(uvs[i][0], uvs[i][1])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }

    /** The whole pool, so the rods still draw when the controller itself is off screen. */
    @Override
    public AABB getRenderBoundingBox(PoolControllerBlockEntity pool) {
        return new AABB(pool.getBlockPos()).inflate(4, 2, 4);
    }
}
