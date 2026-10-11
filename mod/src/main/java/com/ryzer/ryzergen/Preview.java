package com.ryzer.ryzergen;

import com.mojang.serialization.MapCodec;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.List;

/**
 * Content from the next tier that is built but not finished yet: tritium breeding (lithium target
 * rods, design section 10, for fusion) and the creative previews of fusion, the sun gate and the
 * mega drill. The breeder and the thorium line left the preview with tier 4. Preview items stay
 * registered so worlds keep their blocks and items, but while the preview config is off their
 * recipes do not load and they are left out of the creative tab and recipe viewers.
 */
public final class Preview {
    /** Recipes guarded by this load only with the preview on. */
    public static final ICondition CONDITION = new Condition();

    private Preview() {}

    public static boolean enabled() {
        return Config.SPEC.isLoaded() ? Config.PREVIEW_CONTENT.get() : Config.PREVIEW_CONTENT.getDefault();
    }

    public static List<ItemLike> items() {
        return List.of(ModItems.LITHIUM_TARGET_ROD.get(), ModItems.IRRADIATED_TARGET_ROD.get(), ModItems.FUSION_PREVIEW.get(),
                ModItems.SUN_GATE_PREVIEW.get(), ModItems.MEGA_DRILL_PREVIEW.get());
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
