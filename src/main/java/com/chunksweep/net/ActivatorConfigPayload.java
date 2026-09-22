package com.chunksweep.net;

import com.chunksweep.ChunkSweep;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** クライアント -> サーバ。活性装置の設定変更。 */
public record ActivatorConfigPayload(int radius, int mask, boolean active) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActivatorConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ChunkSweep.id("activator_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActivatorConfigPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ActivatorConfigPayload::radius,
                    ByteBufCodecs.VAR_INT, ActivatorConfigPayload::mask,
                    ByteBufCodecs.BOOL, ActivatorConfigPayload::active,
                    ActivatorConfigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
