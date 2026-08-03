package com.overyourhead.craftandfind.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only presentation settings for storage highlight feedback. */
public final class CraftAndFindClientConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.BooleanValue SHOW_ITEM_MARKER;
    private static final ModConfigSpec.BooleanValue ENABLE_PARTICLES;
    private static final ModConfigSpec.DoubleValue PARTICLE_AMOUNT;
    private static final ModConfigSpec.DoubleValue DURATION_SECONDS;
    private static final ModConfigSpec.DoubleValue EXTRA_PARTICLE_DURATION_SECONDS;
    private static final ModConfigSpec.BooleanValue ENABLE_SOUNDS;
    private static final ModConfigSpec.DoubleValue SOUND_VOLUME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Storage highlight appearance and sound settings.",
                "These options only affect this client. They do not change storage searching,",
                "crafting, item counts, or the settings of other players."
        ).push("highlight");

        ENABLED = builder.comment(
                "Enables the complete storage-location feedback effect.",
                "true  = show enabled marker/particles and play enabled sounds",
                "false = disable the location effect without disabling storage search or crafting"
        ).define("enabled", true);

        SHOW_ITEM_MARKER = builder.comment(
                "Shows the framed item icon above the primary matching container.",
                "Disable this to keep only particles and sounds."
        ).define("showItemMarker", true);

        ENABLE_PARTICLES = builder.comment(
                "Shows particles around containers that contain the selected item.",
                "Disable this on weak computers or when particles interfere with shaders."
        ).define("enableParticles", true);

        PARTICLE_AMOUNT = builder.comment(
                "Multiplier for the number of storage-highlight particles.",
                "0.0 = no particles, 0.5 = half, 1.0 = default, 1.5 = 50% more, 2.0 = maximum.",
                "Recommended: weak computer 0.5, normal computer 1.0, strong computer 1.25-1.5."
        ).defineInRange("particleAmount", 1.0D, 0.0D, 2.0D);

        DURATION_SECONDS = builder.comment(
                "How long the framed item marker remains visible, in seconds.",
                "Recommended: small storage room 3-4, normal usage 5, large warehouse 7-10."
        ).defineInRange("durationSeconds", 5.0D, 1.0D, 15.0D);

        EXTRA_PARTICLE_DURATION_SECONDS = builder.comment(
                "How long particles remain after the item marker disappears, in seconds.",
                "0.0 makes particles disappear with the marker; 1.5 is the default smooth ending."
        ).defineInRange("extraParticleDurationSeconds", 1.5D, 0.0D, 5.0D);

        ENABLE_SOUNDS = builder.comment(
                "Enables the click and directional chime sounds for storage highlighting."
        ).define("enableSounds", true);

        SOUND_VOLUME = builder.comment(
                "Volume multiplier for Craft & Find highlight sounds.",
                "0.0 = silent, 0.5 = half volume, 1.0 = default full volume."
        ).defineInRange("soundVolume", 1.0D, 0.0D, 1.0D);

        builder.pop();
        SPEC = builder.build();
    }

    private CraftAndFindClientConfig() {
    }

    public static boolean enabled() {
        return ENABLED.get();
    }

    public static boolean showItemMarker() {
        return SHOW_ITEM_MARKER.get();
    }

    public static boolean enableParticles() {
        return ENABLE_PARTICLES.get();
    }

    public static double particleAmount() {
        return PARTICLE_AMOUNT.get();
    }

    public static long markerDurationMillis() {
        return Math.round(DURATION_SECONDS.get() * 1_000.0D);
    }

    public static long extraParticleDurationMillis() {
        return Math.round(EXTRA_PARTICLE_DURATION_SECONDS.get() * 1_000.0D);
    }

    public static boolean enableSounds() {
        return ENABLE_SOUNDS.get();
    }

    public static float soundVolume() {
        return SOUND_VOLUME.get().floatValue();
    }
}
