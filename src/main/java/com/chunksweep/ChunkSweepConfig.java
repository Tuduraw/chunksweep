package com.chunksweep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.chunksweep.category.SweepCategory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntitySpawnReason;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 依存ライブラリなしの JSON コンフィグ (config/chunksweep.json)。
 * マルチプレイではサーバ側の値が権威。クライアントに必要な値は
 * ScreenHandler 経由でのみ渡している。
 */
public final class ChunkSweepConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("chunksweep.json");

    private static ChunkSweepConfig instance = new ChunkSweepConfig();

    public static ChunkSweepConfig get() { return instance; }

    // ---- 保存されるフィールド -------------------------------------------------

    /** 分類ごとの必要討伐数。0 以下でその分類の掃討機能を無効化。 */
    public int hostileThreshold = 32;
    public int neutralThreshold = 32;
    /** 動物は主にチャンク生成時にしか湧かないため、32 は同一チャンクではかなり厳しい。 */
    public int friendlyThreshold = 32;

    /**
     * true の場合、プレイヤーが与えた致命傷のみをカウントする。
     * 味方 mob やトラップを使うプレイでは実態に合わないため既定は false。
     * 誰が倒したかを問わず、死因も問わない（落下・溶岩・mob 同士の戦闘も含む）。
     */
    public boolean requirePlayerKill = false;

    /**
     * true の場合、討伐数を「死亡したチャンク」ではなく「その個体がスポーンしたチャンク」に加算する。
     *
     * トラップや味方 mob を使うと mob は湧いた場所から移動して倒されるため、死亡地点で数えると
     * トラップのある 1 チャンクだけが掃討済みになり、肝心の湧き元が永久に掃討されない。
     * false にすると従来どおり死亡地点で数える。
     *
     * スポーン地点が記録されていない個体（mod 導入前から居た mob 等）は死亡地点にフォールバックする。
     */
    public boolean countAtSpawnChunk = true;

    /**
     * 「自然湧き」とみなすスポーン理由。ここに含まれる理由で湧いた個体のみカウントされる。
     * CHUNK_GENERATION を含めないと友好 mob はほぼカウントされない。
     */
    public Set<String> countedSpawnReasons = new LinkedHashSet<>(Set.of(
            "NATURAL", "CHUNK_GENERATION", "PATROL", "EVENT"));

    /** 掃討済みチャンクで阻止するスポーン理由。スポナー/繁殖/コマンド等は既定で対象外。 */
    public Set<String> blockedSpawnReasons = new LinkedHashSet<>(Set.of(
            "NATURAL", "PATROL", "EVENT"));

    /** true なら、spawnOverrides を持つ構造物（ネザー要塞など）の内側は阻止しない。 */
    public boolean respectStructureSpawns = true;

    /** スポーン理由が記録されていない個体（mod 導入前から居た mob 等）の扱い。 */
    public boolean countUnmarkedMobs = false;

    /** 活性装置の最大半径（チャンク単位）。 */
    public int maxActivatorRadius = 8;
    public int defaultActivatorRadius = 2;

    /** true なら掃討済みチャンクでも湧き潰し判定などのデバッグログを出す。 */
    public boolean debugLogging = false;

    // ---- 導出値 ---------------------------------------------------------------

    private transient EnumSet<EntitySpawnReason> countedCache;
    private transient EnumSet<EntitySpawnReason> blockedCache;

    public int threshold(SweepCategory category) {
        return switch (category) {
            case HOSTILE -> hostileThreshold;
            case NEUTRAL -> neutralThreshold;
            case FRIENDLY -> friendlyThreshold;
        };
    }

    public boolean isCounted(EntitySpawnReason reason) {
        if (countedCache == null) countedCache = parse(countedSpawnReasons);
        return countedCache.contains(reason);
    }

    public boolean isBlocked(EntitySpawnReason reason) {
        if (blockedCache == null) blockedCache = parse(blockedSpawnReasons);
        return blockedCache.contains(reason);
    }

    private static EnumSet<EntitySpawnReason> parse(Set<String> names) {
        EnumSet<EntitySpawnReason> out = EnumSet.noneOf(EntitySpawnReason.class);
        for (String n : names) {
            try {
                out.add(EntitySpawnReason.valueOf(n.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                ChunkSweep.LOGGER.warn("不明なスポーン理由をコンフィグで指定しています: {}", n);
            }
        }
        return out;
    }

    private void sanitize() {
        maxActivatorRadius = Math.clamp(maxActivatorRadius, 0, 32);
        defaultActivatorRadius = Math.clamp(defaultActivatorRadius, 0, maxActivatorRadius);
        countedCache = null;
        blockedCache = null;
    }

    // ---- 入出力 ---------------------------------------------------------------

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH, StandardCharsets.UTF_8);
                ChunkSweepConfig loaded = GSON.fromJson(json, ChunkSweepConfig.class);
                if (loaded != null) instance = loaded;
            }
        } catch (Exception e) {
            ChunkSweep.LOGGER.error("chunksweep.json の読み込みに失敗しました。既定値を使用します。", e);
            instance = new ChunkSweepConfig();
        }
        instance.sanitize();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ChunkSweep.LOGGER.error("chunksweep.json の書き込みに失敗しました。", e);
        }
    }
}
