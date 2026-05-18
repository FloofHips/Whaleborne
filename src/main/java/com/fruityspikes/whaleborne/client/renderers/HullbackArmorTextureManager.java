package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HullbackArmorTextureManager {

    private static final Map<Integer, CachedTexture> cache = new HashMap<>();
    private static final ResourceLocation PROGRESS_MASK = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/hullback_armor_progress.png");

    private static class CachedTexture {
        public DynamicTexture texture;
        public ResourceLocation location;
        public Item armorItem;
        public float progress;
        
        public CachedTexture(DynamicTexture texture, ResourceLocation location, Item armorItem, float progress) {
            this.texture = texture;
            this.location = location;
            this.armorItem = armorItem;
            this.progress = progress;
        }
    }

    public static ResourceLocation getOrCreateDamagedTexture(HullbackEntity entity, ResourceLocation baseTexture, Item armorItem, float progress) {
        int entityId = entity.getId();

        CachedTexture cached = cache.get(entityId);
        
        // If we have a cached texture and the state hasn't changed, return it.
        if (cached != null && cached.armorItem == armorItem && Math.abs(cached.progress - progress) < 0.01f) {
            return cached.location;
        }

        // State changed or doesn't exist. Regenerate.
        try {
            Minecraft mc = Minecraft.getInstance();

            // Load base image: prefer auto-generated NativeImage, fall back to ResourceManager
            NativeImage baseImage;
            NativeImage generatedImage = ArmorTextureGenerator.getCachedNativeImage(armorItem);
            if (generatedImage != null) {
                // Copy the auto-generated image since we'll composite on it
                int gw = generatedImage.getWidth();
                int gh = generatedImage.getHeight();
                baseImage = new NativeImage(gw, gh, true);
                for (int gy = 0; gy < gh; gy++) {
                    for (int gx = 0; gx < gw; gx++) {
                        baseImage.setPixelRGBA(gx, gy, generatedImage.getPixelRGBA(gx, gy));
                    }
                }
            } else {
                Optional<Resource> baseRes = mc.getResourceManager().getResource(baseTexture);
                if (baseRes.isEmpty()) return baseTexture;
                baseImage = NativeImage.read(baseRes.get().open());
            }

            Optional<Resource> maskRes = mc.getResourceManager().getResource(PROGRESS_MASK);

            if (maskRes.isPresent()) {
                try (InputStream maskIn = maskRes.get().open()) {

                    NativeImage maskImage = NativeImage.read(maskIn);

                    int width = baseImage.getWidth();
                    int height = baseImage.getHeight();

                    // Create a new image for the composite
                    NativeImage compImage = new NativeImage(width, height, true);

                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int baseColor = baseImage.getPixelRGBA(x, y);
                            
                            // Map mask coordinates depending on its size.
                            int maskX = (int) ((x / (float) width) * maskImage.getWidth());
                            int maskY = (int) ((y / (float) height) * maskImage.getHeight());
                            int maskColor = maskImage.getPixelRGBA(maskX, maskY);
                            
                            // Replicates the dragonExplosionAlpha discard test: mask pixels with
                            // alpha above progress*255 survive (wood drawn), the rest go transparent.
                            int maskAlpha = (maskColor >> 24) & 0xFF;
                            int threshold = (int)(progress * 255);
                            
                            if (maskAlpha > threshold) {
                                // This pixel survives the alpha test → wood is visible here
                                compImage.setPixelRGBA(x, y, baseColor);
                            } else {
                                // This pixel is discarded → transparent (whale body shows)
                                compImage.setPixelRGBA(x, y, 0x00000000);
                            }
                        }
                    }

                    baseImage.close();
                    maskImage.close();

                    if (cached != null) {
                        cached.texture.close(); // Free old GPU texture
                    }

                    DynamicTexture newTexture = new DynamicTexture(compImage);
                    ResourceLocation newLocation = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "dynamic_armor_" + entityId);
                    mc.getTextureManager().register(newLocation, newTexture);

                    cache.put(entityId, new CachedTexture(newTexture, newLocation, armorItem, progress));
                    return newLocation;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fallback
        return baseTexture;
    }
    
    public static void clearCache(int entityId) {
        CachedTexture cached = cache.remove(entityId);
        if (cached != null) {
            cached.texture.close();
        }
    }
}
