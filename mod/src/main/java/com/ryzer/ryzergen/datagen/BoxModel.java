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
        model.texture("particle", texture.apply(particle));
        for (Box box : boxes) {
            model.texture(box.material, texture.apply(box.material));
            box.decals.values().forEach(name -> model.texture(name, texture.apply(name)));
            ModelBuilder<BlockModelBuilder>.ElementBuilder element = model.element()
                    .from(box.from[0], box.from[1], box.from[2]).to(box.to[0], box.to[1], box.to[2]);
            for (Direction dir : Direction.values()) {
                ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder face = element.face(dir);
                String decal = box.decals.get(dir);
                if (decal != null) {
                    float[] uv = decalUv(dir, box);
                    face.texture("#" + decal).uvs(uv[0], uv[1], uv[2], uv[3]);
                } else {
                    face.texture("#" + box.material);
                }
                if (box.glowing.contains(dir)) {
                    face.emissivity(15, 15);
                }
                int axis = dir.getAxis().ordinal();
                float edge = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.to[axis] : box.from[axis];
                if (edge == 0 || edge == 16) {
                    face.cullface(dir);
                }
                face.end();
            }
            element.end();
        }
    }

    private static float[] decalUv(Direction dir, Box box) {
        float[] f = box.from;
        float[] t = box.to;
        float h = t[1] - f[1];
        return switch (dir) {
            case NORTH, SOUTH -> new float[] {0, 0, t[0] - f[0], h};
            case WEST, EAST -> new float[] {0, 0, t[2] - f[2], h};
            case UP, DOWN -> new float[] {0, 0, t[0] - f[0], t[2] - f[2]};
        };
    }
}
