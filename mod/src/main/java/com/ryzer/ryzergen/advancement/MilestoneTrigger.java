package com.ryzer.ryzergen.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ryzer.ryzergen.registry.ModTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Advancements for things that are done rather than held: forming a microreactor, switching its
 * safeties off, a meltdown, a full battery cabinet. The game calls {@link #trigger} with a
 * {@link Milestone}, and an advancement names the milestone it waits for.
 */
public class MilestoneTrigger extends SimpleCriterionTrigger<MilestoneTrigger.Instance> {
    /** How close a player must be to count as there when a machine does something. */
    private static final double NEARBY = 16;

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, Milestone milestone) {
        trigger(player, instance -> instance.milestone() == milestone);
    }

    /** Credits everyone within 16 blocks, for events with no single player behind them. */
    public void triggerNearby(ServerLevel level, Vec3 centre, Milestone milestone) {
        for (ServerPlayer player : level.players()) {
            if (player.position().closerThan(centre, NEARBY)) {
                trigger(player, milestone);
            }
        }
    }

    public static Criterion<Instance> of(Milestone milestone) {
        return ModTriggers.MILESTONE.get().createCriterion(new Instance(Optional.empty(), milestone));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Milestone milestone)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Milestone.CODEC.fieldOf("milestone").forGetter(Instance::milestone)
        ).apply(instance, Instance::new));
    }
}
