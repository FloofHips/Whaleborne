package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.HullbackArmorModel;
import com.fruityspikes.whaleborne.client.models.HullbackModel;
import com.fruityspikes.whaleborne.client.models.SailModel;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.SailEntity;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.MinecartModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
public class SailRenderer<T extends SailEntity> extends WhaleWidgetRenderer<SailEntity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/sail.png");
    private final SailModel<SailEntity> model;
    public SailRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.7F;
        this.model = new SailModel<>(context.bakeLayer(WBEntityModelLayers.SAIL));
    }

    public ResourceLocation getTextureLocation(WhaleWidgetEntity whaleWidgetEntity) {
        return TEXTURE;
    }

    public Model getModel() {
        return model;
    }
}
