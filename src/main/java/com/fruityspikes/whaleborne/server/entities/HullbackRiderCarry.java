package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.WhaleborneDebug;
import com.fruityspikes.whaleborne.mixin.FloatingPlayerAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

public final class HullbackRiderCarry {

    private static final int DECK_PARTS = 3;
    private static final double TILE_QUERY_PAD = 8.0;
    private static final double TILE_QUERY_LIFT = 16.0;
    private static final double RIDER_PAD = 1.0;
    private static final double RIDER_HEADROOM = 4.0;
    private static final double RIDER_UNDERSHOOT = 0.5;
    private static final double SURFACE_SLACK = 0.35;
    private static final double SLACK_PER_LIFT = 2.0;
    private static final double PROBE_PAD_MIN = 0.35;
    private static final double PROBE_PAD_PER_SPEED = 2.0;
    private static final double LIFT_LIMIT = 0.2;
    private static final double FOOTING_LIFT = 0.012;
    private static final double BOB_PROBE_DEPTH = 0.1;
    private static final double RESEAT_LIMIT = 1.0;
    private static final double DEPTH_TIE = 1.0E-4;
    private static final int HOLD_TICKS = 6;
    private static final float MAX_TURN = 12.0F;
    private static final float MIN_TURN = 0.05F;
    private static final double MAX_TRAVEL_SQR = 4.0;
    private static final double MIN_CARRY_SQR = 1.0E-10;
    private static final int LOG_INTERVAL = 20;
    private static final int PROBE_HEARTBEAT = 100;
    private static final double PROBE_CLIP = 1.0E-3;

    private final HullbackEntity whale;
    private final Map<Integer, Integer> held = new HashMap<>();
    private final Map<Integer, Integer> footing = new HashMap<>();
    private final Set<Integer> flight = new HashSet<>();
    private final Map<Integer, Vec3> flightCarry = new HashMap<>();
    private final Map<Integer, Double> lastY = new HashMap<>();
    private final Map<Integer, Boolean> lastGround = new HashMap<>();
    private boolean launched;
    private final Map<Integer, HullSample> lastHull = new HashMap<>();
    private final Map<Integer, String> lastGate = new HashMap<>();

    private record HullSample(Vec3 local, int tick) {
    }

    private Map<Integer, Double> previousTops = new HashMap<>();
    private Map<Integer, Double> currentTops = new HashMap<>();
    private double pendingRise;
    private double deckJump;
    private double lastPlatformHeight;
    private int lastTick = Integer.MIN_VALUE;
    private double platformDelta;
    private double appliedUp;
    private double whaleTravel;
    private double hullStep;
    private Vec3 lastWhalePos;

    private int probeMisses;
    private int reportedSeated = -1;
    private String reportedGate = "none";
    private int rawTiles;
    private int ownedTiles;
    private double probeVerticalMiss;
    private double probeHorizontalMiss;
    private double probeWant;
    private double probeGot;
    private double probeHull;
    private int probeHullGap;
    private HullbackWalkableEntity probeTile;
    private Vec3 probeCarry = Vec3.ZERO;
    private AABB probeFrom;

    HullbackRiderCarry(HullbackEntity whale) {
        this.whale = whale;
    }

    void serverTick() {
        heartbeat();
        List<HullbackWalkableEntity> deck = openTick();
        if (deck == null) {
            return;
        }
        AABB box = riderBox(deck);
        for (Player aboard : whale.level().getEntitiesOfClass(Player.class, box)) {
            if (!(aboard instanceof ServerPlayer seated)) {
                continue;
            }
            if (seated.isPassenger() || seated.isSpectator()) {
                continue;
            }
            boolean found = !Double.isNaN(deckTopUnder(deck, seated.getBoundingBox(),
                    SURFACE_SLACK + deckJump, PROBE_PAD_MIN, seated.getId()));
            HullbackWalkableEntity support = probeTile;
            if (!found) {
                if (!heldRecently(seated.getId())) {
                    continue;
                }
                support = tileById(deck, footing.getOrDefault(seated.getId(), -1));
                if (support == null) {
                    continue;
                }
            }
            ((FloatingPlayerAccessor) seated.connection).whaleborne$setAboveGroundTickCount(0);
            if (found) {
                held.put(seated.getId(), whale.tickCount);
                if (support != null) {
                    footing.put(seated.getId(), support.getId());
                }
            }
            DeckRiderAnchors.set(seated, whale, support);
        }
        carry(deck, whale.level().getEntities(whale, box,
                rider -> accepts(rider) && !(rider instanceof Player)), false);
    }

