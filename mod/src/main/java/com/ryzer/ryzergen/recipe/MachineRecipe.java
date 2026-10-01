package com.ryzer.ryzergen.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * A recipe for one of the fuel cycle machines (design section 7): up to three item inputs in any
 * slot order, an optional fluid, and up to three results and an optional fluid result (into the
 * machine's tank), taking {@code time} ticks. The same shape
 * serves the Core Cracker, Reprocessor and Fuel Fabricator; {@link Process} says which machine runs
 * it, and each process is its own recipe type so packs can change one machine's recipes alone.
 */
public record MachineRecipe(Process process, List<SizedIngredient> inputs, Optional<SizedFluidIngredient> fluid,
                            List<ItemStack> results, Optional<FluidStack> fluidResult, int time) implements Recipe<MachineRecipe.Input> {
    public static final int MAX_INPUTS = 3;
    public static final int MAX_RESULTS = 3;
    public static final int DEFAULT_TIME = 200;

    /** Which machine a recipe belongs to. */
    public enum Process {
        CRACKING, REPROCESSING, FABRICATING, EXTRACTING, ELECTROREFINING;

        public RecipeType<MachineRecipe> type() {
            return switch (this) {
                case CRACKING -> ModRecipes.CRACKING_TYPE.get();
                case REPROCESSING -> ModRecipes.REPROCESSING_TYPE.get();
                case FABRICATING -> ModRecipes.FABRICATING_TYPE.get();
                case EXTRACTING -> ModRecipes.EXTRACTING_TYPE.get();
                case ELECTROREFINING -> ModRecipes.ELECTROREFINING_TYPE.get();
            };
        }

        public RecipeSerializer<MachineRecipe> serializer() {
            return switch (this) {
                case CRACKING -> ModRecipes.CRACKING_SERIALIZER.get();
                case REPROCESSING -> ModRecipes.REPROCESSING_SERIALIZER.get();
                case FABRICATING -> ModRecipes.FABRICATING_SERIALIZER.get();
                case EXTRACTING -> ModRecipes.EXTRACTING_SERIALIZER.get();
                case ELECTROREFINING -> ModRecipes.ELECTROREFINING_SERIALIZER.get();
            };
        }
    }

    private static <T> Codec<List<T>> upTo(Codec<T> codec, int max, String what) {
        return codec.listOf().validate(list -> list.isEmpty() || list.size() > max
                ? DataResult.error(() -> "A machine recipe needs 1 to " + max + " " + what + ", not " + list.size())
                : DataResult.success(list));
    }

    /**
     * Which input slot each ingredient takes from, or null if the recipe does not match. Each
     * ingredient needs its own slot holding enough; the fluid needs enough in the tank.
     */
    public int @Nullable [] slotsFor(Input input) {
        if (fluid.isPresent()) {
            SizedFluidIngredient needed = fluid.get();
            if (!needed.ingredient().test(input.fluid()) || input.fluid().getAmount() < needed.amount()) {
                return null;
            }
        }
        int[] slots = new int[inputs.size()];
        return assign(input, 0, slots, new boolean[input.size()]) ? slots : null;
    }

    /** Tries every way of giving each ingredient a slot (at most three of each, so a quick search). */
    private boolean assign(Input input, int ingredient, int[] slots, boolean[] used) {
        if (ingredient == inputs.size()) {
            return true;
        }
        SizedIngredient wanted = inputs.get(ingredient);
        for (int slot = 0; slot < input.size(); slot++) {
            if (!used[slot] && wanted.test(input.getItem(slot))) {
                used[slot] = true;
                slots[ingredient] = slot;
                if (assign(input, ingredient + 1, slots, used)) {
                    return true;
                }
                used[slot] = false;
            }
        }
        return false;
    }

    /** Whether any ingredient would take this item, so machines only let useful items in. */
    public boolean uses(ItemStack stack) {
        return inputs.stream().anyMatch(ingredient -> ingredient.ingredient().test(stack));
    }

    @Override
    public boolean matches(Input input, Level level) {
        return slotsFor(input) != null;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.getFirst();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        inputs.forEach(input -> list.add(input.ingredient()));
        return list;
    }

    // Keeps it out of the vanilla recipe book, which has no category for it.
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return process.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return process.type();
    }

    /** What a machine holds: its input slots and, for the reprocessor, its water. */
    public record Input(List<ItemStack> items, FluidStack fluid) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<MachineRecipe> {
        private final MapCodec<MachineRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec;

        public Serializer(Process process) {
            MapCodec<MachineRecipe> fields = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    upTo(SizedIngredient.FLAT_CODEC, MAX_INPUTS, "inputs").fieldOf("inputs").forGetter(MachineRecipe::inputs),
                    SizedFluidIngredient.FLAT_CODEC.optionalFieldOf("fluid").forGetter(MachineRecipe::fluid),
                    ItemStack.CODEC.listOf().validate(list -> list.size() > MAX_RESULTS
                            ? DataResult.error(() -> "A machine recipe makes at most " + MAX_RESULTS + " results, not " + list.size())
                            : DataResult.success(list)).optionalFieldOf("results", List.of()).forGetter(MachineRecipe::results),
                    FluidStack.CODEC.optionalFieldOf("fluid_result").forGetter(MachineRecipe::fluidResult),
                    ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(MachineRecipe::time)
            ).apply(instance, (inputs, fluid, results, fluidResult, time) -> new MachineRecipe(process, inputs, fluid, results,
                    fluidResult, time)));
            codec = fields.validate(recipe -> recipe.results().isEmpty() && recipe.fluidResult().isEmpty()
                    ? DataResult.error(() -> "A machine recipe needs a result or a fluid result") : DataResult.success(recipe));
            streamCodec = StreamCodec.composite(
                    SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::inputs,
                    ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC), MachineRecipe::fluid,
                    ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::results,
                    ByteBufCodecs.optional(FluidStack.STREAM_CODEC), MachineRecipe::fluidResult,
                    ByteBufCodecs.VAR_INT, MachineRecipe::time,
                    (inputs, fluid, results, fluidResult, time) -> new MachineRecipe(process, inputs, fluid, results, fluidResult, time));
        }

        @Override
        public MapCodec<MachineRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
