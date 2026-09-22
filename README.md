# Chunk Sweep

Minecraft 1.21.11 / Fabric 向け。チャンクごとの討伐数を数え、閾値を超えたチャンクを
「掃討済み」として自然スポーンを無効化します。

## アーキテクチャ

### データの持ち方（2層構成）

| データ | 置き場所 | 理由 |
|---|---|---|
| チャンクごとの討伐数 | Fabric Data Attachment（チャンクNBTに同梱） | 常駐メモリが読み込み中チャンク数にしか比例しない。自然スポーンは読み込み中チャンクでしか起きないので必要十分。ワールドのコピー/バックアップ/サーバ移行に自動追従 |
| 活性装置の一覧 | `SavedData` (`world/data/chunksweep_activators.dat`) | 台数が少なく、未ロードチャンクの装置も参照する必要があるため |

**掃討済みフラグ自体は保存していません。** カウントのみを保存し、参照のたびに
`count >= threshold` で導出します。これにより Config の閾値変更が既存ワールドへ
遡って反映されます。カウントは short に飽和させているので 1 チャンクあたり実質 6 バイトです。

**mod を外したとき**: チャンクNBT の未知キーはバニラが読み飛ばすためクラッシュしません。
ただし次回セーブで破棄されるので、mod を外して遊んだあと再導入すると討伐数は消えます。
`.dat` の方はバニラから参照されず放置されるだけで無害です。

### mob の 3 分類

`instanceof` ではなく **entity_type タグ**で分類しています。スポーン阻止の時点では
実体が存在せず `EntityType` しか手に入らないため、キル集計とスポーン阻止で必ず同じ
判定になる必要があるからです。副次的に、他 mod の mob もデータパックで分類できます。

- `chunksweep:ignored` → 集計・阻止の対象外（既定でボスと村人・行商人）
- `chunksweep:neutral` → 中立
- `chunksweep:hostile` → 敵対
- `chunksweep:friendly` → 友好
- どのタグにも無い場合: `MobCategory == MONSTER` なら敵対、`MISC` なら対象外、それ以外は友好

`neutral` をタグ内で最優先にしているのは、エンダーマンやゾンビピグリンのように
`MobCategory` 上は `MONSTER` でも中立として扱いたい mob を拾うためです。

### スポーン阻止（3 つの Mixin）

1. `MobMixin` — `Mob#finalizeSpawn` でスポーン理由を個体に記録
2. `NaturalSpawnerMixin` — 掃討済みかつ構造物を含まないチャンクは
   `spawnCategoryForChunk` の head で丸ごと打ち切り（位置抽選・密度計算に入らないので
   バニラより軽くなります）
3. `ServerLevelMixin` — `addFreshEntity` で個体単位の最終判定。特殊スポーナー
   （ファントム / パトロール / ネコ / 行商人）と、構造物チャンクで 2 を通り抜けた
   スポーンがここで拾われます

**構造物由来のスポーンは阻止しません。** ネザー要塞のブレイズなどはバニラでは
`NaturalSpawner` を通る「自然スポーン」なので、チャンク単位で切ると巻き添えになります。
そのため構造物を含むチャンクだけ個体単位判定に落とし、`spawnOverrides` を持つ構造物ピースの
内側なら通しています（`respectStructureSpawns`）。

スポナーブロック・トライアルスポナー・繁殖・スポーンエッグ・コマンドはスポーン理由が
異なるため既定で通過します。

## Config (`config/chunksweep.json`)

| キー | 既定値 | 説明 |
|---|---|---|
| `hostileThreshold` / `neutralThreshold` / `friendlyThreshold` | 32 | 分類ごとの必要討伐数。0 以下でその分類を無効化 |
| `requirePlayerKill` | false | true にするとプレイヤーが与えた致命傷のみカウント |
| `countAtSpawnChunk` | true | 死亡地点ではなく、その個体が湧いたチャンクに加算する |
| `countedSpawnReasons` | NATURAL, CHUNK_GENERATION, PATROL, EVENT | 「自然湧き」とみなすスポーン理由 |
| `blockedSpawnReasons` | NATURAL, PATROL, EVENT | 掃討済みチャンクで阻止する理由 |
| `respectStructureSpawns` | true | 構造物内は阻止しない |
| `countUnmarkedMobs` | false | mod 導入前から居た個体を数えるか |
| `maxActivatorRadius` / `defaultActivatorRadius` | 8 / 2 | 装置の半径（チャンク単位、正方形） |
| `debugLogging` | false | 阻止・掃討到達をログに出す |

