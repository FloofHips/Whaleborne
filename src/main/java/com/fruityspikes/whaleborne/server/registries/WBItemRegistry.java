package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.items.WhaleEquipment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class WBItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Whaleborne.MODID);
    public static final Supplier<WhaleEquipment> SAIL = ITEMS.register("sail", () -> new WhaleEquipment(WBEntityRegistry.SAIL::get, new Item.Properties().stacksTo(1)));
    public static final Supplier<WhaleEquipment> CANNON = ITEMS.register("cannon", () -> new WhaleEquipment(WBEntityRegistry.CANNON::get, new Item.Properties().stacksTo(1)));
    public static final Supplier<WhaleEquipment> MAST = ITEMS.register("mast", () -> new WhaleEquipment(WBEntityRegistry.MAST::get, new Item.Properties().stacksTo(1)));
    public static final Supplier<WhaleEquipment> HELM = ITEMS.register("helm", () -> new WhaleEquipment(WBEntityRegistry.HELM::get, new Item.Properties().stacksTo(1)));
    public static final Supplier<WhaleEquipment> ANCHOR = ITEMS.register("anchor", () -> new WhaleEquipment(WBEntityRegistry.ANCHOR::get, new Item.Properties().stacksTo(1)));
    public static final Supplier<DeferredSpawnEggItem> HULLBACK_SPAWN_EGG = ITEMS.register("hullback_spawn_egg", () -> new DeferredSpawnEggItem(WBEntityRegistry.HULLBACK, -5787987,  -9600639, new Item.Properties()));
    public static final Supplier<Item> BARNACLE = ITEMS.register("barnacle", () -> new BlockItem(WBBlockRegistry.BARNACLE.get(), new Item.Properties()));
    public static final Supplier<Item> ROUGH_BARNACLE = ITEMS.register("rough_barnacle", () -> new BlockItem(WBBlockRegistry.ROUGH_BARNACLE.get(), new Item.Properties()));

}
