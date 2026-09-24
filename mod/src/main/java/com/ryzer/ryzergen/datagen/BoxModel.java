package com.ryzer.ryzergen.datagen;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A single-block machine model drawn as boxes, for depth in the Mekanism and Oritech spirit: frames,
 * recessed bodies, raised panels. Material faces leave out UVs so tiling textures map by position;
 * decal faces map their texture from the top left at one texel per pixel.
 */
final class BoxModel {
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final List<Box> boxes = new ArrayList<>();

    static final class Box {
        final String material;
        final float[] from;
        final float[] to;
        final Map<Direction, String> decals = new EnumMap<>(Direction.class);
        final Set<Direction> glowing = EnumSet.noneOf(Direction.class);

        Box(String material, float x1, float y1, float z1, float x2, float y2, float z2) {
            this.material = material;
            this.from = new float[] {x1, y1, z1};
            this.to = new float[] {x2, y2, z2};
        }

        Box decal(Direction dir, String texture) {
            decals.put(dir, texture);
            return this;
        }

        /** Full brightness in the dark, for windows and lamps. */
        Box glow(Direction dir) {
            glowing.add(dir);
            return this;
        }
    }

    Box add(String material, float x1, float y1, float z1, float x2, float y2, float z2) {
        Box box = new Box(material, x1, y1, z1, x2, y2, z2);
        boxes.add(box);
        return box;
    }

    /** Writes the boxes into {@code model}, with every named texture resolved by {@code texture}. */
    void build(BlockModelBuilder model, String particle, Function<String, ResourceLocation> texture) {
        build(model, particle, texture, 0);
    }

    /**
     * Writes the slice of a taller design that falls in one block, {@code yOffset} pixels up (0 for
     * the lower block of a two-high machine, 16 for the upper). Boxes are cut at the join; faces on
     * the cut are left out, and decals keep their place across it.
     */
    void build(BlockModelBuilder model, String particle, Function<String, ResourceLocation> texture, float yOffset) {
        model.texture("particle", texture.apply(particle));
        for (Box box : boxes) {
            float y1 = Math.max(box.from[1], yOffset);
            float y2 = Math.min(box.to[1], yOffset + 16);
            if (y2 <= y1) {
                continue;
            }
            float[] from = {box.from[0], y1, box.from[2]};
            float[] to = {box.to[0], y2, box.to[2]};
            model.texture(box.material, texture.apply(box.material));
            box.decals.values().forEach(name -> model.texture(name, texture.apply(name)));
            ModelBuilder<BlockModelBuilder>.ElementBuilder element = model.element()
                    .from(from[0], from[1] - yOffset, from[2]).to(to[0], to[1] - yOffset, to[2]);
            for (Direction dir : Direction.values()) {
                int axis = dir.getAxis().ordinal();
                boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                float edge = positive ? to[axis] : from[axis];
                if (edge != (positive ? box.to[axis] : box.from[axis])) {
                    continue;
                }
                ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder face = element.face(dir);
                String decal = box.decals.get(dir);
                if (decal != null) {
                    float[] uv = decalUv(dir, box, from, to);
                    face.texture("#" + decal).uvs(uv[0], uv[1], uv[2], uv[3]);
                } else {
                    face.texture("#" + box.material);
                }
                if (box.glowing.contains(dir)) {
                    face.emissivity(15, 15);
                }
                float local = axis == 1 ? edge - yOffset : edge;
                if (local == 0 || local == 16) {
                    face.cullface(dir);
                }
                face.end();
            }
            element.end();
        }
    }

    /** UVs for the part of a decal face inside this piece, measured from the box's own top left. */
    private static float[] decalUv(Direction dir, Box box, float[] from, float[] to) {
        float[] bf = box.from;
        float[] bt = box.to;
        float v1 = bt[1] - to[1];
        float v2 = bt[1] - from[1];
        return switch (dir) {
            case NORTH -> new float[] {bt[0] - to[0], v1, bt[0] - from[0], v2};
            case SOUTH -> new float[] {from[0] - bf[0], v1, to[0] - bf[0], v2};
            case WEST -> new float[] {from[2] - bf[2], v1, to[2] - bf[2], v2};
            case EAST -> new float[] {bt[2] - to[2], v1, bt[2] - from[2], v2};
            case UP -> new float[] {from[0] - bf[0], from[2] - bf[2], to[0] - bf[0], to[2] - bf[2]};
            case DOWN -> new float[] {from[0] - bf[0], bt[2] - to[2], to[0] - bf[0], bt[2] - from[2]};
        };
    }
}
