package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.client.renderers.HullbackRenderer;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import fuzs.healthbars.client.helper.HealthTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "fuzs.healthbars.client.handler.GuiRenderingHandler", remap = false)
public class HealthBarsGuiMixin {

    @Inject(method = "renderEntityDisplay", at = @At("HEAD"))
    private static void onRenderEntityDisplayHead(GuiGraphics guiGraphics, MutableInt posX, MutableInt posY, HealthTracker healthTracker, LivingEntity livingEntity, CallbackInfo ci) {
        if (livingEntity instanceof HullbackEntity) {
            HullbackRenderer.isRenderingInHealthbarsGui = true;
        }
    }

    @Inject(method = "renderEntityDisplay", at = @At("RETURN"))
    private static void onRenderEntityDisplayReturn(GuiGraphics guiGraphics, MutableInt posX, MutableInt posY, HealthTracker healthTracker, LivingEntity livingEntity, CallbackInfo ci) {
        if (livingEntity instanceof HullbackEntity) {
            HullbackRenderer.isRenderingInHealthbarsGui = false;
        }
    }
}
