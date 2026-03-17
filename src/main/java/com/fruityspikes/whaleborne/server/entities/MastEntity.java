package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MastEntity extends RideableWhaleWidgetEntity{
    public MastEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.MAST.get());
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0, this.getBbHeight() - 16.0f, 0);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        Vec3 offset = getPassengerAttachmentPoint(passenger, this.getDimensions(this.getPose()), 1.0F)
                .add(0, -1.0, 0);
        // Rotate the vertical offset by the mast's pitch/yaw (cannon-style)
        // so the rider follows the arc of the tail's rotation, amplified by the mast's height
        offset = offset.xRot(this.getXRot() * Mth.DEG_TO_RAD).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
        callback.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.getPassengers().isEmpty() && !(this.getFirstPassenger() instanceof Player)) {
            LivingEntity entity = this.getFirstPassenger();
            this.ejectPassengers();
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.5, 0));
            return InteractionResult.SUCCESS;
        }

        AABB searchArea = new AABB(this.getX() - 7, this.getY() - 7, this.getZ() - 7,
                                   this.getX() + 7, this.getY() + 7, this.getZ() + 7);
        List<Mob> nearbyMobs = level().getEntitiesOfClass(Mob.class, searchArea);

        for (Mob mob : nearbyMobs) {
            if (mob.getLeashHolder() == player) {
                if (mob.startRiding(this)) {
                    this.playSound(SoundEvents.LADDER_STEP);
                    mob.dropLeash(true, !player.getAbilities().instabuild);
                    return InteractionResult.SUCCESS;
                } else {
                    return InteractionResult.FAIL;
                }
            }
        }
        return super.interact(player, hand);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}
