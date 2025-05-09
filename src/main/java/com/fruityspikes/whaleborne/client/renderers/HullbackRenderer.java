package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.HullbackModel;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HullbackRenderer<T extends HullbackEntity> extends MobRenderer<HullbackEntity, HullbackModel<HullbackEntity>> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/hullback.png");

    public HullbackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HullbackModel<>(ctx.bakeLayer(WBEntityModelLayers.HULLBACK)), 5F);
    }

    @Override
    public void render(HullbackEntity pEntity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(pEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

//        pEntity.nose.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, this.model.getHead());
//        pEntity.body.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, this.model.getBody());
//        pEntity.tail.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, this.model.getTail());
//        pEntity.fluke.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, this.model.getFluke());


        pEntity.head.renderDirt(poseStack, buffer, packedLight);
        pEntity.body.renderDirt(poseStack, buffer, packedLight);
        pEntity.tail.renderDirt(poseStack, buffer, packedLight);
        pEntity.fluke.renderDirt(poseStack, buffer, packedLight);

//        poseStack.translate(pEntity.head.getX() - pEntity.getX(),pEntity.head.getY() - pEntity.getY(),pEntity.head.getZ() - pEntity.getZ() );
//        LevelRenderer.renderLineBox(
//                poseStack,
//                buffer.getBuffer(RenderType.lines()),
//                new AABB(-0.1, -0.1, -0.1, 0.1, 10, 0.1),
//                0, 1, 0, 1
//        );

        //poseStack.mulPose(Axis.YP.rotationDegrees(-pEntity.head.getYRot()));
        //poseStack.mulPose(Axis.XP.rotationDegrees(-pEntity.head.getXRot()));

//        poseStack.translate(-2, 5,0);
//        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.KELP.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
//        poseStack.translate(1, 0,4);
//        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.KELP.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
//        poseStack.translate(1, 0,-3);
//        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.KELP.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
//        poseStack.translate(1, 0,3);
//        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.KELP.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
//        poseStack.popPose();

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