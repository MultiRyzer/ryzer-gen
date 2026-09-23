package com.ryzer.ryzergen.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ryzer.ryzergen.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Two inputs, in either slot order, cooked into one result in the alloy smelter. */
public record AlloyingRecipe(SizedIngredient first, SizedIngredient second, ItemStack result, int cookingTime)
        implements Recipe<AlloyingRecipe.Input> {

    public static final int DEFAULT_COOKING_TIME = 200;

    public static final MapCodec<AlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.FLAT_CODEC.fieldOf("first").forGetter(AlloyingRecipe::first),
            SizedIngredient.FLAT_CODEC.fieldOf("second").forGetter(AlloyingRecipe::second),
            ItemStack.CODEC.fieldOf("result").forGetter(AlloyingRecipe::result),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("cooking_time", DEFAULT_COOKING_TIME).forGetter(AlloyingRecipe::cookingTime)
    ).apply(instance, AlloyingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC, AlloyingRecipe::first,
            SizedIngredient.STREAM_CODEC, AlloyingRecipe::second,
            ItemStack.STREAM_CODEC, AlloyingRecipe::result,
            ByteBufCodecs.VAR_INT, AlloyingRecipe::cookingTime,
            AlloyingRecipe::new);

    /** True when the first slot holds the first ingredient; false when the player put them in the other way round. */
    public boolean matchesInOrder(Input input) {
        return first.test(input.first()) && second.test(input.second());
    }

    @Override
    public boolean matches(Input input, Level level) {
        return matchesInOrder(input) || (first.test(input.second()) && second.test(input.first()));
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, first.ingredient(), second.ingredient());
    }

    // Keeps it out of the vanilla recipe book, which has no category for it.
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ALLOYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ALLOYING_TYPE.get();
    }

    public record Input(ItemStack first, ItemStack second) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? first : second;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public static class Serializer implements RecipeSerializer<AlloyingRecipe> {
        @Override
        public MapCodec<AlloyingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
