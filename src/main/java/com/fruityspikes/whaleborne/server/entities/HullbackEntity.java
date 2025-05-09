package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;

public class HullbackEntity extends WaterAnimal {// implements HasCustomInventoryScreen, OwnableEntity, PlayerRideableJumping, Saddleable {
    private float leftEyeYaw, rightEyeYaw, eyePitch;
    public final HullbackPartEntity head;
    public final HullbackPartEntity nose;
    public final HullbackPartEntity body;
    public final HullbackPartEntity tail;
    public final HullbackPartEntity fluke;
    private final HullbackPartEntity[] subEntities;
    private final float[] partDragFactors;
    private Vec3[] prevPartPositions;
    private float mouthOpenProgress;
    private boolean isOpening;
    public HullbackEntity(EntityType<? extends WaterAnimal> entityType, Level level) {
        super(entityType, level);

        this.moveControl = new SmoothSwimmingMoveControl(this, 1, 1, 0.02F, 0.1F, true);
        //this.moveControl = new HullbackMoveControl(this);
        this.lookControl = new SmoothSwimmingLookControl(this, 180);

        this.nose = new HullbackPartEntity(this, "nose", 5.0F, 5.0F, new Vec3(0, 0, 6));
        this.head = new HullbackPartEntity(this, "head", 5.0F, 5.0F, new Vec3(0, 0, 2.5));
        this.body = new HullbackPartEntity(this, "body", 5.0F, 5.0F, new Vec3(0, 0, -3));
        this.tail = new HullbackPartEntity(this, "tail", 2.5F, 2.5F, new Vec3(0, 0, -7));
        this.fluke = new HullbackPartEntity(this, "fluke", 4.0F, 1F, new Vec3(0, 0, -11));

        this.subEntities = new HullbackPartEntity[]{this.nose, this.head, this.body, this.tail, this.fluke};
        this.setId(ENTITY_COUNTER.getAndAdd(this.subEntities.length + 1) + 1);

        this.partDragFactors = new float[]{1f, 0.9f, 0.5f, 0.1f, 0.07f};
        this.prevPartPositions = new Vec3[5];
        this.mouthOpenProgress = 0.0f;
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 1.2000000476837158).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    public float getLeftEyeYaw() { return leftEyeYaw; }
    public float getRightEyeYaw() { return rightEyeYaw; }
    public float getEyePitch() { return eyePitch; }

    public void setLeftEyeYaw(float yaw) { this.leftEyeYaw = Mth.wrapDegrees(yaw); }
    public void setRightEyeYaw(float yaw) { this.rightEyeYaw = Mth.wrapDegrees(yaw); }
    public void setEyePitch(float pitch) { this.eyePitch = Mth.wrapDegrees(pitch); }

    public void setId(int p_20235_) {
        super.setId(p_20235_);

        for(int i = 0; i < this.subEntities.length; ++i) {
            this.subEntities[i].setId(p_20235_ + i + 1);
        }

    }
    public HullbackPartEntity[] getSubEntities() {
        return this.subEntities;
    }
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    public boolean isPickable() {
        return false;
    }


    public boolean isMultipartEntity() {
        return true;
    }

    public PartEntity<?>[] getParts() {
        return this.subEntities;
    }

    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreathAirGoal(this));
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(4, new HullbackRandomSwimGoal(this, 0.8, 10));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(1, new HullbackApproachPlayerGoal(this, 0.4f));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.2000000476837158, true));
        this.goalSelector.addGoal(8, new FollowBoatGoal(this));
        this.goalSelector.addGoal(9, new AvoidEntityGoal(this, Guardian.class, 8.0F, 1.0, 1.0));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, new Class[]{Guardian.class})).setAlertOthers(new Class[0]));

