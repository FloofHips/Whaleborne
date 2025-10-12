package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public class ClientPacketHandler {
    public static void handleDirtSync(SyncHullbackDirtPacket packet) {
        Entity entity = Minecraft.getInstance().level.getEntity(packet.getEntityId());
        if (entity instanceof HullbackEntity hullback) {
            BlockState[][] dirtArray = SyncHullbackDirtPacket.deserializeDirtArray(packet.getDirtData());

            switch (packet.getArrayType()) {
                case 0 -> hullback.headDirt = dirtArray;
                case 1 -> hullback.headTopDirt = dirtArray;
                case 2 -> hullback.bodyDirt = dirtArray;
                case 3 -> hullback.bodyTopDirt = dirtArray;
                case 4 -> hullback.tailDirt = dirtArray;
                case 5 -> hullback.flukeDirt = dirtArray;
            }
        }
    }

    public static void handleHullbackHurtSync(HullbackHurtPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Entity entity = minecraft.level.getEntity(packet.getEntityId());
            if (entity instanceof HullbackEntity whale) {

                whale.inventory.setItem(HullbackEntity.INV_SLOT_ARMOR, packet.getArmorItem());
                whale.inventory.setItem(HullbackEntity.INV_SLOT_CROWN, packet.getCrownItem());

                whale.getEntityData().set(HullbackEntity.DATA_ARMOR, packet.getArmorItem());
                whale.getEntityData().set(HullbackEntity.DATA_CROWN_ID, packet.getCrownItem());
                whale.getEntityData().set(HullbackEntity.DATA_ID_FLAGS, packet.getFlags());

                whale.updateContainerEquipment();
            }
        }
    }
}
