package com.fruityspikes.whaleborne.server.data;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.components.hullback.PlatformLayout;
import com.fruityspikes.whaleborne.server.entities.components.hullback.SeatLayout;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads per-material hull configurations from datapack JSON.
 * Directory: data/{namespace}/whaleborne/hull_config/{name}.json
 */
public class HullConfigManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<Item, HullConfig> CONFIGS = new HashMap<>();
    private static final Map<Item, SeatLayout> SEAT_LAYOUTS = new HashMap<>();
    private static final Map<Item, PlatformLayout> PLATFORM_LAYOUTS = new HashMap<>();

    public HullConfigManager() {
        super(GSON, "whaleborne/hull_config");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        CONFIGS.clear();
        SEAT_LAYOUTS.clear();
        PLATFORM_LAYOUTS.clear();

        entries.forEach((id, json) -> {
            HullConfig config = HullConfig.CODEC.parse(JsonOps.INSTANCE, json).result()
                    .orElseGet(() -> {
                        Whaleborne.LOGGER.error("Failed to parse hull config {}", id);
                        return null;
                    });

            if (config != null) {
                Item item = BuiltInRegistries.ITEM.get(config.item());
                if (item != null) {
                    CONFIGS.put(item, config);

                    // Parse optional sub-objects from the raw JSON (alongside the codec-driven fields).
                    if (json.isJsonObject()) {
                        JsonObject obj = json.getAsJsonObject();
//                        if (obj.has("seats")) {
//                            SeatLayout layout = parseSeatLayout(obj.getAsJsonArray("seats"), id);
//                            if (layout != null) {
//                                SEAT_LAYOUTS.put(item, layout);
//                            }
//                        }
                        if (obj.has("platforms")) {
                            PlatformLayout layout = parsePlatformLayout(obj.getAsJsonArray("platforms"), id);
                            if (layout != null) {
                                PLATFORM_LAYOUTS.put(item, layout);
                            }
                        }
                    }
                } else {
                    Whaleborne.LOGGER.warn("Hull config {} references unknown item: {}", id, config.item());
                }
            }
        });

        Whaleborne.LOGGER.info("Loaded {} hull configs ({} with custom seats, {} with custom platforms)",
                CONFIGS.size(), SEAT_LAYOUTS.size(), PLATFORM_LAYOUTS.size());
    }

    /** Parse a "platforms" JSON array. Supports legacy single-rectangle fields and a {@code shapes}
     *  array of composite rectangles per part. */
    private static PlatformLayout parsePlatformLayout(JsonArray platformsArray, ResourceLocation configId) {
        try {
            PlatformLayout.PlatformDef[] defs = new PlatformLayout.PlatformDef[PlatformLayout.MAX_PARTS];
            for (int i = 0; i < PlatformLayout.MAX_PARTS; i++) defs[i] = PlatformLayout.PlatformDef.defaults();

            for (int i = 0; i < platformsArray.size(); i++) {
                JsonObject obj = platformsArray.get(i).getAsJsonObject();
                String partName = obj.has("part") ? obj.get("part").getAsString() : "body";
                int partIndex = partNameToIndex(partName);
                if (partIndex < 0 || partIndex >= PlatformLayout.MAX_PARTS) {
                    Whaleborne.LOGGER.warn("Hull config {} platform {}: part '{}' has no platform slot, skipping",
                            configId, i, partName);
                    continue;
                }

                float width = obj.has("width") ? obj.get("width").getAsFloat() : PlatformLayout.DEFAULT_WIDTH;
                float length = obj.has("length") ? obj.get("length").getAsFloat() : PlatformLayout.LENGTH_AXIS_ALIGNED;
                float height = obj.has("height") ? obj.get("height").getAsFloat() : PlatformLayout.DEFAULT_HEIGHT;
                float xOffset = obj.has("x_offset") ? obj.get("x_offset").getAsFloat() : PlatformLayout.DEFAULT_X_OFFSET;
                float yOffset = obj.has("y_offset") ? obj.get("y_offset").getAsFloat() : PlatformLayout.DEFAULT_Y_OFFSET;
                float zOffset = obj.has("z_offset") ? obj.get("z_offset").getAsFloat() : PlatformLayout.DEFAULT_Z_OFFSET;
                float detectRange = obj.has("detection_range") ? obj.get("detection_range").getAsFloat() : PlatformLayout.DEFAULT_DETECTION_RANGE;
                boolean legacyAabb = obj.has("legacy_aabb") && obj.get("legacy_aabb").getAsBoolean();

                List<PlatformLayout.ShapeDef> shapes = null;
                if (obj.has("shapes") && obj.get("shapes").isJsonArray()) {
                    JsonArray sArr = obj.getAsJsonArray("shapes");
                    List<PlatformLayout.ShapeDef> parsed = new ArrayList<>(sArr.size());
                    for (int k = 0; k < sArr.size(); k++) {
                        JsonObject so = sArr.get(k).getAsJsonObject();
                        float sDx = so.has("dx") ? so.get("dx").getAsFloat() : 0f;
                        float sDy = so.has("dy") ? so.get("dy").getAsFloat() : 0f;
                        float sDz = so.has("dz") ? so.get("dz").getAsFloat() : 0f;
                        float sW = so.has("width") ? so.get("width").getAsFloat() : PlatformLayout.DEFAULT_WIDTH;
                        // length sentinel: -1 means "not declared" → single-AABB fallback in spawn.
                        float sL = so.has("length") ? so.get("length").getAsFloat() : PlatformLayout.LENGTH_AXIS_ALIGNED;
                        boolean rot = !so.has("rotate_with_yaw") || so.get("rotate_with_yaw").getAsBoolean();
                        int anchor = so.has("anchor") ? anchorNameToIndex(so.get("anchor").getAsString()) : PlatformLayout.ANCHOR_SELF;
                        float ts = so.has("tile_size") ? so.get("tile_size").getAsFloat() : PlatformLayout.TILE_SIZE_DEFAULT;
                        parsed.add(new PlatformLayout.ShapeDef(sDx, sDy, sDz, sW, sL, rot, anchor, ts));
                    }
                    if (!parsed.isEmpty()) shapes = parsed;
                }

                defs[partIndex] = new PlatformLayout.PlatformDef(width, length, height, xOffset, yOffset, zOffset, shapes, detectRange, legacyAabb);
            }

            return new PlatformLayout(defs);
        } catch (Exception e) {
            Whaleborne.LOGGER.warn("Failed to parse platforms in hull config {}: {}", configId, e.getMessage());
            return null;
        }
    }

    /** Parse a "seats" JSON array into a SeatLayout; fluke seat defaults to the
     *  last seat whose part_pos is "fluke". */
    private static SeatLayout parseSeatLayout(JsonArray seatsArray, ResourceLocation configId) {
        try {
            List<SeatLayout.SeatDef> defs = new ArrayList<>();
            int flukeSeatIndex = -1;

            for (int i = 0; i < seatsArray.size() && i < SeatLayout.MAX_SEATS; i++) {
                JsonObject seatObj = seatsArray.get(i).getAsJsonObject();
                JsonArray offset = seatObj.getAsJsonArray("offset");

                String posPartName = seatObj.has("part_pos") ? seatObj.get("part_pos").getAsString() : "body";
                String rotPartName = seatObj.has("part_rot") ? seatObj.get("part_rot").getAsString() : posPartName;

                int posPartIndex = partNameToIndex(posPartName);
                int rotPartIndex = partNameToIndex(rotPartName);

                if (posPartIndex < 0) {
                    Whaleborne.LOGGER.warn("Hull config {} seat {}: unknown part_pos '{}', skipping", configId, i, posPartName);
                    continue;
                }
                if (rotPartIndex < 0) rotPartIndex = posPartIndex;

                defs.add(new SeatLayout.SeatDef(
                        new Vec3(offset.get(0).getAsDouble(), offset.get(1).getAsDouble(), offset.get(2).getAsDouble()),
                        posPartIndex, rotPartIndex
                ));

                // Track last fluke seat for smoothing
                if (posPartIndex == 4) flukeSeatIndex = defs.size() - 1;
            }

            return defs.isEmpty() ? null : new SeatLayout(defs.toArray(new SeatLayout.SeatDef[0]), flukeSeatIndex);
        } catch (Exception e) {
            Whaleborne.LOGGER.warn("Failed to parse seats in hull config {}: {}", configId, e.getMessage());
            return null;
        }
    }

    private static int partNameToIndex(String name) {
        return switch (name.toLowerCase()) {
            case "nose" -> 0;
            case "head" -> 1;
            case "body" -> 2;
            case "tail" -> 3;
            case "fluke" -> 4;
            default -> -1;
        };
    }

    private static int anchorNameToIndex(String name) {
        return switch (name.toLowerCase()) {
            case "self" -> PlatformLayout.ANCHOR_SELF;
            case "whale" -> PlatformLayout.ANCHOR_WHALE;
            case "nose" -> 0;
            case "head" -> 1;
            case "body" -> 2;
            case "tail" -> 3;
            case "fluke" -> 4;
            default -> PlatformLayout.ANCHOR_SELF;
        };
    }

    /** Hull config for an item; generates and caches a default if no datapack JSON exists. */
    public static HullConfig getConfig(Item item) {
        HullConfig config = CONFIGS.get(item);
        if (config != null) return config;

        // Generate default based on block properties
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        float resistance = HullConfig.DEFAULT_RESISTANCE;
        if (item instanceof BlockItem blockItem) {
            try {
                BlockState state = blockItem.getBlock().defaultBlockState();
                // getDestroySpeed(null, null) is safe for vanilla blocks but modded
                // blocks may override and access the parameters — wrap in try-catch
                float destroySpeed = state.getDestroySpeed(null, null);
                if (destroySpeed > 0) {
                    resistance = destroySpeed;
                }
            } catch (Exception e) {
                // Modded block threw exception — use default resistance
            }
        }

        // Cache the default so we don't recompute every frame
        HullConfig defaultConfig = HullConfig.createDefault(itemId, resistance);
        CONFIGS.put(item, defaultConfig);
        return defaultConfig;
    }

    public static int getMaxPlanks(Item item) {
        return getConfig(item).planksRequired();
    }

    public static float getResistance(Item item) {
        return getConfig(item).resistance();
    }

    public static String getArmorModel(Item item) {
        return getConfig(item).armorModel();
    }

    /** Get effective block chance (direct or resistance/70). */
    public static float getBlockChance(Item item) {
        return getConfig(item).getEffectiveBlockChance();
    }

    /** Get swim speed bonus for an item. */
    public static float getSwimSpeedBonus(Item item) {
        return getConfig(item).swimSpeedBonus();
    }

    /** Get seat layout for an item. Returns default 7-seat layout if none defined. */
    public static SeatLayout getSeatLayout(Item item) {
        SeatLayout layout = SEAT_LAYOUTS.get(item);
        return layout != null ? layout : SeatLayout.defaultLayout();
    }

    /** Get walkable-platform layout for an item. Returns the hardcoded default rig if none defined. */
    public static PlatformLayout getPlatformLayout(Item item) {
        PlatformLayout layout = PLATFORM_LAYOUTS.get(item);
        return layout != null ? layout : PlatformLayout.defaultLayout();
    }
}
