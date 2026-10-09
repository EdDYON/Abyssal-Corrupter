package com.eddy1.tidesourcer;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.init.BlockEntityInit;
import com.eddy1.tidesourcer.init.BlockInit;
import com.eddy1.tidesourcer.init.CreativeTabInit;
import com.eddy1.tidesourcer.init.EntityInit;
import com.eddy1.tidesourcer.init.ItemInit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(TideSourcerMod.MOD_ID)
public class TideSourcerMod {
    public static final String MOD_ID = "abyssal_corrupter";

    public TideSourcerMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, AbyssalConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, AbyssalConfig.CLIENT_SPEC);

        BlockInit.BLOCKS.register(modEventBus);
        BlockEntityInit.BLOCK_ENTITY_TYPES.register(modEventBus);
        EntityInit.ENTITY_TYPES.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        CreativeTabInit.CREATIVE_MODE_TABS.register(modEventBus);
    }
}
