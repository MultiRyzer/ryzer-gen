package com.ryzer.ryzergen.registry;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.recipe.AlloyingRecipe;
import com.ryzer.ryzergen.recipe.MachineRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, RyzerGen.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, RyzerGen.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyingRecipe>> ALLOYING_TYPE = TYPES.register("alloying",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "alloying")));
    public static final DeferredHolder<RecipeSerializer<?>, AlloyingRecipe.Serializer> ALLOYING_SERIALIZER =
            SERIALIZERS.register("alloying", AlloyingRecipe.Serializer::new);

    // The fuel cycle machines (design section 7): one recipe shape, a type per machine.
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> CRACKING_TYPE = type("cracking");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> REPROCESSING_TYPE = type("reprocessing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> FABRICATING_TYPE = type("fabricating");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> EXTRACTING_TYPE = type("extracting");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> ELECTROREFINING_TYPE = type("electrorefining");
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipe.Serializer> CRACKING_SERIALIZER =
            SERIALIZERS.register("cracking", () -> new MachineRecipe.Serializer(MachineRecipe.Process.CRACKING));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipe.Serializer> REPROCESSING_SERIALIZER =
            SERIALIZERS.register("reprocessing", () -> new MachineRecipe.Serializer(MachineRecipe.Process.REPROCESSING));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipe.Serializer> FABRICATING_SERIALIZER =
            SERIALIZERS.register("fabricating", () -> new MachineRecipe.Serializer(MachineRecipe.Process.FABRICATING));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipe.Serializer> EXTRACTING_SERIALIZER =
            SERIALIZERS.register("extracting", () -> new MachineRecipe.Serializer(MachineRecipe.Process.EXTRACTING));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipe.Serializer> ELECTROREFINING_SERIALIZER =
            SERIALIZERS.register("electrorefining", () -> new MachineRecipe.Serializer(MachineRecipe.Process.ELECTROREFINING));

    private static DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> type(String name) {
        return TYPES.register(name, () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, name)));
    }

    private ModRecipes() {}
}
