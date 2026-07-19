package com.fruityspikes.whaleborne.mixin;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.render.WakeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WakeColor.class, remap = false)
public class WakesColorShaderSkipMixin {

    @Inject(method = "invertedLogisticCurve", at = @At("HEAD"), cancellable = true, require = 0)
    private static void whaleborne$skipConfigWhenNoShaders(float x, CallbackInfoReturnable<Double> cir) {
        if (!WakesClient.areShadersEnabled) {
            cir.setReturnValue((double) x);
        }
    }
}
