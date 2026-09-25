package com.ryzer.ryzergen.radiation;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorPartBlock;
import com.ryzer.ryzergen.registry.ModAttachments;
import com.ryzer.ryzergen.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Radiation (design rule 10), for players and mobs alike. Once a second each player takes the dose rate from every source
 * nearby: strength over distance squared, cut by whatever stands in between. Dose builds while
 * exposed and slowly recovers away from sources. Effects escalate slowly and never kill outright
 * outside a meltdown.
 *
 * <p>Mobs work the same way, checked once a second around each source rather than per mob, so
 * they cost nothing away from reactors. Their recovery is worked out when they are next exposed.
 * Strong sources make no-go zones for mobs and can run mob farms.
 *
 * <p>Real basis: dose rate falls off with the square of distance, and shielding attenuates it; lead
 * is by far the best common shield. The recovery is a gameplay fudge.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID)
public final class Radiation {
    /** Beyond this, sources are ignored (dose rate is negligible and it saves work). */
    public static final double RANGE = 16;
    public static final float WEAKNESS = 100;
    public static final float SICKNESS = 250;
    public static final float DAMAGE = 500;
    public static final float MAX_DOSE = 1000;
    private static final float RECOVERY = 0.5F;
    private static final TagKey<Block> LEAD_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lead"));

    private Radiation() {}

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        boolean exempt = !Config.RADIATION_ENABLED.get() || player.isCreative() || player.isSpectator();
        // The ring reads the radiation around the player (its gauge and clicks); the dose taken is
        // what gets past it.
        float field = Config.RADIATION_ENABLED.get() ? fieldRate(level, player) : 0;
        float rate = exempt ? 0 : doseRate(field, player);
        float dose = player.getData(ModAttachments.RADIATION_DOSE);
        dose = rate > 0.05F ? Math.min(MAX_DOSE, dose + rate) : Math.max(0, dose - RECOVERY);
        player.setData(ModAttachments.RADIATION_DOSE, dose);
        if (!exempt) {
            applyEffects(player, dose);
        }
        PacketDistributor.sendToPlayer(player, new RadiationPayload(dose, field));
    }

    /** Once a second, every mob near a source takes its dose. */
    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0
                || !Config.RADIATION_ENABLED.get() || !Config.RADIATION_MOBS.get()) {
            return;
        }
        List<RadiationSources.Source> sources = RadiationSources.sources(level);
        if (sources.isEmpty()) {
            return;
        }
        Set<Mob> exposed = new HashSet<>();
        for (RadiationSources.Source source : sources) {
            exposed.addAll(level.getEntitiesOfClass(Mob.class, new AABB(BlockPos.containing(source.pos())).inflate(RANGE)));
        }
        long now = level.getGameTime();
        for (Mob mob : exposed) {
            float rate = sourceRate(level, sources, mob.getEyePosition());
            // Recovery since the last dose, at the players' rate.
            long since = now - mob.getData(ModAttachments.RADIATION_EXPOSED);
            float dose = Math.max(0, mob.getData(ModAttachments.RADIATION_DOSE) - RECOVERY * since / 20F);
            dose = Math.min(MAX_DOSE, dose + rate);
            mob.setData(ModAttachments.RADIATION_DOSE, dose);
            mob.setData(ModAttachments.RADIATION_EXPOSED, now);
            applyEffects(mob, dose);
        }
    }

    /** mSv per second at the player's head, after shielding: what the dosimeter ring reads. */
    public static float fieldRate(ServerLevel level, ServerPlayer player) {
        return sourceRate(level, RadiationSources.sources(level), player.getEyePosition());
    }

    /** The share of {@code field} the player actually takes, after the dosimeter ring. */
    public static float doseRate(float field, ServerPlayer player) {
        return DosimeterRingItem.isWorn(player) ? field * (1 - DosimeterRingItem.PROTECTION) : field;
    }

    /** mSv per second at a point from every source in range, after shielding and the config multiplier. */
    private static float sourceRate(ServerLevel level, List<RadiationSources.Source> sources, Vec3 eye) {
        float total = 0;
        for (RadiationSources.Source source : sources) {
            double distSq = Math.max(1, source.pos().distanceToSqr(eye));
            if (distSq > RANGE * RANGE) {
                continue;
            }
            total += (float) (source.strength() / distSq * shielding(level, source.pos(), eye));
        }
        return total * Config.RADIATION_STRENGTH.get().floatValue();
    }

    /**
     * How much gets through the blocks between source and target, from 1 (nothing in the way) down.
     * Solid blocks halve it, water takes a fifth, lead takes nine tenths. The reactor's own blocks
     * are part of the source and do not count.
     */
    private static double shielding(ServerLevel level, Vec3 from, Vec3 to) {
        double factor = 1;
        int steps = (int) Math.ceil(from.distanceTo(to) * 2);
        BlockPos last = null;
        for (int i = 1; i < steps && factor > 0.001; i++) {
            BlockPos pos = BlockPos.containing(from.lerp(to, i / (double) steps));
            if (pos.equals(last)) {
                continue;
            }
            last = pos;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getBlock() instanceof MicroreactorPartBlock) {
                continue;
            }
            if (state.is(LEAD_BLOCKS)) {
                factor *= 0.1;
            } else if (state.getFluidState().is(FluidTags.WATER)) {
                factor *= 0.8;
            } else if (state.isSolidRender(level, pos)) {
                factor *= 0.5;
            }
        }
        return factor;
    }

    /**
     * Weakness, then sickness, then slow damage. Refreshed each second while the dose is high. Players
     * get hunger and bouts of nausea; mobs, which neither eat nor see straight anyway, slow down.
     */
    private static void applyEffects(LivingEntity entity, float dose) {
        if (dose >= WEAKNESS) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
        }
        if (dose >= SICKNESS) {
            if (entity instanceof ServerPlayer) {
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false, true));
                if (entity.tickCount % 200 == 0) {
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false, true));
                }
            } else {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false, true));
            }
        }
        // Every 4 seconds for players (checked each tick of the second they are processed); mobs are
        // processed once a second, so they take it on every fourth.
        boolean hurtNow = entity instanceof ServerPlayer ? entity.tickCount % 80 == 0 : entity.level().getGameTime() % 80 == 0;
        if (dose >= DAMAGE && hurtNow) {
            entity.hurt(new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(ModDamageTypes.RADIATION)), 1);
        }
    }
}
