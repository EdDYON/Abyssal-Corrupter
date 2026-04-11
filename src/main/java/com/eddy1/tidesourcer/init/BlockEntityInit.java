package com.eddy1.tidesourcer.init;

import com.eddy1.tidesourcer.TideSourcerMod;
import com.eddy1.tidesourcer.block.entity.AbyssalSummoningAltarBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TideSourcerMod.MOD_ID);

    public static final Supplier<BlockEntityType<AbyssalSummoningAltarBlockEntity>> ABYSSAL_SUMMONING_ALTAR =
            BLOCK_ENTITY_TYPES.register("abyssal_summoning_altar",
                    () -> BlockEntityType.Builder.of(
                            AbyssalSummoningAltarBlockEntity::new,
                            BlockInit.ABYSSAL_SUMMONING_ALTAR.get()
                    ).build(null));

    private BlockEntityInit() {
    }
}
