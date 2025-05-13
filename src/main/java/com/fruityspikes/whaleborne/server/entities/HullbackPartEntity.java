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
import net.minecraft.world.entity.animal.Animal;
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

    private Vec3 prevPos;
    private float prevYRot;
    private float prevXRot;
    private Vec3 targetPosition;
    private float targetYRot;
    private float targetXRot;
    private float lerpProgress;
    private float lerpSpeed;

    public HullbackPartEntity(HullbackEntity parent, String name, float width, float height, float dragFactor) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.parent = parent;
        this.name = name;
        this.lerpSpeed = 1.0f - dragFactor;
    }

    public void setTargetPosition(double x, double y, double z, float targetYRot, float targetXRot) {
        Vec3 target = new Vec3(x, y ,z);
        if (!target.equals(this.targetPosition)) {
            this.targetPosition = target;
            this.targetYRot = targetYRot;
            this.targetXRot = targetXRot;
            //this.lerpProgress = 0.0f;
        }
    }

    public void tick() {
        super.tick();

//        lerpProgress+=0.1;
//
//        if(lerpProgress>=1)
//            lerpProgress = 1;
//        if(targetPosition!=null && targetPosition.distanceTo(this.position())<=0)
//            lerpProgress = 0;

        //if (lerpProgress < 1.0f) {

        //lerpProgress = Math.min(1.0f, lerpProgress + lerpSpeed * 0.1f);

//        double x = Mth.lerp(lerpProgress, this.getX(), targetPosition.x);
//        double y = Mth.lerp(lerpProgress, this.getY(), targetPosition.y);
//        double z = Mth.lerp(lerpProgress, this.getZ(), targetPosition.z);

//        this.setYRot(Mth.lerp(lerpProgress, this.getYRot(), targetYRot));
//        this.setXRot(Mth.lerp(lerpProgress, this.getXRot(), targetXRot));

//        this.setYRot(targetYRot);
//        this.setXRot(targetXRot);
//        this.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
//        } else {
//            this.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
//            this.setYRot(targetYRot);
//            this.setXRot(targetXRot);
//        }
    }

    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (this.hasPassenger(passenger)) {
            float f = 0;
            float f1 = (float)((this.isRemoved() ? 0.009999999776482582 : this.getPassengersRidingOffset()) + passenger.getMyRidingOffset());
            if (this.getPassengers().size() > 1) {
                int i = this.getPassengers().indexOf(passenger);
                if (i == 0) {
                    f = 0.2F;
                } else {
                    f = -0.6F;
                }

                if (passenger instanceof Animal) {
                    f += 0.2F;
                }
            }

            Vec3 vec3 = (new Vec3((double)f, 0.0, 0.0)).yRot(-this.getYRot() * 0.017453292F - 1.5707964F);
            callback.accept(passenger, this.getX() + vec3.x, this.getY() + (double)f1, this.getZ() + vec3.z);
            passenger.setYRot(passenger.getYRot() + this.getYRot());
            passenger.setYHeadRot(passenger.getYHeadRot() + this.getYRot());

            if (passenger instanceof Animal && this.getPassengers().size() == this.getMaxPassengers()) {
                int j = passenger.getId() % 2 == 0 ? 90 : 270;
                passenger.setYBodyRot(((Animal)passenger).yBodyRot + (float)j);
                passenger.setYHeadRot(passenger.getYHeadRot() + (float)j);
            }
        }
    }
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.getMaxPassengers();
    }
    protected int getMaxPassengers() {
        return 2;
    }
    public double getPassengersRidingOffset() {
        return size.height;
    }
    public boolean dismountsUnderwater() {
        return false;
    }
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.isVehicle()) {
            player.startRiding(this);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.interact(player, hand);
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
//    public Packet<ClientGamePacketListener> getAddEntityPacket() {
//        throw new UnsupportedOperationException();
//    }

    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

//    public boolean shouldBeSaved() {
//        return false;
//    }

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

//        System.out.println(xo);
//        System.out.println(this.getX());
//
//        float X = (float) Mth.lerp(partialTicks, xo, position().x);
//        float Y = (float) Mth.lerp(partialTicks, yo, position().y);
//        float Z = (float) Mth.lerp(partialTicks, zo, position().z);
//
//        System.out.println(X);

        float lerpedYRot = Mth.lerp(partialTicks, yRotO, this.getYRot());
        float lerpedXRot = Mth.lerp(partialTicks, xRotO, this.getXRot());

        part.resetPose();
        part.setPos(0,0,0);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(this.getPosition(partialTicks).x - parent.getPosition(partialTicks).x,
                - (this.getPosition(partialTicks).y - parent.getPosition(partialTicks).y),
                -(this.getPosition(partialTicks).z - parent.getPosition(partialTicks).z));


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
