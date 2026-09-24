package com.ryzer.ryzergen.datagen;

import com.ryzer.ryzergen.machine.microreactor.MicroreactorSlot;
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
 * The assembled microreactor as one 3D design, split into the four block quarters.
 *
 * <p>The machine is drawn facing north in a 16 x 32 x 32 space: x runs west to east, y is height
 * and z runs from the front (0) to the back (32). Each box is cut at the block joins and every
 * block renders its own pieces, so lighting and culling behave like normal blocks.
 *
 * <p>Material faces leave out UVs, so Minecraft maps the tiling texture by position and it runs on
 * across joins. Decal faces map their texture from the top left at one texel per pixel.
 */
final class MicroreactorModel {
    static final String[] TEXTURES = {
            "steel", "steel_dark", "lead", "copper", "hazard", "glow", "glow_off", "accent",
            "screen", "porthole", "porthole_on", "gauge", "grille", "port_energy", "port_coolant", "port_steam", "port_fuel",
    };

    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private MicroreactorModel() {}

    private static final class Box {
        final String material;
        final float[] from;
        final float[] to;
        final Map<Direction, String> faces = new EnumMap<>(Direction.class);
        final Map<Direction, String> decals = new EnumMap<>(Direction.class);
        final Set<Direction> glowing = EnumSet.noneOf(Direction.class);

        Box(String material, float x1, float y1, float z1, float x2, float y2, float z2) {
            this.material = material;
            this.from = new float[] {x1, y1, z1};
            this.to = new float[] {x2, y2, z2};
        }

        Box sides(String texture) {
            for (Direction dir : SIDES) {
                faces.put(dir, texture);
            }
            return this;
        }

        Box face(Direction dir, String texture) {
            faces.put(dir, texture);
            return this;
        }

        Box decal(Direction dir, String texture) {
            decals.put(dir, texture);
            return this;
        }

        /** Full brightness in the dark, for screens and lights. */
        Box glow(Direction dir) {
            glowing.add(dir);
            return this;
        }

        Box glowAll() {
            glowing.addAll(EnumSet.allOf(Direction.class));
            return this;
        }
    }

    private static List<Box> design(boolean running) {
        List<Box> boxes = new ArrayList<>();
        // Skid: hazard-striped base with a deck plate.
        boxes.add(new Box("steel_dark", 0, 0, 0, 16, 2, 32).sides("hazard"));
        boxes.add(new Box("steel", 1, 2, 1, 15, 3, 31));

        // Corner clips holding the machine to the skid.
        for (float x : new float[] {0, 13}) {
            for (float z : new float[] {0, 29}) {
                boxes.add(new Box("accent", x, 2, z, x + 3, 5, z + 3));
            }
        }

        // Reactor vessel at the front: an octagonal lead column, three overlapping boxes.
        boxes.add(new Box("lead", 3, 3, 3, 13, 26, 15));
        boxes.add(new Box("lead", 2, 3, 5, 14, 26, 13));
        boxes.add(new Box("lead", 5, 3, 2, 11, 26, 16));
        // Clamp rings and the lid with its lifting lug.
        boxes.add(new Box("steel_dark", 1.5F, 9, 1.5F, 14.5F, 11, 15.5F));
        boxes.add(new Box("steel_dark", 1.5F, 20, 1.5F, 14.5F, 22, 15.5F));
        // Light strips set into the clamp rings and under the coolant housing's roof. Lit while running.
        boxes.add(lightStrip(1.4F, 9.75F, 1.4F, 14.6F, 10.25F, 15.6F, running));
        boxes.add(lightStrip(1.4F, 20.75F, 1.4F, 14.6F, 21.25F, 15.6F, running));
        boxes.add(lightStrip(1.9F, 22.25F, 16.4F, 14.1F, 22.75F, 30.1F, running));
        boxes.add(new Box("lead", 4, 26, 4, 12, 27, 14));
        boxes.add(new Box("lead", 5, 27, 5, 11, 28, 13));
        // Pressure gauges on both flanks, between the clamp rings.
        boxes.add(new Box("steel", 1.5F, 12, 6, 2, 19, 13).decal(Direction.WEST, "gauge"));
        boxes.add(new Box("steel", 14, 12, 6, 14.5F, 19, 13).decal(Direction.EAST, "gauge"));
        // Porthole onto the core, and the control console below it.
        // Running, the core glows Cherenkov blue through the porthole and lights up in the dark.
        Box porthole = new Box("steel", 3, 11, 1, 13, 21, 2).decal(Direction.NORTH, running ? "porthole_on" : "porthole");
        boxes.add(running ? porthole.glow(Direction.NORTH) : porthole);
        boxes.add(new Box("steel_dark", 3, 3, 0, 13, 9, 3).decal(Direction.NORTH, "screen").glow(Direction.NORTH));

        // Coolant section at the back: housing, radiator fins and the intake chute on top.
        boxes.add(new Box("steel", 2, 3, 16, 14, 23, 30));
        boxes.add(new Box("steel_dark", 1.5F, 23, 16.5F, 14.5F, 24, 30.5F));
        boxes.add(new Box("steel", 4, 16, 30, 12, 22, 30.5F).decal(Direction.SOUTH, "grille"));
        for (float y : new float[] {8, 11, 14, 17, 20}) {
            boxes.add(new Box("copper", 1, y, 17, 2, y + 1, 29));
            boxes.add(new Box("copper", 14, y, 17, 15, y + 1, 29));
        }
        // Coolant loop pipes from the housing into the vessel, low on each side.
        boxes.add(new Box("copper", 1, 4, 10, 3, 6, 20));
        boxes.add(new Box("copper", 13, 4, 10, 15, 6, 20));

        // Ports (see MicroreactorPort). Each is a 10 x 10 flange flush with the block face around an
        // 8 x 8 socket, centred on the face. That seats the 6 x 6 pipes and cables of Pipez and
        // Mekanism, their 8 x 8 extract plates, and Mekanism's 8 x 8 large transmitters.
        // Energy out: back of the lower back block.
        boxes.add(new Box("steel_dark", 4, 4, 30, 12, 12, 31));
        boxes.add(new Box("steel_dark", 3, 3, 31, 13, 13, 32).decal(Direction.SOUTH, "port_energy"));
        // Coolant in: top of the upper back block, a chute rising out of the housing.
        boxes.add(new Box("steel", 4, 24, 20, 12, 25, 28));
        boxes.add(new Box("copper", 5, 25, 21, 11, 31, 27));
        boxes.add(new Box("steel_dark", 3, 31, 19, 13, 32, 29).decal(Direction.UP, "port_coolant"));
        // Steam out: right-hand side of the lower back block.
        boxes.add(new Box("steel_dark", 14, 4, 20, 15, 12, 28));
        boxes.add(new Box("steel_dark", 15, 3, 19, 16, 13, 29).decal(Direction.EAST, "port_steam"));
        // Fuel hatch: the top of the upper front block, built like the coolant intake behind it. A
        // column rises straight off the lid, square with its top tier, with an orange band (items),
        // and the port sits on top, centred on the block face so pipes seat in it.
        boxes.add(new Box("steel", 5, 28, 5, 11, 31, 11));
        boxes.add(new Box("accent", 4.75F, 29.25F, 4.75F, 11.25F, 29.75F, 11.25F));
        boxes.add(new Box("steel_dark", 3, 31, 3, 13, 32, 13).decal(Direction.UP, "port_fuel"));
        return boxes;
    }

