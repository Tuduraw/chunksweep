package com.chunksweep.mixin;

import com.chunksweep.spawn.SpawnReasonAccess;
import com.chunksweep.spawn.SweepGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 個体単位の最終判定。
 *
 * ここを押さえることで、
 *   - PhantomSpawner / PatrolSpawner / CatSpawner / WanderingTraderSpawner
 *   - 構造物チャンクで高速パスを通り抜けたスポーン
 * のいずれも同じ判定に載る。スポナーブロック・繁殖・スポーンエッグ・コマンドは
 * スポーン理由が異なるため既定では通過する。
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void chunksweep$blockSweptSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (SweepGate.shouldBlockSpawn(self, entity)) {
            entity.discard();
            cir.setReturnValue(false);
            return;
        }

        // 湧き元チャンクの記録。finalizeSpawn ではなくここで行うのは、
        // スポーナーによっては finalizeSpawn の時点でまだ座標が確定していないため。
        if (SpawnReasonAccess.get(entity) != null && !SpawnReasonAccess.hasSpawnChunk(entity)) {
            SpawnReasonAccess.setSpawnChunk(entity, new ChunkPos(entity.blockPosition()));
        }
    }
}
