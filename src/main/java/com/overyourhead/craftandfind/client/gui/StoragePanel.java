package com.overyourhead.craftandfind.client.gui;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.common.network.payload.HighlightRequestPayload;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StoragePanel {
    public static final int WIDTH = 147;
    public static final int HEIGHT = 166;

    private static final int COLUMNS = 5;
    private static final int ROWS = 5;
    private static final int CELL = 25;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 34;
    private static final int GRID_HEIGHT = ROWS * CELL;
    private static final int SCROLLBAR_X = 136;
    private static final int SCROLLBAR_WIDTH = 7;
    private static final ResourceLocation RECIPE_BOOK_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/gui/recipe_book.png"
    );

    private final Font font;
    private final int containerId;
    private final EditBox searchBox;
    private final List<StorageItemEntry> filteredEntries = new ArrayList<>();

    private boolean open;
    private int x;
    private int y;
    private int scrollRow;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public StoragePanel(Font font, int containerId) {
        this.font = font;
        this.containerId = containerId;
        this.searchBox = new EditBox(
                font,
                0,
                0,
                95,
                14,
                Component.translatable("gui.craftandfind.search")
        );
        this.searchBox.setHint(Component.translatable("gui.craftandfind.search"));
        this.searchBox.setResponder(ignored -> rebuildFilter());
        this.searchBox.visible = false;
        rebuildFilter();
    }

    public EditBox searchBox() {
        return searchBox;
    }

    public boolean isOpen() {
        return open;
    }

    public void toggle() {
        setOpen(!open);
    }

    public void setOpen(boolean open) {
        this.open = open;
        searchBox.visible = open;
        searchBox.setFocused(open);
        if (!open) {
            searchBox.setFocused(false);
            draggingScrollbar = false;
        }
    }

    public void updatePosition(int mainLeft, int screenWidth, int screenHeight) {
        x = screenWidth < 379 ? mainLeft : mainLeft - WIDTH - 4;
        y = (screenHeight - HEIGHT) / 2;
        searchBox.setX(x + 25);
        searchBox.setY(y + 13);
    }

    public void onStorageUpdated() {
        rebuildFilter();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            return;
        }

        graphics.blit(RECIPE_BOOK_TEXTURE, x, y, 1, 1, WIDTH, HEIGHT);
        searchBox.render(graphics, mouseX, mouseY, partialTick);

        if (filteredEntries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.craftandfind.no_items"),
                    x + WIDTH / 2,
                    y + 78,
                    0xFF404040
            );
            return;
        }

        int firstIndex = scrollRow * COLUMNS;
        int maxVisible = COLUMNS * ROWS;
        int endIndex = Math.min(filteredEntries.size(), firstIndex + maxVisible);

        for (int index = firstIndex; index < endIndex; index++) {
            int visibleIndex = index - firstIndex;
            int column = visibleIndex % COLUMNS;
            int row = visibleIndex / COLUMNS;
            int itemX = x + GRID_X + column * CELL;
            int itemY = y + GRID_Y + row * CELL;
            StorageItemEntry entry = filteredEntries.get(index);

            boolean hovered = mouseX >= itemX && mouseX < itemX + CELL
                    && mouseY >= itemY && mouseY < itemY + CELL;
            renderSlotFrame(graphics, itemX, itemY, hovered);
            graphics.renderItem(entry.stack(), itemX + 4, itemY + 4);
            graphics.renderItemDecorations(font, entry.stack(), itemX + 4, itemY + 4, null);
            renderCount(graphics, entry, itemX, itemY);
        }

        renderScrollbar(graphics, mouseX, mouseY);

        StorageItemEntry hoveredEntry = entryAt(mouseX, mouseY);
        if (hoveredEntry != null) {
            graphics.renderTooltip(font, hoveredEntry.stack(), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) {
            return false;
        }

        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && handleScrollbarClick(mouseX, mouseY)) {
            return true;
        }

        if (button == 0) {
            StorageItemEntry entry = entryAt(mouseX, mouseY);
            if (entry != null) {
                PacketDistributor.sendToServer(new HighlightRequestPayload(containerId, entry.stack()));
                return true;
            }
        }

        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!open || button != 0 || !draggingScrollbar) {
            return false;
        }

        updateScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!open || mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + HEIGHT) {
            return false;
        }

        int maxRows = maxScrollRows();
        if (scrollY < 0) {
            scrollRow = Math.min(maxRows, scrollRow + 1);
        } else if (scrollY > 0) {
            scrollRow = Math.max(0, scrollRow - 1);
        }
        return true;
    }

    private void renderSlotFrame(GuiGraphics graphics, int itemX, int itemY, boolean hovered) {
        int border = hovered ? 0xFFFFFFFF : 0xFF151515;
        int fill = hovered ? 0xFF6D6D6D : 0xFF343434;

        // CELL-sized frames touch one another, so no background holes remain between items.
        graphics.fill(itemX, itemY, itemX + CELL, itemY + CELL, border);
        graphics.fill(itemX + 2, itemY + 2, itemX + CELL - 2, itemY + CELL - 2, fill);
    }

    private void renderCount(GuiGraphics graphics, StorageItemEntry entry, int itemX, int itemY) {
        String text = entry.count() > 512 ? "512+" : Integer.toString(entry.count());
        int textWidth = font.width(text);
        float scale = textWidth <= 16 ? 1.0F : 16.0F / textWidth;

        graphics.pose().pushPose();
        graphics.pose().translate(itemX + CELL - 3.0F, itemY + CELL - 3.0F, 200.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -textWidth, -8, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int maxRows = maxScrollRows();
        if (maxRows <= 0) {
            return;
        }

        int trackX = x + SCROLLBAR_X;
        int trackY = y + GRID_Y;
        int thumbHeight = scrollbarThumbHeight();
        int thumbY = scrollbarThumbY();
        boolean hovered = mouseX >= trackX && mouseX < trackX + SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY < thumbY + thumbHeight;

        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + GRID_HEIGHT, 0xFF111111);
        graphics.fill(trackX + 1, trackY + 1, trackX + SCROLLBAR_WIDTH - 1, trackY + GRID_HEIGHT - 1, 0xFF2A2A2A);
        graphics.fill(
                trackX + 1,
                thumbY,
                trackX + SCROLLBAR_WIDTH - 1,
                thumbY + thumbHeight,
                hovered || draggingScrollbar ? 0xFFD8D8D8 : 0xFFA0A0A0
        );
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (maxScrollRows() <= 0) {
            return false;
        }

        int trackX = x + SCROLLBAR_X;
        int trackY = y + GRID_Y;
        if (mouseX < trackX || mouseX >= trackX + SCROLLBAR_WIDTH
                || mouseY < trackY || mouseY >= trackY + GRID_HEIGHT) {
            return false;
        }

        int thumbY = scrollbarThumbY();
        int thumbHeight = scrollbarThumbHeight();
        if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
            scrollbarGrabOffset = (int) mouseY - thumbY;
        } else {
            scrollbarGrabOffset = thumbHeight / 2;
        }

        draggingScrollbar = true;
        updateScrollFromMouse(mouseY);
        return true;
    }

    private void updateScrollFromMouse(double mouseY) {
        int maxRows = maxScrollRows();
        int thumbHeight = scrollbarThumbHeight();
        int movablePixels = GRID_HEIGHT - thumbHeight;
        if (maxRows <= 0 || movablePixels <= 0) {
            scrollRow = 0;
            return;
        }

        double relative = mouseY - (y + GRID_Y) - scrollbarGrabOffset;
        relative = Math.max(0.0D, Math.min(movablePixels, relative));
        scrollRow = (int) Math.round(relative * maxRows / movablePixels);
    }

    private int scrollbarThumbHeight() {
        int totalRows = totalRows();
        return Math.max(18, GRID_HEIGHT * ROWS / Math.max(ROWS, totalRows));
    }

    private int scrollbarThumbY() {
        int maxRows = maxScrollRows();
        int movablePixels = GRID_HEIGHT - scrollbarThumbHeight();
        if (maxRows <= 0 || movablePixels <= 0) {
            return y + GRID_Y;
        }
        return y + GRID_Y + Math.round((float) movablePixels * scrollRow / maxRows);
    }

    private int totalRows() {
        return (filteredEntries.size() + COLUMNS - 1) / COLUMNS;
    }

    private int maxScrollRows() {
        return Math.max(0, totalRows() - ROWS);
    }

    private StorageItemEntry entryAt(double mouseX, double mouseY) {
        int relativeX = (int) mouseX - (x + GRID_X);
        int relativeY = (int) mouseY - (y + GRID_Y);
        if (relativeX < 0 || relativeY < 0) {
            return null;
        }

        int column = relativeX / CELL;
        int row = relativeY / CELL;
        if (column >= COLUMNS || row >= ROWS) {
            return null;
        }

        int index = (scrollRow + row) * COLUMNS + column;
        return index >= 0 && index < filteredEntries.size() ? filteredEntries.get(index) : null;
    }

    private void rebuildFilter() {
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        filteredEntries.clear();
        for (StorageItemEntry entry : ClientStorageState.entries(containerId)) {
            String name = entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (query.isEmpty() || name.contains(query)) {
                filteredEntries.add(entry);
            }
        }
        scrollRow = 0;
        draggingScrollbar = false;
    }
}
