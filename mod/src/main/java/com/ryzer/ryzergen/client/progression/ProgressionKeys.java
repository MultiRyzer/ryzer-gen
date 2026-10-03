package com.ryzer.ryzergen.client.progression;

import com.mojang.blaze3d.platform.InputConstants;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** The progression map's key (Y by default, rebindable under Controls), and the tracker's tick. */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class ProgressionKeys {
    public static final KeyMapping OPEN_MAP = new KeyMapping("key.ryzergen.progression", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.ryzergen");

    private ProgressionKeys() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAP);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ProgressionTracker.tick(minecraft);
        while (OPEN_MAP.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new ProgressionScreen());
            }
        }
    }
}
