package com.fruityspikes.whaleborne.client.events;

import com.fruityspikes.whaleborne.Config;
import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces hullback ambient sounds client-side so each player's volume preference
 * applies locally without affecting the server's fixed-volume emission.
 */
@Mod.EventBusSubscriber(modid = Whaleborne.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientAudioHandler {

    private static final String SOUND_PREFIX = "entity.hullback.";

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance original = event.getSound();
        if (original == null) return;

        ResourceLocation soundLocation = original.getLocation();
        if (!soundLocation.getNamespace().equals(Whaleborne.MODID)) return;
        if (!soundLocation.getPath().startsWith(SOUND_PREFIX)) return;

        float userVolume = Config.soundDistance > 0 ? (float) Config.soundDistance : 3.0f;
        if (userVolume <= 0.01f) {
            event.setSound(null);
            return;
        }

        event.setSound(new VolumeOverrideSound(original, userVolume));
    }

    private static class VolumeOverrideSound implements SoundInstance {
        private final SoundInstance original;
        private final float customVolume;

        VolumeOverrideSound(SoundInstance original, float volume) {
            this.original = original;
            this.customVolume = volume;
        }

        @Override public ResourceLocation getLocation() { return original.getLocation(); }
        @Override public WeighedSoundEvents resolve(SoundManager manager) { return original.resolve(manager); }
        @Override public Sound getSound() { return original.getSound(); }
        @Override public SoundSource getSource() { return original.getSource(); }
        @Override public boolean isLooping() { return original.isLooping(); }
        @Override public boolean isRelative() { return original.isRelative(); }
        @Override public int getDelay() { return original.getDelay(); }
        @Override public float getVolume() { return this.customVolume; }
        @Override public float getPitch() { return original.getPitch(); }
        @Override public double getX() { return original.getX(); }
        @Override public double getY() { return original.getY(); }
        @Override public double getZ() { return original.getZ(); }
        @Override public Attenuation getAttenuation() { return original.getAttenuation(); }
    }
}
