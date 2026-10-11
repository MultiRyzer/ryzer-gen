package com.ryzer.ryzergen.compat;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.client.text.RichText;
import com.ryzer.ryzergen.machine.breeder.BreederReactor;
import com.ryzer.ryzergen.machine.drill.MegaDrillLayout;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.registry.ModFluids;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * What the JEI and EMI plugins show besides recipes, kept here so both show the same thing: the
 * Reactor Fuel page (each fuel, what it burns down to, the reactor that burns it, its power and
 * life, from the config) and the info pages on the multiblock parts and on steam. Plain game classes only, so
 * nothing here needs either viewer.
 */
public final class RecipeViewerPages {
    private RecipeViewerPages() {}

    /** One fuel on the Reactor Fuel page. */
    public record Fuel(ItemStack fuel, ItemStack spent, ItemStack reactor, Component power, Component life) {}

    public static List<Fuel> fuels() {
        int output = Config.get(Config.MICROREACTOR_OUTPUT);
        int coreMinutes = Config.get(Config.MICROREACTOR_FUEL_LIFE) / 1200;
        float multiplier = Config.get(Config.STATION_OUTPUT) / 100F;
        List<Fuel> fuels = new ArrayList<>(List.of(
                new Fuel(stack(ModItems.SEALED_FUEL_CORE.get()), stack(ModItems.DEPLETED_FUEL_CORE.get()), stack(ModItems.REACTOR_HEART.get()),
                        RichText.trLight("jei.ryzergen.fuel.microreactor", String.format("%,d", output)),
                        RichText.trLight("jei.ryzergen.fuel.life", coreMinutes)),
                new Fuel(stack(ModItems.URANIUM_FUEL_ROD.get()), stack(ModItems.SPENT_URANIUM_ROD.get()), stack(ModItems.STATION_CORE.get()),
                        RichText.trLight("jei.ryzergen.fuel.station", String.format("%,d", Math.round(StationReactor.URANIUM_HEAT * multiplier))),
                        RichText.trLight("jei.ryzergen.fuel.life", StationReactor.URANIUM_LIFE / 1200)),
                new Fuel(stack(ModItems.MOX_FUEL_ROD.get()), stack(ModItems.SPENT_MOX_ROD.get()), stack(ModItems.STATION_CORE.get()),
                        RichText.trLight("jei.ryzergen.fuel.station", String.format("%,d", Math.round(StationReactor.MOX_HEAT * multiplier))),
                        RichText.trLight("jei.ryzergen.fuel.life", StationReactor.MOX_LIFE / 1200))));
        fuels.add(new Fuel(stack(ModItems.U233_FUEL_ROD.get()), stack(ModItems.SPENT_U233_ROD.get()), stack(ModItems.STATION_CORE.get()),
                RichText.trLight("jei.ryzergen.fuel.station", String.format("%,d", Math.round(StationReactor.U233_HEAT * multiplier))),
                RichText.trLight("jei.ryzergen.fuel.life", StationReactor.U233_LIFE / 1200)));
        float breeder = Config.get(Config.BREEDER_OUTPUT) / 100F;
        fuels.add(new Fuel(stack(ModItems.BREEDER_FUEL.get()), stack(ModItems.SPENT_BREEDER_FUEL.get()), stack(ModItems.BREEDER_CORE.get()),
                RichText.trLight("jei.ryzergen.fuel.assembly", String.format("%,d", Math.round(BreederReactor.FUEL_HEAT * breeder))),
                RichText.trLight("jei.ryzergen.fuel.life", BreederReactor.FUEL_LIFE / 1200)));
        // Not fuels, but they change in the breeder's core the same way: each blanket catches the
        // fuel's spare neutrons and comes out bred.
        fuels.add(new Fuel(stack(ModItems.URANIUM_BLANKET.get()), stack(ModItems.BRED_URANIUM_BLANKET.get()), stack(ModItems.BREEDER_CORE.get()),
                RichText.trLight("jei.ryzergen.fuel.blanket_uranium"), RichText.trLight("jei.ryzergen.fuel.target_cost")));
        fuels.add(new Fuel(stack(ModItems.THORIUM_BLANKET.get()), stack(ModItems.BRED_THORIUM_BLANKET.get()), stack(ModItems.BREEDER_CORE.get()),
                RichText.trLight("jei.ryzergen.fuel.blanket_thorium"), RichText.trLight("jei.ryzergen.fuel.blanket_slow")));
        if (Preview.enabled()) {
            fuels.add(new Fuel(stack(ModItems.LITHIUM_TARGET_ROD.get()), stack(ModItems.IRRADIATED_TARGET_ROD.get()), stack(ModItems.BREEDER_CORE.get()),
                    RichText.trLight("jei.ryzergen.fuel.target"), RichText.trLight("jei.ryzergen.fuel.target_cost")));
        }
        return fuels;
    }

