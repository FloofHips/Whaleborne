package com.fruityspikes.whaleborne.server.entities;

import com.mojang.datafixers.optics.Prism;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MastEntity extends RideableWhaleWidgetEntity{
    public MastEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, Items.STICK);
    }

}
