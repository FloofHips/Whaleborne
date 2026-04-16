package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.items.AnchorItem;
import com.fruityspikes.whaleborne.server.items.PlaceableWhaleEquipment;
import com.fruityspikes.whaleborne.server.items.WhaleEquipment;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WBItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Whaleborne.MODID);

    public static final ResourceKey<JukeboxSong> THE_PLANK_SONG = ResourceKey.create(
            Registries.JUKEBOX_SONG,
            ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "the_plank")
    );

    public static final DeferredHolder<Item, WhaleEquipment> SAIL = ITEMS.register("sail", () -> new PlaceableWhaleEquipment(WBEntityRegistry.SAIL::get, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, WhaleEquipment> CANNON = ITEMS.register("cannon", () -> new PlaceableWhaleEquipment(WBEntityRegistry.CANNON::get, SoundEvents.ANVIL_PLACE, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, WhaleEquipment> MAST = ITEMS.register("mast", () -> new WhaleEquipment(WBEntityRegistry.MAST::get, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, WhaleEquipment> HELM = ITEMS.register("helm", () -> new WhaleEquipment(WBEntityRegistry.HELM::get, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, WhaleEquipment> ANCHOR = ITEMS.register("anchor", () -> new AnchorItem(WBEntityRegistry.ANCHOR::get, SoundEvents.ANVIL_PLACE, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> HULLBACK_SPAWN_EGG = ITEMS.register("hullback_spawn_egg", () -> new DeferredSpawnEggItem(WBEntityRegistry.HULLBACK, -5787987,  -9600639, new Item.Properties()));
    public static final DeferredHolder<Item, Item> BARNACLE = ITEMS.register("barnacle", () -> new BlockItem(WBBlockRegistry.BARNACLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> ROUGH_BARNACLE = ITEMS.register("rough_barnacle", () -> new BlockItem(WBBlockRegistry.ROUGH_BARNACLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> MUSIC_DISC_THE_PLANK = ITEMS.register("music_disc_the_plank", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(THE_PLANK_SONG)));

}
