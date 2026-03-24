package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(Items.WATER_BUCKET) && !this.getBanner().isEmpty()) {
            if (!this.level().isClientSide) {
                this.spawnAtLocation(this.entityData.get(DATA_BANNER));
                this.entityData.set(DATA_BANNER, ItemStack.EMPTY);

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMBIENT_UNDERWATER_EXIT,
                        SoundSource.PLAYERS, 1.0F, this.random.nextFloat() * 0.5f + 0.5f);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ARMOR_EQUIP_LEATHER,
                        SoundSource.PLAYERS, 1.0F, this.random.nextFloat() * 0.5f + 0.5f);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (player.getItemInHand(hand).is(ItemTags.BANNERS)) {
            if (!this.level().isClientSide) {
                if (!this.getBanner().isEmpty()) {
                    this.spawnAtLocation(this.entityData.get(DATA_BANNER));
                }
                ItemStack bannerStack = player.getItemInHand(hand).copy();
                bannerStack.setCount(1);

                this.entityData.set(DATA_BANNER, bannerStack);

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ARMOR_EQUIP_LEATHER,
                        SoundSource.PLAYERS, 1.0F, this.random.nextFloat() * 0.5f + 0.5f);
                if (!player.getAbilities().instabuild) {
                    player.getItemInHand(hand).shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        // Orientation change when placed on the ground (not a passenger)
        if (!this.isPassenger()) {
            if (player.isShiftKeyDown()) {
                this.setYRot(getYRot() - 11.25f);
            } else {
                this.setYRot(getYRot() + 11.25f);
            }
            this.playSound(SoundEvents.WOOD_HIT);
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
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
        if (!this.level().isClientSide) return;
        if(this.isPassenger()){
            Entity whale = this.getVehicle();

            if (whale != null && whale.getDeltaMovement().length() > 0.1){
                if (this.tickCount % 20 == 0) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), WBSoundRegistry.SAIL_BLOW.get(), SoundSource.AMBIENT, 2, (float) whale.getDeltaMovement().length()*2, true);
                }
            }
        }
    }

    @Override
    protected void destroy(DamageSource damageSource) {
        spawnAtLocation(getBanner());
        super.destroy(damageSource);
    }
}
