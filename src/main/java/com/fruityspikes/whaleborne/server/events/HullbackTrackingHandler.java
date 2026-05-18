package com.fruityspikes.whaleborne.server.events;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.network.SeatLayoutPacket;
import com.fruityspikes.whaleborne.network.WhaleborneNetwork;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.components.hullback.SeatLayout;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = Whaleborne.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HullbackTrackingHandler {

    /**
     * Sends current seat layout when a player starts tracking the whale, so clients
     * re-entering chunks pick up a hull-config override instead of the default layout.
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof HullbackEntity hullback
                && event.getEntity() instanceof ServerPlayer player) {
            SeatLayout layout = hullback.getSeatLayout();
            WhaleborneNetwork.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SeatLayoutPacket(hullback.getId(), layout.getAllSeatDefs(), layout.getFlukeSeatIndex())
            );
        }
    }
}
