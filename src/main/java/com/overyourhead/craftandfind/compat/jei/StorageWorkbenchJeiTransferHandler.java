package com.overyourhead.craftandfind.compat.jei;

import com.overyourhead.craftandfind.client.ClientStorageState;
import com.overyourhead.craftandfind.common.network.payload.PlaceRecipePayload;
import com.overyourhead.craftandfind.common.storage.StorageItemEntry;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

/**
 * JEI sees the client side of the Storage Workbench as a normal CraftingMenu.
 * The server owns StorageWorkbenchMenu, but MenuType.CRAFTING reconstructs a
 * vanilla CraftingMenu on the client. Therefore this handler must target
 * CraftingMenu and identify Craft & Find by its active storage snapshot.
 */
public final class StorageWorkbenchJeiTransferHandler
        implements IRecipeTransferHandler<CraftingMenu, RecipeHolder<CraftingRecipe>> {
    private static final int FIRST_INPUT_SLOT = 1;
    private static final int INPUT_SLOT_COUNT = 9;
    private static final int FIRST_PLAYER_SLOT = 10;
    private static final int PLAYER_SLOT_COUNT = 36;

    private final IRecipeTransferHandlerHelper helper;
    private final IRecipeTransferHandler<CraftingMenu, RecipeHolder<CraftingRecipe>> vanillaFallback;

    public StorageWorkbenchJeiTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;

        IRecipeTransferInfo<CraftingMenu, RecipeHolder<CraftingRecipe>> vanillaInfo =
                helper.createBasicRecipeTransferInfo(
                        CraftingMenu.class,
                        MenuType.CRAFTING,
                        RecipeTypes.CRAFTING,
                        FIRST_INPUT_SLOT,
                        INPUT_SLOT_COUNT,
                        FIRST_PLAYER_SLOT,
                        PLAYER_SLOT_COUNT
                );
        this.vanillaFallback = helper.createUnregisteredRecipeTransferHandler(vanillaInfo);
    }

    @Override
    public Class<? extends CraftingMenu> getContainerClass() {
        return CraftingMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingMenu>> getMenuType() {
        return Optional.of(MenuType.CRAFTING);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            CraftingMenu container,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
    ) {
        if (!isStorageWorkbench(container)) {
            return vanillaFallback.transferRecipe(
                    container,
                    recipe,
                    recipeSlots,
                    player,
                    maxTransfer,
                    doTransfer
            );
        }

        List<IRecipeSlotView> missingSlots = findMissingIngredientSlots(container, recipeSlots, player);
        if (!missingSlots.isEmpty()) {
            return helper.createUserErrorForMissingSlots(
                    Component.literal("Missing ingredients in inventory or nearby storage"),
                    missingSlots
            );
        }

        if (!doTransfer) {
            return null;
        }

        // Do not use Minecraft's recipe-book placement packet: NERB may empty
        // the vanilla recipe book. The Craft & Find packet goes directly to the
        // server-side StorageWorkbenchMenu and its NearbyStorage placement code.
        PacketDistributor.sendToServer(new PlaceRecipePayload(
                container.containerId,
                recipe.id(),
                maxTransfer
        ));
        return null;
    }

    private static boolean isStorageWorkbench(CraftingMenu container) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.containerMenu == container
                && ClientStorageState.isActive(container.containerId);
    }

    private static List<IRecipeSlotView> findMissingIngredientSlots(
            CraftingMenu container,
            IRecipeSlotsView recipeSlots,
            Player player
    ) {
        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)
                .stream()
                .filter(slot -> !slot.isEmpty())
                .toList();

        if (inputSlots.isEmpty()) {
            return List.of();
        }

        List<ItemStack> availableUnits = collectAvailableUnits(container, player, inputSlots.size());
        // JEI inputs backed by tags may expose hundreds or thousands of candidate
        // stacks. Never re-enumerate that stream for every inventory/storage unit
        // (and on every recursive attempt in the bipartite matcher).
        // Index available stacks by item, then build each slot's compatible
        // unit indices only once. Components are still checked exactly.
        Map<Item, List<Integer>> unitsByItem = new HashMap<>();
        for (int index = 0; index < availableUnits.size(); index++) {
            unitsByItem.computeIfAbsent(availableUnits.get(index).getItem(), ignored -> new ArrayList<>())
                    .add(index);
        }

        List<int[]> compatibleUnits = new ArrayList<>(inputSlots.size());
        for (IRecipeSlotView slot : inputSlots) {
            boolean[] accepted = new boolean[availableUnits.size()];
            slot.getItemStacks().forEach(candidate -> {
                List<Integer> indices = unitsByItem.get(candidate.getItem());
                if (indices == null) {
                    return;
                }
                for (int index : indices) {
                    if (ItemStack.isSameItemSameComponents(candidate, availableUnits.get(index))) {
                        accepted[index] = true;
                    }
                }
            });
            int count = 0;
            for (boolean value : accepted) {
                if (value) count++;
            }
            int[] indices = new int[count];
            int next = 0;
            for (int index = 0; index < accepted.length; index++) {
                if (accepted[index]) indices[next++] = index;
            }
            compatibleUnits.add(indices);
        }

        int[] unitOwners = new int[availableUnits.size()];
        Arrays.fill(unitOwners, -1);
        for (int inputIndex = 0; inputIndex < inputSlots.size(); inputIndex++) {
            boolean[] visitedUnits = new boolean[availableUnits.size()];
            tryAssignInput(inputIndex, compatibleUnits, unitOwners, visitedUnits);
        }

        boolean[] matchedInputs = new boolean[inputSlots.size()];
        for (int owner : unitOwners) {
            if (owner >= 0) {
                matchedInputs[owner] = true;
            }
        }

        List<IRecipeSlotView> missing = new ArrayList<>();
        for (int inputIndex = 0; inputIndex < inputSlots.size(); inputIndex++) {
            if (!matchedInputs[inputIndex]) {
                missing.add(inputSlots.get(inputIndex));
            }
        }
        return missing;
    }

    private static List<ItemStack> collectAvailableUnits(
            CraftingMenu container,
            Player player,
            int maxNeededPerStack
    ) {
        List<ItemStack> units = new ArrayList<>();

        for (ItemStack stack : player.getInventory().items) {
            addUnits(units, stack, stack.getCount(), maxNeededPerStack);
        }

        for (int slotIndex = FIRST_INPUT_SLOT; slotIndex < FIRST_INPUT_SLOT + INPUT_SLOT_COUNT; slotIndex++) {
            ItemStack stack = container.getSlot(slotIndex).getItem();
            addUnits(units, stack, stack.getCount(), maxNeededPerStack);
        }

        for (StorageItemEntry entry : ClientStorageState.entries(container.containerId)) {
            addUnits(units, entry.stack(), entry.count(), maxNeededPerStack);
        }

        return units;
    }

    private static void addUnits(
            List<ItemStack> units,
            ItemStack stack,
            int count,
            int maxNeededPerStack
    ) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }

        int unitCount = Math.min(count, maxNeededPerStack);
        for (int i = 0; i < unitCount; i++) {
            units.add(stack.copyWithCount(1));
        }
    }

    private static boolean tryAssignInput(
            int inputIndex,
            List<int[]> compatibleUnits,
            int[] unitOwners,
            boolean[] visitedUnits
    ) {
        for (int unitIndex : compatibleUnits.get(inputIndex)) {
            if (visitedUnits[unitIndex]) {
                continue;
            }
            visitedUnits[unitIndex] = true;
            int previousOwner = unitOwners[unitIndex];
            if (previousOwner == -1
                    || tryAssignInput(previousOwner, compatibleUnits, unitOwners, visitedUnits)) {
                unitOwners[unitIndex] = inputIndex;
                return true;
            }
        }
        return false;
    }
}
