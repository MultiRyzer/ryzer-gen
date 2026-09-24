package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.radiation.DosimeterRingItem;
import com.ryzer.ryzergen.radiation.Radiation;
import com.ryzer.ryzergen.radiation.RadiationClientState;
import com.ryzer.ryzergen.registry.ModSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The dosimeter's readout: a small gauge in the top left while the ring is worn or carried, and
 * Geiger clicks that come faster as the dose rate rises. Without a ring, radiation is silent.
 */
public final class RadiationHud {
    private RadiationHud() {}

    private static boolean hasRing() {
        Player player = Minecraft.getInstance().player;
        return player != null && DosimeterRingItem.isWorn(player);
    }

    /** Mod bus: register the HUD layer. */
    @EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
    public static final class Layers {
        @SubscribeEvent
        public static void register(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "dosimeter"),
                    RadiationHud::render);
        }
    }

    /** Game bus: Geiger clicks. */
    @EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
    public static final class Clicks {
        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            float rate = RadiationClientState.rate();
            if (minecraft.player == null || minecraft.isPaused() || rate < 0.02F || !hasRing()) {
                return;
            }
            // A Geiger counter clicks at random, more often the stronger the field.
            if (minecraft.player.getRandom().nextFloat() < Math.min(0.9F, rate * 0.4F)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.DOSIMETER_CLICK.get(),
                        0.9F + minecraft.player.getRandom().nextFloat() * 0.2F, 0.35F));
            }
        }
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !hasRing()) {
            return;
        }
        float dose = RadiationClientState.dose();
        float rate = RadiationClientState.rate();
        int x = 4;
        int y = 4;
        int w = 92;
        // Graphite panel with a thin light edge, like the machine GUIs.
        graphics.fill(x, y, x + w, y + 24, 0xC01D2126);
        graphics.fill(x, y, x + w, y + 1, 0xC04D545D);
        int colour = rate < 0.1F ? 0xFF44D65E : rate < 1F ? 0xFFF0A030 : 0xFFD13C3C;
        graphics.drawString(minecraft.font, Component.translatable("hud.ryzergen.dose", String.format("%.0f", dose)),
                x + 4, y + 3, 0xFFC9D0D7, false);
        graphics.drawString(minecraft.font, Component.translatable("hud.ryzergen.rate", String.format("%.2f", rate)),
                x + 4, y + 13, colour, false);
        // Dose bar along the right: fills towards the damage threshold, marked at each effect threshold.
        int bx = x + w - 8;
        int bh = 20;
        float fraction = Mth.clamp(dose / Radiation.DAMAGE, 0, 1);
        graphics.fill(bx, y + 2, bx + 4, y + 2 + bh, 0xFF15171B);
        GuiGauges.glow(graphics, bx, y + 2, 4, bh, fraction, dose >= Radiation.SICKNESS ? 0xFFD13C3C : dose >= Radiation.WEAKNESS ? 0xFFF0A030 : 0xFF44D65E);
        for (float mark : new float[] {Radiation.WEAKNESS, Radiation.SICKNESS}) {
            int my = y + 2 + bh - Math.round(mark / Radiation.DAMAGE * bh);
            graphics.fill(bx - 1, my, bx + 5, my + 1, 0xFF868F9A);
        }
    }
}
