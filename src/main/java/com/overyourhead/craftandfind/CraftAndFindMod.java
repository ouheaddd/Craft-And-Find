package com.overyourhead.craftandfind;

import com.overyourhead.craftandfind.common.network.CraftAndFindNetwork;
import com.overyourhead.craftandfind.core.ModBlocks;
import com.overyourhead.craftandfind.core.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.Mod;

@Mod(CraftAndFindMod.MOD_ID)
public final class CraftAndFindMod {
    public static final String MOD_ID = "craftandfind";
    public static final int STORAGE_RADIUS = 16;

    public CraftAndFindMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        CraftAndFindNetwork.register(event);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.FUNCTIONAL_BLOCKS.equals(event.getTabKey())) {
            event.accept(ModItems.STORAGE_WORKBENCH_ITEM.get());
        }
    }
}
