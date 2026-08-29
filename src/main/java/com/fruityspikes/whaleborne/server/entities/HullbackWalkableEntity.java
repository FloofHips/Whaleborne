package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class HullbackWalkableEntity extends Entity {

    private static final int OWNER_CHECK_INTERVAL = 32;
    private static final int OWNER_CHECK_GRACE_TICKS = 20;
    private static final double OWNER_MAX_DIST_SQ = 24.0 * 24.0;

    private UUID ownerUuid;

    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(HullbackWalkableEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(HullbackWalkableEntity.class, EntityDataSerializers.FLOAT);

    public HullbackWalkableEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public void applyDimensions(float width, float height) {
        this.entityData.set(DATA_WIDTH, Math.max(0.1f, width));
        this.entityData.set(DATA_HEIGHT, Math.max(0.1f, height));
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (this.entityData == null) return super.getDimensions(pose);
        return EntityDimensions.fixed(this.entityData.get(DATA_WIDTH), this.entityData.get(DATA_HEIGHT));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_WIDTH.equals(accessor) || DATA_HEIGHT.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    public void setOwner(Entity owner) {
        this.ownerUuid = owner == null ? null : owner.getUUID();
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.isRemoved()) return;
        if (this.tickCount < OWNER_CHECK_GRACE_TICKS || this.tickCount % OWNER_CHECK_INTERVAL != 0) return;
        if (this.ownerUuid == null) {
            this.discard();
            return;
        }
        Entity owner = ((ServerLevel) this.level()).getEntity(this.ownerUuid);
        if (!(owner instanceof HullbackEntity whale)
                || whale.isRemoved()
                || !whale.ownsPlatform(this)
                || whale.distanceToSqr(this) > OWNER_MAX_DIST_SQ) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean mayInteract(Level level, BlockPos pos) { return false; }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WIDTH, 1.5f);
        builder.define(DATA_HEIGHT, 0.5f);
    }


    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {}
}
