package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.breeder.BreederCoreBlockEntity;
import com.ryzer.ryzergen.machine.breeder.BreederLayout;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;

/**
 * Draws a formed breeder reactor from its concept design (art/tools/breeder_concept.py, exported to
 * assets/ryzergen/breeder/breeder.json): the static body from a GPU mesh, as the fission station's
 * is (or the plain draw while a shader pack is on), and the beacon on the platform, turned about
 * its mast every frame. The design is drawn facing north from its north-west corner, then turned
 * about the footprint's centre to the core's facing.
 */
public class BreederRenderer implements BlockEntityRenderer<BreederCoreBlockEntity> {
    private static final ResourceLocation DATA = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "breeder/breeder.json");
    private static final float HALF = BreederLayout.SIZE / 2F;
    /** The beacon mast's axis in the design, in blocks: 10 pixels in front of the centre. */
    private static final float BEACON_X = HALF;
    private static final float BEACON_Z = HALF - 10 / 16F;
    /** A cell inside the sphere, open air within the shell, where the lantern's light is read. */
    private static final BlockPos SPHERE_MIDDLE = new BlockPos(BreederLayout.SIZE / 2, 4, BreederLayout.SIZE / 2);

    public BreederRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BreederCoreBlockEntity core, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!core.isFormed()) {
            return;
        }
        light = StationRenderer.openLight(core, BreederLayout.HEIGHT + 1, light);
        Direction facing = core.facing();
        // The whole reactor is drawn with one light. The station's glass glows right beside its core,
        // but the breeder's lantern is high up, so take the block light at the sphere's middle too:
        // running, the lantern lights the reactor as the station's glass lights the station.
        BlockPos middle = BreederLayout.toWorld(core.getBlockPos(), facing, SPHERE_MIDDLE);
        int glow = core.getLevel().getBrightness(LightLayer.BLOCK, middle);
        light = LightTexture.pack(Math.max(LightTexture.block(light), glow), LightTexture.sky(light));
        // The lights run with the reactor: each group's lit quads while it runs, the same quads dark
        // while it is off.
        String lights = core.isRunning() ? "_on" : "_off";
        var body = StationGeometry.group(DATA, "static");
        if (!StationMesh.shadersInUse()) {
            StationMesh mesh = core.clientMesh instanceof StationMesh cached && cached.matches(light, facing) ? cached : null;
            if (mesh == null) {
                if (core.clientMesh instanceof StationMesh old) {
                    old.close();
                }
                PoseStack local = new PoseStack();
                place(local, facing);
                mesh = StationMesh.build(body, local, light, facing);
                core.clientMesh = mesh;
            }
            mesh.draw(pose.last().pose());
        }
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        pose.pushPose();
        place(pose, facing);
        if (StationMesh.shadersInUse()) {
            StationRenderer.draw(buffer, pose, body, light);
        }
        StationRenderer.draw(buffer, pose, StationGeometry.group(DATA, "static" + lights), light);
        // The parts that will move, drawn where the design parks them for now: the lantern's rotating plugs.
        for (String part : new String[] {"plug_large", "plug_small"}) {
            StationRenderer.draw(buffer, pose, StationGeometry.group(DATA, part), light);
            StationRenderer.draw(buffer, pose, StationGeometry.group(DATA, part + lights), light);
        }
        // The lantern's glass last, translucent, so it tints the hall behind it rather than hiding it.
        StationRenderer.draw(buffers.getBuffer(Sheets.translucentCullBlockSheet()), pose, StationGeometry.group(DATA, "glass"), light);
        pose.translate(BEACON_X, 0, BEACON_Z);
        pose.mulPose(Axis.YP.rotationDegrees(core.beaconAngle(partialTick)));
        pose.translate(-BEACON_X, 0, -BEACON_Z);
        StationRenderer.draw(buffer, pose, StationGeometry.group(DATA, "beacon"), light);
        StationRenderer.draw(buffer, pose, StationGeometry.group(DATA, "beacon" + lights), light);
        pose.popPose();
    }

    /** From the core's corner to the design's north-west corner, then turned about the centre to the facing. */
    private static void place(PoseStack pose, Direction facing) {
        BlockPos coreCell = BreederLayout.turn(BreederLayout.CORE, facing);
        pose.translate(-coreCell.getX() + HALF, 0, -coreCell.getZ() + HALF);
        pose.mulPose(Axis.YP.rotationDegrees(BreederLayout.yRotation(facing)));
        pose.translate(-HALF, 0, -HALF);
    }

    @Override
    public AABB getRenderBoundingBox(BreederCoreBlockEntity core) {
        BlockPos a = BreederLayout.toWorld(core.getBlockPos(), core.facing(), BlockPos.ZERO);
        BlockPos b = BreederLayout.toWorld(core.getBlockPos(), core.facing(),
                new BlockPos(BreederLayout.SIZE - 1, BreederLayout.HEIGHT, BreederLayout.SIZE - 1));
        return new AABB(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ()).inflate(1);
    }

    @Override
    public boolean shouldRenderOffScreen(BreederCoreBlockEntity core) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
