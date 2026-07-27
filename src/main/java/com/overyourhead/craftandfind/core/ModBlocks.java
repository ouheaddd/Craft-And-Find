package com.overyourhead.craftandfind.core;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.common.block.StorageWorkbenchBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CraftAndFindMod.MOD_ID);

    public static final DeferredBlock<StorageWorkbenchBlock> STORAGE_WORKBENCH = BLOCKS.registerBlock(
            "storage_workbench",
            StorageWorkbenchBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
    );

    private ModBlocks() {
    }
}
