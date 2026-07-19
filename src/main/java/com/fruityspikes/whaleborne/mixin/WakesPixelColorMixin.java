package com.fruityspikes.whaleborne.mixin;

import com.leclowndu93150.wakes.config.WakesConfig;
import com.leclowndu93150.wakes.render.WakeColor;
import com.leclowndu93150.wakes.simulation.SimulationNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SimulationNode.class, remap = false)
public class WakesPixelColorMixin {
    @Shadow public float[][][] u;

    @Unique private int whaleborne$cachedZ = -1;
    @Unique private boolean whaleborne$debug;
    @Unique private float[] whaleborne$r0;
    @Unique private float[] whaleborne$r1;
    @Unique private float[] whaleborne$r2;

    @Inject(method = "getPixelColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void whaleborne$fastPixelColor(int x, int z, int fluidCol, int lightCol, float opacity, CallbackInfoReturnable<Integer> cir) {
        if (z != this.whaleborne$cachedZ) {
            this.whaleborne$r0 = this.u[0][z + 1];
            this.whaleborne$r1 = this.u[1][z + 1];
            this.whaleborne$r2 = this.u[2][z + 1];
            this.whaleborne$debug = WakesConfig.DEBUG.debugColors.get();
            this.whaleborne$cachedZ = z;
        }
        if (this.whaleborne$debug) {
            return;
        }
        float waveEqAvg = (this.whaleborne$r0[x + 1] + this.whaleborne$r1[x + 1] + this.whaleborne$r2[x + 1]) / 3;
        cir.setReturnValue(WakeColor.sampleColor(waveEqAvg, fluidCol, lightCol, opacity));
    }
}
