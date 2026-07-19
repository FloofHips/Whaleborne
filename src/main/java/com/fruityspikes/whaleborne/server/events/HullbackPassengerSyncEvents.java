package com.fruityspikes.whaleborne.server.events;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Resends SetPassengers on tracking start so mounted widgets don't render detached after world load. */
@Mod.EventBusSubscriber(modid = Whaleborne.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HullbackPassengerSyncEvents {
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Entity target = event.getTarget();
        HullbackEntity whale = null;
        if (target instanceof HullbackEntity h) {
            whale = h;
        } else if (target instanceof WhaleWidgetEntity && target.getVehicle() instanceof HullbackEntity h) {
            whale = h;
        }

        if (whale != null && !whale.getPassengers().isEmpty()) {
            player.connection.send(new ClientboundSetPassengersPacket(whale));
        }
    }
}
