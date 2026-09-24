package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.machine.alloysmelter.AlloySmelterMenu;
import com.ryzer.ryzergen.machine.microreactor.MicroreactorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RyzerGen.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<AlloySmelterMenu>> ALLOY_SMELTER =
            MENUS.register("alloy_smelter", () -> new MenuType<>(AlloySmelterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<MicroreactorMenu>> MICROREACTOR =
            MENUS.register("microreactor", () -> new MenuType<>(MicroreactorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {}
}
