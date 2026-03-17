package com.fruityspikes.whaleborne.server.entities.goals.hullback;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class HullbackBreathAirGoal extends Goal {
    private static final int BREACH_HEIGHT = 10; // Blocks above surface to breach
    private static final float BREACH_SPEED = 1.2f;
    private static final float ROTATION_SPEED = 10f;

    private final HullbackEntity hullback;
    private int breachCooldown = 0;
    private boolean isBreaching = false;
    private Vec3 initialPos;

    public HullbackBreathAirGoal(HullbackEntity hullback) {
        this.hullback = hullback;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if(hullback.stationaryTicks > 0) return false;

        for (net.minecraft.world.entity.Entity passenger : hullback.getPassengers()) {
            if (passenger instanceof net.minecraft.world.entity.player.Player) {
                return false;
            }
        }
        if (breachCooldown > 0) {
            breachCooldown--;
            return false;
        }
        return this.hullback.getAirSupply() < this.hullback.getMaxAirSupply() * 0.2;
    }

    @Override
    public boolean canContinueToUse() {
        return (this.hullback.getAirSupply() < this.hullback.getMaxAirSupply() ||
                !this.hullback.isInWater());
    }

    @Override
    public void start() {
        this.isBreaching = true;
        this.hullback.setBreaching(true);
        this.hullback.stationaryTicks = 0; // Clear stationary state so it doesn't interfere with breach
        this.initialPos = this.hullback.position();
        this.hullback.getNavigation().stop();

        int surfaceY = this.hullback.level().getSeaLevel();
        Vec3 breachTarget = new Vec3(
                this.hullback.getX(),
                surfaceY + BREACH_HEIGHT,
                this.hullback.getZ()
        );

        this.hullback.getMoveControl().setWantedPosition(
                breachTarget.x,
                breachTarget.y,
                breachTarget.z,
                BREACH_SPEED
        );
    }

    @Override
    public void tick() {
        super.tick();

        float targetXRot = -60f;
        this.hullback.setXRot(Mth.rotLerp(ROTATION_SPEED * 0.1f, this.hullback.getXRot(), targetXRot));

        if (this.hullback.isInWater()) {
            float yRotRad = this.hullback.getYRot() * (float) (Math.PI / 180.0);

            this.hullback.setDeltaMovement(new Vec3(0, 1.2, 0.8).yRot(-yRotRad));
        } else {
            // Above water: extra downward pull so the whale arcs back faster
            this.hullback.setDeltaMovement(this.hullback.getDeltaMovement().add(0, -0.15, 0));
        }

        if (this.hullback.getY() >= this.hullback.level().getSeaLevel() &&
                this.hullback.level().isClientSide) {
            for (int i = 0; i < 5; i++) {
                this.hullback.level().addParticle(ParticleTypes.SPLASH,
                        this.hullback.getX() + (this.hullback.getRandom().nextFloat() - 0.5f) * 3f,
                        this.hullback.level().getSeaLevel(),
                        this.hullback.getZ() + (this.hullback.getRandom().nextFloat() - 0.5f) * 3f,
                        0, 0.5, 0);
            }
        }
    }

    @Override
    public void stop() {
        this.isBreaching = false;
        this.hullback.setBreaching(false);
        this.hullback.stationaryTicks = 0; // Ensure no leftover stationary state after breach
        this.breachCooldown = 200;

        this.hullback.setAirSupply(this.hullback.getMaxAirSupply());

        if (this.hullback.level().isClientSide && this.hullback.getPartManager() != null
                && this.hullback.getPartManager().partPosition != null
                && this.hullback.getPartManager().partPosition.length > 2) {
            for (int i = 0; i < 20; i++) {
                this.hullback.level().addParticle(ParticleTypes.BUBBLE,
                        this.hullback.getPartManager().partPosition[2].x,
                        this.hullback.getPartManager().partPosition[2].y,
                        this.hullback.getPartManager().partPosition[2].z,
                        (this.hullback.getRandom().nextFloat() - 0.5f) * 0.5f,
                        this.hullback.getRandom().nextFloat() * 0.5f,
                        (this.hullback.getRandom().nextFloat() - 0.5f) * 0.5f);
            }
            this.hullback.setMouthTarget(0.0f);
        }

        if (this.hullback.getPartManager() != null
                && this.hullback.getPartManager().partPosition != null
                && this.hullback.getPartManager().partPosition.length > 1) {
            Vec3 particlePos = this.hullback.getPartManager().partPosition[1].add(new Vec3(0, 7, 0));
            double x = particlePos.x;
            double y = particlePos.y;
            double z = particlePos.z;

            if (this.hullback.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        WBParticleRegistry.SMOKE.get(),
                        x,
                        y,
                        z,
                        50,
                        0.2,
                        0.2,
                        0.2,
                        0.02
                );
            }
        }

        this.hullback.playSound(WBSoundRegistry.HULLBACK_BREATHE.get(), 3.0f, 1);
        this.hullback.setXRot(Mth.rotLerp(0.1f, this.hullback.getXRot(), 0));
    }

    public boolean isBreaching() {
        return this.isBreaching;
    }
}
