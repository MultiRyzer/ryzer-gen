package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.assembler.AssemblerBlockEntity;
import com.ryzer.ryzergen.machine.assembler.AssemblerBlockEntity.Status;
import com.ryzer.ryzergen.machine.assembler.AssemblerPartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

import static com.ryzer.ryzergen.machine.assembler.AssemblerLayout.*;

/**
 * Brings a formed Assembler to life: the arm, the items it works with, and the lamps. The arm is six
 * models of its own (art/tools/assembler_concept.py, ARM_PARTS), each about its joint: the turret
 * turns to face where the grip must go, and the upper arm and forearm pitch to reach it (two links,
 * elbow up), with the tool hanging plumb from the wrist. Through a cycle it fetches each ingredient
 * from the in-feed belt and lays it on its fixture, as the recipe places it, half a second a trip;
 * then the grid becomes the result, which it carries to the out-feed belt. The belts carry the items
 * waiting in the buffer and the output. The rack shows each pattern's result on its cartridge, its
 * lamp cyan (green for the one being made); the bed's fixtures glow red where the pattern the cell
 * waits on lacks its item; the stack light shows the cell's state.
 */
public class AssemblerRenderer implements BlockEntityRenderer<AssemblerBlockEntity> {
    private static final ResourceLocation LAMP = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/machine/family/lamp");
    public static final ModelResourceLocation TURRET = part("turret");
    public static final ModelResourceLocation UPPER_ARM = part("upper");
    public static final ModelResourceLocation FOREARM = part("fore");
    public static final ModelResourceLocation TOOL = part("tool");
    public static final ModelResourceLocation FINGER_WEST = part("finger_west");
    public static final ModelResourceLocation FINGER_EAST = part("finger_east");
    public static final List<ModelResourceLocation> PARTS = List.of(TURRET, UPPER_ARM, FOREARM, TOOL, FINGER_WEST, FINGER_EAST);
    /** The forearm's model origin sits this far in front of the elbow (assembler_concept.py, ARM_PARTS). */
    private static final float FORE_ORIGIN = 16;
    /** The controller's block corner in the design (its cell, 1 across, times 16). */
    private static final float CONTROLLER_X = 16;
    private static final int RED = 0xFFFF4D3D;
    private static final int AMBER = 0xFFF0A030;
    private static final int GREEN = 0xFF44D65E;
    private static final int CYAN = 0xFF35C8F5;
    /** Items are drawn only this near (blocks), as a dropped item would be. */
    private static final double ITEM_RANGE = 32;
    private static final float HOVER = 6;
    private static final int T = AssemblerBlockEntity.TRIP_TICKS;
    /** Pixels a tick, as the belt texture runs (a pixel every two ticks). */
    private static final float BELT_SPEED = 0.5F;

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/assembler/arm_" + name));
    }

    public AssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static float ease(float a, float b, float f) {
        f = Mth.clamp(f, 0, 1);
        return a + (b - a) * f * f * (3 - 2 * f);
    }

    private static Vec3 ease(Vec3 a, Vec3 b, float f) {
        return new Vec3(ease((float) a.x, (float) b.x, f), ease((float) a.y, (float) b.y, f), ease((float) a.z, (float) b.z, f));
    }

    private static Vec3 fixture(int cell) {
        return new Vec3(FIX_X[cell % 3], BED_TOP + 0.5, FIX_Z[cell / 3]);
    }

    private static Vec3 up(Vec3 p) {
        return p.add(0, HOVER, 0);
    }

    private static final Vec3 REST = new Vec3(FIX_X[1], BED_TOP + HOVER + 2, FIX_Z[1]);
    private static final Vec3 PICK = new Vec3(IN_BELT_X, BELT_TOP + 1, PICK_Z);
    private static final Vec3 DROP = new Vec3(OUT_BELT_X, BELT_TOP + 1, PICK_Z);

    /** Where the grip is in a cycle, and what it holds then: 0 nothing, 1 an ingredient, 2 the result. */
    private record Pose(Vec3 grip, int holding, int holdingCell) {}

    /** The cells a pattern lays out, in the order the arm fetches them. */
    private static List<Integer> cells(AssemblerBlockEntity cell, int pattern) {
        List<Integer> out = new ArrayList<>();
        for (int c = 0; c < AssemblerBlockEntity.GRID; c++) {
            if (!cell.ghost(pattern, c).isEmpty()) {
                out.add(c);
            }
        }
        return out;
    }

    private static Pose pose(List<Integer> order, float t) {
        int n = order.size();
        int k = Math.min((int) (t / T), n);
        float u = (t - k * T) / T;
        if (k < n) {
            Vec3 from = k == 0 ? REST : up(fixture(order.get(k - 1)));
            Vec3 to = fixture(order.get(k));
            if (u < 0.35F) {
                return new Pose(ease(from, up(PICK), u / 0.35F), 0, -1);
            }
            if (u < 0.45F) {
                return new Pose(ease(up(PICK), PICK, (u - 0.35F) / 0.1F), 0, -1);
            }
            if (u < 0.55F) {
                return new Pose(ease(PICK, up(PICK), (u - 0.45F) / 0.1F), 1, order.get(k));
            }
            if (u < 0.9F) {
                return new Pose(ease(up(PICK), up(to), (u - 0.55F) / 0.35F), 1, order.get(k));
            }
            return new Pose(ease(up(to), to, (u - 0.9F) / 0.1F), 1, order.get(k));
        }
        // The last trip: fetch the result off the middle fixture and carry it out.
        Vec3 from = n == 0 ? REST : up(fixture(order.get(n - 1)));
        Vec3 centre = fixture(4);
        if (u < 0.2F) {
            return new Pose(ease(from, up(centre), u / 0.2F), 0, -1);
        }
        if (u < 0.3F) {
            return new Pose(ease(up(centre), centre, (u - 0.2F) / 0.1F), 0, -1);
        }
        if (u < 0.4F) {
            return new Pose(ease(centre, up(centre), (u - 0.3F) / 0.1F), 2, -1);
        }
        if (u < 0.85F) {
            return new Pose(ease(up(centre), up(DROP), (u - 0.4F) / 0.45F), 2, -1);
        }
        return new Pose(ease(up(DROP), DROP, (u - 0.85F) / 0.15F), 2, -1);
    }

    @Override
    public void render(AssemblerBlockEntity cell, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!cell.isFormed() || cell.getLevel() == null) {
            return;
        }
        Direction facing = cell.getBlockState().getValue(AssemblerPartBlock.FACING);
        pose.pushPose();
        // The design faces north; turn it as the blockstate turns the models, about the block's centre.
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-(((int) facing.toYRot() + 180) % 360)));
        pose.translate(-0.5, 0, -0.5);
        pose.translate(-CONTROLLER_X / 16, 0, 0);

        int active = cell.active();
        float t = cell.cycleTime(partialTick);
        List<Integer> order = active >= 0 ? cells(cell, active) : List.of();
        Pose at;
        if (active >= 0 && t < cell.total()) {
            at = pose(order, t);
        } else if (active >= 0) {
            // Set down: waiting over the out-feed for the next cycle.
            at = new Pose(up(DROP), 0, -1);
        } else {
            // Idle: parked over the bed, swaying a little.
            float time = cell.getLevel().getGameTime() + partialTick;
            at = new Pose(REST.add(Math.sin(time / 40) * 0.6, 0, 0), 0, -1);
        }
        boolean items = Config.get(Config.ASSEMBLER_SHOW_ITEMS) && Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
                .distanceToSqr(Vec3.atCenterOf(cell.getBlockPos())) < ITEM_RANGE * ITEM_RANGE;
        ItemStack held = ItemStack.EMPTY;
        if (items && at.holding() == 1) {
            held = cell.laid(at.holdingCell());
        } else if (items && at.holding() == 2) {
            held = cell.syncedResult();
        }
        drawArm(cell, pose, buffers, light, overlay, at.grip(), held);

        if (items) {
            // What is laid on the bed so far, and the result once the grid has become it.
            int n = order.size();
            if (active >= 0 && t < n * T) {
                for (int i = 0; i < n; i++) {
                    if (t >= (i + 1) * T) {
                        flatItem(cell, pose, buffers, light, cell.laid(order.get(i)), fixture(order.get(i)), 0.28F);
                    }
                }
            } else if (active >= 0 && at.holding() != 2 && t < n * T + 0.3F * T) {
                flatItem(cell, pose, buffers, light, cell.syncedResult(), fixture(4), 0.36F);
            }
            belts(cell, pose, buffers, light, partialTick, order, active >= 0 ? t : -1);
            rack(cell, pose, buffers, light);
        }
        lamps(cell, pose, buffers, overlay);
        pose.popPose();
    }

    /** The arm, reaching for {@code grip} (design pixels), carrying {@code held}. */
    private static void drawArm(AssemblerBlockEntity cell, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                Vec3 grip, ItemStack held) {
        double dx = grip.x - PIVOT_X;
        double dz = grip.z - PIVOT_Z;
        float yaw = (float) Math.atan2(-dx, -dz);
        double reach = Math.hypot(dx, dz);
        // Two links, from the shoulder to the wrist, which sits the grip's length above the grip.
        double wx = reach;
        double wy = grip.y + GRIP - SHOULDER_Y;
        double d = Mth.clamp(Math.hypot(wx, wy), Math.abs(UPPER - FORE) + 0.01, UPPER + FORE - 0.01);
        double toWrist = Math.atan2(wx, wy);
        double inner = Math.acos(Mth.clamp((UPPER * UPPER + d * d - FORE * FORE) / (2 * UPPER * d), -1, 1));
        float lean = (float) (toWrist - inner);
        double ex = UPPER * Math.sin(lean);
        double ey = UPPER * Math.cos(lean);
        float pitch = (float) Math.atan2(wy - ey, wx - ex);

        pose.pushPose();
        pose.translate(PIVOT_X / 16, TURRET_Y / 16, PIVOT_Z / 16);
        pose.mulPose(Axis.YP.rotation(yaw));
        model(TURRET, cell, pose, buffers, light, overlay);
        pose.translate(0, (SHOULDER_Y - TURRET_Y) / 16, 0);
        pose.mulPose(Axis.XP.rotation(-lean));
        model(UPPER_ARM, cell, pose, buffers, light, overlay);
        pose.translate(0, UPPER / 16, 0);
        pose.mulPose(Axis.XP.rotation(pitch + lean));
        pose.pushPose();
        pose.translate(0, 0, -FORE_ORIGIN / 16);
        model(FOREARM, cell, pose, buffers, light, overlay);
        pose.popPose();
        pose.translate(0, 0, -FORE / 16);
        pose.mulPose(Axis.XP.rotation(-pitch));
        model(TOOL, cell, pose, buffers, light, overlay);
        // The fingers close on what they carry.
        float open = held.isEmpty() ? 0.6F : 0;
        pose.pushPose();
        pose.translate(-open / 16, 0, 0);
        model(FINGER_WEST, cell, pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(open / 16, 0, 0);
        model(FINGER_EAST, cell, pose, buffers, light, overlay);
        pose.popPose();
        if (!held.isEmpty()) {
            pose.translate(0, -(GRIP + 0.5F) / 16, 0);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(0.56F, 0.56F, 0.56F);
            Minecraft.getInstance().getItemRenderer().renderStatic(held, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, cell.getLevel(), 0);
        }
        pose.popPose();
    }

    private static void model(ModelResourceLocation part, AssemblerBlockEntity cell, PoseStack pose, MultiBufferSource buffers,
                              int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()),
                cell.getBlockState(), minecraft.getModelManager().getModel(part), 1, 1, 1, light, overlay, ModelData.EMPTY,
                RenderType.cutout());
    }

    /** An item lying flat at a point of the design (pixels), {@code size} of a block across. */
    private static void flatItem(AssemblerBlockEntity cell, PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack,
                                 Vec3 at, float size) {
        if (stack.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(at.x / 16, (at.y - 0.3) / 16, at.z / 16);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        // A fixed item is drawn half a block across.
        pose.scale(size * 2, size * 2, size * 2);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, cell.getLevel(), 0);
        pose.popPose();
    }

    /**
     * The belts, carrying what the cycle really uses: each ingredient comes out of the in-feed's hood
     * and rides up to the pick point, arriving just as the arm reaches down for it (the belt moves at
     * its texture's speed), and each result the arm sets on the out-feed rides once into the hood.
     */
    private static void belts(AssemblerBlockEntity cell, PoseStack pose, MultiBufferSource buffers, int light, float partialTick,
                              List<Integer> order, float t) {
        if (t >= 0) {
            for (int k = 0; k < order.size(); k++) {
                // The arm closes on ingredient k 0.45 of the way into its trip.
                float arrives = (k + 0.45F) * T;
                if (arrives <= t) {
                    continue;
                }
                float z = PICK_Z - BELT_SPEED * (arrives - t);
                if (z > BELT_FRONT) {
                    flatItem(cell, pose, buffers, light, cell.laid(order.get(k)), new Vec3(IN_BELT_X, BELT_TOP + 0.5, z), 0.3F);
                }
            }
        }
        float now = cell.getLevel().getGameTime() + partialTick;
        for (AssemblerBlockEntity.Drop drop : cell.drops()) {
            float z = PICK_Z - BELT_SPEED * (now - drop.at());
            if (z > BELT_FRONT) {
                flatItem(cell, pose, buffers, light, drop.stack(), new Vec3(OUT_BELT_X, BELT_TOP + 0.5, z), 0.3F);
            }
        }
    }

    /** Each pattern's result on its cartridge in the rack, facing the front. */
    private static void rack(AssemblerBlockEntity cell, PoseStack pose, MultiBufferSource buffers, int light) {
        for (int p = 0; p < AssemblerBlockEntity.PATTERNS; p++) {
            ItemStack result = cell.result(p);
            if (result.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(cartX(p) / 16, CART_Y[p / 3] / 16, (RACK_FACE - 0.1F) / 16);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.scale(0.5F, 0.5F, 0.05F);
            Minecraft.getInstance().getItemRenderer().renderStatic(result, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffers, cell.getLevel(), 0);
            pose.popPose();
        }
    }

    /** A cartridge's middle across: the rack's face is seen from the front, so its first column is the east one. */
    private static float cartX(int pattern) {
        return CART_X[2 - pattern % 3];
    }

    /** The lamps: the cartridges', the bed's fixtures the cell lacks, and the stack light. */
    private static void lamps(AssemblerBlockEntity cell, PoseStack pose, MultiBufferSource buffers, int overlay) {
        TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LAMP);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        pose.pushPose();
        pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
        PoseStack.Pose matrix = pose.last();
        for (int p = 0; p < AssemblerBlockEntity.PATTERNS; p++) {
            if (cell.result(p).isEmpty()) {
                continue;
            }
            int colour = p == cell.active() ? GREEN : p == cell.waiting() ? AMBER : CYAN;
            float x = cartX(p) - 2.5F;
            float y = CART_Y[p / 3] - 2.5F;
            box(buffer, matrix, white, colour, overlay, x - 0.5F, y - 0.5F, RACK_FACE - 0.05F, x + 0.5F, y + 0.5F, RACK_FACE);
        }
        if (cell.status() == Status.STARVED && cell.waiting() >= 0) {
            boolean on = (cell.getLevel().getGameTime() / 10) % 2 == 0;
            for (int c = 0; c < AssemblerBlockEntity.GRID && on; c++) {
                if ((cell.missingMask() & (1 << c)) != 0) {
                    Vec3 f = fixture(c);
                    box(buffer, matrix, white, RED, overlay, (float) f.x - 1, BED_TOP, (float) f.z - 1, (float) f.x + 1, BED_TOP + 0.1F,
                            (float) f.z + 1);
                }
            }
        }
        int lamp = switch (cell.status()) {
            case WORKING -> 2;
            case STARVED, OUTPUT_FULL, REDSTONE -> 1;
            case NO_POWER -> 0;
            default -> -1;
        };
        if (lamp >= 0) {
            int colour = lamp == 2 ? GREEN : lamp == 1 ? AMBER : RED;
            float y = STACK_Y[lamp];
            box(buffer, matrix, white, colour, overlay, STACK[0] - 0.05F, y - 0.05F, STACK[1] - 0.05F, STACK[2] + 0.05F, y + 2.05F,
                    STACK[3] + 0.05F);
        }
        pose.popPose();
    }

    /** A lit box, full bright, its texture the plain lamp tinted. */
    private static void box(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, int colour, int overlay,
                            float x1, float y1, float z1, float x2, float y2, float z2) {
        float u1 = sprite.getU(0);
        float v1 = sprite.getV(0);
        float u2 = sprite.getU(1 / 16F);
        float v2 = sprite.getV(1 / 16F);
        int light = LightTexture.FULL_BRIGHT;
        quad(buffer, pose, colour, light, overlay, Direction.NORTH, new float[][] {{x2, y2, z1}, {x1, y2, z1}, {x1, y1, z1}, {x2, y1, z1}}, u1, v1, u2, v2);
        quad(buffer, pose, colour, light, overlay, Direction.SOUTH, new float[][] {{x1, y2, z2}, {x2, y2, z2}, {x2, y1, z2}, {x1, y1, z2}}, u1, v1, u2, v2);
        quad(buffer, pose, colour, light, overlay, Direction.WEST, new float[][] {{x1, y2, z1}, {x1, y2, z2}, {x1, y1, z2}, {x1, y1, z1}}, u1, v1, u2, v2);
        quad(buffer, pose, colour, light, overlay, Direction.EAST, new float[][] {{x2, y2, z2}, {x2, y2, z1}, {x2, y1, z1}, {x2, y1, z2}}, u1, v1, u2, v2);
        quad(buffer, pose, colour, light, overlay, Direction.UP, new float[][] {{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}}, u1, v1, u2, v2);
    }

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

    /** The whole cell, so the arm still draws when the controller itself is off screen. */
    @Override
    public AABB getRenderBoundingBox(AssemblerBlockEntity cell) {
        return new AABB(cell.getBlockPos()).inflate(3, 3, 3);
    }
}
