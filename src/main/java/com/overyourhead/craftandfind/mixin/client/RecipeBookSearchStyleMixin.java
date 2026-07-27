package com.overyourhead.craftandfind.mixin.client;

import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import com.overyourhead.craftandfind.client.gui.workbench.WorkbenchTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the vanilla recipe-book search box intact in Craft & Find, and only
 * adds the magnifying-glass icon so the row visually matches the storage tab.
 */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookSearchStyleMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void craftandfind$renderVanillaSearchWithIcon(
            EditBox searchBox,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (!(Minecraft.getInstance().screen instanceof StorageWorkbenchScreen)) {
            searchBox.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int iconX = searchBox.getX() - 20;
        int iconY = searchBox.getY() - 1;
        graphics.blit(
                WorkbenchTextures.SEARCH_ICON,
                iconX,
                iconY,
                0,
                0,
                16,
                16,
                16,
                16
        );

        searchBox.render(graphics, mouseX, mouseY, partialTick);
    }
}
