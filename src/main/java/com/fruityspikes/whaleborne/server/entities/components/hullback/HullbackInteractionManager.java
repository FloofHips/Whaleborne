package com.fruityspikes.whaleborne.server.entities.components.hullback;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.data.HullbackDirtManager;
import com.fruityspikes.whaleborne.server.entities.HullbackEntity;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import com.fruityspikes.whaleborne.server.registries.WBTagRegistry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages player interactions with the Hullback entity.
 * Handles riding, equipment placement, cleaning, and debug interactions.
 */
public class HullbackInteractionManager {
    private final HullbackEntity hullback;

    public HullbackInteractionManager(HullbackEntity hullback) {
        this.hullback = hullback;
    }

    /**
     * Main interaction handler.
     * Currently delegates to parent class.
     * @param player The interacting player
     * @param hand The hand used for interaction
     * @return The interaction result
     */
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /**
     * Debug interaction for development/testing.
     * Tames the Hullback and prints seat data.
     * @param player The interacting player
     * @param hand The hand used for interaction
     * @return The interaction result
     */
    public InteractionResult interactDebug(Player player, InteractionHand hand) {
        if (!hullback.isTamed()) {
            hullback.setTamed(true);
            hullback.setPersistenceRequired();
            return InteractionResult.SUCCESS;
        }

        // Debug output removed for production

        return InteractionResult.SUCCESS;
    }

    /**
     * Handles riding interaction for a specific seat.
     * @param player The player attempting to ride
     * @param hand The hand used for interaction
     * @param seatIndex The seat index (0-6)
     * @param entityType Optional entity type to place as equipment
     * @return The interaction result
     */
    public InteractionResult interactRide(Player player, InteractionHand hand, int seatIndex, @Nullable EntityType<?> entityType) {
        // Validate seat index
        if (seatIndex < 0 || seatIndex >= 7) {
            return InteractionResult.FAIL;
        }

        // Check armor progress
        if (hullback.getArmorProgress() < 0.5) {
            return InteractionResult.FAIL;
        }

        // Check if saddled
        if (!hullback.isSaddled()) {
            hullback.setMouthTarget(1.0f);
            hullback.playSound(WBSoundRegistry.HULLBACK_MAD.get());
            return InteractionResult.PASS;
        }

        // Check if seat is occupied
        Optional<UUID> currentSeatOccupant = hullback.getEntityData().get(hullback.getSeatAccessor(seatIndex));
        if (currentSeatOccupant.isPresent()) {
            if (currentSeatOccupant.get().equals(player.getUUID())) {
                return InteractionResult.PASS;
            }
            return InteractionResult.FAIL;
        }

        // Place equipment or mount player
        if (entityType != null) {
            return placeEquipment(player, hand, seatIndex, entityType);
        }

        return mountPlayer(player, seatIndex);
    }

