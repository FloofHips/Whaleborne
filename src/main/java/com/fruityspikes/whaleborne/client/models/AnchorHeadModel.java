package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.AnchorHeadEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class AnchorHeadModel<T extends AnchorHeadEntity> extends EntityModel<T> {
    private final ModelPart bb_main;

    public AnchorHeadModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, -27.0F, -2.0F, 4.0F, 27.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-10.0F, -8.0F, -2.0F, 20.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(16, 30).addBox(-14.0F, -16.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(32, 30).addBox(-10.0F, -16.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(32, 30).mirror().addBox(6.0F, -16.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(16, 30).mirror().addBox(10.0F, -16.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 8).addBox(-10.0F, -24.0F, -2.0F, 20.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(16, 16).addBox(-5.0F, -37.0F, -2.0F, 10.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
    @Override
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
