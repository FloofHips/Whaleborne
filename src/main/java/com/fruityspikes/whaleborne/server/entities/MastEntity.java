package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.mojang.datafixers.optics.Prism;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Iterator;

public class MastEntity extends RideableWhaleWidgetEntity{
    public MastEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.MAST.get());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(this.getPassengers().size() > 0 && !(this.getFirstPassenger() instanceof Player player1)){
            LivingEntity entity = this.getFirstPassenger();
            this.ejectPassengers();
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.5, 0));

            return InteractionResult.SUCCESS;
        }

        int i = (int) this.getX();
        int j = (int) this.getY();
        int k = (int) this.getZ();
        Iterator var10 = level().getEntitiesOfClass(Mob.class, new AABB((double)i - 7.0, (double)j - 7.0, (double)k - 7.0, (double)i + 7.0, (double)j + 7.0, (double)k + 7.0)).iterator();

        while(var10.hasNext()) {
            Mob mob = (Mob)var10.next();
            if (mob.getLeashHolder() == player) {
                if(mob.startRiding(this)){
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
}
