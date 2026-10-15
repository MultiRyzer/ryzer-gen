package com.ryzer.ryzergen.item;

import com.ryzer.ryzergen.Config;
import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The Operator's Handbook: use it to open the handbook screen, which teaches the basics and, above
 * all, the progression map's key. Every player is given one the first time they join a world (a
 * config switch turns this off), and it is crafted from paper and uranium.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID)
public class HandbookItem extends Item {
    /** Kept in the player's persisted data, so the book is given once per world, deaths included. */
    private static final String GIVEN = RyzerGen.MOD_ID + ":handbook_given";

    public HandbookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            com.ryzer.ryzergen.client.handbook.HandbookScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @SubscribeEvent
    public static void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && Config.GIVE_HANDBOOK.get()) {
            giveOnce(player);
        }
    }

    /** Gives the player a handbook unless they have had one in this world; true if it gave one. */
    public static boolean giveOnce(Player player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(GIVEN)) {
            return false;
        }
        persisted.putBoolean(GIVEN, true);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        ItemStack book = new ItemStack(ModItems.OPERATORS_HANDBOOK.get());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        return true;
    }
}
