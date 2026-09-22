package com.chunksweep;

import com.chunksweep.block.ModBlockEntities;
import com.chunksweep.block.ModBlocks;
import com.chunksweep.category.MobClassifier;
import com.chunksweep.data.ModAttachments;
import com.chunksweep.event.KillTracker;
import com.chunksweep.net.ModNetworking;
import com.chunksweep.screen.ModMenus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkSweep implements ModInitializer {

    public static final String MOD_ID = "chunksweep";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ChunkSweepConfig.load();

        ModAttachments.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModMenus.init();
        ModNetworking.init();
        KillTracker.register();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(ModBlocks.SPAWN_ACTIVATOR));

        // タグがリロードされたら分類キャッシュを捨てる。
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> MobClassifier.invalidateCache());

        LOGGER.info("Chunk Sweep initialized");
    }
}
