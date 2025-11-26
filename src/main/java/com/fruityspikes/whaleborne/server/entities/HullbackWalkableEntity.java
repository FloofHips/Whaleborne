package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public class HullbackWalkableEntity extends Entity {

    public HullbackWalkableEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

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
    public void tick() {
        super.tick();
        if (this.tickCount % 200 == 0) {
            if (this.level().getEntities(this, this.getBoundingBox().inflate(1F, 1F, 1F), EntitySelector.NO_CREATIVE_OR_SPECTATOR.and((entity) -> (entity instanceof HullbackPartEntity))).isEmpty())
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

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
