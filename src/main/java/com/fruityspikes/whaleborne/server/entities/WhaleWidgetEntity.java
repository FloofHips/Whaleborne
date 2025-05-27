package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class WhaleWidgetEntity extends Entity {
    public WhaleWidgetEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();
        if(!this.isPassenger()){
            kill();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(player.isSecondaryUseActive())
            this.unRide();
        return super.interact(player, hand);
    }
}
