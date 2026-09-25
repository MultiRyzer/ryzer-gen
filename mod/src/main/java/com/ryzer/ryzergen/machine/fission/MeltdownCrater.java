package com.ryzer.ryzergen.machine.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The crater a fission station leaves when it melts down: a wide scorched bowl, far bigger than a
 * vanilla explosion can dig (their rays thin out past a radius of about 10). Everything inside the
 * bowl is cleared, including whatever was built over it; the floor is left as blackstone, basalt
 * and magma with fires burning; the ground round the rim is scorched.
 *
 * <p>The blocks go through NeoForge's explosion event first, as a real explosion's would, so land
 * claim mods (FTB Chunks and the like) can keep their blocks out of it. The vanilla blast that
 * follows does the damage and knockback. The radius is a config setting; 0 leaves only the blast.
 */
public final class MeltdownCrater {
    /** The bowl's depth below the station's base, and how high it clears above, per block of radius. */
    private static final double DEPTH = 0.45;
    private static final double HEIGHT = 0.8;

    private MeltdownCrater() {}

    /** Digs the crater round {@code base}, the middle of the station's footprint at ground level. */
    public static void carve(ServerLevel level, BlockPos base, int radius) {
        if (radius <= 0) {
            return;
        }
        RandomSource random = level.random;
        List<BlockPos> blown = new ArrayList<>();
        List<BlockPos> floor = new ArrayList<>();
        for (int dx = -radius - 3; dx <= radius + 3; dx++) {
            for (int dz = -radius - 3; dz <= radius + 3; dz++) {
                // A ragged edge: each column's reach wobbles a little.
                double reach = radius * (0.9 + random.nextDouble() * 0.2);
                double d = Math.sqrt(dx * dx + dz * dz) / reach;
                if (d > 1) {
                    continue;
                }
                double bowl = Math.sqrt(1 - d * d);
                int bottom = base.getY() - (int) Math.round(radius * DEPTH * bowl);
                int top = base.getY() + (int) Math.round(radius * HEIGHT * bowl) + 4;
                for (int y = Math.max(level.getMinBuildHeight(), bottom); y <= Math.min(level.getMaxBuildHeight() - 1, top); y++) {
                    BlockPos pos = base.offset(dx, y - base.getY(), dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0) {
                        blown.add(pos);
                    }
                }
                floor.add(new BlockPos(base.getX() + dx, bottom - 1, base.getZ() + dz));
            }
        }

        // Let other mods see (and trim) the list, as they would for any explosion.
        Explosion explosion = new Explosion(level, null, base.getX() + 0.5, base.getY(), base.getZ() + 0.5, radius, false,
                Explosion.BlockInteraction.DESTROY, blown);
        EventHooks.onExplosionDetonate(level, explosion, new ArrayList<>(), radius * 2);
        Set<BlockPos> cleared = new HashSet<>(explosion.getToBlow());
        for (BlockPos pos : cleared) {
            // Straight to air: no drops, and no neighbour updates for each of thousands of blocks.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }

        // The floor: the molten core's heat glassed and blackened the rock.
        for (BlockPos pos : floor) {
            if (!cleared.contains(pos.above()) || level.getBlockState(pos).isAir()) {
                continue;
            }
            float roll = random.nextFloat();
            level.setBlock(pos, (roll < 0.15F ? Blocks.MAGMA_BLOCK : roll < 0.55F ? Blocks.BLACKSTONE : Blocks.BASALT)
                    .defaultBlockState(), Block.UPDATE_ALL);
            if (random.nextFloat() < 0.12F) {
                level.setBlock(pos.above(), Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // The rim: scorched ground a few blocks beyond the edge.
        for (int dx = -radius - 6; dx <= radius + 6; dx++) {
            for (int dz = -radius - 6; dz <= radius + 6; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < radius * 0.95 || d > radius + 6 || random.nextFloat() > 0.6F) {
                    continue;
                }
                int x = base.getX() + dx;
                int z = base.getZ() + dz;
                BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
                BlockState state = level.getBlockState(ground);
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
                    level.setBlock(ground, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
                    if (random.nextFloat() < 0.15F && level.getBlockState(ground.above()).isAir()) {
                        level.setBlock(ground.above(), Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
}
