package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class HullbackModel<T extends HullbackEntity> extends EntityModel<T> {
    private final ModelPart head;
    private final ModelPart lip;
    private final ModelPart jaw_2;
    private final ModelPart jaw_1;
    private final ModelPart jaw_0;
    private final ModelPart left_eye;
    private final ModelPart left_upper_eyelid;
    private final ModelPart left_pupil;
    private final ModelPart left_lower_eyelid;
    private final ModelPart right_eye;
    private final ModelPart right_upper_eyelid;
    private final ModelPart right_pupil;
    private final ModelPart right_lower_eyelid;
    private final ModelPart body;
    private final ModelPart left_fin;
    private final ModelPart right_fin;
    private final ModelPart tail;
    private final ModelPart fluke;
    private HullbackEntity entity;
    private float a;

    public HullbackModel(ModelPart root) {
        this.head = root.getChild("head");
        this.lip = this.head.getChild("lip");
        this.jaw_2 = this.head.getChild("jaw_2");
        this.jaw_1 = this.head.getChild("jaw_1");
        this.jaw_0 = this.head.getChild("jaw_0");
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

        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-40.0F, -40.0F, 0.0F, 80.0F, 80.0F, 130.0F, new CubeDeformation(0.0F))
                .texOffs(0, 210).addBox(-38.0F, -39.0F, 2.0F, 76.0F, 78.0F, 126.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition lip = head.addOrReplaceChild("lip", CubeListBuilder.create().texOffs(420, 100).addBox(-40.0F, -10.0F, -25.0F, 80.0F, 20.0F, 50.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -10.0F, 25.0F));
        PartDefinition jaw_2 = head.addOrReplaceChild("jaw_2", CubeListBuilder.create().texOffs(420, 0).addBox(-40.0F, -30.0F, -45.0F, 80.0F, 10.0F, 90.0F, new CubeDeformation(-0.001F)), PartPose.offset(0.0F, 30.0F, 45.0F));
        PartDefinition jaw_1 = head.addOrReplaceChild("jaw_1", CubeListBuilder.create().texOffs(0, 414).addBox(-40.0F, -20.0F, -45.0F, 80.0F, 10.0F, 90.0F, new CubeDeformation(-0.001F)), PartPose.offset(0.0F, 30.0F, 45.0F));
        PartDefinition jaw_0 = head.addOrReplaceChild("jaw_0", CubeListBuilder.create().texOffs(404, 390).addBox(-40.0F, -10.0F, -45.0F, 80.0F, 10.0F, 90.0F, new CubeDeformation(-0.001F)), PartPose.offset(0.0F, 30.0F, 45.0F));
        PartDefinition left_eye = head.addOrReplaceChild("left_eye", CubeListBuilder.create(), PartPose.offset(40.0F, 17.0F, 110.0F));
        PartDefinition left_upper_eyelid = left_eye.addOrReplaceChild("left_upper_eyelid", CubeListBuilder.create().texOffs(340, 424).addBox(-1.28F, -3.75F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(340, 414).mirror().addBox(-1.28F, 1.25F, -5.0F, 5.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(1.3F, -3.25F, 0.0F));
        PartDefinition left_pupil = left_eye.addOrReplaceChild("left_pupil", CubeListBuilder.create().texOffs(420, 200).addBox(-0.09F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.1F, 0.5F, -0.5F));
        PartDefinition left_lower_eyelid = left_eye.addOrReplaceChild("left_lower_eyelid", CubeListBuilder.create().texOffs(360, 424).addBox(-1.28F, -1.25F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(370, 414).mirror().addBox(-1.28F, -1.25F, -5.0F, 5.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(1.3F, 4.25F, 0.0F));
        PartDefinition right_eye = head.addOrReplaceChild("right_eye", CubeListBuilder.create(), PartPose.offset(-40.0F, 18.0F, 109.0F));
        PartDefinition right_upper_eyelid = right_eye.addOrReplaceChild("right_upper_eyelid", CubeListBuilder.create().texOffs(340, 424).addBox(1.28F, -3.75F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(340, 414).addBox(-3.72F, 1.25F, -5.0F, 5.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.3F, -4.25F, 1.0F));
        PartDefinition right_pupil = right_eye.addOrReplaceChild("right_pupil", CubeListBuilder.create().texOffs(420, 200).addBox(0.09F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.1F, -0.5F, 0.5F));
        PartDefinition right_lower_eyelid = right_eye.addOrReplaceChild("right_lower_eyelid", CubeListBuilder.create().texOffs(360, 424).addBox(1.28F, -1.25F, -5.0F, 0.0F, 5.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(370, 414).addBox(-3.72F, -1.25F, -5.0F, 5.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.3F, 3.25F, 1.0F));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(404, 210).addBox(-40.0F, -40.0F, 0.0F, 80.0F, 80.0F, 100.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition left_fin = body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(0, 564).mirror().addBox(0.0F, -5.0F, -20.0F, 60.0F, 10.0F, 40.0F, new CubeDeformation(0.0F)), PartPose.offset(40.0F, 35.0F, 30.0F));
        PartDefinition right_fin = body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(0, 564).addBox(-60.0F, -5.0F, -20.0F, 60.0F, 10.0F, 40.0F, new CubeDeformation(0.0F)), PartPose.offset(-40.0F, 35.0F, 30.0F));
        PartDefinition tail = partdefinition.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(340, 490).addBox(-20.0F, -20.0F, 0.0F, 40.0F, 40.0F, 80.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition fluke = partdefinition.addOrReplaceChild("fluke", CubeListBuilder.create().texOffs(0, 514).addBox(-40.0F, -5.0F, 0.0F, 80.0F, 10.0F, 40.0F, new CubeDeformation(0.0F))
                .texOffs(420, 170).addBox(10.0F, -5.0F, 40.0F, 30.0F, 10.0F, 20.0F, new CubeDeformation(0.0F))
                .texOffs(240, 514).addBox(-40.0F, -5.0F, 40.0F, 30.0F, 10.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

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
        this.lip.resetPose();
        this.tail.resetPose();
        this.fluke.resetPose();
        this.left_lower_eyelid.resetPose();
        this.right_lower_eyelid.resetPose();
        this.left_upper_eyelid.resetPose();
        this.right_upper_eyelid.resetPose();
        this.left_pupil.resetPose();
        this.right_pupil.resetPose();


        this.lip.y=Mth.lerp(entity.getMouthOpenProgress(), this.lip.getInitialPose().y, this.lip.getInitialPose().y+27);
        this.jaw_0.y=Math.min(Mth.lerp(entity.getMouthOpenProgress(), this.jaw_0.getInitialPose().y, this.jaw_0.getInitialPose().y+27), this.jaw_0.getInitialPose().y+10);
        this.jaw_1.y=Math.min(Mth.lerp(entity.getMouthOpenProgress(), this.jaw_1.getInitialPose().y, this.jaw_1.getInitialPose().y+27), this.jaw_1.getInitialPose().y+20);
        this.jaw_2.y=Math.min(Mth.lerp(entity.getMouthOpenProgress(), this.jaw_2.getInitialPose().y, this.jaw_2.getInitialPose().y+27), this.jaw_2.getInitialPose().y+30);

        this.left_lower_eyelid.y=Mth.lerp(entity.getMouthOpenProgress(), this.left_lower_eyelid.getInitialPose().y, this.left_lower_eyelid.getInitialPose().y-2.25f);
        this.right_lower_eyelid.y=Mth.lerp(entity.getMouthOpenProgress(), this.right_lower_eyelid.getInitialPose().y, this.right_lower_eyelid.getInitialPose().y-2.25f);

        this.left_upper_eyelid.y=Mth.lerp(entity.getMouthOpenProgress(), this.left_upper_eyelid.getInitialPose().y, this.left_upper_eyelid.getInitialPose().y+2.25f);
        this.right_upper_eyelid.y=Mth.lerp(entity.getMouthOpenProgress(), this.right_upper_eyelid.getInitialPose().y, this.right_upper_eyelid.getInitialPose().y+2.25f);

        float perpendicularYaw = netHeadYaw + 90;
        // Normalize to 0-1 range where 0 = forward, 0.5 = perpendicular, 1 = backward
        float normalized = (perpendicularYaw % 180) / 180f;

        this.left_pupil.z=Mth.lerp(Mth.clamp(normalized, 0, 1), this.left_pupil.getInitialPose().z-2, this.left_pupil.getInitialPose().z+2.25f);
        this.right_pupil.z=Mth.lerp(Mth.clamp(normalized, 0, 1), this.right_pupil.getInitialPose().z-2, this.right_pupil.getInitialPose().z+2.25f);

        //this.body.xRot = entity.body.getXRot();
        //this.body.yRot = entity.body.getYRot();

        //float finMovement = Mth.sin(ageInTicks * 0.1f) * 0.2f;
        left_fin.zRot = -swimCycle;
        right_fin.zRot = swimCycle;

    }



    public ModelPart getHead() {
        return head;
    }
    public ModelPart getBody() {
        return body;
    }
    public ModelPart getTail() {
        return tail;
    }
    public ModelPart getFluke() {
        return fluke;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, int i2) {

    }
}
