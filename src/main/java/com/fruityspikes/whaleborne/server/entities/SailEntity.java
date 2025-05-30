package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class SailEntity extends WhaleWidgetEntity{
    private float SPEED_MODIFIER = 0.5F;

    public SailEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, Items.PAPER);
    }

    public float getSpeedModifier() {
        return SPEED_MODIFIER;
    }

    @Override
    public void tick() {
        super.tick();
//        if(this.isPassenger()){
//            Entity whale = this.getVehicle();
//
//            if (whale.getDeltaMovement().length()>0.1f){
//                if (this.tickCount % 100 == 0)
//                    this.level().playSound(this, BlockPos.containing(this.position()), SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, (float) whale.getDeltaMovement().length(), (float) whale.getDeltaMovement().length());
//            }
//        }
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
        return true;
    }
}
