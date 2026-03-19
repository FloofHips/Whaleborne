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
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class RideableWhaleWidgetEntity extends WhaleWidgetEntity{
    public RideableWhaleWidgetEntity(EntityType<?> entityType, Level level, Item dropItem) {
        super(entityType, level, dropItem);
    }

    @Override
    public void tick() {
        super.tick();
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
        if (this.getPassengers().isEmpty()) {
            if (!this.level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.interact(player, hand);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0, this.getBbHeight() + 0.5f, 0);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

}
