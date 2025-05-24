package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
public class HullbackArmorModel<T extends HullbackEntity> extends EntityModel<T> {

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart fluke;

    public HullbackArmorModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.fluke = root.getChild("fluke");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(288, 226).addBox(-48.0F, -57.0F, 114.0F, 96.0F, 64.0F, 24.0F, new CubeDeformation(0.0F))
                .texOffs(0, 226).addBox(-48.0F, 31.0F, 90.0F, 96.0F, 16.0F, 48.0F, new CubeDeformation(0.0F))
                .texOffs(492, 172).addBox(-8.0F, -49.0F, 34.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(492, 196).addBox(-8.0F, -49.0F, 82.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-40.0F, -41.0F, 0.0F, 80.0F, 2.0F, 130.0F, new CubeDeformation(0.0F))
                .texOffs(344, 132).addBox(-48.0F, -57.0F, -8.0F, 96.0F, 32.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 354).addBox(-8.0F, -73.0F, -56.0F, 16.0F, 16.0F, 64.0F, new CubeDeformation(0.0F))
                .texOffs(240, 290).addBox(-8.0F, -57.0F, 0.0F, 16.0F, 16.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(504, 77).addBox(-8.0F, -57.0F, -16.0F, 16.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(96, 434).addBox(-48.0F, -57.0F, 98.0F, 8.0F, 64.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(160, 388).addBox(-48.0F, -49.0F, 0.0F, 8.0F, 24.0F, 58.0F, new CubeDeformation(0.0F))
                .texOffs(344, 172).addBox(-48.0F, -57.0F, 0.0F, 8.0F, 8.0F, 34.0F, new CubeDeformation(0.0F))
                .texOffs(420, 77).addBox(40.0F, -57.0F, 0.0F, 8.0F, 8.0F, 34.0F, new CubeDeformation(0.0F))
                .texOffs(272, 470).addBox(40.0F, -57.0F, 98.0F, 8.0F, 64.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(48, 434).addBox(40.0F, -57.0F, 82.0F, 8.0F, 88.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(208, 470).addBox(40.0F, -49.0F, 58.0F, 8.0F, 40.0F, 24.0F, new CubeDeformation(0.0F))
                .texOffs(292, 388).addBox(40.0F, -49.0F, 0.0F, 8.0F, 24.0F, 58.0F, new CubeDeformation(0.0F))
                .texOffs(144, 470).addBox(-48.0F, -49.0F, 58.0F, 8.0F, 40.0F, 24.0F, new CubeDeformation(0.0F))
                .texOffs(0, 434).addBox(-48.0F, -57.0F, 82.0F, 8.0F, 88.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 290).addBox(-48.0F, -57.0F, 84.0F, 96.0F, 40.0F, 24.0F, new CubeDeformation(0.0F))
                .texOffs(432, 314).addBox(-48.0F, -49.0F, 52.0F, 8.0F, 32.0F, 32.0F, new CubeDeformation(0.0F))
                .texOffs(160, 354).addBox(-32.0F, -49.0F, 60.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(428, 172).addBox(16.0F, -49.0F, 60.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(320, 470).addBox(16.0F, -49.0F, 28.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(428, 196).addBox(-32.0F, -49.0F, 28.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(420, 0).addBox(40.0F, -49.0F, -8.0F, 8.0F, 17.0F, 60.0F, new CubeDeformation(0.0F))
                .texOffs(0, 132).addBox(-40.0F, -41.0F, -8.0F, 80.0F, 2.0F, 92.0F, new CubeDeformation(0.0F))
                .texOffs(424, 388).addBox(-48.0F, -49.0F, -8.0F, 8.0F, 17.0F, 60.0F, new CubeDeformation(0.0F))
                .texOffs(424, 465).addBox(40.0F, -49.0F, 52.0F, 8.0F, 32.0F, 32.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition fluke = partdefinition.addOrReplaceChild("fluke", CubeListBuilder.create().texOffs(240, 314).addBox(-24.0F, -13.0F, 20.0F, 48.0F, 26.0F, 48.0F, new CubeDeformation(0.0F))
                .texOffs(320, 494).addBox(-8.0F, -21.0F, 36.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 1024, 1024);
    }
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        fluke.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }


    public ModelPart getHead() {
        return head;
    }
    public ModelPart getBody() {
        return body;
    }
    public ModelPart getFluke() {
        return fluke;
    }
}
