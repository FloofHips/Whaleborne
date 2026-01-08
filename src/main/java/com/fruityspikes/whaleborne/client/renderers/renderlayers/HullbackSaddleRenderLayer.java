package com.fruityspikes.whaleborne.client.renderers.renderlayers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.HullbackModel;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class HullbackSaddleRenderLayer extends RenderLayer<HullbackEntity, HullbackModel<HullbackEntity>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/bewereager/bewereager_collar.png");

    public HullbackSaddleRenderLayer(RenderLayerParent<HullbackEntity, HullbackModel<HullbackEntity>> p_117707_) {
        super(p_117707_);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int i, HullbackEntity bewereager, float v, float v1, float v2, float v3, float v4, float v5) {
        //renderColoredCutoutModel(this.getParentModel(), TEXTURE, poseStack, bufferSource, i, bewereager, $$10[0], $$10[1], $$10[2]);
    }
}