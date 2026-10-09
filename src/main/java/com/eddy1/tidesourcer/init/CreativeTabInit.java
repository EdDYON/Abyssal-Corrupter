package com.eddy1.tidesourcer.init;

import com.eddy1.tidesourcer.TideSourcerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeTabInit {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TideSourcerMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ABYSSAL_CORRUPTER_TAB =
            CREATIVE_MODE_TABS.register("abyssal_corrupter", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.abyssal_corrupter.abyssal_corrupter"))
                    .icon(() -> new ItemStack(ItemInit.ABYSSAL_SUMMONING_ALTAR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemInit.ABYSSAL_SUMMONING_ALTAR.get());
                        output.accept(ItemInit.ABYSSAL_CORRUPTER_SPAWN_EGG.get());
                    })
                    .build());

    private CreativeTabInit() {
    }
}
