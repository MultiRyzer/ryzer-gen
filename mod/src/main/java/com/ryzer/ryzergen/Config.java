package com.ryzer.ryzergen;

import com.ryzer.ryzergen.material.OreType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Map<OreType, ModConfigSpec.BooleanValue> ORE_GENERATION = new EnumMap<>(OreType.class);

    static {
        BUILDER.comment("World generation. Changes apply after a restart, and only to chunks generated after that.")
                .push("worldgen");
        for (OreType ore : OreType.values()) {
            ORE_GENERATION.put(ore, BUILDER
                    .comment("Generate " + ore.id() + " ore in the Overworld. Turn off if another mod already supplies it.")
                    .define(ore.id() + "_ore", true));
        }
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Unknown ore IDs (for example from a datapack typo) stay enabled rather than silently vanishing. */
    public static boolean isOreGenerationEnabled(String oreId) {
        return OreType.byId(oreId).map(ore -> ORE_GENERATION.get(ore).get()).orElse(true);
    }

    private Config() {}
}
