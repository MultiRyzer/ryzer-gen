package com.ryzer.ryzergen.client.progression;

import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The mod's whole production line, from ore in the ground to the sun (design pillar 6: roots run
 * deep), as the progression map draws it: a node per step, in tier columns, each with the steps
 * that feed it. Text lives in the lang file: {@code progression.ryzergen.<id>} for a node's name
 * (else its item's), {@code .info.1} and on for how it works, {@code .spoiler.1} and on for the
 * optimal setups, which the panel hides until asked.
 */
public final class ProgressionMap {
    /** A spoiler layout drawn as a grid: rows of letters, each letter a coloured cell; '.' is blank. */
    public record Grid(String title, String[] rows, boolean hex) {}

    /**
     * A step. {@code icon} is the item shown, and the one that marks it reached; {@code inputs} are
     * the steps that feed it. Preview steps show only with the preview setting on.
     */
    public record Node(String id, Supplier<? extends ItemLike> icon, int column, int row, List<String> inputs, boolean preview,
                       List<Grid> grids) {
        public Item item() {
            return icon.get().asItem();
        }
    }

    /** Column headings, left to right. */
    public static final String[] COLUMNS = {"dig", "materials", "microreactor", "fuel_cycle", "fission", "breeder", "fusion", "swarm"};
    /** The first column that is still a preview: shown greyed as "more to come" with the preview off. */
    public static final int FIRST_PREVIEW_COLUMN = 5;

    /**
     * Cell colours for the spoiler grids, by letter, as each control screen colours its plan: the
     * station's fuel green, moderator amber, control red, coolant blue; the breeder's fuel cyan (B)
     * and blanket green (b).
     */
    public static final Map<Character, Integer> CELL_COLOURS = Map.of(
            'F', 0xFF44D65E, 'M', 0xFFE8B030, 'R', 0xFFE0503C, 'C', 0xFF3A8CF0,
            'B', 0xFF35C8F5, 'b', 0xFF44D65E);

    private static final Map<String, Node> NODES = new LinkedHashMap<>();

    private static void add(String id, Supplier<? extends ItemLike> icon, int column, int row, String... inputs) {
        NODES.put(id, new Node(id, icon, column, row, List.of(inputs), column >= FIRST_PREVIEW_COLUMN, List.of()));
    }

    private static void add(String id, Supplier<? extends ItemLike> icon, int column, int row, List<Grid> grids, String... inputs) {
        NODES.put(id, new Node(id, icon, column, row, List.of(inputs), column >= FIRST_PREVIEW_COLUMN, grids));
    }

    private static Supplier<ItemLike> drop(OreType ore) {
        return () -> ModItems.ORE_DROPS.get(ore).get();
    }

    private static Supplier<ItemLike> ingot(OreType ore) {
        return () -> ModItems.INGOTS.get(ore).get();
    }

    static {
        // From the ground.
        add("iron", () -> Items.RAW_IRON, 0, 0);
        add("coal", () -> Items.COAL, 0, 1);
        add("copper", () -> Items.RAW_COPPER, 0, 2);
        add("sand", () -> Items.SAND, 0, 3);
        add("redstone", () -> Items.REDSTONE, 0, 4);
        add("gold", () -> Items.RAW_GOLD, 0, 5);
        add("uranium", drop(OreType.URANIUM), 0, 6);
        add("lead", drop(OreType.LEAD), 0, 7);
        add("fluorite", drop(OreType.FLUORITE), 0, 8);
        add("silver", drop(OreType.SILVER), 0, 9);
        add("salt", drop(OreType.SALT), 0, 10);

        // Tier 1 materials, from the fuel-burning alloy smelter and the blast furnace.
        add("alloy_smelter", ModItems.ALLOY_SMELTER, 1, 0, "iron", "coal");
        add("steel", ModItems.STEEL_INGOT, 1, 1, "alloy_smelter", "iron", "coal");
        add("graphite", ModItems.GRAPHITE, 1, 2, "coal");
        add("silicon_carbide", ModItems.SILICON_CARBIDE, 1, 3, "sand", "graphite");
        add("basic_board", ModItems.BASIC_CONTROL_BOARD, 1, 4, "redstone", "copper", "iron");
        add("uranium_ingot", ingot(OreType.URANIUM), 1, 6, "uranium");
        add("lead_ingot", ingot(OreType.LEAD), 1, 7, "lead");

        // Tier 1: the microreactor.
        add("triso", ModItems.TRISO_PELLETS, 2, 2, "uranium_ingot", "graphite", "silicon_carbide");
        add("fuel_core", ModItems.SEALED_FUEL_CORE, 2, 3, "triso", "steel", "graphite");
        add("microreactor", ModItems.REACTOR_HEART, 2, 4, List.of(), "fuel_core", "steel", "lead_ingot", "basic_board");
        add("intake_pump", ModItems.INTAKE_PUMP, 2, 5, "steel", "basic_board");
        add("energy_cable", ModItems.ENERGY_CABLE, 2, 6, "copper", "redstone");

        // Tier 2: the fuel cycle.
        add("electric_smelter", ModItems.ELECTRIC_ALLOY_SMELTER, 3, 0, "alloy_smelter", "steel", "basic_board");
        add("silicon", ModItems.SILICON, 3, 1, "electric_smelter", "sand", "coal");
        add("advanced_board", ModItems.ADVANCED_CONTROL_BOARD, 3, 2, "silicon", "steel", "gold");
        add("core_cracker", ModItems.CORE_CRACKER, 3, 3, "microreactor", "steel", "basic_board");
        add("pool", ModItems.POOL_CONTROLLER, 3, 4, "microreactor", "steel", "basic_board");
        add("reprocessor", ModItems.REPROCESSOR, 3, 5, "core_cracker", "pool", "advanced_board", "lead_ingot", "fluorite");
        add("plutonium", ModItems.PLUTONIUM_INGOT, 3, 6, "reprocessor");
        add("fuel_fabricator", ModItems.FUEL_FABRICATOR, 3, 7, "advanced_board", "steel");

        // Tier 3: fission.
        add("uranium_rod", ModItems.URANIUM_FUEL_ROD, 4, 0, "fuel_fabricator", "uranium_ingot", "steel");
        add("mox_rod", ModItems.MOX_FUEL_ROD, 4, 1, "fuel_fabricator", "plutonium");
        add("control_rod", ModItems.CONTROL_ROD, 4, 2, "silver", "steel");
        add("station", ModItems.STATION_CORE, 4, 4, List.of(
                new Grid("uranium_power", flip(new String[] {"FFFFC", "CCCFF", "FFFCF", "FCFCM", "FCFFM"}), false),
                new Grid("uranium_economy", flip(new String[] {"MRMRM", "RFCMC", "CMFMM", "MFMFM", "MCMCC"}), false),
                new Grid("mox_power", flip(new String[] {"FCFFF", "FCFCC", "FFCFF", "CCFFF", "FFFCC"}), false)),
                "uranium_rod", "mox_rod", "control_rod", "advanced_board", "graphite", "lead_ingot");
        add("lithium_extractor", ModItems.LITHIUM_EXTRACTOR, 4, 6, "salt", "advanced_board");
        add("container_battery", ModItems.BATTERY_CONTROLLER, 4, 7, "lithium_extractor", "advanced_board");

        // Tier 4: the breeder (preview).
        add("electrorefiner", ModItems.ELECTROREFINER, 5, 0, "station", "advanced_board");
        add("transuranic", ModItems.TRANSURANIC_METAL, 5, 1, "electrorefiner", "mox_rod");
        add("sodium", ModItems.SODIUM_INGOT, 5, 2, "electrorefiner", "salt");
        add("breeder_fuel", ModItems.BREEDER_FUEL, 5, 3, "transuranic", "fuel_fabricator");
        add("uranium_blanket", ModItems.URANIUM_BLANKET, 5, 4, "fuel_fabricator", "uranium_ingot");
        add("breeder", ModItems.BREEDER_CORE, 5, 5, List.of(
                new Grid("breeder_economy", new String[] {"BBB", "BbBB", "BBbbb", "bbbb", "bbb"}, true),
                new Grid("breeder_breeding", new String[] {"BbB", "bBbb", "BBbBB", "bbBb", "Bbb"}, true)),
                "breeder_fuel", "sodium", "uranium_blanket", "station");
        add("tritium", ModItems.IRRADIATED_TARGET_ROD, 5, 7, "breeder", "lithium_extractor");

        // Tier 5 and 6 (preview).
        add("fusion", ModItems.FUSION_PREVIEW, 6, 5, "tritium", "breeder");
        add("swarm", ModItems.SWARM_CONTROLLER, 7, 5, "fusion");
    }

    /**
     * The station's best layouts as core_optimiser.py prints them have the front row first and
     * channel 0 on the left; the control screen shows the front row at the bottom and mirrors x, so
     * turned half round they read as the screen shows them.
     */
    private static String[] flip(String[] rows) {
        String[] out = new String[rows.length];
        for (int i = 0; i < rows.length; i++) {
            out[rows.length - 1 - i] = new StringBuilder(rows[i]).reverse().toString();
        }
        return out;
    }

    private ProgressionMap() {}

    /** The steps shown: preview steps only with the preview on. */
    public static List<Node> nodes() {
        List<Node> out = new ArrayList<>();
        for (Node node : NODES.values()) {
            if (!node.preview() || Preview.enabled()) {
                out.add(node);
            }
        }
        return out;
    }

    public static Node get(String id) {
        return NODES.get(id);
    }
}
