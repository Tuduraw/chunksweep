package com.chunksweep.data;

import com.chunksweep.block.SpawnActivatorBlockEntity;
import com.chunksweep.category.SweepCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ディメンションごとのスポーン活性装置レジストリ。
 *
 * 台数が少なく、かつ未ロードチャンクにある装置の影響範囲も知る必要があるため、
 * こちらはチャンク添付ではなく world/data/chunksweep_activators.dat に置く。
 * mod を外した場合、この .dat はバニラから参照されず放置されるだけで無害。
 *
 * 重要: このクラスからチャンクを「読み込ませる」操作（Level#getBlockEntity など）を
 * 呼んではいけない。チャンク読み込み中に呼ばれるとデッドロックする。
 * 参照は必ず getChunkNow によるノンブロッキング取得に限定する。
 */
public class ActivatorState extends SavedData {

    private static final String ID = "chunksweep_activators";

    public static final Codec<ActivatorState> CODEC = RecordCodecBuilder.create(i -> i.group(
            ActivatorEntry.CODEC.listOf().fieldOf("activators")
                    .forGetter(s -> List.copyOf(s.entries.values()))
    ).apply(i, ActivatorState::new));

    public static final SavedDataType<ActivatorState> TYPE =
            new SavedDataType<>(ID, ActivatorState::new, CODEC, null);

    private final Map<BlockPos, ActivatorEntry> entries = new HashMap<>();

    public ActivatorState() {}

    private ActivatorState(List<ActivatorEntry> list) {
        for (ActivatorEntry e : list) entries.put(e.pos(), e);
    }

    public static ActivatorState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void put(ActivatorEntry entry) {
        entries.put(entry.pos(), entry);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos) != null) setDirty();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * 指定チャンクが、その分類について活性化の対象になっているか。
     *
     * 見つかったエントリは、その装置があるチャンクが「すでに読み込まれている場合に限り」
     * 実体の有無を検証する（getChunkNow はブロックしない）。装置がワールド編集などで
     * 消えていた場合はここで自己修復される。未ロードなら検証せずそのまま有効扱いにする。
     */
    public boolean isActivated(ServerLevel level, int chunkX, int chunkZ, SweepCategory category) {
        if (entries.isEmpty()) return false;

        List<BlockPos> stale = null;
        boolean hit = false;

        for (ActivatorEntry e : entries.values()) {
            if (!e.covers(category) || !e.coversChunk(chunkX, chunkZ)) continue;
            if (isMissing(level, e.pos())) {
                if (stale == null) stale = new ArrayList<>(1);
                stale.add(e.pos());
                continue;
            }
            hit = true;
        }

        if (stale != null) {
            for (BlockPos pos : stale) remove(pos);
        }
        return hit;
    }

    /** その座標のチャンクが読み込み済みで、かつ装置が存在しないことが確定した場合のみ true。 */
    private static boolean isMissing(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return false; // 未ロード。判断できないので有効のままにする
        return !(chunk.getBlockEntity(pos) instanceof SpawnActivatorBlockEntity);
    }
}
