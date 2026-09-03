package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.client.events.DeckRiderSync;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class DeckRiderLerpMixin {

    @Inject(method = "lerpTo", at = @At("HEAD"), cancellable = true)
    private void whaleborne$keepDeckPlacement(double x, double y, double z, float yRot, float xRot,
                                              int steps, CallbackInfo info) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide || self == Minecraft.getInstance().player) {
            return;
        }
        if (DeckRiderSync.holds(self.getId(), self.level().getGameTime())) {
            info.cancel();
        }
    }
}
