package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.Vec3;

public class SailEntity extends WhaleWidgetEntity{
    private static final float SPEED_MODIFIER = 1.0F;
    public static final EntityDataAccessor<ItemStack> DATA_BANNER = SynchedEntityData.defineId(SailEntity.class, EntityDataSerializers.ITEM_STACK);

    public SailEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.SAIL.get());
    }

    public float getSpeedModifier() {
        return SPEED_MODIFIER;
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BANNER, ItemStack.EMPTY);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    // Ground interaction is handled by SailInteractionHandler event.
    // This method only handles interaction when the sail is a passenger (on the Hullback).

    public ItemStack getBanner() {
        return this.entityData.get(DATA_BANNER);
    }
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        ItemStack banner = getBanner();

        if (!banner.isEmpty()) {
            compound.put("Banner", banner.save(this.registryAccess()));
        }
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Banner")) {
            CompoundTag tag = compound.getCompound("Banner");
            ItemStack banner = ItemStack.parse(this.registryAccess(), tag).orElse(ItemStack.EMPTY);
            this.entityData.set(DATA_BANNER, banner);
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scale) {
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0, this.getBbHeight() - 1.0f, 0);
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isPassenger();
    }

    @Override
    public void tick() {
        super.tick();
        if(this.isPassenger()){
            Entity whale = this.getVehicle();

            if (whale.getDeltaMovement().length()>0.1f){
                if (this.tickCount % 500 == 0)
                    this.level().playSound(this, BlockPos.containing(this.position()), SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, 1, (float) whale.getDeltaMovement().length());
            }
        }
    }

    @Override
    protected void destroy(DamageSource damageSource) {
        spawnAtLocation(getBanner());
        super.destroy(damageSource);
    }
}
