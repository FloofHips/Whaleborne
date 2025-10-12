package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

public class SailEntity extends WhaleWidgetEntity{
    private float SPEED_MODIFIER = 1.0F;
    public static final EntityDataAccessor<ItemStack> DATA_BANNER = SynchedEntityData.defineId(SailEntity.class, EntityDataSerializers.ITEM_STACK);

    public SailEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.SAIL.get());
    }

    public float getSpeedModifier() {
        return SPEED_MODIFIER;
    }
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BANNER, ItemStack.EMPTY);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(Items.WATER_BUCKET)) {
            this.spawnAtLocation(this.entityData.get(DATA_BANNER));
            this.entityData.set(DATA_BANNER, ItemStack.EMPTY);

            this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUCKET_EMPTY,
                    SoundSource.PLAYERS, 1.0F, 1.0f);

            return InteractionResult.SUCCESS;
        }
        if (player.getItemInHand(hand).is(ItemTags.BANNERS)) {
            this.entityData.set(DATA_BANNER, player.getItemInHand(hand));
            this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    SoundSource.PLAYERS, 1.0F, 1.0f);

            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    public ItemStack getBanner() {
        return this.entityData.get(DATA_BANNER);
    }
    public void addAdditionalSaveData(CompoundTag compound) {
        ItemStack banner = getBanner();

        if (!banner.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            banner.save(tag);
            compound.put("Banner", tag);
        }
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("Banner")) {
            CompoundTag tag = compound.getCompound("Banner");
            ItemStack banner = ItemStack.of(tag);
            this.entityData.set(DATA_BANNER, banner);
        }
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
}
