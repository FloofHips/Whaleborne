package com.fruityspikes.whaleborne.client.compat;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects whether shader-altering mods (Iris, Oculus) are loaded in the modpack.
 * When these mods are present, certain vanilla RenderTypes (dragonExplosionAlpha,
 * entityDecal) break due to G-Buffer/depth-test incompatibilities. In that case,
 * Whaleborne uses a safe single-pass rendering path.
 */
public class ShaderCompatLib {
    private static final Logger LOGGER = LoggerFactory.getLogger("Whaleborne-ShaderCompat");
    private static Boolean cachedResult = null;

    /**
     * Returns true if Iris or Oculus is loaded in the modpack.
     * This check is cached after the first call since mods cannot be loaded/unloaded at runtime.
     * We intentionally check mod PRESENCE, not shader ACTIVITY, because Iris corrupts
     * OpenGL state even after toggling shaders off mid-session.
     */
    public static boolean isShaderModLoaded() {
        if (cachedResult == null) {
            boolean iris = ModList.get().isLoaded("iris");
            boolean oculus = ModList.get().isLoaded("oculus");
            cachedResult = iris || oculus;
            LOGGER.info("Shader mod detection: Iris={}, Oculus={}, using safe render path={}", iris, oculus, cachedResult);
        }
        return cachedResult;
    }
}
