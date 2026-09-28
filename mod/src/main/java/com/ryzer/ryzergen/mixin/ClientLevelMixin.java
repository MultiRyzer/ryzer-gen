package com.ryzer.ryzergen.mixin;

import com.ryzer.ryzergen.sky.SunSwarmClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Darkens the overworld's sky, clouds and daylight as the Dyson swarm cuts off the sun's light
 * (sky/SunSwarmClient#dim). With the sun enclosed it is darker than a normal night: there is no
 * sunlight for the moon to reflect, so only the stars are left.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    /** Daylight with the sun gone: well under vanilla's moonlit night (0.2). */
    private static final float RYZERGEN$DARK = 0.05F;

    private float ryzergen$dim(float partialTick) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (!SunSwarmClient.active() || level.dimension() != Level.OVERWORLD) {
            return 0;
        }
        return SunSwarmClient.dim(level.getGameTime(), partialTick);
    }

    @Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
    private void ryzergen$skyDarken(float partialTick, CallbackInfoReturnable<Float> cir) {
        float dim = ryzergen$dim(partialTick);
        if (dim > 0) {
            cir.setReturnValue(cir.getReturnValue() + (RYZERGEN$DARK - cir.getReturnValue()) * dim);
        }
    }

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void ryzergen$skyColour(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        float dim = ryzergen$dim(partialTick);
        if (dim > 0) {
            cir.setReturnValue(cir.getReturnValue().scale(1 - dim));
        }
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void ryzergen$cloudColour(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        float dim = ryzergen$dim(partialTick);
        if (dim > 0) {
            cir.setReturnValue(cir.getReturnValue().scale(1 - 0.95F * dim));
        }
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void ryzergen$starBrightness(float partialTick, CallbackInfoReturnable<Float> cir) {
        float dim = ryzergen$dim(partialTick);
        if (dim > 0) {
            cir.setReturnValue(cir.getReturnValue() + (0.5F - cir.getReturnValue()) * dim);
        }
    }
}
