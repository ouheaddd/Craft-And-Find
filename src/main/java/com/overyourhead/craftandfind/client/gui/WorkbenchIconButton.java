package com.overyourhead.craftandfind.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Small workbench-side button used for both the storage and recipe tabs.
 * Keeping both buttons in one widget guarantees identical dimensions,
 * borders and hover treatment.
 */
public final class WorkbenchIconButton extends AbstractWidget {
    private final Runnable onPress;
    private final ItemStack icon;

    public WorkbenchIconButton(
            int x,
            int y,
            ItemStack icon,
            Component message,
            Runnable onPress
    ) {
        super(x, y, 20, 18, message);
        this.icon = icon.copyWithCount(1);
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int inner = isHoveredOrFocused() ? 0xFFE8E8E8 : 0xFFC6C6C6;

        // Vanilla-like raised slot frame. Both custom buttons use this exact drawing.
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, inner);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 2, y + 2, x + 3, y + height - 2, 0xFFFFFFFF);
        graphics.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, 0xFF555555);
        graphics.fill(x + width - 3, y + 2, x + width - 2, y + height - 2, 0xFF555555);
        graphics.renderItem(icon, x + 2, y + 1);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