> **注意**: 動物は大半が `CHUNK_GENERATION` でのみ湧き、1 チャンクあたり 2〜8 体程度です。
> 同一チャンクで友好 32 体は現実的でないため、`friendlyThreshold` は 8 前後を推奨します。

### 誰が倒したか / どこで数えるか

討伐者は問いません。プレイヤー、味方 mob、トラップ、落下、溶岩、mob 同士の戦闘、いずれも
カウントされます（`requirePlayerKill` を true にすれば従来どおりプレイヤー限定に戻せます）。

一方で、**加算先は死亡地点ではなく「その個体が湧いたチャンク」です**（`countAtSpawnChunk`）。
トラップや味方 mob を使うと mob は湧き元から移動して倒されるため、死亡地点で数えると
トラップのある 1 チャンクだけが掃討済みになり、湧き元のチャンクが永久に掃討されません。
湧き元を基準にすることで「湧いた分だけそのチャンクが掃討されていく」挙動になります。

湧き元が記録されていない個体（mod 導入前から居た mob）や、湧き元チャンクが既に
アンロードされている場合は死亡地点へフォールバックします。

なお、これはスポナー式トラップには影響しません。スポナー由来の個体は
`countedSpawnReasons` に `SPAWNER` が入っていないため、そもそもカウント対象外です。

## スポーン活性装置（`chunksweep:spawn_activator`）

設置チャンクと周辺 N チャンク（チェビシェフ距離＝正方形）の掃討済みフラグを無効化し、
通常のバニラスポーンを復活させます。**カウント自体は消しません**（活性化のみ）ので、
装置を撤去すれば元の掃討済み状態に戻ります。

右クリックで UI が開き、半径と対象分類（敵対 / 中立 / 友好）、稼働の ON/OFF を設定できます。
レシピは鉄インゴット・レッドストーン・エンダーアイ・魂のランタンの定形クラフトです。

## 1.21.11 固有の注意点

1.21.11 は大規模な改名が入ったバージョンです。本 mod では以下に対応済みです。

- `ResourceLocation` -> `Identifier`（`net.minecraft.resources.Identifier`）
- vanilla の `BlockEntityType.Builder` が廃止 -> Fabric API の `FabricBlockEntityTypeBuilder` を使用
- BlockEntity の NBT は `CompoundTag` ではなく `ValueInput` / `ValueOutput`
  （`loadAdditional(ValueInput)` / `saveAdditional(ValueOutput)`）
- `Level#isClientSide` はフィールドが private 化。`isClientSide()` を使う

また 1.21.11 は最後の難読化バージョンで、次の `26.1` は非難読化になります。Yarn は
1.21.11 で更新停止のため、本プロジェクトは最初から Mojang マッピングで書いてあります。

## 実装上の注意（触るときに壊しやすい箇所）

`ActivatorState` と、そこを参照するコードから **チャンクを読み込ませる操作を呼んではいけません**。
具体的には `Level#getBlockEntity` / `Level#getChunk` などです。チャンク読み込み中に
これらを呼ぶと、自分自身の完了を待つデッドロックになりワールドの読み込みが停止します。
装置の実体確認は必ず `getChunkSource().getChunkNow(...)`（ノンブロッキング）経由で行い、
未ロードなら判定を保留してください。

レジストリへの登録は `SpawnActivatorBlock#setPlacedBy`（設置時）と UI からの設定変更時のみ、
削除は `affectNeighborsAfterRemoval`（破壊時）と上記の自己修復のみです。


