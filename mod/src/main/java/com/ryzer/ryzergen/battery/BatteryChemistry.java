package com.ryzer.ryzergen.battery;

import com.ryzer.ryzergen.Config;
import net.minecraft.util.StringRepresentable;

/**
 * What fills a battery module bay (design section 12). Each module adds both capacity and
 * charge/discharge rate; the chemistry decides how much. New chemistries (LFP once lithium exists)
 * are added here and slot into the same cabinet: upgrade, don't replace (design rule 11).
 */
public enum BatteryChemistry implements StringRepresentable {
    EMPTY("empty"),
    /** Lead-acid: heavy and modest, but made from tier 1 lead. Real off-grid homes used these. */
    LEAD_ACID("lead_acid");

    private final String name;

    BatteryChemistry(String name) {
        this.name = name;
    }

    /** FE one module of this chemistry holds. */
    public int capacity() {
        return switch (this) {
            case EMPTY -> 0;
            case LEAD_ACID -> Config.get(Config.LEAD_ACID_CAPACITY);
        };
    }

    /** FE per tick one module of this chemistry can take in or give out. */
    public int rate() {
        return switch (this) {
            case EMPTY -> 0;
            case LEAD_ACID -> Config.get(Config.LEAD_ACID_RATE);
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
