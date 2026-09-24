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

    // Placeholder balance until the energy scale is settled (see OPEN-QUESTIONS.md).
    public static final ModConfigSpec.IntValue MICROREACTOR_OUTPUT;
    public static final ModConfigSpec.IntValue MICROREACTOR_FUEL_LIFE;
    public static final ModConfigSpec.IntValue MICROREACTOR_COOLANT_USE;
    public static final ModConfigSpec.BooleanValue MICROREACTOR_MELTDOWNS;
    public static final ModConfigSpec.DoubleValue MICROREACTOR_MELTDOWN_POWER;

    static {
        BUILDER.comment("Microreactor balance.").push("microreactor");
        MICROREACTOR_OUTPUT = BUILDER
                .comment("FE per tick running dry at operating temperature. Water adds about 28%, overdrive about 85%.")
                .defineInRange("output", 200, 1, 1_000_000);
        MICROREACTOR_FUEL_LIFE = BUILDER
                .comment("How long one sealed fuel core burns, in ticks (72000 is one hour of running).")
                .defineInRange("fuel_life", 72_000, 20, Integer.MAX_VALUE);
        MICROREACTOR_COOLANT_USE = BUILDER
                .comment("Water boiled off per tick while running with coolant, in mB.")
                .defineInRange("coolant_use", 1, 0, 1000);
        MICROREACTOR_MELTDOWNS = BUILDER
                .comment("With the safeties off, losing coolant makes the reactor explode. Off: the interlock re-arms itself instead.",
                        "A reactor with its safeties on can never melt down.")
                .define("meltdowns", true);
        MICROREACTOR_MELTDOWN_POWER = BUILDER
                .comment("Meltdown explosion strength (TNT is 4).")
                .defineInRange("meltdown_power", 5.0, 0.0, 20.0);
        BUILDER.pop();
    }

    public static final ModConfigSpec.IntValue CABLE_RATE;
    public static final ModConfigSpec.IntValue LEAD_ACID_CAPACITY;
    public static final ModConfigSpec.IntValue LEAD_ACID_RATE;
    public static final ModConfigSpec.BooleanValue RADIATION_ENABLED;
    public static final ModConfigSpec.DoubleValue RADIATION_STRENGTH;

    static {
        BUILDER.comment("Cables.").push("cables");
        CABLE_RATE = BUILDER
                .comment("FE per tick an energy cable moves from each input (a pushing generator or an extract side).")
                .defineInRange("energy_rate", 1_000, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.comment("Home battery. Each module adds capacity and charge rate; a cabinet holds up to 6.").push("battery");
        LEAD_ACID_CAPACITY = BUILDER
                .comment("FE one lead-acid module holds.")
                .defineInRange("lead_acid_capacity", 200_000, 1, 100_000_000);
        LEAD_ACID_RATE = BUILDER
                .comment("FE per tick one lead-acid module can charge or discharge.")
                .defineInRange("lead_acid_rate", 500, 1, 1_000_000);
        BUILDER.pop();

        BUILDER.comment("Radiation. Only running reactors and meltdowns emit it.").push("radiation");
        RADIATION_ENABLED = BUILDER
                .comment("Players take a radiation dose near running reactors and meltdown sites.")
                .define("enabled", true);
        RADIATION_STRENGTH = BUILDER
                .comment("Multiplies every dose rate. 0.5 halves radiation, 2 doubles it.")
                .defineInRange("strength", 1.0, 0.0, 100.0);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** A config value, or its default before the config has loaded (tooltips can ask very early). */
    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** Unknown ore IDs (for example from a datapack typo) stay enabled rather than silently vanishing. */
    public static boolean isOreGenerationEnabled(String oreId) {
        return OreType.byId(oreId).map(ore -> ORE_GENERATION.get(ore).get()).orElse(true);
    }

    private Config() {}
}