    void clientTick(List<? extends Player> players) {
        heartbeat();
        List<HullbackWalkableEntity> deck = openTick();
        if (deck == null) {
            return;
        }
        AABB box = riderBox(deck);
        List<Entity> aboard = new ArrayList<>(players.size());
        for (Player player : players) {
            if (accepts(player) && player.getBoundingBox().intersects(box)) {
                aboard.add(player);
            }
        }
        if (aboard.isEmpty()) {
            report(0, 0.0, "offdeck");
            return;
        }
        carry(deck, aboard, true);
    }

    private List<HullbackWalkableEntity> openTick() {
        appliedUp = 0.0;
        if (whale.isRemoved() || !whale.arePartsInitialized()) {
            lastTick = Integer.MIN_VALUE;
            platformDelta = 0.0;
            deckJump = 0.0;
            return null;
        }
        int elapsed = whale.tickCount - lastTick;
        if (elapsed != 0) {
            double height = whale.getPlatformHeight();
            platformDelta = elapsed == 1 ? height - lastPlatformHeight : 0.0;
            lastPlatformHeight = height;
            hullStep = lastWhalePos == null || elapsed != 1 ? 0.0
                    : whale.position().subtract(lastWhalePos).horizontalDistance();
            whaleTravel += hullStep;
            lastWhalePos = whale.position();
            lastTick = whale.tickCount;
            if (elapsed != 1) {
                held.clear();
                footing.clear();
                flight.clear();
                deckJump = 0.0;
                return null;
            }
            forget();
        }

        List<HullbackWalkableEntity> deck = deckSurfaces();
        if (deck.isEmpty()) {
            report(0, 0.0, "nodeck");
            return null;
        }
        if (elapsed != 0) {
            Map<Integer, Double> swap = previousTops;
            previousTops = currentTops;
            currentTops = swap;
            currentTops.clear();
            deckJump = 0.0;
            for (HullbackWalkableEntity tile : deck) {
                double top = tile.getBoundingBox().maxY;
                currentTops.put(tile.getId(), top);
                Double before = previousTops.get(tile.getId());
                if (before != null) {
                    deckJump = Math.max(deckJump, Math.abs(top - before));
                }
            }
        }
        return deck;
    }

