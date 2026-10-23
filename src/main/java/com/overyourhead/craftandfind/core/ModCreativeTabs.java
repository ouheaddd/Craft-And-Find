package com.overyourhead.craftandfind.core;

import com.overyourhead.craftandfind.CraftAndFindMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftAndFindMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CRAFT_AND_FIND = TABS.register(
            "craftandfind",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.craftandfind"))
                    .icon(() -> ModItems.STORAGE_WORKBENCH_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(ModItems.STORAGE_WORKBENCH_ITEM.get()))
                    .build()
    );

    private ModCreativeTabs() {
    }
}
