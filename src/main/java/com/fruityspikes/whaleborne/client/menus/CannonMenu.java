package com.fruityspikes.whaleborne.client.menus;

import com.fruityspikes.whaleborne.server.entities.CannonEntity;
import com.fruityspikes.whaleborne.server.registries.WBMenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class CannonMenu extends AbstractContainerMenu {
    private final Container cannonContainer;
    private final CannonEntity cannon;

    public static CannonMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
        int entityId = data.readInt();
        Level level = inv.player.level();
        Entity entity = level.getEntity(entityId);

        if (entity instanceof CannonEntity cannon) {
            return new CannonMenu(windowId, inv, cannon);
        }
        throw new IllegalStateException("Invalid cannon entity");
    }

    public CannonMenu(int windowId, Inventory playerInventory, CannonEntity cannon) {
        super(WBMenuRegistry.CANNON_MENU.get(), windowId);

        this.cannon = cannon;
        this.cannonContainer = cannon.inventory;

        cannonContainer.startOpen(playerInventory.player);

        this.addSlot(new Slot(cannon.inventory, 0, 79, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });

        this.addSlot(new Slot(cannon.inventory, 1, 79, 51) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == Items.GUNPOWDER;
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
        return player.distanceToSqr(cannon) < 8.0 * 8.0;
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
                if (itemstack1.getItem() == Items.GUNPOWDER) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
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
        this.cannonContainer.stopOpen(player);
    }
}
