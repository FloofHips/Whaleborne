package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Config;
import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.HullbackArmorModel;
import com.fruityspikes.whaleborne.client.models.HullbackModel;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.fruityspikes.whaleborne.server.registries.WBTagRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
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
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HullbackRenderer<T extends HullbackEntity> extends MobRenderer<HullbackEntity, HullbackModel<HullbackEntity>> {
    public static boolean isRenderingInHealthbarsGui = false;

    private static final java.util.Set<String> SPECIAL_HEAD_NAMES = java.util.Set.of("SH8RK");

    // Configurable offset and scale overrides for GUI overlay
    public static float GUI_HEAD_X = 0.0f;
    public static float GUI_HEAD_Y = 0.0f;
    public static float GUI_HEAD_Z = 1.0f;
    public static float GUI_HEAD_SCALE = 0.5f;
    public static final ResourceLocation MOB_TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/hullback.png");
    public static final ResourceLocation STEEN_TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/steen.png");
    public static final ResourceLocation SADDLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/hullback_saddle.png");
    public static final ResourceLocation ARMOR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/armor/hullback_dark_oak_planks_armor.png");
    public static final ResourceLocation ARMOR_PROGRESS = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/hullback_armor_progress.png");
    private final HullbackArmorModel<HullbackEntity> armorModel;

    public HullbackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HullbackModel<>(ctx.bakeLayer(WBEntityModelLayers.HULLBACK)), 5F);
        this.armorModel = new HullbackArmorModel<>(ctx.bakeLayer(WBEntityModelLayers.HULLBACK_ARMOR));
    }

    public ResourceLocation getArmor(HullbackEntity pEntity) {
        if (pEntity.getArmorProgress() > 0) {
            ItemStack armor = pEntity.getArmor();
            if (armor.is(WBTagRegistry.HULLBACK_EQUIPPABLE)) {
                ResourceLocation armor_tex = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/armor/hullback_" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(armor.getItem()).getPath() + "_armor.png");
                Optional<Resource> tex = Minecraft.getInstance().getResourceManager().getResource(armor_tex);
                return tex.isPresent() ? armor_tex : ARMOR_TEXTURE;
            }
        }
        return ARMOR_TEXTURE;
    }

    @Override
    public void render(HullbackEntity pEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (isRenderingInHealthbarsGui) {
            poseStack.pushPose();
            
            // We use the 'O' (Old) variables here because InventoryScreen heavily overrides 
            // the main rotation variables (yBodyRot, xRot, etc) with a static fake mouse tracking offset. 
            // Fuzs Healthbars passes fixed mouse coordinates, so the standard variables are totally frozen!
            float trueBodyRot = pEntity.yBodyRotO;
            float trueHeadRot = pEntity.yHeadRotO; 
            float truePitch = pEntity.xRotO;
            float headYawOffset = trueHeadRot - trueBodyRot;

            ModelPart head = this.model.getHead();
            ModelPart armorHead = this.armorModel.getHead();

            // Zero out model offsets identically to world rendering so dirt/armor overlays perfectly align
            head.resetPose();
            head.setPos(0, 0, 0);

            this.model.setupAnim(pEntity, 0, 0, pEntity.tickCount + partialTicks, headYawOffset, truePitch);
            if (armorHead != null) {
                this.armorModel.setupAnim(pEntity, 0, 0, pEntity.tickCount + partialTicks, headYawOffset, truePitch);
            }

            // Frame centering on the GUI (adjusted slightly to frame the new 0,0,0 origin beautifully)
            poseStack.translate(GUI_HEAD_X, GUI_HEAD_Y, GUI_HEAD_Z);
            poseStack.scale(GUI_HEAD_SCALE, GUI_HEAD_SCALE, GUI_HEAD_SCALE);
            
            // Rotate the head 180 degrees on X to fix the inverted rendering of Minecraft models
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            
            // Shift pivot to the face (Z is depth, face is at ~2.5 local)
            poseStack.translate(0, 0, 2.5f);
            
            // Apply the actual WORLD yaw sequence to spin freely
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - trueBodyRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(headYawOffset));
            poseStack.mulPose(Axis.XP.rotationDegrees(truePitch));

            // Shift back so the face rests at the frame pivot
            poseStack.translate(0, 0, -2.5f);

            // Now, EXACTLY like renderPart: apply the logical bounding block center offset
            float height = 5.0f;
            float width = 5.0f;
            poseStack.translate(0, -height/2, -width/2);

            // 1. Render Armor
            renderArmor(pEntity, poseStack, buffer, packedLight, false, armorHead, 0);

            // 2. Render Crown
            renderCrown(pEntity, poseStack, buffer, packedLight, false, 0);

            // 3. Render Head
            VertexConsumer vertexConsumer = buffer.getBuffer(this.model.renderType(this.getTextureLocation(pEntity)));
            head.render(poseStack, vertexConsumer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

            // 4. Render Dirt (index 0 implies width 5, height 5)
            poseStack.pushPose();
            poseStack.translate(-width/2, height/2, 0);
            renderBottomDirt(poseStack, buffer, packedLight, pEntity, 0);
            poseStack.translate(0, -height, 0);
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.translate(-width, 0, 0);
            renderTopDirt(poseStack, buffer, packedLight, pEntity, 0);
            poseStack.popPose();

            // Restore the model parts so the actual world render doesn't break
            head.resetPose();
            if (armorHead != null) {
                armorHead.resetPose();
            }

            poseStack.popPose();
            return;
        }

        super.render(pEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getHead(), this.armorModel.getHead(), 0, 5.0F, 5.0F);
        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getBody(), this.armorModel.getBody(), 2, 5.0F, 5.0F);

        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getTail(), null, 3, 2.5F, 2.5F);
        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getFluke(), this.armorModel.getFluke(), 4, 0.6F, 4.0F);

        if(entityRenderDispatcher.shouldRenderHitBoxes())
            renderDebug(pEntity, poseStack, buffer, partialTicks);
    }

    private void renderDebug(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, float partialTicks) {
        if(pEntity.hullbackSeatManager.seats[0]!=null){
            for ( Vec3 seat : pEntity.hullbackSeatManager.seats ) {
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

        Vec3 finalPos = Vec3.ZERO;

        if(pEntity.getOldPartPos(index) != null){
            Vec3 pos = pEntity.getPartPos(index);
            Vec3 oldPos = pEntity.getOldPartPos(index);
            finalPos = oldPos.lerp(pos, partialTicks);
        }

        part.resetPose();
        part.setPos(0,0,0);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(finalPos.x - pEntity.getPosition(partialTicks).x, - (finalPos.y - pEntity.getPosition(partialTicks).y), -(finalPos.z - pEntity.getPosition(partialTicks).z));

        Quaternionf rotation = new Quaternionf();

        if(pEntity.getOldPartPos(index) != null) {
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

        renderArmor(pEntity, poseStack, buffer, packedLight, flag, armorPart, index);

        renderCrown(pEntity, poseStack, buffer, packedLight, flag, index);

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
            renderTopDirt(poseStack, buffer, packedLight, pEntity, index);
        }
        poseStack.popPose();
    }

    private void renderCrown(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean flag, int index) {
        ItemStack crown = pEntity.getCrown();
        if (index == 0 && !crown.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0, -4.07, -4);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));

            if (crown.is(ItemTags.SKULLS)) {
                poseStack.pushPose();
                poseStack.translate(0, 0, 0.23);
                if (isSpecialPlayerHead(crown)) {
                    poseStack.scale(2.5F, 2.5F, 2.5F);
                }
                Minecraft.getInstance().getItemRenderer().renderStatic(crown, ItemDisplayContext.FIXED, packedLight, OverlayTexture.pack(0.0F, flag), poseStack, buffer, pEntity.level(), 0);
                poseStack.popPose();
            } else {
                Minecraft.getInstance().getItemRenderer().renderStatic(crown, ItemDisplayContext.HEAD, packedLight, OverlayTexture.pack(0.0F, flag), poseStack, buffer, pEntity.level(), 0);
            }
            poseStack.popPose();
        }
    }

    private static boolean isSpecialPlayerHead(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) return false;
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        return profile != null && profile.name().isPresent() && SPECIAL_HEAD_NAMES.contains(profile.name().get());
    }

    private void renderArmor(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean flag, ModelPart armorPart, int index) {
        if (armorPart != null && pEntity.getArmorProgress() > 0) {
            poseStack.pushPose();
            poseStack.translate(0, -1.5f, 0);
            poseStack.scale(1.005f, 1.005f, 1.005f);
            float progress = 1 - pEntity.getArmorProgress();

            if (index == 2) {
                renderFixedNameTag(pEntity, poseStack, buffer, packedLight);
            }

            if (Config.armorProgress) {
                if (progress == 0) {
                    armorPart.render(
                            poseStack,
                            buffer.getBuffer(RenderType.entityCutout(getArmor(pEntity))),
                            packedLight,
                            OverlayTexture.pack(0.0F, flag)
                    );
                } else if (!com.fruityspikes.whaleborne.client.compat.ShaderCompatLib.isShaderModLoaded()) {
                    armorPart.render(
                            poseStack,
                            buffer.getBuffer(RenderType.dragonExplosionAlpha(ARMOR_PROGRESS)),
                            packedLight,
                            OverlayTexture.pack(0.0F, flag),
                            (int)(progress * 255) << 24 | 0xFFFFFF
                    );
                    armorPart.render(
                            poseStack,
                            buffer.getBuffer(RenderType.entityDecal(getArmor(pEntity))),
                            packedLight,
                            OverlayTexture.pack(0.0F, flag)
                    );
                } else {
                    ResourceLocation damagedTexture = HullbackArmorTextureManager.getOrCreateDamagedTexture(
                            pEntity, getArmor(pEntity), pEntity.getArmor().getItem(), progress
                    );
                    armorPart.render(
                            poseStack,
                            buffer.getBuffer(RenderType.entityCutoutNoCull(damagedTexture)),
                            packedLight,
                            OverlayTexture.pack(0.0F, flag)
                    );
                }
            } else {
                armorPart.render(
                        poseStack,
                        buffer.getBuffer(RenderType.entityCutoutNoCull(getArmor(pEntity))),
                        packedLight,
                        OverlayTexture.pack(0.0F, flag),
                        -1
                );
                armorPart.render(
                        poseStack,
                        buffer.getBuffer(RenderType.entityTranslucent(ARMOR_PROGRESS)),
                        packedLight,
                        OverlayTexture.pack(0.0F, flag),
                        (int)(progress * 255) << 24 | ((int)(pEntity.getArmorProgress() * 255) << 16) | ((int)(pEntity.getArmorProgress() * 255) << 8) | 255
                );
            }

            poseStack.popPose();
        }
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

    private void renderTopDirt(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HullbackEntity parent, int index) {
        boolean flag = parent.hurtTime > 0;
        BlockState[][] array = parent.getWhaleDirt().getDirtArray(index, false);

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
        BlockState[][] array = parent.getWhaleDirt().getDirtArray(index, true);

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
