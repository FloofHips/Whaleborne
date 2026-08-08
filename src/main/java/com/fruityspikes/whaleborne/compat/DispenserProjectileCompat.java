package com.fruityspikes.whaleborne.compat;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

public final class DispenserProjectileCompat {
    private static boolean warned;

    private DispenserProjectileCompat() {
    }

    public static Projectile create(Level level, ItemStack ammo, Vec3 muzzle, Entity owner) {
        DispenseItemBehavior behavior = DispenserBlock.DISPENSER_REGISTRY.get(ammo.getItem());
        if (!(behavior instanceof AbstractProjectileDispenseBehavior)) {
            return null;
        }
        try {
            for (Class<?> c = behavior.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 3 && p[0] == Level.class && p[1] == Position.class && p[2] == ItemStack.class
                            && Projectile.class.isAssignableFrom(m.getReturnType())) {
                        m.setAccessible(true);
                        Projectile launched = (Projectile) m.invoke(behavior, level, muzzle, ammo);
                        launched.setOwner(owner);
                        return launched;
                    }
                }
            }
            if (!warned) {
                warned = true;
                Whaleborne.LOGGER.warn("No dispenser projectile factory found on {} for {}", behavior.getClass().getName(), ammo.getItem());
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                Whaleborne.LOGGER.warn("Dispenser projectile creation failed for {}", ammo.getItem(), t);
            }
        }
        return null;
    }
}
