package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.SailEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class SailModel<T extends SailEntity> extends EntityModel<T> {
    private final ModelPart bone;
    public SailModel(ModelPart root) {
        this.bone = root.getChild("bone");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0).addBox(-31.0F, -6.0F, -1.0F, 60.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 24).addBox(-5.0F, -69.0F, -1.0F, 6.0F, 69.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 12).addBox(-31.0F, -69.0F, -1.0F, 60.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 24.0F, -2.0F));

        return LayerDefinition.create(meshdefinition, 256, 128);
    }
    @Override
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, float v, float v1, float v2, float v3) {
        bone.render(poseStack, vertexConsumer, i, i1, v, v1, v2, v3);
    }
}
