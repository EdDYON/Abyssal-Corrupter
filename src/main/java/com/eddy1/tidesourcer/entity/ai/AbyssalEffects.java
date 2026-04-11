package com.eddy1.tidesourcer.entity.ai;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class AbyssalEffects {
    private AbyssalEffects() {
    }

    public static void spawnCharge(ServerLevel sl, Vec3 pos, double horizontalSpread, double verticalSpread) {
        send(sl, ParticleTypes.SOUL_FIRE_FLAME, pos, 3, horizontalSpread, verticalSpread, horizontalSpread, 0.01);
        send(sl, ParticleTypes.SOUL, pos, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
        send(sl, ParticleTypes.ASH, pos, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.01);
        send(sl, ParticleTypes.SMOKE, pos, 1, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
    }

    public static void spawnInfectionCloud(ServerLevel sl, Vec3 pos, double horizontalSpread, double verticalSpread) {
        send(sl, ParticleTypes.REVERSE_PORTAL, pos, 4, horizontalSpread, verticalSpread, horizontalSpread, 0.06);
        send(sl, ParticleTypes.SQUID_INK, pos, 3, horizontalSpread, verticalSpread, horizontalSpread, 0.0);
        send(sl, ParticleTypes.LARGE_SMOKE, pos, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
        send(sl, ParticleTypes.ASH, pos, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.01);
    }

    public static void spawnFearBurst(ServerLevel sl, Vec3 pos, double horizontalSpread, double verticalSpread) {
        send(sl, ParticleTypes.SOUL, pos, 4, horizontalSpread, verticalSpread, horizontalSpread, 0.03);
        send(sl, ParticleTypes.SOUL_FIRE_FLAME, pos, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.01);
        send(sl, ParticleTypes.SMOKE, pos, 3, horizontalSpread, verticalSpread, horizontalSpread, 0.04);
        send(sl, ParticleTypes.DAMAGE_INDICATOR, pos, 1, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
    }

    public static void spawnImpact(ServerLevel sl, Vec3 pos, double horizontalSpread, double verticalSpread) {
        send(sl, ParticleTypes.EXPLOSION, pos, 1, horizontalSpread * 0.25, verticalSpread * 0.25, horizontalSpread * 0.25, 0.0);
        send(sl, ParticleTypes.LARGE_SMOKE, pos, 4, horizontalSpread, verticalSpread, horizontalSpread, 0.03);
        send(sl, ParticleTypes.ASH, pos, 5, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
        send(sl, ParticleTypes.DAMAGE_INDICATOR, pos, 2, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
    }

    public static void spawnRiftPillar(ServerLevel sl, double x, double y, double z) {
        for (int h = 0; h < 10; h += 2) {
            send(sl, ParticleTypes.SOUL_FIRE_FLAME, x, y + h, z, 1, 0.3, 0.6, 0.3, 0.01);
            send(sl, ParticleTypes.SMOKE, x, y + h, z, 1, 0.35, 0.6, 0.35, 0.02);
            send(sl, ParticleTypes.ASH, x, y + h, z, 1, 0.4, 0.7, 0.4, 0.01);
        }
        send(sl, ParticleTypes.REVERSE_PORTAL, x, y + 5.0, z, 1, 0.25, 0.6, 0.25, 0.03);
    }

    public static void spawnBeam(ServerLevel sl, Vec3 start, Vec3 direction, int steps, double spacing) {
        Vec3 dir = direction.normalize();
        for (int i = 1; i <= steps; i++) {
            Vec3 pos = start.add(dir.scale(i * spacing));
            send(sl, ParticleTypes.SOUL_FIRE_FLAME, pos, 1, 0.1, 0.1, 0.1, 0.01);
            send(sl, ParticleTypes.REVERSE_PORTAL, pos, 1, 0.15, 0.15, 0.15, 0.02);
            send(sl, ParticleTypes.SMOKE, pos, 1, 0.12, 0.12, 0.12, 0.01);
            if (i % 6 == 0) {
                send(sl, ParticleTypes.ASH, pos, 1, 0.15, 0.15, 0.15, 0.01);
            }
        }
    }

    public static void send(ServerLevel sl, ParticleOptions particle, Vec3 pos, int count, double xSpread, double ySpread, double zSpread, double speed) {
        send(sl, particle, pos.x, pos.y, pos.z, count, xSpread, ySpread, zSpread, speed);
    }

    public static void send(ServerLevel sl, ParticleOptions particle, double x, double y, double z, int count, double xSpread, double ySpread, double zSpread, double speed) {
        int scaledCount = AbyssalConfig.scaledParticleCount(count);
        if (scaledCount <= 0) {
            return;
        }
        sl.sendParticles(particle, x, y, z, scaledCount, xSpread, ySpread, zSpread, speed);
    }

    public static void play(ServerLevel sl, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(pos), sound, SoundSource.HOSTILE, volume, pitch);
    }
}
