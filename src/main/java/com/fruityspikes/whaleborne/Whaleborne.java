package com.fruityspikes.whaleborne;

import com.fruityspikes.whaleborne.client.ClientProxy;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.data.HullConfigManager;
import com.fruityspikes.whaleborne.server.registries.*;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Whaleborne.MODID)
public class Whaleborne
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "whaleborne";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static CommonProxy PROXY = FMLLoader.getDist().isClient() ? new ClientProxy() : new CommonProxy();

    public static final ResourceLocation ANCHOR_GUI = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/gui/anchor.png");
    public Whaleborne(IEventBus modEventBus, ModContainer modContainer)
    {

        WBEntityRegistry.ENTITY_TYPES.register(modEventBus);
        WBBlockRegistry.BLOCKS.register(modEventBus);
        WBItemRegistry.ITEMS.register(modEventBus);
        WBMenuRegistry.MENUS.register(modEventBus);
        WBSoundRegistry.SOUND_EVENTS.register(modEventBus);
        WBLootModifierRegistry.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        WBParticleRegistry.PARTICLE_TYPES.register(modEventBus);
        WBBiomeModifierRegistry.register(modEventBus);
        WBCreativeTabsRegistry.CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        PROXY.init();
        modEventBus.addListener(this::registerSpawnPlacements);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }
    public void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
            WBEntityRegistry.HULLBACK.get(),
            SpawnPlacementTypes.IN_WATER,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            HullbackEntity::checkHullbackSpawnRules,
            RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        // Server starting hook
    }


    private static final HullConfigManager HULL_CONFIG_MANAGER = new HullConfigManager();

    @SubscribeEvent
    public void register(AddReloadListenerEvent event) {
        event.addListener(Whaleborne.PROXY.getHullbackDirtManager());
        event.addListener(HULL_CONFIG_MANAGER);
    }
}
