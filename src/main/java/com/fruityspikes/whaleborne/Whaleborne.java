package com.fruityspikes.whaleborne;

import com.fruityspikes.whaleborne.client.menus.CannonScreen;
import com.fruityspikes.whaleborne.client.menus.HullbackScreen;
import com.fruityspikes.whaleborne.client.models.*;
import com.fruityspikes.whaleborne.client.renderers.*;
import com.fruityspikes.whaleborne.network.WhaleborneNetwork;
import com.fruityspikes.whaleborne.server.entities.AnchorEntity;
import com.fruityspikes.whaleborne.server.entities.CannonEntity;
import com.fruityspikes.whaleborne.server.entities.HelmEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.particles.WBSmokeParticle;
import com.fruityspikes.whaleborne.server.particles.WBSmokeProvider;
import com.fruityspikes.whaleborne.server.registries.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Whaleborne.MODID)
public class Whaleborne
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "whaleborne";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation ANCHOR_GUI = new ResourceLocation(Whaleborne.MODID, "textures/gui/anchor.png");
    public Whaleborne()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        WBEntityRegistry.ENTITY_TYPES.register(modEventBus);
        WBBlockRegistry.BLOCKS.register(modEventBus);
        WBItemRegistry.ITEMS.register(modEventBus);
        WBMenuRegistry.MENUS.register(modEventBus);
        WBSoundRegistry.SOUND_EVENTS.register(modEventBus);
        WBLootModifierRegistry.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        WBParticleRegistry.PARTICLE_TYPES.register(modEventBus);
        WBCreativeTabsRegistry.CREATIVE_MODE_TABS.register(modEventBus);
        WhaleborneNetwork.init();

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        //LOGGER.info("HELLO FROM COMMON SETUP");

        //if (Config.logDirtBlock)
        //    LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        //LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        //Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        //if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
        //    event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("whaleborne_anchor_overlay", (gui, poseStack, partialTick, width, height) -> {
                Player player = Minecraft.getInstance().player;

                if (player.getVehicle() instanceof HelmEntity) {
                    if (player.getRootVehicle() instanceof HullbackEntity hullback) {
                        for (Entity passenger : hullback.getPassengers()) {
                            if (passenger instanceof AnchorEntity) {
                                    int j = width / 2 - 12;
                                    int k = height - 28 - 13;

                                    poseStack.pose().pushPose();
                                    poseStack.blit(ANCHOR_GUI, j, k, 0, 0, hullback.hasAnchorDown() ? 24 : 0, 24, 24, 24, 48);
                                    poseStack.pose().popPose();
                                    break;
                            }
                        }
                    }
                }
            });
            //event.registerAboveAll("whaleborne_cannon_overlay", (gui, poseStack, partialTick, width, height) -> {
            //    Player player = Minecraft.getInstance().player;
//
            //    if (player.getVehicle() instanceof CannonEntity cannonEntity) {
//
            //        int j = width / 2;
            //        int k = height / 2 - 8;
//
            //        poseStack.pose().pushPose();
            //        poseStack.renderItem(cannonEntity.inventory.getItem(0), j - 32, k);
            //        poseStack.renderItem(cannonEntity.inventory.getItem(1), j + 16, k);
            //       // poseStack.blit(ANCHOR_GUI, j, k, 0, 0, hullback.hasAnchorDown() ? 24 : 0, 24, 24, 24, 48);
            //        poseStack.pose().popPose();
            //    }
            //});
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            MenuScreens.register(WBMenuRegistry.CANNON_MENU.get(), CannonScreen::new);
            MenuScreens.register(WBMenuRegistry.HULLBACK_MENU.get(), HullbackScreen::new);

            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(WBEntityModelLayers.HULLBACK, HullbackModel::createBodyLayer);
            event.registerLayerDefinition(WBEntityModelLayers.HULLBACK_ARMOR, HullbackArmorModel::createBodyLayer);
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
        }

        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(WBParticleRegistry.SMOKE.get(), WBSmokeProvider::new);
        }


    }
}
