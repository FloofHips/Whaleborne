package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class DeckRiderClaims {

    private static final int FRESH_TICKS = 3;

    private static final int KEEP_TICKS = 100;

    private static final Map<Integer, Claim> CLAIMS = new HashMap<>();

    private DeckRiderClaims() {
    }

    public record Claim(int whaleId, int tileId, Vec3 offset, long stamp) {
    }

    public static void set(int riderId, int whaleId, int tileId, Vec3 offset, long gameTime) {
        CLAIMS.put(riderId, new Claim(whaleId, tileId, offset, gameTime));
    }

    public static void clear(int riderId) {
        CLAIMS.remove(riderId);
    }

    public static Claim fresh(int riderId, long gameTime) {
        Claim claim = last(riderId, gameTime);
        return claim == null || Math.abs(gameTime - claim.stamp()) > FRESH_TICKS ? null : claim;
    }

    public static Claim last(int riderId, long gameTime) {
        Claim claim = CLAIMS.get(riderId);
        if (claim == null) {
            return null;
        }
        if (Math.abs(gameTime - claim.stamp()) > KEEP_TICKS) {
            CLAIMS.remove(riderId);
            return null;
        }
        return claim;
    }
}
