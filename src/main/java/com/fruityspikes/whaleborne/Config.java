package com.fruityspikes.whaleborne;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.fml.ModList;

@EventBusSubscriber(modid = Whaleborne.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue HULLBACK_SPAWN_WEIGHT = SERVER_BUILDER
            .comment("Chance/Weight of a Hullback spawning.",
                     "Weight determines how often the Hullback will be chosen from the list of creatures that can spawn in the ocean.",
                     "Higher value = more common. Vanilla Squid weight is 10, Vanilla Dolphin weight is 2. The Hullback is extremely rare by default.",
                     "Setting this to 0 will disable Hullback natural spawning entirely.")
            .defineInRange("hullbackSpawnWeight", 1, 0, 100);

    public static final ModConfigSpec.IntValue HULLBACK_DESPAWN_TIME_TICKS = SERVER_BUILDER
            .comment("Base time in ticks before a wild hullback despawns.")
            .defineInRange("hullbackDespawnTimeTicks", 24000, 1, 24000);

    public static final ModConfigSpec.IntValue HULLBACK_DESPAWN_TIME_MULTIPLIER = SERVER_BUILDER
            .comment("Multiplier applied to the base despawn time. (1 = 1 Minecraft Day, 2 = 2 Minecraft Days...)")
            .defineInRange("hullbackDespawnTimeMultiplier", 2, 1, 10);

    public static final ModConfigSpec.IntValue HULLBACK_DESPAWN_GRACE_RADIUS = SERVER_BUILDER
            .comment("Radius in blocks around the Hullback where the despawn timer pauses if a player is nearby.",
                     "Setting this to 0 disables the grace zone entirely.")
            .defineInRange("hullbackDespawnGraceRadius", 16, 0, 256);

    public static final ModConfigSpec.BooleanValue ARMOR_PROGRESS = CLIENT_BUILDER
            .comment("Shows hullback building and damage progress. Turning this off solve incompatibility issues with shaders but will sacrifice some visual flair")
            .define("hullbackArmorProgress", true);

    public static final ModConfigSpec.DoubleValue SOUND_DISTANCE = CLIENT_BUILDER
            .comment("Determines how far hullback sounds travel.")
            .defineInRange("hullbackSoundDistance", 3f, 0f, 5f);

    public static final ModConfigSpec.DoubleValue NEAT_OFFSET;
    public static final ModConfigSpec.IntValue HEALTH_BARS_OFFSET;

    static {
        if (ModList.get().isLoaded("neat")) {
            NEAT_OFFSET = CLIENT_BUILDER
                    .comment("Height offset for the Neat health bar on the Hullback entity. Increase to move it higher.")
                    .defineInRange("hullbackNeatOffset", 4.0, 0.0, 10.0);
        } else {
            NEAT_OFFSET = null;
        }

        if (ModList.get().isLoaded("healthbars")) {
            HEALTH_BARS_OFFSET = CLIENT_BUILDER
                    .comment("Height offset for the Fuzs' Health Bars on the Hullback entity. Increase to move it higher.")
                    .defineInRange("hullbackHealthBarsOffset", 0, -100, 100);
        } else {
            HEALTH_BARS_OFFSET = null;
        }
    }

    static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
    static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    public static int hullbackSpawnWeight;
    public static int hullbackDespawnTimeTicks;
    public static int hullbackDespawnTimeMultiplier;
    public static int hullbackDespawnGraceRadius;

    public static boolean armorProgress;
    public static double soundDistance;
    public static double neatOffset;
    public static int healthBarsOffset;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == CLIENT_SPEC) {
            armorProgress = ARMOR_PROGRESS.get();
            soundDistance = SOUND_DISTANCE.get();
            if (NEAT_OFFSET != null) neatOffset = NEAT_OFFSET.get();
            if (HEALTH_BARS_OFFSET != null) {
                healthBarsOffset = HEALTH_BARS_OFFSET.get();
            }
        }
        
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            hullbackSpawnWeight = HULLBACK_SPAWN_WEIGHT.get();
            hullbackDespawnTimeTicks = HULLBACK_DESPAWN_TIME_TICKS.get();
            hullbackDespawnTimeMultiplier = HULLBACK_DESPAWN_TIME_MULTIPLIER.get();
            hullbackDespawnGraceRadius = HULLBACK_DESPAWN_GRACE_RADIUS.get();
        }
    }
}