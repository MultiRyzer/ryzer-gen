package com.ryzer.ryzergen.machine.pool;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * Spent fuel fresh out of a reactor is hot: its fission products keep decaying and giving off heat.
 * Reactors mark what they give out, and the Core Cracker and Reprocessor refuse marked items until
 * a Spent Fuel Pool has cooled them (design section 7). Items with no mark count as cooled, so
 * nothing made before the pool existed is stranded.
 */
public final class HotFuel {
    private HotFuel() {}

    /** Marks spent fuel as hot, unless the config says cooling is not required. */
    public static ItemStack mark(ItemStack stack) {
        if (Config.REQUIRE_COOLING.get()) {
            stack.set(ModDataComponents.HOT.get(), true);
        }
        return stack;
    }

    public static boolean isHot(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.HOT.get(), false);
    }

    /**
     * Whether the pool's racks take an item: spent fuel of any kind, hot or not. Fuel that is
     * already cool (made before the pool existed, or with cooling turned off) passes straight through.
     */
    public static boolean isSpentFuel(ItemStack stack) {
        return isHot(stack) || stack.is(ModItems.DEPLETED_FUEL_CORE.get()) || stack.is(ModItems.SPENT_URANIUM_ROD.get())
                || stack.is(ModItems.SPENT_MOX_ROD.get()) || stack.is(ModItems.SPENT_BREEDER_FUEL.get());
    }

    /** Takes the mark off: the item has cooled. */
    public static void cool(ItemStack stack) {
        stack.remove(ModDataComponents.HOT.get());
    }

    /**
     * Ticks a hot item cools for: a microreactor core cools faster than a big fission rod, which
     * holds far more fuel. Fuel already cool takes a tick, just passing through.
     */
    public static int coolingTicks(ItemStack stack) {
        if (!isHot(stack)) {
            return 1;
        }
        boolean core = stack.is(ModItems.DEPLETED_FUEL_CORE.get());
        return 20 * Config.get(core ? Config.CORE_COOLING_SECONDS : Config.ROD_COOLING_SECONDS);
    }
}
