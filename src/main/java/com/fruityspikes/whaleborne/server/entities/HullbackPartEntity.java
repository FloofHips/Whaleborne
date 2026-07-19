package com.fruityspikes.whaleborne.server.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import com.fruityspikes.whaleborne.server.items.WhaleEquipment;
import com.fruityspikes.whaleborne.server.registries.WBTagRegistry;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import net.neoforged.neoforge.entity.PartEntity;

public class HullbackPartEntity extends PartEntity<HullbackEntity> {
    public final String name;
    private final EntityDimensions size;

    public HullbackPartEntity(HullbackEntity parent, String name, float width, float height) {
        super(parent);
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
        this.name = name;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.getParent().hurt(source, amount);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Nullable
    public ItemStack getPickResult() {
        return this.getParent().getPickResult();
    }

    public boolean is(Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    public void tick() {
        super.tick();
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public EntityDimensions getSize() {
        return size;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        boolean topClicked = vec.y > size.height() * 0.6f;
        ItemStack heldItem = player.getItemInHand(hand);

        // Let vanilla handle nametags (routes through Mob.interact → NameTagItem logic)
        if (heldItem.getItem() instanceof NameTagItem) {
            return getParent().interact(player, hand);
        }

        if (heldItem.getItem() instanceof DebugStickItem) {
            return getParent().interactDebug(player, hand);
        }

        if (heldItem.getItem() instanceof SaddleItem || WBTagRegistry.isHullMaterial(heldItem)) {
            return getParent().interactArmor(player, hand, this, topClicked);
        }

        if (heldItem.isEmpty()) {
            return handleSeatInteraction(player, hand, vec, topClicked, null);
        }

        if ((heldItem.getItem() instanceof WhaleEquipment) || (heldItem.getItem() instanceof SpawnEggItem)) {
            EntityType<?> entity;
            if (heldItem.getItem() instanceof SpawnEggItem spawnEggItem)
                entity = spawnEggItem.getType(heldItem);
            else if (heldItem.getItem() instanceof WhaleEquipment whaleEquipment)
                entity = whaleEquipment.getEntity();
            else
                entity = EntityType.EXPERIENCE_ORB;

            return handleSeatInteraction(player, hand, vec, topClicked, entity);
        }

        InteractionResult result = getParent().interactClean(player, hand, this, topClicked);
        return result.consumesAction() ? result : getParent().interact(player, hand);
    }

    /** Unified seat interaction for empty-hand mounting and equipment placement
     *  (entityType null means mount the player). */
    private InteractionResult handleSeatInteraction(Player player, InteractionHand hand, Vec3 vec, boolean topClicked, @Nullable EntityType<?> entityType) {
        if ("tail".equals(this.name))
            return getParent().interact(player, hand);
        if ("fluke".equals(this.name))
            return getParent().interactRide(player, hand, getParent().getPartManager().getFlukeSeatIndex(), entityType);
        if (topClicked) {
            int partIndex = getPartIndex();
            if (partIndex >= 0) {
                int seatIndex = findNearestSeatForPart(vec, partIndex);
                if (seatIndex >= 0) {
                    return getParent().interactRide(player, hand, seatIndex, entityType);
                }
            }
        }
        return getParent().interact(player, hand);
    }

    /** Part index for this named part, or -1 if no seats allowed. */
    private int getPartIndex() {
        return switch (this.name) {
            case "nose" -> 0;
            case "head" -> 1;
            case "body" -> 2;
            case "fluke" -> 4;
            default -> -1; // tail and others: no seat
        };
    }

    /** Nearest seat to the click in world space; matches seats whose pos or rot
     *  part index is this part, preferring empty seats. */
    private int findNearestSeatForPart(Vec3 clickVec, int partIndex) {
        if (getParent() == null || getParent().getPartManager() == null) return -1;
        var seatManager = getParent().hullbackSeatManager;
        var partManager = getParent().getPartManager();
        if (seatManager == null) return -1;
        var layout = partManager.getSeatLayout();
        int activeSeatCount = layout.getActiveSeatCount();

        // Convert click to world coordinates: click is relative to this PartEntity's position
        Vec3 clickWorld = this.position().add(clickVec);

        int bestSeat = -1;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < activeSeatCount; i++) {
            var def = layout.getSeatDef(i);
            if (def == null) continue;

            // Match seats that belong to this part (either position OR rotation source)
            if (def.posPartIndex() != partIndex && def.rotPartIndex() != partIndex) continue;

            // Use the pre-computed world-space seat position (from calculateSeats)
            Vec3 seatWorldPos = i < partManager.seats.length ? partManager.seats[i] : null;
            if (seatWorldPos == null) continue;

            // 3D distance from click to seat world position
            double dist = clickWorld.distanceToSqr(seatWorldPos);

            // Prefer empty seats — add heavy penalty for occupied ones
            boolean occupied = seatManager.getSeatData(i).isPresent();
            double penalty = occupied ? 10000.0 : 0.0;

            if (dist + penalty < bestDist) {
                bestDist = dist + penalty;
                bestSeat = i;
            }
        }

        return bestSeat;
    }
}
