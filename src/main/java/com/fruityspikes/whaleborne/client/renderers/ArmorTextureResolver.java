package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Resolves armor textures for any item (vanilla or modded), trying explicit textures
 *  then auto-generated ones, falling back to oak. Results cached per Item. */
public class ArmorTextureResolver {
    private static final Map<Item, ResourceLocation> CACHE = new HashMap<>();
    private static final ResourceLocation FALLBACK = ResourceLocation.fromNamespaceAndPath(
            Whaleborne.MODID, "textures/entity/armor/hullback_oak_planks_armor.png");

    /** Resolve the armor texture for an item (render-thread only; cache is not synchronized). */
    public static ResourceLocation resolve(Item item) {
        ResourceLocation cached = CACHE.get(item);
        if (cached != null) return cached;

        ResourceLocation resolved = doResolve(item);
        CACHE.put(item, resolved);
        return resolved;
    }

    private static ResourceLocation doResolve(Item item) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        String namespace = itemId.getNamespace();
        String path = itemId.getPath();

        // 1. Try whaleborne:textures/entity/armor/hullback_{path}_armor.png
        ResourceLocation tex1 = ResourceLocation.fromNamespaceAndPath(
                Whaleborne.MODID, "textures/entity/armor/hullback_" + path + "_armor.png");
        if (resourceExists(tex1)) return tex1;

        // 2. Try whaleborne:textures/entity/armor/hullback_{namespace}_{path}_armor.png (for modded)
        if (!namespace.equals("minecraft")) {
            ResourceLocation tex2 = ResourceLocation.fromNamespaceAndPath(
                    Whaleborne.MODID, "textures/entity/armor/hullback_" + namespace + "_" + path + "_armor.png");
            if (resourceExists(tex2)) return tex2;
        }

        // 3. Try {mod_namespace}:textures/entity/armor/hullback_{path}_armor.png (mod provides own)
        if (!namespace.equals("minecraft") && !namespace.equals(Whaleborne.MODID)) {
            ResourceLocation tex3 = ResourceLocation.fromNamespaceAndPath(
                    namespace, "textures/entity/armor/hullback_" + path + "_armor.png");
            if (resourceExists(tex3)) return tex3;
        }

        // 4. Auto-generate tinted texture for BlockItems (modded wood)
        if (item instanceof BlockItem) {
            ResourceLocation generated = ArmorTextureGenerator.getOrGenerateArmorTexture(item);
            if (generated != null) return generated;
        }

        // 5. Final fallback to oak
        return FALLBACK;
    }

    private static boolean resourceExists(ResourceLocation loc) {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
        return res.isPresent();
    }

    /** Clear cache on resource reload */
    public static void clearCache() {
        CACHE.clear();
        ArmorTextureGenerator.clearCache();
    }
}
