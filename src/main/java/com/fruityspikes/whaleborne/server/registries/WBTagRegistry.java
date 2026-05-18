package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class WBTagRegistry {
    public static final TagKey<Item> HULLBACK_EQUIPPABLE = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "hullback_equippable"));

    // Common tag encouraged by NeoForge — modded planks should be here
    public static final TagKey<Item> COMMON_PLANKS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "planks"));

    /** Check if an item is a valid hull material (vanilla planks, common planks, or custom equippable) */
    public static boolean isHullMaterial(net.minecraft.world.item.ItemStack stack) {
        return stack.is(ItemTags.PLANKS) || stack.is(COMMON_PLANKS) || stack.is(HULLBACK_EQUIPPABLE);
    }
}
