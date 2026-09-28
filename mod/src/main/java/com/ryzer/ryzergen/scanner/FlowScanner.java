package com.ryzer.ryzergen.scanner;

import com.ryzer.ryzergen.cable.CableBlockEntity;
import com.ryzer.ryzergen.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The server side of the Flow Scanner: twice a second, each player holding one gets the readings of
 * every pipe and cable input within {@link #RADIUS} blocks. Only chunks already loaded are looked
 * at, and nothing runs while nobody holds a scanner.
 */
public final class FlowScanner {
    public static final int RADIUS = 24;
    public static final int MAX_ENTRIES = 1024;
    private static final int INTERVAL = 10;

    private FlowScanner() {}

    public static boolean holding(net.minecraft.world.entity.player.Player player) {
        return player.getMainHandItem().is(ModItems.FLOW_SCANNER.get()) || player.getOffhandItem().is(ModItems.FLOW_SCANNER.get());
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (holding(player)) {
                PacketDistributor.sendToPlayer(player, new FlowScanPayload(scan(level, player.blockPosition())));
            }
        }
    }

    private static List<FlowScanPayload.Entry> scan(ServerLevel level, BlockPos centre) {
        List<FlowScanPayload.Entry> entries = new ArrayList<>();
        int minX = (centre.getX() - RADIUS) >> 4, maxX = (centre.getX() + RADIUS) >> 4;
        int minZ = (centre.getZ() - RADIUS) >> 4, maxZ = (centre.getZ() + RADIUS) >> 4;
        long radiusSq = (long) RADIUS * RADIUS;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof CableBlockEntity<?> cable) || entity.getBlockPos().distSqr(centre) > radiusSq) {
                        continue;
                    }
                    byte kind = (byte) cable.kind().ordinal();
                    for (CableBlockEntity.FlowReading reading : cable.readings()) {
                        entries.add(new FlowScanPayload.Entry(entity.getBlockPos().asLong(), (byte) reading.side().get3DDataValue(),
                                kind, reading.rate(), reading.limit()));
                        if (entries.size() >= MAX_ENTRIES) {
                            return entries;
                        }
                    }
                }
            }
        }
        return entries;
    }
}
