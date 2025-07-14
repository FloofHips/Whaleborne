package com.fruityspikes.whaleborne.server.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class WBSmokeProvider implements ParticleProvider<SimpleParticleType> {
    private final SpriteSet sprites;

    public WBSmokeProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        return new WBSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
    }
}
