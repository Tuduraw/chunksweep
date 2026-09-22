package com.chunksweep.event;

import com.chunksweep.ChunkSweep;
import com.chunksweep.ChunkSweepConfig;
import com.chunksweep.category.MobClassifier;
import com.chunksweep.category.SweepCategory;
import com.chunksweep.data.ChunkSweepCounts;
import com.chunksweep.data.ModAttachments;
import com.chunksweep.spawn.SpawnReasonAccess;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class KillTracker {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(KillTracker::onDeath);
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!(entity instanceof Mob mob)) return;

        ChunkSweepConfig cfg = ChunkSweepConfig.get();

        // 既定ではこの制限は無効。有効にした場合のみプレイヤーによるキルに絞る
        // （矢や使い魔経由でも getEntity() は撃った側を返す）。
        if (cfg.requirePlayerKill && !(source.getEntity() instanceof ServerPlayer)) return;

        // 自然スポーンした個体のみ。mod 導入前から居た個体は記録がないので既定では対象外。
        EntitySpawnReason reason = SpawnReasonAccess.get(mob);
        if (reason == null) {
            if (!cfg.countUnmarkedMobs) return;
        } else if (!cfg.isCounted(reason)) {
            return;
        }

        SweepCategory category = MobClassifier.classify(mob);
        if (category == null) return;
        if (cfg.threshold(category) <= 0) return;

        // 加算先チャンクの決定。
        // 既定では「湧いたチャンク」に加算する。トラップや味方 mob を使うと mob は湧き元から
        // 移動して倒されるため、死亡地点で数えるとトラップのチャンクだけが掃討済みになってしまう。
        ChunkPos target = null;
        if (cfg.countAtSpawnChunk) {
            target = SpawnReasonAccess.getSpawnChunk(mob);
        }
        if (target == null) {
            BlockPos pos = mob.blockPosition();
            target = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        }

        // 湧き元チャンクが未ロードの場合は getChunkNow が null を返す。ここでチャンクを
        // 読み込ませるとデッドロックの危険があるため、死亡地点へフォールバックする。
        ChunkAccess chunk = level.getChunkSource().getChunkNow(target.x, target.z);
        if (chunk == null) {
            BlockPos pos = mob.blockPosition();
            target = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
            chunk = level.getChunkSource().getChunkNow(target.x, target.z);
        }
        if (chunk == null) return;

        ChunkSweepCounts before = chunk.getAttachedOrElse(ModAttachments.CHUNK_COUNTS, ChunkSweepCounts.EMPTY);
        ChunkSweepCounts after = before.increment(category);
        if (after.equals(before)) return; // 上限に達している

        // setAttached を通すと Fabric がチャンクの unsaved フラグを立ててくれる。
        chunk.setAttached(ModAttachments.CHUNK_COUNTS, after);

        int threshold = cfg.threshold(category);
        if (before.get(category) < threshold && after.get(category) >= threshold && cfg.debugLogging) {
            ChunkSweep.LOGGER.info("チャンク ({}, {}) が {} について掃討済みになりました",
                    target.x, target.z, category);
        }
    }

    private KillTracker() {}
}
