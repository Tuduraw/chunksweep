package com.chunksweep.block;

import com.chunksweep.ChunkSweepConfig;
import com.chunksweep.category.SweepCategory;
import com.chunksweep.data.ActivatorEntry;
import com.chunksweep.data.ActivatorState;
import com.chunksweep.screen.SpawnActivatorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SpawnActivatorBlockEntity extends BlockEntity
        implements MenuProvider, ExtendedScreenHandlerFactory<BlockPos> {

    private int radius;
    private int categoryMask;
    private boolean active = true;

    public SpawnActivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPAWN_ACTIVATOR, pos, state);
        ChunkSweepConfig cfg = ChunkSweepConfig.get();
        this.radius = cfg.defaultActivatorRadius;
        this.categoryMask = SweepCategory.allBits();
    }

    // ---- 設定値 -------------------------------------------------------------

    public int radius() { return radius; }
    public int categoryMask() { return categoryMask; }
    public boolean active() { return active; }

    public void applySettings(int radius, int categoryMask, boolean active) {
        int max = ChunkSweepConfig.get().maxActivatorRadius;
        this.radius = Math.clamp(radius, 0, max);
        this.categoryMask = categoryMask & SweepCategory.allBits();
        this.active = active;
        setChanged();
        syncToState();
        if (level != null) {
            BlockState state = getBlockState();
            boolean lit = active && this.categoryMask != 0;
            if (state.getValue(SpawnActivatorBlock.POWERED) != lit) {
                level.setBlock(worldPosition, state.setValue(SpawnActivatorBlock.POWERED, lit), Block.UPDATE_ALL);
            }
        }
    }

    // ---- レジストリ同期 -----------------------------------------------------

    /** SavedData 側のレジストリへ自分の設定を書き戻す。 */
    public void syncToState() {
        if (level instanceof ServerLevel serverLevel) {
            ActivatorState.get(serverLevel)
                    .put(new ActivatorEntry(worldPosition, radius, categoryMask, active));
        }
    }

    // ---- NBT ----------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.radius = input.getIntOr("radius", ChunkSweepConfig.get().defaultActivatorRadius);
        this.categoryMask = input.getIntOr("mask", SweepCategory.allBits());
        this.active = input.getBooleanOr("active", true);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("radius", radius);
        output.putInt("mask", categoryMask);
        output.putBoolean("active", active);
    }

    // ---- MenuProvider -------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.chunksweep.spawn_activator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        return new SpawnActivatorMenu(syncId, inventory, this);
    }

    /** ExtendedScreenHandlerType にクライアントへ送る初期データを渡す。 */
    @Override
    public BlockPos getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) {
        return worldPosition;
    }
}
