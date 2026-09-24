package com.ryzer.ryzergen.radiation;

import com.ryzer.ryzergen.RyzerGen;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Once a second, server to client: the player's dose (mSv) and current dose rate (mSv/s), for the dosimeter. */
public record RadiationPayload(float dose, float rate) implements CustomPacketPayload {
    public static final Type<RadiationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "radiation"));
    public static final StreamCodec<ByteBuf, RadiationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RadiationPayload::dose,
            ByteBufCodecs.FLOAT, RadiationPayload::rate,
            RadiationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
