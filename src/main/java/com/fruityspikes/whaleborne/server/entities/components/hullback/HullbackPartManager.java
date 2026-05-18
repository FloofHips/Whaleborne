package com.fruityspikes.whaleborne.server.entities.components.hullback;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackWalkableEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Positions the Hullback's part entities and the per-part walkable platform tile grids. */
public class HullbackPartManager {
    private final HullbackEntity hullback;
    public final HullbackPartEntity[] subEntities;

    // ─── Constants ────────────────────────────────────────────────
    private static final int PART_COUNT = 5;
    private SeatLayout seatLayout = SeatLayout.defaultLayout();
    private PlatformLayout platformLayout = PlatformLayout.defaultLayout();
    private static final float SWIM_CYCLE_TICK_MULTIPLIER = 0.1f;
    private static final float HEAD_BODY_SWIM_AMPLITUDE = 2f;
    private static final float TAIL_SWIM_AMPLITUDE = 8f;
    private static final float TAIL_PITCH_SCALE = 1.5f;
    private static final float TAIL_PITCH_SWIM_AMPLITUDE = 20f;
    private static final float FLUKE_DISTANCE = 4.0f;
    private static final float FLUKE_Y_SWIM_AMPLITUDE = 5.5f;
    private static final float FLUKE_PITCH_SCALE = 1.5f;
    private static final float FLUKE_PITCH_SWIM_AMPLITUDE = 30f;
    private static final double MOVE_ENTITIES_THRESHOLD = 0.001;
    private static final float UNSTABLE_PLATFORM_FACTOR = 0.5F;
    private static final float PLAYER_SMOOTH_FACTOR = 0.8F;

    // Position/Rotation Arrays
    public final Vec3[] prevPartPositions = new Vec3[PART_COUNT];
    public final Vec3[] partPosition = new Vec3[PART_COUNT];
    public final float[] partYRot = new float[PART_COUNT];
    public final float[] partXRot = new float[PART_COUNT];
    public final Vec3[] oldPartPosition = new Vec3[PART_COUNT];
    public final float[] oldPartYRot = new float[PART_COUNT];
    public final float[] oldPartXRot = new float[PART_COUNT];

    // Seats (pre-allocated for MAX_SEATS, only activeSeatCount are computed)
    public final Vec3[] seats = new Vec3[SeatLayout.MAX_SEATS];
    public final Vec3[] oldSeats = new Vec3[SeatLayout.MAX_SEATS];
    private Vec3 smoothedFlukeSeat = null;
    private Vec3 rawFlukeSeat = null;
    private static final float FLUKE_SEAT_SMOOTH_FACTOR = 0.35f;

    private static final float[] PART_DRAG_FACTORS = {1f, 0.9f, 0.2f, 0.1f, 0.09f};
    private static final Vec3[] BASE_OFFSETS = {
            new Vec3(0, 0, 6),      // Nose
            new Vec3(0, 0, 2.5),    // Head
            new Vec3(0, 0, -2.25),  // Body
            new Vec3(0, 0, -7),     // Tail
            new Vec3(0, 0, -11)     // Fluke
    };
    private static final double[] MAX_DIST = {10.0, 3.55, 4.8, 4.8, 4.1};

    /** Pre-allocated working array reused every tick instead of {@code new Vec3[5]}. */
    private final Vec3[] partOffsetsScratch = new Vec3[PART_COUNT];
    // Stable-skip snapshot for updatePartPositions.
    private double lastWhaleX = Double.NaN, lastWhaleY = Double.NaN, lastWhaleZ = Double.NaN;
    private float lastWhaleYaw = Float.NaN, lastWhalePitch = Float.NaN;
    private boolean partsConverged = false;
    private boolean partSnapshotValid = false;
    private static final double PART_STABILITY_EPSILON_SQ = 1.0e-8;

    public HullbackPartManager(HullbackEntity hullback, HullbackPartEntity[] subEntities) {
        this.hullback = hullback;
        this.subEntities = subEntities;
        Arrays.fill(partPosition, Vec3.ZERO);
    }

