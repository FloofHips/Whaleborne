package com.fruityspikes.whaleborne.client.renderers;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class NothingRenderer<T extends Entity> extends EntityRenderer<Entity> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Whaleborne.MODID, "textures/entity/mast.png");
    public NothingRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return null;
    }
}
