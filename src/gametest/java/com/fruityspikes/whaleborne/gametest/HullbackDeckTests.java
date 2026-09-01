package com.fruityspikes.whaleborne.gametest;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.DeckRiderAnchors;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackWalkableEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Locale;

@GameTestHolder(Whaleborne.MODID)
@PrefixGameTestTemplate(false)
public final class HullbackDeckTests {

    private static final int FLOOR = 1;
    private static final int SPAN_X = 94;
    private static final int SPAN_Z = 46;
    private static final int SPAWN_X = 14;
    private static final int SPAWN_Z = 23;
    private static final int SETTLE_TICKS = 12;
    private static final int DRIVE_TICKS = 60;
    private static final int DECK_PARTS = 3;

    private static final double TILE_SEARCH = 16.0;
    private static final double FOLLOW_EPSILON = 0.02;
    private static final double CARRY_TOLERANCE = 0.35;
    private static final double CRUISE = 0.30;
    private static final double FAST = 0.60;
    private static final double BOB = 0.22;
    private static final double BOB_RATE = 0.35;
    private static final int BOB_TICKS = 20;
    private static final double MIN_PART_SPREAD = 0.05;
    private static final double FOOTING_TOLERANCE = 0.02;
    private static final double SEAM_FRACTION = 0.5;
    private static final double EDGE_OFFSET = 2.8;
    private static final double PACKET_NUDGE = 5.0;
    private static final int SEAM_TICKS = 40;
    private static final double SEAM_PROBE_LIFT = 8.0;
    private static final double SEAM_FOOTING_TOLERANCE = FOOTING_TOLERANCE;
    private static final int STEP_TICKS = 48;
    private static final int STEP_PERIOD = 8;
    private static final double STEP_SIZE = 0.45;
    private static final double CLIP_EPSILON = 1.0E-4;
    private static final int SWIM_TICKS = 140;
    private static final int SWIM_PERIOD = 126;
    private static final double SUNK_BY = 0.30;
    private static final double LOST_BY = 0.80;
    private static final int WARMUP_TICKS = 6;
    private static final int SUNK_TICKS = 20;
    private static final double SINK_RATE = 0.10;
    private static final int SINK_HALF = 12;
    private static final int SINK_CYCLES = 3;
    private static final double HULL_GAP = 0.05;
    private static final double HULL_DEPTH = 0.20;
    private static final double HULL_FALL = 1.5;
    private static final int STRANGER_X = 55;
    private static final double HOP = 0.12;
    private static final double DIAGONAL = 0.70710678;
    private static final int JUMP_WARMUP = 20;
    private static final int JUMP_WINDOW = 26;
    private static final int CONTROL_Z = 15;
    private static final double APEX_TOLERANCE = 0.05;
    private static final double HOP_TOLERANCE = 0.05;
    private static final double MIN_APEX = 1.0;
    private static final double RISE_RATE = 0.06;
    private static final int IDLE_WATCH_TICKS = 45;
    private static final int DECK_EMPTY_DISCARD_WATCH = 25;
    private static final int DECK_DISCARD_WAIT = 60;
    private static final double FAST_CRUISE = 0.95;
    private static final double FAST_SINK = 0.12;
    private static final double FAST_STRIDE = 0.10;
    private static final int FAST_TICKS = 40;
    private static final double TURN_ARM = 2.5;
    private static final float TURN_RATE = 3.0F;
    private static final int TURN_TICKS = 40;
    private static final double TURN_STEP = 0.05;

    private HullbackDeckTests() {
    }

    private static void floor(final GameTestHelper helper) {
        for (int x = 0; x < SPAN_X; x++) {
            for (int z = 0; z < SPAN_Z; z++) {
                helper.setBlock(new BlockPos(x, FLOOR, z), Blocks.STONE);
            }
        }
    }

    private static HullbackEntity whale(final GameTestHelper helper) {
        return helper.spawn(WBEntityRegistry.HULLBACK.get(), new BlockPos(SPAWN_X, FLOOR + 1, SPAWN_Z));
    }

    private static List<HullbackWalkableEntity> tiles(final GameTestHelper helper, final HullbackEntity whale) {
        int id = whale.getId();
        return helper.getLevel().getEntitiesOfClass(HullbackWalkableEntity.class,
                whale.getBoundingBox().inflate(TILE_SEARCH), tile -> tile.getOwnerId() == id);
    }

    private static double deckTopOver(final List<HullbackWalkableEntity> tiles, final HullbackPartEntity part) {
        double best = Double.NaN;
        for (HullbackWalkableEntity tile : tiles) {
            AABB box = tile.getBoundingBox();
            if (part.getX() < box.minX || part.getX() > box.maxX
                    || part.getZ() < box.minZ || part.getZ() > box.maxZ) {
                continue;
            }
            if (Double.isNaN(best) || box.maxY > best) {
                best = box.maxY;
            }
        }
        return best;
    }

    private static double deckTopUnderRider(final List<HullbackWalkableEntity> tiles, final LivingEntity rider) {
        AABB box = rider.getBoundingBox();
        double best = Double.NaN;
        for (HullbackWalkableEntity tile : tiles) {
            AABB piece = tile.getBoundingBox();
            if (piece.maxX <= box.minX || piece.minX >= box.maxX
                    || piece.maxZ <= box.minZ || piece.minZ >= box.maxZ) {
                continue;
            }
            if (Double.isNaN(best) || piece.maxY > best) {
                best = piece.maxY;
            }
        }
        return best;
    }

    private static HullbackWalkableEntity nearestTileOver(final List<HullbackWalkableEntity> tiles,
                                                          final LivingEntity rider) {
        HullbackWalkableEntity best = null;
        double bestGap = Double.MAX_VALUE;
        for (HullbackWalkableEntity tile : tiles) {
            AABB box = tile.getBoundingBox();
            if (rider.getX() < box.minX || rider.getX() > box.maxX
                    || rider.getZ() < box.minZ || rider.getZ() > box.maxZ) {
                continue;
            }
            double gap = Math.abs(box.maxY - rider.getBoundingBox().minY);
            if (gap < bestGap) {
                bestGap = gap;
                best = tile;
            }
        }
        return best;
    }

    private static HullbackWalkableEntity seatTile(final List<HullbackWalkableEntity> tiles, final Player rider) {
        HullbackWalkableEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (HullbackWalkableEntity tile : tiles) {
            double dx = rider.getX() - tile.getX();
            double dz = rider.getZ() - tile.getZ();
            double sq = dx * dx + dz * dz;
            if (best == null || sq < bestSq - 1.0E-6
                    || (sq < bestSq + 1.0E-6 && tile.getAnchor() < best.getAnchor())) {
                bestSq = sq;
                best = tile;
            }
        }
        return best;
    }

    private static double deckFootingMiss(final List<HullbackWalkableEntity> tiles, final Player rider) {
        double best = Double.NaN;
        for (HullbackWalkableEntity tile : tiles) {
            if (!covers(tile, rider)) {
                continue;
            }
            double miss = Math.abs(rider.getY() - tile.getBoundingBox().maxY);
            if (Double.isNaN(best) || miss < best) {
                best = miss;
            }
        }
        return best;
    }

