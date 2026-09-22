package com.chunksweep.net;

import com.chunksweep.screen.SpawnActivatorMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {

    public static void init() {
        PayloadTypeRegistry.playC2S().register(ActivatorConfigPayload.TYPE, ActivatorConfigPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ActivatorConfigPayload.TYPE, (payload, context) -> {
            // 受信スレッドではなくサーバスレッドで実行される。
            var player = context.player();
            if (player.containerMenu instanceof SpawnActivatorMenu menu && menu.stillValid(player)) {
                menu.apply(payload.radius(), payload.mask(), payload.active());
            }
        });
    }

    private ModNetworking() {}
}