    private void carry(List<HullbackWalkableEntity> deck, List<Entity> riders, boolean client) {
        if (riders.isEmpty()) {
            report(0, 0.0, "noriders");
            return;
        }
        int seated = 0;
        double biggest = 0.0;
        String gate = "ok";
        for (Entity rider : riders) {
            if (client && rider instanceof Player walker) {
                bobReport(walker, deck);
            }
            probeWant = Double.NaN;
            probeGot = Double.NaN;
            probeHull = Double.NaN;
            probeHullGap = -1;
            deckTopUnder(deck, rider.getBoundingBox(), SURFACE_SLACK + deckJump, PROBE_PAD_MIN, rider.getId());
            HullbackWalkableEntity support = probeTile;
            boolean flying = inFlight(rider, deck);
            if (flying) {
                support = probeTile;
            } else if (support == null) {
                probeMisses++;
            }
            int part = support == null ? nearestDeckPart(rider)
                    : Mth.clamp(support.getAnchor(), 0, DECK_PARTS - 1);
            Vec3 pivot = whale.getOldPartPos(part);
            Vec3 travel = whale.getPartPos(part).subtract(pivot);
            if (travel.lengthSqr() > MAX_TRAVEL_SQR) {
                gate = "jump";
                probeReport(rider, part, deck, travel, Double.NaN, gate);
                continue;
            }
            float turn = turnOf();

            pendingRise = deckRise(support, travel.y + platformDelta);
            Vec3 carry = rigidCarry(rider.position(), pivot, travel, turn);
            if (carry.lengthSqr() <= MIN_CARRY_SQR) {
                gate = "still";
            }

            AABB box = rider.getBoundingBox();
            double top = Double.NaN;
            boolean onDeck = false;
            if (flying) {
                double leaving = launched ? deckStepOf(support) : 0.0;
                if (launched) {
                    flightCarry.put(rider.getId(), new Vec3(carry.x, 0.0, carry.z));
                }
                Vec3 inherited = flightCarry.get(rider.getId());
                carry = inherited == null
                        ? new Vec3(carry.x, leaving, carry.z)
                        : new Vec3(inherited.x, leaving, inherited.z);
                appliedUp = leaving;
                gate = "flying";
                held.put(rider.getId(), whale.tickCount);
                footing.put(rider.getId(), support.getId());
                if (launched) {
                    Vec3 speed = rider.getDeltaMovement();
                    rider.setDeltaMovement(speed.x, speed.y + leaving, speed.z);
                }
            } else {
                double reach = Math.max(PROBE_PAD_MIN, PROBE_PAD_PER_SPEED * travel.horizontalDistance());
                double slack = SURFACE_SLACK + Math.abs(carry.y) * SLACK_PER_LIFT;
                top = deckTopUnder(deck, box.move(carry.x, carry.y, carry.z), slack, reach, carry, rider.getId());
                if (Double.isNaN(top) && turn != 0.0F) {
                    carry = new Vec3(travel.x, carry.y, travel.z);
                    top = deckTopUnder(deck, box.move(carry.x, carry.y, carry.z), slack, reach, carry, rider.getId());
                }

                onDeck = !Double.isNaN(top);
                if (onDeck) {
                    held.put(rider.getId(), whale.tickCount);
                    if (probeTile != null) {
                        footing.put(rider.getId(), probeTile.getId());
                    }
                    double gap = (top + FOOTING_LIFT) - (box.minY + carry.y);
                    double settle = Math.max(LIFT_LIMIT, deckJump);
                    carry = carry.add(0.0, Mth.clamp(gap, -settle, settle), 0.0);
                    seated++;
                } else if (!heldRecently(rider.getId())) {
                    gate = "offsurface";
                    probeReport(rider, part, deck, travel, top, gate);
                    continue;
                }
            }

            double fromX = rider.getX();
            double fromZ = rider.getZ();
            probeCarry = carry;
            probeFrom = rider.getBoundingBox();
            rider.move(MoverType.SELF, carry);
            probeWant = Math.hypot(carry.x, carry.z);
            probeGot = Math.hypot(rider.getX() - fromX, rider.getZ() - fromZ);
            Vec3 hullLocal = rider.position().subtract(whale.position());
            HullSample previous = lastHull.put(rider.getId(), new HullSample(hullLocal, whale.tickCount));
            probeHullGap = previous == null ? -1 : whale.tickCount - previous.tick();
            probeHull = probeHullGap == 1
                    ? hullLocal.subtract(previous.local()).horizontalDistance() : Double.NaN;
            if (onDeck) {
                Vec3 velocity = rider.getDeltaMovement();
                if (support != null && deckStepOf(support) > 0.0 && velocity.y < 0.0) {
                    rider.setDeltaMovement(velocity.x, 0.0, velocity.z);
                }
                rider.setOnGround(true);
                rider.fallDistance = 0.0F;
            } else if (flying && support != null
                    && rider.getBoundingBox().minY - support.getBoundingBox().maxY <= LIFT_LIMIT) {
                rider.setOnGround(true);
                rider.fallDistance = 0.0F;
            }
            if ((onDeck || flying) && !client && support != null && !(rider instanceof Player)) {
                DeckRiderAnchors.set(rider, whale, support);
            }
            if ((onDeck || flying) && client && support != null) {
                float deckYaw = whale.getYRot();
                DeckRiderLocalClaim.set(rider.getId(), whale.getId(), support.getId(),
                        DeckRiderAnchors.toDeckLocal(
                                rider.position().subtract(support.position()), deckYaw),
                        whale.level().getGameTime());
            }
            if (client && rider instanceof Player && WhaleborneDebug.on()) {
                lastY.put(rider.getId(), rider.getY());
            }
            if (!client) {
                rider.hurtMarked = true;
            }
            biggest = Math.max(biggest, carry.length());
            probeReport(rider, part, deck, travel, top, onDeck || flying ? gate : "grace");
        }
        report(seated, biggest, gate);
    }

    private boolean accepts(Entity rider) {
        if (rider.isPassenger() || rider.isSpectator() || rider.noPhysics || !rider.isAlive()) {
            return false;
        }
        return !(rider instanceof HullbackEntity || rider instanceof HullbackPartEntity
                || rider instanceof HullbackWalkableEntity || rider instanceof WhaleWidgetEntity);
    }

