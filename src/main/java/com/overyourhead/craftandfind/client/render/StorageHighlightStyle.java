package com.overyourhead.craftandfind.client.render;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Visual and sound tuning for storage-location feedback.
 *
 * Texture files live under:
 * assets/craftandfind/textures/effect/storage_highlight/
 *
 * The constants are intentionally gathered here so the effect can be changed
 * without touching packet or storage logic.
 */
public final class StorageHighlightStyle {
    public static final long DURATION_MILLIS = 5_000L;
    public static final int MAX_RENDERED_CONTAINERS = 64;
    public static final int MAX_SOUND_CONTAINERS = 3;

    public static final int MAIN_PARTICLE_COUNT = 11;
    public static final int SECONDARY_PARTICLE_COUNT = 5;
    public static final float MAIN_PARTICLE_ALPHA = 1.0F;
    public static final float SECONDARY_PARTICLE_ALPHA = 0.62F;
    public static final float PARTICLE_MIN_SIZE = 0.055F;
    public static final float PARTICLE_MAX_SIZE = 0.145F;
    public static final float PARTICLE_RADIUS_MIN = 0.47F;
    public static final float PARTICLE_RADIUS_MAX = 0.72F;
    public static final float PARTICLE_VERTICAL_DRIFT = 0.24F;

    public static final float MARKER_WIDTH = 0.56F;
    public static final float MARKER_HEIGHT = 0.66F;
    public static final float MARKER_FACE_OFFSET = 0.57F;
    public static final float MARKER_CENTER_Y = 0.55F;
    public static final float MARKER_BOB_DISTANCE = 0.025F;
    public static final float MARKER_ITEM_SCALE = 0.35F;
    public static final float MARKER_ITEM_Y = 0.055F;
    public static final float MARKER_ITEM_Z = -0.025F;

    public static final float CLICK_VOLUME = 0.34F;
    public static final float CLICK_PITCH = 1.15F;
    public static final float PRIMARY_CHIME_VOLUME = 0.50F;
    public static final float SECONDARY_CHIME_VOLUME = 0.28F;
    public static final float CHIME_BASE_PITCH = 1.18F;
    public static final float CHIME_PITCH_STEP = 0.10F;

    public static final ResourceLocation MARKER_TEXTURE = texture("marker");
    public static final List<ResourceLocation> PARTICLE_TEXTURES = List.of(
            texture("particle_0"),
            texture("particle_1"),
            texture("particle_2"),
            texture("particle_3"),
            texture("particle_4"),
            texture("particle_5"),
            texture("particle_6")
    );

    private StorageHighlightStyle() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CraftAndFindMod.MOD_ID,
                "textures/effect/storage_highlight/" + name + ".png"
        );
    }
}
