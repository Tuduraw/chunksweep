package com.chunksweep.data;

import com.chunksweep.category.SweepCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 1 チャンク分の討伐数。イミュータブルにしてあるのは、Data Attachment API が
 * setAttached() 経由の更新でのみチャンクの unsaved フラグを立てるため。
 * 値は short 相当に飽和させ、1 チャンクあたり実質 6 バイトに収める。
 */
public record ChunkSweepCounts(int hostile, int neutral, int friendly) {

    public static final int MAX = Short.MAX_VALUE;
    public static final ChunkSweepCounts EMPTY = new ChunkSweepCounts(0, 0, 0);

    public static final Codec<ChunkSweepCounts> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.SHORT.optionalFieldOf("h", (short) 0).forGetter(c -> (short) c.hostile),
            Codec.SHORT.optionalFieldOf("n", (short) 0).forGetter(c -> (short) c.neutral),
            Codec.SHORT.optionalFieldOf("f", (short) 0).forGetter(c -> (short) c.friendly)
    ).apply(i, (h, n, f) -> new ChunkSweepCounts(h, n, f)));

    public int get(SweepCategory category) {
        return switch (category) {
            case HOSTILE -> hostile;
            case NEUTRAL -> neutral;
            case FRIENDLY -> friendly;
        };
    }

    public ChunkSweepCounts increment(SweepCategory category) {
        return switch (category) {
            case HOSTILE -> new ChunkSweepCounts(Math.min(hostile + 1, MAX), neutral, friendly);
            case NEUTRAL -> new ChunkSweepCounts(hostile, Math.min(neutral + 1, MAX), friendly);
            case FRIENDLY -> new ChunkSweepCounts(hostile, neutral, Math.min(friendly + 1, MAX));
        };
    }

    public ChunkSweepCounts with(SweepCategory category, int value) {
        int v = Math.clamp(value, 0, MAX);
        return switch (category) {
            case HOSTILE -> new ChunkSweepCounts(v, neutral, friendly);
            case NEUTRAL -> new ChunkSweepCounts(hostile, v, friendly);
            case FRIENDLY -> new ChunkSweepCounts(hostile, neutral, v);
        };
    }

    public boolean isEmpty() {
        return hostile == 0 && neutral == 0 && friendly == 0;
    }
}
