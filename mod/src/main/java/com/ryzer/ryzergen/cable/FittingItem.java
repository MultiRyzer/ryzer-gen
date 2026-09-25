package com.ryzer.ryzergen.cable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A cable fitting (see {@link CableUpgrade}). Goes in a side's slot in the cable's panel. */
public class FittingItem extends Item {
    private final CableUpgrade tier;

    public FittingItem(CableUpgrade tier, Properties properties) {
        super(properties.stacksTo(16));
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ryzergen.fittings.tooltip", tier.multiplier())
                .withStyle(style -> style.withColor(0xFF9AA3AE)));
        if (tier == CableUpgrade.CRYOGENIC) {
            // Its recipe loads only when some mod supplies yttrium (we make it in tier 5).
            tooltip.add(Component.translatable("item.ryzergen.cryogenic_fittings.yttrium")
                    .withStyle(style -> style.withColor(0xFF9AA3AE)));
        }
    }
}
