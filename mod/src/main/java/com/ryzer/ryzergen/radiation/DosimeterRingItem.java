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
 * slot if that mod is installed, or simply carried anywhere in the inventory. It shields you, and
 * quietly records your total dose (shown in its tooltip); the live readout and the clicks come from
 * the Geiger counter.
 *
 * <p>Real basis: nuclear workers wear ring and badge dosimeters, and fluorite (calcium fluoride) is a
 * real thermoluminescent dosimeter material. The fudge, as the design says: ours also protects.
 * Linings (lead, then silicon carbide, then tungsten) will add protection later instead of new rings.
 */
public class DosimeterRingItem extends Item {
    /**
     * Share of the dose the plain ring blocks: all of it, for now. There is no other radiation gear
     * yet (and may never be), so the ring is the whole answer. Lower this if linings arrive.
     */
    public static final float PROTECTION = 1.0F;

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
        // The dose the ring has recorded: the player's own, as last sent to this client.
        if (context.level() != null && context.level().isClientSide) {
            tooltip.add(Component.translatable("item.ryzergen.dosimeter_ring.dose", String.format("%.0f", RadiationClientState.dose()))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
