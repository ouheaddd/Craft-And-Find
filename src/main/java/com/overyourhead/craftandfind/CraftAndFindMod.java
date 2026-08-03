package com.overyourhead.craftandfind;

import com.overyourhead.craftandfind.common.event.CommonEvents;
import com.overyourhead.craftandfind.config.CraftAndFindClientConfig;
import com.overyourhead.craftandfind.config.CraftAndFindServerConfig;
import com.overyourhead.craftandfind.common.network.CraftAndFindNetwork;
import com.overyourhead.craftandfind.core.ModBlocks;
import com.overyourhead.craftandfind.core.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CraftAndFindMod.MOD_ID)
public final class CraftAndFindMod {
    public static final String MOD_ID = "craftandfind";
    public CraftAndFindMod(IEventBus modBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CraftAndFindClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, CraftAndFindServerConfig.SPEC);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::addCreativeTabContents);
        NeoForge.EVENT_BUS.addListener(CommonEvents::onPlayerClone);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        CraftAndFindNetwork.register(event);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.FUNCTIONAL_BLOCKS.equals(event.getTabKey())) {
            event.insertAfter(
                    Blocks.ENCHANTING_TABLE.asItem().getDefaultInstance(),
                    ModItems.STORAGE_WORKBENCH_ITEM.get().getDefaultInstance(),
                    CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }
}
