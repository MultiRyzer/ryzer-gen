package com.ryzer.ryzergen.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A bubble rising off a hot rod in a Spent Fuel Pool (vanilla's bubble texture). Vanilla's bubbles
 * vanish outside water blocks, and the pool's water is part of its model, so this one needs no
 * water: it rises the height it was given (the particle's y speed carries it) with a little wobble,
 * then pops at the surface with vanilla's pop.
 */
public class PoolBubbleParticle extends TextureSheetParticle {
    private static final double SPEED = 0.04;

    protected PoolBubbleParticle(ClientLevel level, double x, double y, double z, double rise, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        gravity = 0;
        hasPhysics = false;
        quadSize = 0.03F + random.nextFloat() * 0.03F;
        setSize(0.02F, 0.02F);
        double speed = SPEED * (0.8 + random.nextDouble() * 0.4);
        xd = 0;
        yd = speed;
        zd = 0;
        lifetime = Math.max(1, (int) (rise / speed));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0, 0);
            remove();
            return;
        }
        // A little wobble, as bubbles do.
        xd += (random.nextDouble() - 0.5) * 0.004;
        zd += (random.nextDouble() - 0.5) * 0.004;
        xd *= 0.85;
        zd *= 0.85;
        move(xd, yd, zd);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double rise, double zd) {
            return new PoolBubbleParticle(level, x, y, z, rise, sprites);
        }
    }
}
