package com.fruityspikes.whaleborne.server.entities.components.hullback;

import net.minecraft.world.phys.Vec3;

/**
 * Seat configuration for a Hullback hull: per-seat offsets and part indices.
 * Part indices: 0=nose, 1=head, 2=body, 3=tail, 4=fluke.
 */
public class SeatLayout {
    public static final int MAX_SEATS = 16;

    /** offset is relative to the part origin; posPartIndex/rotPartIndex (0-4)
     *  pick which part supplies position and rotation. */
    public record SeatDef(Vec3 offset, int posPartIndex, int rotPartIndex) {}

    private final SeatDef[] seatDefs;
    private final int flukeSeatIndex; // which seat gets fluke smoothing (-1 = none)

    public SeatLayout(SeatDef[] seatDefs, int flukeSeatIndex) {
        // Enforce cap to prevent abuse
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

    /** Default 7-seat layout. */
    public static SeatLayout defaultLayout() {
        return new SeatLayout(new SeatDef[] {
            new SeatDef(new Vec3(0, 5.5, 0.0),      0, 1),  // seat 0: sail — pos from nose[0], rot from head[1]
            new SeatDef(new Vec3(0, 5.5, -3.0),      0, 1),  // seat 1: captain
            new SeatDef(new Vec3(1.5, 5.5, 0.3),     2, 2),  // seat 2: body right
            new SeatDef(new Vec3(-1.5, 5.5, 0.3),    2, 2),  // seat 3: body left
            new SeatDef(new Vec3(1.5, 5.5, -1.75),   2, 2),  // seat 4: body back right
            new SeatDef(new Vec3(-1.5, 5.5, -1.75),  2, 2),  // seat 5: body back left
            new SeatDef(new Vec3(0, 1.6, -0.8),      4, 4),  // seat 6: fluke
        }, 6);
    }
}
