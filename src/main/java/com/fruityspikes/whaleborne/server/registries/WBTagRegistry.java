package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class WBTagRegistry {
    public static final TagKey<Item> HULLBACK_EQUIPPABLE = ItemTags.create(new ResourceLocation(Whaleborne.MODID, "hullback_equippable"));
    /** Forge convention tag for cross-mod plank items. */
    public static final TagKey<Item> COMMON_PLANKS = ItemTags.create(new ResourceLocation("forge", "planks"));

    /**
     * Single source of truth for "is this a hull material": accepts vanilla planks,
     * the Forge cross-mod planks tag, and the explicit Whaleborne equippable list.
     */
    public static boolean isHullMaterial(ItemStack stack) {
        return stack.is(ItemTags.PLANKS) || stack.is(COMMON_PLANKS) || stack.is(HULLBACK_EQUIPPABLE);
    }
}
