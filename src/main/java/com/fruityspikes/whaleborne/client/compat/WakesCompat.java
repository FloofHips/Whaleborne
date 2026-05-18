package com.fruityspikes.whaleborne.client.compat;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.components.hullback.HullbackPartManager;
import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.particle.ModParticles;
import com.leclowndu93150.wakes.simulation.WakeHandler;
import com.leclowndu93150.wakes.simulation.WakeNode;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Compatibility bridge between Whaleborne and the Wakes mod.
 */
public final class WakesCompat {

    private static Boolean loaded;
    private static final int PART_COUNT = 5;

    // ─── Per-entity tracking (combined array) ────────────────────
    // [x0,z0, x1,z1, ..., x4,z4, valid0..valid4] = 15 doubles
    private static final int TRACK_SIZE = PART_COUNT * 2 + PART_COUNT;
    private static final Map<Integer, double[]> ENTITY_TRACK = new HashMap<>();
    private static final Map<Integer, double[]> PASSENGER_PREV_XZ = new HashMap<>();

    // ─── Hull widths ─────────────────────────────────────────────
    private static final float[] BARE_WIDTHS =    { 4.5f, 5.0f, 5.0f, 2.0f, 3.5f };
    private static final float[] ARMORED_WIDTHS = { 6.5f, 6.0f, 6.0f, 2.0f, 3.5f };

    private static final boolean[] ALWAYS_SURFACE = { true, true, true, false, false };
    private static final int[] PART_TICK_PHASE = { 0, 1, 0, 1, 0 };

    private static final float EMERGE_DEPTH = 1.5f;
    private static final double MIN_VEL_SQ = 0.005 * 0.005;
    private static final double MAX_VEL_SQ = 15.0 * 15.0;

    private static final int PASSENGER_INTERVAL = 3;
    private static final float PASSENGER_MIN_INTENSITY = 0.05f;

    private static final float BOW_SPLASH_MIN_INTENSITY = 0.25f;
    private static final int BOW_SPLASH_INTERVAL = 2;

    // ─── Cached config (refreshed every 100 ticks) ───────────────
    private static float cachedBaseStrength = 50f;
    private static float cachedHullStrength = 100f;   // baseStrength * 2.0 (pre-computed)
    private static float cachedWidgetStrength = 75f;   // baseStrength * 1.5 (pre-computed)
    private static boolean cachedDisabled = false;
    private static boolean cachedSpawnParticles = true;
    private static int configLastTick = -1;

    // ─── Cached WakeHandler (refreshed every 20 ticks) ───────────
    private static WakeHandler cachedHandler = null;
    private static int handlerLastTick = -1;

    // ─── Cached particle type (avoid Supplier.get() per spawn) ───
    private static net.minecraft.core.particles.SimpleParticleType cachedSplashCloud = null;

    // ─── Water surface cache ─────────────────────────────────────
    private static final Map<Integer, float[]> WATER_CACHE = new HashMap<>();
    private static final int WATER_CACHE_TICKS = 10;
    private static final BlockPos.MutableBlockPos PROBE = new BlockPos.MutableBlockPos();

    // ─── Cached forward direction + yaw (from hull loop → bow splash) ──
    private static double cachedFwdX, cachedFwdZ;
    private static float cachedFwdYaw;
    private static boolean cachedFwdValid;

