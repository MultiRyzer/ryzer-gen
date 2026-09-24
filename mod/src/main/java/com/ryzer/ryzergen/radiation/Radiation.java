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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Player radiation (design rule 10). Once a second each player takes the dose rate from every source
 * nearby: strength over distance squared, cut by whatever stands in between. Dose builds while
 * exposed and slowly recovers away from sources. Effects escalate slowly and never kill outright
 * outside a meltdown.
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
        float rate = exempt ? 0 : doseRate(level, player);
        float dose = player.getData(ModAttachments.RADIATION_DOSE);
        dose = rate > 0.05F ? Math.min(MAX_DOSE, dose + rate) : Math.max(0, dose - RECOVERY);
        player.setData(ModAttachments.RADIATION_DOSE, dose);
        if (!exempt) {
            applyEffects(player, dose);
        }
        PacketDistributor.sendToPlayer(player, new RadiationPayload(dose, rate));
    }

    /** mSv per second at the player's head, after shielding and the dosimeter ring. */
    public static float doseRate(ServerLevel level, ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        float total = 0;
        for (RadiationSources.Source source : RadiationSources.sources(level)) {
            double distSq = Math.max(1, source.pos().distanceToSqr(eye));
            if (distSq > RANGE * RANGE) {
                continue;
            }
            total += (float) (source.strength() / distSq * shielding(level, source.pos(), eye));
        }
        total *= Config.RADIATION_STRENGTH.get().floatValue();
        if (DosimeterRingItem.isWorn(player)) {
            total *= 1 - DosimeterRingItem.PROTECTION;
        }
        return total;
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

    /** Weakness, then nausea and hunger, then slow damage. Refreshed each second while the dose is high. */
    private static void applyEffects(ServerPlayer player, float dose) {
        if (dose >= WEAKNESS) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
        }
        if (dose >= SICKNESS) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false, true));
            if (player.tickCount % 200 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false, true));
            }
        }
        if (dose >= DAMAGE && player.tickCount % 80 == 0) {
            player.hurt(new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(ModDamageTypes.RADIATION)), 1);
        }
    }
}
