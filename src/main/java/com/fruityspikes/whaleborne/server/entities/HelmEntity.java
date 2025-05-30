package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class HelmEntity extends RideableWhaleWidgetEntity {
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputUp;
    private boolean inputDown;
    private float deltaRotation;
    private static final float BASE_SPEED = 0.04F;
    private static final float TURN_SPEED = 0.5F;
    private static final float DRAG_FACTOR = 0.98F;
    private static final float MAX_SPEED = 0.2F;
    public HelmEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, Items.STICK);
    }
//    public void setInput(boolean inputLeft, boolean inputRight, boolean inputUp, boolean inputDown) {
//        this.inputLeft = inputLeft;
//        this.inputRight = inputRight;
//        this.inputUp = inputUp;
//        this.inputDown = inputDown;
//    }
//    @Override
//    public void tick() {
//        super.tick();
//        if (this.isPassenger()) {
//            controlVehicle(this.getVehicle());
//        }
//    }
////    @Override
////    public void onPassengerTurned(Entity passenger) {
////        if (passenger instanceof Player player) {
////            this.setInput(
////                    player.xxa < 0,
////                    player.xxa > 0,
////                    player.zza < 0,
////                    player.zza > 0
////            );
////        }
////    }
//    @Nullable
//    public LivingEntity getControllingPassenger() {
//        Entity entity = this.getFirstPassenger();
//        LivingEntity livingentity1;
//        if (entity instanceof LivingEntity livingentity) {
//            livingentity1 = livingentity;
//        } else {
//            livingentity1 = null;
//        }
//
//        return livingentity1;
//    }
//    private void controlVehicle(Entity vehicle) {
//        LivingEntity pilot = getControllingPassenger();
//        if (pilot == null) return;
//
//        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
//        Vec3 movement = Vec3.ZERO;
//
//        if (inputLeft != inputRight) {
//            this.deltaRotation = Mth.clamp(deltaRotation + (inputLeft ? -TURN_SPEED : TURN_SPEED), -3, 3);
//            vehicle.setYRot(vehicle.getYRot() + deltaRotation);
//        } else {
//            deltaRotation *= 0.8F;
//        }
//
//        float speedModifier = 0;
//        if (inputUp) speedModifier += BASE_SPEED;
//        if (inputDown) speedModifier -= BASE_SPEED * 0.5F;
//
//        if (speedModifier != 0) {
//            movement = new Vec3(
//                    -Mth.sin(yawRad) * speedModifier,
//                    0,
//                    Mth.cos(yawRad) * speedModifier
//            );
//        }
//
//        Vec3 currentMotion = vehicle.getDeltaMovement();
//        Vec3 newMotion = currentMotion.multiply(DRAG_FACTOR, DRAG_FACTOR, DRAG_FACTOR)
//                .add(movement);
//
//
//        vehicle.setDeltaMovement(newMotion);
//    }
}
