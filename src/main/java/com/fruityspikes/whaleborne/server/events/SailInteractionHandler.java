package com.fruityspikes.whaleborne.server.events;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.entities.SailEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Whaleborne.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SailInteractionHandler {

    @SubscribeEvent
    public static void onSailInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Entity target = event.getTarget();
        if (!(target instanceof SailEntity sail)) return;
        if (sail.isPassenger()) return;

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());

        // Water bucket removes banner
        if (held.is(Items.WATER_BUCKET) && !sail.getBanner().isEmpty()) {
            if (!sail.level().isClientSide) {
                sail.spawnAtLocation(sail.getBanner());
                sail.getEntityData().set(SailEntity.DATA_BANNER, ItemStack.EMPTY);

                sail.level().playSound(null, sail.getX(), sail.getY(), sail.getZ(),
                        SoundEvents.AMBIENT_UNDERWATER_EXIT,
                        SoundSource.PLAYERS, 1.0F, sail.getRandom().nextFloat() * 0.5f + 0.5f);
                sail.level().playSound(null, sail.getX(), sail.getY(), sail.getZ(),
                        SoundEvents.ARMOR_EQUIP_LEATHER,
                        SoundSource.PLAYERS, 1.0F, sail.getRandom().nextFloat() * 0.5f + 0.5f);
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(sail.level().isClientSide));
            event.setCanceled(true);
            return;
        }

        // Banner attachment
        if (held.is(ItemTags.BANNERS)) {
            if (!sail.level().isClientSide) {
                if (!sail.getBanner().isEmpty()) {
                    sail.spawnAtLocation(sail.getBanner());
                }
                ItemStack bannerStack = held.copy();
                bannerStack.setCount(1);
                sail.getEntityData().set(SailEntity.DATA_BANNER, bannerStack);

                sail.level().playSound(null, sail.getX(), sail.getY(), sail.getZ(),
                        SoundEvents.ARMOR_EQUIP_LEATHER,
                        SoundSource.PLAYERS, 1.0F, sail.getRandom().nextFloat() * 0.5f + 0.5f);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(sail.level().isClientSide));
            event.setCanceled(true);
            return;
        }

        // Orientation change — server-only; entity tracker syncs to client.
        if (!sail.level().isClientSide) {
            if (player.isShiftKeyDown()) {
                sail.setYRot(sail.getYRot() - 11.25f);
            } else {
                sail.setYRot(sail.getYRot() + 11.25f);
            }
            sail.playSound(SoundEvents.WOOD_HIT, 1.0F, 1.0F);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(sail.level().isClientSide));
        event.setCanceled(true);
    }
}
