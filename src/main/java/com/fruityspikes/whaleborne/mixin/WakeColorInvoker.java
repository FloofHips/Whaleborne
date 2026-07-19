package com.fruityspikes.whaleborne.mixin;

import com.leclowndu93150.wakes.render.WakeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WakeColor.class, remap = false)
public interface WakeColorInvoker {
    @Invoker("blendFast")
    static int whaleborne$blendFast(WakeColor color, int tintR, int tintG, int tintB, int lightColor, float opacity) {
        throw new AssertionError();
    }
}
