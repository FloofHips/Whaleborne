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
import net.minecraft.util.Mth;
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

//        pEntity.nose.render(poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getHead());
//        pEntity.body.render(poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getBody());
//        pEntity.tail.render(poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getTail());
//        pEntity.fluke.render(poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getFluke());
//
        renderPart(pEntity, poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getHead(), 0, 5.0F, 5.0F);
        renderPart(pEntity, poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getBody(), 2, 5.0F, 5.0F);

        renderPart(pEntity, poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getTail(), 3, 2.5F, 2.5F);
        renderPart(pEntity, poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getFluke(), 4, 0.6F, 4.0F);
    }

    private void renderPart(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, float partialTicks, ResourceLocation texture, int packedLight, ModelPart part, int index, float height, float width) {
        poseStack.pushPose();

        Vec3 pos = pEntity.getPartPos(index);
        float yRot = pEntity.getPartYRot(index);
        float xRot = pEntity.getPartXRot(index);

        Vec3 oldPos = pEntity.getOldPartPos(index);
        float oldYRot = pEntity.getOldPartYRot(index);
        float oldXRot = pEntity.getOldPartXRot(index);

        Vec3 finalPos = oldPos.lerp(pos, partialTicks);
        float finalYRot = Mth.lerp(partialTicks, oldYRot, yRot);
        float finalXRot = Mth.lerp(partialTicks, oldXRot, xRot);

        part.resetPose();
        part.setPos(0,0,0);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(finalPos.x - pEntity.getX(), - (finalPos.y - pEntity.getY()), -(finalPos.z - pEntity.getZ()));

        if (index == 4){
            System.out.println("Ah");
            System.out.println(yRot);
            System.out.println(oldYRot);
            System.out.println(finalYRot);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(finalYRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-finalXRot));
        poseStack.translate(0, -height/2, -width/2);

        boolean flag = pEntity.hurtTime > 0;
        part.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, OverlayTexture.pack(0.0F, flag));
        poseStack.translate(0, height/2, width/2);
        renderDirt(poseStack, buffer, packedLight, pEntity);
        poseStack.translate(0, -height-1, 0);
        renderDirt(poseStack, buffer, packedLight, pEntity);
        poseStack.popPose();
    }
    public void renderDirt(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HullbackEntity parent) {
        if(Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    buffer.getBuffer(RenderType.lines()),
                    new AABB(-0.1, -0.1, -0.1, 0.1, 10, 0.1),
                    1, 0, 0, 1
            );
        }

        boolean flag = parent.hurtTime > 0;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.DARK_OAK_PLANKS.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.pack(0.0F, flag));
    }
    @Override
    public ResourceLocation getTextureLocation(HullbackEntity entity) {
        return TEXTURE;
    }
}