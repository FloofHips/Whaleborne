package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.DeckRiderClaims;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeckRiderClaimPayload(int whaleId, int tileId,
                                    float offX, float offY, float offZ) implements CustomPacketPayload {

    public static final Type<DeckRiderClaimPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "deck_rider_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeckRiderClaimPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
                buf.writeVarInt(payload.whaleId());
                buf.writeVarInt(payload.tileId());
                buf.writeFloat(payload.offX());
                buf.writeFloat(payload.offY());
                buf.writeFloat(payload.offZ());
            }, buf -> new DeckRiderClaimPayload(buf.readVarInt(), buf.readVarInt(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeckRiderClaimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            DeckRiderClaims.set(sender.getId(), payload.whaleId(), payload.tileId(),
                    new Vec3(payload.offX(), payload.offY(), payload.offZ()),
                    sender.level().getGameTime());
        });
    }
}
