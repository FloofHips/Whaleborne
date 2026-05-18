package com.fruityspikes.whaleborne.server.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-material hull configuration loaded from datapack JSON.
 * Allows each plank type to have different plank counts, resistance, and armor models.
 */
public record HullConfig(
        ResourceLocation item,
        int planksRequired,
        float resistance,
        String armorModel,
        float blockChance,
        float swimSpeedBonus
) {
    public static final int DEFAULT_PLANKS = 64;
    public static final float DEFAULT_RESISTANCE = 2.0f;
    public static final String DEFAULT_MODEL = "default";
    public static final float DEFAULT_BLOCK_CHANCE = -1.0f; // sentinel: use resistance/70
    public static final float DEFAULT_SWIM_SPEED_BONUS = 0.0f;

    public static final Codec<HullConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(HullConfig::item),
            Codec.INT.optionalFieldOf("planks_required", DEFAULT_PLANKS).forGetter(HullConfig::planksRequired),
            Codec.FLOAT.optionalFieldOf("resistance", DEFAULT_RESISTANCE).forGetter(HullConfig::resistance),
            Codec.STRING.optionalFieldOf("armor_model", DEFAULT_MODEL).forGetter(HullConfig::armorModel),
            Codec.FLOAT.optionalFieldOf("block_chance", DEFAULT_BLOCK_CHANCE).forGetter(HullConfig::blockChance),
            Codec.FLOAT.optionalFieldOf("swim_speed_bonus", DEFAULT_SWIM_SPEED_BONUS).forGetter(HullConfig::swimSpeedBonus)
    ).apply(instance, HullConfig::new));

    /** Returns effective block chance. If block_chance >= 0, use it directly. Otherwise resistance/70. */
    public float getEffectiveBlockChance() {
        return blockChance >= 0 ? blockChance : resistance / 70f;
    }

    /** Create a default config for an item that has no datapack JSON */
    public static HullConfig createDefault(ResourceLocation itemId, float blockResistance) {
        return new HullConfig(itemId, DEFAULT_PLANKS, blockResistance > 0 ? blockResistance : DEFAULT_RESISTANCE,
                DEFAULT_MODEL, DEFAULT_BLOCK_CHANCE, DEFAULT_SWIM_SPEED_BONUS);
    }
}
