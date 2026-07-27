package com.overyourhead.craftandfind.core;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CraftAndFindMod.MOD_ID);

    public static final DeferredItem<BlockItem> STORAGE_WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem(
            ModBlocks.STORAGE_WORKBENCH,
            new Item.Properties()
    );

    private ModItems() {
    }
}
