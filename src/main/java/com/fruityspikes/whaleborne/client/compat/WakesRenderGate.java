package com.fruityspikes.whaleborne.client.compat;

import com.fruityspikes.whaleborne.Config;
import com.leclowndu93150.wakes.simulation.Brick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WakesRenderGate {
    private static volatile Frustum lastFrustum;
    private static final double MARGIN = 16.0;

    private static final double D2_T1 = 48.0 * 48.0;
    private static final double D2_T2 = 96.0 * 96.0;
    private static final double D2_T3 = 160.0 * 160.0;

    private WakesRenderGate() {}

    public static void setFrustum(Frustum f) {
        lastFrustum = f;
    }

    public static boolean shouldRecolor(Brick brick) {
        if (!Config.wakesRecolorFrustumCulling) return true;
        Frustum f = lastFrustum;
        if (f == null) return true;
        double x = brick.pos.x, y = brick.pos.y, z = brick.pos.z;
        int d = brick.dim;
        return f.isVisible(new AABB(x - MARGIN, y - 0.5 - MARGIN, z - MARGIN,
                                    x + d + MARGIN, y + 0.5 + MARGIN, z + d + MARGIN));
    }

    public static boolean shouldRecolorTemporal(Brick brick, long gameTime, boolean hasPainted, boolean grew) {
        if (!Config.wakesRecolorTemporalLod) return true;
        if (!hasPainted || grew) return true;
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = brick.pos.x + brick.dim * 0.5 - cam.x;
        double dz = brick.pos.z + brick.dim * 0.5 - cam.z;
        double d2 = dx * dx + dz * dz;
        int stride = d2 < D2_T1 ? 1 : d2 < D2_T2 ? 2 : d2 < D2_T3 ? 3 : 4;
        if (stride == 1) return true;
        int phase = ((int) brick.pos.x * 31 + (int) brick.pos.z) & 0x7fffffff;
        return ((gameTime + phase) % stride) == 0;
    }
}
