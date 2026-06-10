package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Config;
import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.HullbackArmorModel;
import com.fruityspikes.whaleborne.client.models.HullbackModel;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.registries.WBBlockRegistry;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.fruityspikes.whaleborne.server.registries.WBTagRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ForgeItemTagsProvider;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HullbackRenderer<T extends HullbackEntity> extends MobRenderer<HullbackEntity, HullbackModel<HullbackEntity>> {
    /** Suppresses water-wake overlay during inventory/healthbars GUI previews. */
    public static boolean isRenderingInHealthbarsGui = false;
    private static final java.util.Set<String> SPECIAL_HEAD_NAMES = java.util.Set.of("SH8RK");
    public static final ResourceLocation MOB_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback.png");
    public static final ResourceLocation STEEN_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/steen.png");
    //public static final ResourceLocation MOBIUS_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/mobius.png");
    public static final ResourceLocation SADDLE_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback_saddle.png");
    public static final ResourceLocation ARMOR_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/armor/hullback_dark_oak_planks_armor.png");
    public static final ResourceLocation ARMOR_PROGRESS = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback_armor_progress.png");
    private final HullbackArmorModel<HullbackEntity> armorModel;
    private final Map<String, HullbackArmorModel<HullbackEntity>> materialArmorModels = new HashMap<>();

    public HullbackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HullbackModel<>(ctx.bakeLayer(WBEntityModelLayers.HULLBACK)), 5F);
        this.armorModel = new HullbackArmorModel<>(ctx.bakeLayer(WBEntityModelLayers.HULLBACK_ARMOR));
    }

    public ResourceLocation getArmor(HullbackEntity pEntity) {
        if (pEntity.getArmorProgress() > 0) {
            ItemStack armor = pEntity.getArmor();
            if (!armor.isEmpty()) {
                return ArmorTextureResolver.resolve(armor.getItem());
            }
        }
        return ARMOR_TEXTURE;
    }

    @Override
    public void render(HullbackEntity pEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(pEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // Procedural water-wake foam overlay
        HullbackWakeRenderer.renderWake(pEntity, partialTicks, poseStack, buffer);

        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getHead(), this.armorModel.getHead(), 0, 5.0F, 5.0F);
        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getBody(), this.armorModel.getBody(), 2, 5.0F, 5.0F);

        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getTail(), null, 3, 2.5F, 2.5F);
        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getFluke(), this.armorModel.getFluke(), 4, 0.6F, 4.0F);

        if(entityRenderDispatcher.shouldRenderHitBoxes())
            renderDebug(pEntity, poseStack, buffer, partialTicks);
    }

    private void renderDebug(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, float partialTicks) {
        if(pEntity.seats[0]!=null){
            for ( Vec3 seat : pEntity.seats ) {
                if (seat == null) continue;
                poseStack.pushPose();

                poseStack.translate(
                        seat.x - pEntity.position().x,
                        seat.y - pEntity.position().y,
                        seat.z - pEntity.position().z
                );

                LevelRenderer.renderLineBox(
                        poseStack,
                        buffer.getBuffer(RenderType.lines()),
                        new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5),
                        1, 0, 0, 1
                );
                poseStack.popPose();
            }
        }
    }

    private void renderPart(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, float partialTicks, int packedLight, ModelPart part, ModelPart armorPart, int index, float height, float width) {
        poseStack.pushPose();

        boolean simulated = pEntity.arePartsInitialized();

        Vec3 relative;
        if (simulated) {
            Vec3 pos = pEntity.getPartPos(index);
            Vec3 oldPos = pEntity.getOldPartPos(index);
            Vec3 finalPos = oldPos.lerp(pos, partialTicks);
            relative = finalPos.subtract(pEntity.getPosition(partialTicks));
        } else {
            relative = HullbackEntity.getRestPartOffset(index);
        }

        part.resetPose();
        part.setPos(0,0,0);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(relative.x, -relative.y, -relative.z);

        Quaternionf rotation = new Quaternionf();

        if(simulated) {
            float yRot = pEntity.getPartYRot(index);
            float xRot = pEntity.getPartXRot(index);
            float oldYRot = pEntity.getOldPartYRot(index);
            float oldXRot = pEntity.getOldPartXRot(index);

            float deltaYRot = Mth.wrapDegrees(yRot - oldYRot);
            float interpYRot = oldYRot + deltaYRot * partialTicks;

            float deltaXRot = Mth.wrapDegrees(xRot - oldXRot);
            float interpXRot = oldXRot + deltaXRot * partialTicks;

            rotation.rotationYXZ(
                    interpYRot * Mth.DEG_TO_RAD,
                    -interpXRot * Mth.DEG_TO_RAD,
                    0
            );
        }


        poseStack.mulPose(rotation);
        poseStack.translate(0, -height/2, -width/2);


        boolean flag = pEntity.hurtTime > 0;

        if (armorPart!=null && pEntity.getArmorProgress() > 0){
            poseStack.pushPose();
            poseStack.translate(0, -1.5f, 0);
            poseStack.scale(1.005f,1.005f,1.005f );
            float progress = 1 - pEntity.getArmorProgress();

            if (index == 2){
                renderFixedNameTag(pEntity, poseStack, buffer, packedLight);
            }

            // Per-material model override (datapack-driven). Falls back to default armorPart
            // when no override is defined or the override has no geometry for this index.
            String material = getMaterialName(pEntity);
            HullbackArmorModel<HullbackEntity> materialModel = getArmorModelForMaterial(material);
            ModelPart effectivePart = armorPart;
            if (materialModel != null && materialModel != armorModel) {
                switch (index) {
                    case 0: effectivePart = materialModel.getHead(); break;
                    case 2: effectivePart = materialModel.getBody(); break;
                    case 4: effectivePart = materialModel.getFluke(); break;
                }
                if (effectivePart.isEmpty()) effectivePart = armorPart;
            }

            if (Config.armorProgress) {
                if (progress == 0) {
                    effectivePart.render(
                            poseStack,
                            buffer.getBuffer(RenderType.entityCutout(getArmor(pEntity))),
                            packedLight,
                            OverlayTexture.pack(0.0F, flag)
                    );

                } else if (com.fruityspikes.whaleborne.client.compat.ShaderCompatLib.isShaderModLoaded()) {
                    // Oculus path: collapse vanilla dragonExplosionAlpha + entityDecal into one
                    // pass, thresholding mask alpha against progress*255 as the vanilla shader does.
                    ResourceLocation damagedTexture = HullbackArmorTextureManager.getOrCreateDamagedTexture(
                            pEntity, getArmor(pEntity), pEntity.getArmor().getItem(), progress
                    );
                    effectivePart.render(
                            poseStack,
                            buffer.getBuffer(RenderType.entityCutoutNoCull(damagedTexture)),
                            packedLight,
                            OverlayTexture.pack(0.0F, flag)
                    );

                } else {
                    effectivePart.render(
                            poseStack,
                            buffer.getBuffer(RenderType.dragonExplosionAlpha(ARMOR_PROGRESS)),
                            packedLight,
                            OverlayTexture.pack(0.0F, flag),
                            1, 1, 1, progress
                    );

                    effectivePart.render(
                            poseStack,
                            buffer.getBuffer(RenderType.entityDecal(getArmor(pEntity))),
                            packedLight,
                            OverlayTexture.pack(0.0F, flag)
                    );
                }
            } else {
                effectivePart.render(
                        poseStack,
                        buffer.getBuffer(RenderType.entityCutoutNoCull(getArmor(pEntity))),
                        packedLight,
                        OverlayTexture.pack(0.0F, flag),
                        1, 1, 1, 1
                );
                effectivePart.render(
                        poseStack,
                        buffer.getBuffer(RenderType.entityTranslucent(ARMOR_PROGRESS)),
                        packedLight,
                        OverlayTexture.pack(0.0F, flag),
                        1, pEntity.getArmorProgress(), pEntity.getArmorProgress(), progress
                );
            }

            poseStack.popPose();
        }

        ItemStack crown = pEntity.getCrown();

        if(index == 0 && !crown.isEmpty()){
            poseStack.pushPose();
            poseStack.translate(0,-4.07, -4);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));

            if (crown.is(Tags.Items.HEADS)) {
                poseStack.pushPose();
                poseStack.translate(0,0,0.23);
                if (isSpecialPlayerHead(crown)) {
                    poseStack.scale(2.5F, 2.5F, 2.5F);
                }
                Minecraft.getInstance().getItemRenderer().renderStatic(crown, ItemDisplayContext.FIXED, packedLight, OverlayTexture.pack(0.0F, flag), poseStack, buffer, pEntity.level(), 0);
                poseStack.popPose();
            } else
                Minecraft.getInstance().getItemRenderer().renderStatic(crown, ItemDisplayContext.HEAD, packedLight, OverlayTexture.pack(0.0F, flag), poseStack, buffer, pEntity.level(), 0);
            poseStack.popPose();
        }

        part.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(pEntity))), packedLight, OverlayTexture.pack(0.0F, flag));

        if(pEntity.isSaddled()) {
            poseStack.pushPose();
            poseStack.scale(1.009f, 1.009f, 1.009f);
            poseStack.translate(0, -0.01f, -0.01f);
            part.render(
                    poseStack,
                    buffer.getBuffer(RenderType.entityCutoutNoCull(SADDLE_TEXTURE)),
                    packedLight,
                    OverlayTexture.pack(0.0F, flag)
            );
            poseStack.popPose();
        }

        if(index == 4)
            poseStack.translate(-0.5f, 0, 0);
        if(index == 3)
            poseStack.translate(0.25f, 0, 0);
        poseStack.translate(-width/2, height/2, 0);

        renderBottomDirt(poseStack, buffer, packedLight, pEntity, index);
        if(index==0 || index==2){
            poseStack.translate(0, -height, 0);
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.translate(-width, 0, 0);
            rendertTopDirt(poseStack, buffer, packedLight, pEntity, index);
        }
        poseStack.popPose();
    }

    /**
     * Resolves a per-material armor model. The lookup key respects the JSON {@code armor_model}
     * override declared in the hull config; absent that, falls back to the material itself.
     */
    private HullbackArmorModel<HullbackEntity> getArmorModelForMaterial(String material) {
        if (material == null) return armorModel;
        // Without the data-driven armor_model override, every material is keyed by its own id.
        return materialArmorModels.computeIfAbsent(material, mat -> {
            LayerDefinition layerDef = ArmorModelLoader.loadMergedArmor(mat);
            if (layerDef != null) {
                return new HullbackArmorModel<>(layerDef.bakeRoot());
            }
            return null;
        });
    }

    /** Material key for armor lookup: vanilla items return path only, modded items use {@code namespace/path}. */
    private String getMaterialName(HullbackEntity entity) {
        if (entity.getArmorProgress() <= 0) return null;
        ItemStack armor = entity.getArmor();
        if (armor.isEmpty()) return null;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(armor.getItem());
        if (itemId == null) return null;
        if (itemId.getNamespace().equals("minecraft")) {
            return itemId.getPath();
        }
        return itemId.getNamespace() + "/" + itemId.getPath();
    }

    private static boolean isSpecialPlayerHead(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        if (tag.contains("SkullOwner", Tag.TAG_COMPOUND)) {
            String name = tag.getCompound("SkullOwner").getString("Name");
            return SPECIAL_HEAD_NAMES.contains(name);
        }
        if (tag.contains("SkullOwner", Tag.TAG_STRING)) {
            return SPECIAL_HEAD_NAMES.contains(tag.getString("SkullOwner"));
        }
        return false;
    }

    private void renderFixedNameTag(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Component name = pEntity.getDisplayName();
        if (name.getString().equals("entity.whaleborne.hullback")) return;
        if (pEntity.isAlive() && !name.getString().isEmpty()) {
            Font font = this.getFont();

            poseStack.pushPose();

            poseStack.translate(0, -1.0, 6.79);
            poseStack.mulPose(Axis.YN.rotationDegrees(180));

            float scale = 0.08f;
            poseStack.scale(scale, scale, scale);

            List<FormattedCharSequence> lines = font.split(name, 8 * 6);

            float totalHeight = lines.size() * font.lineHeight;

            for (int i = 0; i < lines.size(); i++) {
                FormattedCharSequence line = lines.get(i);
                float xOffset = -font.width(line) / 2f;
                float yOffset = i * font.lineHeight - totalHeight / 2f;

                font.drawInBatch(
                        line,
                        xOffset, yOffset,
                        0xFFFFFF,
                        false,
                        poseStack.last().pose(),
                        buffer,
                        Font.DisplayMode.NORMAL,
                        0,
                        packedLight
                );
            }

            poseStack.popPose();
        }
    }

    private void rendertTopDirt(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HullbackEntity parent, int index) {
        boolean flag = parent.hurtTime > 0;
        BlockState[][] array = parent.getDirtArray(index, false);

        if(array!=null){
            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[x].length; y++) {
                    poseStack.translate(y, 0, x);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(array[x][y], poseStack, buffer, packedLight, OverlayTexture.pack(0.0F, flag));
                    poseStack.translate(-y, 0, -x);
                }
            }
        }
    }

    public void renderBottomDirt(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HullbackEntity parent, int index) {
        boolean flag = parent.hurtTime > 0;
        BlockState[][] array = parent.getDirtArray(index, true);

        if(array!=null){
            for (int x = 0; x < array.length; x++) {
                for (int y = 0; y < array[x].length; y++) {
                    poseStack.translate(y, 0, x);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(array[x][y], poseStack, buffer, packedLight, OverlayTexture.pack(0.0F, flag));
                    if(array[x][y].is(Blocks.TALL_SEAGRASS)) {
                        poseStack.translate(0, 1, 0);
                        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(array[x][y].setValue(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER), poseStack, buffer, packedLight, OverlayTexture.pack(0.0F, flag));
                        poseStack.translate(0, -1, 0);
                    }
                    if(array[x][y].is(Blocks.KELP_PLANT)) {
                        poseStack.translate(0, 1, 0);
                        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.KELP.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.pack(0.0F, flag));
                        poseStack.translate(0, -1, 0);
                    }
                    poseStack.translate(-y, 0, -x);
                }
            }
        }
    }
    @Override
    public ResourceLocation getTextureLocation(HullbackEntity entity) {
//        if(entity.getDisplayName().getString().equals("Mobius"))
//            return MOBIUS_TEXTURE;
        if(entity.getDisplayName().getString().equals("Steen"))
            return STEEN_TEXTURE;
        return MOB_TEXTURE;
    }

    @Override
    public boolean shouldRender(HullbackEntity livingEntity, Frustum frustum, double v, double v1, double v2) {
        if (this.shouldRenderAll(livingEntity, frustum, v, v1, v2)) {
            return true;
        } else {
            Entity entity = livingEntity.getLeashHolder();
            return entity != null && frustum.isVisible(entity.getBoundingBoxForCulling());
        }
    }

    public boolean shouldRenderAll(HullbackEntity hullbackEntity, Frustum frustum, double v, double v1, double v2) {
        if (!hullbackEntity.shouldRender(v, v1, v2)) {
            return false;
        } else if (hullbackEntity.noCulling) {
            return true;
        } else {
            ArrayList<AABB> list = new ArrayList<>(List.of());
            for (HullbackPartEntity entity : hullbackEntity.getSubEntities()) {
                list.add(entity.getBoundingBoxForCulling().inflate(0.5F));
            }
            return list.stream().anyMatch(frustum::isVisible);
        }
    }
}