package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.Config;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobCategory.class)
public class MobCategoryMixin {

    @Inject(method = "getMaxInstancesPerChunk", at = @At("RETURN"), cancellable = true)
    private void whaleborne$getCustomMobCap(CallbackInfoReturnable<Integer> cir) {
        MobCategory self = (MobCategory) (Object) this;

        if ("WHALEBORNE_HULLBACK".equals(self.getName()) || "whaleborne:hullback".equals(self.getName()) || "HULLBACK".equals(self.getName()) || "hullback".equals(self.getName())) {
            cir.setReturnValue(Config.hullbackSpawnCap);
        }
    }
}
