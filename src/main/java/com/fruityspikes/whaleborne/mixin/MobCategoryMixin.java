package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.Config;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobCategory.class)
public class MobCategoryMixin {

    @Inject(method = "getMaxInstancesPerChunk", at = @At("HEAD"), cancellable = true)
    private void whaleborne$getCustomMobCap(CallbackInfoReturnable<Integer> cir) {
        MobCategory self = (MobCategory) (Object) this;

        // Check if it's the Whaleborne Hullback category
        if ("WHALEBORNE_HULLBACK".equals(self.getName()) || "whaleborne:hullback".equals(self.getName()) || "HULLBACK".equals(self.getName()) || "hullback".equals(self.getName())) {
            // Return the custom cap direct from the configuration file
            cir.setReturnValue(Config.HULLBACK_SPAWN_CAP.get());
        }
    }
}
