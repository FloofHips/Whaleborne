package com.fruityspikes.whaleborne.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface FloatingPlayerAccessor {

    @Accessor("aboveGroundTickCount")
    void whaleborne$setAboveGroundTickCount(int ticks);
}
