package com.overyourhead.craftandfind.client;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.client.gui.StorageWorkbenchScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Gives Craft & Find first refusal on primary clicks inside its custom recipe
 * panel. This keeps JEI/other screen input hooks from swallowing the click.
 */
@EventBusSubscriber(modid = CraftAndFindMod.MOD_ID, value = Dist.CLIENT)
public final class CraftAndFindScreenEvents {
    private CraftAndFindScreenEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (!(event.getScreen() instanceof StorageWorkbenchScreen screen)) {
            return;
        }

        if (screen.handleRecipePanelPrimaryClick(event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }
}
