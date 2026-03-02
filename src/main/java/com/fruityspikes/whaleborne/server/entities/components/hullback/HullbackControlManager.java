package com.fruityspikes.whaleborne.server.entities.components.hullback;


import com.fruityspikes.whaleborne.network.ToggleControlPayload;
import com.fruityspikes.whaleborne.server.entities.HelmEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Manages movement and rotation controls for the Hullback entity.
 *
 * Processes player steering input, client-side control state prediction,
 * and vector control toggling (mouse vs keyboard steering).
 *
 * Also contains inner classes for stationary-aware move/look/body rotation
 * controls that prevent AI from rotating the whale when anchored or stationary.
 *
 * Called during tick() from HullbackEntity.
 */
public class HullbackControlManager {
    private final HullbackEntity whale;
    
    // Cache for third person mod check
    private static Boolean IS_THIRD_PERSON_MOD_LOADED = null;

    public HullbackControlManager(HullbackEntity whale) {
        this.whale = whale;
    }

    @OnlyIn(Dist.CLIENT)
    public void clientHandleControlState() {
        // Check if the local player is controlling
        // Robust check for Helm/Multipart
        LivingEntity controller = whale.getControllingPassenger();
        
        if (controller == null) {
             try {
                 net.minecraft.client.player.LocalPlayer local = net.minecraft.client.Minecraft.getInstance().player;
                 if (local != null && local.getRootVehicle() == whale) {
                     controller = local;
                 }
             } catch (Exception e) {}
        }

        if (controller instanceof Player player && player == net.minecraft.client.Minecraft.getInstance().player) {
            
            // Check mod presence only once and cache it
            if (IS_THIRD_PERSON_MOD_LOADED == null) {
                IS_THIRD_PERSON_MOD_LOADED = ModList.get().isLoaded("leawind_third_person");
            }

            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.options == null) return;

            boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
            // Logic: Enable vector if mod is present AND NOT in first person
            boolean shouldVector = IS_THIRD_PERSON_MOD_LOADED && !isFirstPerson;
            
            boolean currentVectorState = whale.getEntityData().get(HullbackEntity.DATA_VECTOR_CONTROL);

            // Only send packet if DESIRED state is different from CURRENT
            if (shouldVector != currentVectorState) {
                // 1. Local Prediction (Update immediately to avoid visual lag/spam)
                whale.getEntityData().set(HullbackEntity.DATA_VECTOR_CONTROL, shouldVector);
                
                // 2. Send to server to confirm
                PacketDistributor.sendToServer(new ToggleControlPayload(whale.getId(), shouldVector));
            }
        }
    }

    public boolean isVectorControlActive() {
        // Logic 1: If Client, look at camera IMMEDIATELY and check if it's local player.
        if (whale.level().isClientSide) {
             return isVectorControlActiveClient();
        }
        
        // Logic 2: If Server (or another player viewing), trust synchronized data.
        return whale.getEntityData().get(HullbackEntity.DATA_VECTOR_CONTROL);
    }

    @OnlyIn(Dist.CLIENT)
    private boolean isVectorControlActiveClient() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || whale.getControllingPassenger() != mc.player) {
            // If I am not piloting, trust visual entityData
            return whale.getEntityData().get(HullbackEntity.DATA_VECTOR_CONTROL);
        }

        // Cache mod check (safety)
        if (IS_THIRD_PERSON_MOD_LOADED == null) {
            IS_THIRD_PERSON_MOD_LOADED = ModList.get().isLoaded("leawind_third_person");
        }

        // Absolute priority for current camera
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();
        return IS_THIRD_PERSON_MOD_LOADED && !isFirstPerson;
    }

    public Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        boolean hasInput = Mth.abs(player.xxa) > 0 || Mth.abs(player.zza) > 0;

        if (hasInput) {
            if (whale.hasAnchorDown()) {
                if (whale.tickCount % 10 == 0) whale.playSound(SoundEvents.WOOD_HIT, 1, 1);
                return Vec3.ZERO; 
            }
            
            if (whale.tickCount % 2 == 0) whale.playSound(SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.5f, 1.0f);
             
            if(whale.getControllingPassenger() != null && whale.getControllingPassenger().getVehicle() instanceof HelmEntity helmEntity){
                 helmEntity.setWheelRotation(helmEntity.getWheelRotation() + player.xxa / 10);
            }
        } else {
             if(whale.getControllingPassenger() != null && whale.getControllingPassenger().getVehicle() instanceof HelmEntity helmEntity){
                helmEntity.setPrevWheelRotation(helmEntity.getWheelRotation());
            }
        }

        if (this.whale.isInWater()) {
            float currentPitch = this.whale.getXRot();
            float maxPitch = 25f; // Maximum tilt angle in degrees

            if (Math.abs(currentPitch) > maxPitch) {
                this.whale.setXRot(Mth.clamp(currentPitch, -maxPitch, maxPitch));
                this.whale.xRotO = this.whale.getXRot();
            }
        }

        boolean vectorControl = isVectorControlActive();
        
        float xxa = player.xxa;
        float zza = player.zza;

        if (vectorControl) {
            // --- VECTOR MODE (3rd Person) ---
            if (hasInput) {
                // Calculates rotation based on relative camera input
                float targetYaw = player.getYRot() - (float)(Mth.atan2(player.xxa, player.zza) * (180D / Math.PI));
                
                whale.setYRot(Mth.rotLerp(0.05f, whale.getYRot(), targetYaw));
                whale.yBodyRot = whale.getYRot();
                whale.yHeadRot = whale.getYRot();
                
                // Converts lateral movement to forward force
                zza = Mth.sqrt(xxa * xxa + zza * zza); 
                xxa = 0; 
            } else {
                zza = 0;
            }
        } else {
            // --- TANK MODE (1st Person / Vanilla) ---
            if (zza <= 0.0F) {
                zza *= 0.25F; // Slower reverse
            }
            
            // The lateral input (A/D) turns into ROTATION, not lateral movement
            if (player.xxa != 0) {
                whale.setYRot(Mth.rotLerp(0.8f, whale.getYRot(), whale.getYRot() - player.xxa)); 
                whale.yBodyRot = whale.getYRot();
            }
            
            // IMPORTANT: Zero the xxa to prevent "drift" lateral in first person
            xxa = 0; 
        }

        // Buoyancy is handled in HullbackEntity.travel()
        // Anchor horizontal logic remains here
        if (whale.hasAnchorDown() && whale.isInWater()) {
             zza = 0; // Ensure no forward movement with anchor
        }

        return new Vec3(0, 0, zza); 
    }
    public static class HullbackBodyRotationControl extends net.minecraft.world.entity.ai.control.BodyRotationControl {
        private final HullbackEntity hullback;

        public HullbackBodyRotationControl(HullbackEntity hullback) {
            super(hullback);
            this.hullback = hullback;
        }

        @Override
        public void clientTick() {
            if (hullback.getStationaryTicks() > 0) {
                return;
            }
            hullback.setYBodyRot(hullback.getYRot());
        }
    }

    /** Prevents SmoothSwimmingMoveControl from rotating the entity when stationary. */
    public static class StationaryAwareMoveControl extends net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl {
        private final HullbackEntity hullback;

        public StationaryAwareMoveControl(HullbackEntity entity, int maxTurnX, int maxTurnY, float inWaterSpeedModifier, float outsideWaterSpeedModifier, boolean applyGravity) {
            super(entity, maxTurnX, maxTurnY, inWaterSpeedModifier, outsideWaterSpeedModifier, applyGravity);
            this.hullback = entity;
        }

        @Override
        public void tick() {
            if (hullback.getStationaryTicks() > 0 || hullback.isPitchLocked()) {
                this.operation = Operation.WAIT;
                this.mob.setSpeed(0.0F);
                // Also force XRot to 0 if locked
                if (hullback.isPitchLocked()) {
                    hullback.setXRot(0f);
                }
                return;
            }
            if (this.mob.isInWater()) {
                this.mob.setSpeed((float) this.mob.getAttributeValue(HullbackEntity.getSwimSpeed()));
            } else {
                this.mob.setSpeed((float) this.mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
            }
            super.tick();

            // Limit pitch angle during AI movement to prevent excessive tilting
            if (this.mob.isInWater()) {
                this.mob.setXRot(Mth.clamp(this.mob.getXRot(), -20f, 20f));
            }
        }
    }

    /** Prevents SmoothSwimmingLookControl from rotating the entity when stationary. */
    public static class StationaryAwareLookControl extends net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl {
        private final HullbackEntity hullback;

        public StationaryAwareLookControl(HullbackEntity entity, int maxTurnDegrees) {
            super(entity, maxTurnDegrees);
            this.hullback = entity;
        }

        @Override
        public void tick() {
            if (hullback.getStationaryTicks() > 0) {
                return;
            }
            super.tick();
        }
    }

    public static class HullbackMoveControl extends net.minecraft.world.entity.ai.control.MoveControl {
        private final HullbackEntity hullback;
        private final int maxTurnX = 1;
        private final int maxTurnY = 2;
        private final float inWaterSpeedModifier = 1.0F;
        private final float outsideWaterSpeedModifier = 1.0F;
        private final boolean applyGravity = true;

        public HullbackMoveControl(HullbackEntity entity) {
            super(entity);
            this.hullback = entity;
        }

        @Override
        public void tick() {
            if (hullback.getStationaryTicks() > 0 || hullback.isPitchLocked()) {
                this.mob.setSpeed(0.0F);
                this.mob.setZza(0.0F);
                this.mob.setYya(0.0F);
                // Force XRot to 0 if locked
                if (hullback.isPitchLocked()) {
                    hullback.setXRot(0f);
                }
                return;
            }

            if (this.applyGravity && this.mob.isInWater()) {
                this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.005, 0.0));
            }

            if (this.operation == Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
                double d0 = this.wantedX - this.mob.getX();
                double d1 = this.wantedY - this.mob.getY();
                double d2 = this.wantedZ - this.mob.getZ();
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                if (d3 < 2.5000003E-7F) {
                    this.mob.setZza(0.0F);
                } else {
                    float f = (float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) - 90.0F;
                    this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f, (float)this.maxTurnY));
                    this.mob.yBodyRot = this.mob.getYRot();
                    this.mob.yHeadRot = this.mob.getYRot();

                    double baseSpeed = this.mob.isInWater() ?
                            this.mob.getAttributeValue(HullbackEntity.getSwimSpeed()) :
                            this.mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);

                    float f1 = (float)(this.speedModifier * baseSpeed);

                    if (this.mob.isInWater()) {
                        this.mob.setSpeed(f1 * this.inWaterSpeedModifier);
                        double d4 = Math.sqrt(d0 * d0 + d2 * d2);
                        if (Math.abs(d1) > 1.0E-5F || Math.abs(d4) > 1.0E-5F) {
                            float f3 = -((float)(Mth.atan2(d1, d4) * 180.0F / (float)Math.PI));
                            
                            // Limit pitch angle during AI movement to prevent excessive tilting
                            f3 = Mth.clamp(f3, -20f, 20f);

                            f3 = Mth.clamp(Mth.wrapDegrees(f3), (float)(-this.maxTurnX), (float)this.maxTurnX);
                            this.mob.setXRot(this.rotlerp(this.mob.getXRot(), f3, 5.0F));
                        }

                        float f6 = Mth.cos(this.mob.getXRot() * (float) (Math.PI / 180.0));
                        float f4 = Mth.sin(this.mob.getXRot() * (float) (Math.PI / 180.0));
                        this.mob.zza = f6 * f1;
                        this.mob.yya = -f4 * f1;
                    } else {
                        float f5 = Math.abs(Mth.wrapDegrees(this.mob.getYRot() - f));
                        float f2 = getTurningSpeedFactor(f5);
                        this.mob.setSpeed(f1 * this.outsideWaterSpeedModifier * f2);
                    }
                }
            } else {
                this.mob.setSpeed(0.0F);
                this.mob.setXxa(0.0F);
                this.mob.setYya(0.0F);
                this.mob.setZza(0.0F);
            }
        }

        private float getTurningSpeedFactor(float p_249853_) {
            return 1.0F - Mth.clamp((p_249853_ - 10.0F) / 50.0F, 0.0F, 1.0F);
        }
    }
}
