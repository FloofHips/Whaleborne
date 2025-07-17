package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.items.WhaleEquipment;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class WBItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Whaleborne.MODID);

    public static final RegistryObject<WhaleEquipment> SAIL = ITEMS.register("sail", () -> new WhaleEquipment(WBEntityRegistry.SAIL::get, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<WhaleEquipment> CANNON = ITEMS.register("cannon", () -> new WhaleEquipment(WBEntityRegistry.CANNON::get, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<WhaleEquipment> MAST = ITEMS.register("mast", () -> new WhaleEquipment(WBEntityRegistry.MAST::get, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<WhaleEquipment> HELM = ITEMS.register("helm", () -> new WhaleEquipment(WBEntityRegistry.HELM::get, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<WhaleEquipment> ANCHOR = ITEMS.register("anchor", () -> new WhaleEquipment(WBEntityRegistry.ANCHOR::get, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<ForgeSpawnEggItem> HULLBACK_SPAWN_EGG = ITEMS.register("hullback_spawn_egg", () -> new ForgeSpawnEggItem(WBEntityRegistry.HULLBACK, -5787987,  -9600639, new Item.Properties()));
    public static final RegistryObject<Item> BARNACLE = ITEMS.register("barnacle", () -> new BlockItem(WBBlockRegistry.BARNACLE.get(), new Item.Properties()));

}
