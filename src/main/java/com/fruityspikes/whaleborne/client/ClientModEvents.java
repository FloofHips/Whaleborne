package com.fruityspikes.whaleborne.client;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.menus.CannonScreen;
import com.fruityspikes.whaleborne.client.menus.HullbackScreen;
import com.fruityspikes.whaleborne.client.models.*;
import com.fruityspikes.whaleborne.client.renderers.*;
import com.fruityspikes.whaleborne.server.entities.AnchorEntity;
import com.fruityspikes.whaleborne.server.entities.HelmEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.particles.WBSmokeProvider;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import com.fruityspikes.whaleborne.server.registries.WBMenuRegistry;
import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import static com.fruityspikes.whaleborne.Whaleborne.ANCHOR_GUI;
import static com.fruityspikes.whaleborne.Whaleborne.LOGGER;
import static com.fruityspikes.whaleborne.Whaleborne.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents
{
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(MODID, "anchor_overlay"), (guiGraphics, partialTick) -> {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            Player player = Minecraft.getInstance().player;

            if (player.getVehicle() instanceof HelmEntity) {
                if (player.getRootVehicle() instanceof HullbackEntity hullback) {
                    for (Entity passenger : hullback.getPassengers()) {
                        if (passenger instanceof AnchorEntity) {
                            int j = width / 2 - 12;
                            int k = height - 28 - 13;

                            guiGraphics.pose().pushPose();
                            guiGraphics.blit(ANCHOR_GUI, j, k, 0, 0, hullback.hasAnchorDown() ? 24 : 0, 24, 24, 24, 48);
                            guiGraphics.pose().popPose();
                            break;
                        }
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WBMenuRegistry.CANNON_MENU.get(), CannonScreen::new);
        event.register(WBMenuRegistry.HULLBACK_MENU.get(), HullbackScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        //Screen registration moved to registerScreens
        // Client setup complete
    }
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WBEntityModelLayers.HULLBACK, HullbackModel::createBodyLayer);
        event.registerLayerDefinition(WBEntityModelLayers.HULLBACK_ARMOR, HullbackArmorModel::createDataDrivenLayer);
        event.registerLayerDefinition(WBEntityModelLayers.SAIL, SailModel::createBodyLayer);
        event.registerLayerDefinition(WBEntityModelLayers.HELM, HelmModel::createBodyLayer);
        event.registerLayerDefinition(WBEntityModelLayers.MAST, MastModel::createBodyLayer);
        event.registerLayerDefinition(WBEntityModelLayers.CANNON, CannonModel::createBodyLayer);
        event.registerLayerDefinition(WBEntityModelLayers.ANCHOR, AnchorModel::createBodyLayer);
        event.registerLayerDefinition(WBEntityModelLayers.ANCHOR_HEAD, AnchorHeadModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WBEntityRegistry.HULLBACK.get(), HullbackRenderer::new);
        event.registerEntityRenderer(WBEntityRegistry.SAIL.get(), SailRenderer::new);
        event.registerEntityRenderer(WBEntityRegistry.MAST.get(), MastRenderer::new);
        event.registerEntityRenderer(WBEntityRegistry.CANNON.get(), CannonRenderer::new);
        event.registerEntityRenderer(WBEntityRegistry.HELM.get(), HelmRenderer::new);
        event.registerEntityRenderer(WBEntityRegistry.ANCHOR.get(), AnchorRenderer::new);
        event.registerEntityRenderer(WBEntityRegistry.ANCHOR_HEAD.get(), AnchorHeadRenderer::new);
        event.registerEntityRenderer(WBEntityRegistry.HULLBACK_PLATFORM.get(), NothingRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WBParticleRegistry.SMOKE.get(), WBSmokeProvider::new);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                resourceManager -> ArmorTextureResolver.clearCache());
    }
}
