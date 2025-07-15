package com.fruityspikes.whaleborne.network;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SyncHullbackDirtPacket {
    private final int entityId;
    private final CompoundTag dirtData;
    private final int arrayType;
    private final boolean isBottom;

    public SyncHullbackDirtPacket(int entityId, BlockState[][] dirtArray, int arrayType, boolean isBottom) {
        this.entityId = entityId;
        this.arrayType = arrayType;
        this.isBottom = isBottom;
        this.dirtData = serializeDirtArray(dirtArray);
    }

    public static SyncHullbackDirtPacket decode(FriendlyByteBuf buf) {
        return new SyncHullbackDirtPacket(
                buf.readInt(),
                null,
                buf.readByte(),
                buf.readBoolean()
        ) {
            private final CompoundTag dirtData = buf.readNbt();

            @Override
            public CompoundTag getDirtData() {
                return dirtData;
            }
        };
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeByte(arrayType);
        buf.writeBoolean(isBottom);
        buf.writeNbt(dirtData);
    }

    public static void handle(SyncHullbackDirtPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                Entity entity = Minecraft.getInstance().level.getEntity(packet.getEntityId());
                if (entity instanceof HullbackEntity hullback) {
                    BlockState[][] dirtArray = deserializeDirtArray(packet.getDirtData());
                    if (dirtArray != null) {
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
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static CompoundTag serializeDirtArray(BlockState[][] array) {
        CompoundTag tag = new CompoundTag();
        if (array == null || array.length == 0) {
            tag.putInt("width", 0);
            tag.putInt("height", 0);
            return tag;
        }

        tag.putInt("width", array.length);
        tag.putInt("height", array[0].length);

        for (int x = 0; x < array.length; x++) {
            ListTag column = new ListTag();
            if (array[x] != null) {
                for (int y = 0; y < array[x].length; y++) {
                    column.add(NbtUtils.writeBlockState(array[x][y] != null ? array[x][y] : Blocks.AIR.defaultBlockState()));
                }
            }
            tag.put("x" + x, column);
        }
        return tag;
    }

    public static BlockState[][] deserializeDirtArray(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains("width") || !tag.contains("height")) {
            return new BlockState[0][0];
        }

        int width = tag.getInt("width");
        int height = tag.getInt("height");
        BlockState[][] array = new BlockState[width][height];

        for (int x = 0; x < width; x++) {
            String key = "x" + x;
            if (tag.contains(key)) {
                ListTag column = tag.getList(key, 10);
                for (int y = 0; y < Math.min(column.size(), height); y++) {
                    array[x][y] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), column.getCompound(y));
                }
            }
        }
        return array;
    }

    public CompoundTag getDirtData() { return dirtData; }
    public int getEntityId() { return entityId; }
    public int getArrayType() { return arrayType; }
    public boolean isBottom() { return isBottom; }
}
