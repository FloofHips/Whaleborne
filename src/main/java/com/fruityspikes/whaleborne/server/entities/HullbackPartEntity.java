package com.fruityspikes.whaleborne.server.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;

public class HullbackPartEntity extends PartEntity<HullbackEntity> {
    public final HullbackEntity parent;
    public final String name;
    private final EntityDimensions size;
    public final Vec3 restingOffset;
    public HullbackPartEntity(HullbackEntity parent, String name, float width, float height, Vec3 restingOffset) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.parent = parent;
        this.name = name;
        this.restingOffset = restingOffset;
    }

    protected void defineSynchedData() {
    }

    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    public boolean isPickable() {
        return true;
    }

    @Nullable
    public ItemStack getPickResult() {
        return this.parent.getPickResult();
    }

    public boolean hurt(DamageSource source, float amount) {
        return this.isInvulnerableTo(source) ? false : this.parent.hurt(source, amount);
    }

    public boolean is(Entity entity) {
        return this == entity || this.parent == entity;
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }

    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    public boolean shouldBeSaved() {
        return false;
    }

    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, ModelPart part) {

        poseStack.pushPose();
        if(name.equals("tail"))
            poseStack.translate(0, 0, -2);
        if(name.equals("head"))
            poseStack.translate(0, 0, 5.5);
        poseStack.translate(0, 0, -size.width / 2);

        poseStack.mulPose(Axis.YP.rotationDegrees(-parent.getYRot()));
        poseStack.translate(this.getX() - parent.getX(), -(this.getY() - parent.getY()), -(this.getZ() - parent.getZ()) );

        poseStack.mulPose(Axis.YP.rotationDegrees(this.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-this.getXRot()));

        part.render(poseStack, vertexConsumer, packedLight, packedOverlay);

        poseStack.popPose();
    }
}
