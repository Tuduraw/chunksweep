package com.chunksweep.category;

import com.chunksweep.ChunkSweep;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModEntityTypeTags {
    public static final TagKey<EntityType<?>> HOSTILE = of("hostile");
    public static final TagKey<EntityType<?>> NEUTRAL = of("neutral");
    public static final TagKey<EntityType<?>> FRIENDLY = of("friendly");
    /** このタグの mob はカウントも阻止も一切行わない（ボス等の除外用）。 */
    public static final TagKey<EntityType<?>> IGNORED = of("ignored");

    private static TagKey<EntityType<?>> of(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, ChunkSweep.id(path));
    }

    private ModEntityTypeTags() {}
}
