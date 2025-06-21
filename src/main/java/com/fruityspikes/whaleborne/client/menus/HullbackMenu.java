package com.fruityspikes.whaleborne.client.menus;

import com.fruityspikes.whaleborne.server.entities.CannonEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.registries.WBMenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;

public class HullbackMenu extends AbstractContainerMenu {
    private final Container hullbackContainer;
    public final HullbackEntity hullback;

    public static HullbackMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
        int entityId = data.readInt();
        Level level = inv.player.level();
        Entity entity = level.getEntity(entityId);

        if (entity instanceof HullbackEntity hullback) {

            return new HullbackMenu(windowId, inv, hullback);
        }
        throw new IllegalStateException("Invalid hullback entity");
    }

    public HullbackMenu(int windowId, Inventory playerInventory, HullbackEntity hullback) {
        super(WBMenuRegistry.HULLBACK_MENU.get(), windowId);

        if (hullback.getInventory() == null) {
            System.out.println("Hullback inventory is null!");
        }
        this.hullback = hullback;
        this.hullbackContainer = hullback.getInventory();



        hullbackContainer.startOpen(playerInventory.player);

        this.addSlot(new Slot(hullbackContainer, HullbackEntity.INV_SLOT_CROWN, 152, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });

        this.addSlot(new Slot(hullbackContainer, HullbackEntity.INV_SLOT_SADDLE, 152,36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == Items.SADDLE;
            }

            @Override
            public boolean mayPickup(Player player) {
                return hullback.getArmorProgress() == 0;
            }
        });

        this.addSlot(new Slot(hullbackContainer, HullbackEntity.INV_SLOT_ARMOR, 152, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return player.isCreative();
            }
        });

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 2) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (itemstack1.getItem() == Items.SADDLE) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }
    public void removed(Player player) {
        super.removed(player);
        this.hullbackContainer.stopOpen(player);
    }
    public float getHealth(){
        return this.hullback.getHealth();
    }
    public float getMaxHealth(){
        return this.hullback.getMaxHealth();
    }
    public float getArmorProgress(){
        return this.hullback.getArmorProgress();
    }
    public float getSpeedModifier(){
        AttributeInstance inst = this.hullback.getAttribute(ForgeMod.SWIM_SPEED.get());
        if (inst != null){
            if(inst.getModifier(HullbackEntity.getSailSpeedModifierUuid()) != null)
                return (float) inst.getModifier(HullbackEntity.getSailSpeedModifierUuid()).getAmount();
            return 0;
        }
        return 0;
    }

    public String getName() { return this.hullback.getDisplayName().getString();}

    public boolean isVehicleAlive() {
        return this.hullback.isAlive();
    }
}
