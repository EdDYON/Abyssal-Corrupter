package com.eddy1.tidesourcer.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class AbyssalEffects {
    private AbyssalEffects() {
    }

    public static void spawnCharge(ServerLevel sl, Vec3 pos, double horizontalSpread, double verticalSpread) {
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 3, horizontalSpread, verticalSpread, horizontalSpread, 0.01);
        sl.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
        sl.sendParticles(ParticleTypes.ASH, pos.x, pos.y, pos.z, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.01);
        sl.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
    }

    public static void spawnInfectionCloud(ServerLevel sl, Vec3 pos, double horizontalSpread, double verticalSpread) {
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 4, horizontalSpread, verticalSpread, horizontalSpread, 0.06);
        sl.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z, 3, horizontalSpread, verticalSpread, horizontalSpread, 0.0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
        sl.sendParticles(ParticleTypes.ASH, pos.x, pos.y, pos.z, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.01);
    }

    public static void spawnFearBurst(ServerLevel sl, Vec3 pos, double horizontalSpread, double verticalSpread) {
        sl.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 4, horizontalSpread, verticalSpread, horizontalSpread, 0.03);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.01);
        sl.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 3, horizontalSpread, verticalSpread, horizontalSpread, 0.04);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y, pos.z, 1, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
    }

    public static void spawnImpact(ServerLevel sl, Vec3 pos, double horizontalSpread, double verticalSpread) {
        sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, horizontalSpread * 0.25, verticalSpread * 0.25, horizontalSpread * 0.25, 0.0);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 4, horizontalSpread, verticalSpread, horizontalSpread, 0.03);
        sl.sendParticles(ParticleTypes.ASH, pos.x, pos.y, pos.z, 5, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y, pos.z, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
    }

    public static void spawnRiftPillar(ServerLevel sl, double x, double y, double z) {
        for (int h = 0; h < 10; h += 2) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + h, z, 1, 0.3, 0.6, 0.3, 0.01);
            sl.sendParticles(ParticleTypes.SMOKE, x, y + h, z, 1, 0.35, 0.6, 0.35, 0.02);
            sl.sendParticles(ParticleTypes.ASH, x, y + h, z, 1, 0.4, 0.7, 0.4, 0.01);
        }
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 5.0, z, 1, 0.25, 0.6, 0.25, 0.03);
    }

    public static void spawnBeam(ServerLevel sl, Vec3 start, Vec3 direction, int steps, double spacing) {
        Vec3 dir = direction.normalize();
        for (int i = 1; i <= steps; i++) {
            Vec3 pos = start.add(dir.scale(i * spacing));
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.01);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 1, 0.15, 0.15, 0.15, 0.02);
            sl.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.12, 0.12, 0.12, 0.01);
            if (i % 6 == 0) {
                sl.sendParticles(ParticleTypes.ASH, pos.x, pos.y, pos.z, 1, 0.15, 0.15, 0.15, 0.01);
            }
        }
    }

    public static void play(ServerLevel sl, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(pos), sound, SoundSource.HOSTILE, volume, pitch);
    }
}
