package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

public class ArmorModelLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("Whaleborne-ArmorLoader");

    // Base path for armor geometry — outside models/ to avoid vanilla ModelManager scanning
    // Custom path that won't be scanned by vanilla ModelManager (models/) or GeckoLib (geo/)
    private static final String GEO_BASE = "whaleborne_armor/";

    // Loads a single armor piece from JSON, with material-specific override support
    // Lookup order:
    //   1. geo/armor/{material}/{piece}.json  (per-material override)
    //   2. geo/armor/default/{piece}.json      (default geometry)
    //   3. returns null (caller falls back to hardcoded)
    public static LayerDefinition loadPiece(String piece, String material) {
        // Try material-specific first
        if (material != null) {
            LayerDefinition result = tryLoadJson(
                GEO_BASE + material + "/" + piece + ".json", piece);
            if (result != null) return result;
        }
        // Try default
        return tryLoadJson(GEO_BASE + "default/" + piece + ".json", piece);
    }

    private static LayerDefinition tryLoadJson(String path, String pieceName) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, path);
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (res.isEmpty()) return null;

            try (InputStream in = res.get().open()) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
                return parseModel(root, pieceName);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load armor piece JSON {}: {}", path, e.getMessage());
            return null;
        }
    }

    private static LayerDefinition parseModel(JsonObject root, String pieceName) {
        JsonArray texSize = root.getAsJsonArray("texture_size");
        int texWidth = texSize.get(0).getAsInt();
        int texHeight = texSize.get(1).getAsInt();

        MeshDefinition meshDef = new MeshDefinition();
        PartDefinition partDef = meshDef.getRoot();

        CubeListBuilder cubeList = CubeListBuilder.create();
        JsonArray elements = root.getAsJsonArray("elements");

        for (JsonElement elem : elements) {
            JsonObject el = elem.getAsJsonObject();
            JsonArray from = el.getAsJsonArray("from");
            JsonArray to = el.getAsJsonArray("to");
            JsonArray uv = el.getAsJsonArray("uv");
            float inflate = el.has("inflate") ? el.get("inflate").getAsFloat() : 0.0f;

            float fromX = from.get(0).getAsFloat();
            float fromY = from.get(1).getAsFloat();
            float fromZ = from.get(2).getAsFloat();
            float toX = to.get(0).getAsFloat();
            float toY = to.get(1).getAsFloat();
            float toZ = to.get(2).getAsFloat();
            int uvX = uv.get(0).getAsInt();
            int uvY = uv.get(1).getAsInt();

            cubeList.texOffs(uvX, uvY).addBox(
                fromX, fromY, fromZ,
                toX - fromX, toY - fromY, toZ - fromZ,
                new CubeDeformation(inflate)
            );
        }

        partDef.addOrReplaceChild(pieceName, cubeList, PartPose.offset(0.0F, 24.0F, 0.0F));

        // Add empty parts for the other pieces so ModelPart.getChild() doesn't crash
        String[] allPieces = {"head", "body", "fluke"};
        for (String p : allPieces) {
            if (!p.equals(pieceName)) {
                partDef.addOrReplaceChild(p, CubeListBuilder.create(), PartPose.ZERO);
            }
        }

        return LayerDefinition.create(meshDef, texWidth, texHeight);
    }

    // Load all 3 armor pieces from JSON and merge into one LayerDefinition
    public static LayerDefinition loadMergedArmor(String material) {
        MeshDefinition meshDef = new MeshDefinition();
        PartDefinition root = meshDef.getRoot();
        int texWidth = 1024;
        int texHeight = 512;
        boolean anyLoaded = false;

        for (String piece : new String[]{"head", "body", "fluke"}) {
            JsonObject json = loadJsonRaw(piece, material);
            if (json != null) {
                anyLoaded = true;
                // Read texture size from first successful load
                if (json.has("texture_size")) {
                    JsonArray ts = json.getAsJsonArray("texture_size");
                    texWidth = ts.get(0).getAsInt();
                    texHeight = ts.get(1).getAsInt();
                }
                parsePieceInto(root, json, piece);
            } else {
                // Add empty part as placeholder
                root.addOrReplaceChild(piece, CubeListBuilder.create(), PartPose.ZERO);
            }
        }

        if (!anyLoaded) return null;
        return LayerDefinition.create(meshDef, texWidth, texHeight);
    }

    private static JsonObject loadJsonRaw(String piece, String material) {
        // Try material-specific
        if (material != null) {
            JsonObject result = tryLoadJsonRaw(GEO_BASE + material + "/" + piece + ".json");
            if (result != null) return result;
        }
        // Try default
        return tryLoadJsonRaw(GEO_BASE + "default/" + piece + ".json");
    }

    private static JsonObject tryLoadJsonRaw(String path) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, path);
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().open()) {
                return JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void parsePieceInto(PartDefinition root, JsonObject json, String pieceName) {
        CubeListBuilder cubeList = CubeListBuilder.create();
        JsonArray elements = json.getAsJsonArray("elements");

        for (JsonElement elem : elements) {
            JsonObject el = elem.getAsJsonObject();
            JsonArray from = el.getAsJsonArray("from");
            JsonArray to = el.getAsJsonArray("to");
            JsonArray uv = el.getAsJsonArray("uv");
            float inflate = el.has("inflate") ? el.get("inflate").getAsFloat() : 0.0f;

            cubeList.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt()).addBox(
                from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                to.get(0).getAsFloat() - from.get(0).getAsFloat(),
                to.get(1).getAsFloat() - from.get(1).getAsFloat(),
                to.get(2).getAsFloat() - from.get(2).getAsFloat(),
                new CubeDeformation(inflate)
            );
        }

        root.addOrReplaceChild(pieceName, cubeList, PartPose.offset(0.0F, 24.0F, 0.0F));
    }
}
