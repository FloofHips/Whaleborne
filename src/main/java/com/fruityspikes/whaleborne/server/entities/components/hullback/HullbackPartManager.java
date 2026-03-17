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
    
    // Position/Rotation Arrays
    public final Vec3[] prevPartPositions = new Vec3[5];
    public final Vec3[] partPosition = new Vec3[5];
    public final float[] partYRot = new float[5];
    public final float[] partXRot = new float[5];
    public final Vec3[] oldPartPosition = new Vec3[5];
    public final float[] oldPartYRot = new float[5];
    public final float[] oldPartXRot = new float[5];
    
    // Seats
    public final Vec3[] seats = new Vec3[7];
    public final Vec3[] oldSeats = new Vec3[7];

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

    public void init() {
         // Initial allocation logic if needed, currently empty as arrays are final
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
             swimCycle = Mth.sin(hullback.tickCount * 0.1f) * horizontalSpeed;
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

        this.partPosition[1] = new Vec3(prevPartPositions[1].x, prevPartPositions[1].y + swimCycle * 2, prevPartPositions[1].z);
        this.partYRot[1] = calculateYaw(prevPartPositions[0], prevPartPositions[1]);
        this.partXRot[1] = calculatePitch(prevPartPositions[0], prevPartPositions[1]);
        subEntities[1].moveTo(prevPartPositions[1].x, prevPartPositions[1].y + swimCycle * 2, prevPartPositions[1].z,
                partYRot[1],
                partXRot[1]);

        this.partPosition[2] = new Vec3(prevPartPositions[2].x, prevPartPositions[2].y + swimCycle * 2, prevPartPositions[2].z);
        this.partYRot[2] = calculateYaw(prevPartPositions[1], prevPartPositions[2]);
        this.partXRot[2] = calculatePitch(prevPartPositions[1], prevPartPositions[2]);
        subEntities[2].moveTo(prevPartPositions[2].x,
                prevPartPositions[2].y + swimCycle * 2,
                prevPartPositions[2].z,
                partYRot[2],
                partXRot[2]);

        this.partPosition[3] = new Vec3(prevPartPositions[3].x, prevPartPositions[3].y + swimCycle * 8, prevPartPositions[3].z);
        this.partYRot[3] = calculateYaw(prevPartPositions[2], prevPartPositions[3]);
        this.partXRot[3] = calculatePitch(prevPartPositions[2], prevPartPositions[3]) * 1.5f - swimCycle * 20f;
        subEntities[3].moveTo(prevPartPositions[3].x,
                prevPartPositions[3].y + swimCycle * 8,
                prevPartPositions[3].z,
                partYRot[3],
                partXRot[3]);

        float flukeDistance = 4.0f;
        Vec3 flukeOffset = new Vec3(0, 0, -flukeDistance)
                .yRot(-subEntities[3].getYRot() * Mth.DEG_TO_RAD)
                .xRot(subEntities[3].getXRot() * Mth.DEG_TO_RAD);

        Vec3 flukeTarget = new Vec3(
                subEntities[3].getX() + flukeOffset.x,
                subEntities[3].getY() + flukeOffset.y + swimCycle * 5.5,
                subEntities[3].getZ() + flukeOffset.z
        );

        float flukeYaw = calculateYaw(subEntities[3].position(), flukeTarget);
        float flukePitch = calculatePitch(subEntities[3].position(), flukeTarget);

        flukeYaw = Mth.rotLerp(PART_DRAG_FACTORS[4], oldPartYRot[4], flukeYaw);
        float flukeXRot = flukePitch * 1.5f + swimCycle * 30f;

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
    
    public void calculateSeats() {
        if (partPosition == null || partYRot == null || partXRot == null || partPosition[0] == null) return;
        
        seats[0] = partPosition[0].add((SEAT_OFFSETS[0]).xRot(partXRot[1] * Mth.DEG_TO_RAD).yRot(-partYRot[1] * Mth.DEG_TO_RAD));
        seats[1] = partPosition[0].add((SEAT_OFFSETS[1]).xRot(partXRot[1] * Mth.DEG_TO_RAD).yRot(-partYRot[1] * Mth.DEG_TO_RAD));
        seats[2] = partPosition[2].add((SEAT_OFFSETS[2]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
        seats[3] = partPosition[2].add((SEAT_OFFSETS[3]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
        seats[4] = partPosition[2].add((SEAT_OFFSETS[4]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
        seats[5] = partPosition[2].add((SEAT_OFFSETS[5]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
        seats[6] = partPosition[4].add((SEAT_OFFSETS[6]).xRot(partXRot[4] * Mth.DEG_TO_RAD).yRot(-partYRot[4] * Mth.DEG_TO_RAD));

        oldSeats[0] = oldPartPosition[0].add((SEAT_OFFSETS[0]).xRot(oldPartXRot[1] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[1] * Mth.DEG_TO_RAD));
        oldSeats[1] = oldPartPosition[0].add((SEAT_OFFSETS[1]).xRot(oldPartXRot[1] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[1] * Mth.DEG_TO_RAD));
        oldSeats[2] = oldPartPosition[2].add((SEAT_OFFSETS[2]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
        oldSeats[3] = oldPartPosition[2].add((SEAT_OFFSETS[3]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
        oldSeats[4] = oldPartPosition[2].add((SEAT_OFFSETS[4]).xRot(oldPartXRot[2] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[2] * Mth.DEG_TO_RAD));
        oldSeats[5] = oldPartPosition[2].add((SEAT_OFFSETS[5]).xRot(partXRot[2] * Mth.DEG_TO_RAD).yRot(-partYRot[2] * Mth.DEG_TO_RAD));
        oldSeats[6] = oldPartPosition[4].add((SEAT_OFFSETS[6]).xRot(oldPartXRot[4] * Mth.DEG_TO_RAD).yRot(-oldPartYRot[4] * Mth.DEG_TO_RAD));
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
        if (offset.length() <= 0.001) return;
        
        // If platforms are not stable, reduce movement
        float movementFactor = platformsStable ? 1.0F : 0.5F;

        for (net.minecraft.world.entity.Entity entity : hullback.level().getEntities(part, part.getBoundingBox().inflate(0F, 0.01F, 0F), net.minecraft.world.entity.EntitySelector.NO_SPECTATORS.and((entity) -> (!entity.isPassenger())))) {
            if (!entity.noPhysics && !(entity instanceof HullbackPartEntity) && !(entity instanceof HullbackEntity) && !(entity instanceof HullbackWalkableEntity)) {
                
                // Smooth movement for players
                if (entity instanceof net.minecraft.world.entity.player.Player) {
                    movementFactor *= 0.8F; // 20% smoother movement for players
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
