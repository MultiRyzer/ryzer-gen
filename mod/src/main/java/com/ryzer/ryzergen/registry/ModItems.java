package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RyzerGen.MOD_ID);

    public static final DeferredItem<Item> GRAPHITE = ITEMS.registerSimpleItem("graphite");

    private ModItems() {}
}
