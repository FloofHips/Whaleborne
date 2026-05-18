package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.Whaleborne;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Whaleborne.MODID, bus = EventBusSubscriber.Bus.MOD)
public class WhaleborneNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Whaleborne.MODID)
                .versioned("1");

        registrar.playToServer(
                CannonFirePayload.TYPE,
                CannonFirePayload.STREAM_CODEC,
                CannonFirePayload::handle
        );

        registrar.playToClient(
                SyncHullbackDirtPayload.TYPE,
                SyncHullbackDirtPayload.STREAM_CODEC,
                SyncHullbackDirtPayload::handle
        );

        registrar.playToClient(
                HullbackHurtPayload.TYPE,
                HullbackHurtPayload.STREAM_CODEC,
                HullbackHurtPayload::handle
        );
        
        registrar.playToServer(
                ToggleControlPayload.TYPE,
                ToggleControlPayload.STREAM_CODEC,
                ToggleControlPayload::handle
        );

        registrar.playToClient(
                SeatLayoutPayload.TYPE,
                SeatLayoutPayload.STREAM_CODEC,
                SeatLayoutPayload::handle
        );
    }
}