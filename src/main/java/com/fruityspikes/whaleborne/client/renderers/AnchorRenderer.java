package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.client.models.AnchorModel;
import com.fruityspikes.whaleborne.client.models.CannonModel;
import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AnchorRenderer extends WhaleWidgetRenderer {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/anchor.png");
    public AnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AnchorModel<>(context.bakeLayer(WBEntityModelLayers.ANCHOR));
    }
    @Override
    public ResourceLocation getTextureLocation(WhaleWidgetEntity whaleWidgetEntity) {
        return TEXTURE;
    }
}