    public void setOldPosAndRots() {
        for (int i = 0; i < 5; i++) {
            this.oldPartPosition[i] = subEntities[i].position();
            this.oldPartYRot[i] = subEntities[i].getYRot();
            this.oldPartXRot[i] = subEntities[i].getXRot();
        }
    }

    public void updatePartPositions() {
        // Stable-skip: when the whale hasn't moved/rotated and parts already converged from the
        // swim-cycle lerp, the entire computation would re-derive the same positions. Skip it.
        double whaleX = hullback.getX(), whaleY = hullback.getY(), whaleZ = hullback.getZ();
        float whaleYaw = hullback.getYRot(), whalePitch = hullback.getXRot();
        boolean noSwim = hullback.isPitchLocked() || hullback.getStationaryTicks() > 0;
        if (partSnapshotValid && partsConverged && noSwim
                && whaleX == lastWhaleX && whaleY == lastWhaleY && whaleZ == lastWhaleZ
                && whaleYaw == lastWhaleYaw && whalePitch == lastWhalePitch
                && prevPartPositions[0] != null) {
            // Parts unchanged from last tick: consumers still see the prior values and
            // subEntities are at their last moveTo coords, so skipping recompute is safe.
            return;
        }
        lastWhaleX = whaleX; lastWhaleY = whaleY; lastWhaleZ = whaleZ;
        lastWhaleYaw = whaleYaw; lastWhalePitch = whalePitch;
        partSnapshotValid = true;

        // Work array reused every tick (pre-allocated, see partOffsetsScratch field).
        Vec3[] offsets = partOffsetsScratch;
        for (int i = 0; i < BASE_OFFSETS.length; i++) {
            offsets[i] = BASE_OFFSETS[i];
        }

        if (prevPartPositions[0] == null) {
            float yawRadInit = -hullback.getYRot() * Mth.DEG_TO_RAD;
            float pitchRadInit = hullback.getXRot() * Mth.DEG_TO_RAD;
            for (int i = 0; i < prevPartPositions.length; i++) {
                Vec3 rotatedOffset = BASE_OFFSETS[i].yRot(yawRadInit).xRot(pitchRadInit);
                prevPartPositions[i] = hullback.position().add(rotatedOffset);
            }
        }

        float horizontalSpeed = hullback.getAnimationSwimSpeed();
        // Disable swimCycle when anchored or stationary (pitch locked) to prevent tilting/wiggling
        // CRITICAL FIX: Disables swimCycle when platforms are stable
        float swimCycle;
        if (hullback.isPitchLocked() || hullback.getStationaryTicks() > 0) {
             swimCycle = 0f;
        } else {
             // MODIFICATION: swimCycle based on horizontal speed only when free
             swimCycle = Mth.sin((float) hullback.level().getGameTime() * SWIM_CYCLE_TICK_MULTIPLIER) * horizontalSpeed;
        }
        float yawRad = -hullback.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = hullback.getXRot() * Mth.DEG_TO_RAD;

        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = offsets[i]
                    .yRot(yawRad)
                    .xRot(pitchRad);

            offsets[i] = offsets[i].add(hullback.getX(), hullback.getY(), hullback.getZ());

            if (i > 0) {
                offsets[i] = new Vec3(
                        Mth.lerp(PART_DRAG_FACTORS[i], prevPartPositions[i].x, offsets[i].x),
                        Mth.lerp(PART_DRAG_FACTORS[i], prevPartPositions[i].y, offsets[i].y),
                        Mth.lerp(PART_DRAG_FACTORS[i], prevPartPositions[i].z, offsets[i].z)
                );

                Vec3 parentPos = prevPartPositions[i-1];
                double dist = offsets[i].distanceTo(parentPos);
                if (dist > MAX_DIST[i]) {
                     offsets[i] = parentPos.add(offsets[i].subtract(parentPos).normalize().scale(MAX_DIST[i]));
                }
            }
            prevPartPositions[i] = offsets[i];
        }

        this.partPosition[0] = prevPartPositions[0];
        this.partYRot[0] = calculateYaw(prevPartPositions[0], prevPartPositions[1]);
        this.partXRot[0] = calculatePitch(prevPartPositions[0], prevPartPositions[1]);
        subEntities[0].moveTo(prevPartPositions[0].x, prevPartPositions[0].y, prevPartPositions[0].z,
                partYRot[0],
                partXRot[0]);

