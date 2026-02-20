package com.fruityspikes.whaleborne.client.compat;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraftforge.fml.ModList;

/**
 * Detects whether Oculus (Forge shader mod) is loaded.
 * When present, the Hullback renderer uses a single-pass composited texture
 * instead of the vanilla dragonExplosionAlpha + entityDecal two-pass technique,
 * which Oculus does not handle correctly.
 */
public class ShaderCompatLib {

    private static Boolean cachedResult = null;

    public static boolean isShaderModLoaded() {
        if (cachedResult == null) {
            boolean oculus = ModList.get().isLoaded("oculus");
            cachedResult = oculus;
            Whaleborne.LOGGER.info("Shader mod detection: Oculus={}, using safe render path={}", oculus, cachedResult);
        }
        return cachedResult;
    }
}
