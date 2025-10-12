package com.fruityspikes.whaleborne.server.items;

import com.fruityspikes.whaleborne.server.blocks.PlaceableBarnacleBlock;
import com.fruityspikes.whaleborne.server.registries.WBBlockRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class BarnacleItem extends BlockItem {
    public BarnacleItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        //BlockPos pos = context.getClickedPos().offset((int) -context.getClickedFace().step().x, (int) -context.getClickedFace().step().y, (int) -context.getClickedFace().step().z);
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);

        if (state.getBlock() instanceof PlaceableBarnacleBlock block) {
            if (context.getLevel().setBlock(pos, WBBlockRegistry.ROUGH_BARNACLE.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED)).setValue(PlaceableBarnacleBlock.NUMBER, Math.min(3, (Integer)state.getValue(PlaceableBarnacleBlock.NUMBER) + 1)), 2)) {

                BlockPos blockpos = context.getClickedPos();
                Level level = context.getLevel();
                Player player = context.getPlayer();
                ItemStack itemstack = context.getItemInHand();
                BlockState blockstate1 = level.getBlockState(blockpos);
                blockstate1.getBlock().setPlacedBy(level, blockpos, blockstate1, player, itemstack);

                if (player instanceof ServerPlayer) {
                    CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
                }

                SoundType soundtype = blockstate1.getSoundType(level, blockpos, context.getPlayer());
                level.playSound(player, blockpos, this.getPlaceSound(blockstate1, level, blockpos, context.getPlayer()), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, blockstate1));

                if (player == null || !player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return super.useOn(context);
        }
        return super.useOn(context);
    }
}
