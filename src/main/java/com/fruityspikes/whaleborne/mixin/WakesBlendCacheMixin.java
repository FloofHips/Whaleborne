package com.fruityspikes.whaleborne.mixin;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.render.WakeColor;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** blendFast runs once per pixel but yields only a few colors per node (fluid/light/opacity are constant
 *  across its res*res pixels), so memoize it by wake color. Client wake tick, single-threaded. */
@Mixin(value = WakeColor.class, remap = false)
public class WakesBlendCacheMixin {
    @Unique private static int wb$tintR = Integer.MIN_VALUE;
    @Unique private static int wb$tintG, wb$tintB, wb$light, wb$opBits;
    @Unique private static boolean wb$shaders;
    @Unique private static final Reference2IntOpenHashMap<WakeColor> wb$cache = new Reference2IntOpenHashMap<>(16);

    @Redirect(
            method = "sampleColor",
            at = @At(value = "INVOKE",
                     target = "Lcom/leclowndu93150/wakes/render/WakeColor;blendFast(Lcom/leclowndu93150/wakes/render/WakeColor;IIIIF)I"),
            require = 0)
    private static int whaleborne$cachedBlend(WakeColor color, int tintR, int tintG, int tintB, int lightColor, float opacity) {
        int opBits = Float.floatToRawIntBits(opacity);
        boolean shaders = WakesClient.areShadersEnabled;
        if (tintR != wb$tintR || tintG != wb$tintG || tintB != wb$tintB || lightColor != wb$light
                || opBits != wb$opBits || shaders != wb$shaders) {
            wb$cache.clear();
            wb$tintR = tintR; wb$tintG = tintG; wb$tintB = tintB; wb$light = lightColor; wb$opBits = opBits; wb$shaders = shaders;
        }
        if (wb$cache.containsKey(color)) {
            return wb$cache.getInt(color);
        }
        int v = WakeColorInvoker.whaleborne$blendFast(color, tintR, tintG, tintB, lightColor, opacity);
        wb$cache.put(color, v);
        return v;
    }
}
