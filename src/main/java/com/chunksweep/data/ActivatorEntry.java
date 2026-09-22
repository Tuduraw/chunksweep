package com.chunksweep.data;

import com.chunksweep.category.SweepCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/**
 * 設置済みの活性装置 1 台分の設定。
 */
public record ActivatorEntry(BlockPos pos, int radius, int categoryMask, boolean enabled) {

    public static final Codec<ActivatorEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(ActivatorEntry::pos),
            Codec.INT.fieldOf("radius").forGetter(ActivatorEntry::radius),
            Codec.INT.fieldOf("mask").forGetter(ActivatorEntry::categoryMask),
            Codec.BOOL.optionalFieldOf("on", true).forGetter(ActivatorEntry::enabled)
    ).apply(i, ActivatorEntry::new));

    public boolean covers(SweepCategory category) {
        return enabled && (categoryMask & category.bit()) != 0;
    }

    /** チェビシェフ距離での判定（正方形の範囲）。 */
    public boolean coversChunk(int chunkX, int chunkZ) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        return Math.abs(chunkX - cx) <= radius && Math.abs(chunkZ - cz) <= radius;
    }
}
