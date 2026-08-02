package com.overyourhead.craftandfind.common.storage;

import net.minecraft.core.BlockPos;

/** A live storage location and the amount of the selected stack inside it. */
public record StorageHighlightTarget(BlockPos pos, int count) {
    public StorageHighlightTarget {
        pos = pos.immutable();
        count = Math.max(0, count);
    }
}
