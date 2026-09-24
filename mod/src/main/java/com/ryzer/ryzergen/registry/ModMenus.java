package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.battery.HomeBatteryMenu;
import com.ryzer.ryzergen.cable.EnergyCableMenu;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterMenu;
import com.ryzer.ryzergen.machine.electricsmelter.ElectricAlloySmelterMenu;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorMenu;
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

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyCableMenu>> ENERGY_CABLE =
            MENUS.register("energy_cable", () -> IMenuTypeExtension.create(EnergyCableMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HomeBatteryMenu>> HOME_BATTERY =
            MENUS.register("home_battery", () -> new MenuType<>(HomeBatteryMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<MicroreactorMenu>> MICROREACTOR =
            MENUS.register("microreactor", () -> new MenuType<>(MicroreactorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
