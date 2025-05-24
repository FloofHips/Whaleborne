package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.HullbackArmorModel;
import com.fruityspikes.whaleborne.client.models.HullbackModel;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class HullbackRenderer<T extends HullbackEntity> extends MobRenderer<HullbackEntity, HullbackModel<HullbackEntity>> {
    public static final ResourceLocation MOB_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback.png");
    public static final ResourceLocation SADDLE_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback_saddle.png");
    public static final ResourceLocation ARMOR_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback_dark_oak_armor.png");
    public static final ResourceLocation ARMOR_PROGRESS = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback_armor_progress.png");
    private final HullbackArmorModel<HullbackEntity> armorModel;

    public HullbackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HullbackModel<>(ctx.bakeLayer(WBEntityModelLayers.HULLBACK)), 5F);
        this.armorModel = new HullbackArmorModel<>(ctx.bakeLayer(WBEntityModelLayers.HULLBACK_ARMOR));
    }

    @Override
    public void render(HullbackEntity pEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(pEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getHead(), this.armorModel.getHead(), 0, 5.0F, 5.0F);
        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getBody(), this.armorModel.getBody(), 2, 5.0F, 5.0F);

        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getTail(), null, 3, 2.5F, 2.5F);
        renderPart(pEntity, poseStack, buffer, partialTicks, packedLight, this.model.getFluke(), this.armorModel.getFluke(), 4, 0.6F, 4.0F);

        renderDebug(pEntity, poseStack, buffer, partialTicks);
    }

    private void renderDebug(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, float partialTicks) {
        if(pEntity.seats[0]!=null){
            for ( Vec3 seat : pEntity.seats ) {
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

        if(armorPart!=null && pEntity.getArmorProgress() > 0){
            poseStack.pushPose();
            poseStack.translate(0, -1.5f, 0);
            poseStack.scale(1.005f,1.005f,1.005f );
            float progress = 1 - pEntity.getArmorProgress();

            armorPart.render(
                    poseStack,
                    buffer.getBuffer(RenderType.dragonExplosionAlpha(ARMOR_PROGRESS)),
                    packedLight,
                    OverlayTexture.pack(0.0F, flag),
                    1, 1, 1, pEntity.getMouthOpenProgress()
            );


            armorPart.render(
                    poseStack,
                    buffer.getBuffer(RenderType.entityDecal(ARMOR_TEXTURE)),
                    packedLight,
                    OverlayTexture.pack(0.0F, flag)
            );

            poseStack.popPose();
        }

        part.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(MOB_TEXTURE)), packedLight, OverlayTexture.pack(0.0F, flag));

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
        return MOB_TEXTURE;
    }
}