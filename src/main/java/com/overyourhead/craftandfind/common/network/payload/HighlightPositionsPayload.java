package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record HighlightPositionsPayload(List<BlockPos> positions) implements CustomPacketPayload {
    private static final int MAX_POSITIONS = 512;

    public static final Type<HighlightPositionsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "highlight_positions")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HighlightPositionsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                int size = Math.min(payload.positions().size(), MAX_POSITIONS);
                buffer.writeVarInt(size);
                for (int index = 0; index < size; index++) {
                    buffer.writeBlockPos(payload.positions().get(index));
                }
            },
            buffer -> {
                int size = readBoundedSize(buffer, MAX_POSITIONS, "highlight positions");
                List<BlockPos> positions = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    positions.add(buffer.readBlockPos());
                }
                return new HighlightPositionsPayload(positions);
            }
    );

    public HighlightPositionsPayload {
        positions = positions.stream().map(BlockPos::immutable).distinct().limit(MAX_POSITIONS).toList();
    }

    private static int readBoundedSize(RegistryFriendlyByteBuf buffer, int maximum, String valueName) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) {
            throw new IllegalArgumentException("Invalid " + valueName + " count: " + size);
        }
        return size;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
