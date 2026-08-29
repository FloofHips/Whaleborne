package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public class HullbackWalkableEntity extends Entity {

    private static final int OWNER_CHECK_INTERVAL = 32;
    private static final int OWNER_CHECK_GRACE_TICKS = 20;
    private static final double OWNER_MAX_DIST_SQ = 24.0 * 24.0;

    private java.util.UUID ownerUuid;


    public HullbackWalkableEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /*
    protected void recalculateBoundingBox() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        if (this.getMovementEmission().emitsAnything()) {
            this.setBoundingBox(new AABB(x, y, z, x, y, z));
        } else {
            double halfWidth = 2.5;
            this.setBoundingBox(new AABB(
                    x - halfWidth, y, z - halfWidth,
                    x + halfWidth, y + 1, z + halfWidth
            ));
        }
    }
    */
    public void setOwner(Entity owner) {
        this.ownerUuid = owner == null ? null : owner.getUUID();
    }

    public java.util.UUID getOwnerUuid() {
        return this.ownerUuid;
    }

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

    public boolean isPickable() {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean mayInteract(Level level, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;// this.getMovementEmission().emitsAnything();
    }
    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {}
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {}

}
