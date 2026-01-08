package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.HelmEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class HelmModel<T extends HelmEntity> extends EntityModel<T> {
    private final ModelPart wheel;
    private final ModelPart bb_main;
    public HelmModel(ModelPart root) {
        this.wheel = root.getChild("wheel");
        this.bb_main = root.getChild("bb_main");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition wheel = partdefinition.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(0, 44).addBox(-5.0F, -5.0F, 2.25F, 10.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 40).addBox(-8.0F, -1.0F, 2.25F, 16.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 44).addBox(5.0F, -7.0F, 2.25F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 44).addBox(-7.0F, -7.0F, 2.25F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 44).addBox(5.0F, 5.0F, 2.25F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 44).addBox(-7.0F, 5.0F, 2.25F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 44).addBox(-1.0F, -1.0F, 0.25F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.0F, -16.25F));

        PartDefinition cube_r1 = wheel.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 40).addBox(-9.0F, -2.0F, -1.0F, 16.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -1.0F, 3.25F, 0.0F, 0.0F, -1.5708F));

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -6.0F, -20.0F, 28.0F, 6.0F, 22.0F, new CubeDeformation(0.0F))
                .texOffs(36, 40).addBox(-5.0F, -10.0F, -20.0F, 10.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 28).addBox(-8.0F, -6.0F, 2.0F, 16.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-2.0F, -21.0F, -20.0F, 4.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 64);
    }
    @Override
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {
        wheel.resetPose();
        wheel.zRot = Mth.lerp(v, t.getPrevWheelRotation(), t.getWheelRotation());
    }


    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, int i2) {
        bb_main.render(poseStack, vertexConsumer, i, i1, i2);
        wheel.render(poseStack, vertexConsumer, i, i1, i2);
    }
}
