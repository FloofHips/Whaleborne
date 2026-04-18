package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.items.AnchorItem;
import com.fruityspikes.whaleborne.server.items.PlaceableWhaleEquipment;
import com.fruityspikes.whaleborne.server.items.WhaleEquipment;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class WBItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Whaleborne.MODID);

    public static final RegistryObject<WhaleEquipment> SAIL = ITEMS.register("sail", () -> new PlaceableWhaleEquipment(WBEntityRegistry.SAIL::get, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<WhaleEquipment> CANNON = ITEMS.register("cannon", () -> new PlaceableWhaleEquipment(WBEntityRegistry.CANNON::get, SoundEvents.ANVIL_PLACE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<WhaleEquipment> MAST = ITEMS.register("mast", () -> new WhaleEquipment(WBEntityRegistry.MAST::get, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<WhaleEquipment> HELM = ITEMS.register("helm", () -> new WhaleEquipment(WBEntityRegistry.HELM::get, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<WhaleEquipment> ANCHOR = ITEMS.register("anchor", () -> new AnchorItem(WBEntityRegistry.ANCHOR::get, SoundEvents.ANVIL_PLACE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<ForgeSpawnEggItem> HULLBACK_SPAWN_EGG = ITEMS.register("hullback_spawn_egg", () -> new ForgeSpawnEggItem(WBEntityRegistry.HULLBACK, -5787987,  -9600639, new Item.Properties()));
    public static final RegistryObject<Item> BARNACLE = ITEMS.register("barnacle", () -> new BlockItem(WBBlockRegistry.BARNACLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ROUGH_BARNACLE = ITEMS.register("rough_barnacle", () -> new BlockItem(WBBlockRegistry.ROUGH_BARNACLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MUSIC_DISC_THE_PLANK = ITEMS.register("music_disc_the_plank", () -> new RecordItem(0, WBSoundRegistry.MUSIC_DISC_THE_PLANK.get(), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 127));

}
