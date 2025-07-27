package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;

public class HelmEntity extends RideableWhaleWidgetEntity implements PlayerRideableJumping, HasCustomInventoryScreen {

    public HelmEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.HELM.get());
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
        //if (this.getVehicle() instanceof HullbackEntity hullback){
        this.getVehicle().playSound(WBSoundRegistry.ORGAN.get());
        //}
    }

    @Override
    public boolean canJump() {
        return true;
    }

    @Override
    public void handleStartJump(int i) {
        this.getVehicle().playSound(WBSoundRegistry.ORGAN.get(), 2, 2);
        this.getVehicle().playSound(WBSoundRegistry.ORGAN.get(), 1.5f, 1.5f);

        boolean hasAnchorDown = false;
        if(this.getVehicle()!=null && this.getVehicle() instanceof HullbackEntity hullback){
            hasAnchorDown = hullback.hasAnchorDown();
        }
        for (Entity passenger : this.getVehicle().getPassengers()) {
            if (passenger instanceof AnchorEntity anchor) {
                if (hasAnchorDown) {
                    if (anchor.isDown()) {
                        this.getVehicle().playSound(SoundEvents.CHAIN_PLACE, 1, 0);
                        anchor.toggleDown();
                    }
                } else {
                    if (!anchor.isDown() && anchor.isClosed()) {
                        this.getVehicle().playSound(SoundEvents.CHAIN_BREAK, 1, 2);
                        anchor.toggleDown();
                    }
                }
            }
        }
    }

    @Override
    public void handleStopJump() {

    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        if(this.isPassenger() && this.getVehicle() instanceof HullbackEntity hullback)
            hullback.openCustomInventoryScreen(player);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        if(this.isVehicle() && getVehicle() instanceof HullbackEntity hullback)
            hullback.stationaryTicks = 100;
        super.removePassenger(passenger);
    }
}
