package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Whaleborne.MODID, bus = EventBusSubscriber.Bus.MOD)
public class WBEntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Whaleborne.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<HullbackEntity>> HULLBACK = ENTITY_TYPES.register(
            "hullback", () ->
                    EntityType.Builder.of(HullbackEntity::new, WBMobCategories.getHullbackCategory())
                            .sized(2F, 2F)
                            .eyeHeight(2.0f)  // Eye position above deck level
                            .attach(EntityAttachment.NAME_TAG, 0.0f, 7.0F, 0.0f)  // Name tag just above hitbox
                            .clientTrackingRange(128)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "hullback").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<HullbackWalkableEntity>> HULLBACK_PLATFORM = ENTITY_TYPES.register(
            "hullback_platform", () ->
                    EntityType.Builder.of(HullbackWalkableEntity::new, MobCategory.MISC)
                            .sized(5.5F, 0.5F)
                            .clientTrackingRange(128)
                            .setShouldReceiveVelocityUpdates(true)
                            .build(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "hullback_platform").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<SailEntity>> SAIL = ENTITY_TYPES.register(
            "sail", () ->
                    EntityType.Builder.of(SailEntity::new, MobCategory.MISC)
                            .sized(1F, 4.5F)
                            .clientTrackingRange(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "sail").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<CannonEntity>> CANNON = ENTITY_TYPES.register(
            "cannon", () ->
                    EntityType.Builder.of(CannonEntity::new, MobCategory.MISC)
                            .sized(2F, 0.5F)
                            .clientTrackingRange(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "cannon").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<HelmEntity>> HELM = ENTITY_TYPES.register(
            "helm", () ->
                    EntityType.Builder.of(HelmEntity::new, MobCategory.MISC)
                            .sized(2F, 0.5F)
                            .clientTrackingRange(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "helm").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MastEntity>> MAST = ENTITY_TYPES.register(
            "mast", () ->
                    EntityType.Builder.of(MastEntity::new, MobCategory.MISC)
                            .sized(1F, 8.3F)
                            .clientTrackingRange(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "mast").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<AnchorEntity>> ANCHOR = ENTITY_TYPES.register(
            "anchor", () ->
                    EntityType.Builder.of(AnchorEntity::new, MobCategory.MISC)
                            .sized(1.5F, 1.75F)
                            .clientTrackingRange(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "anchor").toString())
    );
    public static final DeferredHolder<EntityType<?>, EntityType<AnchorHeadEntity>> ANCHOR_HEAD = ENTITY_TYPES.register(
            "anchor_head", () ->
                    EntityType.Builder.of(AnchorHeadEntity::new, MobCategory.MISC)
                            .sized(0.5F, 2F)
                            .clientTrackingRange(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "anchor_head").toString())
    );
    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(HULLBACK.get(), HullbackEntity.createAttributes().build());
    }
}
