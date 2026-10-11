package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.client.text.RichText;
import com.ryzer.ryzergen.client.text.Term;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/**
 * A badge in the top left corner of each cable and pipe in a slot: the icon of what it carries, in
 * its term's colour (a bolt for energy, a drop for liquids, wisps for gases, a crate for items), so
 * the four read apart at a glance. Drawn as the item count is, over the item.
 */
@EventBusSubscriber(modid = RyzerGen.MOD_ID, value = Dist.CLIENT)
public final class CableBadges {
    private CableBadges() {}

    @SubscribeEvent
    public static void register(RegisterItemDecorationsEvent event) {
        badge(event, ModItems.ENERGY_CABLE.get(), Term.POWER);
        badge(event, ModItems.ITEM_PIPE.get(), Term.ITEMS);
        badge(event, ModItems.FLUID_PIPE.get(), Term.COOLANT);
        badge(event, ModItems.GAS_PIPE.get(), Term.STEAM);
    }

    private static void badge(RegisterItemDecorationsEvent event, Item item, Term term) {
        event.register(item, (graphics, font, stack, x, y) -> {
            graphics.pose().pushPose();
            // Above the item, as the count is.
            graphics.pose().translate(0, 0, 200);
            graphics.drawString(font, RichText.term(term, "", false), x, y, 0xFFFFFFFF, true);
            graphics.pose().popPose();
            return false;
        });
    }
}
