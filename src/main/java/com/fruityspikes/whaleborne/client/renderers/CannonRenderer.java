package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.CannonModel;
import com.fruityspikes.whaleborne.client.models.HelmModel;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class CannonRenderer extends WhaleWidgetRenderer {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Whaleborne.MODID, "textures/entity/cannon.png");
    public CannonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CannonModel<>(context.bakeLayer(WBEntityModelLayers.CANNON));
    }
    @Override
    public ResourceLocation getTextureLocation(WhaleWidgetEntity whaleWidgetEntity) {
        return TEXTURE;
    }
}
