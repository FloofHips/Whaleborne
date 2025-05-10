package com.fruityspikes.whaleborne.server.entities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

    private Vec3 prevPos;
    private float prevYRot;
    private float prevXRot;
    public HullbackPartEntity(HullbackEntity parent, String name, float width, float height, Vec3 restingOffset) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.parent = parent;
        this.name = name;
        this.restingOffset = restingOffset;
    }
    public void tick() {
        this.prevPos = this.position();
        this.prevYRot = this.getYRot();
        this.prevXRot = this.getXRot();
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
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
    public void render(PoseStack poseStack, MultiBufferSource buffer, float partialTicks, ResourceLocation texture, int packedLight, ModelPart part) {
        poseStack.pushPose();

        float X = (float) position().x;
        float Y = (float) position().y;
        float Z= (float) position().z;
        if(prevPos!=null) {
            X = (float) Mth.lerp(partialTicks, prevPos.x, position().x);
            Y = (float) Mth.lerp(partialTicks, prevPos.y, position().y);
            Z = (float) Mth.lerp(partialTicks, prevPos.z, position().z);
        }

        float lerpedYRot = Mth.lerp(partialTicks, prevYRot, this.getYRot());
        float lerpedXRot = Mth.lerp(partialTicks, prevXRot, this.getXRot());


        part.resetPose();
        part.setPos(0,0,0);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(X - parent.getX(), - (Y - parent.getY()), -(Z - parent.getZ()));


        poseStack.mulPose(Axis.YP.rotationDegrees(lerpedYRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-lerpedXRot));
        poseStack.translate(0, -size.height/2, -size.width/2);

        boolean flag = parent.hurtTime > 0;
        part.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), packedLight, OverlayTexture.pack(0.0F, flag));
        poseStack.translate(0, size.height/2, size.width/2);
        renderDirt(poseStack, buffer, packedLight);
        poseStack.translate(0, -size.height-1, 0);
        renderDirt(poseStack, buffer, packedLight);
        poseStack.popPose();
    }
    public void renderDirt(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
//        Vec3 renderPos = this.position();
//        poseStack.translate(renderPos.x - parent.getX(), - (renderPos.y - parent.getY()), -(renderPos.z - parent.getZ()));
//
//        poseStack.mulPose(Axis.XP.rotationDegrees(-this.getXRot()));
//        poseStack.mulPose(Axis.YP.rotationDegrees(-this.getYRot()));
        if(Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    buffer.getBuffer(RenderType.lines()),
                    new AABB(-0.1, -0.1, -0.1, 0.1, 10, 0.1),
                    1, 0, 0, 1
            );
        }

        boolean flag = parent.hurtTime > 0;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.DARK_OAK_PLANKS.defaultBlockState(), poseStack, buffer, packedLight, OverlayTexture.pack(0.0F, flag));
    }
}
