package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class DeckRiderAnchors {

    private static final int STALE_TICKS = 3;

    private static final Map<Integer, Anchor> ANCHORS = new HashMap<>();
    private static long swept = Long.MIN_VALUE;

    private DeckRiderAnchors() {
    }

    public record Anchor(Entity rider, HullbackEntity whale, HullbackWalkableEntity tile, long stamp) {
    }

    public static void set(Entity rider, HullbackEntity whale, HullbackWalkableEntity tile) {
        if (tile == null) {
            clear(rider);
            return;
        }
        long now = whale.level().getGameTime();
        sweep(now);
        ANCHORS.put(rider.getId(), new Anchor(rider, whale, tile, now));
    }

    public static void clear(Entity rider) {
        ANCHORS.remove(rider.getId());
    }

    public static List<Anchor> live(long gameTime) {
        sweep(gameTime);
        return new ArrayList<>(ANCHORS.values());
    }

    public static Anchor current(Entity rider, long gameTime) {
        Anchor anchor = ANCHORS.get(rider.getId());
        if (anchor == null || anchor.rider() != rider || anchor.whale().level() != rider.level()) {
            return null;
        }
        if (Math.abs(gameTime - anchor.stamp()) > STALE_TICKS
                || anchor.whale().isRemoved() || anchor.tile().isRemoved()) {
            return null;
        }
        return anchor;
    }

    private static void sweep(long now) {
        if (now == swept) {
            return;
        }
        swept = now;
        Iterator<Anchor> it = ANCHORS.values().iterator();
        while (it.hasNext()) {
            Anchor anchor = it.next();
            if (Math.abs(now - anchor.stamp()) > STALE_TICKS
                    || anchor.rider().level() != anchor.whale().level()
                    || anchor.rider().isRemoved() || anchor.whale().isRemoved() || anchor.tile().isRemoved()) {
                it.remove();
            }
        }
    }

    public static float deckYaw(Anchor anchor) {
        return anchor.whale().getYRot();
    }

    public static Vec3 toDeckLocal(Vec3 world, float yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(world.x * cos + world.z * sin, world.y, world.z * cos - world.x * sin);
    }

    public static Vec3 toDeckWorld(Vec3 local, float yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(local.x * cos - local.z * sin, local.y, local.x * sin + local.z * cos);
    }
}
