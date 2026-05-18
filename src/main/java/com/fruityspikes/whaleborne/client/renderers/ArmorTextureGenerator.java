package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/** Auto-generates modded-wood armor textures by delta-recoloring the oak template:
 *  per pixel, shift by (targetPalette - oakPalette) at its luminance position so grain
 *  and shading are preserved; green-dominant slime pixels are masked separately. */
public class ArmorTextureGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Whaleborne-ArmorTexGen");

    private static final ResourceLocation OAK_ARMOR_TEMPLATE = ResourceLocation.fromNamespaceAndPath(
            Whaleborne.MODID, "textures/entity/armor/hullback_oak_planks_armor.png");
    private static final ResourceLocation OAK_BLOCK_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/block/oak_planks.png");

    private static final Map<Item, CachedGenerated> CACHE = new HashMap<>();
    private static List<float[]> oakBlockPalette = null; // sorted by luminance, each float[] = {R, G, B} 0-255

    private static class CachedGenerated {
        final ResourceLocation location;
        final DynamicTexture texture;
        final NativeImage image;

        CachedGenerated(ResourceLocation location, DynamicTexture texture, NativeImage image) {
            this.location = location;
            this.texture = texture;
            this.image = image;
        }
    }

    public static ResourceLocation getOrGenerateArmorTexture(Item item) {
        CachedGenerated cached = CACHE.get(item);
        if (cached != null) return cached.location;

        if (!(item instanceof BlockItem blockItem)) return null;

        try {
            ResourceLocation blockTextureLoc = getBlockTexturePath(blockItem);
            if (blockTextureLoc == null) return null;

            List<float[]> targetPalette = extractPalette(blockTextureLoc);
            if (targetPalette == null || targetPalette.isEmpty()) return null;

            if (oakBlockPalette == null) {
                oakBlockPalette = extractPalette(OAK_BLOCK_TEXTURE);
                if (oakBlockPalette == null || oakBlockPalette.isEmpty()) return null;
            }

            NativeImage recolored = recolorArmorTemplate(oakBlockPalette, targetPalette);
            if (recolored == null) return null;

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            String texName = "autogen_armor_" + itemId.getNamespace() + "_" + itemId.getPath();
            ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, texName);

            DynamicTexture dynTex = new DynamicTexture(recolored);
            Minecraft.getInstance().getTextureManager().register(texLoc, dynTex);

            CACHE.put(item, new CachedGenerated(texLoc, dynTex, recolored));
            LOGGER.info("Auto-generated armor texture for {} ({} palette colors)",
                    itemId, targetPalette.size());
            return texLoc;

        } catch (Exception e) {
            LOGGER.warn("Failed to auto-generate armor texture for {}: {}",
                    BuiltInRegistries.ITEM.getKey(item), e.getMessage());
            return null;
        }
    }

    public static NativeImage getCachedNativeImage(Item item) {
        CachedGenerated cached = CACHE.get(item);
        return cached != null ? cached.image : null;
    }

    public static void clearCache() {
        for (CachedGenerated entry : CACHE.values()) {
            entry.texture.close();
        }
        CACHE.clear();
        oakBlockPalette = null;
    }

    // ========================= PALETTE EXTRACTION =========================

    /** Extract a luminance-sorted palette from a texture, reduced to ~32 representative colors. */
    private static List<float[]> extractPalette(ResourceLocation textureLoc) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Optional<Resource> res = mc.getResourceManager().getResource(textureLoc);
            if (res.isEmpty()) return null;

            try (InputStream in = res.get().open()) {
                NativeImage image = NativeImage.read(in);
                // Collect all opaque pixel colors (no dedup — we want the full distribution)
                List<float[]> pixels = new ArrayList<>();

                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int a = (pixel >> 24) & 0xFF;
                        if (a < 128) continue;

                        float r = pixel & 0xFF;
                        float g = (pixel >> 8) & 0xFF;
                        float b = (pixel >> 16) & 0xFF;
                        pixels.add(new float[]{r, g, b});
                    }
                }
                image.close();

                if (pixels.isEmpty()) return null;

                // Sort by luminance
                pixels.sort((a1, b1) -> Float.compare(luminance(a1), luminance(b1)));

                // Reduce to ~32 representative colors by uniform sampling
                int targetSize = Math.min(32, pixels.size());
                List<float[]> palette = new ArrayList<>(targetSize);
                for (int i = 0; i < targetSize; i++) {
                    float t = (float) i / (targetSize - 1) * (pixels.size() - 1);
                    int idx = Math.round(t);
                    palette.add(pixels.get(idx));
                }
                return palette;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static float luminance(float[] rgb) {
        return 0.299f * rgb[0] + 0.587f * rgb[1] + 0.114f * rgb[2];
    }

    private static float luminanceI(int r, int g, int b) {
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }

    // ========================= DELTA-BASED RECOLORING =========================

    /** Recolor the oak armor template by applying the target-vs-oak palette delta per pixel. */
    private static NativeImage recolorArmorTemplate(List<float[]> oakPalette, List<float[]> targetPalette) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Optional<Resource> res = mc.getResourceManager().getResource(OAK_ARMOR_TEMPLATE);
            if (res.isEmpty()) return null;

            // Resize target to match oak palette size
            List<float[]> resizedTarget = resizePalette(targetPalette, oakPalette.size());

            // Determine if target wood is green-ish (for slime complement)
            float[] targetAvg = computeAverageColor(targetPalette);
            boolean targetIsGreenish = targetAvg[1] > targetAvg[0] * 1.05f;

            try (InputStream in = res.get().open()) {
                NativeImage source = NativeImage.read(in);
                int w = source.getWidth();
                int h = source.getHeight();

                // PASS 1: Pre-scan armor template to find WOOD pixel luminance range
                // (excluding slime/transparent pixels)
                float armorMinLum = 255f, armorMaxLum = 0f;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int pixel = source.getPixelRGBA(x, y);
                        int pa = (pixel >> 24) & 0xFF;
                        if (pa == 0) continue;
                        int pr = pixel & 0xFF;
                        int pg = (pixel >> 8) & 0xFF;
                        int pb = (pixel >> 16) & 0xFF;
                        float psat = getSaturation(pr, pg, pb);
                        if ((pg > pr * 1.03f) && psat > 0.10f) continue; // skip slime
                        float lum = luminanceI(pr, pg, pb);
                        armorMinLum = Math.min(armorMinLum, lum);
                        armorMaxLum = Math.max(armorMaxLum, lum);
                    }
                }
                float armorLumRange = armorMaxLum - armorMinLum;

                // PASS 2: Apply delta-based recoloring
                NativeImage result = new NativeImage(w, h, true);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int pixel = source.getPixelRGBA(x, y);
                        int a = (pixel >> 24) & 0xFF;

                        if (a == 0) {
                            result.setPixelRGBA(x, y, 0);
                            continue;
                        }

                        int r = pixel & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = (pixel >> 16) & 0xFF;

                        // MASK: Green-dominant pixels = slime/moss
                        float sat = getSaturation(r, g, b);
                        boolean isSlime = (g > r * 1.03f) && sat > 0.10f;

                        if (isSlime) {
                            if (targetIsGreenish) {
                                result.setPixelRGBA(x, y, complementSlimeColor(r, g, b, a));
                            } else {
                                result.setPixelRGBA(x, y, pixel);
                            }
                            continue;
                        }

                        // Normalize against ARMOR TEMPLATE range (not block palette range)
                        float pixLum = luminanceI(r, g, b);
                        float t;
                        if (armorLumRange > 0.001f) {
                            t = Math.max(0f, Math.min(1f, (pixLum - armorMinLum) / armorLumRange));
                        } else {
                            t = 0.5f;
                        }

                        // Sample BOTH palettes at position t
                        float[] oakColor = samplePalette(oakPalette, t);
                        float[] targetColor = samplePalette(resizedTarget, t);

                        // Compute delta
                        float dr = targetColor[0] - oakColor[0];
                        float dg = targetColor[1] - oakColor[1];
                        float db = targetColor[2] - oakColor[2];

                        // Apply delta to original pixel
                        int nr = Math.min(255, Math.max(0, (int) (r + dr)));
                        int ng = Math.min(255, Math.max(0, (int) (g + dg)));
                        int nb = Math.min(255, Math.max(0, (int) (b + db)));

                        result.setPixelRGBA(x, y, (a << 24) | (nb << 16) | (ng << 8) | nr);
                    }
                }
                source.close();
                return result;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to recolor armor template: {}", e.getMessage());
            return null;
        }
    }

    /** Smoothly sample a palette at normalized position t (0=darkest, 1=lightest). */
    private static float[] samplePalette(List<float[]> palette, float t) {
        float idx = t * (palette.size() - 1);
        int lo = Math.min((int) idx, palette.size() - 1);
        int hi = Math.min(lo + 1, palette.size() - 1);
        float frac = idx - lo;

        float[] cLo = palette.get(lo);
        float[] cHi = palette.get(hi);

        return new float[]{
                cLo[0] + (cHi[0] - cLo[0]) * frac,
                cLo[1] + (cHi[1] - cLo[1]) * frac,
                cLo[2] + (cHi[2] - cLo[2]) * frac
        };
    }

    /** Resize a palette to target size by linear interpolation in luminance space. */
    private static List<float[]> resizePalette(List<float[]> palette, int targetSize) {
        if (palette.size() == targetSize) return new ArrayList<>(palette);
        if (palette.isEmpty()) return Collections.emptyList();

        List<float[]> result = new ArrayList<>(targetSize);
        for (int i = 0; i < targetSize; i++) {
            float t = palette.size() == 1 ? 0f : (float) i / (targetSize - 1) * (palette.size() - 1);
            int lo = Math.min((int) t, palette.size() - 1);
            int hi = Math.min(lo + 1, palette.size() - 1);
            float frac = t - lo;

            float[] c1 = palette.get(lo);
            float[] c2 = palette.get(hi);

            result.add(new float[]{
                    c1[0] + (c2[0] - c1[0]) * frac,
                    c1[1] + (c2[1] - c1[1]) * frac,
                    c1[2] + (c2[2] - c1[2]) * frac
            });
        }
        return result;
    }

    // ========================= SLIME COMPLEMENT =========================

    /** For green-tinted target woods: swap slime green dominance to a warm red complement. */
    private static int complementSlimeColor(int r, int g, int b, int a) {
        // Simple channel swap: move green energy to red for warm complement
        int nr = Math.min(255, (int) (g * 0.9f));
        int ng = Math.min(255, (int) (r * 0.7f));
        int nb = Math.min(255, (int) (b * 0.6f));
        return (a << 24) | (nb << 16) | (ng << 8) | nr;
    }

    // ========================= COLOR UTILITIES =========================

    private static float getSaturation(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        if (max == 0f) return 0f;
        return (max - min) / max;
    }

    private static float[] computeAverageColor(List<float[]> palette) {
        float totalR = 0, totalG = 0, totalB = 0;
        for (float[] c : palette) {
            totalR += c[0];
            totalG += c[1];
            totalB += c[2];
        }
        int n = palette.size();
        return new float[]{totalR / n, totalG / n, totalB / n};
    }

    // ========================= BLOCK TEXTURE LOOKUP =========================

    private static ResourceLocation getBlockTexturePath(BlockItem blockItem) {
        try {
            BlockState state = blockItem.getBlock().defaultBlockState();
            Minecraft mc = Minecraft.getInstance();
            BlockModelShaper shaper = mc.getBlockRenderer().getBlockModelShaper();
            BakedModel model = shaper.getBlockModel(state);

            var sprite = model.getParticleIcon();
            if (sprite == null) return null;

            ResourceLocation spriteName = sprite.contents().name();
            if (spriteName.getPath().equals("missingno")) return null;

            return ResourceLocation.fromNamespaceAndPath(
                    spriteName.getNamespace(),
                    "textures/" + spriteName.getPath() + ".png");
        } catch (Exception e) {
            return null;
        }
    }
}
