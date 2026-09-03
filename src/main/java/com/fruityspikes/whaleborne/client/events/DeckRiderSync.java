package com.fruityspikes.whaleborne.client.events;

import com.fruityspikes.whaleborne.WhaleborneDebug;
import com.fruityspikes.whaleborne.network.DeckRiderSyncPayload;
import com.fruityspikes.whaleborne.server.entities.DeckRiderAnchors;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackWalkableEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class DeckRiderSync {

    private static final int STALE_TICKS = 10;
    private static final double SMOOTHING = 0.6;
    private static final double SNAP_SQR = 1.5 * 1.5;
    private static final double SETTLED = 1.0E-4;

    private static final Map<Integer, Rider> RIDERS = new HashMap<>();

    private DeckRiderSync() {
    }

    private static final class Rider {
        int whaleId;
        int tileId;
        Vec3 target = Vec3.ZERO;
        Vec3 current;
        long stamp;
        String reported = "";
    }

    public static void accept(DeckRiderSyncPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Rider rider = RIDERS.computeIfAbsent(payload.riderId(), id -> new Rider());
        if (rider.tileId != payload.tileId() || rider.whaleId != payload.whaleId()) {
            rider.current = null;
        }
        rider.whaleId = payload.whaleId();
        rider.tileId = payload.tileId();
        rider.target = new Vec3(payload.offX(), payload.offY(), payload.offZ());
        rider.stamp = level.getGameTime();
    }

    public static boolean holds(int riderId, long gameTime) {
        Rider state = RIDERS.get(riderId);
        return state != null && Math.abs(gameTime - state.stamp) <= STALE_TICKS;
    }

    public static void clear() {
        RIDERS.clear();
    }

    public static void resolve(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            RIDERS.clear();
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Rider>> it = RIDERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Rider> entry = it.next();
            Rider rider = entry.getValue();
            if (Math.abs(now - rider.stamp) > STALE_TICKS) {
                report(entry.getKey(), rider, "released");
                it.remove();
                continue;
            }
            String reason = place(level, minecraft, entry.getKey(), rider);
            report(entry.getKey(), rider, reason);
        }
    }

    private static String place(ClientLevel level, Minecraft minecraft, int riderId, Rider state) {
        Entity rider = level.getEntity(riderId);
        if (rider == null || rider == minecraft.player || rider.isControlledByLocalInstance()) {
            return "notremote";
        }
        if (rider.isPassenger() || rider.isSpectator() || !rider.isAlive()) {
            return "notstanding";
        }
        if (!(level.getEntity(state.tileId) instanceof HullbackWalkableEntity tile)
                || tile.getOwnerId() != state.whaleId) {
            return "notile";
        }
        if (!(level.getEntity(state.whaleId) instanceof HullbackEntity whale) || !whale.arePartsInitialized()) {
            return "nowhale";
        }

        Vec3 target = state.target;
        Vec3 current = state.current;
        if (current == null || current.distanceToSqr(target) > SNAP_SQR) {
            current = target;
        } else if (current.distanceToSqr(target) > SETTLED * SETTLED) {
            current = current.add(target.subtract(current).scale(SMOOTHING));
        }
        state.current = current;

        float yaw = whale.getYRot();
        Vec3 world = tile.position().add(DeckRiderAnchors.toDeckWorld(current, yaw));
        rider.setPos(world.x, world.y, world.z);
        rider.setOnGround(true);
        rider.fallDistance = 0.0F;
        return "deck";
    }

    private static void report(int riderId, Rider state, String reason) {
        if (reason.equals(state.reported)) {
            return;
        }
        state.reported = reason;
        WhaleborneDebug.log("decksync id=%d whale=%d tile=%d off=%.3f,%.3f,%.3f src=%s",
                riderId, state.whaleId, state.tileId,
                state.target.x, state.target.y, state.target.z, reason);
    }
}
