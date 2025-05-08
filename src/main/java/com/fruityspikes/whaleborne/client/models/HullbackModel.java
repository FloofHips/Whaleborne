package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public class HullbackModel<T extends HullbackEntity> extends EntityModel<T> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart lip;
    private final ModelPart jaw;
    private final ModelPart left_eye;
    private final ModelPart left_upper_eyelid;
    private final ModelPart left_pupil;
    private final ModelPart left_lower_eyelid;
    private final ModelPart right_eye;
    private final ModelPart right_upper_eyelid;
    private final ModelPart right_pupil;
    private final ModelPart right_lower_eyelid;
    private final ModelPart left_fin;
    private final ModelPart right_fin;
    private final ModelPart tail;
    private final ModelPart fluke;
    private HullbackEntity entity;
    private float a;

    public HullbackModel(ModelPart root) {
        this.head = root.getChild("head");
        this.lip = this.head.getChild("lip");
        this.jaw = this.head.getChild("jaw");
        this.left_eye = this.head.getChild("left_eye");
        this.left_upper_eyelid = this.left_eye.getChild("left_upper_eyelid");
        this.left_pupil = this.left_eye.getChild("left_pupil");
        this.left_lower_eyelid = this.left_eye.getChild("left_lower_eyelid");
        this.right_eye = this.head.getChild("right_eye");
        this.right_upper_eyelid = this.right_eye.getChild("right_upper_eyelid");
        this.right_pupil = this.right_eye.getChild("right_pupil");
        this.right_lower_eyelid = this.right_eye.getChild("right_lower_eyelid");
        this.body = root.getChild("body");
        this.left_fin = this.body.getChild("left_fin");
        this.right_fin = this.body.getChild("right_fin");
        this.tail = root.getChild("tail");
        this.fluke = root.getChild("fluke");
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-40.0F, -43.0F, -123.0F, 80.0F, 80.0F, 130.0F, new CubeDeformation(0.0F))
                .texOffs(0, 210).addBox(-39.0F, -42.0F, -122.0F, 78.0F, 78.0F, 128.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -13.0F, -7.0F));

        PartDefinition lip = head.addOrReplaceChild("lip", CubeListBuilder.create().texOffs(0, 416).addBox(-40.0F, -10.0F, -25.0F, 80.0F, 20.0F, 50.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -13.0F, -98.0F));

        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(412, 390).addBox(-40.0F, -30.0F, -45.0F, 80.0F, 30.0F, 90.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 27.0F, -78.0F));

        PartDefinition left_eye = head.addOrReplaceChild("left_eye", CubeListBuilder.create(), PartPose.offset(40.0F, 14.0F, -13.0F));

        PartDefinition left_upper_eyelid = left_eye.addOrReplaceChild("left_upper_eyelid", CubeListBuilder.create().texOffs(370, 416).addBox(-1.2F, -3.75F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(360, 416).mirror().addBox(-1.3F, 1.25F, -5.0F, 5.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(1.3F, -3.25F, 0.0F));

        PartDefinition left_pupil = left_eye.addOrReplaceChild("left_pupil", CubeListBuilder.create().texOffs(390, 416).addBox(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.1F, 0.5F, -0.5F));

        PartDefinition left_lower_eyelid = left_eye.addOrReplaceChild("left_lower_eyelid", CubeListBuilder.create().texOffs(370, 421).addBox(-1.2F, -1.25F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(360, 416).mirror().addBox(-1.3F, -1.25F, -5.0F, 5.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(1.3F, 4.25F, 0.0F));

        PartDefinition right_eye = head.addOrReplaceChild("right_eye", CubeListBuilder.create(), PartPose.offset(-40.0F, 15.0F, -14.0F));

        PartDefinition right_upper_eyelid = right_eye.addOrReplaceChild("right_upper_eyelid", CubeListBuilder.create().texOffs(370, 416).addBox(1.2F, -3.75F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(360, 416).addBox(-3.7F, 1.25F, -5.0F, 5.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.3F, -4.25F, 1.0F));

        PartDefinition right_pupil = right_eye.addOrReplaceChild("right_pupil", CubeListBuilder.create().texOffs(390, 416).addBox(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.1F, -0.5F, 0.5F));

        PartDefinition right_lower_eyelid = right_eye.addOrReplaceChild("right_lower_eyelid", CubeListBuilder.create().texOffs(370, 421).addBox(1.2F, -1.25F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(360, 416).addBox(-3.7F, -1.25F, -5.0F, 5.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.3F, 3.25F, 1.0F));

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(412, 210).addBox(-40.0F, -53.0F, -21.0F, 80.0F, 80.0F, 100.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.0F, 21.0F));

        PartDefinition left_fin = body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(420, 160).mirror().addBox(0.0F, -5.0F, -20.0F, 60.0F, 10.0F, 40.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(40.0F, 22.0F, 9.0F));

        PartDefinition right_fin = body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(420, 160).addBox(-60.0F, -5.0F, -20.0F, 60.0F, 10.0F, 40.0F, new CubeDeformation(0.0F)), PartPose.offset(-40.0F, 22.0F, 9.0F));

        PartDefinition tail = partdefinition.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(400, -20).addBox(-40.0F, -20.0F, -100.0F, 40.0F, 40.0F, 80.0F, new CubeDeformation(0.0F)), PartPose.offset(20.0F, 4.0F, 100.0F));

        PartDefinition fluke = partdefinition.addOrReplaceChild("fluke", CubeListBuilder.create().texOffs(420, 100).addBox(-79.0F, 9.0F, -153.0F, 80.0F, 10.0F, 40.0F, new CubeDeformation(0.0F))
                .texOffs(260, 416).addBox(-29.0F, 9.0F, -113.0F, 30.0F, 10.0F, 20.0F, new CubeDeformation(0.0F))
                .texOffs(260, 416).addBox(-79.0F, 9.0F, -113.0F, 30.0F, 10.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offset(39.0F, 5.0F, 153.0F));

        return LayerDefinition.create(meshdefinition, 1024, 1024);
    }
    public void prepareMobModel(HullbackEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.entity = entity;
        this.a = partialTick;
    }
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float leftEyeYaw = entity.getLeftEyeYaw();
        float rightEyeYaw = entity.getRightEyeYaw();
        float eyePitch = entity.getEyePitch();

        float swimCycle = (float) ((float)Math.sin(ageInTicks * 0.08f) * entity.getDeltaMovement().length());

        this.body.resetPose();
        this.jaw.resetPose();
        this.lip.resetPose();
        this.tail.resetPose();
        this.fluke.resetPose();

        this.lip.y=Mth.lerp(entity.getMouthOpenProgress(), this.lip.getInitialPose().y+30, this.lip.getInitialPose().y);
        this.jaw.yScale=Mth.lerp(entity.getMouthOpenProgress(), 1, 0);

        //this.body.xRot = entity.body.getXRot();
        //this.body.yRot = entity.body.getYRot();

        //float finMovement = Mth.sin(ageInTicks * 0.1f) * 0.2f;
        left_fin.zRot = swimCycle;
        right_fin.zRot = -swimCycle;

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        //body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);

        HullbackPartEntity headPart = entity.head;
        HullbackPartEntity bodyPart = entity.body;
        HullbackPartEntity tailPart = entity.tail;
        HullbackPartEntity flukePart = entity.fluke;

        headPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, this.head);
        bodyPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, this.body);
        tailPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, this.tail);
        flukePart.render(poseStack, vertexConsumer, packedLight, packedOverlay, this.fluke);

//        this.tail.y = -(float) tailPart.position().subtract(entity.position()).scale(20).y;
//        this.tail.x = -(float) tailPart.position().subtract(entity.position()).x;
//        this.tail.z = (float) tailPart.position().subtract(entity.position()).z;
//
//        tail.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
//
//        this.fluke.y = -(float) flukePart.position().subtract(entity.position()).subtract(0,1,0).scale(20).y;
//        this.fluke.x = -(float) flukePart.position().subtract(entity.position()).x;
//
//        fluke.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        //this.tail.x = tailPart.;
    }

    public ModelPart getTail() {
        return tail;
    }

    public ModelPart getFluke() {
        return fluke;
    }
}
