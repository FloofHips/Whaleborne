package com.fruityspikes.whaleborne.server.items;

import com.fruityspikes.whaleborne.server.entities.AnchorHeadEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class WhaleEquipment extends Item {
    private Supplier<EntityType<?>>  entity;
    private SoundEvent soundEvent;
    public WhaleEquipment(Supplier<EntityType<?>> entity, Properties properties) {
        super(properties);
        this.entity = entity;
        this.soundEvent = SoundEvents.WOOD_PLACE;
    }

    public WhaleEquipment(Supplier<EntityType<?>> entity, SoundEvent soundEvent, Properties properties) {
        super(properties);
        this.entity = entity;
        this.soundEvent = soundEvent;
    }

    public EntityType<?> getEntity() {
        return entity.get();
    }

    public SoundEvent getPlaceSound() {
        return soundEvent;
    }
}
