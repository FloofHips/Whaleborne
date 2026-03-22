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

    public EntityDimensions getSize() {
        return size;
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        boolean topClicked = vec.y > size.height() * 0.6f;
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.getItem() instanceof DebugStickItem) {
            return getParent().interactDebug(player, hand);
        }

        if (heldItem.getItem() instanceof SaddleItem || heldItem.is(WBTagRegistry.HULLBACK_EQUIPPABLE)) {
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

        // Let vanilla handle nametags (routes through Mob.interact → NameTagItem logic)
        if (heldItem.getItem() instanceof NameTagItem) {
            return getParent().interact(player, hand);
        }

        InteractionResult result = getParent().interactClean(player, hand, this, topClicked);
        return result.consumesAction() ? result : getParent().interact(player, hand);
    }

    /**
     * Unified seat interaction logic for both empty hand and equipment placement.
     * @param player The interacting player
     * @param hand The hand used
     * @param vec Click position vector
     * @param topClicked Whether the top of the part was clicked
     * @param entityType Entity type to place, or null for player mounting
     * @return The interaction result
     */
    private InteractionResult handleSeatInteraction(Player player, InteractionHand hand, Vec3 vec, boolean topClicked, @Nullable EntityType<?> entityType) {
        if ("tail".equals(this.name))
            return getParent().interact(player, hand);
        if ("fluke".equals(this.name))
            return getParent().interactRide(player, hand, 6, entityType);
        if (topClicked) {
            if ("body".equals(this.name)) {
                int seatIndex = getBodySeatFromClick(vec);
                return getParent().interactRide(player, hand, seatIndex, entityType);
            }
            if ("nose".equals(this.name))
                return getParent().interactRide(player, hand, 0, entityType);
            if ("head".equals(this.name))
                return getParent().interactRide(player, hand, 1, entityType);
        }
        return getParent().interact(player, hand);
    }

    /**
     * Determines seat index from click position on the body part.
     */
    private int getBodySeatFromClick(Vec3 vec) {
        Vec3 localClick = new Vec3(vec.x, 0, vec.z);
        float inverseYaw = this.getYRot() * Mth.DEG_TO_RAD;
        localClick = localClick.yRot(inverseYaw);
        double angle = Math.atan2(localClick.z, localClick.x) + Math.PI;
        int quadrant = (int) (angle / (Math.PI / 2)) % 4;

        return switch (quadrant) {
            case 0 -> 5;
            case 1 -> 4;
            case 2 -> 2;
            default -> 3;
        };
    }
}
