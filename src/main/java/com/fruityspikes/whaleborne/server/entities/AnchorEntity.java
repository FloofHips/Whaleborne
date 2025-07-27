package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.Optional;

public class AnchorEntity extends WhaleWidgetEntity {
    private static final EntityDataAccessor<Boolean> DEPLOYED = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LOCKED = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> TARGET_POS = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Float> PROGRESS = SynchedEntityData.defineId(AnchorEntity.class, EntityDataSerializers.FLOAT);

    private BlockPos startPos;
    private int cooldown = 0;
    private static final float DEPLOY_SPEED = 0.02f;

    public AnchorEntity(EntityType<?> type, Level level) {
        super(type, level, WBItemRegistry.ANCHOR.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DEPLOYED, false);
        entityData.define(LOCKED, false);
        entityData.define(TARGET_POS, Optional.empty());
        entityData.define(PROGRESS, 0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (cooldown > 0) cooldown--;

        if (isLocked() && getVehicle() instanceof HullbackEntity hullback) {
            hullback.stopMoving();
        }

        if (isDeployed()) {
            updateAnchorMovement();
        }
    }

    private void updateAnchorMovement() {
        Optional<BlockPos> targetOpt = entityData.get(TARGET_POS);
        if (targetOpt.isEmpty()) return;

        BlockPos target = targetOpt.get();
        float progress = entityData.get(PROGRESS);

        if (progress < 1.0f && isDeployed() && !isLocked()) {
            float newProgress = Math.min(progress + DEPLOY_SPEED, 1.0f);
            entityData.set(PROGRESS, newProgress);

            if (tickCount % 5 == 0) {
                playChainSound();
            }

            if (newProgress >= 1.0f) {
                setLocked(true);
                playAnchorHitEffects();
            }
        } else if (progress > 0f && !isDeployed()) {
            float newProgress = Math.max(progress - 0.01f, 0f);
            entityData.set(PROGRESS, newProgress);

            if (tickCount % 5 == 0) {
                playChainSound();
            }
        }
    }

    public Vec3 getAnchorRenderPosition(float partialTicks) {
        Optional<BlockPos> targetOpt = entityData.get(TARGET_POS);
        if (targetOpt.isEmpty()) return null;

        BlockPos target = targetOpt.get();
        float progress = Mth.lerp(partialTicks,
                entityData.get(PROGRESS) - DEPLOY_SPEED,
                entityData.get(PROGRESS));

        progress = Mth.clamp(progress, 0, 1);
        Vec3 start = position();
        Vec3 end = Vec3.atBottomCenterOf(target);

        return start.add(end.subtract(start).scale(progress));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (cooldown <= 0 && !level().isClientSide) {
            toggleAnchor();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void toggleAnchor() {
        if (isDeployed()) {
            retractAnchor();
        } else {
            deployAnchor();
        }
        cooldown = 10;
    }

    private void deployAnchor() {
        BlockPos seafloorPos = findSeafloorPosition();
        entityData.set(TARGET_POS, Optional.of(seafloorPos));
        entityData.set(PROGRESS, 0f);
        setDeployed(true);
        setLocked(false);
        playSound(SoundEvents.CHAIN_PLACE, 1.0f, 0.9f);
    }

    private void retractAnchor() {
        setDeployed(false);
        setLocked(false);
        playSound(SoundEvents.CHAIN_BREAK, 1.0f, 1.0f);

        if (getVehicle() instanceof HullbackEntity hullback) {
            hullback.stationaryTicks = 100;
        }
    }

    private BlockPos findSeafloorPosition() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                (int)getX(),
                (int)getY(),
                (int)getZ()
        );

        while (pos.getY() > level().getMinBuildHeight() &&
                !level().getBlockState(pos).isSolid()) {
            pos.move(Direction.DOWN);
        }

        return pos.immutable();
    }

    private void playAnchorHitEffects() {
        playSound(SoundEvents.ANVIL_LAND, 1.0f, 0.9f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    WBParticleRegistry.SMOKE.get(),
                    getX(), getY(), getZ(),
                    20, 0.2, 0.2, 0.2, 0.02
            );
        }
    }

    private void playChainSound() {
        playSound(SoundEvents.CHAIN_STEP, 0.5f, 1.0f + random.nextFloat() * 0.2f);
    }

    public boolean isDeployed() { return entityData.get(DEPLOYED); }
    public boolean isLocked() { return entityData.get(LOCKED); }
    public Optional<BlockPos> getTargetPos() { return entityData.get(TARGET_POS); }
    public float getProgress() { return entityData.get(PROGRESS); }

    private void setDeployed(boolean deployed) { entityData.set(DEPLOYED, deployed); }
    private void setLocked(boolean locked) { entityData.set(LOCKED, locked); }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setDeployed(tag.getBoolean("Deployed"));
        setLocked(tag.getBoolean("Locked"));
        entityData.set(PROGRESS, tag.getFloat("Progress"));

        if (tag.contains("TargetX")) {
            BlockPos pos = new BlockPos(
                    tag.getInt("TargetX"),
                    tag.getInt("TargetY"),
                    tag.getInt("TargetZ")
            );
            entityData.set(TARGET_POS, Optional.of(pos));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Deployed", isDeployed());
        tag.putBoolean("Locked", isLocked());
        tag.putFloat("Progress", getProgress());

        getTargetPos().ifPresent(pos -> {
            tag.putInt("TargetX", pos.getX());
            tag.putInt("TargetY", pos.getY());
            tag.putInt("TargetZ", pos.getZ());
        });
    }
}