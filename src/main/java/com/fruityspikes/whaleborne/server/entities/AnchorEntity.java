package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class AnchorEntity extends WhaleWidgetEntity{
    private boolean isDown;
    private boolean isClosed;
    private AnchorHeadEntity anchorHead;
    private float sinkSpeed = 0.05f;
    public AnchorEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, Items.PAPER);
    }
    public void setDown(boolean down) {
        isDown = down;
    }

    public boolean getDown() {
        return isDown;
    }

    public boolean getClosed() {
        return isClosed;
    }
    public void setClosed(boolean closed) {
        isClosed = closed;
    }
    @Override
    public void tick() {
        super.tick();

        if(anchorHead!=null) {
            if (isDown) {
                this.anchorHead.setDeltaMovement(0, -0.1, 0);
            } else {
                this.anchorHead.setDeltaMovement(0, 0.1, 0);
            }

            if (this.position().distanceTo(anchorHead.position()) < 1)
                close();
        }
//        if (isDown && anchorHead != null) {
//            double distance = distanceTo(anchorHead);
//
//            float currentSinkSpeed = sinkSpeed * (float)(distance / 10.0);
//
//            Vec3 motion = anchorHead.getDeltaMovement();
//            anchorHead.setDeltaMovement(motion.x, -currentSinkSpeed, motion.z);
//
//
//            Vec3 direction = position().subtract(anchorHead.position()).normalize();
//            double maxDistance = 10.0;
//            if (distance > maxDistance) {
//                Vec3 newPos = position().subtract(direction.scale(maxDistance - 0.5));
//                anchorHead.setPos(newPos.x, newPos.y, newPos.z);
//            }
//        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        toggleDown();
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void toggleDown() {
        if(isClosed){
            isClosed = false;
            isDown = true;

            anchorHead = new AnchorHeadEntity(WBEntityRegistry.ANCHOR_HEAD.get(), level());
            anchorHead.setPos(this.getX(), this.getY() - 0.5, this.getZ());
            level().addFreshEntity(anchorHead);
        }
        else{
            isDown = !isDown;
        }
    }

    public void close(){
        anchorHead.discard();
        anchorHead = null;
        isClosed = true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        isDown = compoundTag.getBoolean("IsDown");
        if (compoundTag.contains("AnchorHead")) {
            CompoundTag headTag = compoundTag.getCompound("AnchorHead");
            UUID headUUID = headTag.getUUID("UUID");

            List<Entity> entities = level().getEntities(this,
                    this.getBoundingBox().inflate(20),
                    entity -> entity.getUUID().equals(headUUID) && entity instanceof AnchorHeadEntity
            );

            if (!entities.isEmpty()) {
                anchorHead = (AnchorHeadEntity) entities.get(0);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putBoolean("IsDown", isDown);
        if (anchorHead != null) {
            CompoundTag headTag = new CompoundTag();
            anchorHead.save(headTag);
            compoundTag.put("AnchorHead", headTag);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (anchorHead != null) {
            anchorHead.discard();
        }
    }
}
