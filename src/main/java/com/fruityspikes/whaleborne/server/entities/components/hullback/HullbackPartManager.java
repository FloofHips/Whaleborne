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
import java.util.Arrays;

/**
 * Manages the multipart body segments and walkable platform entities for the Hullback.
 *
 * Positions all sub-entities (nose, head, body, tail, fluke) relative to the whale,
 * computes per-part yaw/pitch rotations for smooth body articulation, calculates
 * seat anchor points for riding entities, and handles spawning/discarding of
 * walkable platform entities when stationary.
 *
 * Updated every tick via manageMultipartPhysics() in HullbackEntity.tick().
 */
public class HullbackPartManager {
    private final HullbackEntity hullback;
    public final HullbackPartEntity[] subEntities;

    // ─── Constants ────────────────────────────────────────────────
    private static final int PART_COUNT = 5;
    private static final int SEAT_COUNT = 7;
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

    // Seats
    public final Vec3[] seats = new Vec3[SEAT_COUNT];
    public final Vec3[] oldSeats = new Vec3[SEAT_COUNT];
    private Vec3 smoothedSeat6 = null;
    private Vec3 rawSeat6 = null;
    private static final float SEAT6_SMOOTH_FACTOR = 0.35f;

    private static final Vec3[] SEAT_OFFSETS = {
            new Vec3(0, 5.5f, 0.0), //sail
            new Vec3(0, 5.5f, -3.0), //captain
            new Vec3(1.5, 5.5f, 0.3),
            new Vec3(-1.5, 5.5f, 0.3),
            new Vec3(1.5, 5.5f, -1.75),
            new Vec3(-1.5, 5.5f, -1.75),
            new Vec3(0, 1.6f, -0.8) //fluke
    };

    private static final float[] PART_DRAG_FACTORS = {1f, 0.9f, 0.2f, 0.1f, 0.09f};
    private static final Vec3[] BASE_OFFSETS = {
            new Vec3(0, 0, 6),      // Nose
            new Vec3(0, 0, 2.5),    // Head
            new Vec3(0, 0, -2.25),  // Body
            new Vec3(0, 0, -7),     // Tail
            new Vec3(0, 0, -11)     // Fluke
    };
    private static final double[] MAX_DIST = {10.0, 3.55, 4.8, 4.8, 4.1};

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
        // Work array — recomputed each tick from immutable BASE_OFFSETS
        Vec3[] offsets = new Vec3[BASE_OFFSETS.length];
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
    }
    
    // ─── Seat-to-part mapping ──────────────────────────────────
    // Each entry: [partIndex for position, partIndex for rotation]
    private static final int[][] SEAT_PART_MAP = {
            {0, 1}, // seat 0 (sail) — position from nose[0], rotation from head[1]
            {0, 1}, // seat 1 (captain)
            {2, 2}, // seat 2 — body
            {2, 2}, // seat 3 — body
            {2, 2}, // seat 4 — body
            {2, 2}, // seat 5 — body
            {4, 4}, // seat 6 (fluke)
    };

    public void calculateSeats() {
        if (partPosition == null || partYRot == null || partXRot == null || partPosition[0] == null) return;

        // Current seats
        for (int i = 0; i < SEAT_COUNT - 1; i++) {
            int posIdx = SEAT_PART_MAP[i][0];
            int rotIdx = SEAT_PART_MAP[i][1];
            seats[i] = computeSeat(partPosition[posIdx], partXRot[rotIdx], partYRot[rotIdx], i);
        }

        // Seat 6 (fluke) — raw position stored for widgets, smoothed for players/mobs
        rawSeat6 = computeSeat(partPosition[4], partXRot[4], partYRot[4], 6);
        if (smoothedSeat6 == null) {
            smoothedSeat6 = rawSeat6;
        } else {
            smoothedSeat6 = new Vec3(
                    Mth.lerp(SEAT6_SMOOTH_FACTOR, smoothedSeat6.x, rawSeat6.x),
                    Mth.lerp(SEAT6_SMOOTH_FACTOR, smoothedSeat6.y, rawSeat6.y),
                    Mth.lerp(SEAT6_SMOOTH_FACTOR, smoothedSeat6.z, rawSeat6.z)
            );
        }
        seats[6] = smoothedSeat6;

        // Old seats (for interpolation)
        for (int i = 0; i < SEAT_COUNT - 1; i++) {
            int posIdx = SEAT_PART_MAP[i][0];
            int rotIdx = SEAT_PART_MAP[i][1];
            oldSeats[i] = computeSeat(oldPartPosition[posIdx], oldPartXRot[rotIdx], oldPartYRot[rotIdx], i);
        }
        oldSeats[6] = computeSeat(oldPartPosition[4], oldPartXRot[4], oldPartYRot[4], 6);
    }

    /** Returns the raw (unsmoothed) seat 6 position for rigid widget attachment. */
    public Vec3 getRawSeat6() {
        return rawSeat6;
    }

    /** Computes a seat world position from a part position, rotation, and seat offset index. */
    private Vec3 computeSeat(Vec3 pos, float xRot, float yRot, int seatIndex) {
        return pos.add(SEAT_OFFSETS[seatIndex].xRot(xRot * Mth.DEG_TO_RAD).yRot(-yRot * Mth.DEG_TO_RAD));
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
    public HullbackWalkableEntity moving_head;
    public HullbackWalkableEntity moving_nose;
    public HullbackWalkableEntity moving_body;

    public void discardAllPlatforms() {
        if (this.moving_nose != null) { this.moving_nose.discard(); this.moving_nose = null; }
        if (this.moving_head != null) { this.moving_head.discard(); this.moving_head = null; }
        if (this.moving_body != null) { this.moving_body.discard(); this.moving_body = null; }
    }

    public HullbackWalkableEntity spawnPlatform(int index) {
        if (!hullback.isDeadOrDying()) {
            HullbackWalkableEntity part = new HullbackWalkableEntity(WBEntityRegistry.HULLBACK_PLATFORM.get(), hullback.level());
            part.setPos(subEntities[index].getX(), hullback.position().y + 4.7, subEntities[index].getZ());
            if (hullback.level().addFreshEntity(part)) {
                return part;
            }
        }
        return null;
    }

    public void updateStationaryPlatforms(float currentPlatformHeight, Vec3 deltaMovement) {
        if (this.moving_nose != null) {
            this.moving_nose.moveTo(this.subEntities[0].getX(), hullback.getY() + currentPlatformHeight, this.subEntities[0].getZ());
            this.moving_nose.setDeltaMovement(deltaMovement);
        }
        if (this.moving_head != null) {
            this.moving_head.moveTo(this.subEntities[1].getX(), hullback.getY() + currentPlatformHeight, this.subEntities[1].getZ());
            this.moving_head.setDeltaMovement(deltaMovement);
        }
        if (this.moving_body != null) {
            this.moving_body.moveTo(this.subEntities[2].getX(), hullback.getY() + currentPlatformHeight, this.subEntities[2].getZ());
            this.moving_body.setDeltaMovement(deltaMovement);
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
