package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchLayout;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the original recipe-book styling intact. Only the requested search-row
 * positioning and magnifying-glass alignment are changed.
 */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookSearchStyleMixin {
    @Shadow
    private EditBox searchBox;

    @Shadow
    private StateSwitchingButton filterButton;

    @Unique
    private boolean craftandfind$searchRowCaptured;
    @Unique
    private int craftandfind$baseSearchX;
    @Unique
    private int craftandfind$baseSearchY;
    @Unique
    private int craftandfind$baseFilterX;
    @Unique
    private int craftandfind$baseFilterY;

    @Inject(method = "render", at = @At("HEAD"))
    private void craftandfind$positionSearchRow(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!craftandfind$ownScreen() || searchBox == null) {
            return;
        }

        craftandfind$captureSearchRowIfNeeded();
        searchBox.setX(craftandfind$baseSearchX + WorkbenchLayout.RECIPE_BOOK_SEARCH_SHIFT_X);
        searchBox.setY(craftandfind$baseSearchY + WorkbenchLayout.RECIPE_BOOK_SEARCH_SHIFT_Y);

        if (filterButton != null) {
            filterButton.setX(craftandfind$baseFilterX);
            filterButton.setY(craftandfind$baseFilterY + WorkbenchLayout.RECIPE_BOOK_FILTER_SHIFT_Y);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void craftandfind$renderVanillaSearchWithMovedIcon(
            EditBox renderedSearchBox,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!craftandfind$ownScreen()) {
            renderedSearchBox.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.blit(
                WorkbenchTextures.SEARCH_ICON,
                renderedSearchBox.getX() + WorkbenchLayout.RECIPE_BOOK_ICON_X_FROM_SEARCH,
                renderedSearchBox.getY() + WorkbenchLayout.RECIPE_BOOK_ICON_Y_FROM_SEARCH,
                0,
                0,
                16,
                16,
                16,
                16
        );
        renderedSearchBox.render(graphics, mouseX, mouseY, partialTick);
    }

    @Unique
    private void craftandfind$captureSearchRowIfNeeded() {
        int expectedSearchX = craftandfind$baseSearchX + WorkbenchLayout.RECIPE_BOOK_SEARCH_SHIFT_X;
        int expectedSearchY = craftandfind$baseSearchY + WorkbenchLayout.RECIPE_BOOK_SEARCH_SHIFT_Y;
        int expectedFilterX = craftandfind$baseFilterX;
        int expectedFilterY = craftandfind$baseFilterY + WorkbenchLayout.RECIPE_BOOK_FILTER_SHIFT_Y;

        boolean searchWasRepositionedByVanilla = !craftandfind$searchRowCaptured
                || searchBox.getX() != expectedSearchX
                || searchBox.getY() != expectedSearchY;
        boolean filterWasRepositionedByVanilla = filterButton != null
                && (filterButton.getX() != expectedFilterX || filterButton.getY() != expectedFilterY);

        if (!searchWasRepositionedByVanilla && !filterWasRepositionedByVanilla) {
            return;
        }

        craftandfind$baseSearchX = searchBox.getX();
        craftandfind$baseSearchY = searchBox.getY();
        if (filterButton != null) {
            craftandfind$baseFilterX = filterButton.getX();
            craftandfind$baseFilterY = filterButton.getY();
        }
        craftandfind$searchRowCaptured = true;
    }

    @Unique
    private static boolean craftandfind$ownScreen() {
        return Minecraft.getInstance().screen instanceof StorageWorkbenchScreen;
    }
}
