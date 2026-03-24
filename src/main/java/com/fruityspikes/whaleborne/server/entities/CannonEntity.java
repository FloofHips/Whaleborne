package com.fruityspikes.whaleborne.server.entities;

import com.fruityspikes.whaleborne.client.menus.CannonMenu;
import com.fruityspikes.whaleborne.network.CannonFirePayload;
import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
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
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;

public class CannonEntity extends RideableWhaleWidgetEntity implements ContainerListener, HasCustomInventoryScreen, PlayerRideableJumping {
    protected float cannonXRot;
    private IItemHandler itemHandler;
    public SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
        }
    };

    public CannonEntity(EntityType<?> entityType, Level level) {
        super(entityType, level, WBItemRegistry.CANNON.get());
        cannonXRot = this.getXRot();
        this.inventory.addListener(this);
        this.itemHandler = new InvWrapper(this.inventory);
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

    public SoundEvent getDeathSound() { return SoundEvents.ANVIL_PLACE;}
    public BlockState getDeathBlock() { return Blocks.ANVIL.defaultBlockState();}

    @Override
    public void tick() {
        super.tick();
        Entity firstPassenger = this.getFirstPassenger();
        if(this.isVehicle() && firstPassenger != null){
            // Save current rotation as "old" for smooth rendering interpolation
            // (rotatePassengers skips cannon when ridden, so prevWidget fields must be updated here)
            this.prevWidgetYRot = this.getYRot();
            this.prevWidgetXRot = this.getXRot();

            this.setCannonXRot(Mth.rotLerp(0.1f, cannonXRot, firstPassenger.getXRot()));
            this.setYRot(Mth.rotLerp(0.1f, this.getYRot(), firstPassenger.getYRot()));
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        //if (!heldItem.isEmpty()) {
        //    if (isGunpowder(heldItem)) {
        //        return tryInsertItem(player, hand, heldItem, 1); // Gunpowder slot
        //    } else {
        //        return tryInsertItem(player, hand, heldItem, 0); // Ammo slot
        //    }
        //}
        if (player.isShiftKeyDown()) {
            openCannonMenu(player);
            return InteractionResult.SUCCESS;
        }
        else return super.interact(player, hand);
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
        } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
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
            PacketDistributor.sendToServer(new CannonFirePayload(this.getId(), power));
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

            Entity passenger = this.getFirstPassenger();
            if (passenger == null) return;

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

            Vec3 lookAngle = passenger.getLookAngle();
            Entity projectile = null;

            if(ammo.is(Items.ENDER_PEARL)){
                if (passenger instanceof LivingEntity livingPassenger) {
                    projectile = new ThrownEnderpearl(this.level(), livingPassenger);
                }
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDER_PEARL_THROW, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else if(ammo.is(WBItemRegistry.BARNACLE.get())){
                // Barnacle launches the passenger directly — no projectile entity.
                // ejectPassengers() may reset velocity, so we set delta AFTER and return
                // immediately to mirror the original behaviour (which crashed before reaching
                // the projectile block, effectively acting as an early return).
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

                if (this.getVehicle() != null)
                    this.getVehicle().push(-lookAngle.x * ((double) power / 200), 0, -lookAngle.z * ((double) power / 200));

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            WBParticleRegistry.SMOKE.get(),
                            this.getX(), this.getY() + 2, this.getZ(),
                            5, 0.1, 0.1, 0.1, 0.02
                    );
                }
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F,
                        power / 100 + (this.random.nextFloat() * 0.4F));
                return;
            }
            else if(ammo.getItem().asItem() instanceof BoatItem boatItem){
                String[] boatName = ammo.getItem().asItem().toString().split("_");
                StringBuilder boatTypeBuilder = new StringBuilder();
                boolean hasChest = false;
                for (int i = 0; i < boatName.length - 1; i++) {
                    if (!boatName[i].equals("chest")) {
                        if (boatTypeBuilder.length() > 0) {
                            boatTypeBuilder.append("_");
                        }
                        boatTypeBuilder.append(boatName[i]);
                    } else {
                        hasChest = true;
                    }
                }
                Boat boat = (Boat)(hasChest ? new ChestBoat(this.level(), this.getX(), this.getY(), this.getZ()) : new Boat(this.level(), this.getX(), this.getY(), this.getZ()));
                boat.setVariant(Boat.Type.byName(boatTypeBuilder.toString()));
                boat.setYRot(passenger.getYRot());
                passenger.startRiding(boat);

                projectile = boat;

                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else if(ammo.getItem() instanceof SpawnEggItem spawnEggItem){
                projectile = spawnEggItem.getType(ammo).create(this.level());
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else if(ammo.is(Items.TNT)){
                LivingEntity tntOwner = this.getVehicle() instanceof LivingEntity living ? living : null;
                projectile = new PrimedTnt(
                        this.level(), this.getX(), this.getY(), this.getZ(),
                        tntOwner);
                level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F,
                        (float) power / 50);
            }
            else if(ammo.is(Items.ARROW)){
                LivingEntity arrowOwner = this.getVehicle() instanceof LivingEntity living ? living : null;
                projectile = new Arrow(
                        this.level(), arrowOwner, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
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

            if (projectile != null) {
                projectile.setPos(this.position().add(0, 1, 0));
                projectile.setDeltaMovement(
                        lookAngle.x * ((double) power / 50),
                        lookAngle.y * ((double) power / 50),
                        lookAngle.z * ((double) power / 50)
                );
                this.level().addFreshEntity(projectile);
            }

            if (this.getVehicle() != null)
                this.getVehicle().push(-lookAngle.x * ((double) power / 200), 0, -lookAngle.z * ((double) power / 200));

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
            serverPlayer.openMenu(new MenuProvider() {
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
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0, this.getBbHeight() - 1.25f, 0);
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
        // No-Op. Inventory is created on initialization and should not be replaced.
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
                // Use robust saving logic matching HullbackEntity for 1.21+
                try {
                     net.minecraft.nbt.Tag savedTag = stack.save(this.registryAccess());
                     if (savedTag instanceof CompoundTag itemTag) {
                         itemTag.putByte("Slot", (byte)i);
                         items.add(itemTag);
                     }
                } catch (Exception e) {
                    // Log error if needed, but ensure we don't crash saving
                }
            }
        }
        tag.put("CannonItems", items);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        if (tag.contains("CannonItems")) {
            ListTag items = tag.getList("CannonItems", 10);
            
            for(int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < this.inventory.getContainerSize()) {
                    ItemStack stack = ItemStack.parse(this.registryAccess(), itemTag).orElse(ItemStack.EMPTY);
                    this.inventory.setItem(slot, stack);
                }
            }
        }
    }
    @Override
    public void openCustomInventoryScreen(Player player) {
        openCannonMenu(player);
    }
}
