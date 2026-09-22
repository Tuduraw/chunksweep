package com.chunksweep.screen;

import com.chunksweep.ChunkSweep;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {

    public static final MenuType<SpawnActivatorMenu> SPAWN_ACTIVATOR =
            Registry.register(BuiltInRegistries.MENU, ChunkSweep.id("spawn_activator"),
                    new ExtendedScreenHandlerType<>(SpawnActivatorMenu::new, BlockPos.STREAM_CODEC));

    public static void init() {}

    private ModMenus() {}
}
