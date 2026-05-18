package com.fruityspikes.whaleborne.server.entities.components.hullback;

import java.util.List;

/** Per-hull walkable-platform configuration loaded from datapack JSON.
 *  Indexed by part: 0 = nose, 1 = head, 2 = body. */
public class PlatformLayout {
    public static final int MAX_PARTS = 3;

    public static final float DEFAULT_WIDTH = 6.5f;
    public static final float DEFAULT_HEIGHT = 0.5f;
    public static final float DEFAULT_X_OFFSET = 0f;
    public static final float DEFAULT_Y_OFFSET = 4.7f;
    public static final float DEFAULT_Z_OFFSET = 0f;
    public static final float DEFAULT_DETECTION_RANGE = 3.0f;
    public static final float DEFAULT_TILE_SIZE = 2.0f;
    public static final float LENGTH_AXIS_ALIGNED = -1.0f;
    public static final float TILE_SIZE_DEFAULT = -1.0f;

    public static final int ANCHOR_SELF = -1;
    public static final int ANCHOR_WHALE = 5;

    /** One rectangle in a part's composite collision shape. {@code dx}/{@code dz} are body-local
     *  XZ offsets (rotated by anchor yaw when placed); {@code dy} is an additional Y offset.
     *  {@code rotateWithYaw} controls whether the rectangle rotates with the anchor's yaw.
     *  {@code anchorPart} selects the pivot: {@link #ANCHOR_SELF} (the platform's own part),
     *  0..4 (nose/head/body/tail/fluke), or {@link #ANCHOR_WHALE} (the whale center +
     *  unarticulated yaw — use this for rigid ship structures that shouldn't wobble with
     *  per-part swim articulation). */
    public record ShapeDef(float dx, float dy, float dz, float width, float length,
                           boolean rotateWithYaw, int anchorPart, float tileSize) {}

    /** Per-part definition. {@code width}/{@code length}/{@code xOffset}/{@code zOffset} drive
     *  the legacy single-AABB path; {@code shapes} drives the tile-grid path. Both can run
     *  together when {@code legacyAabb} is explicitly {@code true} (hybrid mode). */
    public record PlatformDef(float width, float length, float height, float xOffset, float yOffset, float zOffset,
                              List<ShapeDef> shapes, float detectionRange, boolean legacyAabb) {
        public static PlatformDef defaults() {
            return new PlatformDef(DEFAULT_WIDTH, LENGTH_AXIS_ALIGNED, DEFAULT_HEIGHT,
                    DEFAULT_X_OFFSET, DEFAULT_Y_OFFSET, DEFAULT_Z_OFFSET, null, DEFAULT_DETECTION_RANGE, false);
        }

        /** True when the part should spawn its legacy single-AABB. Auto-detected when no shapes
         *  and no length are declared (wild Hullback case); explicit opt-in via {@code legacy_aabb}
         *  for hybrid setups where the legacy AABB coexists with tile shapes. */
        public boolean shouldSpawnLegacyAabb() {
            boolean hasShapes = shapes != null && !shapes.isEmpty();
            boolean hasLength = length >= 0;
            return legacyAabb || (!hasShapes && !hasLength);
        }
    }

    private final PlatformDef[] platforms;

    public PlatformLayout(PlatformDef[] platforms) {
        if (platforms.length != MAX_PARTS) {
            PlatformDef[] padded = new PlatformDef[MAX_PARTS];
            for (int i = 0; i < MAX_PARTS; i++) {
                padded[i] = (i < platforms.length && platforms[i] != null) ? platforms[i] : PlatformDef.defaults();
            }
            this.platforms = padded;
        } else {
            this.platforms = platforms;
        }
    }

    /** @param partIndex 0=nose, 1=head, 2=body */
    public PlatformDef getPlatform(int partIndex) {
        if (partIndex < 0 || partIndex >= MAX_PARTS) return PlatformDef.defaults();
        return platforms[partIndex] != null ? platforms[partIndex] : PlatformDef.defaults();
    }

    private static final PlatformLayout DEFAULT_LAYOUT_INSTANCE = new PlatformLayout(new PlatformDef[] {
            PlatformDef.defaults(),
            PlatformDef.defaults(),
            PlatformDef.defaults()
    });

    /** Shared singleton used by wild Hullbacks and unarmored materials — no per-call allocation. */
    public static PlatformLayout defaultLayout() {
        return DEFAULT_LAYOUT_INSTANCE;
    }
}
