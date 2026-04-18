package com.fruityspikes.whaleborne.server.items;

import com.fruityspikes.whaleborne.server.entities.WhaleWidgetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class PlaceableWhaleEquipment extends WhaleEquipment {

    public PlaceableWhaleEquipment(Supplier<EntityType<?>> entity, Properties properties) {
        super(entity, properties);
    }

    public PlaceableWhaleEquipment(Supplier<EntityType<?>> entity, SoundEvent soundEvent, Properties properties) {
        super(entity, soundEvent, properties);
    }

    public InteractionResult useOn(UseOnContext context) {
        Direction direction = context.getClickedFace();

        if (direction == Direction.DOWN) {
            return InteractionResult.FAIL;
        } else {
            Level level = context.getLevel();
            BlockPlaceContext blockplacecontext = new BlockPlaceContext(context);
            BlockPos blockpos = blockplacecontext.getClickedPos();
            ItemStack itemstack = context.getItemInHand();
            Vec3 vec3 = Vec3.atBottomCenterOf(blockpos);
            AABB aabb = this.getEntity().getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());

            if (level.noCollision(null, aabb) && level.getEntities((Entity)null, aabb).isEmpty()) {

                Entity whaleWidget = this.getEntity().create(level);

                if (!(whaleWidget instanceof WhaleWidgetEntity widgetEntity)) return InteractionResult.FAIL;

                float angle = ((int) (context.getPlayer().getYRot() / 11.25f)) * 11.25f;
                widgetEntity.moveTo(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5, angle, 0.0F);
                level.addFreshEntity(widgetEntity);
                widgetEntity.setPersistent(true);

                level.playSound(null, widgetEntity.getX(), widgetEntity.getY(), widgetEntity.getZ(), this.getPlaceSound(), SoundSource.BLOCKS, 0.75F, 1);
                widgetEntity.gameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());

                itemstack.shrink(1);
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.FAIL;
            }
        }
    }
}