    private List<HullbackWalkableEntity> deckSurfaces() {
        AABB near = null;
        for (int i = 0; i < DECK_PARTS; i++) {
            AABB part = whale.getSubEntities()[i].getBoundingBox();
            near = near == null ? part : near.minmax(part);
        }
        near = near.inflate(TILE_QUERY_PAD, TILE_QUERY_LIFT, TILE_QUERY_PAD);

        int id = whale.getId();
        List<HullbackWalkableEntity> all =
                whale.level().getEntitiesOfClass(HullbackWalkableEntity.class, near);
        rawTiles = all.size();
        List<HullbackWalkableEntity> owned = new ArrayList<>(all.size());
        for (HullbackWalkableEntity tile : all) {
            if (tile.getOwnerId() == id) {
                owned.add(tile);
            }
        }
        ownedTiles = owned.size();
        return owned;
    }

    private static AABB riderBox(List<HullbackWalkableEntity> deck) {
        AABB box = deck.get(0).getBoundingBox();
        for (int i = 1; i < deck.size(); i++) {
            box = box.minmax(deck.get(i).getBoundingBox());
        }
        return box.inflate(RIDER_PAD, 0.0, RIDER_PAD)
                .expandTowards(0.0, RIDER_HEADROOM, 0.0)
                .expandTowards(0.0, -RIDER_UNDERSHOOT, 0.0);
    }

    private double deckTopUnder(List<HullbackWalkableEntity> deck, AABB box, double slack, double reach, int riderId) {
        return deckTopUnder(deck, box, slack, reach, Vec3.ZERO, riderId);
    }

    private double deckTopUnder(List<HullbackWalkableEntity> deck, AABB box, double slack, double reach,
                                Vec3 swept, int riderId) {

        double best = Double.NaN;
        double standingTop = Double.NaN;
        double coveredTop = Double.NaN;
        double deepest = 0.0;
        int standing = riderId < 0 ? -1 : footing.getOrDefault(riderId, -1);
        HullbackWalkableEntity standingTile = null;
        HullbackWalkableEntity coveredTile = null;
        probeVerticalMiss = Double.NaN;
        probeHorizontalMiss = Double.NaN;
        probeTile = null;
        for (HullbackWalkableEntity tile : deck) {
            AABB piece = tile.getBoundingBox();
            AABB surface = swept.lengthSqr() > MIN_CARRY_SQR
                    ? piece.expandTowards(-swept.x, 0.0, -swept.z) : piece;
            double sideways = clearance(box, surface);
            if (Double.isNaN(probeHorizontalMiss) || sideways < probeHorizontalMiss) {
                probeHorizontalMiss = sideways;
            }
            if (sideways < reach) {
                double height = surface.maxY - box.minY;
                if (Double.isNaN(probeVerticalMiss) || Math.abs(height) < Math.abs(probeVerticalMiss)) {
                    probeVerticalMiss = height;
                }
            }
            double window = tile.getId() == standing ? RESEAT_LIMIT : slack;
            if (sideways >= reach || surface.maxY < box.minY - window || surface.maxY > box.minY + window) {
                continue;
            }
            double inside = clearance(box, piece);
            if (inside < 0.0) {
                if (tile.getId() == standing) {
                    standingTile = tile;
                    standingTop = piece.maxY;
                } else if (coveredTile == null || deeper(inside, tile, deepest, coveredTile)) {
                    deepest = inside;
                    coveredTile = tile;
                    coveredTop = piece.maxY;
                }
            } else if (Double.isNaN(best) || piece.maxY > best) {
                best = piece.maxY;
                probeTile = tile;
            }
        }
        if (standingTile != null) {
            probeTile = standingTile;
            return standingTop;
        }
        if (coveredTile != null) {
            probeTile = coveredTile;
            return coveredTop;
        }
        return best;
    }

