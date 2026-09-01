package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.events.DeckRiderSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeckRiderSyncPayload(int riderId, int whaleId, int tileId,
                                   float offX, float offY, float offZ) implements CustomPacketPayload {

    public static final Type<DeckRiderSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "deck_rider_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeckRiderSyncPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                buf.writeVarInt(payload.riderId());
                buf.writeVarInt(payload.whaleId());
                buf.writeVarInt(payload.tileId());
                buf.writeFloat(payload.offX());
                buf.writeFloat(payload.offY());
                buf.writeFloat(payload.offZ());
            }, buf -> new DeckRiderSyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeckRiderSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> DeckRiderSync.accept(payload));
    }
}
