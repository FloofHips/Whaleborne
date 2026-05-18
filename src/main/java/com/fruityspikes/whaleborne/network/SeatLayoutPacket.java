package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.server.entities.components.hullback.SeatLayout;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs seat layout (positions + part mapping) from server to client.
 * Sent when armor changes and a custom SeatLayout is applied.
 */
public class SeatLayoutPacket {
    private final int entityId;
    private final SeatLayout.SeatDef[] seats;
    private final int flukeSeatIndex;

    public SeatLayoutPacket(int entityId, SeatLayout.SeatDef[] seats, int flukeSeatIndex) {
        this.entityId = entityId;
        this.seats = seats;
        this.flukeSeatIndex = flukeSeatIndex;
    }

    public static void encode(SeatLayoutPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeByte(msg.seats.length);
        for (SeatLayout.SeatDef def : msg.seats) {
            buf.writeFloat((float) def.offset().x);
            buf.writeFloat((float) def.offset().y);
            buf.writeFloat((float) def.offset().z);
            buf.writeByte(def.posPartIndex());
            buf.writeByte(def.rotPartIndex());
        }
        buf.writeByte(msg.flukeSeatIndex);
    }

    public static SeatLayoutPacket decode(FriendlyByteBuf buf) {
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
        return new SeatLayoutPacket(entityId, defs, flukeIdx);
    }

    public static void handle(SeatLayoutPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                ClientPacketHandler.handleSeatLayoutSync(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public int getEntityId() { return entityId; }
    public SeatLayout.SeatDef[] getSeats() { return seats; }
    public int getFlukeSeatIndex() { return flukeSeatIndex; }
}
