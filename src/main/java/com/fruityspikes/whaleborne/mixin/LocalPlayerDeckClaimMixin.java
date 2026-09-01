package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.network.DeckRiderClaimPayload;
import com.fruityspikes.whaleborne.server.entities.DeckRiderLocalClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDeckClaimMixin {

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void whaleborne$claimDeckSpot(CallbackInfo info) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (Minecraft.getInstance().player != self || self.clientLevel == null) {
            return;
        }
        if (DeckRiderLocalClaim.riderId() != self.getId()
                || !DeckRiderLocalClaim.isCurrent(self.clientLevel.getGameTime())) {
            return;
        }
        Vec3 offset = DeckRiderLocalClaim.offset();
        PacketDistributor.sendToServer(new DeckRiderClaimPayload(
                DeckRiderLocalClaim.whaleId(), DeckRiderLocalClaim.tileId(),
                (float) offset.x, (float) offset.y, (float) offset.z));
    }
}
