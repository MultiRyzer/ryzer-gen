package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.sky.SunSwarm;
import com.ryzer.ryzergen.sky.SunSwarmClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.opengl.GL11;

/**
 * Draws the Dyson swarm (sky/SunSwarm) round the overworld's sun, right after the sky, on the same
 * path vanilla's sun takes across it and about as far out. Fog is turned off for it, then the depth
 * it wrote is cleared so the world draws over it like any sky. Vanilla's square sun is hidden while the swarm is up
 * (mixin/LevelRendererMixin). The look itself is SkySun's.
 *
 * <p>While the shell closes, the sky, the fog and the world's daylight darken with the light it
 * cuts off (the sky and daylight through mixin/ClientLevelMixin), and stay that way while it is shut.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class SkySwarmRenderer {
    /**
     * How far from the camera it is drawn, in blocks: as far as vanilla's sun (100), less the
     * shell's size, so view bobbing hardly moves it and it stays inside the far clipping plane.
     */
    static final float DISTANCE = 90;
    /**
     * The sun's radius at that distance: big enough that the disc covers vanilla's square sun, corners
     * and all. Vanilla's is hidden (mixin/LevelRendererMixin), but a shader pack may draw its own.
     */
    static final float SCALE = 2.2F * DISTANCE / 20;
    /**
     * With a shader pack on it is drawn much closer instead: packs fog anything far off, and at 90
     * blocks it would sit deep in their fog. So close, view bobbing would carry it about, so the
     * bob's shift is taken back off it (see unbob).
     */
    private static final float PACK_DISTANCE = 20;

    private SkySwarmRenderer() {}

    /**
     * Whether our sun takes vanilla's place in this sky: in the overworld, always while the swarm is
     * up, and otherwise if the round sun is on in the config. Vanilla's square sun is then not drawn
     * (mixin/LevelRendererMixin).
     */
    public static boolean roundSun() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.dimension() == Level.OVERWORLD && (SunSwarmClient.active() || Config.ROUND_SUN.get());
    }

    /**
     * Whether it should show now: our sun in this sky, seen from the open air, not in rain (vanilla's
     * fades out in rain too), and not once it has set well below the horizon, where it would show
     * under the edge of the world.
     */
    static boolean visible(ClientLevel level, float partialTick) {
        return roundSun() && level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL
                && Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() == FogType.NONE
                && level.getRainLevel(partialTick) < 0.05F
                && Math.cos(level.getTimeOfDay(partialTick) * Math.PI * 2) > -0.2;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (level == null || !visible(level, partialTick)) {
            return;
        }
        long time = level.getGameTime();
        boolean pack = StationMesh.shadersInUse();
        float distance = pack ? PACK_DISTANCE : DISTANCE;
        float scale = SCALE * distance / DISTANCE;
        PoseStack pose = new PoseStack();
        if (pack) {
            unbob(pose, minecraft, partialTick);
        }
        pose.mulPose(event.getModelViewMatrix());
        // Vanilla's sun path (LevelRenderer#renderSky): the sun sits straight up after these turns.
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360));
        pose.translate(0, distance, 0);
        pose.scale(scale, scale, scale);

        float fogStart = RenderSystem.getShaderFogStart();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float seconds = (time + partialTick) / 20F;
        float dim = SunSwarmClient.dim(time, partialTick);
        SunSwarm.Phase phase = SunSwarmClient.phase();
        if (phase == SunSwarm.Phase.COLLAPSING || phase == SunSwarm.Phase.WORMHOLE) {
            // The shell crushes in on the enclosed sun, condensing to a point, then flashes; the
            // wormhole opens out of the flash (WormholeRenderer, after the world is drawn).
            float t = SunSwarmClient.collapse(time, partialTick);
            if (t < 0.72F) {
                SkySun.collapse(buffers, pose, t, dim);
            }
            // The white builds to its peak and holds there, swelling, until the wormhole pops open
            // and it is gone at once, with a shockwave racing out.
            float pop = SunSwarmClient.POP;
            float flash = SunSwarmClient.smooth(0.45F, pop, t) * (1 - SunSwarmClient.smooth(pop, pop + 0.03F, t));
            if (flash > 0) {
                SkySun.flash(buffers, pose, flash, 0.4F + 3.2F * SunSwarmClient.smooth(0.45F, 0.7F, t) + 0.8F * SunSwarmClient.smooth(0.7F, pop, t));
            }
            float shock = (t - pop) / 0.1F;
            if (shock > 0 && shock < 1) {
                SkySun.shockwave(buffers, pose, 1 - shock, 1 + 9 * (float) Math.sqrt(shock));
            }
            if (!WormholeRenderer.canLens()) {
                SkySun.wormholeFallback(buffers, pose, SunSwarmClient.wormhole(time, partialTick), seconds);
            }
        } else {
            SkySun.draw(buffers, pose, seconds, SunSwarmClient.coverage(time, partialTick),
                    SunSwarmClient.closing(time, partialTick), dim);
        }
        buffers.endBatch();
        RenderSystem.setShaderFogStart(fogStart);
        // Nothing but the sky is drawn yet, so this only forgets the swarm's depth.
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    /**
     * Takes view bobbing's sideways and up-down shift back off (GameRenderer#bobView), so a sky
     * object drawn close to the camera stays put like a far one. Its small tilt is left, as vanilla's
     * own sky tilts with it.
     */
    private static void unbob(PoseStack pose, Minecraft minecraft, float partialTick) {
        if (!minecraft.options.bobView().get() || !(minecraft.getCameraEntity() instanceof Player player)) {
            return;
        }
        float step = player.walkDist - player.walkDistO;
        float walked = -(player.walkDist + step * partialTick);
        float bob = Mth.lerp(partialTick, player.oBob, player.bob);
        pose.translate(-Mth.sin(walked * (float) Math.PI) * bob * 0.5F, Math.abs(Mth.cos(walked * (float) Math.PI) * bob), 0);
    }

    /** The fog darkens with the sky, or the horizon would stay bright once the sun is gone. */
    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !SunSwarmClient.active() || level.dimension() != Level.OVERWORLD
                || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float keep = 1 - 0.95F * SunSwarmClient.dim(level.getGameTime(), (float) event.getPartialTick());
        event.setRed(event.getRed() * keep);
        event.setGreen(event.getGreen() * keep);
        event.setBlue(event.getBlue() * keep);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SunSwarmClient.clear();
    }
}
