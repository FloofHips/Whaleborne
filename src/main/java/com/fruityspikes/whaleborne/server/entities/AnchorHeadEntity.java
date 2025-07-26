package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class AnchorHeadEntity extends Entity {
    public AnchorHeadEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof PickaxeItem)
            playSound(WBSoundRegistry.ORGAN.get());
        return super.interact(player, hand);
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
    }

    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    public boolean isPickable() {
        return true;
    }
    @Nullable
    public ItemStack getPickResult() {
        return WBItemRegistry.ANCHOR.get().getDefaultInstance();
    }
}
