package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class CannonEntity extends RideableWhaleWidgetEntity {
    public CannonEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, Items.FIREWORK_ROCKET);
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
    public void onPassengerTurned(Entity entityToUpdate) {
        super.onPassengerTurned(entityToUpdate);
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
