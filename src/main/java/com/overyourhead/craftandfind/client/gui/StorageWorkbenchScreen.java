package com.overyourhead.craftandfind.client.gui;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import com.overyourhead.craftandfind.mixin.client.RecipeBookComponentAccessor;
import com.overyourhead.craftandfind.mixin.client.SlotPositionAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class StorageWorkbenchScreen extends CraftingScreen {
    private static boolean rememberedRecipeTabActive;

    private StoragePanel storagePanel;
    private WorkbenchRecipePanel recipePanel;
    private WorkbenchIconButton compassButton;
    private WorkbenchIconButton customRecipeButton;
    private ImageButton vanillaRecipeBookButton;
    private boolean resultSlotShiftApplied;
    private boolean recipeTabActive = rememberedRecipeTabActive;

    public StorageWorkbenchScreen(CraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WorkbenchLayout.MAIN_WIDTH;
        imageHeight = WorkbenchLayout.MAIN_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        applyResultSlotShift();

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

        // Craft & Find owns its recipe panel. Keep Minecraft's recipe book hidden
        // even if another mod removes, redirects or disables the vanilla book.
        setRecipeBookVisible(false);

        storagePanel = new StoragePanel(font, menu.containerId, this::onClose);
        addWidget(storagePanel.searchBox());
        recipePanel = new WorkbenchRecipePanel(minecraft, font, menu);
        addWidget(recipePanel.searchBox());

        compassButton = addRenderableWidget(new WorkbenchIconButton(
                0,
                0,
                WorkbenchTextures.COMPASS_ICON,
                Component.translatable("gui.craftandfind.open_storage"),
                () -> storagePanel != null && storagePanel.isOpen(),
                this::toggleStoragePanel
        ));
        customRecipeButton = addRenderableWidget(new WorkbenchIconButton(
                0,
                0,
                WorkbenchTextures.RECIPE_BOOK_ICON,
                Component.translatable("gui.craftandfind.open_recipes"),
                () -> recipePanel != null && recipePanel.isOpen(),
                this::toggleRecipeBook
        ));
        applyActiveTabState();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The panel, every slot type and the crafting arrow are separate files.
        // Artists can replace one piece without repainting the whole screen.
        blitPart(
                graphics,
                WorkbenchTextures.MAIN_PANEL,
                leftPos,
                topPos,
                imageWidth,
                imageHeight
        );

        blitPart(
                graphics,
                WorkbenchTextures.CRAFTING_ARROW,
                leftPos + WorkbenchLayout.CRAFTING_ARROW_X,
                topPos + WorkbenchLayout.CRAFTING_ARROW_Y,
                WorkbenchLayout.CRAFTING_ARROW_WIDTH,
                WorkbenchLayout.CRAFTING_ARROW_HEIGHT
        );

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (slotIndex == 0) {
                blitPart(
                        graphics,
                        WorkbenchTextures.RESULT_SLOT,
                        leftPos + slot.x
                                - WorkbenchLayout.RESULT_SLOT_OFFSET
                                + WorkbenchLayout.RESULT_SLOT_X_ADJUST,
                        topPos + slot.y
                                - WorkbenchLayout.RESULT_SLOT_OFFSET
                                + WorkbenchLayout.RESULT_SLOT_Y_ADJUST,
                        WorkbenchLayout.RESULT_SLOT_SIZE,
                        WorkbenchLayout.RESULT_SLOT_SIZE
                );
                continue;
            }

            ResourceLocation slotTexture = slotIndex <= 9
                    ? WorkbenchTextures.CRAFTING_SLOT
                    : WorkbenchTextures.INVENTORY_SLOT;
            blitPart(
                    graphics,
                    slotTexture,
                    leftPos + slot.x - WorkbenchLayout.NORMAL_SLOT_OFFSET,
                    topPos + slot.y - WorkbenchLayout.NORMAL_SLOT_OFFSET,
                    WorkbenchLayout.NORMAL_SLOT_SIZE,
                    WorkbenchLayout.NORMAL_SLOT_SIZE
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                title,
                titleLabelX + WorkbenchLayout.CRAFTING_LABEL_X_OFFSET,
                titleLabelY,
                WorkbenchLayout.LABEL_COLOR,
                true
        );
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                WorkbenchLayout.LABEL_COLOR,
                true
        );
    }

    private static void blitPart(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (recipePanel != null) {
            recipePanel.tick();
        }
        updateCustomPositions();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateCustomPositions();
        super.render(graphics, mouseX, mouseY, partialTick);
        storagePanel.render(graphics, mouseX, mouseY, partialTick);
        recipePanel.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (storagePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (recipePanel.mouseClicked(mouseX, mouseY, button)) {
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
        if (recipePanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (storagePanel.isOpen() && storagePanel.searchBox().isFocused()) {
            storagePanel.searchBox().keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (recipePanel != null && recipePanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (storagePanel.isOpen() && storagePanel.searchBox().charTyped(codePoint, modifiers)) {
            return true;
        }
        if (recipePanel != null && recipePanel.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        // A real workbench close should drop the client snapshot immediately.
        // JEI/EMI recipe screens temporarily replace this screen without closing
        // the active crafting container, so that case is handled in removed().
        ClientStorageState.clear(menu.containerId);
        super.onClose();
    }

    @Override
    public void removed() {
        // Opening JEI/EMI Recipes/Uses calls removed() because another screen is
        // displayed, but the Storage Workbench menu itself is still active. Keep
        // the nearby-storage snapshot alive so the left panel is populated again
        // when the player returns. Clear only after the actual menu has changed.
        if (minecraft == null
                || minecraft.player == null
                || minecraft.player.containerMenu.containerId != menu.containerId) {
            ClientStorageState.clear(menu.containerId);
        }
        super.removed();
    }

    public boolean isStoragePanelOpen() {
        return storagePanel != null && storagePanel.isOpen();
    }

    public Rect2i storagePanelArea() {
        if (storagePanel == null) {
            return new Rect2i(0, 0, 0, 0);
        }
        return new Rect2i(storagePanel.left(), storagePanel.top(), StoragePanel.WIDTH, StoragePanel.HEIGHT);
    }

    public boolean isRecipePanelOpen() {
        return recipePanel != null && recipePanel.isOpen();
    }

    public Rect2i recipePanelArea() {
        if (recipePanel == null) {
            return new Rect2i(0, 0, 0, 0);
        }
        return new Rect2i(recipePanel.left() - 24, recipePanel.top(), WorkbenchRecipePanel.WIDTH + 24, WorkbenchRecipePanel.HEIGHT);
    }

    public ItemStack storageItemUnderMouse(double mouseX, double mouseY) {
        if (storagePanel == null) {
            return ItemStack.EMPTY;
        }
        return storagePanel.itemUnderMouse(mouseX, mouseY);
    }

    public ItemStack recipeItemUnderMouse(double mouseX, double mouseY) {
        if (recipePanel == null) {
            return ItemStack.EMPTY;
        }
        return recipePanel.itemUnderMouse(mouseX, mouseY);
    }

    public void onStorageUpdated() {
        if (storagePanel != null) {
            storagePanel.onStorageUpdated();
        }
        if (recipePanel != null) {
            recipePanel.onStorageUpdated();
        }

        // Keep vanilla's hidden ghost-recipe component in sync without using
        // its recipe collections as our source of recipes.
        super.recipesUpdated();
    }

    @Override
    public void recipesUpdated() {
        super.recipesUpdated();
        if (recipePanel != null) {
            recipePanel.recipesUpdated();
        }
    }

    /** Draws the ingredient layout for an unavailable recipe, like vanilla. */
    public void showGhostRecipe(ResourceLocation recipeId) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        // Server-side recipe switching normally clears the real 3x3 grid before
        // this packet arrives. If a full inventory/storage prevented that, never
        // draw a ghost on top of real items: keep the persistent grid readable.
        for (int slotIndex = 1; slotIndex <= 9; slotIndex++) {
            if (!menu.getSlot(slotIndex).getItem().isEmpty()) {
                return;
            }
        }

        minecraft.level.getRecipeManager().byKey(recipeId).ifPresent(recipe ->
                getRecipeBookComponent().setupGhostRecipe(recipe, menu.slots)
        );
    }

    private void toggleStoragePanel() {
        if (!recipeTabActive && storagePanel.isOpen()) {
            return;
        }

        recipeTabActive = false;
        rememberedRecipeTabActive = false;
        applyActiveTabState();
    }

    private void toggleRecipeBook() {
        if (recipeTabActive && recipePanel != null && recipePanel.isOpen()) {
            return;
        }

        recipeTabActive = true;
        rememberedRecipeTabActive = true;
        applyActiveTabState();
    }

    private void applyActiveTabState() {
        // The vanilla component remains hidden permanently. It is retained only
        // for vanilla ghost-recipe rendering, never as Craft & Find's recipe UI.
        if (getRecipeBookComponent().isVisible()) {
            setRecipeBookVisible(false);
        }

        if (recipeTabActive) {
            storagePanel.setOpen(false);
            recipePanel.setOpen(true);
        } else {
            recipePanel.setOpen(false);
            storagePanel.setOpen(true);
        }

        leftPos = mainLeftForActiveTab();
        updateCustomPositions();
    }

    private void setRecipeBookVisible(boolean visible) {
        ((RecipeBookComponentAccessor) (Object) getRecipeBookComponent())
                .craftandfind$setVisible(visible);
    }

    private int mainLeftForActiveTab() {
        if (width < 379) {
            return (width - imageWidth) / 2;
        }
        if (!recipeTabActive) {
            return (width - imageWidth) / 2 + 77;
        }

        // This is exactly the old themed vanilla recipe-book geometry. Keeping
        // the formula avoids even a one-pixel parity shift when switching tabs.
        int recipePanelX = (width - WorkbenchLayout.PANEL_WIDTH) / 2 - 86;
        return recipePanelX + WorkbenchLayout.PANEL_WIDTH + WorkbenchLayout.PANEL_GAP;
    }

    private void updateCustomPositions() {
        if (storagePanel == null || recipePanel == null || compassButton == null || customRecipeButton == null) {
            return;
        }

        leftPos = mainLeftForActiveTab();

        int buttonX = leftPos + WorkbenchLayout.TAB_BUTTON_X;
        compassButton.setX(buttonX);
        compassButton.setY(topPos + WorkbenchLayout.TAB_COMPASS_Y);
        customRecipeButton.setX(buttonX);
        customRecipeButton.setY(topPos + WorkbenchLayout.TAB_RECIPE_Y);
        storagePanel.updatePosition(leftPos, width, height);
        recipePanel.updatePosition(leftPos, width, height);
    }

    private void applyResultSlotShift() {
        if (resultSlotShiftApplied || menu.slots.isEmpty()) {
            return;
        }

        Slot resultSlot = menu.getSlot(0);
        SlotPositionAccessor position = (SlotPositionAccessor) resultSlot;
        position.craftandfind$setX(resultSlot.x + WorkbenchLayout.RESULT_SLOT_CONTENT_X_SHIFT);
        position.craftandfind$setY(resultSlot.y + WorkbenchLayout.RESULT_SLOT_CONTENT_Y_SHIFT);
        resultSlotShiftApplied = true;
    }

}
