package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.AnchorHeadModel;
import com.fruityspikes.whaleborne.client.models.AnchorModel;
import com.fruityspikes.whaleborne.client.models.CannonModel;
import com.fruityspikes.whaleborne.server.entities.AnchorEntity;
import com.fruityspikes.whaleborne.server.entities.AnchorHeadEntity;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AnchorRenderer extends WhaleWidgetRenderer {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/anchor.png");
    public static final ResourceLocation CHAIN = new ResourceLocation(Whaleborne.MODID, "textures/entity/chain.png");
    public static final ResourceLocation ANCHOR_HEAD_TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/anchor_head.png");
    private final AnchorHeadModel anchorHeadModel;

    public AnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AnchorModel<>(context.bakeLayer(WBEntityModelLayers.ANCHOR));
        this.anchorHeadModel = new AnchorHeadModel<>(context.bakeLayer(WBEntityModelLayers.ANCHOR_HEAD));
    }

    @Override
    public void render(WhaleWidgetEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        if (entity instanceof AnchorEntity anchor){

            if(!anchor.isClosed() && anchor.getHeadPos() != null){
                renderChain(anchor, poseStack, partialTick, buffer, packedLight, true);
                renderChain(anchor, poseStack, partialTick, buffer, packedLight, false);
                renderAnchorHead(anchor, anchor.getHeadPos(), poseStack, partialTick, buffer, packedLight);
            }
        }
    }

    private void renderAnchorHead(AnchorEntity anchor, BlockPos headPos, PoseStack poseStack, float partialTick, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        Vec3 anchorPos = anchor.position();
        Vec3 headVec = Vec3.atCenterOf(headPos).subtract(anchorPos);

        Vec3 tip = new Vec3(anchor.position().subtract(anchor.getHeadPos().getX(), anchor.getHeadPos().getY(), anchor.getHeadPos().getZ()).toVector3f()).add(0, -2,0).multiply(-1, -1, -1);;

        poseStack.translate(tip.x, tip.y - 0.2, tip.z);

        poseStack.mulPose(Axis.XP.rotationDegrees(180f));

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ANCHOR_HEAD_TEXTURE));
        anchorHeadModel.renderToBuffer(
                poseStack,
                vertexConsumer,
                (int) (packedLight * tip.y),
                OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F
        );

        poseStack.popPose();
    }

    // stolen from specie
    public void renderChain(AnchorEntity anchor, PoseStack poseStack, float partialTick, MultiBufferSource buffer, int packedLight, boolean left){
        poseStack.pushPose();

        PoseStack.Pose pose = poseStack.last();

        VertexConsumer builder = buffer.getBuffer(RenderType.entityCutoutNoCull(CHAIN));
        Vec3 base = Vec3.ZERO.add(0, 1,0);

        Vec3 entityPos = anchor.position();
        Vec3 tip = new Vec3(entityPos.subtract(anchor.getHeadPos().getX(), anchor.getHeadPos().getY(), anchor.getHeadPos().getZ()).toVector3f()).add(0, -2,0).multiply(-1, -1, -1);;

        Vec3 direction = tip.subtract(base);

        Vec3 dirNorm = direction.normalize();
        Vec3 up = Math.abs(dirNorm.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = dirNorm.cross(up).normalize().scale(0.3);
        Vec3 side = dirNorm.cross(right).normalize().scale(0.3);
        Vec3 dir = left ? right : side;
        float length = (float) direction.length();

        builder.vertex(pose.pose(), (float) (base.x + dir.x), (float) (base.y + dir.y), (float) (base.z + dir.z))
                .color(1F, 1F, 1F, 1F)
                .uv(left ? 0 : 0.5f, 0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();

        builder.vertex(pose.pose(), (float) (base.x - dir.x), (float) (base.y - dir.y), (float) (base.z - dir.z))
                .color(1F, 1F, 1F, 1F)
                .uv(left ? 0.5f : 1, 0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();

        builder.vertex(pose.pose(), (float) (tip.x - dir.x), (float) (tip.y - dir.y), (float) (tip.z - dir.z))
                .color(1F, 1F, 1F, 1F)
                .uv(left ? 0.5f : 1, length)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2((int) (packedLight * tip.y))
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();

        builder.vertex(pose.pose(), (float) (tip.x + dir.x), (float) (tip.y + dir.y), (float) (tip.z + dir.z))
                .color(1F, 1F, 1F, 1F)
                .uv(left ? 0 : 0.5f, length)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2((int) (packedLight * tip.y))
                .normal(pose.normal(), 0, 1, 0)
                .endVertex();

        poseStack.popPose();
    }
    @Override
    public ResourceLocation getTextureLocation(WhaleWidgetEntity whaleWidgetEntity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(Entity livingEntity, Frustum camera, double camX, double camY, double camZ) {
        if(livingEntity instanceof AnchorEntity anchor) {
            if (!anchor.isClosed() && anchor.getHeadPos()!=null) {
                return true;
            }
        }
        return super.shouldRender(livingEntity, camera, camX, camY, camZ);
    }
}
