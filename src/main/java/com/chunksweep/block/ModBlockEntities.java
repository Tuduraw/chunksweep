package com.chunksweep.block;

import com.chunksweep.ChunkSweep;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    // 1.21.11 で vanilla の BlockEntityType.Builder が無くなったため、
    // Fabric API 側のビルダーを使う。
    public static final BlockEntityType<SpawnActivatorBlockEntity> SPAWN_ACTIVATOR =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ChunkSweep.id("spawn_activator"),
                    FabricBlockEntityTypeBuilder
                            .<SpawnActivatorBlockEntity>create(SpawnActivatorBlockEntity::new,
                                    ModBlocks.SPAWN_ACTIVATOR)
                            .build());

    public static void init() {}

    private ModBlockEntities() {}
}
