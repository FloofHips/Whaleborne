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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

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

    public record HullbackDirtEntry(Block block, List<String> placements, float placementChance, Optional<Block> growth, @Nullable Map<String,String> growthProperties, List<String> removableWith, Item drop, int dropAmount, @Nullable Map<String,String> blockProperties, @Nullable SoundEvent soundOnGrowth, @Nullable SoundEvent soundOnRemove) {
        public static final Codec<HullbackDirtEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(HullbackDirtEntry::block),
                        Codec.list(Codec.STRING).fieldOf("placements").forGetter(HullbackDirtEntry::placements),
                        Codec.FLOAT.fieldOf("placement_chance").forGetter(HullbackDirtEntry::placementChance),
                        BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("growth").forGetter(HullbackDirtEntry::growth),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("growth_properties").forGetter(entry -> Optional.ofNullable(entry.growthProperties)),
                        Codec.list(Codec.STRING).fieldOf("removable_with").forGetter(HullbackDirtEntry::removableWith),
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("drop").orElse(Items.AIR).forGetter(HullbackDirtEntry::drop),
                        Codec.INT.fieldOf("drop_amount").orElse(1).forGetter(HullbackDirtEntry::dropAmount),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("block_properties").forGetter(entry -> Optional.ofNullable(entry.blockProperties)),
                        BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound_on_growth").forGetter(entry -> Optional.ofNullable(entry.soundOnGrowth)),
                        BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound_on_remove").forGetter(entry -> Optional.ofNullable(entry.soundOnRemove))
                ).apply(instance, (block, placements, placementChance, growth, growthProperties, removableWith, drop, dropAmount, props, soundOnGrowth, soundOnRemove) ->
                        new HullbackDirtEntry(block, placements, placementChance, growth, growthProperties.orElse(null), removableWith, drop, dropAmount, props.orElse(null), soundOnGrowth.orElse(null), soundOnRemove.orElse(null))
                )
        );



        public boolean matches(BlockState state) {
            if (state.getBlock() != this.block) return false;

            if (blockProperties == null || blockProperties.isEmpty()) {
                return true;
            }

            for (Map.Entry<String, String> entry : blockProperties.entrySet()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property == null) return false;

                Optional<?> expected = property.getValue(entry.getValue());
                if (expected.isEmpty()) return false;

                if (!state.getValue(property).equals(expected.get())) {
                    return false;
                }
            }

            return true;
        }
    }

}

