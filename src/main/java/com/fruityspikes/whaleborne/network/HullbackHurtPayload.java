package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HullbackHurtPayload(int entityId, ItemStack armorItem, ItemStack crownItem, ItemStack saddleItem, byte flags) implements CustomPacketPayload {
    public static final Type<HullbackHurtPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "hullback_hurt"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HullbackHurtPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HullbackHurtPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC, HullbackHurtPayload::armorItem,
            ItemStack.OPTIONAL_STREAM_CODEC, HullbackHurtPayload::crownItem,
            ItemStack.OPTIONAL_STREAM_CODEC, HullbackHurtPayload::saddleItem,
            ByteBufCodecs.BYTE, HullbackHurtPayload::flags,
            HullbackHurtPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HullbackHurtPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.handleHullbackHurtSync(payload));
    }
}
