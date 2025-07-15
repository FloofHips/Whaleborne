package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;

public abstract class WhaleWidgetEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(WhaleWidgetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ID_HURTDIR = SynchedEntityData.defineId(WhaleWidgetEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(WhaleWidgetEntity.class, EntityDataSerializers.FLOAT);
    protected Item item;
    public WhaleWidgetEntity(EntityType<?> entityType, Level level, Item dropItem) {
        super(entityType, level);
        this.item = dropItem;
    }
    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ID_HURT, 0);
        this.entityData.define(DATA_ID_HURTDIR, 1);
        this.entityData.define(DATA_ID_DAMAGE, 0.0F);
    }

    public void setDamage(float damageTaken) {
        this.entityData.set(DATA_ID_DAMAGE, damageTaken);
    }

    public float getDamage() {
        return (Float)this.entityData.get(DATA_ID_DAMAGE);
    }

    public void setHurtTime(int hurtTime) {
        this.entityData.set(DATA_ID_HURT, hurtTime);
    }

    public int getHurtTime() {
        return (Integer)this.entityData.get(DATA_ID_HURT);
    }
    public void setHurtDir(int hurtDirection) {
        this.entityData.set(DATA_ID_HURTDIR, hurtDirection);
    }

    public int getHurtDir() {
        return (Integer)this.entityData.get(DATA_ID_HURTDIR);
    }


    @Override
    public void tick() {
        super.tick();
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }
        if(this.tickCount > 10 && !this.isPassenger()){
            destroy(null);
        }
    }
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        return this.item.getDefaultInstance();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    public void animateHurt(float yaw) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() * 11.0F);
    }
    protected void destroy(DamageSource damageSource) {
        this.spawnAtLocation(this.getDropItem());
        this.kill();
    }

    public Item getDropItem() {
        return this.item;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level().isClientSide && !this.isRemoved()) {
            this.setHurtDir(-this.getHurtDir());
            this.setHurtTime(10);
            this.setDamage(this.getDamage() + amount * 10.0F);
            this.markHurt();
            this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
            boolean flag = source.getEntity() instanceof Player && ((Player)source.getEntity()).getAbilities().instabuild;
            if (flag || this.getDamage() > 40.0F) {
                if (!flag && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    this.destroy(source);
                }

                this.discard();
            }

            return true;
        } else {
            return true;
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (this.isVehicle()) {
                this.ejectPassengers();
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS,
                        0.5F, 1.5F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (this.isPassenger()) {
                this.stopRiding();
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS,
                        0.5F, 0.8F);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }
        return super.interact(player, hand);
    }
}
