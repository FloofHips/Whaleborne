package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.HullbackModel;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class HullbackRenderer<T extends HullbackEntity> extends MobRenderer<HullbackEntity, HullbackModel<HullbackEntity>> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback.png");

    public HullbackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HullbackModel<>(ctx.bakeLayer(WBEntityModelLayers.HULLBACK)), 5F);
    }

    @Override
    public void render(HullbackEntity pEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(pEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        Vec3 entityPos = pEntity.position();

        Vec3 flukeOffset = new Vec3(0, 1, 0);
        Vec3 tailOffset = new Vec3(0, 2, 0);

        renderModelPart(poseStack, buffer, packedLight, entityPos, this.model.getFluke(), partialTicks, pEntity, pEntity.fluke, flukeOffset);
        renderModelPart(poseStack, buffer, packedLight, entityPos, this.model.getTail(), partialTicks, pEntity, pEntity.tail, tailOffset);

    }

    private void renderModelPart(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Vec3 entityPos, ModelPart part, float partialTicks, HullbackEntity pEntity, HullbackPartEntity entityPart, Vec3 offset) {
        poseStack.pushPose();

        part.resetPose();
        part.setPos(0,0,0);

        Vec3 renderPos = entityPart.position();

        renderPos = renderPos.add(entityPart.getForward().scale(offset.y)).add(0,1,0);//);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        //poseStack.translate(leg.getRestingOffset().x / -4, 0, leg.getRestingOffset().z / 4);
        poseStack.translate(renderPos.x - entityPos.x, - (renderPos.y - entityPos.y), -(renderPos.z - entityPos.z));

        //double angle = leg.getCurrentPos().distanceTo(leg.getTargetPos());

        poseStack.mulPose(Axis.YP.rotationDegrees((float) (pEntity.yBodyRot)));

        part.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(HullbackEntity entity) {
        return TEXTURE;
    }
}