        this.partPosition[1] = new Vec3(prevPartPositions[1].x, prevPartPositions[1].y + swimCycle * HEAD_BODY_SWIM_AMPLITUDE, prevPartPositions[1].z);
        this.partYRot[1] = calculateYaw(prevPartPositions[0], prevPartPositions[1]);
        this.partXRot[1] = calculatePitch(prevPartPositions[0], prevPartPositions[1]);
        subEntities[1].moveTo(prevPartPositions[1].x, prevPartPositions[1].y + swimCycle * HEAD_BODY_SWIM_AMPLITUDE, prevPartPositions[1].z,
                partYRot[1],
                partXRot[1]);

        this.partPosition[2] = new Vec3(prevPartPositions[2].x, prevPartPositions[2].y + swimCycle * HEAD_BODY_SWIM_AMPLITUDE, prevPartPositions[2].z);
        this.partYRot[2] = calculateYaw(prevPartPositions[1], prevPartPositions[2]);
        this.partXRot[2] = calculatePitch(prevPartPositions[1], prevPartPositions[2]);
        subEntities[2].moveTo(prevPartPositions[2].x,
                prevPartPositions[2].y + swimCycle * HEAD_BODY_SWIM_AMPLITUDE,
                prevPartPositions[2].z,
                partYRot[2],
                partXRot[2]);

        this.partPosition[3] = new Vec3(prevPartPositions[3].x, prevPartPositions[3].y + swimCycle * TAIL_SWIM_AMPLITUDE, prevPartPositions[3].z);
        this.partYRot[3] = calculateYaw(prevPartPositions[2], prevPartPositions[3]);
        this.partXRot[3] = calculatePitch(prevPartPositions[2], prevPartPositions[3]) * TAIL_PITCH_SCALE - swimCycle * TAIL_PITCH_SWIM_AMPLITUDE;
        subEntities[3].moveTo(prevPartPositions[3].x,
                prevPartPositions[3].y + swimCycle * TAIL_SWIM_AMPLITUDE,
                prevPartPositions[3].z,
                partYRot[3],
                partXRot[3]);

        Vec3 flukeOffset = new Vec3(0, 0, -FLUKE_DISTANCE)
                .yRot(-subEntities[3].getYRot() * Mth.DEG_TO_RAD)
                .xRot(subEntities[3].getXRot() * Mth.DEG_TO_RAD);

        Vec3 flukeTarget = new Vec3(
                subEntities[3].getX() + flukeOffset.x,
                subEntities[3].getY() + flukeOffset.y + swimCycle * FLUKE_Y_SWIM_AMPLITUDE,
                subEntities[3].getZ() + flukeOffset.z
        );

        float flukeYaw = calculateYaw(subEntities[3].position(), flukeTarget);
        float flukePitch = calculatePitch(subEntities[3].position(), flukeTarget);

        flukeYaw = Mth.rotLerp(PART_DRAG_FACTORS[4], oldPartYRot[4], flukeYaw);
        float flukeXRot = flukePitch * FLUKE_PITCH_SCALE + swimCycle * FLUKE_PITCH_SWIM_AMPLITUDE;

        this.partPosition[4] = flukeTarget;
        this.partYRot[4] = flukeYaw;
        this.partXRot[4] = flukeXRot;
        subEntities[4].moveTo(
                flukeTarget.x,
                flukeTarget.y,
                flukeTarget.z,
                flukeYaw,
                flukeXRot
        );

        // Calculate seat positions after updating part positions
        calculateSeats();

