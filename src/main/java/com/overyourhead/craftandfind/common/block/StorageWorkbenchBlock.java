package com.overyourhead.craftandfind.common.block;

import com.overyourhead.craftandfind.common.menu.StorageWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class StorageWorkbenchBlock extends CraftingTableBlock {
    // Keep the screen title exactly vanilla; the block itself is named Storage Workbench.
    private static final Component TITLE = Component.translatable("container.crafting");

    public StorageWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new StorageWorkbenchMenu(
                        containerId,
                        inventory,
                        ContainerLevelAccess.create(level, pos),
                        pos
                ),
                TITLE
        );
    }
}
