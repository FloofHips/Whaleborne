package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.world.ConfigurableSpawnModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class WBBiomeModifierRegistry {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Whaleborne.MODID);

    public static final Supplier<MapCodec<ConfigurableSpawnModifier>> CONFIGURABLE_SPAWNS =
            BIOME_MODIFIER_SERIALIZERS.register("configurable_spawns", () -> ConfigurableSpawnModifier.CODEC);

    public static void register(IEventBus bus) {
        BIOME_MODIFIER_SERIALIZERS.register(bus);
    }
}
