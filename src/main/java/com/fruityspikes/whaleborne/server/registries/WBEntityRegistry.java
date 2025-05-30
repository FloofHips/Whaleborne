package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.MastEntity;
import com.fruityspikes.whaleborne.server.entities.SailEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Whaleborne.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class WBEntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Whaleborne.MODID);
    public static final RegistryObject<EntityType<HullbackEntity>> HULLBACK = ENTITY_TYPES.register(
            "hullback", () ->
                    EntityType.Builder.of(HullbackEntity::new, MobCategory.UNDERGROUND_WATER_CREATURE)
                            .sized(2F, 2F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "hullback").toString())
    );
    public static final RegistryObject<EntityType<SailEntity>> SAIL = ENTITY_TYPES.register(
            "sail", () ->
                    EntityType.Builder.of(SailEntity::new, MobCategory.AMBIENT)
                            .sized(1F, 4.5F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "sail").toString())
    );
    public static final RegistryObject<EntityType<MastEntity>> MAST = ENTITY_TYPES.register(
            "mast", () ->
                    EntityType.Builder.of(MastEntity::new, MobCategory.AMBIENT)
                            .sized(1F, 8.3F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "mast").toString())
    );
    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(HULLBACK.get(), HullbackEntity.createAttributes().build());
    }
}
