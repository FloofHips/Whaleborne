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
    public void onPassengerTurned(Entity entityToUpdate) {
        super.onPassengerTurned(entityToUpdate);
    }



    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public boolean canRiderInteract() {
        return super.canRiderInteract();
    }
}
