package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.HomeBatteryMenu;
import com.ryzer.ryzergen.cable.CableKind;
import com.ryzer.ryzergen.cable.CableMenu;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterMenu;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterMenu;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorMenu;
import com.ryzer.ryzergen.machine.fission.StationControlMenu;
import com.ryzer.ryzergen.machine.fission.StationCoreMenu;
import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.machine.processing.ProcessingMenu;
import com.ryzer.ryzergen.machine.pump.IntakePumpMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RyzerGen.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AlloySmelterMenu>> ALLOY_SMELTER =
            MENUS.register("alloy_smelter", () -> new MenuType<>(AlloySmelterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricAlloySmelterMenu>> ELECTRIC_ALLOY_SMELTER =
            MENUS.register("electric_alloy_smelter", () -> new MenuType<>(ElectricAlloySmelterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<CableMenu>> ENERGY_CABLE =
            MENUS.register("energy_cable", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new CableMenu(CableKind.ENERGY, id, inventory, buffer)));

    public static final DeferredHolder<MenuType<?>, MenuType<CableMenu>> ITEM_PIPE =
            MENUS.register("item_pipe", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new CableMenu(CableKind.ITEMS, id, inventory, buffer)));

    public static final DeferredHolder<MenuType<?>, MenuType<CableMenu>> FLUID_PIPE =
            MENUS.register("fluid_pipe", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new CableMenu(CableKind.FLUID, id, inventory, buffer)));

    public static final DeferredHolder<MenuType<?>, MenuType<CableMenu>> GAS_PIPE =
            MENUS.register("gas_pipe", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new CableMenu(CableKind.GAS, id, inventory, buffer)));

    public static final DeferredHolder<MenuType<?>, MenuType<IntakePumpMenu>> INTAKE_PUMP =
            MENUS.register("intake_pump", () -> new MenuType<>(IntakePumpMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<StationCoreMenu>> STATION_CORE =
            MENUS.register("station_core", () -> new MenuType<>(StationCoreMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<StationControlMenu>> STATION_CONTROL =
            MENUS.register("station_control", () -> new MenuType<>(StationControlMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<HomeBatteryMenu>> HOME_BATTERY =
            MENUS.register("home_battery", () -> new MenuType<>(HomeBatteryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<MicroreactorMenu>> MICROREACTOR =
            MENUS.register("microreactor", () -> new MenuType<>(MicroreactorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> CORE_CRACKER = processing(ProcessingMachine.CORE_CRACKER);
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> REPROCESSOR = processing(ProcessingMachine.REPROCESSOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> FUEL_FABRICATOR = processing(ProcessingMachine.FUEL_FABRICATOR);
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> LITHIUM_EXTRACTOR = processing(ProcessingMachine.LITHIUM_EXTRACTOR);

    private static DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> processing(ProcessingMachine machine) {
        return MENUS.register(machine.id(), () -> new MenuType<>(
                (id, inventory) -> new ProcessingMenu(machine, id, inventory), FeatureFlags.DEFAULT_FLAGS));
    }

    private ModMenus() {}
}
