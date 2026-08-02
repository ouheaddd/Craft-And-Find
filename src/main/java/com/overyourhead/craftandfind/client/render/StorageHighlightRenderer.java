package com.overyourhead.craftandfind.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.overyourhead.craftandfind.CraftAndFindMod;
import com.overyourhead.craftandfind.common.storage.StorageHighlightTarget;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Client-only storage locator effect.
 *
 * Every matching container gets editable sparkle textures. The container with
 * the largest amount gets the editable marker texture and the selected item.
 */
@EventBusSubscriber(modid = CraftAndFindMod.MOD_ID, value = Dist.CLIENT)
public final class StorageHighlightRenderer {
    private static List<StorageHighlightTarget> targets = List.of();
    private static ItemStack highlightedStack = ItemStack.EMPTY;
    private static long startedAt;
    private static long visibleUntil;

    private StorageHighlightRenderer() {
    }

    public static void show(ItemStack stack, List<StorageHighlightTarget> newTargets) {
        highlightedStack = stack.copyWithCount(1);
        targets = List.copyOf(newTargets);
        startedAt = Util.getMillis();
        visibleUntil = startedAt + StorageHighlightStyle.DURATION_MILLIS;
        playFeedbackSounds();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        long now = Util.getMillis();
        if (targets.isEmpty() || highlightedStack.isEmpty() || now > visibleUntil) {
            clear();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        if (minecraft.level == null || poseStack == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float elapsedSeconds = (now - startedAt) / 1_000.0F;
        float globalAlpha = visibilityAlpha(now);

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        int renderedTargets = Math.min(targets.size(), StorageHighlightStyle.MAX_RENDERED_CONTAINERS);
        for (int index = 0; index < renderedTargets; index++) {
            StorageHighlightTarget target = targets.get(index);
            if (!minecraft.level.hasChunkAt(target.pos())) {
                continue;
            }

            renderParticles(
                    poseStack,
                    buffers,
                    camera,
                    target.pos(),
                    index == 0,
                    elapsedSeconds,
                    globalAlpha
            );
        }

        StorageHighlightTarget primary = targets.getFirst();
        if (minecraft.level.hasChunkAt(primary.pos())) {
            renderMarker(
                    minecraft,
                    poseStack,
                    buffers,
                    camera,
                    primary.pos(),
                    elapsedSeconds,
                    globalAlpha
            );
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void renderParticles(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Camera camera,
            BlockPos pos,
            boolean primary,
            float elapsedSeconds,
            float globalAlpha
    ) {
        int particleCount = primary
                ? StorageHighlightStyle.MAIN_PARTICLE_COUNT
                : StorageHighlightStyle.SECONDARY_PARTICLE_COUNT;
        float roleAlpha = primary
                ? StorageHighlightStyle.MAIN_PARTICLE_ALPHA
                : StorageHighlightStyle.SECONDARY_PARTICLE_ALPHA;

        for (int index = 0; index < particleCount; index++) {
            long seed = mix(pos.asLong() + 0x9E3779B97F4A7C15L * (index + 1L));
            int variant = Math.floorMod((int) seed, StorageHighlightStyle.PARTICLE_TEXTURES.size());
            ResourceLocation texture = StorageHighlightStyle.PARTICLE_TEXTURES.get(variant);

            double lifetime = 1.10D + unit(seed + 11L) * 0.95D;
            double phaseOffset = unit(seed + 23L) * lifetime;
            double phase = ((elapsedSeconds + phaseOffset) % lifetime) / lifetime;
            float lifeAlpha = (float) Math.sin(Math.PI * phase);
            int alpha = Math.round(255.0F * globalAlpha * roleAlpha * lifeAlpha);
            if (alpha <= 3) {
                continue;
            }

            double angle = unit(seed + 37L) * Math.PI * 2.0D;
            double radius = lerp(
                    StorageHighlightStyle.PARTICLE_RADIUS_MIN,
                    StorageHighlightStyle.PARTICLE_RADIUS_MAX,
                    unit(seed + 41L)
            );
            double wobble = Math.sin(elapsedSeconds * 2.2D + unit(seed + 43L) * Math.PI * 2.0D) * 0.035D;
            double x = pos.getX() + 0.5D + Math.cos(angle) * (radius + wobble);
            double z = pos.getZ() + 0.5D + Math.sin(angle) * (radius + wobble);
            double y = pos.getY()
                    + 0.12D
                    + unit(seed + 47L) * 0.78D
                    + phase * StorageHighlightStyle.PARTICLE_VERTICAL_DRIFT;

            float sizeT = (variant + 0.5F) / StorageHighlightStyle.PARTICLE_TEXTURES.size();
            float size = (float) lerp(
                    StorageHighlightStyle.PARTICLE_MIN_SIZE,
                    StorageHighlightStyle.PARTICLE_MAX_SIZE,
                    sizeT
            );
            size *= 0.82F + (float) unit(seed + 53L) * 0.36F;
            if (!primary) {
                size *= 0.84F;
            }

            renderTexturedBillboard(
                    poseStack,
                    buffers,
                    camera,
                    texture,
                    new Vec3(x, y, z),
                    size,
                    size,
                    alpha
            );
        }
    }

    private static void renderMarker(
            Minecraft minecraft,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Camera camera,
            BlockPos pos,
            float elapsedSeconds,
            float globalAlpha
    ) {
        Vec3 blockCenter = new Vec3(
                pos.getX() + 0.5D,
                pos.getY() + StorageHighlightStyle.MARKER_CENTER_Y,
                pos.getZ() + 0.5D
        );
        Vec3 cameraPosition = camera.getPosition();
        Vec3 horizontalToCamera = new Vec3(
                cameraPosition.x - blockCenter.x,
                0.0D,
                cameraPosition.z - blockCenter.z
        );

        Vec3 markerPosition = blockCenter;
        if (horizontalToCamera.lengthSqr() > 0.0001D) {
            markerPosition = markerPosition.add(
                    horizontalToCamera.normalize().scale(StorageHighlightStyle.MARKER_FACE_OFFSET)
            );
        }
        markerPosition = markerPosition.add(
                0.0D,
                Math.sin(elapsedSeconds * 3.2F) * StorageHighlightStyle.MARKER_BOB_DISTANCE,
                0.0D
        );

        int alpha = Math.round(255.0F * globalAlpha);
        poseStack.pushPose();
        poseStack.translate(markerPosition.x, markerPosition.y, markerPosition.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        VertexConsumer markerConsumer = buffers.getBuffer(
                RenderType.entityTranslucentEmissive(StorageHighlightStyle.MARKER_TEXTURE)
        );
        drawQuad(
                poseStack.last().pose(),
                markerConsumer,
                StorageHighlightStyle.MARKER_WIDTH,
                StorageHighlightStyle.MARKER_HEIGHT,
                alpha
        );

        poseStack.pushPose();
        poseStack.translate(
                0.0F,
                StorageHighlightStyle.MARKER_ITEM_Y,
                StorageHighlightStyle.MARKER_ITEM_Z
        );
        poseStack.scale(
                StorageHighlightStyle.MARKER_ITEM_SCALE,
                StorageHighlightStyle.MARKER_ITEM_SCALE,
                StorageHighlightStyle.MARKER_ITEM_SCALE
        );
        minecraft.getItemRenderer().renderStatic(
                highlightedStack,
                ItemDisplayContext.GUI,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffers,
                minecraft.level,
                0
        );
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderTexturedBillboard(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            Camera camera,
            ResourceLocation texture,
            Vec3 position,
            float width,
            float height,
            int alpha
    ) {
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
        drawQuad(poseStack.last().pose(), consumer, width, height, alpha);
        poseStack.popPose();
    }

    private static void drawQuad(
            Matrix4f matrix,
            VertexConsumer consumer,
            float width,
            float height,
            int alpha
    ) {
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;

        vertex(consumer, matrix, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, alpha);
        vertex(consumer, matrix, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, alpha);
        vertex(consumer, matrix, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, alpha);
        vertex(consumer, matrix, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, alpha);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float u,
            float v,
            int alpha
    ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    private static void playFeedbackSounds() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK.value(),
                StorageHighlightStyle.CLICK_PITCH,
                StorageHighlightStyle.CLICK_VOLUME
        ));

        int soundCount = Math.min(targets.size(), StorageHighlightStyle.MAX_SOUND_CONTAINERS);
        for (int index = 0; index < soundCount; index++) {
            StorageHighlightTarget target = targets.get(index);
            float volume = index == 0
                    ? StorageHighlightStyle.PRIMARY_CHIME_VOLUME
                    : StorageHighlightStyle.SECONDARY_CHIME_VOLUME;
            float pitch = StorageHighlightStyle.CHIME_BASE_PITCH
                    + StorageHighlightStyle.CHIME_PITCH_STEP * index;

            minecraft.level.playLocalSound(
                    target.pos().getX() + 0.5D,
                    target.pos().getY() + 0.5D,
                    target.pos().getZ() + 0.5D,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    volume,
                    pitch,
                    false
            );
        }
    }

    private static float visibilityAlpha(long now) {
        float fadeIn = Math.min(1.0F, (now - startedAt) / 180.0F);
        float fadeOut = Math.min(1.0F, (visibleUntil - now) / 550.0F);
        return Math.max(0.0F, Math.min(fadeIn, fadeOut));
    }

    private static void clear() {
        targets = List.of();
        highlightedStack = ItemStack.EMPTY;
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static double unit(long seed) {
        return (mix(seed) >>> 11) * 0x1.0p-53;
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }
}
