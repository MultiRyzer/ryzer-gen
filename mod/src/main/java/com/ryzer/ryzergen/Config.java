package com.ryzer.ryzergen;

import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
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
                .comment("How long one sealed fuel core burns, in ticks (48000 is 40 minutes of running).")
                .defineInRange("fuel_life", 48_000, 20, Integer.MAX_VALUE);
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
    public static final ModConfigSpec.IntValue ITEM_PIPE_BATCH;
    public static final ModConfigSpec.IntValue ITEM_PIPE_INTERVAL;
    public static final ModConfigSpec.IntValue FLUID_PIPE_RATE;
    public static final ModConfigSpec.IntValue GAS_PIPE_RATE;
    public static final ModConfigSpec.IntValue PUMP_RATE;
    public static final ModConfigSpec.IntValue PUMP_ENERGY;
    public static final ModConfigSpec.IntValue TANK_CAPACITY;
    public static final ModConfigSpec.IntValue STATION_OUTPUT;
    public static final ModConfigSpec.IntValue STATION_WATER_USE;
    public static final ModConfigSpec.BooleanValue STATION_MELTDOWNS;
    public static final ModConfigSpec.DoubleValue STATION_MELTDOWN_POWER;
    public static final ModConfigSpec.IntValue STATION_TILT_SECONDS;
    public static final ModConfigSpec.IntValue STATION_MELTDOWN_RADIUS;
    public static final ModConfigSpec.IntValue FLUID_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue STEAM_PER_WATER;
    public static final ModConfigSpec.IntValue LEAD_ACID_CAPACITY;
    public static final ModConfigSpec.IntValue LEAD_ACID_RATE;
    public static final ModConfigSpec.BooleanValue ANNOUNCER_ENABLED;
    public static final ModConfigSpec.BooleanValue ROUND_SUN;
    public static final ModConfigSpec.BooleanValue RADIATION_ENABLED;
    public static final ModConfigSpec.DoubleValue RADIATION_STRENGTH;
    public static final ModConfigSpec.BooleanValue RADIATION_MOBS;

    static {
        BUILDER.comment("Cables.").push("cables");
        CABLE_RATE = BUILDER
                .comment("FE per tick an energy cable moves from each input (a pushing generator or an extract side).")
                .defineInRange("energy_rate", 1_000, 1, Integer.MAX_VALUE);
        ITEM_PIPE_BATCH = BUILDER
                .comment("Items an item pipe's extract side pulls at a time.")
                .defineInRange("item_batch", 8, 1, 64 * 64);
        ITEM_PIPE_INTERVAL = BUILDER
                .comment("Ticks between pulls (20 is one second). The default moves 8 items a second per extract side.")
                .defineInRange("item_interval", 20, 1, 1200);
        FLUID_PIPE_RATE = BUILDER
                .comment("mB per tick a fluid pipe moves from each input (a pushing machine or an extract side).")
                .defineInRange("fluid_rate", 250, 1, Integer.MAX_VALUE);
        GAS_PIPE_RATE = BUILDER
                .comment("mB per tick a gas pipe moves from each input. Gases are light, so this is higher than for liquids.")
                .defineInRange("gas_rate", 1_000, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.comment("Water and steam.").push("water");
        PUMP_RATE = BUILDER
                .comment("mB of water per tick an intake pump draws. It needs at least two water source blocks next to it.")
                .defineInRange("pump_rate", 100, 1, 100_000);
        PUMP_ENERGY = BUILDER
                .comment("FE per tick an intake pump uses while pumping.")
                .defineInRange("pump_energy", 10, 0, 100_000);
        STEAM_PER_WATER = BUILDER
                .comment("mB of steam the microreactor makes from each mB of water it boils.")
                .defineInRange("steam_per_water", 10, 0, 1_600);
        TANK_CAPACITY = BUILDER
                .comment("mB of gas each block of a pressure tank holds.")
                .defineInRange("tank_capacity", 32_000, 1, 100_000_000);
        FLUID_TANK_CAPACITY = BUILDER
                .comment("mB of liquid each block of a fluid tank holds.")
                .defineInRange("fluid_tank_capacity", 16_000, 1, 100_000_000);
        BUILDER.pop();

        BUILDER.comment("Modular battery rack. Each module adds capacity and charge rate; a cabinet holds up to 6.").push("battery");
        LEAD_ACID_CAPACITY = BUILDER
                .comment("FE one lead-acid module holds.")
                .defineInRange("lead_acid_capacity", 200_000, 1, 100_000_000);
        LEAD_ACID_RATE = BUILDER
                .comment("FE per tick one lead-acid module can charge or discharge.")
                .defineInRange("lead_acid_rate", 500, 1, 1_000_000);
        BUILDER.pop();

        BUILDER.comment("Fission power station.").push("fission_station");
        STATION_OUTPUT = BUILDER
                .comment("Percent of the normal heat each fuel rod makes, and each coolant channel carries (so also power out).")
                .defineInRange("output_percent", 100, 1, 10_000);
        STATION_WATER_USE = BUILDER
                .comment("Percent of the normal water a station boils. Normal is 1 mB per 50 heat (1 per 25 in overdrive):",
                        "a strong core needs several intake pumps, and more in overdrive.")
                .defineInRange("water_use_percent", 100, 1, 10_000);
        STATION_MELTDOWNS = BUILDER
                .comment("With the safeties off, a core left unstable or overheating explodes. Off: the interlock re-arms itself and SCRAMs instead.")
                .define("meltdowns", true);
        STATION_MELTDOWN_POWER = BUILDER
                .comment("Meltdown explosion strength (TNT is 4).")
                .defineInRange("meltdown_power", 8.0, 0.0, 20.0);
        STATION_MELTDOWN_RADIUS = BUILDER
                .comment("Radius in blocks of the crater a station meltdown digs, on top of the blast. 0 leaves only the blast.")
                .defineInRange("meltdown_crater_radius", 24, 0, 64);
        STATION_TILT_SECONDS = BUILDER
                .comment("In overdrive, how long a fuel channel may sit without a live rod before the core goes unstable.")
                .defineInRange("flux_tilt_seconds", 300, 10, 3600);
        BUILDER.pop();

        BUILDER.comment("The reactor announcer: spoken warnings from the reactors' control systems.").push("announcer");
        ANNOUNCER_ENABLED = BUILDER
                .comment("Reactors speak when something worth hearing happens (safeties, SCRAM, coolant loss, meltdown).",
                        "Off here turns it off for everyone; each player can also use the Voice/Speech volume slider.")
                .define("enabled", true);
        BUILDER.pop();

        BUILDER.comment("The overworld's sky.").push("sky");
        ROUND_SUN = BUILDER
                .comment("Draw the overworld's sun as a round, glowing disc in place of vanilla's square one.",
                        "Read on each client. The Dyson swarm always uses it.")
                .define("round_sun", true);
        BUILDER.pop();

        BUILDER.comment("Radiation. Only running reactors and meltdowns emit it.").push("radiation");
        RADIATION_ENABLED = BUILDER
                .comment("Players take a radiation dose near running reactors and meltdown sites.")
                .define("enabled", true);
        RADIATION_STRENGTH = BUILDER
                .comment("Multiplies every dose rate. 0.5 halves radiation, 2 doubles it.")
                .defineInRange("strength", 1.0, 0.0, 100.0);
        RADIATION_MOBS = BUILDER
                .comment("Mobs (animals, monsters, villagers) take a dose too, so strong reactors make no-go zones and",
                        "can run mob farms. Turn off to keep villagers near reactors safe.")
                .define("affect_mobs", true);
        BUILDER.pop();
    }

    private static final Map<ProcessingMachine, ModConfigSpec.IntValue> MACHINE_DRAW = new EnumMap<>(ProcessingMachine.class);
    private static final Map<ProcessingMachine, ModConfigSpec.IntValue> MACHINE_TIME = new EnumMap<>(ProcessingMachine.class);

    static {
        BUILDER.comment("Processing machines. The energy one operation costs is its draw times its time, so raising either",
                "makes it dearer. Machines marked as gated need their full draw every tick (design section 5).").push("machines");
        for (ProcessingMachine machine : ProcessingMachine.values()) {
            BUILDER.push(machine.id());
            MACHINE_DRAW.put(machine, BUILDER
                    .comment("Percent of the normal FE per tick drawn while working (" + machine.baseEnergyPerTick() + " FE/t"
                            + (machine.gated() ? ", which is also its minimum draw)." : ")."))
                    .defineInRange("draw_percent", 100, 1, 100_000));
            MACHINE_TIME.put(machine, BUILDER
                    .comment("Percent of each recipe's normal time.")
                    .defineInRange("time_percent", 100, 1, 100_000));
            BUILDER.pop();
        }
        BUILDER.pop();
    }

    public static final ModConfigSpec.BooleanValue PREVIEW_CONTENT;

    static {
        BUILDER.comment("Unfinished content.").push("preview");
        PREVIEW_CONTENT = BUILDER
                .comment("Turn on unfinished content from the next tier: the Lithium Extractor, lithium target rods and the",
                        "creative Fusion Reactor Preview block.",
                        "Their products have no use yet. Needs a restart (or /reload) to add or remove their recipes.")
                .define("next_tier", false);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** A config value, or its default before the config has loaded (tooltips can ask very early). */
    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** A processing machine's draw, as a percent of normal. */
    public static int drawPercent(ProcessingMachine machine) {
        return get(MACHINE_DRAW.get(machine));
    }

    /** A processing machine's recipe times, as a percent of normal. */
    public static int timePercent(ProcessingMachine machine) {
        return get(MACHINE_TIME.get(machine));
    }

    /** Unknown ore IDs (for example from a datapack typo) stay enabled rather than silently vanishing. */
    public static boolean isOreGenerationEnabled(String oreId) {
        return OreType.byId(oreId).map(ore -> ORE_GENERATION.get(ore).get()).orElse(true);
    }

    private Config() {}
}
