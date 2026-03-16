package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.Config;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vazkii.neat.HealthBarRenderer;
import vazkii.neat.NeatConfig;

@Mixin(HealthBarRenderer.class)
public class NeatCompatMixin {

    @Redirect(method = "hookRender", at = @At(value = "INVOKE", target = "Lvazkii/neat/NeatConfig$ConfigAccess;heightAbove()D"), remap = false)
    private static double modifyHeightAbove(NeatConfig.ConfigAccess instance, Entity entity) {
        double originalHeight = instance.heightAbove();
        if (entity instanceof HullbackEntity) {
            return originalHeight + Config.neatOffset;
        }
        return originalHeight;
    }
}