    /** An info page: the items and fluids it is shown on, and its lines, one fact each. */
    public record Info(String id, List<ItemStack> items, List<Fluid> fluids, List<Component> lines) {
        Info(String id, List<ItemStack> items, List<Component> lines) {
            this(id, items, List.of(), lines);
        }
    }

    /**
     * A page's lines: {@code jei.ryzergen.info.<key>.1} and on, in the mod's markup for the viewers'
     * light pages, each filled from the same values ({@code %1$s}, {@code %2$s} and on).
     */
    private static List<Component> lines(String key, Object... args) {
        List<Component> out = new ArrayList<>();
        for (int n = 1; RichText.has("jei.ryzergen.info." + key + "." + n); n++) {
            out.add(RichText.trLight("jei.ryzergen.info." + key + "." + n, args));
        }
        return out;
    }

    public static List<Info> info() {
        int water = Config.get(Config.MICROREACTOR_COOLANT_USE);
        int steam = water * Config.get(Config.STEAM_PER_WATER);
        int capacity = Config.get(Config.LFP_RACK_CAPACITY);
        int rate = Config.get(Config.LFP_RACK_RATE);
        List<Info> pages = new ArrayList<>(List.of(
                new Info("spent_fuel_pool", List.of(stack(ModItems.POOL_CONTROLLER.get()), stack(ModItems.POOL_LINER.get()),
                        stack(ModItems.POOL_CRANE.get())), lines(Config.get(Config.REQUIRE_COOLING) ? "spent_fuel_pool" : "spent_fuel_pool_optional",
                        minutes(Config.get(Config.CORE_COOLING_SECONDS)), minutes(Config.get(Config.ROD_COOLING_SECONDS)))),
                new Info("container_battery", List.of(stack(ModItems.BATTERY_CONTROLLER.get()), stack(ModItems.CONTAINER_FRAME.get()),
                        stack(ModItems.THERMAL_UNIT.get()), stack(ModItems.LFP_BATTERY_RACK.get())),
                        lines("container_battery", String.format("%,d", capacity), String.format("%,d", rate),
                                String.format("%,d", capacity * 20L), String.format("%,d", rate * 20))),
                new Info("steam", List.of(stack(ModItems.REACTOR_HEART.get()), stack(ModItems.STATION_CORE.get())),
                        List.of(ModFluids.STEAM.get()), lines("steam", water, String.format("%,d", steam), water * 2, String.format("%,d", steam * 2))),
                new Info("microreactor", List.of(stack(ModItems.REACTOR_HEART.get()), stack(ModItems.REACTOR_MACHINE_UNIT.get()),
                        stack(ModItems.COOLANT_JACKET.get())), lines("microreactor")),
                new Info("fission_station", List.of(stack(ModItems.STATION_CORE.get()), stack(ModItems.STATION_CASING.get()),
                        stack(ModItems.STATION_GLASS.get()), stack(ModItems.TURBINE_ROTOR.get())), lines("fission_station")),
                new Info("station_core", List.of(stack(ModItems.GRAPHITE_BLOCK.get()), stack(ModItems.CONTROL_ROD.get())),
                        lines("station_core")),
                new Info("fittings", List.of(stack(ModItems.SILVER_FITTINGS.get()), stack(ModItems.BUSBAR_FITTINGS.get()),
                        stack(ModItems.CRYOGENIC_FITTINGS.get())), lines("fittings")),
                new Info("mega_drill", List.of(stack(ModItems.MEGA_DRILL_CONTROLLER.get()), stack(ModItems.DRILL_FRAME.get()),
                        stack(ModItems.DRILL_HEAD_MK1.get()), stack(ModItems.TUNGSTEN_TIP.get())),
                        lines("mega_drill", MegaDrillLayout.CELLS - 1, String.format("%,d", Config.get(Config.DRILL_DRAW)),
                                String.format("%,d", Config.get(Config.DRILL_WATER))))));
        pages.add(new Info("breeder", List.of(stack(ModItems.BREEDER_CORE.get()), stack(ModItems.BREEDER_FRAME.get()),
                stack(ModItems.BREEDER_SHELL.get())), lines("breeder")));
        return pages;
    }

    /** A time in seconds as whole minutes, or minutes and seconds ("7:30") when it is not a whole number. */
    private static String minutes(int seconds) {
        return seconds % 60 == 0 ? Integer.toString(seconds / 60) : String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }
}
