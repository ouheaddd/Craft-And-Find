package com.overyourhead.craftandfind.common.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cached view of loaded container positions inside the workbench radius.
 * Items are never stored here: every operation resolves the current block
 * entity and reads or moves stacks in the original container.
 */
public final class NearbyStorage {
    private final Level level;
    private final BlockPos origin;
    private final List<ContainerReference> containers;

    private NearbyStorage(Level level, BlockPos origin, List<ContainerReference> containers) {
        this.level = level;
        this.origin = origin.immutable();
        this.containers = List.copyOf(containers);
    }

    public static NearbyStorage empty(BlockPos origin) {
        return new NearbyStorage(null, origin, List.of());
    }

    /**
     * Performs the relatively expensive radius scan. Only container positions
     * are cached, so slot contents can still change immediately between scans.
     */
    public static NearbyStorage scan(Level level, BlockPos origin, int radius) {
        List<ContainerReference> found = new ArrayList<>();
        BlockPos min = origin.offset(-radius, -radius, -radius);
        BlockPos max = origin.offset(radius, radius, radius);
        double radiusSquared = (double) radius * radius;

        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();
            if (pos.equals(origin)
                    || pos.distSqr(origin) > radiusSquared
                    || !level.hasChunkAt(pos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container) {
                found.add(new ContainerReference(pos, (int) pos.distSqr(origin)));
            }
        }

        return new NearbyStorage(level, origin, found);
    }

    /**
     * Builds the item list shown in the storage panel. Item components are part
     * of identity, so differently enchanted or otherwise component-bearing
     * stacks remain separate entries.
     */
    public List<StorageItemEntry> snapshot() {
        List<MutableEntry> totals = new ArrayList<>();

        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (stack.isEmpty()) {
                continue;
            }

            MutableEntry existing = null;
            for (MutableEntry candidate : totals) {
                if (ItemStack.isSameItemSameComponents(candidate.stack, stack)) {
                    existing = candidate;
                    break;
                }
            }

            if (existing == null) {
                totals.add(new MutableEntry(stack.copyWithCount(1), stack.getCount()));
            } else {
                existing.count = saturatedAdd(existing.count, stack.getCount());
            }
        }

        List<StorageItemEntry> result = new ArrayList<>(totals.size());
        for (MutableEntry total : totals) {
            result.add(new StorageItemEntry(total.stack, total.count));
        }
        result.sort(Comparator
                .comparing((StorageItemEntry value) -> value.stack().getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(value -> value.stack().getDescriptionId()));
        return List.copyOf(result);
    }

    /** Adds nearby storage stacks to vanilla's recipe availability counter. */
    public void account(StackedContents contents) {
        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (!stack.isEmpty()) {
                contents.accountStack(stack, stack.getCount());
            }
        }
    }

    /**
     * Moves up to {@code remaining} matching items into a crafting-grid slot.
     * Started storage stacks are consumed first, then nearer containers.
     * Returns the number of items still missing after the operation.
     */
    public int moveToCraftingSlot(Slot target, ItemStack wanted, int remaining) {
        if (remaining <= 0 || wanted.isEmpty()) {
            return 0;
        }

        List<SlotReference> candidates = currentSlots().stream()
                .filter(reference -> {
                    ItemStack stack = reference.currentStack();
                    return !stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, wanted);
                })
                .sorted(Comparator
                        .comparingInt((SlotReference reference) -> isStartedStack(reference.currentStack()) ? 0 : 1)
                        .thenComparingInt(SlotReference::distanceSquared)
                        .thenComparingLong(reference -> reference.pos().asLong()))
                .toList();

        for (SlotReference reference : candidates) {
            if (remaining <= 0) {
                break;
            }

            ItemStack source = reference.currentStack();
            if (source.isEmpty()) {
                continue;
            }

            ItemStack targetStack = target.getItem();
            if (!targetStack.isEmpty() && !ItemStack.isSameItemSameComponents(targetStack, wanted)) {
                break;
            }

            int targetCount = targetStack.isEmpty() ? 0 : targetStack.getCount();
            int room = Math.min(target.getMaxStackSize(wanted), wanted.getMaxStackSize()) - targetCount;
            if (room <= 0) {
                break;
            }

            int moved = Math.min(Math.min(source.getCount(), remaining), room);
            if (moved <= 0) {
                continue;
            }

            if (targetStack.isEmpty()) {
                target.set(wanted.copyWithCount(moved));
            } else {
                targetStack.grow(moved);
                target.setChanged();
            }

            source.shrink(moved);
            reference.container().setItem(
                    reference.slot(),
                    source.isEmpty() ? ItemStack.EMPTY : source
            );
            reference.container().setChanged();
            remaining -= moved;
        }

        return remaining;
    }

    /**
     * Returns every container holding the selected stack, together with its
     * exact amount. The container with the largest amount is first; equal
     * amounts are resolved by distance to the requesting player.
     */
    public List<StorageHighlightTarget> highlightTargets(ItemStack wanted, Vec3 observerPosition) {
        if (wanted.isEmpty()) {
            return List.of();
        }

        Map<BlockPos, Integer> amounts = new HashMap<>();
        for (SlotReference reference : currentSlots()) {
            ItemStack stack = reference.currentStack();
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted)) {
                continue;
            }

            amounts.merge(reference.pos(), stack.getCount(), NearbyStorage::saturatedAdd);
        }

        return amounts.entrySet().stream()
                .map(entry -> new StorageHighlightTarget(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparingInt(StorageHighlightTarget::count)
                        .reversed()
                        .thenComparingDouble(target -> distanceSquared(target.pos(), observerPosition))
                        .thenComparingLong(target -> target.pos().asLong()))
                .toList();
    }

    private static double distanceSquared(BlockPos pos, Vec3 observerPosition) {
        double x = pos.getX() + 0.5D - observerPosition.x;
        double y = pos.getY() + 0.5D - observerPosition.y;
        double z = pos.getZ() + 0.5D - observerPosition.z;
        return x * x + y * y + z * z;
    }

    /**
     * Resolves cached positions against the live world. Removed or unloaded
     * containers disappear immediately, while newly filled slots are visible
     * without waiting for the next radius scan.
     */
    private List<SlotReference> currentSlots() {
        if (level == null || containers.isEmpty()) {
            return List.of();
        }

        List<SlotReference> result = new ArrayList<>();
        for (ContainerReference reference : containers) {
            Container container = reference.currentContainer(level);
            if (container == null) {
                continue;
            }

            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!container.getItem(slot).isEmpty()) {
                    result.add(new SlotReference(
                            reference.pos(),
                            container,
                            slot,
                            reference.distanceSquared()
                    ));
                }
            }
        }
        return result;
    }

    private static boolean isStartedStack(ItemStack stack) {
        return stack.getCount() < stack.getMaxStackSize();
    }

    private static int saturatedAdd(int first, int second) {
        long result = (long) first + second;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }

    private record ContainerReference(BlockPos pos, int distanceSquared) {
        Container currentContainer(Level level) {
            if (!level.hasChunkAt(pos)) {
                return null;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof Container container ? container : null;
        }
    }

    private record SlotReference(BlockPos pos, Container container, int slot, int distanceSquared) {
        ItemStack currentStack() {
            return container.getItem(slot);
        }
    }

    private static final class MutableEntry {
        private final ItemStack stack;
        private int count;

        private MutableEntry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }
}
