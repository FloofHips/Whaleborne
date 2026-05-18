package com.fruityspikes.whaleborne.server.entities.components.hullback;

import com.fruityspikes.whaleborne.server.entities.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seat assignments on the Hullback. Seats 0-6 use individual EntityDataAccessors;
 * seats 7+ pack into one CompoundTag accessor (empty seats cost nothing), capped at MAX_TOTAL_SEATS.
 */
public class HullbackSeatManager {
    private static final int BASE_SEAT_COUNT = 7; // seats 0-6 with individual accessors

    private final HullbackEntity whale;
    private final EntityDataAccessor<Optional<UUID>>[] baseAccessors; // seats 0-6
    public final Vec3[] seats;
    public final Vec3[] oldSeats;
    private int activeSeatCount;
    private SeatLayout seatLayout;

    @SuppressWarnings("unchecked")
    public HullbackSeatManager(HullbackEntity whale, EntityDataAccessor<Optional<UUID>>... accessors) {
        this.whale = whale;
        this.baseAccessors = accessors; // exactly 7
        this.seats = new Vec3[HullbackEntity.MAX_TOTAL_SEATS];
        this.oldSeats = new Vec3[HullbackEntity.MAX_TOTAL_SEATS];
        this.seatLayout = SeatLayout.defaultLayout();
        this.activeSeatCount = seatLayout.getActiveSeatCount();
    }

    public void tick() {
        validateAssignments();
    }

    public void updateSeats(Vec3[] newSeats, Vec3[] newOldSeats) {
        System.arraycopy(newSeats, 0, this.seats, 0, Math.min(newSeats.length, this.seats.length));
        System.arraycopy(newOldSeats, 0, this.oldSeats, 0, Math.min(newOldSeats.length, this.oldSeats.length));
    }

    public int getSeatCount() { return HullbackEntity.MAX_TOTAL_SEATS; }
    public int getActiveSeatCount() { return activeSeatCount; }
    public SeatLayout getSeatLayout() { return seatLayout; }

    public void setSeatLayout(SeatLayout layout) {
        this.seatLayout = layout;
        this.activeSeatCount = Math.min(layout.getActiveSeatCount(), HullbackEntity.MAX_TOTAL_SEATS);
    }

    // ─── Seat Data Access (hybrid: accessor for 0-6, CompoundTag for 7+) ───

