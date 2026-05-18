package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Renders code-generated water wake foam for the Hullback using a no-depth-write
 *  RenderType so foam overlays the water surface without cutting it. */
public class HullbackWakeRenderer {

    // ─── Static foam texture ──────────────────────────────────────────
    private static final int TEX_SIZE = 64;
    private static ResourceLocation foamTexId = null;
    private static DynamicTexture foamDynTex = null;
    private static RenderType foamRenderType = null;

    // ─── Hull geometry reference ─────────────────────────────────────
    // Parts: nose(0)=5w, head(1)=5w, body(2)=5w, tail(3)=2.5w, fluke(4)=4w
    // Base Z offsets from entity center: nose=+6, head=+2.5, body=-2.25, tail=-7, fluke=-11
    // Hull half-width at waterline ≈ 3.0 blocks (model extends beyond bounding box)

    // ─── Wake shape constants ─────────────────────────────────────────
    private static final float SURFACE_Y_OFFSET = 0.125f;
    /** Hull half-width at water surface — foam starts at this distance from centerline. */
    private static final float HULL_HALF_WIDTH = 2.5f;

    // Bow wave — V-arms extend the full hull length (nose to tail)
    // 3-phase curve: V-spread → hull-parallel → tail-taper
    private static final float BOW_MAX_LENGTH = 18f;
    private static final float BOW_MAX_HALF_WIDTH = HULL_HALF_WIDTH + 1.5f; // ~4.7 blocks
    /** How far in front of the nose part the foam extends. */
    private static final float NOSE_FORWARD = 3f;
    private static final int BOW_SEGMENTS = 16;

    // Fluke splash
    private static final float FLUKE_SPLASH_SIZE = 3.5f;

    // Trail (records from tail at Z=-7, fluke at Z=-11)
    private static final float TRAIL_MAX_WIDTH = 2.5f;
    private static final float TRAIL_MIN_WIDTH = 0.6f;
    /** Skip the N newest trail entries so trail starts behind the fluke. */
    private static final int TRAIL_SKIP_NEWEST = 3;
    /** Minimum distance between consecutive trail points to render a segment. */
    private static final float TRAIL_MIN_SEGMENT_DIST = 0.3f;
    /** Trail points closer than this to the current fluke are not rendered (squared). */
    private static final float TRAIL_MIN_ENTITY_DIST_SQ = 20f; // ~4.5 blocks from fluke

    // Per-entity rendering state (thread-safe: rendering is single-threaded)
    private static int currentFoamR = 230;
    private static int currentFoamG = 245;
    private static int currentFoamB = 255;

    private static Boolean wakesModLoaded;

    /** Cached check for the Wakes mod. Lazy-initialized, safe to call from render thread. */
    public static boolean isWakesModLoaded() {
        if (wakesModLoaded == null) {
            wakesModLoaded = net.neoforged.fml.ModList.get().isLoaded("wakes");
        }
        return wakesModLoaded;
    }

    private HullbackWakeRenderer() {}

    // ═══════════════════════════════════════════════════════════════════
    //  Custom RenderType — no depth write
    // ═══════════════════════════════════════════════════════════════════

    private static class FoamRenderHelper extends RenderStateShard {
        private FoamRenderHelper() { super("whaleborne_foam_helper", () -> {}, () -> {}); }

