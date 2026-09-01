package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.world.phys.Vec3;

public final class DeckRiderLocalClaim {

    private static int riderId = -1;
    private static int whaleId = -1;
    private static int tileId = -1;
    private static Vec3 offset = Vec3.ZERO;
    private static long stamp = Long.MIN_VALUE;

    private static final long MAX_AGE = 2L;

    private DeckRiderLocalClaim() {
    }

    public static void set(int rider, int whale, int tile, Vec3 deckLocal, long gameTime) {
        riderId = rider;
        whaleId = whale;
        tileId = tile;
        offset = deckLocal;
        stamp = gameTime;
    }

    public static void clear() {
        riderId = -1;
        whaleId = -1;
        tileId = -1;
        stamp = Long.MIN_VALUE;
    }

    public static boolean isCurrent(long gameTime) {
        return whaleId >= 0 && tileId >= 0 && gameTime - stamp >= 0L && gameTime - stamp <= MAX_AGE;
    }

    public static int riderId() {
        return riderId;
    }

    public static int whaleId() {
        return whaleId;
    }

    public static int tileId() {
        return tileId;
    }

    public static Vec3 offset() {
        return offset;
    }
}
