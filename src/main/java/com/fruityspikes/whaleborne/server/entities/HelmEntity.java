package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

    private static final EntityDataAccessor<Float> DATA_WHEEL_ROTATION =
            SynchedEntityData.defineId(HelmEntity.class, EntityDataSerializers.FLOAT);

    /** Client-only previous value kept for render interpolation. */
    public float prevWheelRotation;

    /**
     * Client-side predicted wheel rotation.  On the LOCAL client we replicate
     * the server's {@code wheel += xxa / 10} logic so the wheel responds
     * instantly to input without waiting for the entityData round-trip.
     * A slow blend towards the server-synced value (entityData) prevents drift.
     */
    private float clientWheelRotation;

    public HelmEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.HELM.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WHEEL_ROTATION, 0.0F);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            // ---- client-side wheel prediction ----
            this.prevWheelRotation = this.clientWheelRotation;

            // Mirror server logic: apply local input immediately
            if (this.getVehicle() instanceof HullbackEntity hullback) {
                LivingEntity ctrl = hullback.getControllingPassenger();
                if (ctrl instanceof Player player && player == net.minecraft.client.Minecraft.getInstance().player) {
                    if (Mth.abs(player.xxa) > 0 && !hullback.hasAnchorDown()) {
                        this.clientWheelRotation += player.xxa / 10;
                    }
                }
            }

            // Slowly reconcile with the server-authoritative value to prevent drift
            float serverValue = this.getWheelRotation();
            this.clientWheelRotation = Mth.lerp(0.25f, this.clientWheelRotation, serverValue);
        } else {
            // Server: simple prev tracking (not used for rendering)
            this.prevWheelRotation = this.getWheelRotation();
        }

        super.tick();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0, this.getBbHeight() - 1.25f, 0);
    }

    /** Raw synced value from the server (entityData). */
    public float getWheelRotation() {
        return this.entityData.get(DATA_WHEEL_ROTATION);
    }

    /**
     * Returns the wheel rotation the renderer should display.
     * On the client this is the locally-predicted value;
     * elsewhere it falls back to the synced entityData value.
     */
    public float getRenderWheelRotation() {
        return this.level().isClientSide ? this.clientWheelRotation : this.getWheelRotation();
    }

    public void setWheelRotation(float wheelRotation) {
        this.entityData.set(DATA_WHEEL_ROTATION, wheelRotation);
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
            hullback.setStationaryTicks(100);
        super.removePassenger(passenger);
    }
}
