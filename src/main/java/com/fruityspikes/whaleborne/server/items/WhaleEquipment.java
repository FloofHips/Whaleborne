package com.fruityspikes.whaleborne.server.items;

import com.fruityspikes.whaleborne.server.entities.AnchorHeadEntity;
import com.fruityspikes.whaleborne.server.registries.WBEntityRegistry;
import com.fruityspikes.whaleborne.server.registries.WBItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class WhaleEquipment extends Item {
    private Supplier<EntityType<?>>  entity;
    public WhaleEquipment(Supplier<EntityType<?>> entity, Properties properties) {
        super(properties);
        this.entity = entity;
    }

    public EntityType<?> getEntity() {
        return entity.get();
    }
    public InteractionResult useOn(UseOnContext context) {
        Direction direction = context.getClickedFace();
        if (!context.getItemInHand().is(WBItemRegistry.ANCHOR.get()))
            return InteractionResult.FAIL;
        if (direction == Direction.DOWN) {
            return InteractionResult.FAIL;
        } else {
            Level level = context.getLevel();
            BlockPlaceContext blockplacecontext = new BlockPlaceContext(context);
            BlockPos blockpos = blockplacecontext.getClickedPos();
            ItemStack itemstack = context.getItemInHand();
            Vec3 vec3 = Vec3.atBottomCenterOf(blockpos);
            AABB aabb = WBEntityRegistry.ANCHOR_HEAD.get().getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());

            if (level.noCollision((Entity)null, aabb) && level.getEntities((Entity)null, aabb).isEmpty()) {
                if (level instanceof ServerLevel) {
                    ServerLevel serverlevel = (ServerLevel)level;

                    Consumer<AnchorHeadEntity> consumer = EntityType.createDefaultStackConfig(serverlevel, itemstack, context.getPlayer());
                    AnchorHeadEntity anchorHead = (AnchorHeadEntity) WBEntityRegistry.ANCHOR_HEAD.get().create(serverlevel, itemstack.getTag(), consumer, blockpos, MobSpawnType.SPAWN_EGG, true, true);

                    if (anchorHead == null) {
                        return InteractionResult.FAIL;
                    }

                    float angle = ((int) (context.getPlayer().getYRot() / 11.25f)) * 11.25f;
                    anchorHead.moveTo(anchorHead.getX(), anchorHead.getY(), anchorHead.getZ(), angle, 0.0F);
                    serverlevel.addFreshEntityWithPassengers(anchorHead);
                    level.playSound((Player)null, anchorHead.getX(), anchorHead.getY(), anchorHead.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.75F, 0.4F);
                    anchorHead.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
                }

                itemstack.shrink(1);
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.FAIL;
            }
        }
    }
}
