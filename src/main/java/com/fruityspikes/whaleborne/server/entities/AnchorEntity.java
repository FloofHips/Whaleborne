package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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
        isDown = false;
        isClosed = true;
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

        if (!isClosed && getVehicle() != null) {
            getVehicle().setPos(getVehicle().xOld, getVehicle().yOld, getVehicle().zOld);
            getVehicle().setYRot(getVehicle().yRotO);
            getVehicle().setXRot(getVehicle().xRotO);
        }

        if(anchorHead!=null) {
            if (isDown) {
                if (!level().getBlockState(BlockPos.containing(anchorHead.position().add(0, 0, 0))).isSolid()) {
                    sinkSpeed -= 0.1f;
                    playSound(SoundEvents.GRINDSTONE_USE);
                    this.anchorHead.moveTo(this.position().add(0, sinkSpeed, 0));
                }
            } else {
                sinkSpeed += 0.1f;
                playSound(SoundEvents.GRINDSTONE_USE);
                this.anchorHead.moveTo(this.position().add(0, sinkSpeed, 0));
            }
            this.anchorHead.setXRot(Math.max(0, 90 - (float) this.position().distanceTo(anchorHead.position())*15));
            this.anchorHead.setYRot(this.getVehicle().getYRot());

            if (this.position().distanceTo(anchorHead.position()) < 0.1 || this.position().y < anchorHead.position().y)
                close();
        }
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

            playSound(SoundEvents.CHAIN_PLACE);
            anchorHead = new AnchorHeadEntity(WBEntityRegistry.ANCHOR_HEAD.get(), level());
            anchorHead.setPos(this.getX(), this.getY() - 0.2, this.getZ());
            level().addFreshEntity(anchorHead);
        }
        else{
            playSound(SoundEvents.CHAIN_BREAK);
            isDown = !isDown;
        }
    }

    public void close(){
        anchorHead.discard();
        anchorHead = null;
        isClosed = true;
        playSound(SoundEvents.NETHER_BRICKS_HIT);
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
        close();
    }
}
