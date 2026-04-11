package com.eddy1.tidesourcer;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.init.EntityInit;
import com.eddy1.tidesourcer.init.ItemInit;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(TideSourcerMod.MOD_ID)
public class TideSourcerMod {
    public static final String MOD_ID = "abyssal_corrupter";

    public TideSourcerMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, AbyssalConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, AbyssalConfig.CLIENT_SPEC);

        EntityInit.ENTITY_TYPES.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ItemInit.ABYSSAL_CORRUPTER_SPAWN_EGG.get());
        }
    }
}
