package com.fruityspikes.whaleborne.server.entities.goals.hullback;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import com.fruityspikes.whaleborne.server.registries.WBTagRegistry;
import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import java.util.EnumSet;

public class HullbackArmorPlayerGoal extends Goal {
    private static final float APPROACH_DISTANCE = 6.0f;
    private static final float SIDE_OFFSET = 5.0f;
    private static final float ROTATION_SPEED = 0.8f;
    private static Ingredient TEMPT_PLANKS = Ingredient.of(WBTagRegistry.HULLBACK_EQUIPPABLE);
    private static Ingredient TEMPT_WIDGETS = Ingredient.of(WBItemRegistry.SAIL.get(), WBItemRegistry.ANCHOR.get(), WBItemRegistry.MAST.get(), WBItemRegistry.HELM.get(), WBItemRegistry.CANNON.get());
    private final HullbackEntity hullback;
    private final float speedModifier;
    // Increased range to 40.0 for better response
    private static final TargetingConditions TEMP_TARGETING = TargetingConditions.forNonCombat().range(40.0).ignoreLineOfSight();
    private final TargetingConditions targetingConditions;
    private Player targetPlayer;
    private boolean approachFromRight;
    private Vec3 targetPosition;

    public HullbackArmorPlayerGoal(HullbackEntity hullback, float speedModifier) {
        this.hullback = hullback;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.targetingConditions = TEMP_TARGETING.copy().selector(this::shouldFollow);
    }

    private boolean shouldFollow(LivingEntity entity) {
        if (hullback.isSaddled()) {
            if (hullback.getArmorProgress() >= 0.5 && hullback.getArmorProgress() < 1)
                return TEMPT_PLANKS.test(entity.getMainHandItem()) || TEMPT_PLANKS.test(entity.getOffhandItem()) || TEMPT_WIDGETS.test(entity.getMainHandItem()) || TEMPT_WIDGETS.test(entity.getOffhandItem());
            if (hullback.getArmorProgress() == 1)
                return TEMPT_WIDGETS.test(entity.getMainHandItem()) || TEMPT_WIDGETS.test(entity.getOffhandItem());
            return TEMPT_PLANKS.test(entity.getMainHandItem()) || TEMPT_PLANKS.test(entity.getOffhandItem());
        }
        return false;
    }

    @Override
    public boolean canUse() {
        if (!hullback.isTamed()) return false;
        if (hullback.hasAnchorDown()) return false;
        if (hullback.getAirSupply() < hullback.getMaxAirSupply() * 0.2) return false;
        if (hullback.isBreaching()) return false;

        this.targetPlayer = this.hullback.level().getNearestPlayer(this.targetingConditions, this.hullback);
        if (this.targetPlayer == null) return false;
        
        // Anti-mounting check (don't approach if player is already on me or on an entity relative to me)
        if (this.targetPlayer.isPassenger()) {
            Entity vehicle = this.targetPlayer.getVehicle();
            if (vehicle != null && (vehicle.is(this.hullback) || (vehicle.isPassenger() && vehicle.getVehicle() != null && vehicle.getVehicle().is(this.hullback)))) return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (hullback.getAirSupply() < hullback.getMaxAirSupply() * 0.2) return false;
        if (hullback.isBreaching()) return false;
        return canUse();
    }

    @Override
    public void start() {
        super.start();
        this.hullback.setApproachingPlayer(true);
        this.hullback.setTarget(this.targetPlayer);
        
        Vec3 toPlayer = targetPlayer.position().subtract(hullback.position());
        Vec3 whaleRight = Vec3.directionFromRotation(0, hullback.getYRot() + 90);
        this.approachFromRight = toPlayer.dot(whaleRight) > 0;

        hullback.playSound(WBSoundRegistry.HULLBACK_HAPPY.get());
        this.hullback.setMouthTarget(0.1f);
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.targetPosition = null;
        this.hullback.setTarget(null);
        this.hullback.setApproachingPlayer(false);
        this.hullback.getNavigation().stop();
        this.hullback.setMouthTarget(0.0f);
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null) return;

        this.hullback.setMouthTarget(0.6f);
        
        // RECALCULATION PER TICK: Ensures the whale doesn't "park" far away if the player moves
        Vec3 playerLook = this.targetPlayer.getLookAngle();
        Vec3 perpendicular = new Vec3(-playerLook.z, 0, playerLook.x).normalize();
        Vec3 sideOffset = perpendicular.scale(approachFromRight ? SIDE_OFFSET : -SIDE_OFFSET);
        this.targetPosition = this.targetPlayer.position()
                .add(sideOffset)
                .add(playerLook.scale(-APPROACH_DISTANCE));

        // NATIVE NAVIGATION: Tries pathfinding
        this.hullback.getNavigation().moveTo(
                targetPosition.x, targetPosition.y, targetPosition.z, this.speedModifier);

        // CONTINUOUS SMOOTH ROTATION
        Vec3 toPlayer = targetPlayer.position().subtract(hullback.position());
        float desiredYaw = (float) Math.toDegrees(Math.atan2(toPlayer.z, toPlayer.x)) - 90f;
        float sideYawOffset = approachFromRight ? -90f : 90f;
        float targetYaw = desiredYaw + sideYawOffset;
        this.hullback.setYRot(Mth.rotLerp(0.05f, this.hullback.getYRot(), targetYaw));
        this.hullback.yBodyRot = this.hullback.getYRot();
    }
}
