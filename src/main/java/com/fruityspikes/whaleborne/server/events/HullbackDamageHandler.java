package com.fruityspikes.whaleborne.server.events;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.network.SeatLayoutPayload;
import com.fruityspikes.whaleborne.server.data.HullConfigManager;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.SimpleContainer;

@EventBusSubscriber(modid = Whaleborne.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HullbackDamageHandler {
    
    @SubscribeEvent
    public static void onHullbackDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof HullbackEntity hullback)) {
            return;
        }

        ItemStack armorStack = hullback.getInventory().getItem(HullbackEntity.INV_SLOT_ARMOR);
        if (armorStack.isEmpty()) {
            hullback.setMouthTarget(0);
            return;
        }

        // Get block chance from HullConfig (uses direct block_chance if set, else resistance/70)
        float blockChance = HullConfigManager.getBlockChance(armorStack.getItem());
        boolean blocked = hullback.getRandom().nextFloat() < blockChance;

        if (blocked) {
            // FULL BLOCK - does not consume armor, cancels damage
            event.setCanceled(true);
            hullback.setMouthTarget(0.3f);
            hullback.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + hullback.getRandom().nextFloat() * 0.4F);
            hullback.playSound(WBSoundRegistry.HULLBACK_HAPPY.get(), 1.0F, 0.8F + hullback.getRandom().nextFloat() * 0.4F);
            hullback.updateContainerEquipment();
        } else {
            // DID NOT BLOCK - consumes armor and reduces damage
            hullback.setMouthTarget(0.8f);
            
            float amount = event.getAmount();
            int armorDamage = Math.min(armorStack.getCount(), (int)Math.ceil(amount));
            
            armorStack.shrink(armorDamage);
            hullback.getInventory().setItem(HullbackEntity.INV_SLOT_ARMOR, armorStack);
            hullback.getEntityData().set(HullbackEntity.DATA_ARMOR, armorStack);
            
            hullback.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + hullback.getRandom().nextFloat() * 0.4F);
            hullback.playSound(WBSoundRegistry.HULLBACK_MAD.get());
            
            float remainingDamage = Math.max(0, amount - armorDamage);
            event.setAmount(remainingDamage);
            
            hullback.updateContainerEquipment();
        }
    }

    /** Sync the current seat layout when a player starts tracking a Hullback
     *  (covers mid-game joins and newly loaded chunks). */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof HullbackEntity hullback && event.getEntity() instanceof ServerPlayer player) {
            var layout = hullback.partManager.getSeatLayout();
            PacketDistributor.sendToPlayer(player,
                    new SeatLayoutPayload(
                            hullback.getId(), layout.getAllSeatDefs(), layout.getFlukeSeatIndex()));
        }
    }
}
