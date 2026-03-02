package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.SailModel;
import com.fruityspikes.whaleborne.server.entities.SailEntity;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SailRenderer<T extends SailEntity> extends WhaleWidgetRenderer<SailEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/sail.png");
    public static final ResourceLocation TARP_TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/tarp.png");
    private final SailModel<SailEntity> model;

    private Vec3 edge1 = new Vec3(0,0,0);
    private Vec3 edge2 = new Vec3(0,0.25,0);
    private Vec3 edge3 = new Vec3(0,0.5,0);
    private Vec3 edge4 = new Vec3(0,0.75,0);
    private Vec3 edge5 = new Vec3(0,1,0);
    public SailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.7F;
        this.model = new SailModel<>(context.bakeLayer(WBEntityModelLayers.SAIL));
    }

    public ResourceLocation getTextureLocation(WhaleWidgetEntity whaleWidgetEntity) {
        return TEXTURE;
    }

    @Override
    public void render(WhaleWidgetEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        float f = (float)entity.getHurtTime() - partialTick;
        float f1 = entity.getDamage() - partialTick;
        if (f1 < 0.0F) {
            f1 = 0.0F;
        }

        if (f > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(f) * f * f1 / 10.0F * (float)entity.getHurtDir()));
        }

        poseStack.mulPose(Axis.XN.rotationDegrees(Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));
        model.setupAnim((SailEntity) entity, partialTick, 0.0F, -0.1F, 0.0F, 0.0F);
        poseStack.pushPose();

        SailEntity sail = (SailEntity) entity;
        ItemStack item = sail.getBanner();
        
        List<Pair<Holder<BannerPattern>, DyeColor>> list = new ArrayList<>();
        if (!item.isEmpty() && item.getItem() instanceof BannerItem) {
            BannerPatternLayers layers = item.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
            for (BannerPatternLayers.Layer layer : layers.layers()) {
                list.add(new Pair<>(layer.pattern(), layer.color()));
            }
        }

        poseStack.popPose();
        VertexConsumer vertexconsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        getModel().renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        this.renderSails((SailEntity) entity, poseStack, buffer, partialTick, packedLight, OverlayTexture.NO_OVERLAY, list, 1.0F, 1.0F, 1.0F, 1.0f);

        poseStack.popPose();
    }

    private void renderSails(SailEntity entity, PoseStack poseStack, MultiBufferSource multiBufferSource, float partialTick,
                             int packedLight, int overlay, List<Pair<Holder<BannerPattern>, DyeColor>> patterns, float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        poseStack.translate(0.07, -2.44, -0.19);

        double deltaZ = entity.getDeltaMovement().length();
        if(entity.isPassenger())
            deltaZ = entity.getVehicle().getDeltaMovement().length();

        float windEffect = (float) Math.abs(deltaZ) * 10f;
        windEffect = Mth.clamp(windEffect, 0f, 1f);

        float time = entity.tickCount * 0.1f + entity.getId();
        float randomSway = Mth.sin(time) * 0.2f;
        float freakOutAmount = (float) (Mth.sin(time * 100) * 0.1f * (entity.level().isRaining() ? 1.1 : 0) * (entity.level().isThundering() ? 1.1 : 0));
        windEffect += randomSway + freakOutAmount;

        float middleBend = (float) (-windEffect * 0.8f * (entity.level().isRaining() ? 1.1 : 1) * (entity.level().isThundering() ? 1.1 : 1));

        edge2 = new Vec3(0, edge2.y, middleBend * 0.5f);
        edge3 = new Vec3(0, edge3.y, middleBend * 0.85f);
        edge4 = new Vec3(0, edge4.y, middleBend);

        float width = 3.75f;

        if (entity.getBanner().isEmpty()) {
            renderSailSegment(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TARP_TEXTURE)), edge1, edge2, width, packedLight, overlay, red, green, blue, alpha);
            renderSailSegment(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TARP_TEXTURE)), edge2, edge3, width, packedLight, overlay, red, green, blue, alpha);
            renderSailSegment(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TARP_TEXTURE)), edge3, edge4, width, packedLight, overlay, red, green, blue, alpha);
            renderSailSegment(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TARP_TEXTURE)), edge4, edge5, width, packedLight, overlay, red, green, blue, alpha);
        } else {

            BannerItem item = (BannerItem) entity.getBanner().getItem();
            DyeColor baseColor = item.getColor();
            
            int baseColorInt = baseColor.getTextureDiffuseColor();
            float baseRed = ((baseColorInt >> 16) & 0xFF) / 255.0F;
            float baseGreen = ((baseColorInt >> 8) & 0xFF) / 255.0F;
            float baseBlue = (baseColorInt & 0xFF) / 255.0F;

            renderSailSegment(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TARP_TEXTURE)), edge1, edge2, width, packedLight, overlay, baseRed, baseGreen, baseBlue, alpha);
            renderSailSegment(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TARP_TEXTURE)), edge2, edge3, width, packedLight, overlay, baseRed, baseGreen, baseBlue, alpha);
            renderSailSegment(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TARP_TEXTURE)), edge3, edge4, width, packedLight, overlay, baseRed, baseGreen, baseBlue, alpha);
            renderSailSegment(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TARP_TEXTURE)), edge4, edge5, width, packedLight, overlay, baseRed, baseGreen, baseBlue, alpha);

            // In 1.21.1, BANNER_PATTERNS does not include the base color at index 0
            // (unlike 1.20.1's createPatterns()), so no skip needed here.
            for(int i = 0; i < 17 && i < patterns.size(); ++i) {
                Pair<Holder<BannerPattern>, DyeColor> pair = patterns.get(i);
                int patternColorInt = pair.getSecond().getTextureDiffuseColor();
                float patternRed = ((patternColorInt >> 16) & 0xFF) / 255.0F;
                float patternGreen = ((patternColorInt >> 8) & 0xFF) / 255.0F;
                float patternBlue = (patternColorInt & 0xFF) / 255.0F;

                Material bannerMaterial = Sheets.getBannerMaterial(pair.getFirst());
                VertexConsumer patternVertexConsumer = bannerMaterial.buffer(multiBufferSource, RenderType::entityNoOutline);

                renderBannerSailSegment(poseStack, patternVertexConsumer, edge1, edge2, width, packedLight, overlay, patternRed, patternGreen, patternBlue, 1);
                renderBannerSailSegment(poseStack, patternVertexConsumer, edge2, edge3, width, packedLight, overlay, patternRed, patternGreen, patternBlue, 1);
                renderBannerSailSegment(poseStack, patternVertexConsumer, edge3, edge4, width, packedLight, overlay, patternRed, patternGreen, patternBlue, 1);
                renderBannerSailSegment(poseStack, patternVertexConsumer, edge4, edge5, width, packedLight, overlay, patternRed, patternGreen, patternBlue, 1);
            }
        }

        poseStack.popPose();

    }

    private void renderSailSegment(PoseStack poseStack, VertexConsumer vertexConsumer, Vec3 topEdge, Vec3 bottomEdge, float width, int packedLight, int overlay, float red, float green, float blue, float alpha) {
        org.joml.Matrix4f matrix = poseStack.last().pose();

        float x0 = (-width / 2f);
        float x1 = (width / 2f);
        float topY = ((float) topEdge.y * 3.55f);
        float topZ = ((float) topEdge.z);
        float bottomY = ((float) bottomEdge.y * 3.55f);
        float bottomZ = ((float) bottomEdge.z);

        float u0 = 0.0f;
        float u1 = 1.0f;
        float v0 = (float) (1.0f - topEdge.y);
        float v1 = (float) (1.0f - bottomEdge.y);

        float minLight = 0.9f;
        float topLight = (minLight + (1f - minLight) * v0);
        float bottomLight = (minLight + (1f - minLight) * v1);

        float nx = 0f;
        float nz = -1f;

        addVertex(vertexConsumer, matrix, x0, topY, topZ, u0, v0, red, green, blue, alpha, (int)(packedLight * topLight), overlay, nx, 0f, nz);
        addVertex(vertexConsumer, matrix, x1, topY, topZ, u1, v0, red, green, blue, alpha, (int)(packedLight * topLight), overlay, nx, 0f, nz);
        addVertex(vertexConsumer, matrix, x1, bottomY, bottomZ, u1, v1, red, green, blue, alpha, (int)(packedLight * bottomLight), overlay, nx, 0f, nz);
        addVertex(vertexConsumer, matrix, x0, bottomY, bottomZ, u0, v1, red, green, blue, alpha, (int)(packedLight * bottomLight), overlay, nx, 0f, nz);

        addVertex(vertexConsumer, matrix, x1, topY, topZ, u1, v0, red, green, blue, alpha, (int)(packedLight * topLight), overlay, nx, 0f, -nz);
        addVertex(vertexConsumer, matrix, x0, topY, topZ, u0, v0, red, green, blue, alpha, (int)(packedLight * topLight), overlay, nx, 0f, -nz);
        addVertex(vertexConsumer, matrix, x0, bottomY, bottomZ, u0, v1, red, green, blue, alpha, (int)(packedLight * bottomLight), overlay, nx, 0f, -nz);
        addVertex(vertexConsumer, matrix, x1, bottomY, bottomZ, u1, v1, red, green, blue, alpha, (int)(packedLight * bottomLight), overlay, nx, 0f, -nz);
    }

    // Uses the same addVertex helper as renderSailSegment for vertex precision consistency.
    // Small Z-offset prevents Z-fighting between banner pattern and base sail quads.
    private void renderBannerSailSegment(PoseStack poseStack, VertexConsumer vertexConsumer, Vec3 topEdge, Vec3 bottomEdge, float width, int packedLight, int overlay, float red, float green, float blue, float alpha) {
        org.joml.Matrix4f matrix = poseStack.last().pose();

        float x0 = (-width / 2f);
        float x1 = (width / 2f);
        float topY = ((float) topEdge.y * 3.55f);
        float topZ = ((float) topEdge.z);
        float bottomY = ((float) bottomEdge.y * 3.55f);
        float bottomZ = ((float) bottomEdge.z);

        float segmentHeight = 0.25f;
        float segmentIndex = (float) (topEdge.y / segmentHeight);

        float u0 = 0.0f;
        float u1 = 0.333f;

        float v0 = (segmentIndex * 0.25f) * 0.666f;
        float v1 = (segmentIndex + 1) * 0.25f * 0.666f;

        float nx = 0f;
        float nz = -1f;

        // Z-offset to push pattern slightly in front/behind the base sail
        float zOff = 0.001f;

        // Front face (offset forward)
        addVertex(vertexConsumer, matrix, x0, topY, topZ + zOff, u0, v0, red, green, blue, alpha, packedLight, overlay, nx, 0f, nz);
        addVertex(vertexConsumer, matrix, x1, topY, topZ + zOff, u1, v0, red, green, blue, alpha, packedLight, overlay, nx, 0f, nz);
        addVertex(vertexConsumer, matrix, x1, bottomY, bottomZ + zOff, u1, v1, red, green, blue, alpha, packedLight, overlay, nx, 0f, nz);
        addVertex(vertexConsumer, matrix, x0, bottomY, bottomZ + zOff, u0, v1, red, green, blue, alpha, packedLight, overlay, nx, 0f, nz);

        // Back face (offset backward)
        addVertex(vertexConsumer, matrix, x1, topY, topZ - zOff, u1, v0, red, green, blue, alpha, packedLight, overlay, nx, 0f, -nz);
        addVertex(vertexConsumer, matrix, x0, topY, topZ - zOff, u0, v0, red, green, blue, alpha, packedLight, overlay, nx, 0f, -nz);
        addVertex(vertexConsumer, matrix, x0, bottomY, bottomZ - zOff, u0, v1, red, green, blue, alpha, packedLight, overlay, nx, 0f, -nz);
        addVertex(vertexConsumer, matrix, x1, bottomY, bottomZ - zOff, u1, v1, red, green, blue, alpha, packedLight, overlay, nx, 0f, -nz);
    }
    
    private void addVertex(VertexConsumer builder, org.joml.Matrix4f matrix, float x, float y, float z, float u, float v, float r, float g, float b, float a, int light, int overlay, float nx, float ny, float nz) {
        org.joml.Vector4f vector = new org.joml.Vector4f(x, y, z, 1.0F);
        vector.mul(matrix);
        builder.addVertex(vector.x, vector.y, vector.z).setColor(r, g, b, a).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
    }


    public Model getModel() {
        return model;
    }
    private VertexConsumer vertex(VertexConsumer builder, org.joml.Matrix4f matrix, float x, float y, float z) {
        return builder.addVertex(matrix, x, y, z);
    }
}
