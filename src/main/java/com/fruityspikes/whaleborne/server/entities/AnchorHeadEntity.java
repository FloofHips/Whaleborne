package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            this.setYRot(getYRot() - 11.25f);
            this.playSound(SoundEvents.METAL_HIT);
            return InteractionResult.SUCCESS;
        }
        else {
            this.setYRot(getYRot() + 11.25f);
            this.playSound(SoundEvents.METAL_HIT);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
    }
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level().isClientSide && !this.isRemoved()) {
            this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
            boolean flag = source.getEntity() instanceof Player && ((Player)source.getEntity()).getAbilities().instabuild;
            if (!flag && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                this.playSound(SoundEvents.METAL_HIT);
                this.destroy(source);
            }
            this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.75F, this.random.nextFloat() * 0.5f + 0.4F);
            this.discard();
            if (this.level() instanceof ServerLevel) {
                ((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ANVIL.defaultBlockState()), this.getX(), this.getY(0.6666666666666666), this.getZ(), 10, (double)(this.getBbWidth() / 4.0F), (double)(this.getBbHeight() / 4.0F), (double)(this.getBbWidth() / 4.0F), 0.05);
            }
            return true;
        } else {
            return true;
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    protected void destroy(DamageSource damageSource) {
        this.spawnAtLocation(this.getDropItem());
        this.kill();
    }

    public Item getDropItem() {
        return WBItemRegistry.ANCHOR.get();
    }

    @Override
    public void tick() {
        super.tick();
    }

    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

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
