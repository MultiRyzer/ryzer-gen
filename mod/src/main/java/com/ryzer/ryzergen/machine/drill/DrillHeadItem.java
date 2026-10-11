package com.ryzer.ryzergen.machine.drill;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A mega drill head (design section 11c): the beam's source, a gyrotron and its emitter, fitted in
 * the controller's head slot. Each mark is crafted from the one before (upgrade, don't replace,
 * rule 11) and adds a full speed; power grows with the square of the speed, so a Mk I runs on a
 * microreactor and a Mk III wants fission power. Real basis: millimetre-wave drilling, where a
 * gyrotron beams energy down the hole to melt and spall the rock. Its tooltip is in the lang file.
 */
public class DrillHeadItem extends Item {
    private final int mark;

    public DrillHeadItem(int mark, Properties properties) {
        super(properties.stacksTo(1));
        this.mark = mark;
    }

    /** Mk I, II or III. */
    public int mark() {
        return mark;
    }

    /** The speed the head drills at on its own; speed modules add to it. */
    public int speed() {
        return mark;
    }

    /** The speed of the head in a stack, or 0 if it is not one. */
    public static int speed(ItemStack stack) {
        return stack.getItem() instanceof DrillHeadItem head ? head.speed() : 0;
    }
}
