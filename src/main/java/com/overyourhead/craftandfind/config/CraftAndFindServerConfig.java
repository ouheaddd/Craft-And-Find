package com.overyourhead.craftandfind.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** World/server settings for storage discovery and update frequency. */
public final class CraftAndFindServerConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue SEARCH_RADIUS;
    private static final ModConfigSpec.IntValue MAX_HIGHLIGHTED_CONTAINERS;
    private static final ModConfigSpec.IntValue CONTAINER_SCAN_INTERVAL_TICKS;
    private static final ModConfigSpec.IntValue CONTENT_REFRESH_INTERVAL_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Gameplay settings for nearby storage discovery.",
                "On multiplayer servers, the server values are used and synchronized to clients."
        ).push("storage");

        SEARCH_RADIUS = builder.comment(
                "Maximum distance from the Storage Workbench in which containers are discovered.",
                "The scan uses a three-dimensional radius, so larger values can increase server work.",
                "Recommended: small storage room 8-12, normal storage room 16, large warehouse 24.",
                "Use 32 only for very large storage builds."
        ).defineInRange("searchRadius", 16, 4, 32);

        MAX_HIGHLIGHTED_CONTAINERS = builder.comment(
                "Maximum number of matching containers that receive highlight effects after a click.",
                "This does not limit storage searching, crafting, or the displayed total item count.",
                "Recommended: weak clients/large servers 32, default 64, showcase builds up to 128."
        ).defineInRange("maxHighlightedContainers", 64, 1, 128);

        builder.pop();

        builder.comment(
                "Advanced performance settings.",
                "Lower intervals react faster but make the server perform the related work more often.",
                "20 game ticks are approximately one second. Most users should keep the defaults."
        ).push("advanced");

        CONTAINER_SCAN_INTERVAL_TICKS = builder.comment(
                "How often the workbench performs a full radius scan for placed or removed containers.",
                "Recommended: fast updates 40-60, default 100, large servers 200-400."
        ).defineInRange("containerScanIntervalTicks", 100, 20, 600);

        CONTENT_REFRESH_INTERVAL_TICKS = builder.comment(
                "How often item contents of already discovered containers are refreshed and checked",
                "for a changed storage snapshot.",
                "Recommended: very responsive 10, default 20, large servers 40-60."
        ).defineInRange("contentRefreshIntervalTicks", 20, 5, 100);

        builder.pop();
        SPEC = builder.build();
    }

    private CraftAndFindServerConfig() {
    }

    public static int searchRadius() {
        return SEARCH_RADIUS.get();
    }

    public static int maxHighlightedContainers() {
        return MAX_HIGHLIGHTED_CONTAINERS.get();
    }

    public static int containerScanIntervalTicks() {
        return CONTAINER_SCAN_INTERVAL_TICKS.get();
    }

    public static int contentRefreshIntervalTicks() {
        return CONTENT_REFRESH_INTERVAL_TICKS.get();
    }
}
