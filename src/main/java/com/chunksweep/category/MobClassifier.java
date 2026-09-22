package com.chunksweep.category;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EntityType -> 3 分類への変換。
 *
 * スポーン阻止側では実体がまだ存在せず EntityType しか手に入らないため、
 * instanceof ではなく entity_type タグで分類する。これによりキル集計と
 * スポーン阻止で必ず同じ結果になり、他 mod の mob もデータパックで分類できる。
 *
 * 判定順:
 *   1. chunksweep:ignored  -> 対象外 (null)
 *   2. chunksweep:neutral  -> NEUTRAL
 *   3. chunksweep:hostile  -> HOSTILE
 *   4. chunksweep:friendly -> FRIENDLY
 *   5. MobCategory == MONSTER -> HOSTILE / MISC -> 対象外 / それ以外 -> FRIENDLY
 *
 * neutral をタグ内で最優先にしているのは、エンダーマンやゾンビピグリンのように
 * MobCategory 上は MONSTER でも中立として扱いたい mob を拾うため。
 */
public final class MobClassifier {

    private static final Map<EntityType<?>, Object> CACHE = new ConcurrentHashMap<>();
    private static final Object NONE = new Object();

    /** タグのリロード時に呼ぶ。 */
    public static void invalidateCache() {
        CACHE.clear();
    }

    @Nullable
    public static SweepCategory classify(@Nullable EntityType<?> type) {
        if (type == null) return null;
        Object cached = CACHE.computeIfAbsent(type, t -> {
            SweepCategory c = compute(t);
            return c == null ? NONE : c;
        });
        return cached == NONE ? null : (SweepCategory) cached;
    }

    @Nullable
    public static SweepCategory classify(@Nullable Entity entity) {
        return entity == null ? null : classify(entity.getType());
    }

    @Nullable
    private static SweepCategory compute(EntityType<?> type) {
        var holder = type.builtInRegistryHolder();
        if (holder.is(ModEntityTypeTags.IGNORED)) return null;
        if (holder.is(ModEntityTypeTags.NEUTRAL)) return SweepCategory.NEUTRAL;
        if (holder.is(ModEntityTypeTags.HOSTILE)) return SweepCategory.HOSTILE;
        if (holder.is(ModEntityTypeTags.FRIENDLY)) return SweepCategory.FRIENDLY;

        MobCategory mc = type.getCategory();
        if (mc == MobCategory.MISC) return null;
        return mc == MobCategory.MONSTER ? SweepCategory.HOSTILE : SweepCategory.FRIENDLY;
    }

    private MobClassifier() {}
}
