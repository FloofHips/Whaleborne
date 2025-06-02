package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class RideableWhaleWidgetEntity extends WhaleWidgetEntity{
    public RideableWhaleWidgetEntity(EntityType<?> entityType, Level level, Item dropItem) {
        super(entityType, level, dropItem);
    }

    @Override
    public void tick() {
        super.tick();
        if(this.isVehicle()){
            if(this.getFirstPassenger() instanceof Player player){

            }
        }
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger();
    }
    @Nullable
    public LivingEntity getFirstPassenger() {
        return getPassengers().isEmpty() ? null : (LivingEntity) getPassengers().get(0);
    }
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && this.getPassengers().isEmpty()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight() + 0.5f;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }
}
