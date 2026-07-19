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

    public static final ModConfigSpec.DoubleValue HULLBACK_SPAWN_CHANCE = SERVER_BUILDER
            .comment("Probability of a Hullback spawn attempt succeeding (0.0 to 1.0).",
                     "0.001 = 0.1% chance per spawn attempt. 0.0 disables natural spawning entirely.",
                     "The Hullback uses its own spawn category and does not compete with other ocean mobs.")
            .defineInRange("hullbackSpawnChance", 0.005, 0.0, 1.0);

    public static final ModConfigSpec.IntValue HULLBACK_SPAWN_CAP = SERVER_BUILDER
            .comment("Maximum number of wild Hullbacks allowed in the nearby area.",
                     "Once this cap is reached, no more will spawn until existing ones despawn or are tamed.")
            .defineInRange("hullbackSpawnCap", 1, 0, 10);

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

    public static final ModConfigSpec.DoubleValue HULLBACK_DEPTH_SAILING = SERVER_BUILDER
            .comment("Target Y offset from sea level when a player is actively sailing at the helm.",
                     "Negative values submerge the whale. -4.55 aligns the deck nicely with the waterline.")
            .defineInRange("hullbackDepthSailing", -4.55, -20.0, 0.0);

    public static final ModConfigSpec.DoubleValue HULLBACK_DEPTH_BOARDING = SERVER_BUILDER
            .comment("Target Y offset from sea level when the Hullback is tamed, anchored or stationary.",
                     "-5.0 provides a stable boarding height for players.")
            .defineInRange("hullbackDepthBoarding", -5.0, -20.0, 0.0);

    public static final ModConfigSpec.DoubleValue HULLBACK_DEPTH_WILD = SERVER_BUILDER
            .comment("Target Y offset from sea level for wild/active Hullbacks.",
                     "-6.5 keeps wild whales slightly deeper.")
            .defineInRange("hullbackDepthWild", -6.5, -20.0, 0.0);

    public static final ModConfigSpec.BooleanValue HULLBACK_PAUSE_BREACH_TIMER = SERVER_BUILDER
            .comment("When enabled, the Hullback's breaching air timer pauses while it is immobile or being controlled by a player.",
                     "This prevents the whale from needing to breach for air while someone is at the helm or while stationary.")
            .define("hullbackPauseBreachTimer", true);

    public static final ModConfigSpec.BooleanValue ARMOR_PROGRESS = CLIENT_BUILDER
            .comment("Shows hullback building and damage progress. Turning this off solve incompatibility issues with shaders but will sacrifice some visual flair")
            .define("hullbackArmorProgress", true);

    public static final ModConfigSpec.DoubleValue SOUND_DISTANCE = CLIENT_BUILDER
            .comment("Determines how far hullback sounds travel.")
            .defineInRange("hullbackSoundDistance", 3f, 0f, 5f);

    public static final ModConfigSpec.BooleanValue WAKE_RENDERING = CLIENT_BUILDER
            .comment("Built-in procedural Hullback wake rendering (foam, bow splash); independent from the Wakes mod's own integration.")
            .define("hullbackWakeRendering", false);

    public static final ModConfigSpec.BooleanValue WAKES_RECOLOR_FRUSTUM_CULLING;
    public static final ModConfigSpec.BooleanValue WAKES_RECOLOR_TEMPORAL_LOD;
    public static final ModConfigSpec.DoubleValue HULLBACK_WAKE_MAX_DISTANCE;
    public static final ModConfigSpec.DoubleValue HULLBACK_WAKE_WIDTH_SCALE;

    public static final ModConfigSpec.DoubleValue NEAT_OFFSET;
    public static final ModConfigSpec.IntValue HEALTH_BARS_OFFSET;

    static {
        if (ModList.get().isLoaded("wakes")) {
            WAKES_RECOLOR_FRUSTUM_CULLING = CLIENT_BUILDER
                    .comment("Performance: only recolor wake bricks that are on screen.",
                             "Wakes recolors every wake every tick regardless of the camera. Gating that to the view",
                             "frustum removes wasted work with no gameplay effect. Off-screen wakes are repopulated",
                             "before they scroll into view. Disable if a shader pack shows missing distant foam.")
                    .define("wakesRecolorFrustumCulling", true);
            WAKES_RECOLOR_TEMPORAL_LOD = CLIENT_BUILDER
                    .comment("Performance: refresh distant on-screen wake bricks every few ticks instead of every",
                             "tick (near bricks stay every tick). New and growing wakes are never delayed, only far,",
                             "already-drawn foam animates a little less often. No change to wake size or coverage.")
                    .define("wakesRecolorTemporalLod", true);
            HULLBACK_WAKE_MAX_DISTANCE = CLIENT_BUILDER
                    .comment("Performance: skip feeding wakes to the Wakes mod for whales farther than this many blocks",
                             "from the camera. 0 = unlimited (original behavior).")
                    .defineInRange("hullbackWakeMaxDistance", 0.0, 0.0, 512.0);
            HULLBACK_WAKE_WIDTH_SCALE = CLIENT_BUILDER
                    .comment("Performance/visual: scale the width of the whale's wake trails fed to the Wakes mod.",
                             "Lower is cheaper but thinner. 1.0 = original width.")
                    .defineInRange("hullbackWakeWidthScale", 1.0, 0.1, 1.0);
        } else {
            WAKES_RECOLOR_FRUSTUM_CULLING = null;
            WAKES_RECOLOR_TEMPORAL_LOD = null;
            HULLBACK_WAKE_MAX_DISTANCE = null;
            HULLBACK_WAKE_WIDTH_SCALE = null;
        }

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

    public static double hullbackSpawnChance;
    public static int hullbackSpawnCap;
    public static int hullbackDespawnTimeTicks;
    public static int hullbackDespawnTimeMultiplier;
    public static int hullbackDespawnGraceRadius;
    public static double hullbackDepthSailing;
    public static double hullbackDepthBoarding;
    public static double hullbackDepthWild;
    public static boolean hullbackPauseBreachTimer;

    public static boolean armorProgress;
    public static double soundDistance;
    public static boolean wakeRendering;
    public static boolean wakesRecolorFrustumCulling = true;
    public static boolean wakesRecolorTemporalLod = true;
    public static double hullbackWakeMaxDistance = 0.0;
    public static double hullbackWakeMaxDistanceSq = 0.0;
    public static double hullbackWakeWidthScale = 1.0;
    public static double neatOffset;
    public static int healthBarsOffset;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (!(event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading)) {
            return;
        }

        if (event.getConfig().getSpec() == CLIENT_SPEC) {
            armorProgress = ARMOR_PROGRESS.get();
            soundDistance = SOUND_DISTANCE.get();
            wakeRendering = WAKE_RENDERING.get();
            if (WAKES_RECOLOR_FRUSTUM_CULLING != null) wakesRecolorFrustumCulling = WAKES_RECOLOR_FRUSTUM_CULLING.get();
            if (WAKES_RECOLOR_TEMPORAL_LOD != null) wakesRecolorTemporalLod = WAKES_RECOLOR_TEMPORAL_LOD.get();
            if (HULLBACK_WAKE_MAX_DISTANCE != null) {
                hullbackWakeMaxDistance = HULLBACK_WAKE_MAX_DISTANCE.get();
                hullbackWakeMaxDistanceSq = hullbackWakeMaxDistance * hullbackWakeMaxDistance;
            }
            if (HULLBACK_WAKE_WIDTH_SCALE != null) hullbackWakeWidthScale = HULLBACK_WAKE_WIDTH_SCALE.get();
            if (NEAT_OFFSET != null) neatOffset = NEAT_OFFSET.get();
            if (HEALTH_BARS_OFFSET != null) {
                healthBarsOffset = HEALTH_BARS_OFFSET.get();
            }
        }
        
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            hullbackSpawnChance = HULLBACK_SPAWN_CHANCE.get();
            hullbackSpawnCap = HULLBACK_SPAWN_CAP.get();
            hullbackDespawnTimeTicks = HULLBACK_DESPAWN_TIME_TICKS.get();
            hullbackDespawnTimeMultiplier = HULLBACK_DESPAWN_TIME_MULTIPLIER.get();
            hullbackDespawnGraceRadius = HULLBACK_DESPAWN_GRACE_RADIUS.get();
            hullbackDepthSailing = HULLBACK_DEPTH_SAILING.get();
            hullbackDepthBoarding = HULLBACK_DEPTH_BOARDING.get();
            hullbackDepthWild = HULLBACK_DEPTH_WILD.get();
            hullbackPauseBreachTimer = HULLBACK_PAUSE_BREACH_TIMER.get();
        }
    }
}