package com.fruityspikes.whaleborne.server.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RoughBarnacleBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    protected static final VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 8, 16);
    protected static final VoxelShape SHAPE_DOWN = Block.box(0, 8, 0, 16, 16, 16);
    protected static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 8, 16, 16, 16);
    protected static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 8);
    protected static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 8, 16, 16);
    protected static final VoxelShape SHAPE_WEST = Block.box(8, 0, 0, 16, 16, 16);

    public RoughBarnacleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, true).setValue(TYPE, Type.SMALL_LONE).setValue(FACING, Direction.UP));
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return !useContext.isSecondaryUseActive() && useContext.getItemInHand().getItem() == this.asItem() && (state.getValue(TYPE) == Type.SMALL || state.getValue(TYPE) == Type.SMALL_LONE) || super.canBeReplaced(state, useContext);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(TYPE) == Type.SMALL || state.getValue(TYPE) == Type.SMALL_LONE) {
            return switch (state.getValue(FACING)) {
                case UP -> SHAPE_UP;
                case DOWN -> SHAPE_DOWN;
                case SOUTH -> SHAPE_SOUTH;
                case EAST -> SHAPE_EAST;
                case WEST -> SHAPE_WEST;
                default -> SHAPE_NORTH;
            };
        } else {
            return Shapes.block();
        }
    }

    private Type calculateType(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockPos frontPos = pos.relative(facing);
        BlockPos backPos = pos.relative(facing.getOpposite());

        BlockState front = level.getBlockState(frontPos);
        BlockState back = level.getBlockState(backPos);

        boolean hasFront = isSameBarnacle(front, facing);
        boolean hasBack = isSameBarnacle(back, facing);

        if (hasFront && hasBack) return Type.MIDDLE;
        if (hasBack) return Type.TIP;
        if (hasFront) return Type.BASE;
        return Type.TIP_LONE;
    }

    private boolean isSameBarnacle(BlockState state, Direction facing) {
        return state.is(this) && state.getValue(FACING) == facing;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getClickedFace();
        FluidState fluid = level.getFluidState(pos);

        BlockState existing = level.getBlockState(pos);
        if (existing.is(this) && (existing.getValue(TYPE) == Type.SMALL || existing.getValue(TYPE) == Type.SMALL_LONE)) {
            BlockState back = level.getBlockState(pos.relative(existing.getValue(FACING).getOpposite()));
            boolean hasBack = isSameBarnacle(back, existing.getValue(FACING));

            return existing.setValue(TYPE, hasBack ? Type.TIP : Type.TIP_LONE);
        }
        if (!existing.is(this)) {
            BlockState back = level.getBlockState(pos.relative(facing.getOpposite()));
            boolean hasBack = isSameBarnacle(back, facing);

            return defaultBlockState().setValue(FACING, facing).setValue(TYPE, hasBack ? Type.SMALL : Type.SMALL_LONE).setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
        }


        Type type = calculateType(level, pos, facing);

        return defaultBlockState().setValue(FACING, facing).setValue(TYPE, type).setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }


    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(TYPE) == Type.SMALL || state.getValue(TYPE) == Type.SMALL_LONE) {
            return state;
        }

        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        Direction facing = state.getValue(FACING);

        if (dir == facing || dir == facing.getOpposite()) {
            Type newType = calculateType(level, pos, facing);
            return state.setValue(TYPE, newType);
        }

        return state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        Direction facing = state.getValue(FACING);
        level.updateNeighborsAt(pos.relative(facing), this);
        level.updateNeighborsAt(pos.relative(facing.getOpposite()), this);
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }


    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, TYPE, FACING);
    }

    public enum Type implements StringRepresentable {
        SMALL("small"),
        SMALL_LONE("small_lone"),
        BASE("base"),
        MIDDLE("middle"),
        TIP("tip"),
        TIP_LONE("tip_lone");

        public String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