    private boolean inFlight(Entity rider, List<HullbackWalkableEntity> deck) {
        int id = rider.getId();
        AABB box = rider.getBoundingBox();
        double top = flightTopUnder(deck, box, PROBE_PAD_MIN);
        double settle = Math.max(LIFT_LIMIT, deckJump);
        launched = false;
        boolean stillUp = !Double.isNaN(top)
                && (box.minY - top > LIFT_LIMIT || rider.getDeltaMovement().y > 0.0);
        if (!Double.isNaN(top) && box.minY > top
                && ((flight.contains(id) && stillUp) || box.minY - top > settle
                        || rider.getDeltaMovement().y > settle)) {
            launched = flight.add(id) && rider.getDeltaMovement().y > 0.0;
            return true;
        }
        if (!Double.isNaN(top) && probeTile != null) {
            double drop = -deckStepOf(probeTile);
            double reach = Math.max(0.0, -rider.getDeltaMovement().y) + rider.getGravity();
            if (drop - reach > LIFT_LIMIT) {
                flight.add(id);
                return true;
            }
        }
        if (flight.remove(id)) {
            Vec3 kept = flightCarry.remove(id);
            if (Double.isNaN(top)) {
                if (kept != null) {
                    Vec3 own = rider.getDeltaMovement();
                    rider.setDeltaMovement(own.x + kept.x, own.y, own.z + kept.z);
                }
                held.remove(id);
                footing.remove(id);
            }
        }
        return false;
    }

    private double flightTopUnder(List<HullbackWalkableEntity> deck, AABB box, double reach) {
        double best = Double.NaN;
        HullbackWalkableEntity found = null;
        for (HullbackWalkableEntity tile : deck) {
            AABB piece = tile.getBoundingBox();
            if (clearance(box, piece) >= reach || box.minY > piece.maxY + RIDER_HEADROOM) {
                continue;
            }
            if (Double.isNaN(best) || piece.maxY > best) {
                best = piece.maxY;
                found = tile;
            }
        }
        probeTile = found;
        return best;
    }

    private double deckStepOf(HullbackWalkableEntity tile) {
        Double now = currentTops.get(tile.getId());
        Double before = previousTops.get(tile.getId());
        return now == null || before == null ? 0.0 : now - before;
    }

    private static double clearance(AABB box, AABB surface) {
        return Math.max(
                Math.max(box.minX - surface.maxX, surface.minX - box.maxX),
                Math.max(box.minZ - surface.maxZ, surface.minZ - box.maxZ));
    }

    private static boolean deeper(double depth, HullbackWalkableEntity tile,
                                  double bestDepth, HullbackWalkableEntity bestTile) {
        if (Math.abs(depth - bestDepth) > DEPTH_TIE) {
            return depth < bestDepth;
        }
        return tile.getAnchor() < bestTile.getAnchor();
    }

    private int nearestDeckPart(Entity rider) {
        int best = 0;
        double bestSq = Double.MAX_VALUE;
        for (int i = 0; i < DECK_PARTS; i++) {
            Vec3 pos = whale.getPartPos(i);
            double dx = rider.getX() - pos.x;
            double dz = rider.getZ() - pos.z;
            double sq = dx * dx + dz * dz;
            if (sq < bestSq) {
                bestSq = sq;
                best = i;
            }
        }
        return best;
    }

    private float turnOf() {
        float delta = Mth.wrapDegrees(whale.getYRot() - whale.yRotO);
        return Math.abs(delta) < MIN_TURN ? 0.0F : Mth.clamp(delta, -MAX_TURN, MAX_TURN);
    }

    private Vec3 rigidCarry(Vec3 point, Vec3 pivot, Vec3 travel, float turn) {
        double up = pendingRise;
        appliedUp = up;
        if (turn == 0.0F) {
            return new Vec3(travel.x, up, travel.z);
        }
        Vec3 local = new Vec3(point.x - pivot.x, 0.0, point.z - pivot.z).yRot(-turn * Mth.DEG_TO_RAD);
        return new Vec3(pivot.x + local.x + travel.x - point.x, up,
                pivot.z + local.z + travel.z - point.z);
    }

    Vec3 contactMotion(Entity rider) {
        if (whale.isRemoved() || !whale.arePartsInitialized()) {
            return Vec3.ZERO;
        }
        List<HullbackWalkableEntity> deck = deckSurfaces();
        if (deck.isEmpty()) {
            return Vec3.ZERO;
        }
        int part = nearestDeckPart(rider);
        Vec3 pivot = whale.getOldPartPos(part);
        Vec3 travel = whale.getPartPos(part).subtract(pivot);
        if (travel.lengthSqr() > MAX_TRAVEL_SQR) {
            return Vec3.ZERO;
        }
        double lift = travel.y + platformDelta;
        double slack = SURFACE_SLACK + Math.abs(lift) * SLACK_PER_LIFT;
        if (Double.isNaN(deckTopUnder(deck, rider.getBoundingBox(), slack, PROBE_PAD_MIN, rider.getId()))) {
            return Vec3.ZERO;
        }
        int anchored = probeTile == null ? part : Mth.clamp(probeTile.getAnchor(), 0, DECK_PARTS - 1);
        if (anchored != part) {
            part = anchored;
            pivot = whale.getOldPartPos(part);
            travel = whale.getPartPos(part).subtract(pivot);
            if (travel.lengthSqr() > MAX_TRAVEL_SQR) {
                return Vec3.ZERO;
            }
            lift = travel.y + platformDelta;
        }
        float turn = turnOf();
        if (turn == 0.0F) {
            return new Vec3(travel.x, lift, travel.z);
        }
        Vec3 point = rider.position();
        Vec3 local = new Vec3(point.x - pivot.x, 0.0, point.z - pivot.z).yRot(-turn * Mth.DEG_TO_RAD);
        return new Vec3(pivot.x + local.x + travel.x - point.x, lift,
                pivot.z + local.z + travel.z - point.z);
    }

