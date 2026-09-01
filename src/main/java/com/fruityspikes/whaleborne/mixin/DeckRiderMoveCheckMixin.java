package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.server.entities.DeckRiderAnchors;
import com.fruityspikes.whaleborne.server.entities.DeckRiderPassage;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class DeckRiderMoveCheckMixin {

    @Shadow
    public ServerPlayer player;

    @Unique
    private double whaleborne$serverX;

    @Unique
    private double whaleborne$serverY;

    @Unique
    private double whaleborne$serverZ;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void whaleborne$rememberServerSpot(ServerboundMovePlayerPacket packet, CallbackInfo info) {
        this.whaleborne$serverX = this.player.getX();
        this.whaleborne$serverY = this.player.getY();
        this.whaleborne$serverZ = this.player.getZ();
    }

    @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("RETURN"), cancellable = true)
    private void whaleborne$keepDeckRidersWhereTheyClaim(LevelReader level, AABB box, double x, double y,
                                                         double z, CallbackInfoReturnable<Boolean> info) {
        if (!info.getReturnValueZ()) {
            return;
        }
        long now = this.player.level().getGameTime();
        DeckRiderAnchors.Anchor anchor = DeckRiderPassage.seated(this.player, now);
        if (anchor == null
                || !DeckRiderPassage.onlyHullIsNew(level, this.player, anchor.whale(), box, x, y, z)) {
            return;
        }
        DeckRiderPassage.reportSuppressedRollback(anchor, box, x, y, z,
                new Vec3(this.whaleborne$serverX, this.whaleborne$serverY, this.whaleborne$serverZ),
                now - anchor.stamp());
        info.setReturnValue(false);
    }
}
