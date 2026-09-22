package com.chunksweep.mixin;

import com.chunksweep.category.SweepCategory;
import com.chunksweep.spawn.SweepGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 高速パス。掃討済みかつ構造物を含まないチャンクでは、位置抽選にも密度計算にも
 * 入らずに丸ごと打ち切る。掃討済みチャンクではバニラより処理が軽くなる。
 *
 * 構造物を含むチャンクはここでは止めず、ServerLevelMixin の個体単位判定に委ねる。
 *
 * 互換性メモ: Lithium 系の最適化 mod はこの周辺を大きく書き換えるため、
 * 併用時は実機での動作確認が必要。
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(
            method = "spawnCategoryForChunk",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void chunksweep$skipSweptChunk(MobCategory mobCategory,
                                                  ServerLevel level,
                                                  LevelChunk chunk,
                                                  NaturalSpawner.SpawnPredicate filter,
                                                  NaturalSpawner.AfterSpawnCallback callback,
                                                  CallbackInfo ci) {
        if (mobCategory == MobCategory.MISC) return;

        // MobCategory から 3 分類への写像は 1 対 1 ではないため、
        // 「この MobCategory に属しうる全分類が掃討済み」のときだけ丸ごと止める。
        // 例えば MONSTER には敵対と中立（エンダーマン等）が混在しうる。
        boolean skip = true;
        for (SweepCategory category : candidatesFor(mobCategory)) {
            if (!SweepGate.shouldSkipChunk(level, chunk, category)) {
                skip = false;
                break;
            }
        }
        if (skip) ci.cancel();
    }

    private static SweepCategory[] candidatesFor(MobCategory mobCategory) {
        // MONSTER には中立扱いの mob（エンダーマン、ゾンビピグリン等）も含まれる。
        // それ以外は友好か中立。
        return mobCategory == MobCategory.MONSTER
                ? new SweepCategory[]{SweepCategory.HOSTILE, SweepCategory.NEUTRAL}
                : new SweepCategory[]{SweepCategory.FRIENDLY, SweepCategory.NEUTRAL};
    }
}
