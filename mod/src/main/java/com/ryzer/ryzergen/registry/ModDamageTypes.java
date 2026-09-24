package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/** Damage types live in data; these are their keys. Defined in ModWorldGenProvider. */
public final class ModDamageTypes {
    /** Radiation sickness. Ignores armour, since armour is no shield against gamma rays. */
    public static final ResourceKey<DamageType> RADIATION = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "radiation"));

    private ModDamageTypes() {}
}