        static RenderType create(ResourceLocation texture) {
            return RenderType.create(
                    "whaleborne_foam_overlay",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    1536,
                    false,
                    true,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(true)
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Public API
    // ═══════════════════════════════════════════════════════════════════

    /** Base SWIM_SPEED attribute value (no sails). Used to compute minimum wall height. */
    private static final float BASE_SWIM_SPEED = 1.2f;
    /** Maximum SWIM_SPEED with 5 sails (1.2 base + 5 * 1.0 sail modifier). */
    private static final float MAX_SWIM_SPEED = 6.2f;
    /** Minimum vertical wall height (at base speed, no sails). */
    private static final float WALL_MIN_HEIGHT = 0.4f;
    /** Maximum vertical wall height (at max speed, 5 sails). */
    private static final float WALL_MAX_HEIGHT = 2.0f;

    public static void renderWake(HullbackEntity entity, float partialTicks,
                                  PoseStack poseStack, MultiBufferSource buffer) {
        if (HullbackRenderer.isRenderingInHealthbarsGui) return;
        if (!com.fruityspikes.whaleborne.Config.wakeRendering) return;
        if (!entity.isInWater()) return;
        if (isWakesModLoaded()) return;
        if (entity.wakeIntensity < 0.005f && entity.flukeSplashIntensity < 0.005f) return;

        // Skip wakes if the hull is fully submerged (diving deep).
        // Normal swim depths: sailing=-4.55, boarding=-5.0, wild=-6.5 below sea level.
        // Only suppress wakes if deeper than 8 blocks below sea level (actual dive).
        if (entity.getY() < entity.level().getSeaLevel() - 8.0) return;

        ensureFoamTexture();
        if (foamTexId == null || foamRenderType == null) return;

        float waterY = entity.level().getSeaLevel() + SURFACE_Y_OFFSET;
        Vec3 renderPos = entity.getPosition(partialTicks);
        int surfaceLight = computeSurfaceLight(entity, waterY);

        currentFoamR = entity.wakeFoamR;
        currentFoamG = entity.wakeFoamG;
        currentFoamB = entity.wakeFoamB;
        float envMod = entity.wakeDepthFactor * entity.wakeWeatherFactor;

        VertexConsumer vc = buffer.getBuffer(foamRenderType);

        float intensity = Mth.clamp(entity.wakeIntensity * envMod, 0f, 1f);

        if (intensity > 0.005f) {
            float yawDelta = Mth.wrapDegrees(entity.getYRot() - entity.yRotO);
            float turnBoost = Mth.clamp(Math.abs(yawDelta) / 5f, 0f, 0.3f);

            // ── Unified bow-to-stern foam: V-arms extend the full hull length ──
            renderBowWake(entity, poseStack, vc, surfaceLight, partialTicks,
                    renderPos, waterY, intensity, turnBoost);

            // Stern wake and hull spray removed — the U-shape side strips
            // already cover the hull length, stern is handled by the trail

            renderTrail(entity, poseStack, vc, surfaceLight, partialTicks,
                    renderPos, waterY);
        }

        if (entity.flukeSplashIntensity > 0.005f) {
            renderFlukeSplash(entity, poseStack, vc, surfaceLight, partialTicks,
                    renderPos, waterY,
                    Mth.clamp(entity.flukeSplashIntensity * envMod, 0f, 1f));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Bow wave — parametric V-curve with multiple smooth segments
    // ═══════════════════════════════════════════════════════════════════

    /** Renders the bow wake as a U-shaped foam pattern (front bar + side strips), with
     *  a wider lighter water-displacement layer under a narrower near-white foam crest. */
    private static void renderBowWake(HullbackEntity entity, PoseStack ps,
                                      VertexConsumer vc, int light, float pt,
                                      Vec3 renderPos, float waterY,
                                      float intensity, float turnBoost) {
        Vec3 nosePos = lerpPartPos(entity, 0, pt);
        if (nosePos == null) return;
        float noseYaw = lerpPartYRot(entity, 0, pt);

        ps.pushPose();
        translateToWaterSurface(ps, nosePos, renderPos, waterY);
        ps.mulPose(Axis.YP.rotationDegrees(-noseYaw));

        Matrix4f pose = ps.last().pose();

        // ── Conditional positioning based on hull type ──
        // Armored: wider hull (planking), front face extends further forward
        // Bare (wild/unarmored): narrower organic shape, front face closer
        boolean armored = entity.getArmorProgress() > 0.5f;
        float hw = armored
                ? HULL_HALF_WIDTH + 0.7f    // armored hull is wider (planking adds ~1 block total)
                : HULL_HALF_WIDTH - 0.2f;   // bare hull is narrower (organic taper)
        float frontZ = armored
                ? NOSE_FORWARD              // armored bow extends to full NOSE_FORWARD
                : NOSE_FORWARD - 0.8f;      // bare nose is shorter/rounder, wake starts closer
        float uvBase = (entity.tickCount + pt) * 0.005f;

        // ════════════════════════════════════════════════
        //  LAYER 1: Water displacement (wider, lighter blue)
        // ════════════════════════════════════════════════
        int waterR = currentFoamR - 20, waterG = currentFoamG - 10, waterB = currentFoamB;
        float wA = intensity * 0.45f;

        // Front bar — water being pushed ahead of the hull
        float frontDepth = 2.5f * intensity; // how far ahead the water displacement extends
        int aFront = alpha(wA);
        int aFrontFar = alpha(wA * 0.1f);

        // Front displacement bar — transparent FAR, opaque NEAR hull (accumulation/density)
        // V higher at FAR, lower at NEAR → as uvScroll increases, pattern moves FAR→NEAR (adentrando)
        float uvFrontScroll = (entity.tickCount + pt) * 0.005f;
        addVertex(vc, pose, -hw,  0, frontZ + frontDepth, 0f, uvFrontScroll + 0.3f, waterR, waterG, waterB, aFrontFar, light, 0, 1, 0);
        addVertex(vc, pose,  hw,  0, frontZ + frontDepth, 1f, uvFrontScroll + 0.3f, waterR, waterG, waterB, aFrontFar, light, 0, 1, 0);
        addVertex(vc, pose,  hw,  0, frontZ,              1f, uvFrontScroll,        waterR, waterG, waterB, aFront,    light, 0, 1, 0);
        addVertex(vc, pose, -hw,  0, frontZ,              0f, uvFrontScroll,        waterR, waterG, waterB, aFront,    light, 0, 1, 0);

        // ── U-shape corners — UV reversed to match front bar (water entering hull) ──
        float sideW = armored ? 1.2f : 0.8f;  // narrower side strips for bare hull
        float cornerSize = sideW;
        int aCorner = alpha(wA * 0.8f);
        int aCornerOuter = alpha(wA * 0.12f);
        float cornerU = cornerSize / (hw * 2f);
        float cu0 = 0.5f - cornerU * 0.5f;
        float cu1 = 0.5f + cornerU * 0.5f;

        // Left corner — V higher at FAR, lower at NEAR (pattern moves toward hull)
        addVertex(vc, pose, -(hw),              0, frontZ,               cu0, uvFrontScroll,        waterR, waterG, waterB, aCorner,      light, 0, 1, 0);
        addVertex(vc, pose, -(hw + cornerSize), 0, frontZ,               cu1, uvFrontScroll,        waterR, waterG, waterB, aCornerOuter, light, 0, 1, 0);
        addVertex(vc, pose, -(hw + cornerSize), 0, frontZ + frontDepth,  cu1, uvFrontScroll + 0.3f, waterR, waterG, waterB, aCornerOuter, light, 0, 1, 0);
        addVertex(vc, pose, -(hw),              0, frontZ + frontDepth,  cu0, uvFrontScroll + 0.3f, waterR, waterG, waterB, aCornerOuter, light, 0, 1, 0);

        // Right corner — V higher at FAR, lower at NEAR (pattern moves toward hull)
        addVertex(vc, pose, (hw + cornerSize),  0, frontZ,               cu1, uvFrontScroll,        waterR, waterG, waterB, aCornerOuter, light, 0, 1, 0);
        addVertex(vc, pose, (hw),               0, frontZ,               cu0, uvFrontScroll,        waterR, waterG, waterB, aCorner,      light, 0, 1, 0);
        addVertex(vc, pose, (hw),               0, frontZ + frontDepth,  cu0, uvFrontScroll + 0.3f, waterR, waterG, waterB, aCornerOuter, light, 0, 1, 0);
        addVertex(vc, pose, (hw + cornerSize),  0, frontZ + frontDepth,  cu1, uvFrontScroll + 0.3f, waterR, waterG, waterB, aCornerOuter, light, 0, 1, 0);

        // ── Vertical water walls at the bow ──
        // Colors and shared params
        int crestR = Math.max(0, currentFoamR - 40);
        int crestG = Math.max(0, currentFoamG - 20);
        int crestB = currentFoamB;
        float crestHW = hw; // vertical walls match the hull width

        // ── Vertical wall Z position: closer to hull for bare, matches front for armored ──
        float wallBaseZ = armored ? frontZ + 0.05f : frontZ - 0.1f;

        // ── Vertical wall height: proportional to swim speed ──
        // At base swim speed (1.2, no sails): WALL_MIN_HEIGHT
        // At max swim speed (6.2, 5 sails): WALL_MAX_HEIGHT
        // AI goals (randomSwim, approach) use base speed → always minimum height
        float currentSwimSpeed = BASE_SWIM_SPEED;
        if (entity.getAttribute(HullbackEntity.getSwimSpeed()) != null) {
            currentSwimSpeed = (float) entity.getAttributeValue(HullbackEntity.getSwimSpeed());
        }
        float speedFraction = Mth.clamp(
                (currentSwimSpeed - BASE_SWIM_SPEED) / (MAX_SWIM_SPEED - BASE_SWIM_SPEED),
                0f, 1f);
        float wallMaxH = WALL_MIN_HEIGHT + (WALL_MAX_HEIGHT - WALL_MIN_HEIGHT) * speedFraction;

        float wall1Z = wallBaseZ;
        float wall2Z = wall1Z + 0.2f;
        float wall1H = wallMaxH * intensity;
        float wall2H = wall1H - 0.1f;
        float uvVert = (entity.tickCount + pt) * 0.005f;

        if (wall1H > 0.05f) {
            int aW1Base = alpha(intensity * 0.55f);
            int aW1Top  = alpha(intensity * 0.12f);
            // Back face uses higher alpha so the rider at the helm can see the walls clearly
            int aW1BackBase = alpha(intensity * 0.7f);
            int aW1BackTop  = alpha(intensity * 0.2f);

            // Wall 1 — front face
            addVertex(vc, pose, -crestHW, 0,       wall1Z, 0f, uvVert + 0.3f, crestR, crestG, crestB, aW1Base, light, 0, 0, 1);
            addVertex(vc, pose,  crestHW, 0,       wall1Z, 1f, uvVert + 0.3f, crestR, crestG, crestB, aW1Base, light, 0, 0, 1);
            addVertex(vc, pose,  crestHW, wall1H,  wall1Z, 1f, uvVert,        crestR, crestG, crestB, aW1Top,  light, 0, 0, 1);
            addVertex(vc, pose, -crestHW, wall1H,  wall1Z, 0f, uvVert,        crestR, crestG, crestB, aW1Top,  light, 0, 0, 1);
            // Wall 1 — back face (stronger alpha for helm visibility)
            addVertex(vc, pose,  crestHW, 0,       wall1Z, 1f, uvVert + 0.3f, crestR, crestG, crestB, aW1BackBase, light, 0, 0, -1);
            addVertex(vc, pose, -crestHW, 0,       wall1Z, 0f, uvVert + 0.3f, crestR, crestG, crestB, aW1BackBase, light, 0, 0, -1);
            addVertex(vc, pose, -crestHW, wall1H,  wall1Z, 0f, uvVert,        crestR, crestG, crestB, aW1BackTop,  light, 0, 0, -1);
            addVertex(vc, pose,  crestHW, wall1H,  wall1Z, 1f, uvVert,        crestR, crestG, crestB, aW1BackTop,  light, 0, 0, -1);
        }

        if (wall2H > 0.05f) {
            int aW2Base = alpha(intensity * 0.4f);
            int aW2Top  = alpha(intensity * 0.08f);
            int aW2BackBase = alpha(intensity * 0.55f);
            int aW2BackTop  = alpha(intensity * 0.15f);

            // Wall 2 — front face
            addVertex(vc, pose, -crestHW, 0,       wall2Z, 0f, uvVert + 0.3f, crestR, crestG, crestB, aW2Base, light, 0, 0, 1);
            addVertex(vc, pose,  crestHW, 0,       wall2Z, 1f, uvVert + 0.3f, crestR, crestG, crestB, aW2Base, light, 0, 0, 1);
            addVertex(vc, pose,  crestHW, wall2H,  wall2Z, 1f, uvVert,        crestR, crestG, crestB, aW2Top,  light, 0, 0, 1);
            addVertex(vc, pose, -crestHW, wall2H,  wall2Z, 0f, uvVert,        crestR, crestG, crestB, aW2Top,  light, 0, 0, 1);
            // Wall 2 — back face (stronger alpha for helm visibility)
            addVertex(vc, pose,  crestHW, 0,       wall2Z, 1f, uvVert + 0.3f, crestR, crestG, crestB, aW2BackBase, light, 0, 0, -1);
            addVertex(vc, pose, -crestHW, 0,       wall2Z, 0f, uvVert + 0.3f, crestR, crestG, crestB, aW2BackBase, light, 0, 0, -1);
            addVertex(vc, pose, -crestHW, wall2H,  wall2Z, 0f, uvVert,        crestR, crestG, crestB, aW2BackTop,  light, 0, 0, -1);
            addVertex(vc, pose,  crestHW, wall2H,  wall2Z, 1f, uvVert,        crestR, crestG, crestB, aW2BackTop,  light, 0, 0, -1);
        }

        // Top surface connecting wall 1 to wall 2 (visible from above/rider POV)
        if (wall1H > 0.05f) {
            addVertex(vc, pose, -crestHW, wall2H, wall2Z,  0f, uvFrontScroll + 0.3f, crestR, crestG, crestB, alpha(intensity * 0.08f), light, 0, 1, 0);
            addVertex(vc, pose,  crestHW, wall2H, wall2Z,  1f, uvFrontScroll + 0.3f, crestR, crestG, crestB, alpha(intensity * 0.08f), light, 0, 1, 0);
            addVertex(vc, pose,  crestHW, wall1H, wall1Z,  1f, uvFrontScroll,        crestR, crestG, crestB, alpha(intensity * 0.25f), light, 0, 1, 0);
            addVertex(vc, pose, -crestHW, wall1H, wall1Z,  0f, uvFrontScroll,        crestR, crestG, crestB, alpha(intensity * 0.25f), light, 0, 1, 0);
        }




        // ── Side displacement strips with PIXELATED FADING ──
        // UV scaled to match front bar pixel density (sideW/fullWidth ratio)
        float sideLen = BOW_MAX_LENGTH * (0.4f + 0.6f * intensity);
        int FADE_STEPS = 5;
        float uScale = sideW / (hw * 2f); // match front bar's pixels-per-block

        for (int side = -1; side <= 1; side += 2) {
            for (int seg = 0; seg < FADE_STEPS; seg++) {
                float t0 = seg / (float) FADE_STEPS;
                float t1 = (seg + 1) / (float) FADE_STEPS;

                float z0 = Mth.lerp(t0, frontZ, -sideLen);
                float z1 = Mth.lerp(t1, frontZ, -sideLen);

                // Stepped alpha for pixelated fade
                float stepAlpha = wA * (1.0f - t0) * (1.0f - t0);
                int aInner = alpha(stepAlpha * 0.9f);
                int aOuter = alpha(stepAlpha * 0.15f);

                // UV: U scaled to match front bar's pixel density, V tiles per segment
                float u0 = 0.5f - uScale * 0.5f;
                float u1 = 0.5f + uScale * 0.5f;
                float uvTileV0 = uvBase + seg * 0.25f;
                float uvTileV1 = uvTileV0 + 0.25f;

                float xInner = side * hw;
                float xOuter = side * (hw + sideW);

                addVertex(vc, pose, xInner, 0, z0, u0, uvTileV0, waterR, waterG, waterB, aInner, light, 0, 1, 0);
                addVertex(vc, pose, xOuter, 0, z0, u1, uvTileV0, waterR, waterG, waterB, aOuter, light, 0, 1, 0);
                addVertex(vc, pose, xOuter, 0, z1, u1, uvTileV1, waterR, waterG, waterB, aOuter, light, 0, 1, 0);
                addVertex(vc, pose, xInner, 0, z1, u0, uvTileV1, waterR, waterG, waterB, aInner, light, 0, 1, 0);
            }
        }

        // ── Second side layer: raised 0.15f, 40% length of main strips ──
        float sideLen2 = sideLen * 0.4f;
        float sideY2 = 0.15f;
        int FADE_STEPS_2 = 3; // fewer steps for shorter strip
        for (int side = -1; side <= 1; side += 2) {
            for (int seg = 0; seg < FADE_STEPS_2; seg++) {
                float t0 = seg / (float) FADE_STEPS_2;
                float t1 = (seg + 1) / (float) FADE_STEPS_2;

                float z0 = Mth.lerp(t0, frontZ, -sideLen2);
                float z1 = Mth.lerp(t1, frontZ, -sideLen2);

                float stepAlpha = wA * (1.0f - t0) * (1.0f - t0) * 0.7f;
                int aInner = alpha(stepAlpha * 0.85f);
                int aOuter = alpha(stepAlpha * 0.12f);

                float u0 = 0.5f - uScale * 0.5f;
                float u1 = 0.5f + uScale * 0.5f;
                float uvTileV0 = uvBase + 0.5f + seg * 0.25f;
                float uvTileV1 = uvTileV0 + 0.25f;

                float xInner = side * hw;
                float xOuter = side * (hw + sideW);

                addVertex(vc, pose, xInner, sideY2, z0, u0, uvTileV0, waterR, waterG, waterB, aInner, light, 0, 1, 0);
                addVertex(vc, pose, xOuter, sideY2, z0, u1, uvTileV0, waterR, waterG, waterB, aOuter, light, 0, 1, 0);
                addVertex(vc, pose, xOuter, sideY2, z1, u1, uvTileV1, waterR, waterG, waterB, aOuter, light, 0, 1, 0);
                addVertex(vc, pose, xInner, sideY2, z1, u0, uvTileV1, waterR, waterG, waterB, aInner, light, 0, 1, 0);
            }
        }

        // ── Third side layer: 0.1f above second (0.25f total), 15% length ──
        float sideLen3 = sideLen * 0.15f;
        float sideY3 = sideY2 + 0.1f;
        int FADE_STEPS_3 = 2;
        for (int side = -1; side <= 1; side += 2) {
            for (int seg = 0; seg < FADE_STEPS_3; seg++) {
                float t0 = seg / (float) FADE_STEPS_3;
                float t1 = (seg + 1) / (float) FADE_STEPS_3;

                float z0 = Mth.lerp(t0, frontZ, -sideLen3);
                float z1 = Mth.lerp(t1, frontZ, -sideLen3);

                float stepAlpha = wA * (1.0f - t0) * (1.0f - t0) * 0.45f;
                int aInner = alpha(stepAlpha * 0.8f);
                int aOuter = alpha(stepAlpha * 0.1f);

                float u0 = 0.5f - uScale * 0.5f;
                float u1 = 0.5f + uScale * 0.5f;
                float uvTileV0 = uvBase + 1.0f + seg * 0.25f;
                float uvTileV1 = uvTileV0 + 0.25f;

                float xInner = side * hw;
                float xOuter = side * (hw + sideW);

                addVertex(vc, pose, xInner, sideY3, z0, u0, uvTileV0, waterR, waterG, waterB, aInner, light, 0, 1, 0);
                addVertex(vc, pose, xOuter, sideY3, z0, u1, uvTileV0, waterR, waterG, waterB, aOuter, light, 0, 1, 0);
                addVertex(vc, pose, xOuter, sideY3, z1, u1, uvTileV1, waterR, waterG, waterB, aOuter, light, 0, 1, 0);
                addVertex(vc, pose, xInner, sideY3, z1, u0, uvTileV1, waterR, waterG, waterB, aInner, light, 0, 1, 0);
            }
        }

        // ════════════════════════════════════════════════
        //  LAYER 2: Foam crest (narrower, near-white, slightly above)
        // ════════════════════════════════════════════════
        float foamY = 0.03f; // slight Y offset above water layer
        int foamR = Math.min(255, currentFoamR + 20);
        int foamG = Math.min(255, currentFoamG + 10);
        int foamB = 255;
        float fA = intensity * 0.6f;
        float uvFoam = uvBase + 0.5f; // different UV phase

        // Front foam bar (narrower than displacement, sits on top)
        float foamFrontDepth = 1.5f * intensity;
        float foamHW = hw - 0.3f;
        int aFoamFront = alpha(fA);
        int aFoamFar = alpha(fA * 0.15f);

        // Front foam bar — V higher at FAR, lower at NEAR (pattern adentrando)
        addVertex(vc, pose, -foamHW, foamY, frontZ + foamFrontDepth, 0.1f, uvFoam + 0.2f, foamR, foamG, foamB, aFoamFar,   light, 0, 1, 0);
        addVertex(vc, pose,  foamHW, foamY, frontZ + foamFrontDepth, 0.9f, uvFoam + 0.2f, foamR, foamG, foamB, aFoamFar,   light, 0, 1, 0);
        addVertex(vc, pose,  foamHW, foamY, frontZ,                  0.9f, uvFoam,        foamR, foamG, foamB, aFoamFront, light, 0, 1, 0);
        addVertex(vc, pose, -foamHW, foamY, frontZ,                  0.1f, uvFoam,        foamR, foamG, foamB, aFoamFront, light, 0, 1, 0);

        // Side foam strips with PIXELATED FADING and tiled UV
        float foamSideW = 0.8f;

        float foamUScale = foamSideW / (foamHW * 2f);
        for (int side = -1; side <= 1; side += 2) {
            for (int seg = 0; seg < FADE_STEPS; seg++) {
                float t0 = seg / (float) FADE_STEPS;
                float t1 = (seg + 1) / (float) FADE_STEPS;

                float z0 = Mth.lerp(t0, frontZ, -sideLen);
                float z1 = Mth.lerp(t1, frontZ, -sideLen);

                float stepAlpha = fA * (1.0f - t0) * (1.0f - t0);
                int aInner = alpha(stepAlpha * 0.85f);
                int aOuter = alpha(stepAlpha * 0.1f);

                // UV scaled to match front bar's pixel density
                float fu0 = 0.5f - foamUScale * 0.5f;
                float fu1 = 0.5f + foamUScale * 0.5f;
                float uvTileV0 = uvFoam + seg * 0.25f;
                float uvTileV1 = uvTileV0 + 0.25f;

                float xInner = side * hw;
                float xOuter = side * (hw + foamSideW);

                addVertex(vc, pose, xInner, foamY, z0, fu0, uvTileV0, foamR, foamG, foamB, aInner, light, 0, 1, 0);
                addVertex(vc, pose, xOuter, foamY, z0, fu1, uvTileV0, foamR, foamG, foamB, aOuter, light, 0, 1, 0);
                addVertex(vc, pose, xOuter, foamY, z1, fu1, uvTileV1, foamR, foamG, foamB, aOuter, light, 0, 1, 0);
                addVertex(vc, pose, xInner, foamY, z1, fu0, uvTileV1, foamR, foamG, foamB, aInner, light, 0, 1, 0);
            }
        }

        ps.popPose();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Side wakes
    // ═══════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════
    //  Fluke splash
    // ═══════════════════════════════════════════════════════════════════

    private static void renderFlukeSplash(HullbackEntity entity, PoseStack ps,
                                          VertexConsumer vc, int light, float pt,
                                          Vec3 renderPos, float waterY,
                                          float splashIntensity) {
        Vec3 flukePos = lerpPartPos(entity, 4, pt);
        if (flukePos == null) return;
        float flukeYaw = lerpPartYRot(entity, 4, pt);

        if (flukePos.y < waterY - 1.5f) return;

        ps.pushPose();
        translateToWaterSurface(ps, flukePos, renderPos, waterY);
        ps.mulPose(Axis.YP.rotationDegrees(-flukeYaw));

        Matrix4f pose = ps.last().pose();

        float uvS = (entity.tickCount + pt) * 0.02f;
        float r = FLUKE_SPLASH_SIZE * splashIntensity;
        int aCenter = alpha(splashIntensity * 0.4f);
        int aEdge   = alpha(splashIntensity * 0.06f);

        emitQuad(vc, pose, light,
                -r * 0.5f, 0, r,    0.2f, uvS,        aEdge,
                r * 0.5f,  0, r,    0.8f, uvS,        aEdge,
                r * 0.3f,  0, 0,    0.7f, uvS + 0.5f, aCenter,
                -r * 0.3f, 0, 0,    0.3f, uvS + 0.5f, aCenter);
        emitQuad(vc, pose, light,
                -r * 0.3f, 0, 0,    0.3f, uvS + 0.5f, aCenter,
                r * 0.3f,  0, 0,    0.7f, uvS + 0.5f, aCenter,
                r * 0.5f,  0, -r,   0.8f, uvS + 1f,   aEdge,
                -r * 0.5f, 0, -r,   0.2f, uvS + 1f,   aEdge);
        emitQuad(vc, pose, light,
                -r,        0, r * 0.5f,  0f,   uvS + 0.2f, aEdge,
                -r * 0.3f, 0, r * 0.3f,  0.3f, uvS + 0.3f, aCenter,
                -r * 0.3f, 0, -r * 0.3f, 0.3f, uvS + 0.7f, aCenter,
                -r,        0, -r * 0.5f, 0f,   uvS + 0.8f, aEdge);
        emitQuad(vc, pose, light,
                r * 0.3f,  0, r * 0.3f,  0.7f, uvS + 0.3f, aCenter,
                r,         0, r * 0.5f,  1f,   uvS + 0.2f, aEdge,
                r,         0, -r * 0.5f, 1f,   uvS + 0.8f, aEdge,
                r * 0.3f,  0, -r * 0.3f, 0.7f, uvS + 0.7f, aCenter);

        ps.popPose();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Trail — connected strip from consecutive historical positions
    // ═══════════════════════════════════════════════════════════════════

    /** Renders a dual-layer foam ribbon from historical tail positions: a bright narrow
     *  inner crest over a wide diffuse outer spread, with pulsing width and UV flow. */
    private static void renderTrail(HullbackEntity entity, PoseStack ps,
                                    VertexConsumer vc, int light, float pt,
                                    Vec3 renderPos, float waterY) {
        // Use fluke (part 4) as reference — it's the actual end of the entity at Z≈-11
        Vec3 currentTail = lerpPartPos(entity, 4, pt);

        ps.pushPose();
        ps.translate(0, waterY - renderPos.y, 0);
        Matrix4f pose = ps.last().pose();

        float time = entity.tickCount + pt;
        float uvFlow = time * 0.004f;
        int trailLen = HullbackEntity.WAKE_TRAIL_LENGTH;

        // ── Collect valid trail segments first ──
        // We need the full list to compute smooth perpendiculars and dual-layer rendering
        Vec3[] segPos = new Vec3[trailLen];
        float[] segAlpha = new float[trailLen];
        int segCount = 0;

        for (int i = TRAIL_SKIP_NEWEST; i < trailLen; i++) {
            int idx = (entity.wakeTrailHead - 1 - i + trailLen) % trailLen;
            Vec3 pos = entity.wakeTrailPos[idx];
            float a = entity.wakeTrailAlpha[idx];

            if (pos == null || a < 0.003f) continue;
            if (currentTail != null && pos.distanceToSqr(currentTail) < TRAIL_MIN_ENTITY_DIST_SQ) continue;

            // Check minimum distance from last accepted point
            if (segCount > 0) {
                double dx = segPos[segCount - 1].x - pos.x;
                double dz = segPos[segCount - 1].z - pos.z;
                if (dx * dx + dz * dz < TRAIL_MIN_SEGMENT_DIST * TRAIL_MIN_SEGMENT_DIST) continue;
            }

            segPos[segCount] = pos;
            segAlpha[segCount] = a;
            segCount++;
        }

        if (segCount < 2) { ps.popPose(); return; }

        // ── Compute perpendiculars with smoothing ──
        float[] perpX = new float[segCount];
        float[] perpZ = new float[segCount];

        for (int i = 0; i < segCount - 1; i++) {
            double dx = segPos[i].x - segPos[i + 1].x;
            double dz = segPos[i].z - segPos[i + 1].z;
            double hDist = Math.sqrt(dx * dx + dz * dz);
            if (hDist < 0.01) { perpX[i] = 0; perpZ[i] = 1; continue; }
            perpX[i] = (float) (-dz / hDist);
            perpZ[i] = (float) (dx / hDist);
        }
        perpX[segCount - 1] = perpX[segCount - 2];
        perpZ[segCount - 1] = perpZ[segCount - 2];

        // Smooth perpendiculars (3-pass averaging for very fluid curves)
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 1; i < segCount - 1; i++) {
                perpX[i] = perpX[i] * 0.5f + (perpX[i - 1] + perpX[i + 1]) * 0.25f;
                perpZ[i] = perpZ[i] * 0.5f + (perpZ[i - 1] + perpZ[i + 1]) * 0.25f;
                float pLen = (float) Math.sqrt(perpX[i] * perpX[i] + perpZ[i] * perpZ[i]);
                if (pLen > 0.01f) { perpX[i] /= pLen; perpZ[i] /= pLen; }
            }
        }

        // ── Render dual-layer trail ──
        for (int i = 0; i < segCount - 1; i++) {
            float ageFactor = 1.0f - (i / (float) (segCount - 1));

            // Dynamic width pulsing: sine breathing per segment
            float pulse = 1.0f + Mth.sin(time * 0.15f + i * 0.6f) * 0.12f;

            // Inner layer: bright, narrow center foam crest
            float innerW = (TRAIL_MIN_WIDTH * 0.6f + (TRAIL_MAX_WIDTH * 0.5f - TRAIL_MIN_WIDTH * 0.6f) * ageFactor) * pulse;
            // Outer layer: wide, diffuse spread
            float outerW = (TRAIL_MIN_WIDTH + (TRAIL_MAX_WIDTH * 1.3f - TRAIL_MIN_WIDTH) * ageFactor) * pulse;

            // Alpha with per-segment variation (simulates foam density changes)
            float densityVar = 0.85f + pseudoHash(i, entity.getId() & 0xFF) * 0.3f;
            float a0 = segAlpha[i] * densityVar;
            float a1 = segAlpha[i + 1] * densityVar;

            float nx = (float) (segPos[i].x - renderPos.x);
            float nz = (float) (segPos[i].z - renderPos.z);
            float ox = (float) (segPos[i + 1].x - renderPos.x);
            float oz = (float) (segPos[i + 1].z - renderPos.z);

            float px0 = perpX[i], pz0 = perpZ[i];
            float px1 = perpX[i + 1], pz1 = perpZ[i + 1];

            float uvRow0 = uvFlow + i * 0.06f;
            float uvRow1 = uvFlow + (i + 1) * 0.06f;

            // ── Outer layer: wide diffuse foam spread ──
            int aO0Inner = alpha(a0 * 0.4f);
            int aO0Outer = alpha(a0 * 0.08f);
            int aO1Inner = alpha(a1 * 0.4f);
            int aO1Outer = alpha(a1 * 0.08f);

            addVertex(vc, pose, nx + px0 * outerW, 0, nz + pz0 * outerW,
                    0.05f, uvRow0, currentFoamR, currentFoamG, currentFoamB, aO0Inner, light, 0, 1, 0);
            addVertex(vc, pose, ox + px1 * outerW, 0, oz + pz1 * outerW,
                    0.05f, uvRow1, currentFoamR, currentFoamG, currentFoamB, aO1Inner, light, 0, 1, 0);
            addVertex(vc, pose, ox - px1 * outerW, 0, oz - pz1 * outerW,
                    0.95f, uvRow1, currentFoamR, currentFoamG, currentFoamB, aO1Outer, light, 0, 1, 0);
            addVertex(vc, pose, nx - px0 * outerW, 0, nz - pz0 * outerW,
                    0.95f, uvRow0, currentFoamR, currentFoamG, currentFoamB, aO0Outer, light, 0, 1, 0);

            // ── Inner layer: bright foam crest (rendered slightly above outer) ──
            // Uses different UV offset so foam patterns don't overlap
            float uvInner0 = uvFlow * 1.3f + i * 0.09f + 0.5f;
            float uvInner1 = uvFlow * 1.3f + (i + 1) * 0.09f + 0.5f;

            int aI0Center = alpha(a0 * 0.7f);
            int aI0Edge   = alpha(a0 * 0.2f);
            int aI1Center = alpha(a1 * 0.7f);
            int aI1Edge   = alpha(a1 * 0.2f);

            // Slight Y offset above outer layer to prevent z-fighting between layers
            float innerY = 0.02f;

            addVertex(vc, pose, nx + px0 * innerW, innerY, nz + pz0 * innerW,
                    0.15f, uvInner0, currentFoamR + 10, currentFoamG + 5, currentFoamB, aI0Center, light, 0, 1, 0);
            addVertex(vc, pose, ox + px1 * innerW, innerY, oz + pz1 * innerW,
                    0.15f, uvInner1, currentFoamR + 10, currentFoamG + 5, currentFoamB, aI1Center, light, 0, 1, 0);
            addVertex(vc, pose, ox - px1 * innerW, innerY, oz - pz1 * innerW,
                    0.85f, uvInner1, currentFoamR + 10, currentFoamG + 5, currentFoamB, aI1Edge, light, 0, 1, 0);
            addVertex(vc, pose, nx - px0 * innerW, innerY, nz - pz0 * innerW,
                    0.85f, uvInner0, currentFoamR + 10, currentFoamG + 5, currentFoamB, aI0Edge, light, 0, 1, 0);
        }

        ps.popPose();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Procedural foam texture generation
    // ═══════════════════════════════════════════════════════════════════

    /** Generates a 64x64 foam texture of 4x4 white cells on a transparent background. */
    private static void ensureFoamTexture() {
        if (foamTexId != null) return;
        try {
            NativeImage image = new NativeImage(TEX_SIZE, TEX_SIZE, false);

            for (int x = 0; x < TEX_SIZE; x++) {
                for (int y = 0; y < TEX_SIZE; y++) {
                    // 4x4 pixel cells — clean Minecraft-style blocks
                    int bx = x >> 2;
                    int by = y >> 2;
                    float noise = pseudoHash(bx, by);

                    if (noise > 0.45f) {
                        float strength = (noise - 0.45f) / 0.55f;
                        int r = 220 + (int) (35 * strength);
                        int g = 235 + (int) (20 * strength);
                        int b = 255;
                        int a = 100 + (int) (155 * strength);
                        image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                    } else {
                        image.setPixelRGBA(x, y, 0);
                    }
                }
            }

            foamDynTex = new DynamicTexture(image);
            foamTexId = Minecraft.getInstance().getTextureManager()
                    .register("whaleborne_wake_foam", foamDynTex);
            foamRenderType = FoamRenderHelper.create(foamTexId);
        } catch (Exception e) {
            foamTexId = null;
            foamRenderType = null;
        }
    }

    private static float pseudoHash(int x, int y) {
        int n = x * 374761393 + y * 668265263 + 1376312589;
        n = (n ^ (n >> 13)) * 1274126177;
        n = n ^ (n >> 16);
        return (n & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════

    private static Vec3 lerpPartPos(HullbackEntity entity, int index, float pt) {
        Vec3 pos    = entity.getPartPos(index);
        Vec3 oldPos = entity.getOldPartPos(index);
        if (pos == null || oldPos == null) return null;
        return oldPos.lerp(pos, pt);
    }

    private static float lerpPartYRot(HullbackEntity entity, int index, float pt) {
        float yRot    = entity.getPartYRot(index);
        float oldYRot = entity.getOldPartYRot(index);
        return oldYRot + Mth.wrapDegrees(yRot - oldYRot) * pt;
    }

    private static void translateToWaterSurface(PoseStack ps, Vec3 partPos,
                                                Vec3 renderPos, float waterY) {
        ps.translate(
                partPos.x - renderPos.x,
                waterY    - renderPos.y,
                partPos.z - renderPos.z
        );
    }

    private static int alpha(float f) {
        return Mth.clamp((int) (f * 255), 0, 255);
    }

    private static int computeSurfaceLight(HullbackEntity entity, float waterY) {
        try {
            BlockPos surfacePos = BlockPos.containing(entity.getX(), waterY + 0.5, entity.getZ());
            int sky   = entity.level().getBrightness(LightLayer.SKY,   surfacePos);
            int block = entity.level().getBrightness(LightLayer.BLOCK, surfacePos);
            return LightTexture.pack(block, sky);
        } catch (Exception e) {
            return LightTexture.FULL_BRIGHT;
        }
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f pose, int light,
                                 float x0, float y0, float z0, float u0, float v0, int a0,
                                 float x1, float y1, float z1, float u1, float v1, int a1,
                                 float x2, float y2, float z2, float u2, float v2, int a2,
                                 float x3, float y3, float z3, float u3, float v3, int a3) {
        a0 = Mth.clamp(a0, 0, 255);
        a1 = Mth.clamp(a1, 0, 255);
        a2 = Mth.clamp(a2, 0, 255);
        a3 = Mth.clamp(a3, 0, 255);

        addVertex(vc, pose, x0, y0, z0, u0, v0, currentFoamR, currentFoamG, currentFoamB, a0, light, 0, 1, 0);
        addVertex(vc, pose, x1, y1, z1, u1, v1, currentFoamR, currentFoamG, currentFoamB, a1, light, 0, 1, 0);
        addVertex(vc, pose, x2, y2, z2, u2, v2, currentFoamR, currentFoamG, currentFoamB, a2, light, 0, 1, 0);
        addVertex(vc, pose, x3, y3, z3, u3, v3, currentFoamR, currentFoamG, currentFoamB, a3, light, 0, 1, 0);
    }

    private static void addVertex(VertexConsumer vc, Matrix4f pose,
                                  float x, float y, float z, float u, float v,
                                  int r, int g, int b, int a, int light,
                                  float nx, float ny, float nz) {
        org.joml.Vector4f vec = new org.joml.Vector4f(x, y, z, 1.0f);
        vec.mul(pose);
        vc.addVertex(vec.x, vec.y, vec.z)
          .setColor(r, g, b, a)
          .setUv(u, v)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(light)
          .setNormal(nx, ny, nz);
    }

    public static void clearCachedTexture() {
        if (foamDynTex != null) {
            foamDynTex.close();
            foamDynTex = null;
        }
        foamTexId = null;
        foamRenderType = null;
    }
}
