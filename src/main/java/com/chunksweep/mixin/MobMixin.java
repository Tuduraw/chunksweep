package com.chunksweep.mixin;

import com.chunksweep.spawn.SpawnReasonAccess;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * どのスポーン理由で生まれた個体かを記録する。
 * 「自然スポーン個体のプレイヤーキルのみカウント」「特殊スポーンだけ阻止する」の
 * 両方がこの情報に依存している。
 */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void chunksweep$recordSpawnReason(ServerLevelAccessor level,
                                              DifficultyInstance difficulty,
                                              EntitySpawnReason reason,
                                              SpawnGroupData groupData,
                                              CallbackInfoReturnable<SpawnGroupData> cir) {
        SpawnReasonAccess.set((Mob) (Object) this, reason);
    }
}