    private static Box lightStrip(float x1, float y1, float z1, float x2, float y2, float z2, boolean lit) {
        Box strip = new Box(lit ? "glow" : "glow_off", x1, y1, z1, x2, y2, z2);
        return lit ? strip.glowAll() : strip;
    }

    /** Fills {@code model} with this slot's quarter of the design. */
    static void build(BlockModelBuilder model, MicroreactorSlot slot, boolean running, Function<String, ResourceLocation> texture) {
        model.ao(false);
        model.texture("particle", texture.apply("steel"));
        for (String name : TEXTURES) {
            model.texture(name, texture.apply(name));
        }
        boolean back = slot == MicroreactorSlot.LOWER_BACK || slot == MicroreactorSlot.UPPER_BACK;
        boolean upper = slot == MicroreactorSlot.UPPER_FRONT || slot == MicroreactorSlot.UPPER_BACK;
        float[] offset = {0, upper ? 16 : 0, back ? 16 : 0};

        for (Box box : design(running)) {
            float[] from = new float[3];
            float[] to = new float[3];
            boolean inside = true;
            for (int i = 0; i < 3; i++) {
                from[i] = Math.max(box.from[i], offset[i]);
                to[i] = Math.min(box.to[i], offset[i] + 16);
                inside &= to[i] > from[i];
            }
            if (!inside) {
                continue;
            }
            ModelBuilder<BlockModelBuilder>.ElementBuilder element = model.element()
                    .from(from[0] - offset[0], from[1] - offset[1], from[2] - offset[2])
                    .to(to[0] - offset[0], to[1] - offset[1], to[2] - offset[2]);
            for (Direction dir : Direction.values()) {
                int axis = dir.getAxis().ordinal();
                boolean positive = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                float edge = positive ? to[axis] : from[axis];
                // A face on a cut is inside the box and never seen.
                if (edge != (positive ? box.to[axis] : box.from[axis])) {
                    continue;
                }
                ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder face = element.face(dir);
                String decal = box.decals.get(dir);
                if (decal != null) {
                    face.texture("#" + decal);
                    float[] uv = decalUv(dir, box, from, to);
                    face.uvs(uv[0], uv[1], uv[2], uv[3]);
                } else {
                    face.texture("#" + box.faces.getOrDefault(dir, box.material));
                }
                if (box.glowing.contains(dir)) {
                    face.emissivity(15, 15);
                }
                float local = edge - offset[axis];
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
