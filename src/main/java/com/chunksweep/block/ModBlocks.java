package com.chunksweep.block;

import com.chunksweep.ChunkSweep;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {

    public static final Identifier ACTIVATOR_ID = ChunkSweep.id("spawn_activator");

    public static final Block SPAWN_ACTIVATOR = register(
            ACTIVATOR_ID,
            props -> new SpawnActivatorBlock(props
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(SpawnActivatorBlock.POWERED) ? 7 : 0)));

    private static Block register(Identifier id, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        // 1.21.2 以降、ブロック / アイテムは Properties に登録キーを持たせる必要がある。
        Block block = factory.apply(BlockBehaviour.Properties.of().setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
        return block;
    }

    public static void init() {}

    private ModBlocks() {}
}
