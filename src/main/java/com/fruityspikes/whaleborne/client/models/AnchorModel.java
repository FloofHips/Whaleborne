package com.fruityspikes.whaleborne.client.models;

import com.fruityspikes.whaleborne.server.entities.AnchorEntity;
import com.fruityspikes.whaleborne.server.entities.SailEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.phys.Vec3;

public class AnchorModel<T extends AnchorEntity> extends EntityModel<T> {
    private final ModelPart spool;
    private final ModelPart bb_main;

    public AnchorModel(ModelPart root) {
        this.spool = root.getChild("spool");
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition spool = partdefinition.addOrReplaceChild("spool", CubeListBuilder.create().texOffs(48, 20).addBox(-8.0F, -6.0F, -6.0F, 16.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 16.0F, 0.0F));

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 20).addBox(-16.0F, -16.0F, -8.0F, 8.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(0, 20).mirror().addBox(8.0F, -16.0F, -8.0F, 8.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 0).addBox(-12.0F, -20.0F, -8.0F, 24.0F, 4.0F, 16.0F, new CubeDeformation(0.0F))
                .texOffs(0, 52).addBox(-5.0F, -27.0F, 0.0F, 10.0F, 7.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(48, 44).addBox(-8.0F, -11.0F, -3.0F, 16.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(36, 56).addBox(-5.0F, -24.0F, -4.0F, 10.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }
    @Override
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {
        if(t.getHeadPos()!=null)
            spool.xRot = (float) t.position().distanceTo(new Vec3(t.getHeadPos().getX(), t.getHeadPos().getY(), t.getHeadPos().getZ()));
    }


    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int i1, int i2) {
        bb_main.render(poseStack, vertexConsumer, i, i1, i2);
        spool.render(poseStack, vertexConsumer, i, i1, i2);
    }
}