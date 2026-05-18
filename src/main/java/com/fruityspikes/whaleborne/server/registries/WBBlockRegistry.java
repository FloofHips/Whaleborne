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
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WBBlockRegistry {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Whaleborne.MODID);
    public static final DeferredHolder<Block, Block> ROUGH_BARNACLE = BLOCKS.register("rough_barnacle", () -> new RoughBarnacleBlock(BlockBehaviour.Properties.of().strength(1,2).sound(WBSoundRegistry.BARNACLE_BLOCK).mapColor(MapColor.STONE)));
    public static final DeferredHolder<Block, Block> BARNACLE = BLOCKS.register("barnacle", () -> new BarnacleBlock(BlockBehaviour.Properties.of().sound(WBSoundRegistry.BARNACLE_BLOCK).strength(1,2).isRedstoneConductor(WBBlockRegistry::never).mapColor(MapColor.STONE)));
    public static final DeferredHolder<Block, Block> TALL_BARNACLE = BLOCKS.register("tall_barnacle", () -> new Block(BlockBehaviour.Properties.of().sound(WBSoundRegistry.BARNACLE_BLOCK).strength(1,2).isRedstoneConductor(WBBlockRegistry::never).mapColor(MapColor.STONE)));
    // Decorative variant used by HullbackDirt to mark whales carrying the disc — the obtainable item lives in WBItemRegistry.
    public static final DeferredHolder<Block, Block> MUSIC_DISC_THE_PLANK = BLOCKS.register("music_disc_the_plank", () -> new Block(BlockBehaviour.Properties.ofFullCopy(BARNACLE.get()).noOcclusion().noCollission()));

    private static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return false;
    }

}