    private static boolean covers(final HullbackWalkableEntity tile, final Player rider) {
        AABB piece = tile.getBoundingBox();
        AABB box = rider.getBoundingBox();
        return piece.maxX > box.minX && piece.minX < box.maxX
                && piece.maxZ > box.minZ && piece.minZ < box.maxZ;
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 200)
    public static void deckSpawnsOwnedTiles(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    if (owned.isEmpty()) {
                        helper.fail("no deck tile answered to this whale after " + SETTLE_TICKS
                                + " ticks: the carry can never find a surface");
                        return;
                    }
                    for (int part = 0; part < DECK_PARTS; part++) {
                        if (Double.isNaN(deckTopOver(owned, whale.getSubEntities()[part]))) {
                            helper.fail("part " + part + " has no tile over its centre, tiles=" + owned.size());
                            return;
                        }
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 200)
    public static void deckTileIgnoresTrackingPackets(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    if (owned.isEmpty()) {
                        helper.fail("no deck tile answered to this whale, nothing to test");
                        return;
                    }
                    HullbackWalkableEntity tile = owned.get(0);
                    Vec3 before = tile.position();
                    tile.lerpTo(before.x + PACKET_NUDGE, before.y + PACKET_NUDGE,
                            before.z + PACKET_NUDGE, 0.0F, 0.0F, 3);
                    double moved = tile.position().distanceTo(before);
                    if (moved > FOLLOW_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "an owned tile moved %.4f when a tracking packet was applied: the packet"
                                        + " and the owner both write the deck and it trembles between them",
                                moved));
                        return;
                    }
                    HullbackWalkableEntity loose = helper.spawn(
                            WBEntityRegistry.HULLBACK_PLATFORM.get(), new BlockPos(2, FLOOR + 2, 2));
                    Vec3 target = loose.position().add(PACKET_NUDGE, 0.0, PACKET_NUDGE);
                    loose.lerpTo(target.x, target.y, target.z, 0.0F, 0.0F, 3);
                    double missed = loose.position().distanceTo(target);
                    if (missed > FOLLOW_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "a tile with no owner ignored its tracking packet by %.4f: nothing else"
                                        + " can ever correct it", missed));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 200)
    public static void deckTilesCoverTheirParts(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    for (int part = 0; part < DECK_PARTS; part++) {
                        double need = whale.getSubEntities()[part].getBoundingBox().getXsize();
                        double widest = 0.0;
                        for (HullbackWalkableEntity tile : owned) {
                            if (tile.getAnchor() == part) {
                                widest = Math.max(widest, tile.getBoundingBox().getXsize());
                            }
                        }
                        if (widest < need) {
                            helper.fail(String.format(Locale.ROOT,
                                    "part %d is %.2f wide but its widest tile is only %.2f: the deck"
                                            + " does not reach the hull's flanks", part, need, widest));
                            return;
                        }
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void deckFollowsItsOwnPart(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        double[] reference = new double[DECK_PARTS];
        double[] worst = new double[DECK_PARTS];
        int[] samples = {0};
        double[] baseY = {0.0};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    baseY[0] = whale.getY();
                    for (int part = 0; part < DECK_PARTS; part++) {
                        reference[part] = Double.NaN;
                    }
                })
                .thenExecuteFor(DRIVE_TICKS, () -> {
                    double lift = Math.sin(samples[0] * BOB_RATE) * BOB;
                    whale.setPos(whale.getX(), baseY[0] + lift, whale.getZ());
                    samples[0]++;

                    whale.stationaryTicks = 200;
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    for (int part = 0; part < DECK_PARTS; part++) {
                        double top = deckTopOver(owned, whale.getSubEntities()[part]);
                        if (Double.isNaN(top)) {
                            continue;
                        }
                        double offset = top - whale.getSubEntities()[part].getY();
                        if (Double.isNaN(reference[part])) {
                            reference[part] = offset;
                        } else {
                            worst[part] = Math.max(worst[part], Math.abs(offset - reference[part]));
                        }
                    }
                })
                .thenExecute(() -> {
                    for (int part = 0; part < DECK_PARTS; part++) {
                        if (Double.isNaN(reference[part])) {
                            helper.fail("part " + part + " never had a tile over it during the drive");
                            return;
                        }
                        if (worst[part] > FOLLOW_EPSILON) {
                            helper.fail(String.format(Locale.ROOT,
                                    "tile over part %d drifted %.4f from its part while the hull moved"
                                            + " %.2f up and down, offset should be constant at %.4f",
                                    part, worst[part], BOB * 2, reference[part]));
                            return;
                        }
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderHoldsDeckSpotAtCruise(final GameTestHelper helper) {
        carryDrive(helper, CRUISE);
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderHoldsDeckSpotAtSpeed(final GameTestHelper helper) {
        carryDrive(helper, FAST);
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderHoldsDeckSpotOverMovingParts(final GameTestHelper helper) {
        bobDrive(helper, 0.0, 0.0, "over the body centre");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderHoldsDeckSpotAtPartSeam(final GameTestHelper helper) {
        bobDrive(helper, SEAM_FRACTION, 0.0, "on the body/head seam");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderHoldsDeckSpotAtDeckEdge(final GameTestHelper helper) {
        bobDrive(helper, 0.0, EDGE_OFFSET, "at the side of the body tile");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 400)
    public static void riderKeepsDeckSpotThroughVerticalSteps(final GameTestHelper helper) {
        stepDrive(helper, 0.0, 0.0, "over the body centre");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 400)
    public static void riderKeepsSeamSpotThroughVerticalSteps(final GameTestHelper helper) {
        stepDrive(helper, SEAM_FRACTION, 0.0, "on the body/head seam");
    }

    private static void stepDrive(final GameTestHelper helper, final double along, final double across,
                                  final String where) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        double[] anchor = new double[2];
        double[] worst = {0.0};
        boolean[] ready = {false};
        double[] baseY = {0.0};
        int[] samples = {0};
        double[] partSpread = {0.0};
        double[] floated = {0.0};
        int[] unsupported = {0};
        double[] backward = {0.0};
        double[] drift = {0.0};
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to stand on");
                        return;
                    }
                    Vec3 offset = seatOffset(whale, along, across);
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX() + offset.x, top, body.getZ() + offset.z);
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the seat " + where + " is over no tile at all, nothing to stand on");
                        return;
                    }
                    player.setPos(player.getX(), under, player.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    rider[0] = player;
                    baseY[0] = whale.getY();
                    seat[0] = seatTile(owned, player);
                    anchor[0] = player.getX() - seat[0].getX();
                    anchor[1] = player.getZ() - seat[0].getZ();
                    ready[0] = true;
                })
                .thenExecuteFor(STEP_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    Vec3 fall = whale.getDeltaMovement();
                    whale.setDeltaMovement(fall.x, 0.0, fall.z);
                    int phase = samples[0] / STEP_PERIOD;
                    double lift = (phase % 2 == 0) ? 0.0 : STEP_SIZE;
                    samples[0]++;
                    whale.setPos(whale.getX() + CRUISE, baseY[0] + lift, whale.getZ());
                    whale.carryDeckRiders(List.of(rider[0]));

                    partSpread[0] = Math.max(partSpread[0],
                            Math.abs(whale.getPartPos(1).y - whale.getPartPos(2).y));
                    double under = deckTopUnderRider(tiles(helper, whale), rider[0]);
                    if (Double.isNaN(under)) {
                        unsupported[0]++;
                    } else {
                        floated[0] = Math.max(floated[0], Math.abs(rider[0].getY() - under));
                    }
                    double dx = (rider[0].getX() - seat[0].getX()) - anchor[0];
                    double dz = (rider[0].getZ() - seat[0].getZ()) - anchor[1];
                    drift[0] = Math.sqrt(dx * dx + dz * dz);
                    worst[0] = Math.max(worst[0], drift[0]);
                    backward[0] = Math.min(backward[0], dx);
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (partSpread[0] < MIN_PART_SPREAD) {
                        helper.fail(String.format(Locale.ROOT,
                                "head and body never separated vertically (%.4f): this drive does not"
                                        + " reproduce the defect and the result below means nothing",
                                partSpread[0]));
                        return;
                    }
                    if (unsupported[0] > 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s had no tile under it on %d of %d stepped ticks,"
                                        + " drift %.3f, worst %.3f, backward %.3f",
                                where, unsupported[0], STEP_TICKS, drift[0], worst[0], backward[0]));
                        return;
                    }
                    if (floated[0] > FOOTING_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s floated %.4f off the tile under it across %d steps of"
                                        + " %.2f: the carry is not putting it back on the deck",
                                where, floated[0], STEP_TICKS / STEP_PERIOD, STEP_SIZE));
                        return;
                    }
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s slid %.3f (ended %.3f off, %.3f of it backward) over %d"
                                        + " ticks with the deck stepping %.2f every %d ticks"
                                        + " (tolerance %.2f), parts %.4f apart",
                                where, worst[0], drift[0], backward[0], STEP_TICKS, STEP_SIZE,
                                STEP_PERIOD, CARRY_TOLERANCE, partSpread[0]));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 900)
    public static void riderHoldsDeckSpotThroughSwimCycle0(final GameTestHelper helper) {
        swimDrive(helper, 0.0, 0.0, 0, "over the body centre");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 900)
    public static void riderHoldsDeckSpotThroughSwimCycle1(final GameTestHelper helper) {
        swimDrive(helper, 0.0, 0.0, 1, "over the body centre");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 900)
    public static void riderHoldsDeckSpotThroughSwimCycle2(final GameTestHelper helper) {
        swimDrive(helper, 0.0, 0.0, 2, "over the body centre");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 900)
    public static void riderHoldsDeckSpotThroughSwimCycle3(final GameTestHelper helper) {
        swimDrive(helper, 0.0, 0.0, 3, "over the body centre");
    }


    @GameTest(template = "hullback_deck", timeoutTicks = 900)
    public static void riderHoldsSeamSpotThroughSwimCycle0(final GameTestHelper helper) {
        swimDrive(helper, SEAM_FRACTION, 0.0, 0, "on the body/head seam");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 900)
    public static void riderHoldsSeamSpotThroughSwimCycle1(final GameTestHelper helper) {
        swimDrive(helper, SEAM_FRACTION, 0.0, 1, "on the body/head seam");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 900)
    public static void riderHoldsSeamSpotThroughSwimCycle2(final GameTestHelper helper) {
        swimDrive(helper, SEAM_FRACTION, 0.0, 2, "on the body/head seam");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 900)
    public static void riderHoldsSeamSpotThroughSwimCycle3(final GameTestHelper helper) {
        swimDrive(helper, SEAM_FRACTION, 0.0, 3, "on the body/head seam");
    }

    private static void swimDrive(final GameTestHelper helper, final double along, final double across,
                                  final int phase, final String where) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        double[] anchor = new double[2];
        double[] worst = {0.0};
        boolean[] ready = {false};
        double[] holdY = {0.0};
        int[] tick = {0};
        double[] partLow = {Double.MAX_VALUE};
        double[] partHigh = {-Double.MAX_VALUE};
        double[] biggestStep = {0.0};
        int[] worstTick = {0};
        int[] unsupported = {0};
        double[] floated = {0.0};
        boolean[] breached = {false};
        int[] minTiles = {Integer.MAX_VALUE};
        int[] firstLoss = {0};
        int[] occupiedTicks = {0};
        int[] firstMiss = {0};
        int[] unindexed = {0};
        double[] backward = {0.0};
        double[] lastDrift = {0.0};
        double[] lastTop = {Double.NaN};
        double[] downLoss = {0.0};
        double[] upLoss = {0.0};
        double[] flatLoss = {0.0};
        int[] downTicks = {0};
        int[] upTicks = {0};
        double[] clipped = {0.0};
        java.util.Map<String, Integer> blockers = new java.util.TreeMap<>();
        java.util.List<String> clipTrace = new java.util.ArrayList<>();
        double[] lastTileX = {Double.NaN};
        double[] mismatchDown = {0.0};
        double[] mismatchUp = {0.0};
        double[] mismatchFlat = {0.0};
        double[] offsetAfter = {Double.NaN};
        double[] outsideCarry = {0.0};
        double[] insideCarry = {0.0};
        double[] outsideDown = {0.0};
        double[] insideDown = {0.0};
        java.util.List<String> vsTrace = new java.util.ArrayList<>();
        double[] missGap = {0.0};
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to stand on");
                        return;
                    }
                    Vec3 offset = seatOffset(whale, along, across);
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX() + offset.x, top, body.getZ() + offset.z);
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the seat " + where + " is over no tile at all, nothing to stand on");
                        return;
                    }
                    player.setPos(player.getX(), under, player.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    helper.getLevel().addFreshEntity(player);
                    rider[0] = player;
                    holdY[0] = whale.getY();
                    ready[0] = true;
                })
                .thenWaitUntil(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    long at = helper.getLevel().getGameTime() % SWIM_PERIOD;
                    if (at != (long) phase * SWIM_PERIOD / 4L) {
                        helper.fail("waiting for swim phase " + phase + ", at " + at);
                    }
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    seat[0] = seatTile(owned, rider[0]);
                    if (seat[0] == null) {
                        helper.fail("the rider lost its tile while waiting for swim phase " + phase);
                        ready[0] = false;
                        return;
                    }
                    anchor[0] = rider[0].getX() - seat[0].getX();
                    anchor[1] = rider[0].getZ() - seat[0].getZ();
                    holdY[0] = whale.getY();
                })
                .thenExecuteFor(SWIM_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (whale.isBreaching()) {
                        breached[0] = true;
                    }
                    whale.setDeltaMovement(CRUISE, 0.0, 0.0);
                    whale.setYRot(0.0F);
                    whale.setYBodyRot(0.0F);
                    whale.yRotO = 0.0F;
                    whale.setXRot(0.0F);
                    whale.xRotO = 0.0F;
                    whale.setPos(whale.getX(), holdY[0], whale.getZ());
                    if (seat[0] == null) {
                        helper.fail("the rider left every tile, so nothing after this is measurable");
                        return;
                    }
                    Vec3 want = whale.deckMotionAt(rider[0]);
                    double tileXNow = seat[0].getX();
                    double tileTopNow = seat[0].getBoundingBox().maxY;
                    double prevTileX = lastTileX[0];
                    double offsetBefore = rider[0].getX() - tileXNow;
                    boolean descending = !Double.isNaN(lastTop[0]) && tileTopNow - lastTop[0] < -1.0E-6;
                    if (!Double.isNaN(offsetAfter[0])) {
                        double outside = offsetBefore - offsetAfter[0];
                        outsideCarry[0] += Math.abs(outside);
                        if (descending) {
                            outsideDown[0] += outside;
                        }
                    }
                    double beforeX = rider[0].getX();
                    double beforeZ = rider[0].getZ();
                    double partDx = whale.getPartPos(2).x - whale.getOldPartPos(2).x;
                    AABB swept = rider[0].getBoundingBox().expandTowards(partDx, -0.5, 0.0);
                    whale.carryDeckRiders(List.of(rider[0]));
                    tick[0]++;
                    double got = Math.hypot(rider[0].getX() - beforeX, rider[0].getZ() - beforeZ);
                    double lost = want.horizontalDistance() - got;
                    if (lost > CLIP_EPSILON) {
                        clipped[0] += lost;
                        for (Entity blocker : rider[0].level().getEntities(rider[0], swept,
                                Entity::canBeCollidedWith)) {
                            blockers.merge(blocker.getClass().getSimpleName(), 1, Integer::sum);
                        }
                        if (clipTrace.size() < 8) {
                            clipTrace.add(String.format(Locale.ROOT, "t%d want=(%.3f,%.3f) got=%.3f",
                                    tick[0], want.x, want.y, got));
                        }
                    }

                    double partY = whale.getPartPos(2).y - whale.getY();
                    partLow[0] = Math.min(partLow[0], partY);
                    partHigh[0] = Math.max(partHigh[0], partY);
                    biggestStep[0] = Math.max(biggestStep[0],
                            Math.abs(whale.getPartPos(2).y - whale.getOldPartPos(2).y));

                    List<HullbackWalkableEntity> now = tiles(helper, whale);
                    if (now.size() < minTiles[0]) {
                        minTiles[0] = now.size();
                        if (now.size() < DECK_PARTS && firstLoss[0] == 0) {
                            firstLoss[0] = tick[0];
                        }
                    }
                    if (whale.isDeckOccupied()) {
                        occupiedTicks[0]++;
                    }
                    if (!whale.level().getEntitiesOfClass(Player.class,
                            rider[0].getBoundingBox().inflate(2.0)).contains(rider[0])) {
                        unindexed[0]++;
                    }
                    double under = deckTopUnderRider(now, rider[0]);
                    if (Double.isNaN(under)) {
                        unsupported[0]++;
                        if (firstMiss[0] == 0) {
                            firstMiss[0] = tick[0];
                            missGap[0] = rider[0].getY() - deckTopOver(now, whale.getSubEntities()[2]);
                        }
                    } else {
                        floated[0] = Math.max(floated[0], Math.abs(rider[0].getY() - under));
                    }
                    double dx = (rider[0].getX() - seat[0].getX()) - anchor[0];
                    double dz = (rider[0].getZ() - seat[0].getZ()) - anchor[1];
                    double drift = Math.sqrt(dx * dx + dz * dz);
                    backward[0] = Math.min(backward[0], dx);
                    double topNow = seat[0].getBoundingBox().maxY;
                    if (!Double.isNaN(lastTileX[0]) && !Double.isNaN(lastTop[0])) {
                        double tileMoved = tileXNow - lastTileX[0];
                        double off = Math.abs(tileMoved - want.x);
                        double vert = tileTopNow - lastTop[0];

                        if (vert < -1.0E-6) {
                            mismatchDown[0] += off;
                        } else if (vert > 1.0E-6) {
                            mismatchUp[0] += off;
                        } else {
                            mismatchFlat[0] += off;
                        }
                    }
                    lastTileX[0] = tileXNow;
                    double inside = (rider[0].getX() - seat[0].getX()) - offsetBefore;
                    insideCarry[0] += Math.abs(inside);
                    if (descending) {
                        insideDown[0] += inside;
                    }
                    double moved = rider[0].getX() - beforeX;
                    if (tick[0] > 30 && Math.abs(partDx) > 0.05 && Math.abs(moved) < 0.5 * Math.abs(partDx)
                            && vsTrace.size() < 6) {
                        AABB pb = whale.getSubEntities()[2].getBoundingBox();
                        AABB rb = rider[0].getBoundingBox();
                        vsTrace.add(String.format(Locale.ROOT,
                                "SHORT t%d want=%+.4f got=%+.4f | feet=%.3f partTop=%.3f"
                                        + " overlap=%.3f | tileTop=%.3f",
                                tick[0], partDx, moved, rb.minY, pb.maxY, pb.maxY - rb.minY,
                                seat[0].getBoundingBox().maxY));
                    }
                    if (false && tick[0] > 20 && vsTrace.size() < 8) {
                        vsTrace.add(String.format(Locale.ROOT,
                                "t%d tile=%+.4f p0=%+.4f p1=%+.4f p2=%+.4f yaw2=%.3f rot=%s",
                                tick[0], tileXNow - prevTileX,
                                whale.getPartPos(0).x - whale.getOldPartPos(0).x,
                                whale.getPartPos(1).x - whale.getOldPartPos(1).x,
                                whale.getPartPos(2).x - whale.getOldPartPos(2).x,
                                whale.getPartYRot(2), seat[0].rotatesWithPart()));
                    }
                    if (false && Math.abs(partDx) > 0.05 && Math.abs(rider[0].getX() - beforeX) < 1.0E-4
                            && vsTrace.size() < 6) {
                        StringBuilder who = new StringBuilder();
                        for (Entity e : rider[0].level().getEntities(rider[0], swept,
                                Entity::canBeCollidedWith)) {
                            who.append(e.getClass().getSimpleName()).append('@')
                               .append(String.format(Locale.ROOT, "%.2f..%.2f",
                                       e.getBoundingBox().minY, e.getBoundingBox().maxY)).append(' ');
                        }
                        vsTrace.add(String.format(Locale.ROOT,
                                "STUCK t%d p2=%+.4f riderY=%.3f box=%.3f..%.3f blocked by [%s]",
                                tick[0], partDx, rider[0].getY(), rider[0].getBoundingBox().minY,
                                rider[0].getBoundingBox().maxY, who.toString().trim()));
                    }
                    if (false && descending && vsTrace.size() < 8) {
                        vsTrace.add(String.format(Locale.ROOT,
                                "t%d deck=%+.4f p0=%+.4f p1=%+.4f p2=%+.4f carry=%+.4f anchor=%d",
                                tick[0], -(offsetBefore - offsetAfter[0]),
                                whale.getPartPos(0).x - whale.getOldPartPos(0).x,
                                whale.getPartPos(1).x - whale.getOldPartPos(1).x,
                                whale.getPartPos(2).x - whale.getOldPartPos(2).x,
                                rider[0].getX() - beforeX, seat[0].getAnchor()));
                    }
                    offsetAfter[0] = rider[0].getX() - seat[0].getX();
                    double gained = drift - lastDrift[0];
                    if (!Double.isNaN(lastTop[0]) && gained > 0.0) {
                        double vertical = topNow - lastTop[0];
                        if (vertical < -1.0E-6) {
                            downLoss[0] += gained;
                            downTicks[0]++;
                        } else if (vertical > 1.0E-6) {
                            upLoss[0] += gained;
                            upTicks[0]++;
                        } else {
                            flatLoss[0] += gained;
                        }
                    }
                    lastTop[0] = topNow;
                    lastDrift[0] = drift;
                    if (drift > worst[0]) {
                        worst[0] = drift;
                        worstTick[0] = tick[0];
                    }
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    String shape = String.format(Locale.ROOT,
                            "part2 y in [%.3f,%.3f] range %.3f, biggest one-tick %.4f, swim %.4f,"
                                    + " occupied %s on %d/%d, breached %s, feet %.3f,"
                                    + " tiles down to %d first at t%d, first miss t%d gap %.3f,"
                                    + " rider unindexed on %d ticks, backward %.3f"
                                    + " | lost down %.3f on %d ticks, up %.3f on %d, flat %.3f"
                                    + " | clipped %.3f by %s %s"
                                    + " | tile vs part mismatch down %.3f up %.3f flat %.3f"
                                    + " | moved outside carry %.3f (down %.3f), inside %.3f (down %.3f)"
                                    + " | %s",
                            partLow[0], partHigh[0], partHigh[0] - partLow[0], biggestStep[0],
                            whale.getAnimationSwimSpeed(), whale.isDeckOccupied(), occupiedTicks[0],
                            SWIM_TICKS, breached[0], rider[0].getY(),
                            minTiles[0], firstLoss[0], firstMiss[0], missGap[0], unindexed[0],
                            backward[0], downLoss[0], downTicks[0], upLoss[0], upTicks[0],
                            flatLoss[0], clipped[0], blockers, clipTrace,
                            mismatchDown[0], mismatchUp[0], mismatchFlat[0],
                            outsideCarry[0], outsideDown[0], insideCarry[0], insideDown[0],
                            vsTrace);
                    if (partHigh[0] - partLow[0] < MIN_PART_SPREAD) {
                        helper.fail("the swim cycle never moved the body part vertically: " + shape);
                        return;
                    }
                    if (unsupported[0] > 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s had no tile under it on %d of %d ticks | %s",
                                where, unsupported[0], SWIM_TICKS, shape));
                        return;
                    }
                    if (floated[0] > FOOTING_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s floated %.4f off the tile under it | %s",
                                where, floated[0], shape));
                        return;
                    }
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s slid %.3f by tick %d of %d at %.2f/tick"
                                        + " (tolerance %.2f) | %s",
                                where, worst[0], worstTick[0], SWIM_TICKS, CRUISE, CARRY_TOLERANCE,
                                shape));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void hullDoesNotBlockItsOwnRider(final GameTestHelper helper) {
        sunkDrive(helper, SUNK_BY);
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderIsReseatedAfterFallingBehind(final GameTestHelper helper) {
        sunkDrive(helper, LOST_BY);
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void hullPartsDoNotClipTheirOwnDeckRider(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        boolean[] ready = {false};
        double[] carried = {0.0};
        double[] outsider = {0.0};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
                    Player passerby = helper.makeMockPlayer(GameType.SURVIVAL);
                    HullApproach spot = hullApproach(helper, whale, owned, rider, HULL_DEPTH);
                    if (spot == null) {
                        return;
                    }
                    hullApproach(helper, whale, owned, passerby, HULL_DEPTH);
                    DeckRiderAnchors.set(rider, whale, spot.seat());
                    DeckRiderAnchors.clear(passerby);
                    carried[0] = pushIntoHull(rider, spot.push());
                    outsider[0] = pushIntoHull(passerby, spot.push());
                    ready[0] = true;
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (carried[0] < CRUISE - CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "the whale's own part box clipped the rider it carries: asked %.3f,"
                                        + " moved %.3f", CRUISE, carried[0]));
                        return;
                    }
                    if (outsider[0] > HULL_GAP + CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "the hull let a player it is not carrying through: asked %.3f,"
                                        + " moved %.3f past a %.3f gap", CRUISE, outsider[0], HULL_GAP));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void hullClosesOnARiderThatLeftTheDeck(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        boolean[] ready = {false};
        double[] onDeck = {0.0};
        double[] fallen = {0.0};
        double[] strayed = {0.0};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    Player seated = helper.makeMockPlayer(GameType.SURVIVAL);
                    Player sunk = helper.makeMockPlayer(GameType.SURVIVAL);
                    Player stray = helper.makeMockPlayer(GameType.SURVIVAL);
                    HullApproach spot = hullApproach(helper, whale, owned, seated, HULL_DEPTH);
                    if (spot == null) {
                        return;
                    }
                    DeckRiderAnchors.set(seated, whale, spot.seat());
                    onDeck[0] = pushIntoHull(seated, spot.push());

                    double deck = spot.seat().getBoundingBox().maxY;
                    hullApproach(helper, whale, owned, sunk, HULL_DEPTH);
                    sunk.setPos(sunk.getX(), deck - HULL_FALL, sunk.getZ());
                    DeckRiderAnchors.set(sunk, whale, spot.seat());
                    fallen[0] = pushIntoHull(sunk, spot.push());

                    hullApproach(helper, whale, owned, stray, HULL_DEPTH);
                    DeckRiderAnchors.set(stray, whale, farthestTile(owned, stray));
                    strayed[0] = pushIntoHull(stray, spot.push());
                    ready[0] = true;
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (onDeck[0] < CRUISE - CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "the control rider on the deck was clipped too: asked %.3f, moved %.3f",
                                CRUISE, onDeck[0]));
                        return;
                    }
                    if (fallen[0] > HULL_GAP + CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "a rider %.2f under its own deck still walked through the hull:"
                                        + " asked %.3f, moved %.3f past a %.3f gap",
                                HULL_FALL, CRUISE, fallen[0], HULL_GAP));
                        return;
                    }
                    if (strayed[0] > HULL_GAP + CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "a rider standing off the tile its anchor names still walked through"
                                        + " the hull: asked %.3f, moved %.3f past a %.3f gap",
                                CRUISE, strayed[0], HULL_GAP));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void hullPartsDoNotClipACarriedMob(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        boolean[] ready = {false};
        double[] carried = {0.0};
        double[] loose = {0.0};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    Mob aboard = helper.spawnWithNoFreeWill(EntityType.PIG,
                            new BlockPos(SPAWN_X, FLOOR + 1, SPAWN_Z));
                    Mob outsider = helper.spawnWithNoFreeWill(EntityType.PIG,
                            new BlockPos(SPAWN_X, FLOOR + 1, SPAWN_Z));
                    HullApproach spot = hullApproach(helper, whale, owned, aboard, HULL_DEPTH);
                    if (spot == null) {
                        return;
                    }
                    DeckRiderAnchors.set(aboard, whale, spot.seat());
                    carried[0] = pushIntoHull(aboard, spot.push());

                    hullApproach(helper, whale, owned, outsider, HULL_DEPTH);
                    loose[0] = pushIntoHull(outsider, spot.push());
                    ready[0] = true;
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (carried[0] < CRUISE - CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "the whale's own part box clipped a mob it carries: asked %.3f, moved %.3f",
                                CRUISE, carried[0]));
                        return;
                    }
                    if (loose[0] > HULL_GAP + CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "the hull let through a mob it is not carrying: asked %.3f, moved %.3f"
                                        + " past a %.3f gap", CRUISE, loose[0], HULL_GAP));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void hullPassesOnlyTheWhaleThatCarries(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        HullbackEntity stranger = helper.spawn(WBEntityRegistry.HULLBACK.get(),
                new BlockPos(SPAWN_X + STRANGER_X, FLOOR + 1, SPAWN_Z));
        boolean[] ready = {false};
        double[] mine = {0.0};
        double[] theirs = {0.0};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
                    Player guest = helper.makeMockPlayer(GameType.SURVIVAL);
                    HullApproach spot = hullApproach(helper, whale, owned, rider, HULL_DEPTH);
                    if (spot == null) {
                        return;
                    }
                    hullApproach(helper, whale, owned, guest, HULL_DEPTH);
                    DeckRiderAnchors.set(rider, whale, spot.seat());
                    DeckRiderAnchors.set(guest, stranger, spot.seat());
                    mine[0] = pushIntoHull(rider, spot.push());
                    theirs[0] = pushIntoHull(guest, spot.push());
                    ready[0] = true;
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (mine[0] < CRUISE - CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "the control rider on its own whale was clipped: asked %.3f, moved %.3f",
                                CRUISE, mine[0]));
                        return;
                    }
                    if (theirs[0] > HULL_GAP + CLIP_EPSILON) {
                        helper.fail(String.format(Locale.ROOT,
                                "an anchor on another whale opened this one's hull: asked %.3f,"
                                        + " moved %.3f past a %.3f gap", CRUISE, theirs[0], HULL_GAP));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    private record HullApproach(HullbackWalkableEntity seat, Vec3 push) {
    }

    private static HullApproach hullApproach(final GameTestHelper helper, final HullbackEntity whale,
                                             final List<HullbackWalkableEntity> tiles,
                                             final LivingEntity probe, final double depth) {
        if (tiles.isEmpty()) {
            helper.fail("the whale carries no deck, so nobody is riding it");
            return null;
        }
        HullbackPartEntity head = whale.getSubEntities()[1];
        HullbackPartEntity body = whale.getSubEntities()[2];
        double dx = head.getX() - body.getX();
        double dz = head.getZ() - body.getZ();
        Vec3 push = Math.abs(dx) >= Math.abs(dz)
                ? new Vec3(Math.signum(dx) * CRUISE, 0.0, 0.0)
                : new Vec3(0.0, 0.0, Math.signum(dz) * CRUISE);
        if (push.horizontalDistance() < CLIP_EPSILON) {
            helper.fail("the head and the body share a centre, so there is no seam to walk into");
            return null;
        }
        AABB hull = head.getBoundingBox();
        double clear = 0.5 * probe.getBbWidth() + HULL_GAP;
        double x = push.x > 0.0 ? hull.minX - clear : push.x < 0.0 ? hull.maxX + clear : head.getX();
        double z = push.z > 0.0 ? hull.minZ - clear : push.z < 0.0 ? hull.maxZ + clear : head.getZ();
        probe.setPos(x, hull.maxY - depth, z);
        probe.setDeltaMovement(Vec3.ZERO);
        probe.setOnGround(false);
        HullbackWalkableEntity seat = nearestTileOver(tiles, probe);
        if (seat == null) {
            helper.fail(String.format(Locale.ROOT,
                    "no tile of this whale reaches %.2f,%.2f, so the probe is not on its deck", x, z));
            return null;
        }
        return new HullApproach(seat, push);
    }

    private static HullbackWalkableEntity farthestTile(final List<HullbackWalkableEntity> tiles,
                                                       final Player rider) {
        HullbackWalkableEntity best = null;
        double bestSq = -1.0;
        for (HullbackWalkableEntity tile : tiles) {
            double dx = rider.getX() - tile.getX();
            double dz = rider.getZ() - tile.getZ();
            double sq = dx * dx + dz * dz;
            if (sq > bestSq) {
                bestSq = sq;
                best = tile;
            }
        }
        return best;
    }

    private static double pushIntoHull(final LivingEntity rider, final Vec3 push) {
        Vec3 from = rider.position();
        rider.move(MoverType.SELF, push);
        return rider.position().subtract(from).horizontalDistance();
    }

    private static void sunkDrive(final GameTestHelper helper, final double sinkBy) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        double[] anchor = new double[2];
        double[] worst = {0.0};
        boolean[] ready = {false};
        double[] holdY = {0.0};
        int[] blocked = {0};
        double[] lastSeatX = {Double.NaN};
        double[] deckRan = {0.0};
        double[] riderRan = {0.0};
        double[] overlap = {0.0};
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to stand on");
                        return;
                    }
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX(), top, body.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    rider[0] = player;
                    holdY[0] = whale.getY();
                    ready[0] = true;
                })
                .thenExecuteFor(WARMUP_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    Vec3 warm = whale.getDeltaMovement();
                    whale.setDeltaMovement(warm.x, 0.0, warm.z);
                    whale.setPos(whale.getX() + CRUISE, holdY[0], whale.getZ());
                    whale.carryDeckRiders(List.of(rider[0]));
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    rider[0].setPos(rider[0].getX(), rider[0].getY() - sinkBy, rider[0].getZ());
                    overlap[0] = body.getBoundingBox().maxY - rider[0].getBoundingBox().minY;
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    seat[0] = nearestTileOver(owned, rider[0]);
                    if (seat[0] == null) {
                        helper.fail("the sunk rider is under no tile, the probe would bail for another reason");
                        ready[0] = false;
                        return;
                    }
                    anchor[0] = rider[0].getX() - seat[0].getX();
                    anchor[1] = rider[0].getZ() - seat[0].getZ();
                    lastSeatX[0] = Double.NaN;
                })
                .thenExecuteFor(SUNK_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    Vec3 drift = whale.getDeltaMovement();
                    whale.setDeltaMovement(drift.x, 0.0, drift.z);
                    whale.setPos(whale.getX() + CRUISE, holdY[0], whale.getZ());
                    double tileNow = seat[0].getX();
                    double tileMoved = Double.isNaN(lastSeatX[0]) ? 0.0 : tileNow - lastSeatX[0];
                    lastSeatX[0] = tileNow;
                    double beforeX = rider[0].getX();
                    whale.carryDeckRiders(List.of(rider[0]));
                    if (tileMoved > 0.05 && rider[0].getX() - beforeX < 0.5 * tileMoved) {
                        blocked[0]++;
                        deckRan[0] += tileMoved;
                        riderRan[0] += rider[0].getX() - beforeX;
                    }
                    double dx = (rider[0].getX() - seat[0].getX()) - anchor[0];
                    double dz = (rider[0].getZ() - seat[0].getZ()) - anchor[1];
                    worst[0] = Math.max(worst[0], Math.sqrt(dx * dx + dz * dz));
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (blocked[0] > 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "the hull blocked its own rider on %d of %d ticks while %.3f inside"
                                        + " the part: deck ran %.3f, rider only %.3f, slid %.3f",
                                blocked[0], SUNK_TICKS, overlap[0], deckRan[0], riderRan[0],
                                worst[0]));
                        return;
                    }
                    double left = seat[0].getBoundingBox().maxY - rider[0].getBoundingBox().minY;
                    if (left > FOOTING_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider sunk %.2f was never put back on the deck: still %.3f below its"
                                        + " own tile after %d ticks, slid %.3f",
                                sinkBy, left, SUNK_TICKS, worst[0]));
                        return;
                    }
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider %.3f inside the part slid %.3f over %d ticks at %.2f/tick"
                                        + " (tolerance %.2f)",
                                overlap[0], worst[0], SUNK_TICKS, CRUISE, CARRY_TOLERANCE));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 400)
    public static void riderIsNotDraggedBackWhileHullSinks(final GameTestHelper helper) {
        sinkDrive(helper, SEAM_FRACTION, "on the body/head seam");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 400)
    public static void riderOverBodyIsNotDraggedBackWhileHullSinks(final GameTestHelper helper) {
        sinkDrive(helper, 0.0, "over the body centre");
    }

    private static void sinkDrive(final GameTestHelper helper, final double along, final String where) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        double[] anchor = new double[2];
        boolean[] ready = {false};
        double[] baseY = {0.0};
        int[] tick = {0};
        double[] backDown = {0.0};
        double[] backUp = {0.0};
        double[] spread = {0.0};
        double[] worst = {0.0};
        double[] lastDx = {0.0};
        int[] unsupported = {0};
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to stand on");
                        return;
                    }
                    Vec3 offset = seatOffset(whale, along, 0.0);
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX() + offset.x, top, body.getZ() + offset.z);
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the seat " + where + " is over no tile at all");
                        return;
                    }
                    player.setPos(player.getX(), under, player.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    rider[0] = player;
                    baseY[0] = whale.getY();
                    seat[0] = seatTile(owned, player);
                    anchor[0] = player.getX() - seat[0].getX();
                    anchor[1] = player.getZ() - seat[0].getZ();
                    ready[0] = true;
                })
                .thenExecuteFor(SINK_HALF * 2 * SINK_CYCLES, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    Vec3 drift = whale.getDeltaMovement();
                    whale.setDeltaMovement(drift.x, 0.0, drift.z);
                    boolean sinking = (tick[0] / SINK_HALF) % 2 == 0;
                    double step = sinking ? -SINK_RATE : SINK_RATE;
                    tick[0]++;
                    whale.setPos(whale.getX() + CRUISE, whale.getY() + step, whale.getZ());
                    whale.carryDeckRiders(List.of(rider[0]));

                    spread[0] = Math.max(spread[0],
                            Math.abs(whale.getPartPos(1).y - whale.getPartPos(2).y));
                    if (Double.isNaN(deckTopUnderRider(tiles(helper, whale), rider[0]))) {
                        unsupported[0]++;
                    }
                    double dx = (rider[0].getX() - seat[0].getX()) - anchor[0];
                    double dz = (rider[0].getZ() - seat[0].getZ()) - anchor[1];
                    worst[0] = Math.max(worst[0], Math.sqrt(dx * dx + dz * dz));
                    double gained = dx - lastDx[0];
                    if (gained < 0.0) {
                        if (sinking) {
                            backDown[0] += gained;
                        } else {
                            backUp[0] += gained;
                        }
                    }
                    lastDx[0] = dx;
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    String shape = String.format(Locale.ROOT,
                            "parts %.4f apart, lost %.3f while sinking and %.3f while rising,"
                                    + " unsupported %d of %d",
                            spread[0], backDown[0], backUp[0], unsupported[0],
                            SINK_HALF * 2 * SINK_CYCLES);
                    if (spread[0] < MIN_PART_SPREAD) {
                        helper.fail("head and body never separated vertically: " + shape);
                        return;
                    }
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s was dragged %.3f off its deck spot while the hull"
                                        + " rose and sank at %.2f/tick (tolerance %.2f) | %s",
                                where, worst[0], SINK_RATE, CARRY_TOLERANCE, shape));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 400)
    public static void riderKeepsHullRelativeSpotWhilePitching(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        boolean[] ready = {false};
        double[] startRider = new double[2];
        double[] startPart = new double[DECK_PARTS];
        double[] startTile = new double[DECK_PARTS];
        double[] driftPart = new double[DECK_PARTS];
        double[] driftTile = new double[DECK_PARTS];
        double[] driftRider = {0.0};
        double[] holdY = {0.0};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to stand on");
                        return;
                    }
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX(), top, body.getZ());
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the seat is over no tile at all");
                        return;
                    }
                    player.setPos(player.getX(), under, player.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    helper.getLevel().addFreshEntity(player);
                    rider[0] = player;
                    holdY[0] = whale.getY();
                    startRider[0] = player.getX() - whale.getX();
                    startRider[1] = player.getZ() - whale.getZ();
                    for (int p = 0; p < DECK_PARTS; p++) {
                        startPart[p] = whale.getPartPos(p).x - whale.getX();
                        startTile[p] = tileRelative(owned, whale, p);
                    }
                    ready[0] = true;
                })
                .thenExecuteFor(60, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.setYRot(0.0F);
                    whale.setYBodyRot(0.0F);
                    whale.yRotO = 0.0F;
                    whale.setXRot(20.0F);
                    whale.setPos(whale.getX() + CRUISE, holdY[0], whale.getZ());
                    whale.carryDeckRiders(List.of(rider[0]));

                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    for (int p = 0; p < DECK_PARTS; p++) {
                        driftPart[p] = Math.max(driftPart[p],
                                Math.abs((whale.getPartPos(p).x - whale.getX()) - startPart[p]));
                        double rel = tileRelative(owned, whale, p);
                        if (!Double.isNaN(rel) && !Double.isNaN(startTile[p])) {
                            driftTile[p] = Math.max(driftTile[p], Math.abs(rel - startTile[p]));
                        }
                    }
                    double dx = (rider[0].getX() - whale.getX()) - startRider[0];
                    double dz = (rider[0].getZ() - whale.getZ()) - startRider[1];
                    driftRider[0] = Math.max(driftRider[0], Math.sqrt(dx * dx + dz * dz));
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    String shape = String.format(Locale.ROOT,
                            "part drift nose %.3f head %.3f body %.3f | tile drift nose %.3f head %.3f"
                                    + " body %.3f | pitch %.1f",
                            driftPart[0], driftPart[1], driftPart[2],
                            driftTile[0], driftTile[1], driftTile[2], whale.getXRot());
                    double floor = driftPart[0] + CARRY_TOLERANCE;
                    if (driftRider[0] > floor) {
                        helper.fail(String.format(Locale.ROOT,
                                "the rider travelled %.3f along the hull while it pitched, without ever"
                                        + " moving on the deck (%.3f over the drag-free nose) | %s",
                                driftRider[0], driftRider[0] - driftPart[0], shape));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    private static double tileRelative(final List<HullbackWalkableEntity> tiles,
                                       final HullbackEntity whale, final int part) {
        for (HullbackWalkableEntity tile : tiles) {
            if (tile.getAnchor() == part) {
                return tile.getX() - whale.getX();
            }
        }
        return Double.NaN;
    }


    private static void bobDrive(final GameTestHelper helper, final double along, final double across,
                                 final String where) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        double[] anchor = new double[2];
        double[] worst = {0.0};
        boolean[] ready = {false};
        double[] baseY = {0.0};
        int[] samples = {0};
        double[] partSpread = {0.0};
        double[] floated = {0.0};
        int[] unsupported = {0};
        int[] leftSeat = {0};
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to stand on");
                        return;
                    }
                    Vec3 offset = seatOffset(whale, along, across);
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX() + offset.x, top, body.getZ() + offset.z);
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the seat " + where + " is over no tile at all, nothing to stand on");
                        return;
                    }
                    player.setPos(player.getX(), under, player.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    rider[0] = player;
                    baseY[0] = whale.getY();
                    seat[0] = seatTile(owned, player);
                    anchor[0] = player.getX() - seat[0].getX();
                    anchor[1] = player.getZ() - seat[0].getZ();
                    ready[0] = true;
                })
                .thenExecuteFor(BOB_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    Vec3 drift = whale.getDeltaMovement();
                    whale.setDeltaMovement(drift.x, 0.0, drift.z);
                    double lift = Math.sin(samples[0] * BOB_RATE) * BOB;
                    samples[0]++;
                    whale.setPos(whale.getX() + FAST, baseY[0] + lift, whale.getZ());
                    whale.carryDeckRiders(List.of(rider[0]));

                    partSpread[0] = Math.max(partSpread[0],
                            Math.abs(whale.getPartPos(1).y - whale.getPartPos(2).y));
                    double under = deckTopUnderRider(tiles(helper, whale), rider[0]);
                    if (Double.isNaN(under)) {
                        unsupported[0]++;
                    } else {
                        floated[0] = Math.max(floated[0], Math.abs(rider[0].getY() - under));
                    }
                    if (!covers(seat[0], rider[0])) {
                        leftSeat[0]++;
                    }
                    double dx = (rider[0].getX() - seat[0].getX()) - anchor[0];
                    double dz = (rider[0].getZ() - seat[0].getZ()) - anchor[1];
                    worst[0] = Math.max(worst[0], Math.sqrt(dx * dx + dz * dz));
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (partSpread[0] < MIN_PART_SPREAD) {
                        helper.fail(String.format(Locale.ROOT,
                                "head and body never separated vertically (%.4f): this drive does not"
                                        + " reproduce the defect and the result below means nothing",
                                partSpread[0]));
                        return;
                    }
                    if (unsupported[0] > 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s had no tile under it on %d of %d ticks: it left the deck"
                                        + " instead of being carried, parts %.4f apart",
                                where, unsupported[0], BOB_TICKS, partSpread[0]));
                        return;
                    }
                    if (leftSeat[0] > 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s walked off the tile it started on within %d of %d ticks"
                                        + " (parts %.4f apart): the deck slid out from under it",
                                where, leftSeat[0], BOB_TICKS, partSpread[0]));
                        return;
                    }
                    if (floated[0] > FOOTING_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s floated %.4f off the top of the tile actually under it"
                                        + " while the hull rose and fell (parts %.4f apart): the support"
                                        + " probe picked a surface the rider is not standing on",
                                where, floated[0], partSpread[0]));
                        return;
                    }
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider seated %s slid %.3f while the hull rose and fell over %d ticks at"
                                        + " %.2f/tick (tolerance %.2f), parts %.4f apart, feet %.3f",
                                where, worst[0], BOB_TICKS, FAST, CARRY_TOLERANCE, partSpread[0],
                                rider[0].getY()));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 400)
    public static void riderHoldsDeckSpotAtNoseHeadSeam(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        double[] anchor = new double[2];
        double[] worst = {0.0};
        boolean[] ready = {false};
        double[] baseY = {0.0};
        double[] seamSpread = {0.0};
        double[] floated = {0.0};
        int[] unsupported = {0};
        int[] leftSeat = {0};
        int[] covering = {0};
        int[] samples = {0};
        int[] flips = {0};
        boolean[] wasOccupied = {false};
        double[] headStep = {0.0};
        double[] neighbour = {0.0};
        double[] lastHeadY = {Double.NaN};
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    Vec3 nose = whale.getPartPos(0);
                    Vec3 head = whale.getPartPos(1);
                    double x = (nose.x + head.x) * 0.5;
                    double z = (nose.z + head.z) * 0.5;
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(x, Math.max(nose.y, head.y) + SEAM_PROBE_LIFT, z);
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the nose/head midpoint is over no tile at all, nothing to stand on");
                        return;
                    }
                    player.setPos(x, under, z);
                    player.setDeltaMovement(Vec3.ZERO);
                    for (HullbackWalkableEntity tile : owned) {
                        if (covers(tile, player)) {
                            covering[0]++;
                        }
                    }
                    if (covering[0] < 2) {
                        helper.fail(String.format(Locale.ROOT,
                                "the nose/head midpoint is covered by %d tile(s): this drive never reaches"
                                        + " the seam and the result below would mean nothing", covering[0]));
                        return;
                    }
                    helper.getLevel().addFreshEntity(player);
                    rider[0] = player;
                    baseY[0] = whale.getY();
                    seat[0] = seatTile(owned, player);
                    Vec3 seated = DeckRiderAnchors.toDeckLocal(
                            player.position().subtract(seat[0].position()), whale.getYRot());
                    anchor[0] = seated.x;
                    anchor[1] = seated.z;
                    ready[0] = true;
                })
                .thenExecuteFor(SEAM_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 0;
                    whale.setDeltaMovement(CRUISE, whale.getDeltaMovement().y, 0.0);
                    samples[0]++;
                    whale.carryDeckRiders(List.of(rider[0]));

                    seamSpread[0] = Math.max(seamSpread[0],
                            Math.abs(whale.getPartPos(0).y - whale.getPartPos(1).y));
                    double headY = whale.getPartPos(1).y;
                    if (!Double.isNaN(lastHeadY[0])) {
                        headStep[0] = Math.max(headStep[0], Math.abs(headY - lastHeadY[0]));
                    }
                    lastHeadY[0] = headY;
                    if (whale.isDeckOccupied() != wasOccupied[0]) {
                        flips[0]++;
                        wasOccupied[0] = whale.isDeckOccupied();
                    }
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    double miss = deckFootingMiss(owned, rider[0]);
                    if (Double.isNaN(miss)) {
                        unsupported[0]++;
                    } else {
                        floated[0] = Math.max(floated[0], miss);
                        double top = deckTopUnderRider(owned, rider[0]);
                        neighbour[0] = Math.max(neighbour[0], Math.abs(top - rider[0].getY()));
                    }
                    if (!covers(seat[0], rider[0])) {
                        leftSeat[0]++;
                    }
                    Vec3 spot = DeckRiderAnchors.toDeckLocal(
                            rider[0].position().subtract(seat[0].position()), whale.getYRot());
                    double dx = spot.x - anchor[0];
                    double dz = spot.z - anchor[1];
                    worst[0] = Math.max(worst[0], Math.sqrt(dx * dx + dz * dz));
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    String detail = String.format(Locale.ROOT,
                            " | seam %.4f, %d tiles cover the seat, unsupported %d/%d, left seat %d,"
                                    + " floated %.4f, slid %.3f, feet %.3f, occupied %s, swim %.4f,"
                                    + " occupancy flips %d, biggest one-tick head step %.4f,"
                                    + " neighbouring deck up to %.4f away",
                            seamSpread[0], covering[0], unsupported[0], SEAM_TICKS, leftSeat[0],
                            floated[0], worst[0], rider[0].getY(), whale.isDeckOccupied(),
                            whale.getAnimationSwimSpeed(), flips[0], headStep[0], neighbour[0]);
                    if (seamSpread[0] < MIN_PART_SPREAD) {
                        helper.fail("nose and head never separated vertically: this drive does not"
                                + " reproduce the seam and the result means nothing" + detail);
                        return;
                    }
                    if (unsupported[0] > 0) {
                        helper.fail("rider on the nose/head seam had no tile under it: it left the"
                                + " deck instead of being carried" + detail);
                        return;
                    }
                    if (leftSeat[0] > 0) {
                        helper.fail("rider on the nose/head seam walked off the tile it started on:"
                                + " the deck slid out from under it" + detail);
                        return;
                    }
                    if (floated[0] > SEAM_FOOTING_TOLERANCE) {
                        helper.fail("rider on the nose/head seam ended more than one tick of lift off"
                                + " every tile that covers it: the carry lost the deck" + detail);
                        return;
                    }
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail("rider on the nose/head seam slid past the tolerance " + CARRY_TOLERANCE
                                + detail);
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderJumpsAsHighOnDeckAsOnGround(final GameTestHelper helper) {
        jumpDrive(helper, 0.0, 0.0, 0.0, 0.0, "straight up from a still whale");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderKeepsSeamSpotAtFullSpeedWhileSinking(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];
        double[] anchor = new double[2];
        double[] worst = {0.0};
        double[] hold = new double[3];
        int[] tick = {0};
        int[] unsupported = {0};
        boolean[] ready = {false};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to stand on");
                        return;
                    }
                    Vec3 offset = seatOffset(whale, SEAM_FRACTION, 0.0);
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX() + offset.x, top, body.getZ() + offset.z);
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the seam seat is over no tile at all");
                        return;
                    }
                    player.setPos(player.getX(), under, player.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    helper.getLevel().addFreshEntity(player);
                    rider[0] = player;
                    seat[0] = seatTile(owned, player);
                    if (seat[0] == null) {
                        helper.fail("the rider is under no tile, the probe would bail for another reason");
                        return;
                    }
                    Vec3 local = DeckRiderAnchors.toDeckLocal(
                            player.position().subtract(seat[0].position()), whale.getYRot());
                    anchor[0] = local.x;
                    anchor[1] = local.z;
                    hold[0] = whale.getX();
                    hold[1] = whale.getY();
                    hold[2] = whale.getZ();
                    ready[0] = true;
                })
                .thenExecuteFor(FAST_TICKS, () -> {
                    if (!ready[0] || seat[0].isRemoved()) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    whale.setYRot(0.0F);
                    whale.yRotO = 0.0F;
                    whale.setYBodyRot(0.0F);
                    Vec3 drift = whale.getDeltaMovement();
                    whale.setDeltaMovement(drift.x, 0.0, drift.z);
                    int at = tick[0]++;
                    whale.setPos(hold[0] + FAST_CRUISE * (at + 1), hold[1] - FAST_SINK * (at + 1), hold[2]);
                    double stride = (at / 8) % 2 == 0 ? FAST_STRIDE : -FAST_STRIDE;
                    rider[0].move(MoverType.SELF, new Vec3(0.0, 0.0, stride));
                    whale.carryDeckRiders(List.of(rider[0]));
                    if (Double.isNaN(deckTopUnderRider(tiles(helper, whale), rider[0]))) {
                        unsupported[0]++;
                        return;
                    }
                    Vec3 local = DeckRiderAnchors.toDeckLocal(
                            rider[0].position().subtract(seat[0].position()), whale.getYRot());
                    double dx = local.x - anchor[0];
                    worst[0] = Math.max(worst[0], Math.abs(dx));
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    String shape = String.format(Locale.ROOT,
                            "deck ran %.2f/tick sinking %.2f/tick for %d ticks, drift %.3f, unsupported %d",
                            FAST_CRUISE, FAST_SINK, FAST_TICKS, worst[0], unsupported[0]);
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail("the rider lost its seam spot at full speed: " + shape);
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    private static Mob mobOnDeck(final GameTestHelper helper, final HullbackEntity whale,
                                 final List<HullbackWalkableEntity> owned) {
        HullbackPartEntity body = whale.getSubEntities()[2];
        double top = deckTopOver(owned, body);
        if (Double.isNaN(top)) {
            helper.fail("no deck over the body part, nothing to stand on");
            return null;
        }
        Mob mob = helper.spawnWithNoFreeWill(EntityType.PIG,
                new BlockPos(SPAWN_X, FLOOR + 1, SPAWN_Z));
        mob.setPos(body.getX(), top, body.getZ());
        double under = deckTopUnderRider(owned, mob);
        if (Double.isNaN(under)) {
            helper.fail("the mob seat is over no tile at all");
            return null;
        }
        mob.setPos(mob.getX(), under, mob.getZ());
        mob.setDeltaMovement(Vec3.ZERO);
        return mob;
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void deckSurvivesAMobStandingAlone(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Mob[] aboard = new Mob[1];
        int[] gone = {0};
        int[] idle = {0};
        boolean[] ready = {false};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    aboard[0] = mobOnDeck(helper, whale, tiles(helper, whale));
                    ready[0] = aboard[0] != null;
                })
                .thenExecuteFor(IDLE_WATCH_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 0;
                    if (whale.getStationaryTicks() == 0) {
                        idle[0]++;
                    }
                    if (tiles(helper, whale).isEmpty()) {
                        gone[0]++;
                    }
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (idle[0] < DECK_EMPTY_DISCARD_WATCH) {
                        helper.fail(String.format(Locale.ROOT,
                                "the hull was only idle on %d of %d ticks, so the discard guard was"
                                        + " never reached and this test proves nothing",
                                idle[0], IDLE_WATCH_TICKS));
                        return;
                    }
                    if (gone[0] > 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "the deck was thrown away under a mob standing on it, on %d of %d ticks",
                                gone[0], IDLE_WATCH_TICKS));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void aMobOnTheDeckDoesNotStopTheHull(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Mob[] aboard = new Mob[1];
        int[] stalled = {0};
        int[] seen = {0};
        int[] wet = {0};
        int[] firstAt = {-1};
        boolean[] ready = {false};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dy = 0; dy <= 4; dy++) {
                            for (int dz = -3; dz <= 3; dz++) {
                                helper.setBlock(new BlockPos(SPAWN_X + dx, FLOOR + 1 + dy, SPAWN_Z + dz),
                                        Blocks.WATER);
                            }
                        }
                    }
                    whale.stationaryTicks = 0;
                    aboard[0] = mobOnDeck(helper, whale, tiles(helper, whale));
                    ready[0] = aboard[0] != null;
                })
                .thenIdle(1)
                .thenExecuteFor(IDLE_WATCH_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    int at = seen[0]++;
                    if (whale.isInWater()) {
                        wet[0]++;
                    }
                    if (whale.getStationaryTicks() > 0) {
                        stalled[0]++;
                        if (firstAt[0] < 0) {
                            firstAt[0] = at;
                        }
                    }
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (wet[0] < IDLE_WATCH_TICKS / 2) {
                        helper.fail(String.format(Locale.ROOT,
                                "the hull was in water on only %d of %d ticks, and the pause branch"
                                        + " needs water, so this test proves nothing",
                                wet[0], IDLE_WATCH_TICKS));
                        return;
                    }
                    if (stalled[0] > 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "a mob on the deck froze the hull on %d of %d ticks, first at tick %d,"
                                        + " which a shoal of fish would do every time it brushed past",
                                stalled[0], IDLE_WATCH_TICKS, firstAt[0]));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 400)
    public static void deckReturnsAfterItWasDiscarded(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Mob[] aboard = new Mob[1];
        int[] cleared = {-1};
        boolean[] ready = {false};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecuteFor(DECK_DISCARD_WAIT, () -> whale.stationaryTicks = 0)
                .thenExecute(() -> {
                    cleared[0] = tiles(helper, whale).size();
                    if (cleared[0] != 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "the deck was never discarded (%d tiles left), so the state a world"
                                        + " reload leaves behind was never reproduced", cleared[0]));
                        return;
                    }
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    Mob mob = helper.spawnWithNoFreeWill(EntityType.PIG,
                            new BlockPos(SPAWN_X, FLOOR + 1, SPAWN_Z));
                    mob.setPos(body.getX(), body.getBoundingBox().maxY + 0.1, body.getZ());
                    mob.setDeltaMovement(Vec3.ZERO);
                    aboard[0] = mob;
                    ready[0] = true;
                })
                .thenExecuteFor(IDLE_WATCH_TICKS, () -> {
                    if (ready[0]) {
                        whale.stationaryTicks = 0;
                    }
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    if (aboard[0] == null || !aboard[0].isAlive()) {
                        helper.fail("nobody was aboard, so this test proves nothing");
                        return;
                    }
                    if (whale.getStationaryTicks() != 0) {
                        helper.fail(String.format(Locale.ROOT,
                                "the hull settled to %d stationary ticks, so stopMoving could have"
                                        + " spawned the deck and this test proves nothing",
                                whale.getStationaryTicks()));
                        return;
                    }
                    if (tiles(helper, whale).isEmpty()) {
                        helper.fail(String.format(Locale.ROOT,
                                "the deck never came back with someone aboard and the hull pinned"
                                        + " moving, after %d ticks", IDLE_WATCH_TICKS));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderTurnsWithTheHullItStandsOn(final GameTestHelper helper) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];
        double[] anchor = new double[2];
        double[] last = new double[2];
        double[] worst = {0.0};
        double[] step = {0.0};
        double[] hold = new double[3];
        float[] spun = {0.0F};
        boolean[] ready = {false};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity nose = whale.getSubEntities()[0];
                    double top = deckTopOver(owned, nose);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the nose, nothing to stand on");
                        return;
                    }
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(nose.getX() + TURN_ARM, top, nose.getZ());
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the seat off the tile centre is over no tile at all");
                        return;
                    }
                    player.setPos(player.getX(), under, player.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    helper.getLevel().addFreshEntity(player);
                    rider[0] = player;
                    seat[0] = seatTile(owned, player);
                    if (seat[0] == null) {
                        helper.fail("the rider is under no tile, the probe would bail for another reason");
                        return;
                    }
                    Vec3 local = DeckRiderAnchors.toDeckLocal(
                            player.position().subtract(seat[0].position()), whale.getYRot());
                    anchor[0] = local.x;
                    anchor[1] = local.z;
                    last[0] = local.x;
                    last[1] = local.z;
                    hold[0] = whale.getX();
                    hold[1] = whale.getY();
                    hold[2] = whale.getZ();
                    ready[0] = true;
                })
                .thenExecuteFor(TURN_TICKS, () -> {
                    if (!ready[0] || seat[0].isRemoved()) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    whale.setDeltaMovement(Vec3.ZERO);
                    whale.setPos(hold[0], hold[1], hold[2]);
                    whale.yRotO = whale.getYRot();
                    whale.setYRot(whale.getYRot() + TURN_RATE);
                    whale.setYBodyRot(whale.getYRot());
                    spun[0] += TURN_RATE;
                    whale.carryDeckRiders(List.of(rider[0]));
                    Vec3 local = DeckRiderAnchors.toDeckLocal(
                            rider[0].position().subtract(seat[0].position()), whale.getYRot());
                    double dx = local.x - anchor[0];
                    double dz = local.z - anchor[1];
                    worst[0] = Math.max(worst[0], Math.sqrt(dx * dx + dz * dz));
                    step[0] = Math.max(step[0], Math.hypot(local.x - last[0], local.z - last[1]));
                    last[0] = local.x;
                    last[1] = local.z;
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    String shape = String.format(Locale.ROOT,
                            "arm %.2f, hull turned %.1f over %d ticks, drift %.3f, biggest one-tick %.4f",
                            TURN_ARM, spun[0], TURN_TICKS, worst[0], step[0]);
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail("the rider did not turn with the deck it stands on: " + shape);
                        return;
                    }
                    if (step[0] > TURN_STEP) {
                        helper.fail("the rider turned, but in jumps: " + shape);
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderJumpsAsHighFromARisingDeck(final GameTestHelper helper) {
        jumpDrive(helper, 0.0, RISE_RATE, 0.0, 0.0, 0.0, "straight up from a rising deck");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderJumpsAsHighFromASinkingDeck(final GameTestHelper helper) {
        jumpDrive(helper, 0.0, -RISE_RATE, 0.0, 0.0, 0.0, "straight up from a sinking deck");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderJumpsInPlaceOnMovingDeck(final GameTestHelper helper) {
        jumpDrive(helper, CRUISE, 0.0, 0.0, 0.0, "straight up from a moving whale");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderJumpsAlongMovingDeckLikeOnGround(final GameTestHelper helper) {
        jumpDrive(helper, CRUISE, HOP, 0.0, 0.0, "with the hull");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderJumpsAgainstMovingDeckLikeOnGround(final GameTestHelper helper) {
        jumpDrive(helper, CRUISE, -HOP, 0.0, 0.0, "against the hull");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderJumpsAcrossMovingDeckLikeOnGround(final GameTestHelper helper) {
        jumpDrive(helper, CRUISE, HOP * DIAGONAL, HOP * DIAGONAL, 0.0, "at 45 degrees to the hull");
    }

    @GameTest(template = "hullback_deck", timeoutTicks = 300)
    public static void riderJumpsFromDeckSeamLikeOnGround(final GameTestHelper helper) {
        jumpDrive(helper, CRUISE, HOP, 0.0, SEAM_FRACTION, "from the body/head seam");
    }

    private static void jumpDrive(final GameTestHelper helper, final double speed,
                                  final double hopX, final double hopZ, final double along,
                                  final String where) {
        jumpDrive(helper, speed, 0.0, hopX, hopZ, along, where);
    }

    private static void jumpDrive(final GameTestHelper helper, final double speed, final double rise,
                                  final double hopX, final double hopZ, final double along,
                                  final String where) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        Player[] control = new Player[1];
        boolean[] ready = {false};
        double[] holdY = {0.0};
        double[] groundY = {0.0};
        double[] deckApex = {0.0};
        double[] groundApex = {0.0};
        double[] launch = new double[2];
        double[] started = new double[2];
        double[] deckHop = new double[2];
        double[] groundHop = new double[2];
        int[] tick = {0};
        int[] unsupported = {0};
        HullbackWalkableEntity[] seat = new HullbackWalkableEntity[1];

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to jump from");
                        return;
                    }
                    Vec3 offset = seatOffset(whale, along, 0.0);
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX() + offset.x, top, body.getZ() + offset.z);
                    double under = deckTopUnderRider(owned, player);
                    if (Double.isNaN(under)) {
                        helper.fail("the seat " + where + " is over no tile at all");
                        return;
                    }
                    player.setPos(player.getX(), under, player.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    helper.getLevel().addFreshEntity(player);
                    rider[0] = player;

                    BlockPos spot = helper.absolutePos(
                            new BlockPos(SPAWN_X, FLOOR + 1, SPAWN_Z + CONTROL_Z));
                    Player onFloor = helper.makeMockPlayer(GameType.SURVIVAL);
                    onFloor.setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                    onFloor.setDeltaMovement(Vec3.ZERO);
                    helper.getLevel().addFreshEntity(onFloor);
                    control[0] = onFloor;
                    groundY[0] = spot.getY();
                    holdY[0] = whale.getY();
                    ready[0] = true;
                })
                .thenExecuteFor(JUMP_WARMUP + JUMP_WINDOW, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    whale.setYRot(0.0F);
                    whale.setYBodyRot(0.0F);
                    whale.yRotO = 0.0F;
                    whale.setXRot(0.0F);
                    whale.xRotO = 0.0F;
                    Vec3 drift = whale.getDeltaMovement();
                    whale.setDeltaMovement(drift.x, 0.0, drift.z);
                    whale.setPos(whale.getX() + speed, holdY[0] + rise * tick[0], whale.getZ());
                    whale.carryDeckRiders(List.of(rider[0]));

                    int at = tick[0]++;
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    double under = deckTopUnderRider(owned, rider[0]);
                    if (Double.isNaN(under)) {
                        unsupported[0]++;
                    } else if (at >= JUMP_WARMUP) {
                        deckApex[0] = Math.max(deckApex[0], rider[0].getBoundingBox().minY - under);
                    }
                    if (at >= JUMP_WARMUP) {
                        groundApex[0] = Math.max(groundApex[0],
                                control[0].getBoundingBox().minY - groundY[0]);
                    }
                    if (at == JUMP_WARMUP - 1) {
                        seat[0] = seatTile(owned, rider[0]);
                        launch[0] = rider[0].getX() - seat[0].getX();
                        launch[1] = rider[0].getZ() - seat[0].getZ();
                        started[0] = control[0].getX();
                        started[1] = control[0].getZ();
                        leap(rider[0], hopX, hopZ);
                        leap(control[0], hopX, hopZ);
                    }
                    if (at == JUMP_WARMUP) {
                        rider[0].setJumping(false);
                        control[0].setJumping(false);
                    }
                    if (seat[0] != null) {
                        deckHop[0] = (rider[0].getX() - seat[0].getX()) - launch[0];
                        deckHop[1] = (rider[0].getZ() - seat[0].getZ()) - launch[1];
                        groundHop[0] = control[0].getX() - started[0];
                        groundHop[1] = control[0].getZ() - started[1];
                    }
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    String shape = String.format(Locale.ROOT,
                            "apex deck %.3f floor %.3f | hop deck %.3f,%.3f floor %.3f,%.3f"
                                    + " | unsupported %d of %d, hull at %.2f/tick, feet %.3f",
                            deckApex[0], groundApex[0], deckHop[0], deckHop[1],
                            groundHop[0], groundHop[1], unsupported[0],
                            JUMP_WARMUP + JUMP_WINDOW, speed, rider[0].getY());
                    if (groundApex[0] < MIN_APEX) {
                        helper.fail("the control on the floor never left it, so nothing measured on"
                                + " the deck means anything | " + shape);
                        return;
                    }
                    if (unsupported[0] > 0) {
                        helper.fail("the rider was off the deck footprint on some tick, so the two"
                                + " flights are not comparable | " + shape);
                        return;
                    }
                    if (Math.abs(deckApex[0] - groundApex[0]) > APEX_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "a jump %s rose %.3f over the deck against %.3f over the floor"
                                        + " (tolerance %.2f) | %s",
                                where, deckApex[0], groundApex[0], APEX_TOLERANCE, shape));
                        return;
                    }
                    double missX = deckHop[0] - groundHop[0];
                    double missZ = deckHop[1] - groundHop[1];
                    double miss = Math.sqrt(missX * missX + missZ * missZ);
                    if (miss > HOP_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "a jump %s landed %.3f from where the same jump lands on the floor"
                                        + " (tolerance %.2f) | %s",
                                where, miss, HOP_TOLERANCE, shape));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }

    private static void leap(final Player player, final double hopX, final double hopZ) {
        player.setDeltaMovement(hopX, player.getDeltaMovement().y, hopZ);
        player.setJumping(true);
    }

    private static Vec3 seatOffset(final HullbackEntity whale, final double along, final double across) {
        Vec3 body = whale.getPartPos(2);
        Vec3 head = whale.getPartPos(1);
        Vec3 axis = new Vec3(head.x - body.x, 0.0, head.z - body.z);
        double span = axis.horizontalDistance();
        Vec3 unit = span < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : axis.scale(1.0 / span);
        return unit.scale(along * span).add(new Vec3(-unit.z, 0.0, unit.x).scale(across));
    }

    private static void carryDrive(final GameTestHelper helper, final double speed) {
        floor(helper);
        HullbackEntity whale = whale(helper);
        Player[] rider = new Player[1];
        double[] anchor = new double[2];
        double[] worst = {0.0};
        boolean[] ready = {false};
        double[] applied = {0.0};
        int[] carried = {0};
        double[] partDelta = {0.0};
        double[] requested = {0.0};
        double[] holdY = {0.0};
        double[] clipped = {0.0};
        int[] clippedTicks = {0};
        java.util.Map<String, Integer> blockers = new java.util.TreeMap<>();
        int[] blockedByTerrain = {0};
        java.util.List<String> clipTrace = new java.util.ArrayList<>();
        int[] tick = {0};

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    List<HullbackWalkableEntity> owned = tiles(helper, whale);
                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double top = deckTopOver(owned, body);
                    if (Double.isNaN(top)) {
                        helper.fail("no deck over the body part, nothing to stand on");
                        return;
                    }
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    player.setPos(body.getX(), top, body.getZ());
                    player.setDeltaMovement(Vec3.ZERO);
                    rider[0] = player;
                    holdY[0] = whale.getY();
                    anchor[0] = player.getX() - body.getX();
                    anchor[1] = player.getZ() - body.getZ();
                    ready[0] = true;
                })
                .thenExecuteFor(DRIVE_TICKS, () -> {
                    if (!ready[0]) {
                        return;
                    }
                    whale.stationaryTicks = 200;
                    whale.setPos(whale.getX() + speed, holdY[0], whale.getZ());
                    HullbackPartEntity part = whale.getSubEntities()[2];
                    partDelta[0] = Math.max(partDelta[0],
                            whale.getPartPos(2).subtract(whale.getOldPartPos(2)).horizontalDistance());
                    Vec3 want = whale.deckMotionAt(rider[0]);
                    requested[0] += want.horizontalDistance();
                    double beforeX = rider[0].getX();
                    double beforeZ = rider[0].getZ();
                    AABB swept = rider[0].getBoundingBox().expandTowards(want.x, want.y, want.z);
                    whale.carryDeckRiders(List.of(rider[0]));
                    double shifted = Math.hypot(rider[0].getX() - beforeX, rider[0].getZ() - beforeZ);
                    if (shifted > 1.0E-6) {
                        carried[0]++;
                        applied[0] += shifted;
                    }
                    tick[0]++;
                    double lost = want.horizontalDistance() - shifted;
                    if (lost > CLIP_EPSILON) {
                        clipTrace.add(String.format(Locale.ROOT,
                                "t%d want=(%.3f,%.3f,%.3f) got=%.3f x=%.1f feet=%.3f",
                                tick[0], want.x, want.y, want.z, shifted,
                                rider[0].getX(), rider[0].getY()));
                        clipped[0] += lost;
                        clippedTicks[0]++;
                        for (Entity blocker : rider[0].level().getEntities(rider[0], swept,
                                Entity::canBeCollidedWith)) {
                            blockers.merge(blocker.getClass().getSimpleName(), 1, Integer::sum);
                        }
                        if (rider[0].level().getBlockCollisions(rider[0], swept).iterator().hasNext()) {
                            blockedByTerrain[0]++;
                        }
                    }

                    HullbackPartEntity body = whale.getSubEntities()[2];
                    double dx = (rider[0].getX() - body.getX()) - anchor[0];
                    double dz = (rider[0].getZ() - body.getZ()) - anchor[1];
                    worst[0] = Math.max(worst[0], Math.sqrt(dx * dx + dz * dz));
                })
                .thenExecute(() -> {
                    if (!ready[0]) {
                        return;
                    }
                    double travelled = speed * DRIVE_TICKS;
                    if (worst[0] > CARRY_TOLERANCE) {
                        helper.fail(String.format(Locale.ROOT,
                                "rider slid %.3f of %.1f blocks at %.2f/tick (tolerance %.2f)"
                                        + " | carried on %d/%d ticks, requested %.3f applied %.3f,"
                                        + " biggest part delta %.4f, tiles %d, feet %.3f, ground %s"
                                        + " | clipped %.3f over %d ticks by %s, terrain on %d %s",
                                worst[0], travelled, speed, CARRY_TOLERANCE,
                                carried[0], DRIVE_TICKS, requested[0], applied[0], partDelta[0],
                                tiles(helper, whale).size(), rider[0].getY(), rider[0].onGround(),
                                clipped[0], clippedTicks[0], blockers, blockedByTerrain[0],
                                clipTrace));
                        return;
                    }
                    helper.succeed();
                })
                .thenSucceed();
    }
}
