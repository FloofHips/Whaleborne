package com.fruityspikes.whaleborne.server.entities.components.hullback;

import net.minecraft.world.phys.Vec3;

/**
 * Datapack-loadable seat configuration for a Hullback hull (default: 7 seats).
 * Part indices: 0=nose, 1=head, 2=body, 3=tail, 4=fluke.
 */
public class SeatLayout {
    public static final int MAX_SEATS = 16;

    /**
     * @param offset world-space offset relative to the part origin (rotated by part yaw at apply time)
     * @param posPartIndex which body part provides the POSITION (0-4)
     * @param rotPartIndex which body part provides the ROTATION (0-4)
     */
    public record SeatDef(Vec3 offset, int posPartIndex, int rotPartIndex) {}

    private final SeatDef[] seatDefs;
    private final int flukeSeatIndex;

    public SeatLayout(SeatDef[] seatDefs, int flukeSeatIndex) {
        if (seatDefs.length > MAX_SEATS) {
            SeatDef[] capped = new SeatDef[MAX_SEATS];
            System.arraycopy(seatDefs, 0, capped, 0, MAX_SEATS);
            this.seatDefs = capped;
        } else {
            this.seatDefs = seatDefs;
        }
        this.flukeSeatIndex = flukeSeatIndex;
    }

    public int getActiveSeatCount() { return seatDefs.length; }
    public SeatDef getSeatDef(int index) {
        if (index < 0 || index >= seatDefs.length) return null;
        return seatDefs[index];
    }
    public SeatDef[] getAllSeatDefs() { return seatDefs; }
    public int getFlukeSeatIndex() { return flukeSeatIndex; }

    /** Default 7-seat layout reproducing the original hardcoded behaviour. */
    public static SeatLayout defaultLayout() {
        return new SeatLayout(new SeatDef[] {
            new SeatDef(new Vec3(0, 5.5, 0.0),      0, 1),  // seat 0: sail — pos from nose, rot from head
            new SeatDef(new Vec3(0, 5.5, -3.0),      0, 1),  // seat 1: captain
            new SeatDef(new Vec3(1.5, 5.5, 0.3),     2, 2),  // seat 2: body right
            new SeatDef(new Vec3(-1.5, 5.5, 0.3),    2, 2),  // seat 3: body left
            new SeatDef(new Vec3(1.5, 5.5, -1.75),   2, 2),  // seat 4: body back right
            new SeatDef(new Vec3(-1.5, 5.5, -1.75),  2, 2),  // seat 5: body back left
            new SeatDef(new Vec3(0, 1.6, -0.8),      4, 4),  // seat 6: fluke
        }, 6);
    }
}
