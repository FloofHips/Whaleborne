package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.client.compat.WakesRenderGate;
import com.leclowndu93150.wakes.simulation.Brick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Brick.class, remap = false)
public class WakesBrickRecolorMixin {

    @Unique private int whaleborne$lastPaintedOccupied = -1;

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE",
                     target = "Lcom/leclowndu93150/wakes/simulation/Brick;populatePixels()V"),
            require = 0)
    private void whaleborne$gateRecolor(Brick self) {
        if (!WakesRenderGate.shouldRecolor(self)) return;
        boolean grew = self.occupied > whaleborne$lastPaintedOccupied;
        long gameTime = 0L;
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) gameTime = level.getGameTime();
        if (WakesRenderGate.shouldRecolorTemporal(self, gameTime, self.hasPopulatedPixels, grew)) {
            self.populatePixels();
            whaleborne$lastPaintedOccupied = self.occupied;
        }
    }
}
