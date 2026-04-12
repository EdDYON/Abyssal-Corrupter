package com.eddy1.tidesourcer.init;

import com.eddy1.tidesourcer.TideSourcerMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ItemInit {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TideSourcerMod.MOD_ID);

    public static final Supplier<Item> ABYSSAL_CORRUPTER_SPAWN_EGG = ITEMS.register("abyssal_corrupter_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    EntityInit.ABYSSAL_CORRUPTER,
                    0x0A0D12,
                    0x67B78F,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
            ));

    public static final DeferredItem<BlockItem> ABYSSAL_SUMMONING_ALTAR = ITEMS.registerSimpleBlockItem(
            "abyssal_summoning_altar",
            BlockInit.ABYSSAL_SUMMONING_ALTAR,
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
    );

}
