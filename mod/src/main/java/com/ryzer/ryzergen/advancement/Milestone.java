package com.ryzer.ryzergen.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Things a player does that advancements can wait for. See {@link MilestoneTrigger}. */
public enum Milestone implements StringRepresentable {
    MICROREACTOR_FORMED("microreactor_formed"),
    SAFETIES_OFF("safeties_off"),
    MELTDOWN("meltdown"),
    BATTERY_FULL("battery_full");

    public static final Codec<Milestone> CODEC = StringRepresentable.fromEnum(Milestone::values);

    private final String name;

    Milestone(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
