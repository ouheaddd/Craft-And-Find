package com.overyourhead.craftandfind.client.gui;

import com.overyourhead.craftandfind.client.ClientStorageState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public final class StorageWorkbenchScreen extends CraftingScreen {
    private StoragePanel storagePanel;
    private WorkbenchIconButton compassButton;
    private WorkbenchIconButton customRecipeButton;
    private ImageButton vanillaRecipeBookButton;

    public StorageWorkbenchScreen(CraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        for (GuiEventListener child : children()) {
            if (child instanceof ImageButton imageButton
                    && imageButton.getWidth() == 20
                    && imageButton.getHeight() == 18) {
                vanillaRecipeBookButton = imageButton;
                vanillaRecipeBookButton.visible = false;
                vanillaRecipeBookButton.active = false;
                break;
            }
        }

        storagePanel = new StoragePanel(font, menu.containerId);
        addWidget(storagePanel.searchBox());

        compassButton = addRenderableWidget(new WorkbenchIconButton(
                0,
                0,
                new ItemStack(Items.COMPASS),
                Component.translatable("gui.craftandfind.open_storage"),
                this::toggleStoragePanel
        ));
        customRecipeButton = addRenderableWidget(new WorkbenchIconButton(
                0,
                0,
                new ItemStack(Items.KNOWLEDGE_BOOK),
                Component.translatable("gui.craftandfind.open_recipes"),
                this::toggleRecipeBook
        ));
        updateCustomPositions();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateCustomPositions();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateCustomPositions();
        super.render(graphics, mouseX, mouseY, partialTick);
        storagePanel.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (storagePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (storagePanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (storagePanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (storagePanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Escape always closes the whole workbench, regardless of the open side tab.
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (storagePanel.isOpen()) {
            // Keep keyboard input inside the storage search. This also prevents the
            // inventory key (E) from closing the workbench while a query is typed.
            storagePanel.searchBox().setFocused(true);
            storagePanel.searchBox().keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (storagePanel.isOpen() && storagePanel.searchBox().charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void removed() {
        ClientStorageState.clear(menu.containerId);
        super.removed();
    }

    public void onStorageUpdated() {
        if (storagePanel != null) {
            storagePanel.onStorageUpdated();
        }

        // If the recipe book is already open, force its stacked-content cache to
        // rebuild immediately with the new nearby-storage snapshot.
        getRecipeBookComponent().slotClicked(menu.getSlot(1));
        recipesUpdated();
    }

    private void toggleStoragePanel() {
        if (!storagePanel.isOpen() && getRecipeBookComponent().isVisible()) {
            getRecipeBookComponent().toggleVisibility();
        }
        storagePanel.toggle();
        if (storagePanel.isOpen() && width >= 379) {
            leftPos = (width - imageWidth) / 2 + 77;
        } else {
            resetMainPosition();
        }
        updateCustomPositions();
    }

    private void toggleRecipeBook() {
        if (storagePanel.isOpen()) {
            storagePanel.setOpen(false);
        }
        getRecipeBookComponent().toggleVisibility();
        resetMainPosition();
        updateCustomPositions();
    }

    private void resetMainPosition() {
        leftPos = getRecipeBookComponent().isVisible()
                ? getRecipeBookComponent().updateScreenPosition(width, imageWidth)
                : (width - imageWidth) / 2;
    }

    private void updateCustomPositions() {
        if (storagePanel == null || compassButton == null || customRecipeButton == null) {
            return;
        }

        if (storagePanel.isOpen() && width >= 379) {
            leftPos = (width - imageWidth) / 2 + 77;
        }

        // Raise the pair compared with the previous build and keep an exact 3 px gap.
        int compassY = topPos + 27;
        int recipeY = compassY + 21;
        int buttonX = leftPos + 5;

        compassButton.setX(buttonX);
        compassButton.setY(compassY);
        customRecipeButton.setX(buttonX);
        customRecipeButton.setY(recipeY);
        storagePanel.updatePosition(leftPos, width, height);
    }
}
