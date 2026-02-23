package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(targets = "fuzs.healthbars.client.handler.PickEntityHandler", remap = false)
public class HealthBarsPickMixin {

    @Redirect(
        method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;", remap = true)
    )
    private static EntityHitResult redirectGetEntityHitResult(Entity shooter, Vec3 startVec, Vec3 endVec, AABB boundingBox, Predicate<Entity> filter, double distance) {
        EntityHitResult vanillaResult = ProjectileUtil.getEntityHitResult(shooter, startVec, endVec, boundingBox, filter, distance);

        if (vanillaResult == null) {
            // If the vanilla raytrace doesn't find anything (because Hullback is ignored),
            // we try to find a HullbackPartEntity instead.
            EntityHitResult partResult = ProjectileUtil.getEntityHitResult(shooter, startVec, endVec, boundingBox,
                entity -> entity instanceof HullbackPartEntity && !entity.isSpectator() && entity.isPickable(),
                distance
            );

            if (partResult != null && partResult.getEntity() instanceof HullbackPartEntity partEntity) {
                HullbackEntity parent = partEntity.getParent();
                if (parent != null) {
                    return new EntityHitResult(parent, partResult.getLocation());
                }
            }
        }

        return vanillaResult;
    }
}
