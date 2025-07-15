package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.AnchorHeadModel;
import com.fruityspikes.whaleborne.client.models.SailModel;
import com.fruityspikes.whaleborne.server.entities.AnchorHeadEntity;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AnchorHeadRenderer<T extends AnchorHeadEntity> extends EntityRenderer<AnchorHeadEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/anchor_head.png");
    protected EntityModel model;
    public AnchorHeadRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AnchorHeadModel(context.bakeLayer(WBEntityModelLayers.ANCHOR_HEAD));
    }

    @Override
    public void render(AnchorHeadEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));

        poseStack.mulPose(Axis.XN.rotationDegrees(Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));
        model.setupAnim(entity, partialTick, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        getModel().renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AnchorHeadEntity anchorHeadEntity) {
        return TEXTURE;
    }

    public Model getModel() {
        return model;
    }
}
