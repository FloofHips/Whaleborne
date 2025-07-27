package com.fruityspikes.whaleborne.client.events;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.HelmEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = Whaleborne.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {
    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Player player = Minecraft.getInstance().player;

        if (player != null && player.getRootVehicle() instanceof HullbackEntity hullback) {
            double newRoll = Mth.lerp(0.5f, event.getRoll(), Mth.clamp((float) ((hullback.getPartYRot(3) - hullback.getPartYRot(0)) * 0.25f * Minecraft.getInstance().options.fovEffectScale().get()), -2.23f, 2.23f));
            event.setRoll((float) newRoll);
        }
    }
    @SubscribeEvent
    public static void onFovUpdate(ViewportEvent.ComputeFov event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getRootVehicle() instanceof HullbackEntity hullback) {
            if (hullback.getDeltaMovement().length() > 0.2) {
                double newFOV = Mth.lerp(0.5f, event.getFOV(), event.getFOV() + hullback.getDeltaMovement().length() * (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK ? 50 : 20) * Minecraft.getInstance().options.fovEffectScale().get());
                event.setFOV(newFOV);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.getVehicle() instanceof HelmEntity) {
            if (player.getRootVehicle() instanceof HullbackEntity hullback) {
                if (hullback.getDeltaMovement().length() > 0.2) {
                    event.getRenderer().getModel().rightArmPose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                    event.getRenderer().getModel().leftArmPose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                }
            }
        }
    }
}