    /**
     * Places equipment (cannon, mast, etc.) on a seat.
     * @param player The player placing the equipment
     * @param hand The hand holding the equipment
     * @param seatIndex The seat index
     * @param entityType The type of equipment entity
     * @return The interaction result
     */
    private InteractionResult placeEquipment(Player player, InteractionHand hand, int seatIndex, EntityType<?> entityType) {
        Entity entity = entityType.create(hullback.level());
        if (entity == null) {
            return InteractionResult.FAIL;
        }

        Vec3 seatPos = hullback.partManager.seats[seatIndex];
        entity.moveTo(seatPos.x, seatPos.y + 1, seatPos.z, hullback.getYRot(), 0);
        hullback.level().addFreshEntity(entity);

        if (entity.startRiding(hullback, true)) {
            hullback.assignSeat(seatIndex, entity);
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Mounts a player on a specific seat.
     * @param player The player to mount
     * @param seatIndex The seat index
     * @return The interaction result
     */
    private InteractionResult mountPlayer(Player player, int seatIndex) {
        // Dismount player from any other seat first
        for (int i = 0; i < 7; i++) {
            Optional<UUID> occupant = hullback.getEntityData().get(hullback.getSeatAccessor(i));
            if (occupant.isPresent() && occupant.get().equals(player.getUUID())) {
                player.stopRiding();
            }
        }

        if (player.startRiding(hullback, true)) {
            hullback.assignSeat(seatIndex, player);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.FAIL;
        }
    }

    /**
     * Handles cleaning interaction (removing vegetation).
     * @param player The player cleaning
     * @param hand The hand used
     * @param part The Hullback part being cleaned
     * @param top Whether cleaning the top or bottom
     * @return The interaction result
     */
    public InteractionResult interactClean(Player player, InteractionHand hand, HullbackPartEntity part, Boolean top) {
        hullback.setMouthTarget(0.2f);

        InteractionResult removalResult = this.handleVegetationRemoval(player, hand, part, top);
        if (removalResult == InteractionResult.PASS) {
            // No dirt was removed — check if taming conditions are met
            boolean hasTopDirt = false;
            for (BlockState[] states : hullback.hullbackDirt.headTopDirt) {
                for (BlockState state : states) {
                    if (state != Blocks.AIR.defaultBlockState()) {
                        hasTopDirt = true;
                        break;
                    }
                }
                if (hasTopDirt) break;
            }
            if (!hasTopDirt) {
                for (BlockState[] blockStates : hullback.hullbackDirt.bodyTopDirt) {
                    for (BlockState blockState : blockStates) {
                        if (blockState != Blocks.AIR.defaultBlockState()) {
                            hasTopDirt = true;
                            break;
                        }
                    }
                    if (hasTopDirt) break;
                }
            }

            if (hasTopDirt) {
                // Whale still has dirt — reject interaction (mad)
                hullback.setMouthTarget(0.5f);
                hullback.playSound(WBSoundRegistry.HULLBACK_MAD.get());
                return InteractionResult.SUCCESS;
            }

            if (hullback.isTamed()) {
                // Already tamed and clean — let other handlers process the item
                return InteractionResult.PASS;
            }

            // All clean! Tame the Hullback
            hullback.setTamed(true);
            hullback.setPersistenceRequired();
            hullback.setMouthTarget(0.1f);
            hullback.playSound(WBSoundRegistry.HULLBACK_HAPPY.get());

            // Spawn heart particles
            for (int side : new int[]{-1, 1}) {
                Vec3 particlePos = hullback.getPartManager().partPosition[1].add(new Vec3(4 * side, 2, 0).yRot(hullback.getPartManager().partYRot[1]));
                double x = particlePos.x;
                double y = particlePos.y;
                double z = particlePos.z;

                if (hullback.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, x, y, z, 10, 0.5, 0.5, 0.5, 0.02);
                }
            }

            return InteractionResult.SUCCESS;
        }

        // Dirt was successfully removed
        return InteractionResult.SUCCESS;
    }

    public InteractionResult handleVegetationRemoval(Player player, InteractionHand hand, HullbackPartEntity part, boolean top) {
        BlockState[][] dirtArray = hullback.hullbackDirt.getDirtArrayForPart(part, top);
        ItemStack held = player.getItemInHand(hand);

        for (int x = 0; x < dirtArray.length; x++) {
            for (int y = 0; y < dirtArray[x].length; y++) {
                BlockState state = dirtArray[x][y];
                if (state == null || state.isAir()) continue;

                HullbackDirtManager.HullbackDirtEntry entry = HullbackDirtManager.DATA.stream()
                        .filter(e -> e.matches(state)).findFirst().orElse(null);
                if (entry == null) continue;

                boolean removable = entry.removableWith().contains("any") || entry.removableWith().contains(getToolType(held));
                if (!removable) continue;

                if (hullback.level() instanceof ServerLevel serverLevel) {
                    float yOffset = top ? 5 : -1;

                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, HullbackDirt.applyProperties(entry.block(), entry.blockProperties())),
                            hullback.position().x, hullback.position().y + yOffset, hullback.position().z,
                            60, 3, 0.2, -3, 0);

                    dirtArray[x][y] = Blocks.AIR.defaultBlockState();

                    if (entry.drop() != Items.AIR) {
                        for (int i = 0; i < entry.dropAmount(); i++) {
                            ItemStack dropStack = new ItemStack(entry.drop());
                            double px = part.getX() + y - part.getBbWidth() / 2.0;
                            double py = part.getY() + (top ? part.getBbHeight() + 0.5f : -0.5f);
                            double pz = part.getZ() - x + part.getBbWidth() / 2.0;
                            hullback.level().addFreshEntity(new ItemEntity(hullback.level(), px, py, pz, dropStack));
                        }
                    }

                    if (!player.isCreative()) {
                        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        player.getItemInHand(hand).hurtAndBreak(1, player, slot);
                    }

                    hullback.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            entry.soundOnRemove() != null ? entry.soundOnRemove() : SoundEvents.SHEEP_SHEAR,
                            SoundSource.PLAYERS, 1.0F, 1.0f);

                    hullback.setMouthTarget(1.0f);
                    hullback.hullbackDirt.syncDirtToClients();
                } else {
                    dirtArray[x][y] = Blocks.AIR.defaultBlockState();
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    public InteractionResult interactArmor(Player player, InteractionHand hand, HullbackPartEntity part, Boolean top) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.getItem() instanceof SaddleItem) {
            return handleSaddleEquip(player, hand, heldItem);
        } else if (heldItem.is(WBTagRegistry.HULLBACK_EQUIPPABLE)) {
            return handleArmorEquip(player, hand, heldItem);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult handleSaddleEquip(Player player, InteractionHand hand, ItemStack heldItem) {
        if (hullback.isSaddled()) return InteractionResult.PASS;

        if (!hullback.isTamed()) {
            rejectWithMad();
            return InteractionResult.PASS;
        }

        ItemStack saddleToEquip = heldItem.copy();
        saddleToEquip.setCount(1);
        hullback.equipSaddle(saddleToEquip, SoundSource.PLAYERS);
        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }
        hullback.level().playSound(null, hullback.getX(), hullback.getY(), hullback.getZ(),
                SoundEvents.HORSE_SADDLE, SoundSource.PLAYERS, 1.0F, 0.1F);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleArmorEquip(Player player, InteractionHand hand, ItemStack heldItem) {
        if (!hullback.isSaddled()) {
            rejectWithMad();
            return InteractionResult.PASS;
        }

        ItemStack currentArmor = hullback.getInventory().getItem(HullbackEntity.INV_SLOT_ARMOR);

        if (currentArmor.getCount() >= currentArmor.getMaxStackSize()) {
            rejectWithMad();
            return InteractionResult.PASS;
        }

        if (currentArmor.isEmpty()) {
            hullback.getInventory().setItem(HullbackEntity.INV_SLOT_ARMOR, new ItemStack(heldItem.getItem(), 1));
            hullback.updateContainerEquipment();
        } else if (heldItem.getItem() == currentArmor.getItem()) {
            if (player.isCreative()) {
                currentArmor.setCount(64);
            } else {
                currentArmor.grow(1);
            }
            hullback.getEntityData().set(HullbackEntity.DATA_ARMOR, currentArmor.copy());
        } else {
            rejectWithMad();
            return InteractionResult.PASS;
        }

        if (!player.getAbilities().instabuild) {
            heldItem.shrink(1);
        }

        hullback.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        hullback.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS,
                1.0F, 0.5f + ((float) currentArmor.getCount() / 64));

        if (currentArmor.getCount() == 64) {
            hullback.level().playSound(null, hullback.getX(), hullback.getY(), hullback.getZ(),
                    SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 2.0F, 1.0F);
            hullback.playSound(WBSoundRegistry.HULLBACK_TAME.get());
        }

        hullback.updateContainerEquipment();
        return InteractionResult.SUCCESS;
    }

    /** Plays the rejection animation and sound. */
    private void rejectWithMad() {
        hullback.setMouthTarget(0.3f);
        hullback.playSound(WBSoundRegistry.HULLBACK_MAD.get());
    }

    private String getToolType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "hand";
        Item item = stack.getItem();
        if (item instanceof ShearsItem) return "shears";
        if (item instanceof AxeItem || stack.is(ItemTags.AXES)) return "axe";
        if (item instanceof PickaxeItem || stack.is(ItemTags.PICKAXES)) return "pickaxe";
        if (item instanceof HoeItem || stack.is(ItemTags.HOES)) return "hoe";
        if (item instanceof ShovelItem || stack.is(ItemTags.SHOVELS)) return "shovel";
        if (item instanceof SwordItem || stack.is(ItemTags.SWORDS)) return "sword";
        return "hand";
    }

    /**
     * Gets decoration candidates for a specific part and position.
     * @param bottom Whether to get bottom or top candidates
     * @param partName The name of the part
     * @return List of decoration candidates
     */
    public List<HullbackDirtManager.HullbackDirtEntry> getCandidates(boolean bottom, String partName) {
        String key = partName + "_" + (bottom ? "bottom" : "top");
        return Whaleborne.PROXY.getHullbackDirtManager().get().stream()
                .filter(e -> e.placements().contains(key))
                .toList();
    }
}
