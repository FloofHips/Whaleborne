package com.fruityspikes.whaleborne.server.entities.goals.hullback;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class HullbackRandomSwimGoal extends RandomSwimmingGoal {
    private static final int HORIZONTAL_RANGE = 10;
    private static final int VERTICAL_RANGE = 10;
    private static final float FRONT_ANGLE = 45.0f;
    private static final int STUCK_TIMEOUT = 100;
    private static final double MIN_DISTANCE = 2.0;

    private final HullbackEntity mob;
    private int stuckTimer = 0;
    private Vec3 lastPosition = Vec3.ZERO;
    private Vec3 currentTarget = null;
    // Assuming reducedTickDelay is protected/accessible or we need to use a public method or recalculate. 
    // RandomSwimmingGoal extends RandomStrollGoal which has reducedTickDelay(int). It is protected. So we can access it.

    public HullbackRandomSwimGoal(HullbackEntity mob, double speed, int interval) {
        super(mob, speed, interval);
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (mob.stationaryTicks > 0 || mob.hasAnchorDown() || mob.getControllingPassenger() != null) {
            return false;
        }

        if (!this.forceTrigger) {
            if (this.mob.getNoActionTime() >= 100) {
                return false;
            }
            if (this.mob.getRandom().nextInt(60) != 0) {
                return false;
            }
        }

        Vec3 vec3 = this.getPosition();
        if (vec3 == null) {
            return false;
        }

        this.wantedX = vec3.x;
        this.wantedY = vec3.y;
        this.wantedZ = vec3.z;
        this.forceTrigger = false;
        this.currentTarget = getPosition(); // Keep this line from original logic
        this.stuckTimer = 0; // Keep this line from original logic
        this.lastPosition = mob.position(); // Keep this line from original logic
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.stationaryTicks > 0 || mob.hasAnchorDown() || mob.getControllingPassenger() != null) {
            return false;
        }

        Vec3 currentPos = mob.position();
        if (currentPos.distanceTo(lastPosition) < 0.5) {
            stuckTimer++;
        } else {
            stuckTimer = 0;
        }
        lastPosition = currentPos;

        return stuckTimer < STUCK_TIMEOUT &&
                currentPos.distanceTo(currentTarget) > MIN_DISTANCE &&
                !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (currentTarget != null) {
            mob.getNavigation().moveTo(currentTarget.x, currentTarget.y, currentTarget.z, speedModifier);
        }
    }

    @Override
    public void stop() {
        super.stop();
        currentTarget = null;
    }

    @Override
    public void tick() {
        super.tick();

        if (mob.isInWater()) {
            boolean noseSubmerged = mob.getPartManager() != null && 
                                   mob.getPartManager().subEntities != null && 
                                   mob.getPartManager().subEntities.length > 0 && 
                                   mob.getPartManager().subEntities[0].isEyeInFluidType(net.minecraft.world.level.material.Fluids.WATER.getFluidType());

            // Only push down if surfacing (nose out of water)
            if (!noseSubmerged) {
                mob.setDeltaMovement(mob.getDeltaMovement().add(0, -0.1, 0));
            }
        }

        if (currentTarget != null && mob.getNavigation().isDone() && mob.position().distanceTo(currentTarget) > MIN_DISTANCE) {
            mob.getNavigation().moveTo(currentTarget.x, currentTarget.y, currentTarget.z, speedModifier);
        }
        if(this.mob.getDeltaMovement().length() > 0.5) mob.setMouthTarget(1);
    }

    protected Vec3 getPosition() {
        Vec3 target = Vec3.ZERO;
        target = mob.getRandom().nextFloat() < 0.7f ?
                findPositionInFront() :
                BehaviorUtils.getRandomSwimmablePos(mob, HORIZONTAL_RANGE, VERTICAL_RANGE);
        return target == null ? mob.position() : target;
    }

    private Vec3 findPositionInFront() {
        Vec3 lookAngle = mob.getLookAngle();
        Vec3 mobPos = mob.position();

        for (int i = 0; i < 10; i++) {
            float angle = mob.getRandom().nextFloat() * FRONT_ANGLE * 2 - FRONT_ANGLE;
            Vec3 direction = lookAngle.yRot((float)Math.toRadians(angle));

            float distance = HORIZONTAL_RANGE;
            Vec3 targetPos = mobPos.add(direction.scale(distance));

            targetPos = targetPos.add(0, mob.getRandom().nextFloat() * VERTICAL_RANGE * 2 - VERTICAL_RANGE, 0);

            if (isSwimmablePos(mob, targetPos)) {
                return targetPos;
            }
        }
        return BehaviorUtils.getRandomSwimmablePos(mob, HORIZONTAL_RANGE, VERTICAL_RANGE);
    }

    private boolean isSwimmablePos(PathfinderMob entity, Vec3 targetPos) {
        return entity.level().getBlockState(BlockPos.containing(targetPos))
                .isPathfindable(PathComputationType.WATER);
    }
}
