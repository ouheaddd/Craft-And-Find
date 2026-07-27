package com.overyourhead.craftandfind.common.network;

import com.overyourhead.craftandfind.client.network.ClientPayloadRegistration;
import com.overyourhead.craftandfind.common.menu.StorageWorkbenchMenu;
import com.overyourhead.craftandfind.common.network.payload.HighlightPositionsPayload;
import com.overyourhead.craftandfind.common.network.payload.HighlightRequestPayload;
import com.overyourhead.craftandfind.common.network.payload.StorageSnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CraftAndFindNetwork {
    private CraftAndFindNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                HighlightRequestPayload.TYPE,
                HighlightRequestPayload.STREAM_CODEC,
                CraftAndFindNetwork::handleHighlightRequest
        );

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPayloadRegistration.register(registrar);
        } else {
            registrar.playToClient(
                    StorageSnapshotPayload.TYPE,
                    StorageSnapshotPayload.STREAM_CODEC,
                    CraftAndFindNetwork::ignoreStorageSnapshot
            );
            registrar.playToClient(
                    HighlightPositionsPayload.TYPE,
                    HighlightPositionsPayload.STREAM_CODEC,
                    CraftAndFindNetwork::ignoreHighlightPositions
            );
        }
    }

    private static void handleHighlightRequest(HighlightRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.containerMenu instanceof StorageWorkbenchMenu menu)) {
            return;
        }
        if (menu.containerId != payload.containerId() || payload.stack().isEmpty()) {
            return;
        }

        var positions = menu.refreshStorage().positionsContaining(payload.stack());
        PacketDistributor.sendToPlayer(player, new HighlightPositionsPayload(positions));
    }

    private static void ignoreStorageSnapshot(StorageSnapshotPayload payload, IPayloadContext context) {
        // Dedicated servers register the clientbound payload type without running client code.
    }

    private static void ignoreHighlightPositions(HighlightPositionsPayload payload, IPayloadContext context) {
        // Dedicated servers register the clientbound payload type without running client code.
    }
}
