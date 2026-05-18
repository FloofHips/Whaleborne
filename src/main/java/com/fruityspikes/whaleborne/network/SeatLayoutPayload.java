package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.components.hullback.SeatLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Syncs seat layout (positions + part mapping) to clients when a custom SeatLayout is applied. */
public record SeatLayoutPayload(int entityId, SeatLayout.SeatDef[] seats, int flukeSeatIndex) implements CustomPacketPayload {

    public static final Type<SeatLayoutPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "seat_layout"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeatLayoutPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SeatLayoutPayload decode(RegistryFriendlyByteBuf buf) {
                    int entityId = buf.readInt();
                    int count = buf.readByte() & 0xFF;
                    SeatLayout.SeatDef[] defs = new SeatLayout.SeatDef[count];
                    for (int i = 0; i < count; i++) {
                        float x = buf.readFloat();
                        float y = buf.readFloat();
                        float z = buf.readFloat();
                        int posP = buf.readByte();
                        int rotP = buf.readByte();
                        defs[i] = new SeatLayout.SeatDef(new Vec3(x, y, z), posP, rotP);
                    }
                    int flukeIdx = buf.readByte();
                    return new SeatLayoutPayload(entityId, defs, flukeIdx);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SeatLayoutPayload payload) {
                    buf.writeInt(payload.entityId());
                    buf.writeByte(payload.seats().length);
                    for (SeatLayout.SeatDef def : payload.seats()) {
                        buf.writeFloat((float) def.offset().x);
                        buf.writeFloat((float) def.offset().y);
                        buf.writeFloat((float) def.offset().z);
                        buf.writeByte(def.posPartIndex());
                        buf.writeByte(def.rotPartIndex());
                    }
                    buf.writeByte(payload.flukeSeatIndex());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SeatLayoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(payload.entityId());
            if (entity instanceof HullbackEntity hullback) {
                SeatLayout layout = new SeatLayout(payload.seats(), payload.flukeSeatIndex());
                hullback.partManager.setSeatLayout(layout);
                hullback.hullbackSeatManager.setSeatLayout(layout);
            }
        });
    }
}
