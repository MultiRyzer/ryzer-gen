package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.advancement.MilestoneTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, RyzerGen.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, MilestoneTrigger> MILESTONE =
            TRIGGERS.register("milestone", MilestoneTrigger::new);

    private ModTriggers() {}
}
