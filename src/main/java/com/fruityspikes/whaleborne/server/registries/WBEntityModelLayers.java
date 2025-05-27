package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class WBEntityModelLayers {

    public static final ModelLayerLocation HULLBACK = register("hullback");
    public static final ModelLayerLocation HULLBACK_ARMOR = register("hullback_armor");
    public static final ModelLayerLocation SAIL = register("sail");

    private static ModelLayerLocation register(String name) {
        return register(name, "main");
    }

    private static ModelLayerLocation register(String name, String layer_name) {
        return new ModelLayerLocation(new ResourceLocation(Whaleborne.MODID, name), layer_name);
    }
}
