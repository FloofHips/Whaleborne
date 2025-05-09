package com.fruityspikes.whaleborne.server.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.UUID;

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
        Vec3 partOffset = this.position().subtract(parent.position());
        poseStack.mulPose(Axis.YP.rotationDegrees(-parent.getYRot()));
        poseStack.translate(partOffset.x, -partOffset.y, -partOffset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(this.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-this.getXRot()));
        float xPivot = 0;
        float yPivot = -size.height / 2;
        float zPivot = -size.width / 2;
        poseStack.translate(xPivot, yPivot, zPivot);
        part.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, ModelPart part) {
        poseStack.pushPose();
        Vec3 partOffset = this.position().subtract(parent.position());
        poseStack.scale(-1, -1, -1);
        poseStack.translate(-partOffset.x, -partOffset.y, -partOffset.z);

        poseStack.mulPose(Axis.YP.rotationDegrees(-this.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-this.getXRot()));

        poseStack.translate(0, -this.size.height, -this.size.width);
        //poseStack.translate(0, this.size.height/2, 0);
        part.render(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    public void renderDirt(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        Vec3 partOffset = this.position().subtract(parent.position());
        poseStack.translate(0.5, 0, 0.5);
        poseStack.translate(partOffset.x, partOffset.y, partOffset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-this.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-this.getXRot()));
        poseStack.translate(0, this.size.height, 0);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.DARK_OAK_PLANKS.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
