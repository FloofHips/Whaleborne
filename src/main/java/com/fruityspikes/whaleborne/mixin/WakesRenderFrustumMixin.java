package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.client.compat.WakesRenderGate;
import com.leclowndu93150.wakes.render.WakeRenderer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WakeRenderer.class, remap = false)
public class WakesRenderFrustumMixin {

    @Inject(method = "onRenderLevel", at = @At("HEAD"), require = 0)
    private static void whaleborne$captureFrustum(RenderLevelStageEvent event, CallbackInfo ci) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            WakesRenderGate.setFrustum(event.getFrustum());
        }
    }
}
