package com.eddy1.tidesourcer.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AbyssalRitualSite {
    private static final int BLOCK_UPDATE_FLAGS = 3;
    private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> PROTECTED_BLOCKS = new HashMap<>();

    private final BlockPos center;
    private final int radius;
    private final Map<BlockPos, BlockState> originalStates = new LinkedHashMap<>();
    private boolean restored;

    public AbyssalRitualSite(BlockPos center, int radius) {
        this.center = center.immutable();
        this.radius = Math.max(1, radius);
    }

    public boolean isEmpty() {
        return this.originalStates.isEmpty();
    }

    public BlockPos center() {
        return this.center;
    }

    public int radius() {
        return this.radius;
    }

    public boolean place(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.restored) {
            return false;
        }

        BlockPos key = pos.immutable();
        if (!this.originalStates.containsKey(key)) {
            this.originalStates.put(key, level.getBlockState(pos));
            protect(level, key);
        }
        level.setBlock(key, state, BLOCK_UPDATE_FLAGS);
        return true;
    }

    public void restore(ServerLevel level) {
        if (this.restored) {
            return;
        }

        List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(this.originalStates.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            Map.Entry<BlockPos, BlockState> entry = entries.get(i);
            level.setBlock(entry.getKey(), entry.getValue(), BLOCK_UPDATE_FLAGS);
            unprotect(level, entry.getKey());
        }

        this.originalStates.clear();
        this.restored = true;
    }

    public static boolean isProtected(LevelAccessor levelAccessor, BlockPos pos) {
        if (!(levelAccessor instanceof Level level)) {
            return false;
        }

        Map<BlockPos, Integer> positions = PROTECTED_BLOCKS.get(level.dimension());
        return positions != null && positions.containsKey(pos);
    }

    private static void protect(ServerLevel level, BlockPos pos) {
        PROTECTED_BLOCKS
                .computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .merge(pos.immutable(), 1, Integer::sum);
    }

    private static void unprotect(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Integer> positions = PROTECTED_BLOCKS.get(level.dimension());
        if (positions == null) {
            return;
        }

        BlockPos key = pos.immutable();
        int remaining = positions.getOrDefault(key, 0) - 1;
        if (remaining <= 0) {
            positions.remove(key);
        } else {
            positions.put(key, remaining);
        }

        if (positions.isEmpty()) {
            PROTECTED_BLOCKS.remove(level.dimension());
        }
    }
}
