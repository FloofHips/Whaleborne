package com.fruityspikes.whaleborne.mixin;

import com.fruityspikes.whaleborne.server.entities.DeckRiderPassage;
import com.fruityspikes.whaleborne.server.entities.HullbackPartEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class HullPassMixin extends Entity {

    protected HullPassMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity instanceof HullbackPartEntity part
                && DeckRiderPassage.carrier((LivingEntity) (Object) this) == part.getParent()) {
            return false;
        }
        return super.canCollideWith(entity);
    }
}
