package com.chunksweep.data;

import com.chunksweep.ChunkSweep;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModAttachments {

    /**
     * チャンクごとの討伐数。チャンク NBT に同梱されて region ファイルに保存されるため、
     *  - メモリ常駐量は「読み込み中チャンク数」にしか比例しない
     *  - 自然スポーンは読み込み中チャンクでしか起きないので、必要十分
     *  - ワールドのコピー / バックアップ / サーバ移行にそのまま追従する
     * mod を外すとバニラは未知キーを読み飛ばすためクラッシュはしないが、
     * 次回セーブでこのデータは失われる点に注意。
     */
    public static final AttachmentType<ChunkSweepCounts> CHUNK_COUNTS =
            AttachmentRegistry.create(ChunkSweep.id("chunk_counts"), builder -> builder
                    .initializer(() -> ChunkSweepCounts.EMPTY)
                    .persistent(ChunkSweepCounts.CODEC));

    /**
     * 個体がどのスポーン理由で生まれたか。Mob#finalizeSpawn で一度だけ書き込む。
     * 「自然スポーンした個体のみカウント」を実現するために必要。
     */
    public static final AttachmentType<String> SPAWN_REASON =
            AttachmentRegistry.create(ChunkSweep.id("spawn_reason"), builder -> builder
                    .persistent(Codec.STRING));

    /**
     * 個体がどのチャンクで生まれたか（ChunkPos.toLong）。
     * 討伐数を湧き元チャンクへ加算するために使う。
     */
    public static final AttachmentType<Long> SPAWN_CHUNK =
            AttachmentRegistry.create(ChunkSweep.id("spawn_chunk"), builder -> builder
                    .persistent(Codec.LONG));

    public static void init() {
        // クラスロードのためのフック
    }

    private ModAttachments() {}
}
