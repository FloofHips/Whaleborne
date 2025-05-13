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
import org.joml.Quaternionf;

public class HullbackRenderer<T extends HullbackEntity> extends MobRenderer<HullbackEntity, HullbackModel<HullbackEntity>> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/texture.png");

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

        renderPart(pEntity, poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getHead(), 0, 5.0F, 5.0F);
        renderPart(pEntity, poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getBody(), 2, 5.0F, 5.0F);

        renderPart(pEntity, poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getTail(), 3, 2.5F, 2.5F);
        renderPart(pEntity, poseStack, buffer, partialTicks, TEXTURE, packedLight, this.model.getFluke(), 4, 0.6F, 4.0F);
    }

    private void renderPart(HullbackEntity pEntity, PoseStack poseStack, MultiBufferSource buffer, float partialTicks, ResourceLocation texture, int packedLight, ModelPart part, int index, float height, float width) {
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