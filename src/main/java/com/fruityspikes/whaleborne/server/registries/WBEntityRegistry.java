package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.*;
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
                    EntityType.Builder.of(HullbackEntity::new, WBMobCategories.HULLBACK)
                            .sized(2F, 2F)
                            .clientTrackingRange(128)
                            .build(new ResourceLocation(Whaleborne.MODID, "hullback").toString())
    );
    public static final RegistryObject<EntityType<SailEntity>> SAIL = ENTITY_TYPES.register(
            "sail", () ->
                    EntityType.Builder.of(SailEntity::new, MobCategory.MISC)
                            .sized(1F, 4.5F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "sail").toString())
    );
    public static final RegistryObject<EntityType<CannonEntity>> CANNON = ENTITY_TYPES.register(
            "cannon", () ->
                    EntityType.Builder.of(CannonEntity::new, MobCategory.MISC)
                            .sized(2F, 0.5F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "cannon").toString())
    );
    public static final RegistryObject<EntityType<HelmEntity>> HELM = ENTITY_TYPES.register(
            "helm", () ->
                    EntityType.Builder.of(HelmEntity::new, MobCategory.MISC)
                            .sized(2F, 0.5F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "helm").toString())
    );
    public static final RegistryObject<EntityType<MastEntity>> MAST = ENTITY_TYPES.register(
            "mast", () ->
                    EntityType.Builder.of(MastEntity::new, MobCategory.MISC)
                            .sized(1F, 8.3F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "mast").toString())
    );
    public static final RegistryObject<EntityType<AnchorEntity>> ANCHOR = ENTITY_TYPES.register(
            "anchor", () ->
                    EntityType.Builder.of(AnchorEntity::new, MobCategory.MISC)
                            .sized(1.5F, 1.75F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "anchor").toString())
    );
    public static final RegistryObject<EntityType<AnchorHeadEntity>> ANCHOR_HEAD = ENTITY_TYPES.register(
            "anchor_head", () ->
                    EntityType.Builder.of(AnchorHeadEntity::new, MobCategory.MISC)
                            .sized(1.5F, 2F)
                            .clientTrackingRange(20)
                            .build(new ResourceLocation(Whaleborne.MODID, "anchor_head").toString())
    );
    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(HULLBACK.get(), HullbackEntity.createAttributes().build());
//        event.put(SAIL.get(), SailEntity.createAttributes().build());
//        event.put(CANNON.get(), CannonEntity.createAttributes().build());
//        event.put(HELM.get(), HelmEntity.createAttributes().build());
//        event.put(MAST.get(), MastEntity.createAttributes().build());
        //event.put(ANCHOR.get(), HullbackEntity.createAttributes().build());
    }
}
