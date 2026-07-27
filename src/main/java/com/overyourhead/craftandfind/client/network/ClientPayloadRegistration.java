package com.overyourhead.craftandfind.client.network;

import com.overyourhead.craftandfind.common.network.payload.GhostRecipePayload;
import com.overyourhead.craftandfind.common.network.payload.HighlightPositionsPayload;
import com.overyourhead.craftandfind.common.network.payload.StorageSnapshotPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ClientPayloadRegistration {
    private ClientPayloadRegistration() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(
                StorageSnapshotPayload.TYPE,
                StorageSnapshotPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStorageSnapshot
        );
        registrar.playToClient(
                HighlightPositionsPayload.TYPE,
                HighlightPositionsPayload.STREAM_CODEC,
                ClientPayloadHandler::handleHighlightPositions
        );
        registrar.playToClient(
                GhostRecipePayload.TYPE,
                GhostRecipePayload.STREAM_CODEC,
                ClientPayloadHandler::handleGhostRecipe
        );
    }
}
