package com.ryzer.ryzergen.scanner;

import com.ryzer.ryzergen.RyzerGen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Twice a second, server to client, while the player holds a Flow Scanner: every pipe and cable
 * input nearby, with its recent rate and its limit (see CableBlockEntity#readings).
 */
public record FlowScanPayload(List<Entry> entries) implements CustomPacketPayload {
    /**
     * One input: the cable's position (as a long), the side it comes in on, which kind of cable
     * (CableKind's ordinal, for the unit), the recent rate and the limit.
     */
    public record Entry(long pos, byte side, byte kind, float rate, int limit) {}

    public static final Type<FlowScanPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "flow_scan"));
    public static final StreamCodec<ByteBuf, FlowScanPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.entries.size());
                for (Entry entry : payload.entries) {
                    buf.writeLong(entry.pos);
                    buf.writeByte(entry.side);
                    buf.writeByte(entry.kind);
                    buf.writeFloat(entry.rate);
                    buf.writeInt(entry.limit);
                }
            },
            buf -> {
                int count = Math.min(buf.readInt(), FlowScanner.MAX_ENTRIES);
                List<Entry> entries = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    entries.add(new Entry(buf.readLong(), buf.readByte(), buf.readByte(), buf.readFloat(), buf.readInt()));
                }
                return new FlowScanPayload(entries);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
