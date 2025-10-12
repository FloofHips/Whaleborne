package com.fruityspikes.whaleborne.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HullbackHurtPacket {
    private final int entityId;
    private final ItemStack armorItem;
    private final ItemStack crownItem;
    private final byte flags;

    public HullbackHurtPacket(int entityId, ItemStack armorItem, ItemStack crownItem, byte flags) {
        this.entityId = entityId;
        this.armorItem = armorItem;
        this.crownItem = crownItem;
        this.flags = flags;
    }

    public static void encode(HullbackHurtPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeItem(msg.armorItem);
        buffer.writeItem(msg.crownItem);
        buffer.writeByte(msg.flags);
    }

    public static HullbackHurtPacket decode(FriendlyByteBuf buffer) {
        return new HullbackHurtPacket(
                buffer.readInt(),
                buffer.readItem(),
                buffer.readItem(),
                buffer.readByte()
        );
    }

    public static void handle(HullbackHurtPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                ClientPacketHandler.handleHullbackHurtSync(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public int getEntityId() { return entityId; }
    public ItemStack getArmorItem() { return armorItem; }
    public ItemStack getCrownItem() { return crownItem; }
    public byte getFlags() { return flags; }
}
