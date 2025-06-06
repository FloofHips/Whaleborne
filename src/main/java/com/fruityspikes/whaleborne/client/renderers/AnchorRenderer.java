package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
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
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AnchorRenderer extends WhaleWidgetRenderer {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/anchor.png");
    public static final ResourceLocation CHAIN = new ResourceLocation(Whaleborne.MODID, "textures/entity/chain.png");
    public AnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AnchorModel<>(context.bakeLayer(WBEntityModelLayers.ANCHOR));
    }

    @Override
    public void render(WhaleWidgetEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        if (entity instanceof AnchorEntity anchor){

            if(!anchor.isClosed() && anchor.getHeadPos() != null){
                poseStack.pushPose();

                PoseStack.Pose pose = poseStack.last();

                VertexConsumer builder = buffer.getBuffer(RenderType.entityCutoutNoCull(CHAIN));
                Vec3 base = Vec3.ZERO;

                Vec3 entityPos = anchor.position();
                Vec3 tip = new Vec3(entityPos.subtract(anchor.getHeadPos().x, anchor.getHeadPos().y, anchor.getHeadPos().z).toVector3f()).multiply(-1, -1, -1);;

                Vec3 direction = tip.subtract(base);

                Vec3 dirNorm = direction.normalize();
                Vec3 up = Math.abs(dirNorm.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                Vec3 right = dirNorm.cross(up).normalize().scale(0.3);
                Vec3 side = dirNorm.cross(right).normalize().scale(0.3);
                float length = (float) direction.length();

                builder.vertex(pose.pose(), (float) (base.x + side.x), (float) (base.y + side.y), (float) (base.z + side.z))
                        .color(1F, 1F, 1F, 1F)
                        .uv(0F, 0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(pose.normal(), 0, 1, 0)
                        .endVertex();

                builder.vertex(pose.pose(), (float) (base.x - side.x), (float) (base.y - side.y), (float) (base.z - side.z))
                        .color(1F, 1F, 1F, 1F)
                        .uv(1F, 0F)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(pose.normal(), 0, 1, 0)
                        .endVertex();

                builder.vertex(pose.pose(), (float) (tip.x - side.x), (float) (tip.y - side.y), (float) (tip.z - side.z))
                        .color(1F, 1F, 1F, 1F)
                        .uv(1F, length)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(pose.normal(), 0, 1, 0)
                        .endVertex();

                builder.vertex(pose.pose(), (float) (tip.x + side.x), (float) (tip.y + side.y), (float) (tip.z + side.z))
                        .color(1F, 1F, 1F, 1F)
                        .uv(0F, length)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(pose.normal(), 0, 1, 0)
                        .endVertex();

                poseStack.popPose();
            }

            boolean isClosed = anchor.isClosed();
            boolean isDown = anchor.isDown();
            Optional<UUID> anchorHead = null;
            if(anchor.anchorHead!=null)
                anchorHead = anchor.getAnchorHeadUUID();
            if (entity.isAlive()) {
                Font font = this.getFont();

                poseStack.pushPose();

                poseStack.translate(0, 2, 0);
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));
                poseStack.mulPose(Axis.YN.rotationDegrees(180));

                float scale = 0.03f;
                poseStack.scale(scale, scale, scale);
                String name = "Closed: " + isClosed + ", Down: " + isDown + ", Anchor: " + anchorHead;

                List<FormattedCharSequence> lines = font.split(FormattedText.of(name), 20 * 6);

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
    }

    @Override
    public ResourceLocation getTextureLocation(WhaleWidgetEntity whaleWidgetEntity) {
        return TEXTURE;
    }
}
