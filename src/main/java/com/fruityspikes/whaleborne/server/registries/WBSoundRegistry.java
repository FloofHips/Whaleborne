package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;


import java.util.function.Supplier;

public class WBSoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Whaleborne.MODID);

    public static final Supplier<SoundEvent> ORGAN = SOUND_EVENTS.register(
            "block.barnacle.organ",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "block.barnacle.organ"))
    );

    public static final Supplier<SoundEvent> HULLBACK_DEATH = SOUND_EVENTS.register(
            "entity.hullback.death",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "entity.hullback.death"))
    );
    public static final Supplier<SoundEvent> HULLBACK_HURT = SOUND_EVENTS.register(
            "entity.hullback.hurt",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "entity.hullback.hurt"))
    );
    public static final Supplier<SoundEvent> HULLBACK_TAME = SOUND_EVENTS.register(
            "entity.hullback.tame",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "entity.hullback.tame"))
    );
    public static final Supplier<SoundEvent> HULLBACK_SWIM = SOUND_EVENTS.register(
            "entity.hullback.swim",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "entity.hullback.swim"))
    );
    public static final Supplier<SoundEvent> HULLBACK_BREATHE = SOUND_EVENTS.register(
            "entity.hullback.breathe",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "entity.hullback.breathe"))
    );
    public static final Supplier<SoundEvent> HULLBACK_AMBIENT = SOUND_EVENTS.register(
            "entity.hullback.ambient",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "entity.hullback.ambient"))
    );
    public static final Supplier<SoundEvent> HULLBACK_HAPPY = SOUND_EVENTS.register(
            "entity.hullback.happy",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "entity.hullback.happy"))
    );
    public static final Supplier<SoundEvent> HULLBACK_MAD = SOUND_EVENTS.register(
            "entity.hullback.mad",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "entity.hullback.mad"))
    );
}
