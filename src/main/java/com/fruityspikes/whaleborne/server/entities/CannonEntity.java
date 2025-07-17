package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.client.menus.CannonMenu;
import com.fruityspikes.whaleborne.network.CannonFirePacket;
import com.fruityspikes.whaleborne.network.WhaleborneNetwork;
import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Matrix2dc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CannonEntity extends RideableWhaleWidgetEntity implements ContainerListener, HasCustomInventoryScreen, PlayerRideableJumping {
    protected float cannonXRot;
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();
    public SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
        }
    };

    private final Map<Item, EntityType> itemToProjectileMap = new HashMap<>();

    public CannonEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.CANNON.get());
        cannonXRot = this.getXRot();
        this.inventory.addListener(this);
        this.itemHandler = LazyOptional.of(() -> new InvWrapper(this.inventory));

        itemToProjectileMap.put(Items.ENDER_PEARL, EntityType.ENDER_PEARL);
        itemToProjectileMap.put(Items.ARROW, EntityType.ARROW);
        itemToProjectileMap.put(Items.SPECTRAL_ARROW, EntityType.SPECTRAL_ARROW);
        itemToProjectileMap.put(Items.POTION, EntityType.POTION);
        itemToProjectileMap.put(Items.TRIDENT, EntityType.TRIDENT);
        itemToProjectileMap.put(Items.TNT, EntityType.TNT);

    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            Containers.dropContents(this.level(), this.blockPosition(), this.inventory);
        }

        super.remove(reason);
    }

    public void setCannonXRot(float cannonXRot) {
        this.cannonXRot = cannonXRot;
    }

    public float getCannonXRot() {
        return cannonXRot;
    }

    @Override
    public void tick() {
        super.tick();
        if(this.isVehicle()){
            //this.setCannonXRot(this.getFirstPassenger().getXRot());
            this.setCannonXRot(Mth.rotLerp(0.1f, cannonXRot, this.getFirstPassenger().getXRot()));
            this.setYRot(Mth.rotLerp(0.1f, this.getYRot(), this.getFirstPassenger().getYRot()));
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (!heldItem.isEmpty()) {
            if (isGunpowder(heldItem)) {
                return tryInsertItem(player, hand, heldItem, 1); // Gunpowder slot
            } else {
                return tryInsertItem(player, hand, heldItem, 0); // Ammo slot
            }
        }
//        else {
//            openCannonMenu(player);
//            return InteractionResult.SUCCESS;
//        }
        return super.interact(player, hand);
    }

    private boolean isGunpowder(ItemStack stack) {
        return stack.getItem() == Items.GUNPOWDER;
    }

    private InteractionResult tryInsertItem(Player player, InteractionHand hand, ItemStack stack, int slot) {
        ItemStack existing = inventory.getItem(slot);

        if (existing.isEmpty()) {
            inventory.setItem(slot, stack.split(1));
            playSound(SoundEvents.ITEM_FRAME_ADD_ITEM);
            return InteractionResult.SUCCESS;
        } else if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
            int toAdd = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(toAdd);
            stack.shrink(toAdd);
            playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1, (float) existing.getCount() /existing.getMaxStackSize());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Override
    public void onPlayerJump(int power) {
        if (this.level().isClientSide) {
            WhaleborneNetwork.INSTANCE.sendToServer(new CannonFirePacket(this.getId(), power));
        }
    }

    public void fireCannon(int power) {

            ItemStack gunpowder = inventory.getItem(1);

            if (gunpowder.isEmpty()) {
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.NEUTRAL, 1.0F, 0.0F);
                return;
            }

            gunpowder.shrink(1);
            ItemStack ammo = inventory.getItem(0).copy().split(1);

            if (ammo.isEmpty()) {
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.NEUTRAL, 1.0F, 0.0F);
                return;
            } else {
                inventory.getItem(0).shrink(1);
            }

            Vec3 lookAngle = this.getFirstPassenger().getLookAngle();
            Entity projectile = null;

            if(ammo.is(Items.ENDER_PEARL)){
                projectile = new ThrownEnderpearl(
                        this.level(),
                        (LivingEntity) this.getFirstPassenger());
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_PEARL_THROW, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else if(ammo.is(WBItemRegistry.BARNACLE.get())){
                Entity passenger = this.getFirstPassenger();
                if (passenger != null) {
                    this.ejectPassengers();

                    passenger.setDeltaMovement(
                            lookAngle.x * ((double) power),
                            lookAngle.y * ((double) power),
                            lookAngle.z * ((double) power)
                    );
                    passenger.hurtMarked = true;
                    passenger.setPose(Pose.CROUCHING);
                    level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            WBSoundRegistry.ORGAN.get(), SoundSource.BLOCKS, 1.0F,
                            (float) power / 50);
                    level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F,
                            (float) power / 50);
                }
            }
            else if(ammo.getItem() instanceof SpawnEggItem spawnEggItem){
                projectile = spawnEggItem.getType(null).create(this.level());
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else if(ammo.is(Items.TNT)){
                projectile = new PrimedTnt(
                        this.level(), this.getX(), this.getY(), this.getZ(),
                        (LivingEntity) this.getVehicle());
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else if(ammo.is(Items.FIRE_CHARGE)){
                projectile = new LargeFireball(
                        this.level(), (LivingEntity) this.getVehicle(), this.getX(), this.getY(), this.getZ(), power);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else if(ammo.is(Items.ARROW)){
                projectile = new Arrow(
                        this.level(), (LivingEntity) this.getVehicle());
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else {
                projectile = new ItemEntity(
                        this.level(), this.getX(), this.getY(), this.getZ(),
                        ammo);
                ((ItemEntity) projectile).setPickUpDelay(10);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }

            projectile.setPos(this.position().add(0, 1, 0));
            projectile.setDeltaMovement(
                    lookAngle.x * ((double) power / 50),
                    lookAngle.y * ((double) power / 50),
                    lookAngle.z * ((double) power / 50)
            );

            this.getVehicle().push(-lookAngle.x * ((double) power / 200), 0, -lookAngle.z * ((double) power / 200));

            //tnt.setPickUpDelay(20);
            this.level().addFreshEntity(projectile);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        WBParticleRegistry.SMOKE.get(),
                        this.getX(),
                        this.getY() + 2,
                        this.getZ(),
                        5,
                        0.1,
                        0.1,
                        0.1,
                        0.02
                );
            }
            level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F,
                    power / 100 + (this.random.nextFloat() * 0.4F));
    }

    private void openCannonMenu(Player player) {
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("menu.title.whaleborne.cannon");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                    return new CannonMenu(windowId, playerInventory, CannonEntity.this);
                }
            }, buf -> buf.writeInt(this.getId()));
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight();
    }

    @Override
    public boolean canJump() {
        return true;
    }

    @Override
    public void handleStartJump(int i) {
        //this.playSound(SoundEvents.BLAZE_SHOOT, (float) i /50, (float) i /50);
    }

    @Override
    public void handleStopJump() {
        //this.playSound(SoundEvents.BLAZE_SHOOT);

    }
    @Override
    public void containerChanged(Container container) {

    }
    protected void createInventory() {
        SimpleContainer simplecontainer = this.inventory;
        this.inventory = new SimpleContainer(this.getInventorySize());
        if (simplecontainer != null) {
            simplecontainer.removeListener(this);
            int i = Math.min(simplecontainer.getContainerSize(), this.inventory.getContainerSize());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = simplecontainer.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.inventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.inventory.addListener(this);
    }
    protected int getInventorySize() {
        return 2;
    }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag items = new ListTag();

        for(int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack stack = this.inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte)i);
                stack.save(itemTag);
                items.add(itemTag);
            }
        }
        tag.put("Items", items);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ListTag items = tag.getList("Items", 10);

        for(int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                this.inventory.setItem(slot, ItemStack.of(itemTag));
            }
        }
    }
    @Override
    public void openCustomInventoryScreen(Player player) {
        openCannonMenu(player);
    }
}
