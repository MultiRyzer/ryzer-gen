package com.ryzer.ryzergen.radiation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The client's copy of its own dose and dose rate, for the dosimeter HUD and clicks. Kept free of
 * client-only classes, because the packet handler is registered on dedicated servers too.
 */
public final class RadiationClientState {
    private static float dose;
    private static float rate;

    private RadiationClientState() {}

    public static void receive(RadiationPayload payload, IPayloadContext context) {
        dose = payload.dose();
        rate = payload.rate();
    }

    public static float dose() {
        return dose;
    }

    public static float rate() {
        return rate;
    }
}
