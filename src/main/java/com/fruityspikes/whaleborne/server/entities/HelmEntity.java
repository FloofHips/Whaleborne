package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class HelmEntity extends RideableWhaleWidgetEntity implements PlayerRideableJumping {

    public HelmEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, Items.STICK);
    }
    public float wheelRotation;
    public float prevWheelRotation;
    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight();
    }

    public float getWheelRotation() {
        return wheelRotation;
    }

    public void setWheelRotation(float wheelRotation) {
        this.setPrevWheelRotation(this.wheelRotation);
        this.wheelRotation = wheelRotation;
    }
    public float getPrevWheelRotation() {
        return prevWheelRotation;
    }

    public void setPrevWheelRotation(float prevWheelRotation) {
        this.prevWheelRotation = prevWheelRotation;
    }
    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity entity = this.getFirstPassenger();
        LivingEntity livingentity1;
        if (entity instanceof LivingEntity livingentity) {
            livingentity1 = livingentity;
        } else {
            livingentity1 = null;
        }
        return livingentity1;
    }
    @Override
    public void onPlayerJump(int i) {
        this.getVehicle().setDeltaMovement(this.getFirstPassenger().getLookAngle().multiply(1.5, 2, 1.5));
    }

    @Override
    public boolean canJump() {
        return true;
    }

    @Override
    public void handleStartJump(int i) {

    }

    @Override
    public void handleStopJump() {

    }
}
