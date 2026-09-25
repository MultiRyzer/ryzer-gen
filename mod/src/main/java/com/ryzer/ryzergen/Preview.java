package com.ryzer.ryzergen;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.List;

/**
 * Content from the next tier that is built but not finished yet (the lithium side of design
 * section 10). It stays registered so worlds keep their blocks and items, but while the preview
 * config is off its recipes do not load and it is left out of the creative tab and recipe viewers.
 */
public final class Preview {
    /** Recipes guarded by this load only with the preview on. */
    public static final ICondition CONDITION = new Condition();

    private Preview() {}

    public static boolean enabled() {
        return Config.SPEC.isLoaded() ? Config.PREVIEW_CONTENT.get() : Config.PREVIEW_CONTENT.getDefault();
    }

    public static List<ItemLike> items() {
        return List.of(ModItems.LITHIUM_EXTRACTOR.get(), ModItems.LITHIUM_DUST.get(),
                ModItems.LITHIUM_TARGET_ROD.get(), ModItems.IRRADIATED_TARGET_ROD.get());
    }

    /** True for preview items while the preview is off, so they can be hidden. */
    public static boolean hidden(ItemStack stack) {
        return !enabled() && items().stream().anyMatch(item -> stack.is(item.asItem()));
    }

    private record Condition() implements ICondition {
        static final MapCodec<Condition> CODEC = MapCodec.unit(new Condition());

        @Override
        public boolean test(IContext context) {
            return enabled();
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    /** The condition's codec, registered as {@code ryzergen:preview}. */
    public static MapCodec<? extends ICondition> codec() {
        return Condition.CODEC;
    }
}
