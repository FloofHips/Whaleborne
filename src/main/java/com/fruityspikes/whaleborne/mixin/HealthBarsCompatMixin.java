package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.Config;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import fuzs.healthbars.client.helper.HealthTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiFunction;

@Mixin(targets = "fuzs.healthbars.client.handler.InLevelRenderingHandler", remap = false)
public abstract class HealthBarsCompatMixin {

    @Shadow
    private static void renderHealthBar(PoseStack poseStack, float partialTick, int packedLight, HealthTracker healthTracker, LivingEntity livingEntity, int heightOffset, Font font, BiFunction<PoseStack, Integer, GuiGraphics> factory, @Nullable RenderType renderType) {
        throw new AssertionError();
    }

    @Redirect(
        method = "onRenderNameTag",
        at = @At(
            value = "INVOKE",
            target = "Lfuzs/healthbars/client/handler/InLevelRenderingHandler;renderHealthBar(Lcom/mojang/blaze3d/vertex/PoseStack;FILfuzs/healthbars/client/helper/HealthTracker;Lnet/minecraft/world/entity/LivingEntity;ILnet/minecraft/client/gui/Font;Ljava/util/function/BiFunction;Lnet/minecraft/client/renderer/RenderType;)V"
        )
    )
    private static void redirectRenderHealthBar(PoseStack poseStack, float partialTick, int packedLight, HealthTracker healthTracker, LivingEntity livingEntity, int heightOffset, Font font, BiFunction<PoseStack, Integer, GuiGraphics> factory, @Nullable RenderType renderType) {
        if (livingEntity instanceof HullbackEntity) {
            // Scale by 10 to match Neat's pixels-per-unit; subtract because GUI Y grows
            // downward (up = lower Y). The -2 offset makes config 0 the neutral position.
            heightOffset -= ((Config.healthBarsOffset - 2) * 10);
        }
        renderHealthBar(poseStack, partialTick, packedLight, healthTracker, livingEntity, heightOffset, font, factory, renderType);
    }
}
