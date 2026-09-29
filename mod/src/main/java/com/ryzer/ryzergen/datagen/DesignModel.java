package com.ryzer.ryzergen.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A formed multiblock designed as one list of boxes by an art tool (pool_concept.py,
 * container_concept.py and microreactor_concept.py in art/tools write art/designs/*.json), cut
 * into one model per block and state. The design faces north in pixels: x west to east, y up,
 * z front to back.
 *
 * <p>Material faces leave out UVs, so their tiling textures map by position and run on across
 * joins. Decal faces map their texture from the box's top left at one texel per pixel; each decal
 * sits top left on a square canvas (16, or the next multiple of 16 for a long one) and the export
 * records the canvas, so the UVs are scaled to it.
 */
final class DesignModel {
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /** A texture; {@code tint} is the tint index its faces take (vanilla water's biome colour), or -1. */
    record Texture(ResourceLocation path, int canvas, boolean translucent, int tint) {}

    private record Box(String material, float[] from, float[] to, Map<Direction, String> faces,
                       Map<Direction, String> decals, Set<Direction> glowing) {}

    private final int[] size;
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, List<Box>> states = new HashMap<>();

    private DesignModel(JsonObject json) {
        JsonArray blocks = json.getAsJsonArray("size");
        size = new int[] {blocks.get(0).getAsInt(), blocks.get(1).getAsInt(), blocks.get(2).getAsInt()};
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
            JsonObject texture = entry.getValue().getAsJsonObject();
            textures.put(entry.getKey(), new Texture(ResourceLocation.parse(texture.get("path").getAsString()),
                    texture.get("canvas").getAsInt(), texture.has("translucent") && texture.get("translucent").getAsBoolean(),
                    texture.has("tint") ? texture.get("tint").getAsInt() : -1));
        }
        for (Map.Entry<String, JsonElement> state : json.getAsJsonObject("states").entrySet()) {
            List<Box> boxes = new ArrayList<>();
            for (JsonElement element : state.getValue().getAsJsonArray()) {
                boxes.add(box(element.getAsJsonObject()));
            }
            states.put(state.getKey(), boxes);
        }
    }

    /** Reads a design from the folder the data run is given (build.gradle, ryzergen.designs). */
    static DesignModel load(String name) {
        String folder = System.getProperty("ryzergen.designs");
        if (folder == null) {
            throw new IllegalStateException("No ryzergen.designs folder set for the data run (see build.gradle)");
        }
        try (Reader reader = Files.newBufferedReader(Path.of(folder, name + ".json"))) {
            return new DesignModel(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read design " + name + "; run its art tool first", e);
        }
    }

    private static Box box(JsonObject json) {
        Map<Direction, String> faces = new EnumMap<>(Direction.class);
        Map<Direction, String> decals = new EnumMap<>(Direction.class);
        Set<Direction> glowing = EnumSet.noneOf(Direction.class);
        if (json.has("faces")) {
            json.getAsJsonObject("faces").entrySet().forEach(e -> faces.put(Direction.byName(e.getKey()), e.getValue().getAsString()));
        }
        if (json.has("decals")) {
            json.getAsJsonObject("decals").entrySet().forEach(e -> decals.put(Direction.byName(e.getKey()), e.getValue().getAsString()));
        }
        if (json.has("glow")) {
            json.getAsJsonArray("glow").forEach(e -> glowing.add(Direction.byName(e.getAsString())));
        }
        return new Box(json.get("material").getAsString(), floats(json.getAsJsonArray("from")), floats(json.getAsJsonArray("to")),
                faces, decals, glowing);
    }

    private static float[] floats(JsonArray array) {
        return new float[] {array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    /** Blocks along x, y and z. */
    int[] size() {
        return size;
    }

    Set<String> stateNames() {
        return states.keySet();
    }

    ResourceLocation texture(String name) {
        return textures.get(name).path();
    }

    /** Whether a box shows a see-through texture (water, glass) anywhere. */
    private boolean translucent(Box box) {
        if (textures.get(box.material).translucent()) {
            return true;
        }
        for (String name : box.faces.values()) {
            if (textures.get(name).translucent()) {
                return true;
            }
        }
        for (String name : box.decals.values()) {
            if (textures.get(name).translucent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fills {@code model} with the block at (bx, by, bz) of a state of the design, as a composite
     * of two parts (from {@code nested}): the solid structure, drawn cutout so the block outline
     * still shows over it, and the see-through water and glass, drawn translucent. Faces lit in the
     * design glow.
     */
    void build(BlockModelBuilder model, String state, int bx, int by, int bz, String particle,
               java.util.function.Supplier<BlockModelBuilder> nested) {
        model.texture("particle", texture(particle));
        BlockModelBuilder solid = nested.get();
        BlockModelBuilder clear = nested.get();
        solid.ao(false).renderType("cutout");
        clear.ao(false).renderType("translucent");
        boolean anyClear = false;
        float[] offset = {bx * 16, by * 16, bz * 16};
        int[] at = {bx, by, bz};
        // An outermost block keeps what stands proud of the design's edge (a console, a light
        // strip), up to a block beyond it, which a model may reach.
        float[] low = new float[3];
        float[] high = new float[3];
        for (int i = 0; i < 3; i++) {
            low[i] = at[i] == 0 ? offset[i] - 16 : offset[i];
            high[i] = at[i] == size[i] - 1 ? offset[i] + 32 : offset[i] + 16;
        }
        for (Box box : states.get(state)) {
            boolean seeThrough = translucent(box);
            BlockModelBuilder part = seeThrough ? clear : solid;
            Function<String, String> use = name -> {
                part.texture(name, texture(name));
                return "#" + name;
            };
            float[] from = new float[3];
            float[] to = new float[3];
            boolean inside = true;
            for (int i = 0; i < 3; i++) {
                from[i] = Math.max(box.from[i], low[i]);
                to[i] = Math.min(box.to[i], high[i]);
                inside &= to[i] > from[i];
            }
            if (!inside || !hasFace(box, from, to)) {
                continue;
            }
            anyClear |= seeThrough;
            ModelBuilder<BlockModelBuilder>.ElementBuilder element = part.element()
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
                    face.texture(use.apply(decal));
                    float scale = 16F / textures.get(decal).canvas();
                    float[] uv = decalUv(dir, box, from, to);
                    face.uvs(uv[0] * scale, uv[1] * scale, uv[2] * scale, uv[3] * scale);
                } else {
                    String name = box.faces.getOrDefault(dir, box.material);
                    face.texture(use.apply(name));
                    if (textures.get(name).tint() >= 0) {
                        face.tintindex(textures.get(name).tint());
                    }
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
        CompositeModelBuilder<BlockModelBuilder> composite = model.customLoader(CompositeModelBuilder::begin).child("solid", solid);
        if (anyClear) {
            composite.child("translucent", clear);
        }
        composite.end();
    }

    /**
     * Fills {@code model} with a whole state of the design, uncut, moved so the design's point
     * (ox, oy, oz) is the model's origin: a moving part (the pool's crane), drawn by a renderer that
     * puts it in place. Drawn cutout; its faces must stay within a block either side of the origin.
     */
    void buildPart(BlockModelBuilder model, String state, float ox, float oy, float oz, String particle) {
        model.ao(false);
        model.renderType("cutout");
        model.texture("particle", texture(particle));
        Function<String, String> use = name -> {
            model.texture(name, texture(name));
            return "#" + name;
        };
        for (Box box : states.get(state)) {
            ModelBuilder<BlockModelBuilder>.ElementBuilder element = model.element()
                    .from(box.from[0] - ox, box.from[1] - oy, box.from[2] - oz)
                    .to(box.to[0] - ox, box.to[1] - oy, box.to[2] - oz);
            for (Direction dir : Direction.values()) {
                ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder face = element.face(dir);
                String decal = box.decals.get(dir);
                if (decal != null) {
                    face.texture(use.apply(decal));
                    float scale = 16F / textures.get(decal).canvas();
                    float[] uv = decalUv(dir, box, box.from, box.to);
                    face.uvs(uv[0] * scale, uv[1] * scale, uv[2] * scale, uv[3] * scale);
                } else {
                    face.texture(use.apply(box.faces.getOrDefault(dir, box.material)));
                }
                if (box.glowing.contains(dir)) {
                    face.emissivity(15, 15);
                }
                face.end();
            }
            element.end();
        }
    }

    /** Whether a piece keeps any of its box's faces: one cut on every side (the middle of the water) keeps none. */
    private static boolean hasFace(Box box, float[] from, float[] to) {
        for (int i = 0; i < 3; i++) {
            if (from[i] == box.from[i] || to[i] == box.to[i]) {
                return true;
            }
        }
        return false;
    }

    /** UVs for the part of a decal face inside this piece, in pixels from the box's own top left. */
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
