package com.ryzer.ryzergen.machine;

/** How a machine reacts to redstone. Shared by every machine with a redstone button. */
public enum RedstoneMode {
    IGNORED,
    /** Runs only while powered. */
    HIGH,
    /** Runs only while unpowered. */
    LOW;

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case HIGH -> powered;
            case LOW -> !powered;
        };
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static RedstoneMode byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IGNORED;
    }
}
