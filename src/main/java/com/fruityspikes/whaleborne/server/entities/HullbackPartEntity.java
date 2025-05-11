package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class HullbackPartEntity extends Entity {
    public final HullbackEntity parent;
    public final String name;
    private final EntityDimensions size;
    public final Vec3 restingOffset;
    private double lerpAmount;
    private double wantedX;
    private double wantedY;
    private double wantedZ;
    private double wantedYRot;
    private double wantedXRot;

    public HullbackPartEntity(EntityType<? extends HullbackPartEntity> entityType, Level level) {
        super(entityType, level);
        this.size = EntityDimensions.scalable(2, 2);
        this.refreshDimensions();
        this.parent = null;
        this.name = null;
        this.restingOffset = null;
    }

    public HullbackPartEntity(HullbackEntity parent, Level level, String name, float width, float height, Vec3 restingOffset) {
        super(WBEntityRegistry.HULLBACK_PART.get(), level);
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.parent = parent;
        this.name = name;
        this.restingOffset = restingOffset;
        this.lerpAmount = 1;
    }

    public HullbackEntity getParent() {
        return this.parent;
    }

    public void tick() {
        super.tick();
    }
    public void lerpMoveTo(double x, double y, double z, float yRot, float xRot) {
        if(lerpAmount<=0)
            lerpAmount=1;

        Vec3 actualPos = new Vec3(
                Mth.lerp(lerpAmount, this.getX(), x),
                Mth.lerp(lerpAmount, this.getY(), y),
                Mth.lerp(lerpAmount, this.getZ(), z)
        );

        float actualYRot = (float) Mth.lerp(lerpAmount, this.getYRot(), yRot);
        float actualXRot = (float) Mth.lerp(lerpAmount, this.getXRot(), xRot);

        this.setPos(actualPos);
        this.setXRot(actualXRot);
        this.setYRot(actualYRot);
        lerpAmount -= 0.01;

        System.out.println();
    }
    public static boolean canVehicleCollide(Entity vehicle, Entity entity) {
        return (entity.canBeCollidedWith() || entity.isPushable()) && !vehicle.isPassengerOfSameVehicle(entity);
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
    public boolean canCollideWith(Entity entity) {
        return canVehicleCollide(this, entity);
    }
//    public Packet<ClientGamePacketListener> getAddEntityPacket() {
//        throw new UnsupportedOperationException();
//    }

    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
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
        //if(player.getItemInHand(hand).isEmpty() ){
        //    return player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        //}
        //return super.interact(player, hand);
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        } else {
            if (!this.level().isClientSide) {
                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            } else {
                return InteractionResult.SUCCESS;
            }
        }
    }

    public boolean shouldBeSaved() {
        return false;
    }

    public void render(PoseStack poseStack, MultiBufferSource buffer, float partialTicks, ResourceLocation texture, int packedLight, ModelPart part) {
        poseStack.pushPose();

        float X = (float) position().x;
        float Y = (float) position().y;
        float Z= (float) position().z;

        part.resetPose();
        part.setPos(0,0,0);

        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.translate(X - parent.getX(), - (Y - parent.getY()), -(Z - parent.getZ()));


        poseStack.mulPose(Axis.YP.rotationDegrees(this.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-this.getXRot()));
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
