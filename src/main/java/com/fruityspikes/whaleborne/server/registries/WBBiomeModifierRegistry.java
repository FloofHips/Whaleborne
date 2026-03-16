package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.world.ConfigurableSpawnModifier;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class WBBiomeModifierRegistry {

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Whaleborne.MODID);

    public static final RegistryObject<Codec<ConfigurableSpawnModifier>> CONFIGURABLE_SPAWNS =
            BIOME_MODIFIER_SERIALIZERS.register("configurable_spawns", () -> ConfigurableSpawnModifier.CODEC);

}
