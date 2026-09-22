package com.chunksweep.client;

import com.chunksweep.client.screen.SpawnActivatorScreen;
import com.chunksweep.screen.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class ChunkSweepClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.SPAWN_ACTIVATOR, SpawnActivatorScreen::new);
    }
}
