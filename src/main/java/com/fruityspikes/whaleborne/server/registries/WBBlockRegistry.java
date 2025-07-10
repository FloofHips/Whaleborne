package com.fruityspikes.whaleborne.server.registries;

import com.fruityspikes.whaleborne.Whaleborne;
import com.fruityspikes.whaleborne.server.blocks.BarnacleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class WBBlockRegistry {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Whaleborne.MODID);
    public static final RegistryObject<Block> BARNACLE = BLOCKS.register("barnacle", () -> new BarnacleBlock(BlockBehaviour.Properties.of().sound(SoundType.BONE_BLOCK).mapColor(MapColor.STONE)));

    public static final RegistryObject<Block> WHALE_BARNACLE_0 = BLOCKS.register("whale_barnacle_0", () -> new Block(BlockBehaviour.Properties.of().sound(SoundType.BONE_BLOCK).mapColor(MapColor.STONE)));
    public static final RegistryObject<Block> WHALE_BARNACLE_1 = BLOCKS.register("whale_barnacle_1", () -> new Block(BlockBehaviour.Properties.of().sound(SoundType.BONE_BLOCK).mapColor(MapColor.STONE)));
    public static final RegistryObject<Block> WHALE_BARNACLE_2 = BLOCKS.register("whale_barnacle_2", () -> new Block(BlockBehaviour.Properties.of().sound(SoundType.BONE_BLOCK).mapColor(MapColor.STONE)));

}
