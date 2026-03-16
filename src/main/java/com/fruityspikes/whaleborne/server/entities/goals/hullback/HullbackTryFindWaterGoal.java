package com.fruityspikes.whaleborne.server.entities.goals.hullback;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.Iterator;
import java.util.List;

public class HullbackTryFindWaterGoal extends Goal {
    private final PathfinderMob mob;
    private final boolean isBeached;

    public HullbackTryFindWaterGoal(PathfinderMob mob, boolean isBeached) {
        this.mob = mob;
        this.isBeached = isBeached;
    }

    public boolean canUse() {
        if (mob instanceof HullbackEntity hullback && hullback.stationaryTicks > 0) return false;
        
        if(isBeached)
            return mob.tickCount > 20 && !this.mob.isEyeInFluidType(Fluids.WATER.getFluidType()) && !this.mob.level().getFluidState(this.mob.blockPosition().above(3)).is(FluidTags.WATER);
        return mob.tickCount > 20 && !mob.level().getFluidState(mob.blockPosition().below()).is(FluidTags.WATER);
    }

    public void start() {
        BlockPos blockpos = null;

            Iterator var2 = BlockPos.betweenClosed(Mth.floor(this.mob.getX() - (isBeached ? 20.0 : 5)), Mth.floor(this.mob.getY() - 2.0), Mth.floor(this.mob.getZ() - (isBeached ? 20.0 : 5)), Mth.floor(this.mob.getX() + (isBeached ? 20.0 : 5)), this.mob.getBlockY(), Mth.floor(this.mob.getZ() + (isBeached ? 20.0 : 5))).iterator();

        while(var2.hasNext()) {
            BlockPos blockpos1 = (BlockPos)var2.next();
            if (this.mob.level().getFluidState(blockpos1).is(FluidTags.WATER)) {
                blockpos = blockpos1;
                break;
            }
        }

        if (blockpos != null) {
            this.mob.getMoveControl().setWantedPosition((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), 1.0);
            this.mob.getLookControl().setLookAt(mob.getMoveControl().getWantedX(), mob.getMoveControl().getWantedY(), mob.getMoveControl().getWantedZ());
        }
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 target = new Vec3(mob.getMoveControl().getWantedX(),
                mob.getMoveControl().getWantedY(),
                mob.getMoveControl().getWantedZ());
        float targetYRot = (float)Math.toDegrees(Math.atan2(target.z - mob.getZ(), target.x - mob.getX())) - 90;

        mob.setYRot(Mth.rotLerp(0.01f, mob.getYRot(), targetYRot));


        if(mob.tickCount % 10 == 0)
            mob.playSound(WBSoundRegistry.HULLBACK_MAD.get());

        if(mob.tickCount % 100 == 0 && !mob.level().getBlockState(mob.blockPosition().below()).isAir()){

            if (mob instanceof HullbackEntity) ((HullbackEntity) mob).setMouthTarget(0);

            mob.getLookControl().setLookAt(target);
            mob.setYRot(Mth.rotLerp(0.1f, mob.getYRot(), targetYRot));
            mob.yBodyRot = mob.getYRot();
            Vec3 direction = target.subtract(mob.position()).normalize();

            double lungePower = 1.5;
            Vec3 velocity = direction.scale(lungePower).add(0, 1.0, 0);
            if(isBeached) {
                mob.playSound(WBSoundRegistry.ORGAN.get(), 2, 2f);
                mob.playSound(WBSoundRegistry.ORGAN.get(), 2, 1f);
                mob.playSound(WBSoundRegistry.HULLBACK_HURT.get(), 3.0f, 0.2f);
                mob.playSound(WBSoundRegistry.HULLBACK_SWIM.get(), 2, 0.5f);
                pushEntities();
            }
            mob.setDeltaMovement(velocity);

            mob.setYRot(Mth.rotLerp(0.2f, mob.getYRot(), targetYRot));
            mob.yBodyRot = mob.getYRot();
        }
    }

    public void pushEntities(){
        AABB pushArea = mob.getBoundingBox().inflate(20);
        List<Entity> pushableEntities = this.mob.level().getEntities(mob, pushArea);
        pushableEntities.removeIf(entity -> !entity.isPushable() && entity.isPassenger());

        Vec3 center = mob.position();
        double pushStrength = 3;

        for (Entity entity : pushableEntities) {

            Vec3 pushDir = entity.position().subtract(center).normalize();
            entity.push(
                    pushDir.x * pushStrength,
                    0.3,
                    pushDir.z * pushStrength
            );

            if (this.mob.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        WBParticleRegistry.SMOKE.get(),
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        10,
                        0.5,
                        0.5,
                        0.5,
                        0.02
                );
            }

            if (entity instanceof LivingEntity living) {
                living.knockback(0.5f, pushDir.x, pushDir.z);
                living.hurtMarked = true;
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if(isBeached) {
            // Need cast to HullbackEntity for setAirSupply, etc if mob is not generic. 
            // The constructor takes PathfinderMob but HullbackEntity specifics are used.
            if(mob instanceof HullbackEntity hullback) {
                hullback.setAirSupply(hullback.getMaxAirSupply());

                if (hullback.level().isClientSide && hullback.getPartManager() != null
                        && hullback.getPartManager().partPosition != null
                        && hullback.getPartManager().partPosition.length > 2) {
                    for (int i = 0; i < 20; i++) {
                        hullback.level().addParticle(ParticleTypes.BUBBLE,
                                hullback.getPartManager().partPosition[2].x,
                                hullback.getPartManager().partPosition[2].y,
                                hullback.getPartManager().partPosition[2].z,
                                (hullback.getRandom().nextFloat() - 0.5f) * 0.5f,
                                hullback.getRandom().nextFloat() * 0.5f,
                                (hullback.getRandom().nextFloat() - 0.5f) * 0.5f);
                    }
                    hullback.setMouthTarget(0.0f);
                }

                if (hullback.getPartManager() != null
                        && hullback.getPartManager().partPosition != null
                        && hullback.getPartManager().partPosition.length > 1) {
                    Vec3 particlePos = hullback.getPartManager().partPosition[1].add(new Vec3(0, 7, 0));
                    double x = particlePos.x;
                    double y = particlePos.y;
                    double z = particlePos.z;

                    if (hullback.level() instanceof ServerLevel serverLevel) {
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

                hullback.playSound(WBSoundRegistry.HULLBACK_BREATHE.get(), 1.5f, 1);
            }
        }
    }
}
