package com.overyourhead.craftandfind.common.network.payload;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlaceRecipePayload(
        int containerId,
        ResourceLocation recipeId,
        boolean placeAll
) implements CustomPacketPayload {
    public static final Type<PlaceRecipePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CraftAndFindMod.MOD_ID, "place_recipe")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceRecipePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.containerId());
                ResourceLocation.STREAM_CODEC.encode(buffer, payload.recipeId());
                buffer.writeBoolean(payload.placeAll());
            },
            buffer -> new PlaceRecipePayload(
                    buffer.readVarInt(),
                    ResourceLocation.STREAM_CODEC.decode(buffer),
                    buffer.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
