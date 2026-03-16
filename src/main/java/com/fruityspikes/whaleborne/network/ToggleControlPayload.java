package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleControlPayload {
    private final boolean vectorControl;

    public ToggleControlPayload(boolean vectorControl) {
        this.vectorControl = vectorControl;
    }

    public ToggleControlPayload(FriendlyByteBuf buffer) {
        this.vectorControl = buffer.readBoolean();
    }

    public static void encode(ToggleControlPayload message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.vectorControl);
    }

    public static ToggleControlPayload decode(FriendlyByteBuf buffer) {
        return new ToggleControlPayload(buffer);
    }

    public static void handle(ToggleControlPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getRootVehicle() instanceof HullbackEntity hullback) {
                hullback.setVectorControl(message.vectorControl);
            }
        });
        context.setPacketHandled(true);
    }
}
