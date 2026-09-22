package com.chunksweep.client.screen;

import com.chunksweep.category.SweepCategory;
import com.chunksweep.net.ActivatorConfigPayload;
import com.chunksweep.screen.SpawnActivatorMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.EnumMap;
import java.util.Map;

/**
 * 設定用 UI。スロットを持たないので背景テクスチャは使わず、塗りつぶしで描いている
 * （GUI テクスチャの用意が不要で、リソースパックとの衝突も起きない）。
 */
public class SpawnActivatorScreen extends AbstractContainerScreen<SpawnActivatorMenu> {

    private static final int PANEL = 0xF0161A20;
    private static final int BORDER = 0xFF56D6C4;
    private static final int TEXT = 0xFFE6E9EE;

    private final Map<SweepCategory, Button> categoryButtons = new EnumMap<>(SweepCategory.class);
    private RadiusSlider slider;
    private Button powerButton;

    private int radius;
    private int mask;
    private boolean active;

    public SpawnActivatorScreen(SpawnActivatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 150;
    }

    @Override
    protected void init() {
        super.init();
        this.radius = menu.radius();
        this.mask = menu.mask();
        this.active = menu.active();

        int x = leftPos + 12;
        int y = topPos + 34;

        slider = new RadiusSlider(x, y, imageWidth - 24, 20);
        addRenderableWidget(slider);

        y += 28;
        int bw = (imageWidth - 24 - 8) / 3;
        int i = 0;
        for (SweepCategory category : SweepCategory.VALUES) {
            int bx = x + i * (bw + 4);
            Button b = Button.builder(Component.translatable(category.translationKey()), btn -> {
                mask ^= category.bit();
                refreshLabels();
                send();
            }).bounds(bx, y, bw, 20).build();
            categoryButtons.put(category, b);
            addRenderableWidget(b);
            i++;
        }

        y += 28;
        powerButton = Button.builder(Component.empty(), btn -> {
            active = !active;
            refreshLabels();
            send();
        }).bounds(x, y, imageWidth - 24, 20).build();
        addRenderableWidget(powerButton);

        refreshLabels();
    }

    private void refreshLabels() {
        for (var entry : categoryButtons.entrySet()) {
            boolean on = (mask & entry.getKey().bit()) != 0;
            entry.getValue().setMessage(
                    Component.translatable(entry.getKey().translationKey())
                            .withStyle(style -> style.withColor(on ? 0x56D6C4 : 0x707880)));
        }
        powerButton.setMessage(Component.translatable(
                active ? "gui.chunksweep.enabled" : "gui.chunksweep.disabled"));
        if (slider != null) slider.updateMessage0();
    }

    private void send() {
        menu.setLocal(radius, mask, active);
        ClientPlayNetworking.send(new ActivatorConfigPayload(radius, mask, active));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, BORDER);
        graphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, BORDER);
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, BORDER);
        graphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, BORDER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 10, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.chunksweep.targets"), 12, 66, 0xFFA8B0BC, false);
        graphics.drawString(font, Component.translatable("gui.chunksweep.hint"), 12, imageHeight - 22, 0xFF7E8894, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        // スロットが無いので何もしない
    }

    private class RadiusSlider extends AbstractSliderButton {

        RadiusSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), 0.0);
            int max = Math.max(1, menu.maxRadius());
            this.value = (double) radius / max;
            updateMessage();
        }

        void updateMessage0() { updateMessage(); }

        @Override
        protected void updateMessage() {
            setMessage(radius == 0
                    ? Component.translatable("gui.chunksweep.radius.self_only")
                    : Component.translatable("gui.chunksweep.radius", radius));
        }

        @Override
        protected void applyValue() {
            int max = Math.max(0, menu.maxRadius());
            int newRadius = (int) Math.round(value * max);
            if (newRadius != radius) {
                radius = newRadius;
                send();
            }
            updateMessage();
        }
    }
}
