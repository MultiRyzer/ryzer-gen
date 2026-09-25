package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.Preview;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RyzerGen.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ryzergen"))
                    .icon(() -> ModItems.GRAPHITE.get().getDefaultInstance())
                    .displayItems((params, output) -> ModItems.ITEMS.getEntries().stream()
                            .map(item -> item.get().getDefaultInstance())
                            .filter(stack -> !Preview.hidden(stack))
                            .forEach(output::accept))
                    .build());

    private ModCreativeTabs() {}
}
