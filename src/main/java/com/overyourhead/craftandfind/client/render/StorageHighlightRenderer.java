package com.overyourhead.craftandfind.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

@EventBusSubscriber(modid = CraftAndFindMod.MOD_ID, value = Dist.CLIENT)
public final class StorageHighlightRenderer {
    private static final long DURATION_MILLIS = 5_000L;
    private static List<BlockPos> positions = List.of();
    private static long visibleUntil;

    private StorageHighlightRenderer() {
    }

    public static void show(List<BlockPos> newPositions) {
        positions = List.copyOf(newPositions);
        visibleUntil = Util.getMillis() + DURATION_MILLIS;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (positions.isEmpty() || Util.getMillis() > visibleUntil) {
            positions = List.of();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        for (BlockPos pos : positions) {
            if (!minecraft.level.hasChunkAt(pos)) {
                continue;
            }
            AABB box = new AABB(pos).inflate(0.003D);
            LevelRenderer.renderLineBox(poseStack, lines, box, 1.0F, 0.85F, 0.1F, 1.0F);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