    private WakesCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("wakes");
        }
        return loaded;
    }

    private static void refreshConfigIfNeeded(int tick) {
        if (configLastTick < 0 || tick - configLastTick >= 100) {
            cachedBaseStrength = WakesConfig.GENERAL.initialStrength.get();
            cachedHullStrength = cachedBaseStrength * 2.0f;
            cachedWidgetStrength = cachedBaseStrength * 1.5f;
            cachedDisabled = WakesConfig.GENERAL.disableMod.get();
            cachedSpawnParticles = WakesConfig.APPEARANCE.spawnParticles.get();
            cachedSplashCloud = ModParticles.SPLASH_CLOUD.get();
            configLastTick = tick;
        }
    }

    private static WakeHandler getHandler(Level level, int tick) {
        if (tick - handlerLastTick >= 20) {
            cachedHandler = WakeHandler.getInstance(level).orElse(null);
            handlerLastTick = tick;
        }
        return cachedHandler;
    }

    /**
     * Main entry point — called every client tick from HullbackEntity.
     */
    public static void generatePartWakes(HullbackEntity hullback) {
        Level level = hullback.level();
        if (!level.isClientSide) return;
        if (hullback.wakeIntensity < 0.01f && hullback.flukeSplashIntensity < 0.01f) return;
        if (!hullback.isInLiquid()) return; // isInLiquid covers isInWater
        int tick = hullback.tickCount;
        refreshConfigIfNeeded(tick);
        if (cachedDisabled) return;

        WakeHandler handler = getHandler(level, tick);
        if (handler == null) return;

        HullbackPartManager pm = hullback.getPartManager();
        if (pm == null || pm.partPosition == null) return;

        int id = hullback.getId();
        float waterY = getCachedWaterY(level, hullback, id);
        if (Float.isNaN(waterY)) return;
        int floorY = Mth.floor(waterY);

        double[] track = ENTITY_TRACK.get(id);
        if (track == null) {
            track = new double[TRACK_SIZE];
            ENTITY_TRACK.put(id, track);
        }

        boolean armored = hullback.getArmorProgress() > 0.5f;
        float[] widths = armored ? ARMORED_WIDTHS : BARE_WIDTHS;

        int phase = tick & 1;
        int partCount = Math.min(pm.partPosition.length, PART_COUNT);

        cachedFwdValid = false;

        for (int i = 0; i < partCount; i++) {
            Vec3 pp = pm.partPosition[i];
            if (pp == null) continue;

            double cx = pp.x;
            double cz = pp.z;
            int slot = i * 2;
            int validSlot = PART_COUNT * 2 + i;

            // Cache forward direction + yaw when processing head (part 1)
            if (i == 1 && pm.partPosition[0] != null) {
                double fdx = pm.partPosition[0].x - cx;
                double fdz = pm.partPosition[0].z - cz;
                double fLenSq = fdx * fdx + fdz * fdz;
                if (fLenSq > 0.0001) {
                    double fLen = Math.sqrt(fLenSq);
                    cachedFwdX = fdx / fLen;
                    cachedFwdZ = fdz / fLen;
                    cachedFwdYaw = (float) (Math.atan2(cachedFwdZ, cachedFwdX) * (180.0 / Math.PI)) - 90f;
                    cachedFwdValid = true;
                }
            }

            if (PART_TICK_PHASE[i] != phase) {
                if (track[validSlot] == 0.0) {
                    track[slot] = cx;
                    track[slot + 1] = cz;
                    track[validSlot] = 1.0;
                }
                continue;
            }

            float sMult = 1.0f;
            if (!ALWAYS_SURFACE[i]) {
                float depth = waterY - (float) pp.y;
                if (depth > EMERGE_DEPTH) {
                    track[validSlot] = 0.0;
                    continue;
                }
                if (depth > 0) {
                    sMult = 1.0f - (depth / EMERGE_DEPTH);
                    if (sMult < 0.1f) sMult = 0.1f;
                }
            }

            if (track[validSlot] != 0.0) {
                double px = track[slot];
                double pz = track[slot + 1];
                double dx = cx - px;
                double dz = cz - pz;
                double vSq = dx * dx + dz * dz;

                if (vSq > MIN_VEL_SQ && vSq < MAX_VEL_SQ) {
                    Set<WakeNode> nodes = WakeNode.Factory.thickNodeTrail(
                            px, pz, cx, cz, floorY,
                            cachedHullStrength * sMult,
                            Math.sqrt(vSq), widths[i]);
                    for (WakeNode node : nodes) {
                        handler.insert(node);
                    }
                }
            }

            track[slot] = cx;
            track[slot + 1] = cz;
            track[validSlot] = 1.0;
        }

        // ── Bow splash (throttled, reuses cached fwd + yaw) ──
        if (hullback.wakeIntensity >= BOW_SPLASH_MIN_INTENSITY
                && cachedSpawnParticles
                && (tick & 1) == 0  // same as % 2 but branchless
                && cachedFwdValid) {
            spawnBowSplash(hullback, pm, waterY, armored);
        }

        // ── Passengers (every 3 ticks) ──
        if (hullback.wakeIntensity >= PASSENGER_MIN_INTENSITY
                && tick % PASSENGER_INTERVAL == 0) {
            processPassengers(hullback, handler, waterY, floorY);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Bow splash — uses cachedFwdX/Z/Yaw (no recomputation)
    // ═══════════════════════════════════════════════════════════════

    private static void spawnBowSplash(HullbackEntity hullback, HullbackPartManager pm,
                                       float waterY, boolean armored) {
        Vec3 nosePos = pm.partPosition[0];
        if (nosePos == null) return;

        Level level = hullback.level();
        float eqVel = hullback.wakeIntensity * 0.15f;
        float scaledVel = 1.5f * eqVel;

        double perpX = -cachedFwdZ;
        double perpZ = cachedFwdX;

        float fwdOffset = armored ? 3.0f : 2.75f;
        double baseX = nosePos.x + cachedFwdX * fwdOffset;
        double baseZ = nosePos.z + cachedFwdZ * fwdOffset;

        float hullHW = (armored ? ARMORED_WIDTHS[0] : BARE_WIDTHS[0]) * 0.5f;

        // Reuse cachedFwdYaw (computed once in hull loop, no atan2 here)
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int layer = 0; layer < 2; layer++) {
            double layerY = waterY + layer * 0.1;
            for (int i = 0; i < 2; i++) {
                double lat = (rng.nextDouble() - 0.5) * 2.0 * hullHW;

                // Compute velocity components inline (avoid Vec3.directionFromRotation alloc)
                float vPitch = rng.nextFloat() * 45f;
                float vYaw = cachedFwdYaw + (rng.nextFloat() - 0.5f) * 30f;
                float pitchRad = vPitch * Mth.DEG_TO_RAD;
                float yawRad = -vYaw * Mth.DEG_TO_RAD;
                float cosP = Mth.cos(pitchRad);
                double vx = -Mth.sin(yawRad) * cosP * scaledVel;
                double vy = -Mth.sin(pitchRad) * scaledVel;
                double vz = Mth.cos(yawRad) * cosP * scaledVel;

                level.addParticle(cachedSplashCloud,
                        baseX + perpX * lat, layerY, baseZ + perpZ * lat,
                        vx, vy, vz);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Passengers
    // ═══════════════════════════════════════════════════════════════

    private static void processPassengers(HullbackEntity hullback, WakeHandler handler,
                                          float waterY, int floorY) {
        List<Entity> passengers = hullback.getPassengers();
        if (passengers.isEmpty()) return;

        for (int p = 0, size = passengers.size(); p < size; p++) {
            Entity passenger = passengers.get(p);
            if (passenger == null || passenger.isRemoved()) continue;
            emitEntityWake(passenger, handler, waterY, floorY);

            List<Entity> subs = passenger.getPassengers();
            for (int s = 0, sSize = subs.size(); s < sSize; s++) {
                Entity sub = subs.get(s);
                if (sub != null && !sub.isRemoved()) {
                    emitEntityWake(sub, handler, waterY, floorY);
                }
            }
        }
    }

    private static void emitEntityWake(Entity entity, WakeHandler handler,
                                       float waterY, int floorY) {
        float bot = (float) entity.getY();
        float top = bot + entity.getBbHeight();

        boolean crosses = bot <= waterY && top >= waterY;
        boolean emerging = !crosses && top >= (waterY - EMERGE_DEPTH) && top < waterY;
        if (!crosses && !emerging) {
            PASSENGER_PREV_XZ.remove(entity.getId());
            return;
        }

        float sMult;
        if (crosses) {
            sMult = Mth.clamp((waterY - bot) / entity.getBbHeight(), 0.1f, 1.0f);
        } else {
            sMult = Mth.clamp(1.0f - (waterY - top) / EMERGE_DEPTH, 0.05f, 0.6f);
        }

        double cx = entity.getX();
        double cz = entity.getZ();
        int eid = entity.getId();
        double[] prev = PASSENGER_PREV_XZ.get(eid);

        if (prev != null) {
            double dx = cx - prev[0];
            double dz = cz - prev[1];
            double vSq = dx * dx + dz * dz;

            if (vSq > MIN_VEL_SQ && vSq < MAX_VEL_SQ) {
                Set<WakeNode> nodes = WakeNode.Factory.thickNodeTrail(
                        prev[0], prev[1], cx, cz, floorY,
                        cachedWidgetStrength * sMult,
                        Math.sqrt(vSq),
                        entity.getBbWidth());
                for (WakeNode node : nodes) {
                    handler.insert(node);
                }
            }
        }

        if (prev == null) {
            PASSENGER_PREV_XZ.put(eid, new double[]{ cx, cz });
        } else {
            prev[0] = cx;
            prev[1] = cz;
        }
    }

    public static void removeEntity(int entityId) {
        ENTITY_TRACK.remove(entityId);
        PASSENGER_PREV_XZ.remove(entityId);
        WATER_CACHE.remove(entityId);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Water surface cache
    // ═══════════════════════════════════════════════════════════════

    private static float getCachedWaterY(Level level, HullbackEntity hullback, int id) {
        float[] cache = WATER_CACHE.get(id);
        int tick = hullback.tickCount;
        if (cache != null && (tick - (int) cache[1]) < WATER_CACHE_TICKS) {
            return cache[0];
        }

        float waterY = findWaterSurface(level, hullback);
        if (cache != null) {
            cache[0] = waterY;
            cache[1] = tick;
        } else {
            WATER_CACHE.put(id, new float[]{ waterY, tick });
        }
        return waterY;
    }

    private static float findWaterSurface(Level level, HullbackEntity hullback) {
        int ex = Mth.floor(hullback.getX());
        int ez = Mth.floor(hullback.getZ());
        int seaLevel = level.getSeaLevel();

        for (int y = seaLevel + 3; y >= seaLevel - 6; y--) {
            PROBE.set(ex, y, ez);
            FluidState fluid = level.getFluidState(PROBE);
            if (!fluid.isEmpty() && WakesConfig.getFluidWhitelist().contains(fluid.getType())) {
                FluidState above = level.getFluidState(PROBE.above());
                if (above.isEmpty()) {
                    return y + fluid.getHeight(level, PROBE);
                }
            }
        }
        return seaLevel;
    }
}
