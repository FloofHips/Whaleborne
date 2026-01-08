package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.menus.CannonMenu;
import com.fruityspikes.whaleborne.client.menus.HullbackMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class WBMenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Whaleborne.MODID);

    public static final Supplier<MenuType<CannonMenu>> CANNON_MENU =
            MENUS.register("cannon_menu", () ->
                    IMenuTypeExtension.create(CannonMenu::fromNetwork));
    public static final Supplier<MenuType<HullbackMenu>> HULLBACK_MENU =
            MENUS.register("hullback_menu", () ->
                    IMenuTypeExtension.create(HullbackMenu::fromNetwork));



}