//        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
//        this.goalSelector.addGoal(1, new RandomSwimmingGoal(this, 1.0, 40));
//        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
//        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        updatePartPositions();
        updateMouthOpening();
    }

    private void updateMouthOpening() {
        if(isOpening){
            mouthOpenProgress+= 0.1f;
            if(mouthOpenProgress>=1)
                isOpening=false;
        }
        else{
            mouthOpenProgress-= 0.05f;
            if(mouthOpenProgress<=0)
                isOpening=true;
        }
    }

    public float getMouthOpenProgress() {
        return mouthOpenProgress;
    }

    private void updatePartPositions() {
        Vec3[] baseOffsets = {
                new Vec3(0, 0, 6),   // Nose
                new Vec3(0, 0, 2.5), // Head
                new Vec3(0, 0, -3),  // Body
                new Vec3(0, 0, -7),  // Tail base
                new Vec3(0, 0, -11)  // Fluke tip
        };

        if (prevPartPositions[0] == null) {
            prevPartPositions = Arrays.copyOf(baseOffsets, baseOffsets.length);
        }

        float swimCycle = (float) (Mth.sin(this.tickCount * 0.1f) * this.getDeltaMovement().length());
        float yawRad = -this.getYRot() * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;

        // Calculate world positions with entity rotation
        for (int i = 0; i < baseOffsets.length; i++) {
            Vec3 rotatedOffset = baseOffsets[i]
                    .yRot(yawRad)
                    .xRot(pitchRad);

            Vec3 targetPos = this.position().add(rotatedOffset);

            // Apply smooth movement with part-specific drag factors
            if (i > 0) {
                prevPartPositions[i] = new Vec3(
                        Mth.lerp(partDragFactors[i], prevPartPositions[i].x, targetPos.x),
                        Mth.lerp(partDragFactors[i], prevPartPositions[i].y, targetPos.y),
                        Mth.lerp(partDragFactors[i], prevPartPositions[i].z, targetPos.z)
                );
            } else {
                prevPartPositions[i] = targetPos;
            }
        }

        // Nose and Head - basic following
        this.nose.moveTo(prevPartPositions[0].x, prevPartPositions[0].y, prevPartPositions[0].z,
                //getYRot(),
                //getXRot());
                calculateYaw(prevPartPositions[0], prevPartPositions[1]),
                calculatePitch(prevPartPositions[0], prevPartPositions[1]));

        this.head.moveTo(prevPartPositions[1].x, prevPartPositions[1].y + swimCycle * 2, prevPartPositions[1].z,
                calculateYaw(prevPartPositions[0], prevPartPositions[1]),
                calculatePitch(prevPartPositions[0], prevPartPositions[1]));

        // Body - subtle undulation
        this.body.moveTo(prevPartPositions[2].x,
                prevPartPositions[2].y + swimCycle * 2,
                prevPartPositions[2].z,
                calculateYaw(prevPartPositions[1], prevPartPositions[2]),
                calculatePitch(prevPartPositions[1], prevPartPositions[2]));

        this.tail.moveTo(prevPartPositions[3].x,
                prevPartPositions[3].y + swimCycle * 8,
                prevPartPositions[3].z,
                calculateYaw(prevPartPositions[2], prevPartPositions[3]),
                calculatePitch(prevPartPositions[2], prevPartPositions[3]) * 3f - swimCycle * 20f);

        float flukeDistance = 4.0f;
        Vec3 flukeOffset = new Vec3(0, 0, -flukeDistance)
                .yRot(-tail.getYRot() * Mth.DEG_TO_RAD)
                .xRot(tail.getXRot() * Mth.DEG_TO_RAD);

        Vec3 flukeTarget = new Vec3(
                tail.getX() + flukeOffset.x,
                tail.getY() + flukeOffset.y + swimCycle * 10,
                tail.getZ() + flukeOffset.z
        );

        float flukeYaw = calculateYaw(tail.position(), flukeTarget);
        float flukePitch = calculatePitch(tail.position(), flukeTarget);

        this.fluke.moveTo(
                flukeTarget.x,
                flukeTarget.y,
                flukeTarget.z,
                flukeYaw,
                (flukePitch * 1.5f + swimCycle * 30f)
        );
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
//    @Override
//    public void openCustomInventoryScreen(Player player) {
//
//    }
//
//    @Nullable
//    @Override
//    public UUID getOwnerUUID() {
//        return null;
//    }
//
//    @Override
//    public void onPlayerJump(int i) {
//
//    }
//
//    @Override
//    public boolean canJump() {
//        return false;
//    }
//
//    @Override
//    public void handleStartJump(int i) {
//
//    }
//
//    @Override
//    public void handleStopJump() {
//
//    }
//
//    @Override
//    public boolean isSaddleable() {
//        return false;
//    }
//
//    @Override
//    public void equipSaddle(@Nullable SoundSource soundSource) {
//
//    }
//
//    @Override
//    public boolean isSaddled() {
//        return false;
//    }

    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
            }
        } else {
            super.travel(travelVector);
        }

    }
    protected BodyRotationControl createBodyControl() {
        return new HullbackBodyRotationControl(this);
    }

    class HullbackMoveControl extends MoveControl {
        private static final float WHALE_TURN_SPEED = 1.5F; // Degrees per tick (much slower than default)
        private static final float WHALE_PITCH_SPEED = 0.8F; // Degrees per tick
        private static final float MAX_PITCH_ANGLE = 30.0F; // Maximum dive/ascend angle
        private static final float BUOYANCY_FORCE = 0.002F; // Gentle upward force

        private final HullbackEntity whale;
        private float targetYaw;
        private float targetPitch;

        public HullbackMoveControl(HullbackEntity whale) {
            super(whale);
            this.whale = whale;
            this.targetYaw = whale.getYRot();
            this.targetPitch = whale.getXRot();
        }

        @Override
        public void tick() {
            // Apply gentle buoyancy constantly
            if (whale.isInWater()) {
                whale.setDeltaMovement(whale.getDeltaMovement().add(0, BUOYANCY_FORCE, 0));
            }

            if (this.operation == Operation.MOVE_TO) {
                // Calculate direction to target
                double dx = this.wantedX - whale.getX();
                double dy = this.wantedY - whale.getY();
                double dz = this.wantedZ - whale.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);

                if (distance > 0.1) { // Only adjust if we have meaningful distance
                    // Calculate desired angles
                    this.targetYaw = (float)(Mth.atan2(dz, dx) * (180F / (float)Math.PI) - 90F);
                    this.targetPitch = (float)(-Mth.atan2(dy, distance) * (180F / (float)Math.PI));
                    this.targetPitch = Mth.clamp(this.targetPitch, -MAX_PITCH_ANGLE, MAX_PITCH_ANGLE);

                    // Apply extremely gradual rotation
                    whale.setYRot(this.newrotlerp(whale.getYRot(), targetYaw, WHALE_TURN_SPEED));
                    whale.setXRot(this.newrotlerp(whale.getXRot(), targetPitch, WHALE_PITCH_SPEED));

                    // Calculate movement direction
                    float speed = (float)(this.speedModifier * whale.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    float adjustedSpeed = speed * 0.1F; // Reduce speed for more whale-like movement

                    // Apply movement in facing direction
                    float f4 = Mth.cos(whale.getXRot() * ((float)Math.PI / 180F));
                    float f5 = Mth.sin(whale.getXRot() * ((float)Math.PI / 180F));
                    whale.zza = f4 * adjustedSpeed;
                    whale.yya = -f5 * adjustedSpeed;

                    // Gentle side-to-side sway for more natural movement
                    whale.xxa = Mth.sin(whale.tickCount * 0.1F) * 0.05F * adjustedSpeed;
                } else {
                    // Arrived at destination - gentle drift
                    whale.setSpeed(0);
                    whale.setZza(0);
                    whale.setYya(0);
                    whale.setXxa(0);
                }
            } else {
                // Idle floating behavior
                whale.setSpeed(0);
                whale.setZza(0);
                whale.setYya(0);
                whale.setXxa(0);

                // Gentle swaying while idle
                if (whale.tickCount % 40 == 0) {
                    this.targetYaw += (whale.getRandom().nextFloat() - 0.5F) * 30F;
                    this.targetPitch += (whale.getRandom().nextFloat() - 0.5F) * 10F;
                    this.targetPitch = Mth.clamp(this.targetPitch, -15F, 15F);
                }

                whale.setYRot(this.newrotlerp(whale.getYRot(), targetYaw, WHALE_TURN_SPEED * 0.3F));
                whale.setXRot(this.newrotlerp(whale.getXRot(), targetPitch, WHALE_PITCH_SPEED * 0.3F));
            }
        }

        // Custom rotation interpolation that's even smoother than vanilla
        float newrotlerp(float current, float target, float maxStep) {
            float delta = Mth.wrapDegrees(target - current);
            delta = Mth.clamp(delta, -maxStep, maxStep);
            return current + delta * 0.2F; // Additional smoothing factor
        }
    }

    class HullbackBodyRotationControl extends BodyRotationControl {
        public HullbackBodyRotationControl(HullbackEntity hullBack) {
            super(hullBack);
        }

        public void clientTick() {
            HullbackEntity.this.setYBodyRot(HullbackEntity.this.getYRot());
        }
    }

    class HullbackLookControl extends LookControl {
        private final HullbackEntity hullback;
        private static final float EYE_SIDEWAYS_ANGLE = 90F;

        public HullbackLookControl(HullbackEntity mob) {
            super(mob);
            this.hullback = mob;
        }

        @Override
        public void tick() {
            if (this.lookAtCooldown > 0) {
                this.lookAtCooldown--;

                double dx = this.wantedX - this.mob.getX();
                double dz = this.wantedZ - this.mob.getZ();
                double dy = this.wantedY - this.mob.getEyeY();
                double distanceXZ = Math.sqrt(dx * dx + dz * dz);

                float pitch = (float) -Math.toDegrees(Math.atan2(dy, distanceXZ));
                hullback.setEyePitch(pitch);

                float bodyYaw = this.mob.getYRot();
                float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
                float angleToTarget = Mth.wrapDegrees(targetYaw - bodyYaw);

                float leftEyeYaw = bodyYaw - EYE_SIDEWAYS_ANGLE;
                float rightEyeYaw = bodyYaw + EYE_SIDEWAYS_ANGLE;

                if (angleToTarget > 0) {
                    rightEyeYaw += Mth.clamp(angleToTarget * 0.5F, 0, 45F);
                } else {
                    leftEyeYaw += Mth.clamp(angleToTarget * 0.5F, -45F, 0);
                }

                hullback.setLeftEyeYaw(leftEyeYaw);
                hullback.setRightEyeYaw(rightEyeYaw);
            } else {
                hullback.setLeftEyeYaw(this.mob.getYRot() - EYE_SIDEWAYS_ANGLE);
                hullback.setRightEyeYaw(this.mob.getYRot() + EYE_SIDEWAYS_ANGLE);
                hullback.setEyePitch(0);
            }
        }
    }

    class HullbackRandomSwimGoal extends RandomSwimmingGoal{
        public HullbackRandomSwimGoal(PathfinderMob p_25753_, double p_25754_, int p_25755_) {
            super(p_25753_, p_25754_, p_25755_);
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            return BehaviorUtils.getRandomSwimmablePos(this.mob, 50, 20);
        }
    }
    public class HullbackApproachPlayerGoal extends Goal {
        private static final float APPROACH_DISTANCE = 8.0f;
        private static final float SIDE_OFFSET = 5.0f;
        private static final float ROTATION_SPEED = 0.8f; // Degrees per tick

        private final HullbackEntity whale;
        private final float speedModifier;
        private Player targetPlayer;
        private int repositionCooldown;
        private boolean approachFromRight;

        public HullbackApproachPlayerGoal(HullbackEntity whale, float speedModifier) {
            this.whale = whale;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.repositionCooldown > 0) {
                this.repositionCooldown--;
                return false;
            }

            this.targetPlayer = this.whale.level().getNearestPlayer(this.whale, 15.0);
            if (this.targetPlayer == null) return false;

            // Alternate approach sides for variety
            this.approachFromRight = whale.getRandom().nextBoolean();
            this.repositionCooldown = 200 + whale.getRandom().nextInt(200);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetPlayer != null
                    && this.targetPlayer.isAlive()
                    && this.whale.distanceToSqr(this.targetPlayer) < 225.0; // 15^2
        }

        @Override
        public void start() {
            super.start();
            playSound(SoundEvents.AMETHYST_BLOCK_BREAK);
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            this.whale.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.targetPlayer == null) return;

            // 1. Calculate ideal position (perpendicular to player's view)
            Vec3 playerLook = this.targetPlayer.getViewVector(1.0f);
            Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();

            // Flip offset side based on approach direction
            Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
            Vec3 targetPos = this.targetPlayer.position()
                    .add(sideOffset)
                    .add(playerLook.scale(-APPROACH_DISTANCE));

            // 2. Move to position
            this.whale.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, this.speedModifier);

            // 3. Smooth rotation toward ideal angle
            Vec3 toPlayer = this.targetPlayer.position().subtract(this.whale.position());
            float desiredYaw = (float)Math.toDegrees(Math.atan2(toPlayer.z, toPlayer.x)) - 90f;

            // Calculate which side we want the player on (left or right)
            float angleToPlayer = Mth.wrapDegrees(desiredYaw - this.whale.getYRot());
            boolean shouldBeOnRight = angleToPlayer > 0;

            // Adjust desired yaw to put player at 90° to our side
            float sideAwareYaw = desiredYaw + (approachFromRight ? -90f : 90f);

            // Gradual rotation
            float newYRot = Mth.rotLerp(0.05f, this.whale.getYRot(), sideAwareYaw);
            this.whale.setYRot(newYRot);
            this.whale.yBodyRot = newYRot; // Sync body rotation

            // 4. Eye tracking
            float eyeBaseAngle = approachFromRight ? 90f : -90f;
            float eyeFollowAmount = Mth.clamp(Math.abs(angleToPlayer) * 0.5f, 0, 45f);

            if (approachFromRight) {
                this.whale.setRightEyeYaw(eyeBaseAngle + eyeFollowAmount);
                this.whale.setLeftEyeYaw(-90f);
            } else {
                this.whale.setLeftEyeYaw(eyeBaseAngle - eyeFollowAmount);
                this.whale.setRightEyeYaw(90f);
            }

            // Vertical eye tracking
            double yDiff = this.targetPlayer.getEyeY() - this.whale.getEyeY();
            double horizontalDist = new Vec3(toPlayer.x, 0, toPlayer.z).length();
            float pitch = (float)-Math.toDegrees(Math.atan2(yDiff, horizontalDist));
            this.whale.setEyePitch(pitch);
        }
    }
}
