package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves armor textures for any item, trying explicit/namespaced/mod-provided paths,
 * then palette-shifted generation for BlockItems, then a fallback. Cached per Item.
 */
public class ArmorTextureResolver {
    private static final Map<Item, ResourceLocation> CACHE = new HashMap<>();
    private static final ResourceLocation FALLBACK = new ResourceLocation(
            Whaleborne.MODID, "textures/entity/armor/hullback_oak_planks_armor.png");

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

        ResourceLocation tex1 = new ResourceLocation(
                Whaleborne.MODID, "textures/entity/armor/hullback_" + path + "_armor.png");
        if (resourceExists(tex1)) return tex1;

        if (!namespace.equals("minecraft")) {
            ResourceLocation tex2 = new ResourceLocation(
                    Whaleborne.MODID, "textures/entity/armor/hullback_" + namespace + "_" + path + "_armor.png");
            if (resourceExists(tex2)) return tex2;
        }

        if (!namespace.equals("minecraft") && !namespace.equals(Whaleborne.MODID)) {
            ResourceLocation tex3 = new ResourceLocation(
                    namespace, "textures/entity/armor/hullback_" + path + "_armor.png");
            if (resourceExists(tex3)) return tex3;
        }

        if (item instanceof BlockItem) {
            ResourceLocation generated = ArmorTextureGenerator.getOrGenerateArmorTexture(item);
            if (generated != null) return generated;
        }

        return FALLBACK;
    }

    private static boolean resourceExists(ResourceLocation loc) {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
        return res.isPresent();
    }

    /** Clear cache on resource reload. */
    public static void clearCache() {
        CACHE.clear();
        ArmorTextureGenerator.clearCache();
    }
}
