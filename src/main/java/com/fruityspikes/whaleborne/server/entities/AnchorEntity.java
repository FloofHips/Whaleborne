package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Optional;

public class AnchorEntity extends WhaleWidgetEntity {
    private float sinkSpeed = 0.05f;
    boolean hasHitTheBottom = false;
    int coolDown = 0;
    private BlockPos anchorHeadPosition = BlockPos.ZERO;

    private static final EntityDataAccessor<BlockPos> DATA_HEAD_POSITION = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> DATA_IS_CLOSED = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_DOWN = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BOOLEAN);

    public AnchorEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.ANCHOR.get());
    }

    public void setDown(boolean down) {
        this.entityData.set(DATA_IS_DOWN, down);
    }

    public boolean isClosed() {
        return this.entityData.get(DATA_IS_CLOSED);
    }

    public boolean isDown() {
        return this.entityData.get(DATA_IS_DOWN);
    }

    public void setClosed(boolean closed) {
        this.entityData.set(DATA_IS_CLOSED, closed);
    }

    public BlockPos getHeadPos() {
        return this.entityData.get(DATA_HEAD_POSITION);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_CLOSED, true);
        this.entityData.define(DATA_IS_DOWN, false);
        this.entityData.define(DATA_HEAD_POSITION, BlockPos.ZERO);
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (!isClosed() && getVehicle() != null && getVehicle() instanceof HullbackEntity hullback) {
            hullback.stopMoving();
        }

        if (this.coolDown > 0) {
            this.coolDown--;
        }

        if (!this.level().isClientSide) {
            handleServerTick();
            updateHeadPosition();
        }
    }

    private void handleServerTick() {
        if (!this.isClosed() && getVehicle() != null) {
            handleAnchorMovement();
        }
    }

    private void handleAnchorMovement() {
        Vec3 currentHeadPos = Vec3.atCenterOf(anchorHeadPosition);

        if (isDown()) {
            if (!this.level().getBlockState(anchorHeadPosition).isSolid()) {
                this.sinkSpeed -= 0.05f;
                playSound(SoundEvents.CHAIN_STEP, 1.0f, 1.0f);
                Vec3 newPos = currentHeadPos.add(0, this.sinkSpeed, 0);
                anchorHeadPosition = BlockPos.containing(newPos);
                this.hasHitTheBottom = false;
            } else if (!this.hasHitTheBottom) {
                playSound(SoundEvents.ANVIL_LAND, 1.0f, 0.9f);

                if (this.getVehicle() != null && this.getVehicle() instanceof HullbackEntity hullback) {
                    for (int side : new int[]{-1, 1}) {
                        Vec3 particlePos = hullback.getPartPos(1).add(new Vec3(3.5 * side, 2, 0).yRot(hullback.getPartYRot(1)));
                        double x = particlePos.x;
                        double y = particlePos.y;
                        double z = particlePos.z;

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    WBParticleRegistry.SMOKE.get(),
                                    x,
                                    y,
                                    z,
                                    20,
                                    0.2,
                                    0.2,
                                    0.2,
                                    0.02
                            );
                        }

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    ParticleTypes.BUBBLE,
                                    currentHeadPos.x,
                                    currentHeadPos.y+1,
                                    currentHeadPos.z,
                                    50,
                                    0.5,
                                    0.5,
                                    0.5,
                                    0.02
                            );
                        }
                    }
                    hullback.playSound(WBSoundRegistry.HULLBACK_MAD.get());
                }

                this.hasHitTheBottom = true;
            }
        } else {
            if (anchorHeadPosition == BlockPos.ZERO) {
                anchorHeadPosition = BlockPos.containing(this.position().add(0, -0.5, 0));
            }

            this.sinkSpeed += 0.03f;
            playSound(SoundEvents.CHAIN_STEP, 1.0f, 1.0f);
            Vec3 newPos = currentHeadPos.add(0, this.sinkSpeed, 0);
            anchorHeadPosition = BlockPos.containing(newPos);
        }

        if (this.position().y < currentHeadPos.y) {
            close();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.coolDown <= 0 && !this.level().isClientSide) {
            toggleDown();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void toggleDown() {
        if (isClosed()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        WBParticleRegistry.SMOKE.get(),
                        this.getX(),
                        this.getY() + 1,
                        this.getZ(),
                        5,
                        0.1,
                        0.1,
                        0.1,
                        0.02
                );
            }
            deployAnchor();
        } else {
            toggleAnchorState();
        }
        this.coolDown = 20;
    }

    private void updateHeadPosition() {
        if (!level().isClientSide) {
            this.entityData.set(DATA_HEAD_POSITION, anchorHeadPosition);
        }
    }

    private void deployAnchor() {
        this.entityData.set(DATA_IS_CLOSED, false);
        this.entityData.set(DATA_IS_DOWN, true);
        this.sinkSpeed = -0.05f;

        double distanceInFront = -1.0;
        double x = this.getX() + distanceInFront * Math.sin(Math.toRadians(-this.getYRot()));
        double z = this.getZ() + distanceInFront * Math.cos(Math.toRadians(this.getYRot()));

        anchorHeadPosition = BlockPos.containing(x, this.getY() - 1, z);
        this.entityData.set(DATA_HEAD_POSITION, anchorHeadPosition);
        playSound(SoundEvents.CHAIN_PLACE, 1.0f, 0.9f);
    }

    private void toggleAnchorState() {
        boolean newDownState = !isDown();
        this.sinkSpeed = newDownState ? -0.05f : 0.05f;
        this.entityData.set(DATA_IS_DOWN, newDownState);
        playSound(newDownState ? SoundEvents.CHAIN_PLACE : SoundEvents.CHAIN_BREAK, 1.0f, 1.0f);
    }

    public void close() {
        this.entityData.set(DATA_IS_CLOSED, true);
        this.entityData.set(DATA_IS_DOWN, false);
        this.entityData.set(DATA_HEAD_POSITION, BlockPos.ZERO);
        this.sinkSpeed = 0.05f;
        anchorHeadPosition = BlockPos.ZERO;
        playSound(SoundEvents.NETHERITE_BLOCK_HIT, 0.7f, 1.2f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_IS_CLOSED, tag.getBoolean("isClosed"));
        this.entityData.set(DATA_IS_DOWN, tag.getBoolean("isDown"));
        this.hasHitTheBottom = tag.getBoolean("hasHitTheBottom");
        this.sinkSpeed = tag.getFloat("sinkSpeed");
        anchorHeadPosition = BlockPos.of(tag.getLong("anchorHeadPos"));
        this.entityData.set(DATA_HEAD_POSITION, anchorHeadPosition);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("isClosed", isClosed());
        tag.putBoolean("isDown", isDown());
        tag.putBoolean("hasHitTheBottom", this.hasHitTheBottom);
        tag.putFloat("sinkSpeed", this.sinkSpeed);
        tag.putLong("anchorHeadPos", anchorHeadPosition.asLong());
    }
}
