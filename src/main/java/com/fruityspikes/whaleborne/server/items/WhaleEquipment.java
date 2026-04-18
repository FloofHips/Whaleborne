package com.fruityspikes.whaleborne.server.items;

import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class WhaleEquipment extends Item {
    private Supplier<EntityType<?>>  entity;
    private SoundEvent soundEvent;
    public WhaleEquipment(Supplier<EntityType<?>> entity, Properties properties) {
        super(properties);
        this.entity = entity;
        this.soundEvent = WBSoundRegistry.WIDGET_WOODEN_PLACE.get();
    }

    public WhaleEquipment(Supplier<EntityType<?>> entity, SoundEvent soundEvent, Properties properties) {
        super(properties);
        this.entity = entity;
        this.soundEvent = soundEvent;
    }

    public EntityType<?> getEntity() {
        return entity.get();
    }
    public SoundEvent getPlaceSound() { return soundEvent;}
}
