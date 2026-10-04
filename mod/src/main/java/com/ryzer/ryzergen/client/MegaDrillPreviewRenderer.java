package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.drill.MegaDrillPreviewBlock;
import com.ryzer.ryzergen.machine.drill.MegaDrillPreviewBlockEntity;
import com.ryzer.ryzergen.machine.drill.MegaDrillPreviewBlockEntity.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the mega drill's concept design (art/tools/mega_drill_concept.py) round its preview block,
 * and its cycle. The body goes the fission station's way: a GPU mesh built once, or the plain draw
 * while a shader pack is on. What changes when it runs (the lamps, the light lines) comes from the
 * design's 'on' or 'off' group, by the block's redstone signal. Then, from the phase the block entity
 * syncs: the charge running down the drive shaft's ribs from the top, the lens swelling as it
 * arrives, the beam shooting down to the layer being mined, and the heat over that layer, drawn over
 * every block's own model (HeatDecal) so any block, modded ones too, turns red hot: a tint that
 * reddens and darkens it, then glowing cracks that widen through four stages until it bursts.
 */
public class MegaDrillPreviewRenderer implements BlockEntityRenderer<MegaDrillPreviewBlockEntity> {
    /** Half the design's footprint, in blocks (it is 9 across, centred on the middle block). */
    private static final float HALF = 4.5F;
    private static final float HEIGHT = 16;
    // The design's parts the cycle lights, in its pixels (see mega_drill_concept.py).
    private static final float C = 72;
    private static final float SHAFT_TOP = 168;
    private static final float SHAFT_BOTTOM = 72;
    private static final float SHAFT_HALF = 8;
    /** The shaft's two ribs on each face lie this far either side of its middle. */
    private static final float RIB_IN = 2;
    private static final float RIB_OUT = 4;
    private static final float LENS_Y = 37.5F;
    private static final float BEAM_TOP = 36;
    /** The share of the charge spent running down the shaft; the rest swells the lens. */
    private static final float SHAFT_SHARE = 0.8F;
    /** Ticks the beam takes to reach the ground once it fires. */
    private static final float SHOOT_TICKS = 4;

