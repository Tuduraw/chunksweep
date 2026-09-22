package com.chunksweep.category;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * 掃討カウントの分類。mob の種類は区別せず、この 3 分類のみで集計する。
 */
public enum SweepCategory implements StringRepresentable {
    HOSTILE("hostile", 0),
    NEUTRAL("neutral", 1),
    FRIENDLY("friendly", 2);

    public static final SweepCategory[] VALUES = values();
    public static final Codec<SweepCategory> CODEC = StringRepresentable.fromEnum(SweepCategory::values);

    private final String name;
    private final int index;

    SweepCategory(String name, int index) {
        this.name = name;
        this.index = index;
    }

    /** ビットマスク・配列添字に使う安定した番号。NBT 互換のため値を変更しないこと。 */
    public int index() { return index; }

    public int bit() { return 1 << index; }

    public String translationKey() { return "gui.chunksweep.category." + name; }

    @Override
    public String getSerializedName() { return name; }

    public static SweepCategory byIndex(int i) { return VALUES[Math.floorMod(i, VALUES.length)]; }

    public static int allBits() {
        int m = 0;
        for (SweepCategory c : VALUES) m |= c.bit();
        return m;
    }
}
