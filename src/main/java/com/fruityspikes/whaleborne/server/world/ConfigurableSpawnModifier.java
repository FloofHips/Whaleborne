package com.fruityspikes.whaleborne.server.world;

import com.fruityspikes.whaleborne.Config;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import java.util.List;

public record ConfigurableSpawnModifier(HolderSet<Biome> biomes, List<MobSpawnSettings.SpawnerData> spawners) implements BiomeModifier {

    public static final MapCodec<ConfigurableSpawnModifier> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfigurableSpawnModifier::biomes),
            MobSpawnSettings.SpawnerData.CODEC.listOf().fieldOf("spawners").forGetter(ConfigurableSpawnModifier::spawners)
    ).apply(builder, ConfigurableSpawnModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && this.biomes.contains(biome)) {
            int configWeight;
            try {
                configWeight = Config.HULLBACK_SPAWN_WEIGHT.getAsInt();
            } catch (Exception e) {
                configWeight = 1; // Fallback to default if config not yet loaded
            }
            if (configWeight > 0) {
                for (MobSpawnSettings.SpawnerData spawner : this.spawners) {
                    builder.getMobSpawnSettings().addSpawn(
                            spawner.type.getCategory(),
                            new MobSpawnSettings.SpawnerData(spawner.type, configWeight, spawner.minCount, spawner.maxCount)
                    );
                }
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return com.fruityspikes.whaleborne.server.registries.WBBiomeModifierRegistry.CONFIGURABLE_SPAWNS.get();
    }
}
