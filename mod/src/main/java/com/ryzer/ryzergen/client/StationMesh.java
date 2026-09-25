package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.List;

/**
 * The fission station's static body (about 22,000 faces) uploaded to the GPU once, instead of sent
 * from the CPU every frame. It is built for one light level, one facing and one load of resources,
 * and rebuilt when any of those change: the light at dawn and dusk, or a resource reload (the atlas
 * moves). The core's block entity owns it and closes it when the station goes.
 *
 * <p>Shader packs (Iris and its ports) only see geometry drawn through the usual buffers, so while
 * one is in use the renderer draws the body the old way instead (see {@link #shadersInUse()}).
 */
public final class StationMesh implements AutoCloseable {
    private final VertexBuffer buffer;
    private final int light;
    private final Direction facing;
    private final int generation;

    private StationMesh(VertexBuffer buffer, int light, Direction facing, int generation) {
        this.buffer = buffer;
        this.light = light;
        this.facing = facing;
        this.generation = generation;
    }

    /** Whether this mesh still matches what it would be built as now. */
    public boolean matches(int light, Direction facing) {
        return this.light == light && this.facing == facing && generation == StationGeometry.generation();
    }

    /**
     * Builds the mesh from {@code quads}, placed by {@code transform} (the station's turn to its
     * facing, from the core's block), each with {@code light}.
     */
    public static StationMesh build(List<StationGeometry.Quad> quads, PoseStack transform, int light, Direction facing) {
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(quads.size() * 4 * DefaultVertexFormat.NEW_ENTITY.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
            StationRenderer.draw(builder, transform, quads, light);
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            MeshData mesh = builder.build();
            buffer.bind();
            if (mesh != null) {
                // Upload copies the mesh to the GPU and closes it.
                buffer.upload(mesh);
            }
            VertexBuffer.unbind();
            return new StationMesh(buffer, light, facing, StationGeometry.generation());
        }
    }

    /** Draws the mesh at {@code pose} (the block entity's position, as the renderer is given it). */
    public void draw(Matrix4f pose) {
        RenderType type = Sheets.cutoutBlockSheet();
        type.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            buffer.bind();
            buffer.drawWithShader(new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose), RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        }
        type.clearRenderState();
    }

    @Override
    public void close() {
        buffer.close();
    }

    private static Method shaderPackInUse;
    private static Object irisApi;
    private static boolean lookedUp;
    /** Asking Iris failed once: from then on, always draw the safe way. */
    private static boolean irisBroken;

    /** Whether a shader pack is active (Iris API, looked up by name so it is never a dependency). */
    public static boolean shadersInUse() {
        if (!lookedUp) {
            lookedUp = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                shaderPackInUse = api.getMethod("isShaderPackInUse");
            } catch (Throwable absent) {
                shaderPackInUse = null;
            }
        }
        if (irisBroken) {
            return true;
        }
        if (shaderPackInUse == null) {
            return false;
        }
        try {
            return (boolean) shaderPackInUse.invoke(irisApi);
        } catch (Throwable failed) {
            RyzerGen.LOGGER.warn("Could not ask Iris whether shaders are on; drawing the station the safe way", failed);
            irisBroken = true;
            return true;
        }
    }
}