    public Optional<UUID> getSeatData(int index) {
        if (index < 0 || index >= activeSeatCount) return Optional.empty();

        if (index < BASE_SEAT_COUNT) {
            // Base seats: individual EntityDataAccessor
            return whale.getEntityData().get(baseAccessors[index]);
        } else {
            // Overflow seats: CompoundTag
            CompoundTag tag = whale.getEntityData().get(HullbackEntity.DATA_OVERFLOW_SEATS);
            String key = String.valueOf(index);
            if (tag.contains(key)) {
                try {
                    return Optional.of(UUID.fromString(tag.getString(key)));
                } catch (IllegalArgumentException e) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
    }

    public void setSeatData(int index, Optional<UUID> uuid) {
        if (index < 0 || index >= HullbackEntity.MAX_TOTAL_SEATS) return;

        if (index < BASE_SEAT_COUNT) {
            // Base seats: individual EntityDataAccessor
            whale.getEntityData().set(baseAccessors[index], uuid);
        } else {
            // Overflow seats: read-modify-write the CompoundTag
            CompoundTag tag = whale.getEntityData().get(HullbackEntity.DATA_OVERFLOW_SEATS).copy();
            String key = String.valueOf(index);
            if (uuid.isPresent()) {
                tag.putString(key, uuid.get().toString());
            } else {
                tag.remove(key);
            }
            whale.getEntityData().set(HullbackEntity.DATA_OVERFLOW_SEATS, tag);
        }
    }

    /** Base accessor for seats 0-6 only; for overflow seats use getSeatData/setSeatData. */
    public EntityDataAccessor<Optional<UUID>> getSeatAccessor(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= BASE_SEAT_COUNT) {
            return baseAccessors[BASE_SEAT_COUNT - 1]; // fallback to last base seat
        }
        return baseAccessors[seatIndex];
    }

    // ─── Validation (runs every tick on server) ────────────────────────

    public void validateAssignments() {
        if (whale.level().isClientSide) return;

        Set<UUID> currentPassengerUUIDs = whale.getPassengers().stream()
                .map(Entity::getUUID)
                .collect(Collectors.toSet());

        for (int seatIndex = 0; seatIndex < activeSeatCount; seatIndex++) {
            Optional<UUID> assignedUUID = getSeatData(seatIndex);

            if (assignedUUID.isPresent()) {
                UUID uuid = assignedUUID.get();

                if (!currentPassengerUUIDs.contains(uuid)) {
                    boolean shouldClear = true;

                    // "Smart" Check & Active Recovery
                    if (whale.level() instanceof ServerLevel serverLevel) {
                        Entity passengerEntity = serverLevel.getEntity(uuid);

                        if (passengerEntity == null) {
                            // Entity still loading or dead. Wait up to 60s.
                            if (whale.tickCount < 1200) {
                                shouldClear = false;
                            }
                        } else {
                            // Entity found but detached. RECOVERY: teleport + force mount
                            if (passengerEntity.isAlive()) {
                                Vec3 seatPos = seatIndex < seats.length ? seats[seatIndex] : null;
                                if (seatPos != null) {
                                    passengerEntity.teleportTo(seatPos.x, seatPos.y, seatPos.z);
                                }
                                passengerEntity.startRiding(whale, true);
                                shouldClear = false;
                            }
                        }
                    }

                    if (shouldClear) {
                        setSeatData(seatIndex, Optional.empty());
                    }
                } else {
                    Entity passenger = whale.getEntityByUUID(uuid);
                    if (passenger != null && getSeatByEntity(passenger) != seatIndex) {
                        setSeatData(seatIndex, Optional.empty());
                    }
                }
            }
        }
    }

    // ─── Assignment ────────────────────────────────────────────────────

    public void assignSeat(int seatIndex, @Nullable Entity passenger) {
        if (seatIndex < 0 || seatIndex >= HullbackEntity.MAX_TOTAL_SEATS) return;

        if (passenger == null) {
            setSeatData(seatIndex, Optional.empty());
        } else {
            setSeatData(seatIndex, Optional.of(passenger.getUUID()));
            if (whale.level() instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().broadcast(whale, new ClientboundSetPassengersPacket(whale));
            }
        }
    }

    public int getSeatByEntity(Entity entity) {
        if (entity == null) return -1;
        UUID uuid = entity.getUUID();
        for (int i = 0; i < activeSeatCount; i++) {
            Optional<UUID> seat = getSeatData(i);
            if (seat.isPresent() && seat.get().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

    public int findFreeSeat() {
        for (int i = 0; i < activeSeatCount; i++) {
            if (getSeatData(i).isEmpty()) return i;
        }
        return -1;
    }

    public boolean isPassengerAssigned(Entity passenger) {
        return getSeatByEntity(passenger) != -1;
    }

    public void removePassenger(Entity passenger) {
        int seatIndex = getSeatByEntity(passenger);
        if (seatIndex != -1) {
            assignSeat(seatIndex, null);
        }
    }

    public Optional<Entity> getPassengerForSeat(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= activeSeatCount) return Optional.empty();

        Optional<UUID> uuid = getSeatData(seatIndex);
        if (uuid.isEmpty()) return Optional.empty();

        if (whale.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid.get());
            return Optional.ofNullable(entity);
        }
        return Optional.empty();
    }

    // ─── Rotation ──────────────────────────────────────────────────────

    public void rotatePassengers() {
        if (whale.getPartManager() == null) return;

        var partManager = whale.getPartManager();

        for (Entity passenger : whale.getPassengers()) {
            int seat = getSeatByEntity(passenger);
            if (seat < 0 || seat >= activeSeatCount) continue;
            SeatLayout.SeatDef def = seatLayout.getSeatDef(seat);
            if (def == null) continue;
            int partIndex = def.rotPartIndex();
            if (partIndex < 0 || partIndex >= partManager.partYRot.length) continue;
            // Lateral offset: positive X offset = starboard (-1), negative X = port (+1), center = 0
            float offset = (float) Math.signum(def.offset().x) * -1f;

            if (passenger instanceof Player playerPassenger) {
                // For player passengers: only align body rotation with the whale part.
                // Don't touch yRot/xRot (camera look direction) or yHeadRot.
                playerPassenger.setYBodyRot(partManager.partYRot[partIndex] + offset);
            } else if (!(passenger instanceof CannonEntity cannonEntity && cannonEntity.isVehicle())) {
                float newYRot;
                float newXRot;

                if (passenger instanceof SailEntity) {
                    float lerpFactor = (float) (0.05 + 0.1 * partIndex);
                    newYRot = Mth.rotLerp(lerpFactor, passenger.getYRot(), partManager.partYRot[partIndex]) + offset;
                    newXRot = Mth.rotLerp(lerpFactor, passenger.getXRot(), partManager.partXRot[partIndex]);
                } else {
                    newYRot = partManager.partYRot[partIndex] + offset;
                    newXRot = partManager.partXRot[partIndex];
                }

                if (passenger instanceof WhaleWidgetEntity widget) {
                    widget.prevWidgetYRot = widget.getYRot();
                    widget.prevWidgetXRot = widget.getXRot();
                }

                passenger.setYRot(newYRot);
                passenger.setXRot(newXRot);
                passenger.setYBodyRot(newYRot);
                if (passenger instanceof LivingEntity livingWidget) {
                    livingWidget.yHeadRot = newYRot;
                    livingWidget.yBodyRot = newYRot;
                }
            }
        }
    }
}
