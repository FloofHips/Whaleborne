package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.MastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
public class MastModel<T extends MastEntity> extends EntityModel<T> {
    private final ModelPart bb_main;
    public MastModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -128.0F, -4.0F, 8.0F, 120.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(32, 86).addBox(-16.0F, -8.0F, -16.0F, 32.0F, 8.0F, 32.0F, new CubeDeformation(0.0F))
                .texOffs(32, 0).addBox(-16.0F, -144.0F, -16.0F, 32.0F, 16.0F, 32.0F, new CubeDeformation(0.0F))
                .texOffs(32, 48).addBox(-13.0F, -144.0F, -13.0F, 26.0F, 12.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 256, 128);
    }
    @Override
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, float v, float v1, float v2, float v3) {
        bb_main.render(poseStack, vertexConsumer, i, i1, v, v1, v2, v3);
    }
}
