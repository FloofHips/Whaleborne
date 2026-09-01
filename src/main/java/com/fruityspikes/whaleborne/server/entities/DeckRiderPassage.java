package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.WhaleborneDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


public final class DeckRiderPassage {

    private static final double SHAPE_MATCH = 1.0E-7;
    private static final double CHECK_DEFLATE = 1.0E-5F;
    private static final double DECK_REACH = 0.35;
    private static final double DECK_DROP = 1.0;

    private DeckRiderPassage() {
    }

    public static HullbackEntity carrier(LivingEntity rider) {
        long now = rider.level().getGameTime();
        if (!rider.level().isClientSide) {
            DeckRiderAnchors.Anchor anchor = seated(rider, now);
            return anchor == null ? null : anchor.whale();
        }
        if (DeckRiderLocalClaim.riderId() != rider.getId() || !DeckRiderLocalClaim.isCurrent(now)) {
            return null;
        }
        if (!(rider.level().getEntity(DeckRiderLocalClaim.whaleId()) instanceof HullbackEntity whale)) {
            return null;
        }
        return rider.level().getEntity(DeckRiderLocalClaim.tileId()) instanceof HullbackWalkableEntity tile
                && tile.getOwnerId() == whale.getId() && standsOn(rider, tile) ? whale : null;
    }

    public static DeckRiderAnchors.Anchor seated(LivingEntity rider, long gameTime) {
        DeckRiderAnchors.Anchor anchor = DeckRiderAnchors.current(rider, gameTime);
        return anchor != null && standsOn(rider, anchor.tile()) ? anchor : null;
    }

    public static boolean onlyHullIsNew(LevelReader level, Player rider, HullbackEntity whale,
                                        AABB oldBox, double x, double y, double z) {
        if (whale.level() != level) {
            return false;
        }
        AABB claimed = rider.getBoundingBox()
                .move(x - rider.getX(), y - rider.getY(), z - rider.getZ());
        VoxelShape before = Shapes.create(oldBox.deflate(CHECK_DEFLATE));
        boolean found = false;
        for (VoxelShape shape : level.getCollisions(rider, claimed.deflate(CHECK_DEFLATE))) {
            if (shape.isEmpty() || Shapes.joinIsNotEmpty(shape, before, BooleanOp.AND)) {
                continue;
            }
            if (!isHull(whale, shape.bounds())) {
                return false;
            }
            found = true;
        }
        return found;
    }

    public static void reportSuppressedRollback(DeckRiderAnchors.Anchor anchor, AABB oldBox,
                                                double x, double y, double z, Vec3 serverSpot, long age) {
        if (!WhaleborneDebug.on()) {
            return;
        }
        HullbackEntity whale = anchor.whale();
        Vec3 loss = new Vec3(x - serverSpot.x, y - serverSpot.y, z - serverSpot.z);
        Vec3 deck = DeckRiderAnchors.toDeckLocal(loss, DeckRiderAnchors.deckYaw(anchor));
        WhaleborneDebug.log(
                "deckrollback suppressed rider=%d whale=%d tile=%d tileTop=%.3f"
                        + " claimedFeet=%.3f oldFeet=%.3f along=%.4f lateral=%.4f dist=%.4f age=%d",
                anchor.rider().getId(), whale.getId(), anchor.tile().getId(),
                anchor.tile().getBoundingBox().maxY, y, oldBox.minY, deck.z, deck.x,
                loss.horizontalDistance(), age);
    }

    private static boolean standsOn(LivingEntity rider, HullbackWalkableEntity tile) {
        AABB box = rider.getBoundingBox();
        AABB deck = tile.getBoundingBox();
        return box.minY >= deck.maxY - DECK_DROP
                && box.maxX > deck.minX - DECK_REACH && box.minX < deck.maxX + DECK_REACH
                && box.maxZ > deck.minZ - DECK_REACH && box.minZ < deck.maxZ + DECK_REACH;
    }

    private static boolean isHull(HullbackEntity whale, AABB bounds) {
        for (Entity part : whale.getSubEntities()) {
            if (sameBox(bounds, part.getBoundingBox())) {
                return true;
            }
        }
        for (HullbackWalkableEntity tile : whale.level().getEntitiesOfClass(
                HullbackWalkableEntity.class, bounds.inflate(SHAPE_MATCH))) {
            if (tile.getOwnerId() == whale.getId() && sameBox(bounds, tile.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameBox(AABB one, AABB other) {
        return Math.abs(one.minX - other.minX) < SHAPE_MATCH
                && Math.abs(one.minY - other.minY) < SHAPE_MATCH
                && Math.abs(one.minZ - other.minZ) < SHAPE_MATCH
                && Math.abs(one.maxX - other.maxX) < SHAPE_MATCH
                && Math.abs(one.maxY - other.maxY) < SHAPE_MATCH
                && Math.abs(one.maxZ - other.maxZ) < SHAPE_MATCH;
    }
}
