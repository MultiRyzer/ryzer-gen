package com.ryzer.ryzergen.sky;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A creative-only test tool for the Dyson swarm (see SunSwarm): use it to launch the swarm round
 * the sun, again to close the shell, and again to collapse the enclosed sun into a wormhole. Sneak
 * and use it to light a new sun. It has no recipe, and only operators can use it, since it changes
 * the sky for everyone.
 */
public class SwarmControllerItem extends Item {
    public SwarmControllerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResultHolder.success(stack);
        }
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("message.ryzergen.swarm.no_permission"), true);
            return InteractionResultHolder.fail(stack);
        }
        SunSwarm swarm = SunSwarm.get(server);
        String message;
        if (player.isShiftKeyDown()) {
            swarm.reset(server);
            message = "reset";
        } else if (swarm.advance(server)) {
            message = switch (swarm.phase()) {
                case LAUNCHING -> "launching";
                case CLOSING -> "closing";
                default -> "collapsing";
            };
        } else {
            message = swarm.phase() == SunSwarm.Phase.WORMHOLE ? "wormhole" : "busy";
        }
        player.displayClientMessage(Component.translatable("message.ryzergen.swarm." + message), true);
        return InteractionResultHolder.success(stack);
    }
}
