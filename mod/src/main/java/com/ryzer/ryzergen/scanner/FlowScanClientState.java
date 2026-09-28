package com.ryzer.ryzergen.scanner;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * The client's latest Flow Scanner readings. Kept free of client-only classes, because the packet
 * handler is registered on dedicated servers too.
 */
public final class FlowScanClientState {
    private static List<FlowScanPayload.Entry> entries = List.of();
    private static long receivedAt;

    private FlowScanClientState() {}

    public static void receive(FlowScanPayload payload, IPayloadContext context) {
        entries = payload.entries();
        receivedAt = System.currentTimeMillis();
    }

    /** The readings, or none if the server has not sent any for a while (the scanner was put away). */
    public static List<FlowScanPayload.Entry> entries() {
        return System.currentTimeMillis() - receivedAt < 2_000 ? entries : List.of();
    }
}
