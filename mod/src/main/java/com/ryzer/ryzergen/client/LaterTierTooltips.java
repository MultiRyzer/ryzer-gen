package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.material.OreType;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Materials that generate now but are only used by a later tier (aluminium, tungsten, monazite and
 * salt). Their worldgen stays on, because ores only appear in new chunks: turning it off for the
 * alpha would leave today's worlds short when those tiers land. The tooltip says why they exist.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class LaterTierTooltips {
    private static final Set<OreType> LATER = EnumSet.of(OreType.ALUMINIUM, OreType.TUNGSTEN, OreType.MONAZITE, OreType.SALT);

    private LaterTierTooltips() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        for (OreType ore : LATER) {
            if (stack.is(ModItems.ORE_DROPS.get(ore).get()) || (ore.hasIngot() && stack.is(ModItems.INGOTS.get(ore).get()))) {
                event.getToolTip().add(Component.translatable("tooltip.ryzergen.later_tier").withStyle(style -> style.withColor(0xFF9AA3AE)));
                return;
            }
        }
    }
}
