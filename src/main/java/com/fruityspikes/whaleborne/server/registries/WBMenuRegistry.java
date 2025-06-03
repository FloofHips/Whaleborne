package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.menus.CannonMenu;
import com.fruityspikes.whaleborne.server.entities.CannonEntity;
import com.fruityspikes.whaleborne.server.items.WhaleEquipment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class WBMenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Whaleborne.MODID);

    public static final RegistryObject<MenuType<CannonMenu>> CANNON_MENU =
            MENUS.register("cannon_menu", () ->
                    IForgeMenuType.create(CannonMenu::fromNetwork));

}
