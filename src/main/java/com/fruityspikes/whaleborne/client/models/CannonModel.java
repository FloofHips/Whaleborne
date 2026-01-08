package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.CannonEntity;
import com.fruityspikes.whaleborne.server.entities.HelmEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class CannonModel<T extends CannonEntity> extends EntityModel<T> {
    private final ModelPart cannon;
    private final ModelPart bb_main;
    public CannonModel(ModelPart root) {
        this.cannon = root.getChild("cannon");
        this.bb_main = root.getChild("bb_main");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition cannon = partdefinition.addOrReplaceChild("cannon", CubeListBuilder.create().texOffs(0, 58).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(0, 99).addBox(-4.0F, -24.0F, -4.0F, 8.0F, 19.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 82).addBox(-6.0F, -29.0F, -6.0F, 12.0F, 5.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 9.0F, -17.0F));

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -6.0F, -20.0F, 28.0F, 6.0F, 22.0F, new CubeDeformation(0.0F))
                .texOffs(0, 28).addBox(-8.0F, -6.0F, 2.0F, 16.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 40).addBox(4.0F, -18.0F, -20.0F, 5.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 40).mirror().addBox(-9.0F, -18.0F, -20.0F, 5.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }
    @Override
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {
        cannon.resetPose();
        cannon.xRot = (t.getCannonXRot() + 90) * Mth.DEG_TO_RAD;
    }



    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, int i2) {
        bb_main.render(poseStack, vertexConsumer, i, i1, i2);
        cannon.render(poseStack, vertexConsumer, i, i1, i2);
    }
}

