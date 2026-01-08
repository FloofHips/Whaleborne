package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.blocks.BarnacleBlock;
import com.fruityspikes.whaleborne.server.blocks.RoughBarnacleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class WBBlockRegistry {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Whaleborne.MODID);
    public static final Supplier<Block> ROUGH_BARNACLE = BLOCKS.register("rough_barnacle", () -> new RoughBarnacleBlock(BlockBehaviour.Properties.of().strength(1,2).sound(SoundType.BONE_BLOCK).mapColor(MapColor.STONE)));
    public static final Supplier<Block> BARNACLE = BLOCKS.register("barnacle", () -> new BarnacleBlock(BlockBehaviour.Properties.of().sound(SoundType.BONE_BLOCK).strength(1,2).isRedstoneConductor(WBBlockRegistry::never).mapColor(MapColor.STONE)));

    private static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return false;
    }


}
