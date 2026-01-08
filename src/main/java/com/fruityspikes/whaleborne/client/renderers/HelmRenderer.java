package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.HelmModel;
import com.fruityspikes.whaleborne.client.models.SailModel;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class HelmRenderer extends WhaleWidgetRenderer {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/helm.png");
    public HelmRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HelmModel<>(context.bakeLayer(WBEntityModelLayers.HELM));
    }
    @Override
    public ResourceLocation getTextureLocation(WhaleWidgetEntity whaleWidgetEntity) {
        return TEXTURE;
    }
}
