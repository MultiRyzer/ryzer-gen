package com.ryzer.ryzergen.client;

import com.ryzer.ryzergen.RyzerGen;
import com.ryzer.ryzergen.cable.CableBlock;
import com.ryzer.ryzergen.cable.CableBlockEntity;
import com.ryzer.ryzergen.cable.CableSide;
import com.ryzer.ryzergen.cable.CableUpgrade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A cable's model with its fittings: the usual core, arms and flanges, plus the fitting on each
 * side that has one (from the block entity's model data). The fittings are small models of their
 * own, one per tier and side (datagen, cable_fitting/), baked into the chunk with the cable, so
 * they cost nothing per frame. Storing fittings in the block state instead would multiply each
 * cable's states by 4096.
 */
public class FittedCableModel extends BakedModelWrapper<BakedModel> {
    public FittedCableModel(BakedModel base) {
        super(base);
    }

    /** The standalone model for one tier's fitting on one side. */
    public static ModelResourceLocation location(CableUpgrade tier, Direction side) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID,
                "block/cable_fitting/" + tier.id() + "_" + side.getName()));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        CableUpgrade[] fittings = data.get(CableBlockEntity.FITTINGS);
        if (fittings == null || state == null) {
            return quads;
        }
        List<BakedQuad> out = null;
        for (Direction dir : Direction.values()) {
            CableUpgrade tier = fittings[dir.get3DDataValue()];
            if (tier == CableUpgrade.NONE || state.getValue(CableBlock.SIDES.get(dir)) == CableSide.NONE) {
                continue;
            }
            if (out == null) {
                out = new ArrayList<>(quads);
            }
            BakedModel fitting = Minecraft.getInstance().getModelManager().getModel(location(tier, dir));
            out.addAll(fitting.getQuads(state, side, rand, ModelData.EMPTY, renderType));
        }
        return out == null ? quads : out;
    }
}
