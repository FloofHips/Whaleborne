package com.fruityspikes.whaleborne.compat;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class SupplementariesCannonCompat {
    private static final String[] MANAGERS = {
            "net.mehvahdjukaar.supplementaries.common.block.fire_behaviors.FireBehaviorsManager",
            "net.mehvahdjukaar.supplementaries.common.block.blocks.CannonBlock"
    };
    private static final String BEHAVIOR = "net.mehvahdjukaar.supplementaries.common.block.fire_behaviors.IFireItemBehavior";

    private static boolean resolved;
    private static Method lookup;
    private static Method fire;
    private static Object fallback;
    private static boolean fireWarned;

    private SupplementariesCannonCompat() {
    }

    private static void resolve() {
        resolved = true;
        if (!ModList.get().isLoaded("supplementaries")) {
            return;
        }
        try {
            Class<?> behavior = Class.forName(BEHAVIOR);
            for (String name : MANAGERS) {
                try {
                    lookup = Class.forName(name).getMethod("getCannonBehavior", ItemLike.class);
                    break;
                } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                }
            }
            if (lookup == null) {
                throw new NoSuchMethodException("getCannonBehavior(ItemLike)");
            }
            for (Method m : behavior.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("fire") && p.length == 7 && p[2] == Vec3.class && p[3] == Vec3.class) {
                    fire = m;
                    break;
                }
            }
            if (fire == null) {
                throw new NoSuchMethodException("IFireItemBehavior.fire(ItemStack, ServerLevel, Vec3, Vec3, float, int, owner)");
            }
            fallback = lookup.invoke(null, Items.AIR);
        } catch (Throwable t) {
            lookup = null;
            fire = null;
            Whaleborne.LOGGER.warn("Supplementaries is present but its cannon fire behaviors could not be resolved; cannon ammo falls back to Whaleborne's own handling", t);
        }
    }

    public static boolean fire(Level level, ItemStack ammo, Vec3 muzzle, Vec3 direction, int power, Entity owner) {
        if (!resolved) {
            resolve();
        }
        if (fire == null || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        try {
            Object behavior = lookup.invoke(null, ammo.getItem());
            if (behavior == null || behavior == fallback) {
                return false;
            }
            Object shooter = fire.getParameterTypes()[6].isInstance(owner) ? owner : null;
            return (Boolean) fire.invoke(behavior, ammo, serverLevel, muzzle, direction.normalize(),
                    power / 50.0F, 0, shooter);
        } catch (Throwable t) {
            if (!fireWarned) {
                fireWarned = true;
                Whaleborne.LOGGER.warn("Supplementaries cannon behavior failed for {}", ammo.getItem(), t);
            }
            return false;
        }
    }
}
