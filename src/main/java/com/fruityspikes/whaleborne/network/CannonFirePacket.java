package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.server.entities.CannonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CannonFirePacket {
    private final int entityId;
    private final int power;

    public CannonFirePacket(int entityId, int power) {
        this.entityId = entityId;
        this.power = power;
    }

    public static void encode(CannonFirePacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.entityId).writeInt(msg.power);
    }

    public static CannonFirePacket decode(FriendlyByteBuf buffer) {
        return new CannonFirePacket(buffer.readInt(), buffer.readInt());
    }

    public static void handle(CannonFirePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                Entity entity = sender.level().getEntity(msg.entityId);
                if (entity instanceof CannonEntity cannon) {
                    if (cannon.getFirstPassenger() == sender) {
                        cannon.fireCannon(msg.power);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}