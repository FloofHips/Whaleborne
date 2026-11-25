package com.fruityspikes.whaleborne;

import com.fruityspikes.whaleborne.client.ClientProxy;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod(value = Whaleborne.MODID, dist = Dist.CLIENT)
public class WhaleborneClient {
    public static CommonProxy PROXY = new ClientProxy();

    public WhaleborneClient(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);

        // Enables the default in-game ConfigurationScreen provided by NeoForge
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public void register(AddReloadListenerEvent event) {
        event.addListener(WhaleborneClient.PROXY.getHullbackDirtManager());
    }
}
