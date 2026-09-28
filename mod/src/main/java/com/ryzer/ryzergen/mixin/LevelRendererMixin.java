package com.ryzer.ryzergen.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.MeshData;
import com.ryzer.ryzergen.client.SkySwarmRenderer;
import com.ryzer.ryzergen.sky.SunSwarmClient;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla's sun and moon. While our round sun is in the sky (the config's round sun, or the Dyson
 * swarm), the square sun is not drawn at all, since client/SkySun draws the sun in its place: the
 * draw is skipped, rather than drawn clear, so a shader pack has nothing to draw either. With the sun enclosed or gone the moon goes dark too, as
 * it only shines by the sun's light.
 *
 * <p>In LevelRenderer#renderSky the draws go: the sunrise glow (0), the sun (1), the moon (2). A
 * skipped draw's mesh is freed here, as the draw would have. Optional (require = 0): if another mod
 * changes the sky, the sun and moon just stay.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @WrapWithCondition(method = "renderSky", require = 0, at = @At(value = "INVOKE", ordinal = 1,
            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private boolean ryzergen$drawSun(MeshData mesh) {
        return ryzergen$draw(mesh, !SkySwarmRenderer.roundSun());
    }

    @WrapWithCondition(method = "renderSky", require = 0, at = @At(value = "INVOKE", ordinal = 2,
            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private boolean ryzergen$drawMoon(MeshData mesh) {
        return ryzergen$draw(mesh, !SunSwarmClient.sunGone());
    }

    @Unique
    private static boolean ryzergen$draw(MeshData mesh, boolean draw) {
        if (!draw) {
            mesh.close();
        }
        return draw;
    }
}
