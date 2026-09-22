package com.chunksweep.spawn;

import com.chunksweep.ChunkSweep;
import com.chunksweep.ChunkSweepConfig;
import com.chunksweep.category.MobClassifier;
import com.chunksweep.category.SweepCategory;
import com.chunksweep.data.ChunkSweepCounts;
import com.chunksweep.data.ModAttachments;
import com.chunksweep.data.ActivatorState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * 「このチャンクは掃討済みか」「スポーンを止めてよいか」の判断を一箇所に集約したもの。
 * Mixin 側はここを呼ぶだけにして、判定ロジックが分散しないようにしている。
 */
public final class SweepGate {

    /** 掃討済みフラグは保存せず、毎回カウントと閾値から導出する（コンフィグ変更が遡及する）。 */
    public static boolean isSwept(ChunkAccess chunk, SweepCategory category) {
        int threshold = ChunkSweepConfig.get().threshold(category);
        if (threshold <= 0) return false;
        ChunkSweepCounts counts = chunk.getAttachedOrElse(ModAttachments.CHUNK_COUNTS, ChunkSweepCounts.EMPTY);
        return counts.get(category) >= threshold;
    }

    /** 活性装置の影響下にあるか。 */
    public static boolean isActivated(ServerLevel level, int chunkX, int chunkZ, SweepCategory category) {
        ActivatorState state = ActivatorState.get(level);
        return !state.isEmpty() && state.isActivated(level, chunkX, chunkZ, category);
    }

    /**
     * チャンク単位の早期判定。true ならそのチャンクのそのカテゴリのスポーン処理を
     * 丸ごと省略してよい（位置抽選も密度計算も行わないので、バニラより軽くなる）。
     *
     * 構造物の start / reference を持つチャンクでは false を返し、個体単位の
     * 判定（shouldBlockSpawn）に委ねる。ネザー要塞のブレイズなど、構造物由来の
     * スポーンを巻き添えにしないため。
     */
    public static boolean shouldSkipChunk(ServerLevel level, ChunkAccess chunk, SweepCategory category) {
        ChunkPos pos = chunk.getPos();
        if (!isSwept(chunk, category)) return false;
        if (isActivated(level, pos.x, pos.z, category)) return false;
        if (ChunkSweepConfig.get().respectStructureSpawns && hasStructures(chunk)) return false;
        return true;
    }

    private static boolean hasStructures(ChunkAccess chunk) {
        return !chunk.getAllStarts().isEmpty() || !chunk.getAllReferences().isEmpty();
    }

    /**
     * 個体単位の最終判定。ServerLevel#addFreshEntity から呼ばれる。
     * shouldSkipChunk を通り抜けたスポーン（＝特殊スポーンや構造物チャンク）が
     * ここで拾われる。
     */
    public static boolean shouldBlockSpawn(ServerLevel level, Entity entity) {
        ChunkSweepConfig cfg = ChunkSweepConfig.get();

        EntitySpawnReason reason = SpawnReasonAccess.get(entity);
        if (reason == null || !cfg.isBlocked(reason)) return false;

        SweepCategory category = MobClassifier.classify(entity);
        if (category == null) return false;

        BlockPos pos = entity.blockPosition();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) return false; // 未ロードなら判断材料がないので通す

        if (!isSwept(chunk, category)) return false;
        if (isActivated(level, chunkX, chunkZ, category)) return false;
        if (cfg.respectStructureSpawns && isInsideStructureSpawnOverride(level, pos, category)) return false;

        if (cfg.debugLogging) {
            ChunkSweep.LOGGER.info("掃討済みチャンク ({}, {}) の {} スポーンを阻止: {} ({})",
                    chunkX, chunkZ, category, entity.getType().toShortString(), reason);
        }
        return true;
    }

    /**
     * バニラの ChunkGenerator#getMobsAt が行っている構造物由来のスポーン上書きを
     * 簡易に再現し、その内側なら阻止対象から外す。
     *
     * 完全な再現ではない（バニラは MobCategory 単位、こちらは 3 分類単位）ため、
     * 例えば要塞内でウィザースケルトンが湧く場合、同じ HOSTILE 分類の他の mob も
     * その座標では通ることになる。実用上は問題ないはず。
     */
    private static boolean isInsideStructureSpawnOverride(ServerLevel level, BlockPos pos, SweepCategory category) {
        StructureManager manager = level.structureManager();
        var all = manager.getAllStructuresAt(pos);
        if (all.isEmpty()) return false;

        for (Structure structure : all.keySet()) {
            var overrides = structure.spawnOverrides();
            if (overrides.isEmpty()) continue;
            for (var entry : overrides.entrySet()) {
                // その MobCategory に本分類の mob が含まれうるかは EntityType 側で
                // 判断済みなので、ここでは範囲判定のみ行う。
                var override = entry.getValue();
                boolean inside = switch (override.boundingBox()) {
                    case PIECE -> manager.getStructureWithPieceAt(pos, structure).isValid();
                    case STRUCTURE -> manager.getStructureAt(pos, structure).isValid();
                };
                if (inside) return true;
            }
        }
        return false;
    }

    @Nullable
    public static SweepCategory categoryFor(Entity entity) {
        return MobClassifier.classify(entity);
    }

    private SweepGate() {}
}
