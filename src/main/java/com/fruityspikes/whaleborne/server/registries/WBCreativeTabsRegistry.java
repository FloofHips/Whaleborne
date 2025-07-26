package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class WBCreativeTabsRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Whaleborne.MODID);
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
}
