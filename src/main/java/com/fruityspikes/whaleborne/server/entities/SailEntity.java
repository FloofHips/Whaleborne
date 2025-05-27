package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SailEntity extends WhaleWidgetEntity{

    public SailEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.isPassenger()){
            Entity whale = this.getVehicle();
            whale.getDeltaMovement().multiply(2.5f, 2.5f, 2.5f);

            if (whale.getDeltaMovement().length()>0.1f){
                if (this.tickCount % 100 == 0)
                    this.level().playSound(this, BlockPos.containing(this.position()), SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, (float) whale.getDeltaMovement().length(), (float) whale.getDeltaMovement().length());
                //System.out.println(whale.getDeltaMovement().length());
            }
        }
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean canRiderInteract() {
        return super.canRiderInteract();
    }
}
