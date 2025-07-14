package com.fruityspikes.whaleborne.server.blocks;

import com.fruityspikes.whaleborne.server.registries.WBParticleRegistry;
import com.fruityspikes.whaleborne.server.registries.WBSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.NoteBlockEvent;

import javax.annotation.Nullable;

public class BarnacleBlock extends Block {
    public static final BooleanProperty POWERED;
    public BarnacleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean flag = level.hasNeighborSignal(pos);
        if (flag != (Boolean)state.getValue(POWERED)) {
            if (flag) {
                this.playNote((Entity)null, state, level, pos);
            }

            level.setBlock(pos, (BlockState)state.setValue(POWERED, flag), 3);
        }

        //        boolean flag = level.hasNeighborSignal(pos);
//        level.addParticle(ParticleTypes.EXPLOSION, pos.getX(), pos.getY(), pos.getZ(), 0.0, 0.005, 0.0);
//
//        if (flag != state.getValue(POWERED)) {
//            if (flag) {
//                int count = 0;
//
//                Block thisBlock = level.getBlockState(pos).getBlock();
//                BlockPos checkPos = pos.above();
//
//                while (level.getBlockState(checkPos).getBlock() == thisBlock && count < 20) {
//                    count++;
//                    checkPos = checkPos.above();
//                }
//
//                int stackHeight = count;
//                float pitch = Math.min(2f, stackHeight * 0.22f);
//
//                if (!level.isClientSide) {
//                    level.playSound(null, pos, WBSoundRegistry.ORGAN.get(), SoundSource.BLOCKS, 0.5F, pitch);
//                }
//            }
//        }
//        level.setBlock(pos, state.setValue(POWERED, flag), 3);
    }

    private void playNote(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        level.blockEvent(pos, this, 0, 0);
        level.gameEvent(entity, GameEvent.NOTE_BLOCK_PLAY, pos);
    }

    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        int count = 0;

        Block thisBlock = level.getBlockState(pos).getBlock();
        BlockPos checkPos = pos.above();

        while (level.getBlockState(checkPos).getBlock() == thisBlock && count < 20) {
            count++;
            checkPos = checkPos.above();
        }
        int stackHeight = count;
        float pitch = Math.min(2f, stackHeight * 0.22f);

        level.addParticle(ParticleTypes.NOTE, (double)checkPos.getX() + 0.5, (double)checkPos.getY() + 0.5, (double)checkPos.getZ() + 0.5, 0.0, 0.01, 0.0);
        level.addParticle(WBParticleRegistry.SMOKE.get(), (double)checkPos.getX() + 0.5, (double)checkPos.getY() + 0.5, (double)checkPos.getZ() + 0.5, 0.0, 0.01, 0.0);
        level.playSeededSound((Player)null, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, WBSoundRegistry.ORGAN.get(), SoundSource.RECORDS, 2F, pitch, level.random.nextLong());
        return true;
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        this.playNote(player, state, level, pos);
        return InteractionResult.SUCCESS;
    }
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{POWERED});
    }

    static {
        POWERED = BlockStateProperties.POWERED;
    }
}
