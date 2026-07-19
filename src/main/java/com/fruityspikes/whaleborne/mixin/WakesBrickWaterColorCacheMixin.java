package com.fruityspikes.whaleborne.mixin;

import com.leclowndu93150.wakes.simulation.Brick;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Brick.class, remap = false)
public class WakesBrickWaterColorCacheMixin {

    @Unique private static final Long2IntOpenHashMap whaleborne$waterColorCache = new Long2IntOpenHashMap();
    @Unique private static long whaleborne$lastClear = Long.MIN_VALUE;

    @Redirect(
            method = "populatePixels",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/BiomeColors;getAverageWaterColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"),
            require = 0)
    private int whaleborne$cachedWaterColor(BlockAndTintGetter level, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            long gt = mc.level.getGameTime();
            if (gt - whaleborne$lastClear > 200L) {
                whaleborne$waterColorCache.clear();
                whaleborne$lastClear = gt;
            }
        }
        long key = ((long) pos.getX() & 0x3FFFFFFL) | (((long) pos.getZ() & 0x3FFFFFFL) << 26);
        int v = whaleborne$waterColorCache.getOrDefault(key, Integer.MIN_VALUE);
        if (v == Integer.MIN_VALUE) {
            v = BiomeColors.getAverageWaterColor(level, pos);
            whaleborne$waterColorCache.put(key, v);
        }
        return v;
    }
}
