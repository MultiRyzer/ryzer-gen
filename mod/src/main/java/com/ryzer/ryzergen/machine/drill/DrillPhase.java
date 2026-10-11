package com.ryzer.ryzergen.machine.drill;

/**
 * Where a mega drill is in its cycle (design section 11c): idle, charging (the light runs down the
 * drive shaft and the lens swells), firing (the beam heats the layer until it bursts), or done
 * (nothing left above bedrock). The order is saved, so new phases go on the end.
 */
public enum DrillPhase {
    IDLE, CHARGING, FIRING, DONE;

    public static DrillPhase byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IDLE;
    }
}
