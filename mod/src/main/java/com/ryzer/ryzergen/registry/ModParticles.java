package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, RyzerGen.MOD_ID);

    /**
     * A bubble rising off a hot rod in a Spent Fuel Pool. Vanilla's bubbles live only in water
     * blocks, and the pool's water is its model, so this one rises by itself to the surface it is
     * given and pops there. It uses vanilla's bubble texture.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> POOL_BUBBLE =
            PARTICLES.register("pool_bubble", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
