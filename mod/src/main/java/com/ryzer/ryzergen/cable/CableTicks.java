package com.ryzer.ryzergen.cable;

import com.ryzer.ryzergen.RyzerGen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** End-of-tick cable work: rescans changed networks, then energy networks top their machines up from batteries. */
@EventBusSubscriber(modid = RyzerGen.MOD_ID)
public final class CableTicks {
    private CableTicks() {}

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            CableNetwork.endOfTick(level);
        }
    }
}
