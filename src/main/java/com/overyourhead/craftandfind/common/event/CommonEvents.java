package com.overyourhead.craftandfind.common.event;

import com.overyourhead.craftandfind.common.menu.PersistentCraftingGrid;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class CommonEvents {
    private CommonEvents() {
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        PersistentCraftingGrid.copyToClone(event.getOriginal(), event.getEntity());
    }
}
