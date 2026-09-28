package com.ryzer.ryzergen.sky;

import com.ryzer.ryzergen.RyzerGen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the swarm's phase (SunSwarm.Phase's ordinal) and the game time it began. */
public record SunSwarmPayload(byte phase, long start) implements CustomPacketPayload {
    public static final Type<SunSwarmPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "sun_swarm"));
    public static final StreamCodec<ByteBuf, SunSwarmPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, SunSwarmPayload::phase,
            ByteBufCodecs.VAR_LONG, SunSwarmPayload::start,
            SunSwarmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
