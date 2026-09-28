package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.sky.SunSwarmClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;

/**
 * The wormhole where the sun was (sky/SunSwarm): a lensing pass over the finished frame, before
 * the hand is drawn. The frame and its depth are copied, then the wormhole shader
 * (shaders/core/wormhole) redraws the screen from the copy: sky pixels near the wormhole show the
 * sky bent round it, or the far side through its throat, and everything else is copied back as it
 * was. It only runs while the wormhole is open.
 *
 * <p>Shader packs replace the whole pipeline, so with one on this steps aside and SkySun draws a
 * plain picture of the wormhole instead.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class WormholeRenderer {
    private static ShaderInstance shader;
    private static TextureTarget copy;

    private WormholeRenderer() {}

    /** Whether the real lensing can run (not with a shader pack on, and once its shader has loaded). */
    public static boolean canLens() {
        return shader != null && !StationMesh.shadersInUse();
    }

    @SubscribeEvent
    public static void registerShader(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "wormhole"), DefaultVertexFormat.POSITION), loaded -> shader = loaded);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !canLens()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (level == null || !SkySwarmRenderer.visible(level, partialTick)) {
            return;
        }
        long time = level.getGameTime();
        float open = SunSwarmClient.wormhole(time, partialTick);
        if (open <= 0) {
            return;
        }
        // Where the sun is on screen, and how big the throat is: the sun's path, as the sky draws it.
        Matrix4f toScreen = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix())
                .rotate(Axis.YP.rotationDegrees(-90))
                .rotate(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360));
        Vector4f middle = toScreen.transform(new Vector4f(0, SkySwarmRenderer.DISTANCE, 0, 1));
        if (middle.w <= 0) {
            return;
        }
        RenderTarget main = minecraft.getMainRenderTarget();
        float aspect = (float) main.width / main.height;
        float cx = middle.x / middle.w * 0.5F + 0.5F, cy = middle.y / middle.w * 0.5F + 0.5F;
        // Its size from the angle it spans, which stays the same wherever it is in view. (Projecting
        // a point on its edge instead grows without limit towards the side of the view.)
        double angle = Math.atan(SkySwarmRenderer.SCALE * open / SkySwarmRenderer.DISTANCE);
        float radius = (float) Math.tan(angle) * event.getProjectionMatrix().m11() / 2;
        // Nothing to do if even the lensing's reach (four throats) is off screen.
        float reach = radius * 4;
        if (cx < -reach / aspect || cx > 1 + reach / aspect || cy < -reach || cy > 1 + reach) {
            return;
        }

        copyFrame(main);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        shader.setSampler("Scene", copy.getColorTextureId());
        shader.setSampler("SceneDepth", copy.getDepthTextureId());
        shader.safeGetUniform("Centre").set(cx, cy);
        shader.safeGetUniform("Radius").set(radius);
        shader.safeGetUniform("Aspect").set(aspect);
        float seconds = (time + partialTick) / 20F;
        shader.safeGetUniform("Spin").set(seconds * 0.02F);
        shader.safeGetUniform("Time").set(seconds % 3600);
        RenderSystem.setShader(() -> shader);
        BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        quad.addVertex(0, 0, 0);
        quad.addVertex(1, 0, 0);
        quad.addVertex(1, 1, 0);
        quad.addVertex(0, 1, 0);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** Copies the frame drawn so far, colour and depth, then draws to the screen again. */
    private static void copyFrame(RenderTarget main) {
        if (copy == null) {
            copy = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        } else if (copy.width != main.width || copy.height != main.height) {
            copy.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        if (main.isStencilEnabled() && !copy.isStencilEnabled()) {
            copy.enableStencil();
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, copy.width, copy.height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(false);
    }
}
