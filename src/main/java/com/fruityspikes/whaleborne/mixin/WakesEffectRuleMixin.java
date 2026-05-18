package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import com.leclowndu93150.wakes.config.enums.EffectSpawningRule;
import com.leclowndu93150.wakes.utils.WakesUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables the Wakes mod's default wake for Hullback, its parts, and whale widgets;
 *  WakesCompat generates proper per-part and per-widget wakes instead. */
@Mixin(value = WakesUtils.class, remap = false)
public class WakesEffectRuleMixin {

    @Inject(method = "getEffectRuleFromSource", at = @At("HEAD"), cancellable = true)
    private static void whaleborne$overrideHullbackRule(Entity source, CallbackInfoReturnable<EffectSpawningRule> cir) {
        if (source instanceof HullbackEntity
                || source instanceof HullbackPartEntity
                || source instanceof WhaleWidgetEntity) {
            cir.setReturnValue(EffectSpawningRule.DISABLED);
        }
    }
}
