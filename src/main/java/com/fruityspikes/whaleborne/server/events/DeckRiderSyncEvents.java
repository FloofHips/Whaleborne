package com.fruityspikes.whaleborne.server.events;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.WhaleborneDebug;
import com.fruityspikes.whaleborne.network.DeckRiderSyncPayload;
import com.fruityspikes.whaleborne.server.entities.DeckRiderAnchors;
import com.fruityspikes.whaleborne.server.entities.DeckRiderClaims;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Whaleborne.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DeckRiderSyncEvents {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        List<DeckRiderAnchors.Anchor> anchors = DeckRiderAnchors.live(overworld.getGameTime());
        Set<Integer> seen = new HashSet<>(anchors.size());
        for (DeckRiderAnchors.Anchor anchor : anchors) {
            if (anchor.rider().isRemoved() || anchor.tile().isRemoved() || anchor.whale().isRemoved()) {
                continue;
            }
            int riderId = anchor.rider().getId();
            seen.add(riderId);
            int whaleId = anchor.whale().getId();
            int tileId = anchor.tile().getId();
            Vec3 offset;
            long now = overworld.getGameTime();
            DeckRiderClaims.Claim claim = DeckRiderClaims.fresh(riderId, now);
            boolean fresh = claim != null && claim.whaleId() == whaleId;
            if (!fresh) {
                claim = DeckRiderClaims.last(riderId, now);
            }
            String src;
            if (claim != null && claim.whaleId() == whaleId) {
                tileId = claim.tileId();
                offset = claim.offset();
                src = fresh ? "claim" : "stale";
            } else {
                offset = DeckRiderAnchors.toDeckLocal(
                        anchor.rider().position().subtract(anchor.tile().position()),
                        DeckRiderAnchors.deckYaw(anchor));
                src = "derived";
            }
            boolean turned = !src.equals(loggedSrc.put(riderId, src));
            if ((turned || now % 20L == 0L) && WhaleborneDebug.on()) {
                WhaleborneDebug.log(
                        "deckridersync rider=%d whale=%d tile=%d src=%s off=%.3f,%.3f,%.3f",
                        riderId, whaleId, tileId, src, offset.x, offset.y, offset.z);
            }
            PacketDistributor.sendToPlayersTrackingEntity(anchor.rider(), new DeckRiderSyncPayload(
                    riderId, whaleId, tileId,
                    (float) offset.x, (float) offset.y, (float) offset.z));
        }
        loggedSrc.keySet().retainAll(seen);
    }

    private static final Map<Integer, String> loggedSrc = new HashMap<>();
}
