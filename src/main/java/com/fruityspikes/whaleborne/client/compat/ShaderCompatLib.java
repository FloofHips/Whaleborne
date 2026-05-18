package com.fruityspikes.whaleborne.client.compat;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Detects Iris/Oculus, which break certain RenderTypes (dragonExplosionAlpha,
 *  entityDecal); when present, a safe single-pass render path is used instead. */
public class ShaderCompatLib {
    private static final Logger LOGGER = LoggerFactory.getLogger("Whaleborne-ShaderCompat");
    private static Boolean cachedResult = null;

    /** True if Iris or Oculus is loaded (cached). Checks presence, not shader activity,
     *  since Iris corrupts GL state even after shaders are toggled off. */
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
