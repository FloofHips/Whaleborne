package com.fruityspikes.whaleborne.server.blocks;

import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class BarnacleBlock extends Block {
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty BASE = BooleanProperty.create("base");
    public BarnacleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TRIGGERED, false).setValue(BASE, true));
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean signal = level.hasNeighborSignal(pos);

        if (signal) {
            this.playNote(null, state, level, pos);
        }
    }

    private void playNote(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        level.blockEvent(pos, this, 0, 0);
        level.gameEvent(entity, GameEvent.NOTE_BLOCK_PLAY, pos);
    }

    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockPos base = getBase(level, pos);
        int stackHeight = getStackHeight(level, base);
        BlockPos topPos = base.above(stackHeight + 1);

        double pitch = Math.pow(2.0F, (double)(stackHeight - 12) / (double)12.0F);

        AABB boundingBox = new AABB(topPos).move(0, 0, 0);
        List<Entity> entities = level.getEntities(null, boundingBox);
        entities.removeIf(entity1 -> (!entity1.isPushable() && !(entity1 instanceof ItemEntity)));
        for (Entity entity2 : entities) {
            entity2.hurtMarked = true;
            entity2.addDeltaMovement(new Vec3(0, (double) stackHeight / 15, 0));
        }

        for (int i = 0; i < stackHeight + 1; i++) {
            level.addParticle(WBParticleRegistry.SMOKE.get(), topPos.getX() + 0.5, topPos.getY() + 0.25, topPos.getZ() + 0.5, 0, 0, 0);
        }

        if (!level.getBlockState(pos.below()).is(BlockTags.WOOL)) {
            double xSpeed = (double) (stackHeight) / 24.0D;
            level.addParticle(ParticleTypes.NOTE, topPos.getX() + 0.5, topPos.getY(), topPos.getZ() + 0.5, xSpeed, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.NOTE, base.getX() + 0.5 + 0.65, base.getY() + 0.25, base.getZ() + 0.5, xSpeed, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.NOTE, base.getX() + 0.5 - 0.65, base.getY() + 0.25, base.getZ() + 0.5, xSpeed, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.NOTE, base.getX() + 0.5, base.getY() + 0.25, base.getZ() + 0.5 + 0.65, xSpeed, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.NOTE, base.getX() + 0.5, base.getY() + 0.25, base.getZ() + 0.5 - 0.65, xSpeed, 0.0D, 0.0D);
            level.playSeededSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, WBSoundRegistry.ORGAN.get(), SoundSource.RECORDS, 3F, (float) pitch, level.random.nextLong());
        }
        return true;
    }


    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean hasBelow = level.getBlockState(pos.below()).getBlock() == this;
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos).setValue(BASE, !hasBelow);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean hasBelow = context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() == this;
        return this.defaultBlockState().setValue(BASE, !hasBelow);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos base = getBase(level, pos);
        level.scheduleTick(base.above(getStackHeight(level, base)), this, 0);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(TRIGGERED)) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 3);
        } else {
            level.setBlock(pos, state.setValue(TRIGGERED, true), 3);
            level.scheduleTick(pos, this, 7);
            BlockPos base = getBase(level, pos);
            if ((pos.getY() - base.getY()) == getStackHeight(level, base) || level.getBlockState(pos.above()).getBlock() != this.asBlock()) this.playNote(null, state, level, pos);
            if (level.getBlockState(pos.below()).getBlock() == this.asBlock()) level.scheduleTick(pos.below(), this, 0);
        }

    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED, BASE);
    }

    private int getStackHeight(Level level, BlockPos basePos) {
        Block thisBlock = level.getBlockState(basePos).getBlock();
        int height = 0;

        BlockPos checkPos = basePos.above();

        while (level.getBlockState(checkPos).getBlock() == thisBlock && height < 24) {
            height++;
            checkPos = checkPos.above();
        }
        return height;
    }

    private BlockPos getBase(Level level, BlockPos pos) {
        Block thisBlock = level.getBlockState(pos).getBlock();
        BlockPos current = pos;

        while (level.getBlockState(current.below()).getBlock() == thisBlock) {
            current = current.below();
        }

        return current;
    }
}
