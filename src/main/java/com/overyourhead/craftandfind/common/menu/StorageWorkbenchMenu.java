package com.overyourhead.craftandfind.common.menu;

import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.common.network.payload.GhostRecipePayload;
import com.overyourhead.craftandfind.common.network.payload.StorageSnapshotPayload;
import com.overyourhead.craftandfind.common.recipe.NearbyServerPlaceRecipe;
import com.overyourhead.craftandfind.common.storage.NearbyStorage;
import com.overyourhead.craftandfind.core.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public final class StorageWorkbenchMenu extends CraftingMenu {
    private static final int SNAPSHOT_INTERVAL_TICKS = 20;

    private final ContainerLevelAccess workbenchAccess;
    private final BlockPos workbenchPos;
    private final Level level;
    private final ServerPlayer serverPlayer;
    private NearbyStorage cachedStorage;
    private int snapshotTicker;

    public StorageWorkbenchMenu(
            int containerId,
            Inventory playerInventory,
            ContainerLevelAccess access,
            BlockPos workbenchPos
    ) {
        super(containerId, playerInventory, access);
        this.workbenchAccess = access;
        this.workbenchPos = workbenchPos.immutable();
        this.level = playerInventory.player.level();
        this.serverPlayer = playerInventory.player instanceof ServerPlayer player ? player : null;
        this.cachedStorage = serverPlayer == null
                ? NearbyStorage.empty(workbenchPos)
                : NearbyStorage.scan(level, workbenchPos, CraftAndFindMod.STORAGE_RADIUS);
        this.snapshotTicker = SNAPSHOT_INTERVAL_TICKS;

        if (serverPlayer != null) {
            PersistentCraftingGrid.load(serverPlayer, workbenchPos, this);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (serverPlayer != null) {
            PersistentCraftingGrid.save(serverPlayer, workbenchPos, this);
        }
    }

    @Override
    public void removed(Player player) {
        if (player instanceof ServerPlayer closingPlayer) {
            PersistentCraftingGrid.save(closingPlayer, workbenchPos, this);
        }

        // CraftingMenu#removed normally returns the grid to the player. The
        // storage workbench intentionally keeps it laid out for the next open.
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(workbenchAccess, player, ModBlocks.STORAGE_WORKBENCH.get());
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents contents) {
        super.fillCraftSlotsStackedContents(contents);
        if (serverPlayer != null) {
            storage().account(contents);
        }
    }

    @Override
    public void handlePlacement(boolean placeAll, RecipeHolder<?> recipe, ServerPlayer player) {
        if (recipe.value() instanceof CraftingRecipe) {
            @SuppressWarnings("unchecked")
            RecipeHolder<CraftingRecipe> craftingRecipe = (RecipeHolder<CraftingRecipe>) recipe;
            NearbyStorage storage = refreshStorage();

            if (!canCraft(player, craftingRecipe, storage)) {
                PacketDistributor.sendToPlayer(
                        player,
                        new GhostRecipePayload(containerId, craftingRecipe.id())
                );
                return;
            }

            beginPlacingRecipe();
            try {
                new NearbyServerPlaceRecipe(this, storage)
                        .recipeClicked(player, craftingRecipe, placeAll);
            } finally {
                finishPlacingRecipe(craftingRecipe);
            }
            sendSnapshot();
            return;
        }

        super.handlePlacement(placeAll, recipe, player);
    }

    private boolean canCraft(
            ServerPlayer player,
            RecipeHolder<CraftingRecipe> recipe,
            NearbyStorage storage
    ) {
        StackedContents contents = new StackedContents();
        player.getInventory().fillStackedContents(contents);
        super.fillCraftSlotsStackedContents(contents);
        storage.account(contents);
        return contents.canCraft(recipe.value(), null);
    }


    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverPlayer == null) {
            return;
        }

        snapshotTicker++;
        if (snapshotTicker >= SNAPSHOT_INTERVAL_TICKS) {
            snapshotTicker = 0;
            refreshStorage();
            sendSnapshot();
        }
    }

    public NearbyStorage refreshStorage() {
        if (serverPlayer != null) {
            cachedStorage = NearbyStorage.scan(level, workbenchPos, CraftAndFindMod.STORAGE_RADIUS);
        }
        return cachedStorage;
    }

    public NearbyStorage storage() {
        return cachedStorage;
    }

    public BlockPos workbenchPos() {
        return workbenchPos;
    }

    private void sendSnapshot() {
        if (serverPlayer != null) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new StorageSnapshotPayload(containerId, cachedStorage.snapshot())
            );
        }
    }
}
