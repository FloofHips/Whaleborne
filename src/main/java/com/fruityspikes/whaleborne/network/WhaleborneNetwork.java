package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class WhaleborneNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder.named(
                    new ResourceLocation(Whaleborne.MODID, "network"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    protected static int packetID = 0;

    public static void init() {
        INSTANCE.registerMessage(getPacketID(),
                CannonFirePacket.class,
                CannonFirePacket::encode,
                CannonFirePacket::decode,
                CannonFirePacket::handle);

        INSTANCE.registerMessage(getPacketID(),
                SyncHullbackDirtPacket.class,
                SyncHullbackDirtPacket::encode,
                SyncHullbackDirtPacket::decode,
                SyncHullbackDirtPacket::handle);

        INSTANCE.registerMessage(getPacketID(),
                HullbackHurtPacket.class,
                HullbackHurtPacket::encode,
                HullbackHurtPacket::decode,
                HullbackHurtPacket::handle);
    }

    public static int getPacketID() {
        return packetID++;
    }
}