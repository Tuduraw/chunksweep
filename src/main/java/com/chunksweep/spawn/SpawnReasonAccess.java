package com.chunksweep.spawn;

import com.chunksweep.data.ModAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * 個体に記録したスポーン理由の読み書き。
 * Mob#finalizeSpawn の Mixin から一度だけ書き込まれる。
 */
public final class SpawnReasonAccess {

    public static void set(Entity entity, EntitySpawnReason reason) {
        entity.setAttached(ModAttachments.SPAWN_REASON, reason.name());
    }

    @Nullable
    public static EntitySpawnReason get(Entity entity) {
        String raw = entity.getAttached(ModAttachments.SPAWN_REASON);
        if (raw == null) return null;
        try {
            return EntitySpawnReason.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** スポーンしたチャンクを記録する。位置が確定する addFreshEntity 時点で呼ぶこと。 */
    public static void setSpawnChunk(Entity entity, ChunkPos pos) {
        entity.setAttached(ModAttachments.SPAWN_CHUNK, pos.toLong());
    }

    public static boolean hasSpawnChunk(Entity entity) {
        return entity.hasAttached(ModAttachments.SPAWN_CHUNK);
    }

    /** スポーンしたチャンク。記録がなければ null。 */
    @Nullable
    public static ChunkPos getSpawnChunk(Entity entity) {
        Long raw = entity.getAttached(ModAttachments.SPAWN_CHUNK);
        return raw == null ? null : new ChunkPos(raw);
    }

    private SpawnReasonAccess() {}
}
