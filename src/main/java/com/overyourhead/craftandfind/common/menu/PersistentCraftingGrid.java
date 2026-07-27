package com.overyourhead.craftandfind.common.menu;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * Stores a private crafting grid for every player, dimension and concrete
 * Storage Workbench position. Two players using the same block never share
 * ingredients, and one player can keep different layouts in different tables.
 */
public final class PersistentCraftingGrid {
    private static final String DATA_KEY = CraftAndFindMod.MOD_ID + "_crafting_grids";
    private static final int FIRST_INPUT_SLOT = 1;
    private static final int INPUT_SLOT_COUNT = 9;

    private PersistentCraftingGrid() {
    }

    public static void load(ServerPlayer player, BlockPos workbenchPos, StorageWorkbenchMenu menu) {
        CompoundTag playerData = player.getPersistentData();
        if (!playerData.contains(DATA_KEY)) {
            return;
        }

        CompoundTag allGrids = playerData.getCompound(DATA_KEY);
        String dimensionKey = dimensionKey(player);
        if (!allGrids.contains(dimensionKey)) {
            return;
        }

        CompoundTag dimensionGrids = allGrids.getCompound(dimensionKey);
        String positionKey = positionKey(workbenchPos);
        if (!dimensionGrids.contains(positionKey)) {
            return;
        }

        NonNullList<ItemStack> savedItems = NonNullList.withSize(INPUT_SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(
                dimensionGrids.getCompound(positionKey),
                savedItems,
                player.registryAccess()
        );

        for (int index = 0; index < INPUT_SLOT_COUNT; index++) {
            menu.getSlot(FIRST_INPUT_SLOT + index).set(savedItems.get(index).copy());
        }
    }

    public static void save(ServerPlayer player, BlockPos workbenchPos, StorageWorkbenchMenu menu) {
        NonNullList<ItemStack> items = NonNullList.withSize(INPUT_SLOT_COUNT, ItemStack.EMPTY);
        boolean hasAnyItem = false;

        for (int index = 0; index < INPUT_SLOT_COUNT; index++) {
            ItemStack stack = menu.getSlot(FIRST_INPUT_SLOT + index).getItem().copy();
            items.set(index, stack);
            hasAnyItem |= !stack.isEmpty();
        }

        CompoundTag playerData = player.getPersistentData();
        CompoundTag allGrids = playerData.contains(DATA_KEY)
                ? playerData.getCompound(DATA_KEY)
                : new CompoundTag();
        String dimensionKey = dimensionKey(player);
        CompoundTag dimensionGrids = allGrids.contains(dimensionKey)
                ? allGrids.getCompound(dimensionKey)
                : new CompoundTag();
        String positionKey = positionKey(workbenchPos);

        if (hasAnyItem) {
            CompoundTag gridTag = new CompoundTag();
            ContainerHelper.saveAllItems(gridTag, items, player.registryAccess());
            dimensionGrids.put(positionKey, gridTag);
        } else {
            dimensionGrids.remove(positionKey);
        }

        allGrids.put(dimensionKey, dimensionGrids);
        playerData.put(DATA_KEY, allGrids);
    }

    private static String dimensionKey(ServerPlayer player) {
        return player.level().dimension().location().toString();
    }

    private static String positionKey(BlockPos position) {
        return Long.toString(position.asLong());
    }
}
