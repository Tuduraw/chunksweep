package com.chunksweep.screen;

import com.chunksweep.ChunkSweepConfig;
import com.chunksweep.block.ModBlocks;
import com.chunksweep.block.SpawnActivatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * スロットを持たない設定用メニュー。値の同期は DataSlot（サーバ -> クライアント）と
 * カスタムペイロード（クライアント -> サーバ）で行う。
 */
public class SpawnActivatorMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final BlockPos pos;
    @Nullable
    private final SpawnActivatorBlockEntity blockEntity;

    private final DataSlot radius = DataSlot.standalone();
    private final DataSlot mask = DataSlot.standalone();
    private final DataSlot active = DataSlot.standalone();
    private final DataSlot maxRadius = DataSlot.standalone();

    /** クライアント側コンストラクタ（ExtendedScreenHandlerType から呼ばれる）。 */
    public SpawnActivatorMenu(int syncId, Inventory inventory, BlockPos pos) {
        super(ModMenus.SPAWN_ACTIVATOR, syncId);
        this.access = ContainerLevelAccess.NULL;
        this.pos = pos;
        this.blockEntity = null;
        addDataSlots();
    }

    /** サーバ側コンストラクタ。 */
    public SpawnActivatorMenu(int syncId, Inventory inventory, SpawnActivatorBlockEntity be) {
        super(ModMenus.SPAWN_ACTIVATOR, syncId);
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.pos = be.getBlockPos();
        this.blockEntity = be;
        addDataSlots();
        radius.set(be.radius());
        mask.set(be.categoryMask());
        active.set(be.active() ? 1 : 0);
        maxRadius.set(ChunkSweepConfig.get().maxActivatorRadius);
    }

    private void addDataSlots() {
        addDataSlot(radius);
        addDataSlot(mask);
        addDataSlot(active);
        addDataSlot(maxRadius);
    }

    public BlockPos pos() { return pos; }

    public int radius() { return radius.get(); }
    public int mask() { return mask.get(); }
    public boolean active() { return active.get() != 0; }
    public int maxRadius() { return Math.max(0, maxRadius.get()); }

    /** クライアント側のプレビュー用（実際の適用はサーバ側で行う）。 */
    public void setLocal(int radius, int mask, boolean active) {
        this.radius.set(radius);
        this.mask.set(mask);
        this.active.set(active ? 1 : 0);
    }

    /** サーバ側で設定を適用する。 */
    public void apply(int newRadius, int newMask, boolean newActive) {
        if (blockEntity == null) return;
        blockEntity.applySettings(newRadius, newMask, newActive);
        radius.set(blockEntity.radius());
        mask.set(blockEntity.categoryMask());
        active.set(blockEntity.active() ? 1 : 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return access == ContainerLevelAccess.NULL
                || stillValid(access, player, ModBlocks.SPAWN_ACTIVATOR);
    }

    public @Nullable Level level() {
        return blockEntity == null ? null : blockEntity.getLevel();
    }
}
