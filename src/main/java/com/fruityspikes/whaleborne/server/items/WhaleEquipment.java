package com.fruityspikes.whaleborne.server.items;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class WhaleEquipment extends Item {
    private Supplier<EntityType<?>>  entity;
    public WhaleEquipment(Supplier<EntityType<?>> entity, Properties properties) {
        super(properties);
        this.entity = entity;
    }

    public EntityType<?> getEntity() {
        return entity.get();
    }
}