    private static HullbackWalkableEntity tileById(List<HullbackWalkableEntity> deck, int id) {
        if (id < 0) {
            return null;
        }
        for (HullbackWalkableEntity tile : deck) {
            if (tile.getId() == id) {
                return tile;
            }
        }
        return null;
    }

    private double deckRise(HullbackWalkableEntity support, double fallback) {
        if (support == null) {
            return fallback;
        }
        Double before = previousTops.get(support.getId());
        return before == null ? fallback : support.getBoundingBox().maxY - before;
    }

    private boolean heldRecently(int riderId) {
        Integer last = held.get(riderId);
        return last != null && whale.tickCount - last <= HOLD_TICKS;
    }

    private void forget() {
        Iterator<Map.Entry<Integer, Integer>> it = held.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            if (whale.tickCount - entry.getValue() > HOLD_TICKS) {
                footing.remove(entry.getKey());
                it.remove();
            }
        }
        flight.retainAll(held.keySet());
        lastHull.entrySet().removeIf(entry -> whale.tickCount - entry.getValue().tick() > HOLD_TICKS);
        lastGate.keySet().retainAll(lastHull.keySet());
        flightCarry.keySet().retainAll(flight);
        lastY.keySet().retainAll(held.keySet());
        lastGround.keySet().retainAll(held.keySet());
    }

    private void report(int seated, double carry, String gate) {
        reportedGate = gate;
        boolean changed = seated != reportedSeated;
        if (!changed && whale.tickCount % LOG_INTERVAL != 0) {
            return;
        }
        reportedSeated = seated;
        if (!WhaleborneDebug.on()) {
            return;
        }
        WhaleborneDebug.log(
                "deckcarry side=%s id=%d seated=%d tiles=%d/%d carry=%.4f dy=%.4f"
                        + " vmiss=%.3f hmiss=%.3f missTotal=%d travelTotal=%.2f occ=%s swim=%.3f gate=%s",
                whale.level().isClientSide ? "C" : "S", whale.getId(), seated, ownedTiles, rawTiles,
                carry, appliedUp, probeVerticalMiss, probeHorizontalMiss, probeMisses, whaleTravel,
                whale.isDeckOccupied(), whale.swimOffset(), gate);
    }

    private void heartbeat() {
        if (whale.tickCount % PROBE_HEARTBEAT != 0 || !WhaleborneDebug.on()) {
            return;
        }
        WhaleborneDebug.log(
                "deckalive side=%s id=%d tick=%d lastSeated=%d lastGate=%s",
                whale.level().isClientSide ? "C" : "S", whale.getId(), whale.tickCount,
                reportedSeated, reportedGate);
    }

    private void bobReport(Player walker, List<HullbackWalkableEntity> deck) {
        if (!WhaleborneDebug.on()) {
            return;
        }
        Boolean was = lastGround.put(walker.getId(), walker.onGround());
        if (was != null && was == walker.onGround() && whale.tickCount % PROBE_HEARTBEAT != 0) {
            return;
        }
        AABB self = walker.getBoundingBox();
        List<VoxelShape> found = walker.level().getEntityCollisions(
                walker, self.expandTowards(0.0, -BOB_PROBE_DEPTH, 0.0));
        double bestTop = Double.NEGATIVE_INFINITY;
        for (VoxelShape shape : found) {
            if (!shape.isEmpty()) {
                bestTop = Math.max(bestTop, shape.bounds().maxY);
            }
        }
        HullbackWalkableEntity near = deck.isEmpty() ? null : deck.get(0);
        Double before = lastY.get(walker.getId());
        WhaleborneDebug.log(
                "deckbob t=%d ground=%s vcol=%s vy=%.4f fell=%.4f hspeed=%.4f bob=%.4f"
                        + " shapes=%d insideBy=%.4f canHit=%s tiles=%d water=%s",
                whale.tickCount, walker.onGround(), walker.verticalCollision,
                walker.getDeltaMovement().y,
                before == null ? Double.NaN : walker.getY() - before,
                walker.getDeltaMovement().horizontalDistance(), walker.bob, found.size(),
                bestTop == Double.NEGATIVE_INFINITY ? Double.NaN : bestTop - self.minY,
                near == null ? "n/a" : String.valueOf(walker.canCollideWith(near)),
                deck.size(), walker.isInWater());
    }

    private void probeReport(Entity rider, int part, List<HullbackWalkableEntity> deck, Vec3 travel, double top, String gate) {
        boolean clipped = probeWant - probeGot > PROBE_CLIP;
        boolean turned = !gate.equals(lastGate.put(rider.getId(), gate));
        if (!clipped && !turned && whale.tickCount % PROBE_HEARTBEAT != 0) {
            return;
        }
        if (!WhaleborneDebug.on()) {
            return;
        }
        AABB box = rider.getBoundingBox();
        AABB partBox = whale.getSubEntities()[part].getBoundingBox();
        double best = Double.NaN;
        HullbackWalkableEntity highest = null;
        for (HullbackWalkableEntity tile : deck) {
            AABB surface = tile.getBoundingBox();
            if (surface.maxX <= box.minX || surface.minX >= box.maxX
                    || surface.maxZ <= box.minZ || surface.minZ >= box.maxZ) {
                continue;
            }
            if (Double.isNaN(best) || surface.maxY > best) {
                best = surface.maxY;
                highest = tile;
            }
        }
        WhaleborneDebug.log(
                "deckprobe side=%s id=%d rider=%d part=%d feet=%.3f whaleY=%.3f partY=%.3f"
                        + " partTop=%.3f deckTop=%.3f chosen=%.3f gap=%.3f travelY=%.4f plat=%.4f"
                        + " dxz=%.4f off=%s vy=%.4f ground=%s jump=%.4f hullStep=%.4f want=%s got=%s"
                        + " hull=%s hullgap=%d pick=%d/%d top=%d/%d clip=%s blockedBy=%s"
                        + " sneak=%s gate=%s",
                whale.level().isClientSide ? "C" : "S", whale.getId(), rider.getId(), part,
                box.minY, whale.getY(), whale.getPartPos(part).y, partBox.maxY, best, top,
                best - box.minY, travel.y, platformDelta, travel.horizontalDistance(),
                probeTile == null ? "none" : String.format(Locale.ROOT, "%.3f,%.3f",
                        rider.getX() - probeTile.getX(), rider.getZ() - probeTile.getZ()),
                rider.getDeltaMovement().y, rider.onGround(), deckJump, hullStep,
                measured(probeWant), measured(probeGot), measured(probeHull), probeHullGap,
                probeTile == null ? -1 : probeTile.getId(), highest == null ? -1 : highest.getId(),
                probeTile == null ? -1 : probeTile.getAnchor(), highest == null ? -1 : highest.getAnchor(),
                clipped, clipped ? blockers(rider) : "n/a",
                rider instanceof Player crouched && crouched.isShiftKeyDown(), gate);
    }

    private String blockers(Entity rider) {
        if (probeFrom == null) {
            return "n/a";
        }
        AABB swept = probeFrom.expandTowards(probeCarry.x, probeCarry.y, probeCarry.z);
        Map<String, Integer> hits = new TreeMap<>();
        for (Entity other : rider.level().getEntities(rider, swept, rider::canCollideWith)) {
            if (other.getBoundingBox().intersects(probeFrom)) {
                continue;
            }
            hits.merge(other.getClass().getSimpleName(), 1, Integer::sum);
        }
        for (VoxelShape shape : rider.level().getBlockCollisions(rider, swept)) {
            if (!shape.isEmpty() && !shape.bounds().intersects(probeFrom)) {
                hits.merge("block", 1, Integer::sum);
                break;
            }
        }
        return hits.isEmpty() ? "none" : hits.toString();
    }

    private static String measured(double value) {
        return Double.isNaN(value) ? "n/a" : String.format(Locale.ROOT, "%.4f", value);
    }
}
