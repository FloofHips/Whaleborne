package com.fruityspikes.whaleborne.client.compat;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraftforge.fml.ModList;

/**
 * Detects whether a shader mod (Oculus/Optifine) is loaded, so the Hullback renderer
 * can fall back to a single-pass composited texture those mods handle correctly.
 */
public class ShaderCompatLib {

    private static Boolean cachedResult = null;

    public static boolean isShaderModLoaded() {
        if (cachedResult == null) {
            boolean oculus = ModList.get().isLoaded("oculus");
            boolean optifine = ModList.get().isLoaded("optifine");
            
            if (!optifine) {
                try {
                    Class.forName("net.optifine.Config");
                    optifine = true;
                } catch (ClassNotFoundException ignored) {
                }
            }
            
            cachedResult = oculus || optifine;
            Whaleborne.LOGGER.info("Shader mod detection: Oculus={}, Optifine={}, using safe render path={}", oculus, optifine, cachedResult);
        }
        return cachedResult;
    }
}
