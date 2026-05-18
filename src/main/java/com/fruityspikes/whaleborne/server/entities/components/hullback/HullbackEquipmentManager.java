package com.fruityspikes.whaleborne.server.entities.components.hullback;

import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Armor, crown, saddle, and equipment synchronization for the Hullback. */
public class HullbackEquipmentManager {
    private final HullbackEntity hullback;

    // Equipment slot constants (from HullbackEntity)
    private static final int INV_SLOT_SADDLE = 1;
    private static final int INV_SLOT_ARMOR = 2;
    private static final int INV_SLOT_CROWN = 0;

    public HullbackEquipmentManager(HullbackEntity hullback) {
        this.hullback = hullback;
    }

    public ItemStack getArmor() {
        return hullback.getEntityData().get(HullbackEntity.DATA_ARMOR);
    }

    public ItemStack getCrown() {
        return hullback.getEntityData().get(HullbackEntity.DATA_CROWN_ID);
    }

    /** Equips a saddle; plays the equip sound when source is non-null. */
    public void equipSaddle(ItemStack stack, @Nullable SoundSource source) {
        hullback.inventory.setItem(INV_SLOT_SADDLE, stack);
        if (source != null) {
            hullback.level().playSound(null, hullback, SoundEvents.HORSE_SADDLE, source, 0.5F, 1.0F);
        }
    }

    /** Copies armor/crown/saddle from inventory into entity data and syncs on the server. */
    public void updateContainerEquipment() {
        ItemStack crown = hullback.inventory.getItem(INV_SLOT_CROWN);
        ItemStack armor = hullback.inventory.getItem(INV_SLOT_ARMOR);
        ItemStack saddle = hullback.inventory.getItem(INV_SLOT_SADDLE);
        boolean hasSaddle = !saddle.isEmpty();

        // Create copies to avoid reference problems
        hullback.getEntityData().set(HullbackEntity.DATA_CROWN_ID, crown.isEmpty() ? ItemStack.EMPTY : crown.copy());
        hullback.getEntityData().set(HullbackEntity.DATA_ARMOR, armor.isEmpty() ? ItemStack.EMPTY : armor.copy());
        hullback.setFlag(4, hasSaddle);

        // Sync immediately if on server
        if (!hullback.level().isClientSide) {
            hullback.sendHurtSyncPacket();
        }
    }

    /** Inventory-change hook: plays saddle/armor sounds and (re)applies hull config. */
    public void containerChanged(Container invBasic) {
        ItemStack previousArmor = getArmor();
        boolean wasSaddled = hullback.isSaddled();
        
        updateContainerEquipment();
        
        ItemStack currentArmor = getArmor();
        
        // Play saddle sound if just equipped
        if (hullback.tickCount > 20 && !wasSaddled && hullback.isSaddled()) {
            hullback.playSound(hullback.getSaddleSoundEvent(), 0.5F, 1.0F);
        }
        
        // Play armor sound if armor changed
        if (hullback.tickCount > 20 && previousArmor != currentArmor) {
            hullback.playSound(SoundEvents.HORSE_ARMOR, 0.5F, 1.0F);
        }

        // Apply hull config (speed modifier + seat layout) based on armor material
        if (!currentArmor.isEmpty()) {
            hullback.applyHullConfig(currentArmor.getItem());
        } else {
            hullback.removeHullConfig();
        }
    }

    /** Forces an immediate equipment sync to clients. */
    public void forceEquipmentSync() {
        if (!hullback.level().isClientSide) {
            hullback.sendHurtSyncPacket();
        }
    }
}
