package com.ryzer.ryzergen.radiation;

import com.ryzer.ryzergen.compat.accessories.AccessoriesCompat;
import com.ryzer.ryzergen.registry.ModDataComponents;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Geiger counter: the radiation readout. While you carry one (anywhere in the inventory, or in
 * an Accessories belt slot), the HUD shows your dose and the dose rate around you, and it clicks
 * faster the stronger the field. Shift-right-click it to mute the clicks and keep the gauge. It measures,
 * it does not protect: that is the dosimeter ring's job.
 *
 * <p>Real basis: a Geiger-Muller tube, a gas-filled tube that gives a pulse, heard as a click, each
 * time radiation passes through it.
 */
public class GeigerCounterItem extends Item {
    public GeigerCounterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isMuted(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MUTED.get(), false);
    }

    /** The counter the player carries (inventory first, then an accessory slot), or empty. */
    public static ItemStack carried(Player player) {
        Item counter = ModItems.GEIGER_COUNTER.get();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(counter)) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(counter)) {
                return stack;
            }
        }
        return AccessoriesCompat.firstEquipped(player, counter);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Shift only: a plain right-click is how Accessories clips it to the belt.
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        boolean muted = !isMuted(stack);
        stack.set(ModDataComponents.MUTED.get(), muted);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(muted ? "message.ryzergen.geiger_counter.muted"
                    : "message.ryzergen.geiger_counter.unmuted"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.4F, muted ? 0.6F : 0.9F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(isMuted(stack) ? "item.ryzergen.geiger_counter.muted" : "item.ryzergen.geiger_counter.unmuted")
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
