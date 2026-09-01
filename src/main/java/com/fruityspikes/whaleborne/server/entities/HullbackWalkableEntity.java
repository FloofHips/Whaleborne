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
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class HullbackWalkableEntity extends Entity {

    private static final int OWNER_CHECK_INTERVAL = 32;
    private static final int OWNER_CHECK_GRACE_TICKS = 20;
    private static final double OWNER_MAX_DIST_SQ = 24.0 * 24.0;

    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(HullbackWalkableEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANCHOR =
            SynchedEntityData.defineId(HullbackWalkableEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LOCAL_X =
            SynchedEntityData.defineId(HullbackWalkableEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LOCAL_Y =
            SynchedEntityData.defineId(HullbackWalkableEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LOCAL_Z =
            SynchedEntityData.defineId(HullbackWalkableEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ROTATES =
            SynchedEntityData.defineId(HullbackWalkableEntity.class, EntityDataSerializers.BOOLEAN);

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
        this.entityData.set(DATA_OWNER_ID, owner == null ? -1 : owner.getId());
    }

    public int getOwnerId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    public int getAnchor() {
        return this.entityData.get(DATA_ANCHOR);
    }

    public void setAnchor(int slot, float localX, float localY, float localZ, boolean rotates) {
        this.entityData.set(DATA_ANCHOR, slot);
        this.entityData.set(DATA_LOCAL_X, localX);
        this.entityData.set(DATA_LOCAL_Y, localY);
        this.entityData.set(DATA_LOCAL_Z, localZ);
        this.entityData.set(DATA_ROTATES, rotates);
    }

    public boolean rotatesWithPart() {
        return this.entityData.get(DATA_ROTATES);
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            followOwner();
            return;
        }
        if (this.isRemoved()) return;
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

    private boolean followOwner() {
        int anchor = this.entityData.get(DATA_ANCHOR);
        if (anchor < 0) return false;
        if (!(this.level().getEntity(this.entityData.get(DATA_OWNER_ID)) instanceof HullbackEntity whale)) return false;
        if (!whale.arePartsInitialized()) return false;
        Vec3 pos = whale.deckTilePos(anchor,
                this.entityData.get(DATA_LOCAL_X),
                this.entityData.get(DATA_LOCAL_Y),
                this.entityData.get(DATA_LOCAL_Z),
                this.entityData.get(DATA_ROTATES));
        this.setPos(pos.x, pos.y, pos.z);
        return true;
    }

    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (followOwner()) return;
        this.setPos(x, y, z);
        this.setRot(yRot, xRot);
    }

    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpTo(x, y, z, yRot, xRot, steps);
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
        builder.define(DATA_OWNER_ID, -1);
        builder.define(DATA_ANCHOR, -1);
        builder.define(DATA_LOCAL_X, 0f);
        builder.define(DATA_LOCAL_Y, 0f);
        builder.define(DATA_LOCAL_Z, 0f);
        builder.define(DATA_ROTATES, false);
        builder.define(DATA_WIDTH, this.getBbWidth());
        builder.define(DATA_HEIGHT, this.getBbHeight());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {}
}
