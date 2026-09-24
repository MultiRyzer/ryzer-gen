package com.ryzer.ryzergen.cable;

import net.minecraft.util.StringRepresentable;

/** What a cable does on one of its six sides. */
public enum CableSide implements StringRepresentable {
    /** Nothing there, or disconnected with the wrench. */
    NONE("none"),
    /** Joined to another cable, or to a block it delivers energy to (and accepts pushed energy from). */
    CONNECTED("connected"),
    /** Pulls energy out of the block on this side. Shown with a flange, like a Pipez extract plate. */
    EXTRACT("extract");

    private final String name;

    CableSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