        // Convergence check for the next-tick stable-skip — parts are "converged" once their
        // displacement vs the pre-tick snapshot is below an imperceptible threshold.
        double maxDeltaSq = 0.0;
        for (int i = 0; i < PART_COUNT; i++) {
            Vec3 cur = partPosition[i];
            Vec3 prev = oldPartPosition[i];
            if (prev == null) { maxDeltaSq = Double.POSITIVE_INFINITY; break; }
            double dx = cur.x - prev.x, dy = cur.y - prev.y, dz = cur.z - prev.z;
            double sq = dx * dx + dy * dy + dz * dz;
            if (sq > maxDeltaSq) maxDeltaSq = sq;
        }
        partsConverged = maxDeltaSq <= PART_STABILITY_EPSILON_SQ;
    }

    // ─── Seat layout (dynamic, loaded from SeatLayout) ─────────

    public SeatLayout getSeatLayout() { return seatLayout; }
    public void setSeatLayout(SeatLayout layout) { this.seatLayout = layout; }

    public PlatformLayout getPlatformLayout() { return platformLayout; }
    public void setPlatformLayout(PlatformLayout layout) {
        this.platformLayout = layout;
        // Re-spawn tiles for any part that already had a platform so the new shape is visible.
        for (int p = 0; p < PlatformLayout.MAX_PARTS; p++) {
            if (!partTiles[p].isEmpty()) spawnPlatform(p);
        }
    }
    public int getFlukeSeatIndex() { return seatLayout.getFlukeSeatIndex(); }
    public Vec3 getRawFlukeSeat() { return rawFlukeSeat; }

    public void calculateSeats() {
        if (partPosition == null || partYRot == null || partXRot == null || partPosition[0] == null) return;

        int activeSeatCount = seatLayout.getActiveSeatCount();
        int flukeSeatIdx = seatLayout.getFlukeSeatIndex();

        // Current seats — all except fluke (handled separately for smoothing)
        for (int i = 0; i < activeSeatCount; i++) {
            if (i == flukeSeatIdx) continue; // fluke handled below
            SeatLayout.SeatDef def = seatLayout.getSeatDef(i);
            seats[i] = computeSeat(partPosition[def.posPartIndex()], partXRot[def.rotPartIndex()], partYRot[def.rotPartIndex()], def.offset());
        }

        // Fluke seat — raw position stored for widgets, smoothed for players/mobs
        if (flukeSeatIdx >= 0 && flukeSeatIdx < activeSeatCount) {
            SeatLayout.SeatDef flukeDef = seatLayout.getSeatDef(flukeSeatIdx);
            rawFlukeSeat = computeSeat(partPosition[flukeDef.posPartIndex()], partXRot[flukeDef.rotPartIndex()], partYRot[flukeDef.rotPartIndex()], flukeDef.offset());
            if (smoothedFlukeSeat == null) {
                smoothedFlukeSeat = rawFlukeSeat;
            } else {
                smoothedFlukeSeat = new Vec3(
                        Mth.lerp(FLUKE_SEAT_SMOOTH_FACTOR, smoothedFlukeSeat.x, rawFlukeSeat.x),
                        Mth.lerp(FLUKE_SEAT_SMOOTH_FACTOR, smoothedFlukeSeat.y, rawFlukeSeat.y),
                        Mth.lerp(FLUKE_SEAT_SMOOTH_FACTOR, smoothedFlukeSeat.z, rawFlukeSeat.z)
                );
            }
            seats[flukeSeatIdx] = smoothedFlukeSeat;
        }

        // Old seats (for interpolation) — no smoothing needed for old positions
        for (int i = 0; i < activeSeatCount; i++) {
            SeatLayout.SeatDef def = seatLayout.getSeatDef(i);
            oldSeats[i] = computeSeat(oldPartPosition[def.posPartIndex()], oldPartXRot[def.rotPartIndex()], oldPartYRot[def.rotPartIndex()], def.offset());
        }
    }

    /** Computes a seat world position from a part position, rotation, and offset vector. */
    private Vec3 computeSeat(Vec3 pos, float xRot, float yRot, Vec3 offset) {
        return pos.add(offset.xRot(xRot * Mth.DEG_TO_RAD).yRot(-yRot * Mth.DEG_TO_RAD));
    }
    
    private float calculateYaw(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float)(Mth.atan2(dz, dx) * (180F / Math.PI)) + 90F;
    }

    private float calculatePitch(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        return -(float)(Mth.atan2(dy, horizontalDistance) * (180F / Math.PI));
    }

    // ─── Walkable Platforms ──────────────────────────────────────
    /** Tile size-to-spacing ratio: step ≤ size/√2 so diagonal neighbours overlap at any yaw.
     *  0.7 leaves a small safety margin under the √2 bound. */
    private static final float TILE_STEP_RATIO = 0.7f;

    @SuppressWarnings("unchecked")
    private final List<HullbackWalkableEntity>[] partTiles = new List[PlatformLayout.MAX_PARTS];
    /** Parallel to {@link #partTiles}: {bodyLocalX, bodyLocalZ, bodyLocalY, rotateWithYaw?1:0, anchorIndex} per tile. */
    @SuppressWarnings("unchecked")
    private final List<float[]>[] partTileLocals = new List[PlatformLayout.MAX_PARTS];

    /** Pivot cache: pre-computed per anchor slot (0..4 = parts, 5 = whale center). */
    private final double[] pivotX = new double[6];
    private final double[] pivotZ = new double[6];
    private final double[] pivotCos = new double[6];
    private final double[] pivotSin = new double[6];
    /** Snapshot of the previous tick's pivot cache for the no-op skip check. */
    private final double[] lastPivotX = new double[6];
    private final double[] lastPivotZ = new double[6];
    private final double[] lastPivotCos = new double[6];
    private final double[] lastPivotSin = new double[6];
    private double lastCenterYBase = Double.NaN;
    private double lastDeltaSq = Double.NaN;
    private boolean snapshotValid = false;
    private static final double STABILITY_EPSILON = 1e-4;

    {
        for (int p = 0; p < PlatformLayout.MAX_PARTS; p++) {
            partTiles[p] = new ArrayList<>();
            partTileLocals[p] = new ArrayList<>();
        }
    }

    public List<HullbackWalkableEntity> tilesFor(int part) {
        if (part < 0 || part >= PlatformLayout.MAX_PARTS) return List.of();
        return partTiles[part];
    }

    public boolean hasPlatform(int part) {
        return part >= 0 && part < PlatformLayout.MAX_PARTS && !partTiles[part].isEmpty();
    }

    /** Tile centers so outermost edges land on ±width/2 and step ≤ tile size × {@link #TILE_STEP_RATIO}. */
    private static int tileCount(float width, float tileSize) {
        if (width <= tileSize) return 1;
        return (int) Math.ceil((width - tileSize) / (tileSize * TILE_STEP_RATIO)) + 1;
    }

    public void discardAllPlatforms() {
        for (int p = 0; p < PlatformLayout.MAX_PARTS; p++) {
            for (HullbackWalkableEntity t : partTiles[p]) t.discard();
            partTiles[p].clear();
            partTileLocals[p].clear();
        }
    }

    public boolean spawnPlatform(int index) {
        if (hullback.isDeadOrDying() || index < 0 || index >= PlatformLayout.MAX_PARTS) return false;
        List<HullbackWalkableEntity> tiles = partTiles[index];
        List<float[]> locals = partTileLocals[index];
        for (HullbackWalkableEntity t : tiles) t.discard();
        tiles.clear();
        locals.clear();

        PlatformLayout.PlatformDef def = platformLayout.getPlatform(index);
        float height = def.height();
        double cx = subEntities[index].getX();
        double cy = hullback.position().y + def.yOffset();
        double cz = subEntities[index].getZ();

        // Legacy AABB: spawned either implicitly (wild Hullback / part with no shapes & no length)
        // or explicitly via "legacy_aabb": true (hybrid mode — coexists with tile shapes below).
        if (def.shouldSpawnLegacyAabb()) {
            HullbackWalkableEntity tile = new HullbackWalkableEntity(WBEntityRegistry.HULLBACK_PLATFORM.get(), hullback.level());
            tile.applyDimensions(def.width(), height);
            tile.setPos(cx + def.xOffset(), cy, cz + def.zOffset());
            if (hullback.level().addFreshEntity(tile)) {
                tiles.add(tile);
                locals.add(new float[] { def.xOffset(), def.zOffset(), 0f, /* rotate */ 0f, PlatformLayout.ANCHOR_SELF });
            }
        }

        // Tile shapes: from explicit shapes[] array, or from legacy width/length when length is
        // declared (no shapes). When shouldSpawnLegacyAabb already covered the wild-default case,
        // there's nothing to tile.
        List<PlatformLayout.ShapeDef> shapesToTile;
        boolean hasShapes = def.shapes() != null && !def.shapes().isEmpty();
        if (hasShapes) {
            shapesToTile = def.shapes();
        } else if (def.length() >= 0) {
            shapesToTile = List.of(new PlatformLayout.ShapeDef(
                    def.xOffset(), 0f, def.zOffset(), def.width(), def.length(),
                    /* rotate */ true, PlatformLayout.ANCHOR_SELF, PlatformLayout.TILE_SIZE_DEFAULT));
        } else {
            shapesToTile = List.of();
        }

        for (PlatformLayout.ShapeDef s : shapesToTile) {
            float length = s.length() > 0 ? s.length() : s.width();
            // No tile_size and no length explicit → single AABB (sized to fit the whole shape).
            // The user opted out of tiling implicitly; spawn one entity instead of a grid.
            float tileSize;
            if (s.tileSize() > 0) tileSize = s.tileSize();
            else if (s.length() < 0) tileSize = Math.max(s.width(), length);
            else tileSize = PlatformLayout.DEFAULT_TILE_SIZE;
            float tileHalf = tileSize * 0.5f;
            int nx = tileCount(s.width(), tileSize);
            int nz = tileCount(length, tileSize);
            float stepX = nx > 1 ? (s.width() - tileSize) / (nx - 1) : 0f;
            float stepZ = nz > 1 ? (length - tileSize) / (nz - 1) : 0f;
            float startX = nx > 1 ? -s.width() * 0.5f + tileHalf : 0f;
            float startZ = nz > 1 ? -length * 0.5f + tileHalf : 0f;
            float rotFlag = s.rotateWithYaw() ? 1f : 0f;

            for (int i = 0; i < nx; i++) {
                for (int j = 0; j < nz; j++) {
                    float lx = startX + i * stepX + s.dx();
                    float lz = startZ + j * stepZ + s.dz();
                    HullbackWalkableEntity tile = new HullbackWalkableEntity(WBEntityRegistry.HULLBACK_PLATFORM.get(), hullback.level());
                    tile.applyDimensions(tileSize, height);
                    tile.setPos(cx, cy + s.dy(), cz);
                    if (hullback.level().addFreshEntity(tile)) {
                        tiles.add(tile);
                        locals.add(new float[] { lx, lz, s.dy(), rotFlag, s.anchorPart() });
                    }
                }
            }
        }
        return !tiles.isEmpty();
    }

    public void updateStationaryPlatforms(float currentPlatformHeight, Vec3 deltaMovement) {
        // Resolve every possible pivot (5 parts + whale center) once per tick. Slots 0..4 =
        // nose/head/body/tail/fluke. Slot 5 = whale center. Per-tile cost drops to a slot lookup.
        for (int i = 0; i < 5 && i < subEntities.length; i++) {
            pivotX[i] = subEntities[i].getX();
            pivotZ[i] = subEntities[i].getZ();
            double yawRad = Math.toRadians(partYRot != null && i < partYRot.length ? partYRot[i] : 0f);
            pivotCos[i] = Math.cos(yawRad);
            pivotSin[i] = Math.sin(yawRad);
        }
        pivotX[5] = hullback.getX();
        pivotZ[5] = hullback.getZ();
        double whaleYawRad = Math.toRadians(hullback.getYRot());
        pivotCos[5] = Math.cos(whaleYawRad);
        pivotSin[5] = Math.sin(whaleYawRad);
        double centerYBase = hullback.getY() + currentPlatformHeight;
        double deltaSq = deltaMovement.x * deltaMovement.x + deltaMovement.z * deltaMovement.z;

        // Stable-skip: if every pivot, centerY base, and delta are within epsilon of the previous
        // tick, every tile would land on the exact same world position it already occupies — the
        // moveTo + setDeltaMovement loop is a no-op. Skip it.
        boolean stable = snapshotValid
                && Math.abs(centerYBase - lastCenterYBase) <= STABILITY_EPSILON
                && Math.abs(deltaSq - lastDeltaSq) <= STABILITY_EPSILON;
        for (int i = 0; i < 6 && stable; i++) {
            if (Math.abs(pivotX[i] - lastPivotX[i]) > STABILITY_EPSILON
                    || Math.abs(pivotZ[i] - lastPivotZ[i]) > STABILITY_EPSILON
                    || Math.abs(pivotCos[i] - lastPivotCos[i]) > STABILITY_EPSILON
                    || Math.abs(pivotSin[i] - lastPivotSin[i]) > STABILITY_EPSILON) {
                stable = false;
            }
        }
        System.arraycopy(pivotX, 0, lastPivotX, 0, 6);
        System.arraycopy(pivotZ, 0, lastPivotZ, 0, 6);
        System.arraycopy(pivotCos, 0, lastPivotCos, 0, 6);
        System.arraycopy(pivotSin, 0, lastPivotSin, 0, 6);
        lastCenterYBase = centerYBase;
        lastDeltaSq = deltaSq;
        snapshotValid = true;
        if (stable) return;

        for (int part = 0; part < PlatformLayout.MAX_PARTS; part++) {
            List<HullbackWalkableEntity> tiles = partTiles[part];
            if (tiles.isEmpty()) continue;
            List<float[]> locals = partTileLocals[part];
            PlatformLayout.PlatformDef d = platformLayout.getPlatform(part);
            double centerY = centerYBase + (d.yOffset() - PlatformLayout.DEFAULT_Y_OFFSET);

            for (int i = 0; i < tiles.size(); i++) {
                HullbackWalkableEntity tile = tiles.get(i);
                float[] L = locals.get(i);
                float lx = L[0], lz = L[1], ly = L[2];
                boolean rotates = L[3] > 0.5f;
                int anchorIdx = (int) L[4];
                int slot;
                if (anchorIdx == PlatformLayout.ANCHOR_WHALE) slot = 5;
                else if (anchorIdx == PlatformLayout.ANCHOR_SELF) slot = part;
                else slot = (anchorIdx >= 0 && anchorIdx < 5) ? anchorIdx : part;

                double worldDx, worldDz;
                if (rotates) {
                    double cos = pivotCos[slot], sin = pivotSin[slot];
                    worldDx = lx * cos - lz * sin;
                    worldDz = lx * sin + lz * cos;
                } else {
                    worldDx = lx;
                    worldDz = lz;
                }
                tile.moveTo(pivotX[slot] + worldDx, centerY + ly, pivotZ[slot] + worldDz);
                tile.setDeltaMovement(deltaMovement);
            }
        }
    }

    public void moveEntitiesOnTop(int index, boolean platformsStable) {
        HullbackPartEntity part = subEntities[index];
        Vec3 offset = partPosition[index].subtract(oldPartPosition[index]);

        // Increased threshold to avoid micro-movements
        if (offset.length() <= MOVE_ENTITIES_THRESHOLD) return;

        // If platforms are not stable, reduce movement
        float movementFactor = platformsStable ? 1.0F : UNSTABLE_PLATFORM_FACTOR;

        for (net.minecraft.world.entity.Entity entity : hullback.level().getEntities(part, part.getBoundingBox().inflate(0F, 0.01F, 0F), net.minecraft.world.entity.EntitySelector.NO_SPECTATORS.and((entity) -> (!entity.isPassenger())))) {
            if (!entity.noPhysics && !(entity instanceof HullbackPartEntity) && !(entity instanceof HullbackEntity) && !(entity instanceof HullbackWalkableEntity)) {
                
                // Smooth movement for players
                if (entity instanceof net.minecraft.world.entity.player.Player) {
                    movementFactor *= PLAYER_SMOOTH_FACTOR;
                }

                double gravity = entity.isNoGravity() ? 0 : 0.08D;
                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    net.minecraft.world.entity.ai.attributes.AttributeInstance attribute = living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
                    gravity = attribute.getValue();
                }
                
                Vec3 smoothedOffset = offset.scale(movementFactor);
                
                // Use smoothed offset
                float f2 = 1.0F; 
                entity.move(net.minecraft.world.entity.MoverType.SHULKER, new Vec3((double) (f2 * (float) smoothedOffset.x), (double) (f2 * (float) smoothedOffset.y), (double) (f2 * (float) smoothedOffset.z)));
                entity.hurtMarked = true;
            }
        }
    }
}
