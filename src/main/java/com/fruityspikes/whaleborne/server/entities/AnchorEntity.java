package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AnchorEntity extends WhaleWidgetEntity{
    private boolean isDown = false;
    private boolean isClosed = true;
    public AnchorHeadEntity anchorHead;
    private float sinkSpeed = 0.05f;
    boolean hasHitTheBottom = false;
    int coolDown = 0;

    private static final EntityDataAccessor<Vector3f> DATA_HEAD_POSITION = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> DATA_IS_CLOSED = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_DOWN = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_ANCHOR_HEAD_UUID = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public AnchorEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.ANCHOR.get());
    }
    public void setDown(boolean down) {
        isDown = down;
    }

    public boolean isClosed() {
        return this.entityData.get(DATA_IS_CLOSED);
    }

    public boolean isDown() {
        return this.entityData.get(DATA_IS_DOWN);
    }
    public void setClosed(boolean closed) {
        isClosed = closed;
    }
    public Vector3f getHeadPos(){
        return this.entityData.get(DATA_HEAD_POSITION);
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_CLOSED, true);
        this.entityData.define(DATA_IS_DOWN, false);
        this.entityData.define(DATA_ANCHOR_HEAD_UUID, Optional.empty());
        this.entityData.define(DATA_HEAD_POSITION, new Vector3f(0, 0, 0));
    }

    @Override
    public void tick() {
        super.tick();

        if (!isClosed() && getVehicle() != null) {
            getVehicle().setPos(getVehicle().xo, getVehicle().yo, getVehicle().zo);
            getVehicle().setYRot(getVehicle().yRotO);
            getVehicle().setXRot(getVehicle().xRotO);
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
        if (!isClosed() && this.anchorHead == null) {
            relinkAnchorHead();
        }

        if (this.anchorHead != null && getVehicle() != null) {
            handleAnchorMovement();
        }
    }

    private void relinkAnchorHead() {
        getAnchorHeadUUID().ifPresent(uuid -> {
            List<Entity> entities = this.level().getEntities(this,
                    this.getBoundingBox().inflate(20, 100, 20),
                    entity -> entity.getUUID().equals(uuid) && entity instanceof AnchorHeadEntity
            );

            if (!entities.isEmpty()) {
                this.anchorHead = (AnchorHeadEntity) entities.get(0);
            } else {
                // Couldn't find anchor head, reset state
                close();
            }
        });
    }

    private void handleAnchorMovement() {
        if (isDown()) {
            if (!this.level().getBlockState(BlockPos.containing(this.anchorHead.position().add(0, 1, 0))).isSolid()) {
                this.sinkSpeed -= 0.5f;
                playSound(SoundEvents.CHAIN_STEP, 1.0f, 1.0f);
                this.anchorHead.moveTo(this.position().add(0, this.sinkSpeed, 0));
                this.hasHitTheBottom = false;
            } else if (!this.hasHitTheBottom) {
                playSound(SoundEvents.ANVIL_LAND, 1.0f, 0.9f);

                if(this.getVehicle() != null && this.getVehicle() instanceof HullbackEntity hullback){
                    for (int side : new int[]{-1, 1}) {
                        Vec3 particlePos = hullback.getPartPos(1).add(new Vec3(3.5*side, 2, 0).yRot(hullback.getPartYRot(1)));
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
                    }
                    hullback.playSound(WBSoundRegistry.HULLBACK_MAD.get());
                }

                this.hasHitTheBottom = true;
            }
        } else {
            this.sinkSpeed += 0.3f;
            playSound(SoundEvents.CHAIN_STEP, 1.0f, 1.0f);
            this.anchorHead.moveTo(this.position().add(0, this.sinkSpeed, 0));
        }

        this.anchorHead.setXRot(Math.min(0, -90 + (float) this.position().distanceTo(this.anchorHead.position()) * 30));
        this.anchorHead.setYRot(this.getVehicle().getYRot());

        if (this.position().y < this.anchorHead.position().y) {
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
        if (!level().isClientSide && anchorHead != null) {
            this.entityData.set(DATA_HEAD_POSITION, new Vector3f((float) anchorHead.getX(), (float) anchorHead.getY(), (float) anchorHead.getZ()));
        } else if (anchorHead == null) {
            this.entityData.set(DATA_HEAD_POSITION, new Vector3f((float)this.getX(), (float)this.getY(), (float)this.getZ()));
        }
    }
    private void deployAnchor() {
        this.entityData.set(DATA_IS_CLOSED, false);
        this.entityData.set(DATA_IS_DOWN, true);

        double distanceInFront = -1.0;
        double x = this.getX() + distanceInFront * Math.sin(Math.toRadians(-this.getYRot()));
        double z = this.getZ() + distanceInFront * Math.cos(Math.toRadians(this.getYRot()));

        this.anchorHead = new AnchorHeadEntity(WBEntityRegistry.ANCHOR_HEAD.get(), this.level());
        this.anchorHead.moveTo(x, this.getY() - 0.2, z, this.getYRot(), -90);
        this.level().addFreshEntity(this.anchorHead);

        this.entityData.set(DATA_ANCHOR_HEAD_UUID, Optional.of(this.anchorHead.getUUID()));
        playSound(SoundEvents.CHAIN_PLACE, 1.0f, 0.9f);
    }

    private void toggleAnchorState() {
        boolean newDownState = !isDown();
        this.entityData.set(DATA_IS_DOWN, newDownState);
        playSound(newDownState ? SoundEvents.CHAIN_PLACE : SoundEvents.CHAIN_BREAK, 1.0f, 1.0f);
    }

    public void close() {
        if (this.anchorHead != null) {
            this.anchorHead.discard();
            this.anchorHead = null;
        }

        this.entityData.set(DATA_IS_CLOSED, true);
        this.entityData.set(DATA_IS_DOWN, false);
        this.entityData.set(DATA_ANCHOR_HEAD_UUID, Optional.empty());
        this.entityData.set(DATA_HEAD_POSITION,
                new Vector3f((float)this.getX(),
                        (float)this.getY(),
                        (float)this.getZ()));
        playSound(SoundEvents.NETHERITE_BLOCK_HIT, 0.7f, 1.2f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_IS_CLOSED, tag.getBoolean("isClosed"));
        this.entityData.set(DATA_IS_DOWN, tag.getBoolean("isDown"));
        this.hasHitTheBottom = tag.getBoolean("hasHitTheBottom");
        this.sinkSpeed = tag.getFloat("sinkSpeed");

        if (tag.hasUUID("anchorHeadUUID")) {
            this.entityData.set(DATA_ANCHOR_HEAD_UUID, Optional.of(tag.getUUID("anchorHeadUUID")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("isClosed", isClosed());
        tag.putBoolean("isDown", isDown());
        tag.putBoolean("hasHitTheBottom", this.hasHitTheBottom);
        tag.putFloat("sinkSpeed", this.sinkSpeed);

        getAnchorHeadUUID().ifPresent(uuid -> tag.putUUID("anchorHeadUUID", uuid));
    }

    public Optional<UUID> getAnchorHeadUUID() {
        return this.entityData.get(DATA_ANCHOR_HEAD_UUID);
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
