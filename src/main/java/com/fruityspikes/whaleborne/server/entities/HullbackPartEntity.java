package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.server.items.WhaleEquipment;
import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;

public class HullbackPartEntity extends PartEntity<HullbackEntity> {
    public final HullbackEntity parent;
    public final String name;
    private final EntityDimensions size;

    public HullbackPartEntity(HullbackEntity parent, String name, float width, float height) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.parent = parent;
        this.name = name;
    }

    public void tick() {
        super.tick();
    }

    public EntityDimensions getSize() {
        return size;
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
    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        boolean topClicked = vec.y > size.height * 0.6f;
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.getItem() instanceof ShearsItem || heldItem.getItem() instanceof AxeItem) {
            return parent.interactClean(player, hand, this, topClicked);
        }

        if (heldItem.getItem() instanceof SaddleItem || heldItem.is(Items.DARK_OAK_PLANKS)) {
            return parent.interactArmor(player, hand, this, topClicked);
        }

        if (heldItem.isEmpty()){
            if(this.name == "tail")
                return parent.interact(player, hand);
            if(this.name == "fluke")
                return parent.interactRide(player, hand,6, null);
            if(topClicked){
                if(this.name == "body"){
                    Vec3 localClick = new Vec3(vec.x, 0, vec.z);

                    float inverseYaw = this.getYRot() * Mth.DEG_TO_RAD;
                    localClick = localClick.xRot(0).yRot(inverseYaw);
                    double angle = Math.atan2(localClick.z, localClick.x) + Math.PI;
                    int quadrant = (int)(angle / (Math.PI/2)) % 4;

                    switch(quadrant) {
                        case 0: return parent.interactRide(player, hand, 5, null);
                        case 1: return parent.interactRide(player, hand, 4, null);
                        case 2: return parent.interactRide(player, hand, 2, null);
                        default: return parent.interactRide(player, hand, 3, null);
                    }
                }
                if(this.name == "nose")
                    return parent.interactRide(player, hand,0, null);
                if(this.name == "head")
                    return parent.interactRide(player, hand,1, null);
            }
            return parent.interact(player, hand);
        }

        if ((heldItem.getItem() instanceof WhaleEquipment) || (heldItem.getItem() instanceof SpawnEggItem)){
            EntityType<?> entity;

            if(heldItem.getItem() instanceof SpawnEggItem spawnEggItem)
               entity = spawnEggItem.getType(null);
            else if(heldItem.getItem() instanceof WhaleEquipment whaleEquipment)
                entity = whaleEquipment.getEntity();
            else entity = EntityType.EXPERIENCE_ORB;

            if(this.name == "tail")
                return parent.interact(player, hand);
            if(this.name == "fluke")
                return parent.interactRide(player, hand,6, entity);
            if(topClicked){
                if(this.name == "body"){
                    Vec3 localClick = new Vec3(vec.x, 0, vec.z);

                    float inverseYaw = this.getYRot() * Mth.DEG_TO_RAD;
                    localClick = localClick.xRot(0).yRot(inverseYaw);
                    double angle = Math.atan2(localClick.z, localClick.x) + Math.PI;
                    int quadrant = (int)(angle / (Math.PI/2)) % 4;

                    switch(quadrant) {
                        case 0: return parent.interactRide(player, hand, 5, entity);
                        case 1: return parent.interactRide(player, hand, 4, entity);
                        case 2: return parent.interactRide(player, hand, 2, entity);
                        default: return parent.interactRide(player, hand, 3, entity);
                    }
                }
                if(this.name == "nose")
                    return parent.interactRide(player, hand,0, entity);
                if(this.name == "head")
                    return parent.interactRide(player, hand,1, entity);
            }
            return parent.interact(player, hand);
        }

        return parent.interact(player, hand);
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
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

}
