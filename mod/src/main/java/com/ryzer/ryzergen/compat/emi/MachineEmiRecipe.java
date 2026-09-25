package com.ryzer.ryzergen.compat.emi;

import com.ryzer.ryzergen.machine.processing.ProcessingMachine;
import com.ryzer.ryzergen.recipe.MachineRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

/**
 * One fuel cycle machine recipe in EMI, laid out like the JEI page and the machine's screen: the
 * inputs in a row (and the water, for the reprocessor), an arrow, the results in a column.
 */
public class MachineEmiRecipe extends BasicEmiRecipe {
    private static final int OUTPUTS_X = 104;

    private final ProcessingMachine machine;
    private final int time;

    public MachineEmiRecipe(EmiRecipeCategory category, ResourceLocation id, MachineRecipe recipe) {
        super(category, id, OUTPUTS_X + 18, 66);
        machine = ProcessingMachine.forProcess(recipe.process());
        time = recipe.time();
        recipe.inputs().forEach(input -> inputs.add(EmiIngredient.of(input.ingredient(), input.count())));
        recipe.fluid().ifPresent(fluid -> inputs.add(EmiIngredient.of(Arrays.stream(fluid.getFluids())
                .map(stack -> EmiStack.of(stack.getFluid(), stack.getAmount())).toList())));
        recipe.results().forEach(result -> outputs.add(EmiStack.of(result)));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < inputs.size(); i++) {
            widgets.addSlot(inputs.get(i), 18 * i, 18);
        }
        // A tick is 50 ms.
        widgets.addFillingArrow(74, 19, time * 50);
        for (int i = 0; i < outputs.size(); i++) {
            widgets.addSlot(outputs.get(i), OUTPUTS_X, 18 - 9 * (outputs.size() - 1) + 18 * i).recipeContext(this);
        }
        widgets.addText(Component.translatable("jei.ryzergen.machine.time", time / 20F, machine.energyPerTick()),
                0, 56, 0xFF404040, false);
    }
}
