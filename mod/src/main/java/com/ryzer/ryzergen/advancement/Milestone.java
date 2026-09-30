package com.ryzer.ryzergen.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Things a player does that advancements can wait for. See {@link MilestoneTrigger}. */
public enum Milestone implements StringRepresentable {
    MICROREACTOR_FORMED("microreactor_formed"),
    SAFETIES_OFF("safeties_off"),
    MELTDOWN("meltdown"),
    BATTERY_FULL("battery_full"),
    STATION_FORMED("station_formed"),
    STATION_OVERDRIVE("station_overdrive"),
    /** A station running a layout rated 100% against the best known. */
    STATION_PERFECT("station_perfect"),
    STATION_MELTDOWN("station_meltdown"),
    POOL_FORMED("pool_formed"),
    CONTAINER_FORMED("container_formed");

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
