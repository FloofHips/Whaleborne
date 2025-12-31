package com.fruityspikes.whaleborne.server.data;

import com.fruityspikes.whaleborne.Whaleborne;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HullbackDirtManager  extends SimpleJsonResourceReloadListener {
    public static final Gson GSON_INSTANCE = new GsonBuilder().create();
    public static final List<HullbackDirtEntry> DATA = new ArrayList<>();

    public HullbackDirtManager() {
        super(GSON_INSTANCE, "hullback_dirt");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        DATA.clear();

        object.forEach((id, jsonElement) -> {
            HullbackDirtEntry entry = HullbackDirtEntry.CODEC.parse(JsonOps.INSTANCE, jsonElement).result()
                    .orElseGet(() -> {
                        Whaleborne.LOGGER.error("Failed to load Hullback dirt entry {}", id);
                        return null;
                    });
            if (entry != null) DATA.add(entry);
        });

        Whaleborne.LOGGER.info("Loaded {} Hullback dirt entries", DATA.size());
    }

    public List<HullbackDirtEntry> get() {
        return DATA;
    }

    public record HullbackDirtEntry(Block block, List<String> placements, float placementChance, List<Block> growth, List<String> removableWith, Item drop, @Nullable Map<String,Integer> blockProperties, @Nullable SoundEvent soundOnGrowth, @Nullable SoundEvent soundOnRemove) {
        public static final Codec<HullbackDirtEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(HullbackDirtEntry::block),
                        Codec.list(Codec.STRING).fieldOf("placements").forGetter(HullbackDirtEntry::placements),
                        Codec.FLOAT.fieldOf("placement_chance").forGetter(HullbackDirtEntry::placementChance),
                        Codec.list(BuiltInRegistries.BLOCK.byNameCodec()).fieldOf("growth").forGetter(HullbackDirtEntry::growth),
                        Codec.list(Codec.STRING).fieldOf("removable_with").forGetter(HullbackDirtEntry::removableWith),
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("drop").forGetter(HullbackDirtEntry::drop),
                        Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("block_properties").forGetter(entry -> Optional.ofNullable(entry.blockProperties)),
                        BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound_on_growth").forGetter(entry -> Optional.ofNullable(entry.soundOnGrowth)),
                        BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound_on_remove").forGetter(entry -> Optional.ofNullable(entry.soundOnRemove))
                ).apply(instance, (block, placements, placementChance, growth, removableWith, drop, props, soundOnGrowth, soundOnRemove) ->
                        new HullbackDirtEntry(block, placements, placementChance, growth, removableWith, drop, props.orElse(null), soundOnGrowth.orElse(null), soundOnRemove.orElse(null))
                )
        );
    }

}

