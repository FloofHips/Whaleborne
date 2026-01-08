package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class WBTagRegistry {
    public static final TagKey<Item> HULLBACK_EQUIPPABLE = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "hullback_equippable"));

}
