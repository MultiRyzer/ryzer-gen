package com.ryzer.ryzergen.compat;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.machine.fission.StationReactor;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * What the JEI and EMI plugins show besides recipes, kept here so both show the same thing: the
 * Reactor Fuel page (each fuel, what it burns down to, the reactor that burns it, its power and
 * life, from the config) and the info pages on the multiblock parts. Plain game classes only, so
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
                        Component.translatable("jei.ryzergen.fuel.microreactor", String.format("%,d", output)),
                        Component.translatable("jei.ryzergen.fuel.life", coreMinutes)),
                new Fuel(stack(ModItems.URANIUM_FUEL_ROD.get()), stack(ModItems.SPENT_URANIUM_ROD.get()), stack(ModItems.STATION_CORE.get()),
                        Component.translatable("jei.ryzergen.fuel.station", String.format("%,d", Math.round(StationReactor.URANIUM_HEAT * multiplier))),
                        Component.translatable("jei.ryzergen.fuel.life", StationReactor.URANIUM_LIFE / 1200)),
                new Fuel(stack(ModItems.MOX_FUEL_ROD.get()), stack(ModItems.SPENT_MOX_ROD.get()), stack(ModItems.STATION_CORE.get()),
                        Component.translatable("jei.ryzergen.fuel.station", String.format("%,d", Math.round(StationReactor.MOX_HEAT * multiplier))),
                        Component.translatable("jei.ryzergen.fuel.life", StationReactor.MOX_LIFE / 1200))));
        if (Preview.enabled()) {
            // Not a fuel, but it burns down in the core the same way: lithium in, tritium out.
            fuels.add(new Fuel(stack(ModItems.LITHIUM_TARGET_ROD.get()), stack(ModItems.IRRADIATED_TARGET_ROD.get()), stack(ModItems.STATION_CORE.get()),
                    Component.translatable("jei.ryzergen.fuel.target"), Component.translatable("jei.ryzergen.fuel.target_cost")));
        }
        return fuels;
    }

    /** An info page: the items it is shown on, and its text. */
    public record Info(String id, List<ItemStack> items, Component text) {}

    public static List<Info> info() {
        return List.of(
                new Info("microreactor", List.of(stack(ModItems.REACTOR_HEART.get()), stack(ModItems.REACTOR_MACHINE_UNIT.get()),
                        stack(ModItems.COOLANT_JACKET.get())), Component.translatable("jei.ryzergen.info.microreactor")),
                new Info("fission_station", List.of(stack(ModItems.STATION_CORE.get()), stack(ModItems.STATION_CASING.get()),
                        stack(ModItems.STATION_GLASS.get()), stack(ModItems.TURBINE_ROTOR.get())), Component.translatable("jei.ryzergen.info.fission_station")),
                new Info("station_core", List.of(stack(ModItems.GRAPHITE_BLOCK.get()), stack(ModItems.CONTROL_ROD.get())),
                        Component.translatable("jei.ryzergen.info.station_core")),
                new Info("fittings", List.of(stack(ModItems.SILVER_FITTINGS.get()), stack(ModItems.BUSBAR_FITTINGS.get()),
                        stack(ModItems.CRYOGENIC_FITTINGS.get())), Component.translatable("jei.ryzergen.info.fittings")));
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }
}
