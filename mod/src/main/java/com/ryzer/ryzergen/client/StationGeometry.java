package com.ryzer.ryzergen.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ryzer.ryzergen.RyzerGen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The fission station's geometry, as drawn in art/tools/fission_concept.py and exported to
 * assets/ryzergen/station/fission_station.json: every face of every box, in blocks from the
 * station's north-west bottom corner, split into the static body, the spinning rotor and the rods.
 * Loaded once on first use and dropped when resources reload. The fusion reactor's concept design is
 * exported the same way and loads through here too.
 */
public final class StationGeometry {
    private static final ResourceLocation DATA = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "station/fission_station.json");
    /** The fusion reactor's concept design (art/tools/tokamak_concept.py), drawn by its preview block. */
    public static final ResourceLocation FUSION = ResourceLocation.fromNamespaceAndPath(RyzerGen.MOD_ID, "fusion/fusion_reactor.json");
    private static final Map<String, Direction> FACES = Map.of("n", Direction.NORTH, "s", Direction.SOUTH,
            "w", Direction.WEST, "e", Direction.EAST, "u", Direction.UP, "d", Direction.DOWN);

    /**
     * One face: four corners (x, y, z in blocks) with their atlas UVs, wound to face outward, and
     * the way it faces (a box face's axis, or any direction for a curved design's panel).
     */
    public record Quad(float[] xyz, float[] uv, float[] normal, boolean emissive) {}

    private static final Map<ResourceLocation, Map<String, List<Quad>>> designs = new HashMap<>();
    /** Bumped on every reload, so meshes built from the old atlas know to rebuild. */
    private static int generation;

    private StationGeometry() {}

    public static void clear() {
        designs.clear();
        generation++;
    }

    public static int generation() {
        return generation;
    }

    public static List<Quad> group(String name) {
        return group(DATA, name);
    }

    /** A group from any design exported the same way (the station's, or the fusion reactor's). */
    public static List<Quad> group(ResourceLocation data, String name) {
        return designs.computeIfAbsent(data, StationGeometry::load).getOrDefault(name, List.of());
    }

    private static Map<String, List<Quad>> load(ResourceLocation data) {
        Map<String, List<Quad>> out = new HashMap<>();
        var resource = Minecraft.getInstance().getResourceManager().getResource(data);
        if (resource.isEmpty()) {
            RyzerGen.LOGGER.warn("Missing design geometry {}", data);
            return out;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, TextureAtlasSprite> sprites = new HashMap<>();
            var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            for (Map.Entry<String, JsonElement> texture : root.getAsJsonObject("textures").entrySet()) {
                sprites.put(texture.getKey(), atlas.apply(ResourceLocation.parse(texture.getValue().getAsString())));
            }
            Set<String> emissive = new HashSet<>();
            root.getAsJsonArray("emissive").forEach(e -> emissive.add(e.getAsString()));
            if (root.has("quads")) {
                loadQuads(root.getAsJsonObject("quads"), sprites, emissive, out);
            }
            if (!root.has("groups")) {
                return out;
            }
            for (Map.Entry<String, JsonElement> group : root.getAsJsonObject("groups").entrySet()) {
                List<Quad> quads = new ArrayList<>();
                for (JsonElement element : group.getValue().getAsJsonArray()) {
                    JsonArray box = element.getAsJsonArray();
                    float[] f = {box.get(0).getAsFloat() / 16, box.get(1).getAsFloat() / 16, box.get(2).getAsFloat() / 16};
                    float[] t = {box.get(3).getAsFloat() / 16, box.get(4).getAsFloat() / 16, box.get(5).getAsFloat() / 16};
                    for (Map.Entry<String, JsonElement> face : box.get(6).getAsJsonObject().entrySet()) {
                        JsonArray spec = face.getValue().getAsJsonArray();
                        String texture = spec.get(0).getAsString();
                        TextureAtlasSprite sprite = sprites.get(texture);
                        float[] uv = {spec.get(1).getAsFloat() / 16, spec.get(2).getAsFloat() / 16,
                                spec.get(3).getAsFloat() / 16, spec.get(4).getAsFloat() / 16};
                        quads.add(quad(FACES.get(face.getKey()), f, t, sprite, uv, emissive.contains(texture)));
                    }
                }
                out.put(group.getKey(), quads);
            }
        } catch (Exception e) {
            RyzerGen.LOGGER.error("Could not load design geometry {}", data, e);
        }
        return out;
    }

    /**
     * Free quads (the fusion reactor's curved design): each is a texture, four corners of x, y, z, u,
     * v (design pixels, texture pixels), already wound to face outward, then its normal.
     */
    private static void loadQuads(JsonObject quads, Map<String, TextureAtlasSprite> sprites, Set<String> emissive,
                                  Map<String, List<Quad>> out) {
        for (Map.Entry<String, JsonElement> group : quads.entrySet()) {
            List<Quad> list = new ArrayList<>();
            for (JsonElement element : group.getValue().getAsJsonArray()) {
                JsonArray q = element.getAsJsonArray();
                String texture = q.get(0).getAsString();
                TextureAtlasSprite sprite = sprites.get(texture);
                float[] xyz = new float[12];
                float[] uv = new float[8];
                for (int i = 0; i < 4; i++) {
                    int at = 1 + i * 5;
                    xyz[i * 3] = q.get(at).getAsFloat() / 16;
                    xyz[i * 3 + 1] = q.get(at + 1).getAsFloat() / 16;
                    xyz[i * 3 + 2] = q.get(at + 2).getAsFloat() / 16;
                    uv[i * 2] = sprite.getU(q.get(at + 3).getAsFloat() / 16);
                    uv[i * 2 + 1] = sprite.getV(q.get(at + 4).getAsFloat() / 16);
                }
                float[] normal = {q.get(21).getAsFloat(), q.get(22).getAsFloat(), q.get(23).getAsFloat()};
                list.add(new Quad(xyz, uv, normal, emissive.contains(texture)));
            }
            out.put(group.getKey(), list);
        }
    }

    /**
     * A box drawn in code (the rods, which follow the player's core layout), in design pixels. It is
     * cut every 16 pixels up so its texture keeps its scale on a tall box.
     */
    public static List<Quad> box(float x1, float y1, float z1, float x2, float y2, float z2, ResourceLocation texture, boolean emissive) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        List<Quad> quads = new ArrayList<>();
        float w = x2 - x1;
        float d = z2 - z1;
        for (float y = y1; y < y2; y = (float) Math.floor(y / 16 + 1) * 16) {
            float top = Math.min(y2, (float) Math.floor(y / 16 + 1) * 16);
            float h = top - y;
            float[] f = {x1 / 16, y / 16, z1 / 16};
            float[] t = {x2 / 16, top / 16, z2 / 16};
            for (Direction face : Direction.values()) {
                if ((face == Direction.DOWN && y > y1) || (face == Direction.UP && top < y2)) {
                    continue;
                }
                float[] uv = face.getAxis() == Direction.Axis.Y ? new float[] {0, 0, w / 16, d / 16}
                        : new float[] {0, (16 - h) / 16, (face.getAxis() == Direction.Axis.X ? d : w) / 16, 1};
                quads.add(quad(face, f, t, sprite, uv, emissive));
            }
        }
        return quads;
    }

    /**
     * Corners as Minecraft maps a face's UVs: from the corner at (u1, v1) along increasing u, then v.
     * Listed in reverse so the face winds anticlockwise seen from outside.
     */
    private static Quad quad(Direction face, float[] f, float[] t, TextureAtlasSprite sprite, float[] uv, boolean emissive) {
        float x1 = f[0], y1 = f[1], z1 = f[2], x2 = t[0], y2 = t[1], z2 = t[2];
        float[][] frame = switch (face) {
            case NORTH -> new float[][] {{x2, y2, z1}, {x1 - x2, 0, 0}, {0, y1 - y2, 0}};
            case SOUTH -> new float[][] {{x1, y2, z2}, {x2 - x1, 0, 0}, {0, y1 - y2, 0}};
            case WEST -> new float[][] {{x1, y2, z1}, {0, 0, z2 - z1}, {0, y1 - y2, 0}};
            case EAST -> new float[][] {{x2, y2, z2}, {0, 0, z1 - z2}, {0, y1 - y2, 0}};
            case UP -> new float[][] {{x1, y2, z1}, {x2 - x1, 0, 0}, {0, 0, z2 - z1}};
            case DOWN -> new float[][] {{x1, y1, z2}, {x2 - x1, 0, 0}, {0, 0, z1 - z2}};
        };
        float[] o = frame[0], eu = frame[1], ev = frame[2];
        float u1 = sprite.getU(uv[0]), v1 = sprite.getV(uv[1]), u2 = sprite.getU(uv[2]), v2 = sprite.getV(uv[3]);
        // P0 (u1, v1), P3 = P0 + ev (u1, v2), P2 = P0 + eu + ev (u2, v2), P1 = P0 + eu (u2, v1).
        float[] xyz = {
                o[0], o[1], o[2],
                o[0] + ev[0], o[1] + ev[1], o[2] + ev[2],
                o[0] + eu[0] + ev[0], o[1] + eu[1] + ev[1], o[2] + eu[2] + ev[2],
                o[0] + eu[0], o[1] + eu[1], o[2] + eu[2],
        };
        float[] normal = {face.getStepX(), face.getStepY(), face.getStepZ()};
        return new Quad(xyz, new float[] {u1, v1, u1, v2, u2, v2, u2, v1}, normal, emissive);
    }
}
