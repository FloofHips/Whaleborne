package com.fruityspikes.whaleborne;

import com.fruityspikes.whaleborne.client.menus.CannonScreen;
import com.fruityspikes.whaleborne.client.menus.HullbackScreen;
import com.fruityspikes.whaleborne.client.models.*;
import com.fruityspikes.whaleborne.client.renderers.*;
import com.fruityspikes.whaleborne.network.WhaleborneNetwork;
import com.fruityspikes.whaleborne.server.entities.HelmEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.particles.WBSmokeParticle;
import com.fruityspikes.whaleborne.server.particles.WBSmokeProvider;
import com.fruityspikes.whaleborne.server.registries.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

//    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
//    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
//    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
//
//    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    //public static final RegistryObject<Item> EXAMPLE_ITEM = WBItemRegistry.ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
    //        .alwaysEat().nutrition(1).saturationMod(2f).build())));

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> WHALEBORNE = CREATIVE_MODE_TABS.register("whaleborne", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(Component.translatable("itemGroup.whaleborne.whaleborne"))
            .icon(() -> WBItemRegistry.SAIL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                WBItemRegistry.ITEMS.getEntries().forEach((i) -> {
                            output.accept(i.get().asItem());
                        }
                );
            }).build());

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
        WhaleborneNetwork.init();
        CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
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
