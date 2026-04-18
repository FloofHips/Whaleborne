package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class WBSoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Whaleborne.MODID);

    public static final Supplier<SoundEvent> ORGAN = register("block.barnacle.organ");
    public static final Supplier<SoundEvent> HULLBACK_DEATH = register("entity.hullback.death");
    public static final Supplier<SoundEvent> HULLBACK_HURT = register("entity.hullback.hurt");
    public static final Supplier<SoundEvent> HULLBACK_TAME = register("entity.hullback.tame");
    public static final Supplier<SoundEvent> HULLBACK_SWIM = register("entity.hullback.swim");
    public static final Supplier<SoundEvent> HULLBACK_BREATHE = register("entity.hullback.breathe");
    public static final Supplier<SoundEvent> HULLBACK_AMBIENT = register("entity.hullback.ambient");
    public static final Supplier<SoundEvent> HULLBACK_HAPPY = register("entity.hullback.happy");
    public static final Supplier<SoundEvent> HULLBACK_MAD = register("entity.hullback.mad");
    public static final Supplier<SoundEvent> HULLBACK_ORGAN = register("entity.hullback.organ");

    public static final Supplier<SoundEvent> WIDGET_WOODEN_PLACE = register("entity.widget_wooden.place");
    public static final Supplier<SoundEvent> WIDGET_WOODEN_BREAK = register("entity.widget_wooden.break");
    public static final Supplier<SoundEvent> WIDGET_WOODEN_HIT = register("entity.widget_wooden.hit");
    public static final Supplier<SoundEvent> WIDGET_METAL_PLACE = register("entity.widget_metal.place");
    public static final Supplier<SoundEvent> WIDGET_METAL_BREAK = register("entity.widget_metal.break");
    public static final Supplier<SoundEvent> WIDGET_METAL_HIT = register("entity.widget_metal.hit");
    public static final Supplier<SoundEvent> WIDGET_RIDE = register("entity.widget.ride");
    public static final Supplier<SoundEvent> CANNON_SHOOT = register("entity.cannon.shoot");
    public static final Supplier<SoundEvent> CANNON_NO_FUEL = register("entity.cannon.no_fuel");
    public static final Supplier<SoundEvent> CANNON_SHOOT_FAIL = register("entity.cannon.shoot_fail");
    public static final Supplier<SoundEvent> CANNON_SHOOT_ARROW = register("entity.cannon.shoot_arrow");
    public static final Supplier<SoundEvent> SAIL_WIND = register("entity.sail.wind");
    public static final Supplier<SoundEvent> SAIL_COLOR = register("entity.sail.color");
    public static final Supplier<SoundEvent> SAIL_CLEAN = register("entity.sail.clean");
    public static final Supplier<SoundEvent> HELM_TURN = register("entity.helm.turn");
    public static final Supplier<SoundEvent> HELM_TURN_FAIL = register("entity.helm.turn_fail");
    public static final Supplier<SoundEvent> ANCHOR_EXTEND = register("entity.anchor.extend");
    public static final Supplier<SoundEvent> ANCHOR_RETRACT = register("entity.anchor.retract");
    public static final Supplier<SoundEvent> ANCHOR_FINISH = register("entity.anchor.finish");

    public static final Supplier<SoundEvent> MUSIC_DISC_THE_PLANK = register("music_disc.the_plank");

    public static final SoundType BARNACLE_BLOCK = register("barnacle_block", 1, 1.35F);


    private static SoundType register(String name, float volume, float pitch) {
        return new ForgeSoundType(volume, pitch, register("block." + name + ".break"), register("block." + name + ".step"), register("block." + name + ".place"), register("block." + name + ".hit"), register("block." + name + ".fall"));
    }


    public static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(Whaleborne.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
