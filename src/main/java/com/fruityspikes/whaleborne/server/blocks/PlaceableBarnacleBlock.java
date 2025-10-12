package com.fruityspikes.whaleborne.server.blocks;

import com.fruityspikes.whaleborne.server.registries.WBBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class PlaceableBarnacleBlock extends Block implements SimpleWaterloggedBlock {
    public static final int MAX = 3;
    public static final IntegerProperty NUMBER;
    public static final BooleanProperty WATERLOGGED;
    protected static final VoxelShape ONE_AABB;
    protected static final VoxelShape TWO_AABB;
    protected static final VoxelShape THREE_AABB;
    public PlaceableBarnacleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(NUMBER, 1)).setValue(WATERLOGGED, true));

    }

    @Override
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        return state.is(WBBlockRegistry.ROUGH_BARNACLE.get());
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        //BlockPos pos = context.getClickedPos().offset((int) -context.getClickedFace().step().x, (int) -context.getClickedFace().step().y, (int) -context.getClickedFace().step().z);
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos());
        //BlockPos pos = context.getClickedPos().offset((int) -context.getClickedFace().step().x, (int) -context.getClickedFace().step().y, (int) -context.getClickedFace().step().z);
        //BlockState blockstate = context.getLevel().getBlockState(pos);
        //System.out.println(pos);

        if (blockstate.is(this)) {
            return (BlockState)blockstate.setValue(NUMBER, Math.min(3, (Integer)blockstate.getValue(NUMBER) + 1));
        } else {
            FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
            boolean flag = fluidstate.getType() == Fluids.WATER;
            return (BlockState)super.getStateForPlacement(context).setValue(WATERLOGGED, flag);
        }
    }

    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if ((Boolean)state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch ((Integer)state.getValue(NUMBER)) {
            case 1:
            default:
                return ONE_AABB;
            case 2:
                return TWO_AABB;
            case 3:
                return THREE_AABB;
        }
    }

    public FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{NUMBER, WATERLOGGED});
    }

    static {
        NUMBER = IntegerProperty.create("number", 1, 3);;
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        ONE_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
        TWO_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
        THREE_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 32.0, 16.0);
    }
}