    private static final ResourceLocation CHARGE = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/drill/charge");
    private static final ResourceLocation CHARGE_HEAD = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/drill/charge_head");
    private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "block/drill/beam");
    private static final ResourceLocation HEAT_TINT = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/effect/heat_tint.png");
    private static final ResourceLocation[] HEAT = new ResourceLocation[4];

    static {
        for (int i = 0; i < HEAT.length; i++) {
            HEAT[i] = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "textures/effect/heat_" + i + ".png");
        }
    }

    public MegaDrillPreviewRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MegaDrillPreviewBlockEntity preview, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = preview.getBlockState().getValue(MegaDrillPreviewBlock.FACING);
        boolean running = preview.getBlockState().getValue(MegaDrillPreviewBlock.POWERED);
        var body = StationGeometry.group(StationGeometry.MEGA_DRILL, "static");
        if (!StationMesh.shadersInUse()) {
            StationMesh mesh = preview.clientMesh instanceof StationMesh cached && cached.matches(light, facing) ? cached : null;
            if (mesh == null) {
                if (preview.clientMesh instanceof StationMesh old) {
                    old.close();
                }
                PoseStack local = new PoseStack();
                place(local, facing);
                mesh = StationMesh.build(body, local, light, facing);
                preview.clientMesh = mesh;
            }
            mesh.draw(pose.last().pose());
        }
        Level level = preview.getLevel();
        long time = level == null ? 0 : level.getGameTime();
        Phase phase = running ? preview.phase() : Phase.IDLE;
        float progress = preview.progress(time, partialTick);

        pose.pushPose();
        place(pose, facing);
        VertexConsumer cutout = buffers.getBuffer(Sheets.cutoutBlockSheet());
        if (StationMesh.shadersInUse()) {
            StationRenderer.draw(cutout, pose, body, light);
        }
        StationRenderer.draw(cutout, pose, StationGeometry.group(StationGeometry.MEGA_DRILL, running ? "on" : "off"), light);
        float shaft = phase == Phase.CHARGING ? Math.min(1, progress / SHAFT_SHARE) : phase == Phase.FIRING ? 1 : 0;
        if (shaft > 0) {
            StationRenderer.draw(cutout, pose, charge(shaft), light);
        }
        float lens = phase == Phase.CHARGING ? Math.max(0, (progress - SHAFT_SHARE) / (1 - SHAFT_SHARE))
                : phase == Phase.FIRING ? 0.85F + 0.15F * Mth.sin((time + partialTick) * 1.7F) : 0;
        if (lens > 0) {
            float s = 2.6F + 1.6F * lens;
            StationRenderer.draw(cutout, pose, StationGeometry.box(C - s, LENS_Y - s * 0.6F, C - s, C + s, LENS_Y + s * 0.6F, C + s,
                    CHARGE_HEAD, true), light);
        }
        float bottom = 0;
        if (phase == Phase.FIRING) {
            float ground = (preview.layer() + 1 - preview.getBlockPos().getY()) * 16;
            bottom = Mth.lerp(Math.min(1, preview.elapsed(time, partialTick) / SHOOT_TICKS), BEAM_TOP, ground);
            StationRenderer.draw(cutout, pose, StationGeometry.box(C - 1.2F, bottom, C - 1.2F, C + 1.2F, BEAM_TOP, C + 1.2F, BEAM, true), light);
        }
        pose.popPose();

        if (phase == Phase.FIRING && level != null) {
            heat(preview, level, pose, buffers, progress);
            // A soft halo round the beam, light added (drawn after the heat, which shares no buffer with it).
            pose.pushPose();
            place(pose, facing);
            StationRenderer.draw(buffers.getBuffer(RenderType.eyes(InventoryMenu.BLOCK_ATLAS)), pose,
                    StationGeometry.box(C - 2.4F, bottom, C - 2.4F, C + 2.4F, BEAM_TOP, C + 2.4F, BEAM, true), light, 0xFF4A6A80);
            pose.popPose();
        }
    }

    /** The charge on the drive shaft's ribs, lit from the top down to `fraction` of the way, a white-hot head at its front. */
    private static List<StationGeometry.Quad> charge(float fraction) {
        float front = SHAFT_TOP - (SHAFT_TOP - SHAFT_BOTTOM) * fraction;
        List<StationGeometry.Quad> quads = new ArrayList<>();
        for (int side = -1; side <= 1; side += 2) {
            float a1 = Math.min(C + side * RIB_IN, C + side * RIB_OUT);
            float a2 = Math.max(C + side * RIB_IN, C + side * RIB_OUT);
            for (int face = -1; face <= 1; face += 2) {
                for (float proud : new float[] {0.15F, 0.3F}) {
                    boolean head = proud > 0.2F;
                    if (head && fraction >= 1) {
                        continue;
                    }
                    float f1 = Math.min(C + face * SHAFT_HALF, C + face * (SHAFT_HALF + proud));
                    float f2 = Math.max(C + face * SHAFT_HALF, C + face * (SHAFT_HALF + proud));
                    float top = head ? Math.min(SHAFT_TOP, front + 3) : SHAFT_TOP;
                    ResourceLocation texture = head ? CHARGE_HEAD : CHARGE;
                    quads.addAll(StationGeometry.box(a1, front, f1, a2, top, f2, texture, true));
                    quads.addAll(StationGeometry.box(f1, front, a1, f2, top, a2, texture, true));
                }
            }
        }
        return quads;
    }

    /**
     * The heat over the layer being mined, `heat` from 0 (the beam just struck) to 1 (it bursts): first
     * every block's tint (multiplied into it, reddening and darkening it), then its glowing cracks (light
     * added), each in its own pass, as the two use different buffers.
     */
    private static void heat(MegaDrillPreviewBlockEntity preview, Level level, PoseStack pose, MultiBufferSource buffers, float heat) {
        if (heat < 0.02F) {
            return;
        }
        BlockPos origin = preview.getBlockPos();
        int y = preview.layer();
        List<BlockPos> blocks = new ArrayList<>();
        for (int dx = MegaDrillPreviewBlockEntity.LOW; dx <= MegaDrillPreviewBlockEntity.HIGH; dx++) {
            for (int dz = MegaDrillPreviewBlockEntity.LOW; dz <= MegaDrillPreviewBlockEntity.HIGH; dz++) {
                BlockPos at = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                BlockState state = level.getBlockState(at);
                if (!MegaDrillPreviewBlockEntity.footing(dx, dz) && !at.equals(origin) && state.getRenderShape() == RenderShape.MODEL) {
                    blocks.add(at);
                }
            }
        }
        // The tint, as a multiply blend (2 x tint x block): red kept, green and blue pulled down.
        int tint = FastColor.ARGB32.colorFromFloat(1, 0.5F + 0.05F * heat, 0.5F - 0.29F * heat, 0.5F - 0.34F * heat);
        layer(blocks, level, origin, pose, buffers.getBuffer(RenderType.crumbling(HEAT_TINT)), tint);
        int stage = Math.min(HEAT.length - 1, (int) (heat * HEAT.length));
        float within = heat * HEAT.length - stage;
        float glow = Math.min(1, 0.6F + 0.4F * within);
        layer(blocks, level, origin, pose, buffers.getBuffer(RenderType.eyes(HEAT[stage])), FastColor.ARGB32.colorFromFloat(1, glow, glow, glow));
    }

    private static void layer(List<BlockPos> blocks, Level level, BlockPos origin, PoseStack pose, VertexConsumer buffer, int colour) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (BlockPos at : blocks) {
            pose.pushPose();
            pose.translate(at.getX() - origin.getX(), at.getY() - origin.getY(), at.getZ() - origin.getZ());
            // A hair larger than the block, so the decal never fights its faces for depth.
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(1.004F, 1.004F, 1.004F);
            pose.translate(-0.5, -0.5, -0.5);
            int variant = (int) (Mth.getSeed(at) >>> 7) & 7;
            dispatcher.renderBreakingTexture(level.getBlockState(at), at, level, pose, new HeatDecal(buffer, pose.last(), colour, variant));
            pose.popPose();
        }
    }

    /** The design faces north (the ladder at its low z); turn it about the block to the block's facing. */
    private static void place(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        pose.translate(-HALF, 0, -HALF);
    }

    /** The design, and down to the layer being mined. */
    @Override
    public AABB getRenderBoundingBox(MegaDrillPreviewBlockEntity preview) {
        var pos = preview.getBlockPos();
        double low = Math.min(pos.getY(), preview.layer()) - 1;
        return new AABB(pos.getX() + 0.5 - 8, low, pos.getZ() + 0.5 - 8,
                pos.getX() + 0.5 + 9, pos.getY() + HEIGHT, pos.getZ() + 0.5 + 9);
    }

    @Override
    public boolean shouldRenderOffScreen(MegaDrillPreviewBlockEntity preview) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
