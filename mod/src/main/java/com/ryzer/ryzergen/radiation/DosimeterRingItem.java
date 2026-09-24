package com.ryzer.ryzergen.radiation;

import com.ryzer.ryzergen.compat.accessories.AccessoriesCompat;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The dosimeter ring: a fluorite thermoluminescent chip in a metal band. Worn in the Accessories ring
 * slot if that mod is installed, or simply carried anywhere in the inventory. Shows the dose on the
 * HUD, clicks like a Geiger counter near sources, and takes the edge off exposure.
 *
 * <p>Real basis: nuclear workers wear ring and badge dosimeters, and fluorite (calcium fluoride) is a
 * real thermoluminescent dosimeter material. The fudge, as the design says: ours also protects.
 * Linings (lead, then silicon carbide, then tungsten) will add protection later instead of new rings.
 */
public class DosimeterRingItem extends Item {
    /** Share of the dose the plain ring blocks. */
    public static final float PROTECTION = 0.25F;

    public DosimeterRingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** True if the player wears the ring in an accessory slot or carries it anywhere. */
    public static boolean isWorn(Player player) {
        Item ring = ModItems.DOSIMETER_RING.get();
        return player.getInventory().hasAnyMatching(stack -> stack.is(ring)) || AccessoriesCompat.isEquipped(player, ring);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ryzergen.dosimeter_ring.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.ryzergen.dosimeter_ring.protection", Math.round(PROTECTION * 100))
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
