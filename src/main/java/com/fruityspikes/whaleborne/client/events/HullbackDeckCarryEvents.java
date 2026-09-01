package com.fruityspikes.whaleborne.client.events;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Whaleborne.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class HullbackDeckCarryEvents {

    private static final double RANGE = 24.0;
    private static final double DECK_MOTION_EPSILON = 1.0E-8;
    private static final float SWING_SCALE = 4.0F;

    private static final int PLAYER_PACKET_TICKS = 2;

    private static final Map<Integer, Vec3> PREVIOUS_STRIDE = new HashMap<>();
    private static final Set<Integer> CARRIED = new HashSet<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        DeckRiderSync.resolve(minecraft);
        List<HullbackEntity> whales = minecraft.level.getEntitiesOfClass(
                HullbackEntity.class, player.getBoundingBox().inflate(RANGE));
        if (whales.isEmpty()) {
            PREVIOUS_STRIDE.clear();
            return;
        }
        List<LivingEntity> aboard = minecraft.level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(RANGE));
        List<Player> localOnly = List.of(player);
        for (HullbackEntity whale : whales) {
            whale.carryDeckRiders(localOnly);
        }
        settleWalkAnimation(whales, aboard);
    }

    private static void settleWalkAnimation(List<HullbackEntity> whales, List<LivingEntity> riders) {
        CARRIED.clear();
        for (LivingEntity aboard : riders) {
            Vec3 deck = Vec3.ZERO;
            for (HullbackEntity whale : whales) {
                Vec3 motion = whale.deckMotionAt(aboard);
                if (motion.lengthSqr() >= DECK_MOTION_EPSILON) {
                    deck = motion;
                    break;
                }
            }
            if (deck.lengthSqr() < DECK_MOTION_EPSILON) {
                continue;
            }
            Vec3 stride = new Vec3(aboard.getX() - aboard.xo - deck.x, 0.0,
                    aboard.getZ() - aboard.zo - deck.z);
            Vec3 previous = PREVIOUS_STRIDE.put(aboard.getId(), stride);
            CARRIED.add(aboard.getId());
            Vec3 settled = previous == null ? stride
                    : stride.add(previous).scale(1.0 / PLAYER_PACKET_TICKS);
            aboard.walkAnimation.setSpeed(
                    (float) Math.min(settled.horizontalDistance() * SWING_SCALE, 1.0));
        }
        PREVIOUS_STRIDE.keySet().retainAll(CARRIED);
    }
}
