package com.ryzer.ryzergen.machine.drill;

import com.ryzer.ryzergen.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the client's GPU mesh of the preview (see MegaDrillPreviewRenderer). No data, no ticking. */
public class MegaDrillPreviewBlockEntity extends BlockEntity {
    /** The client's GPU mesh of the design's body (a StationMesh), closed when the block goes. */
    public Object clientMesh;

    public MegaDrillPreviewBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEGA_DRILL_PREVIEW.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (clientMesh instanceof AutoCloseable mesh) {
            try {
                mesh.close();
            } catch (Exception ignored) {
                // Only frees GPU memory; nothing to recover.
            }
            clientMesh = null;
        }
    }
}
