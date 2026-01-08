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
                .texOffs(528, 220).addBox(-8.0F, -73.0F, -56.0F, 16.0F, 16.0F, 64.0F, new CubeDeformation(0.0F))
                .texOffs(240, 290).addBox(-8.0F, -57.0F, 0.0F, 16.0F, 16.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(504, 77).addBox(-8.0F, -57.0F, -16.0F, 16.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(624, 300).addBox(-48.0F, -57.0F, 98.0F, 8.0F, 64.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(572, 74).addBox(-48.0F, -49.0F, 0.0F, 8.0F, 24.0F, 58.0F, new CubeDeformation(0.0F))
                .texOffs(344, 172).addBox(-48.0F, -57.0F, 0.0F, 8.0F, 8.0F, 34.0F, new CubeDeformation(0.0F))
                .texOffs(420, 77).addBox(40.0F, -57.0F, 0.0F, 8.0F, 8.0F, 34.0F, new CubeDeformation(0.0F))
                .texOffs(684, 156).addBox(40.0F, -57.0F, 98.0F, 8.0F, 64.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(576, 300).addBox(40.0F, -57.0F, 82.0F, 8.0F, 88.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(620, 156).addBox(40.0F, -49.0F, 58.0F, 8.0F, 40.0F, 24.0F, new CubeDeformation(0.0F))
                .texOffs(704, 74).addBox(40.0F, -49.0F, 0.0F, 8.0F, 24.0F, 58.0F, new CubeDeformation(0.0F))
                .texOffs(556, 156).addBox(-48.0F, -49.0F, 58.0F, 8.0F, 40.0F, 24.0F, new CubeDeformation(0.0F))
                .texOffs(528, 300).addBox(-48.0F, -57.0F, 82.0F, 8.0F, 88.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 290).addBox(-48.0F, -57.0F, 84.0F, 96.0F, 40.0F, 24.0F, new CubeDeformation(0.0F))
                .texOffs(844, 0).addBox(-48.0F, -49.0F, 52.0F, 8.0F, 32.0F, 32.0F, new CubeDeformation(0.0F))
                .texOffs(572, 40).addBox(-32.0F, -49.0F, 60.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(428, 172).addBox(16.0F, -49.0F, 60.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(732, 156).addBox(16.0F, -49.0F, 28.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(428, 196).addBox(-32.0F, -49.0F, 28.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(420, 0).addBox(40.0F, -49.0F, -8.0F, 8.0F, 17.0F, 60.0F, new CubeDeformation(0.0F))
                .texOffs(0, 132).addBox(-40.0F, -41.0F, -8.0F, 80.0F, 2.0F, 92.0F, new CubeDeformation(0.0F))
                .texOffs(836, 74).addBox(-48.0F, -49.0F, -8.0F, 8.0F, 17.0F, 60.0F, new CubeDeformation(0.0F))
                .texOffs(836, 151).addBox(40.0F, -49.0F, 52.0F, 8.0F, 32.0F, 32.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition fluke = partdefinition.addOrReplaceChild("fluke", CubeListBuilder.create().texOffs(652, 0).addBox(-24.0F, -13.0F, 20.0F, 48.0F, 26.0F, 48.0F, new CubeDeformation(0.0F))
                .texOffs(732, 180).addBox(-8.0F, -21.0F, 36.0F, 16.0F, 8.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 1024, 512);
    }
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

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

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, int i2) {
        head.render(poseStack, vertexConsumer, i, i1, i2);
        body.render(poseStack, vertexConsumer, i, i1, i2);
        fluke.render(poseStack, vertexConsumer, i, i1, i2);
    }
}
