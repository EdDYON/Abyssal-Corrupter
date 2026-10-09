package com.eddy1.tidesourcer.init;

import com.eddy1.tidesourcer.TideSourcerMod;
import com.eddy1.tidesourcer.block.AbyssalSummoningAltarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockInit {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TideSourcerMod.MOD_ID);

    public static final DeferredBlock<AbyssalSummoningAltarBlock> ABYSSAL_SUMMONING_ALTAR = BLOCKS.register(
            "abyssal_summoning_altar",
            () -> new AbyssalSummoningAltarBlock(BlockBehaviour.Properties.of()
                    .strength(8.0F, 1200.0F)
                    .sound(SoundType.SCULK)
                    .lightLevel(state -> state.getValue(AbyssalSummoningAltarBlock.ACTIVE) ? 8 : 2)
                    .noOcclusion())
    );

    private BlockInit() {
    }
}
