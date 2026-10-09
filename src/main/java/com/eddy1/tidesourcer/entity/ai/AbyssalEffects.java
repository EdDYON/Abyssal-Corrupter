package com.eddy1.tidesourcer.entity.ai;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class AbyssalEffects {
    private static final DustParticleOptions DANGER_RED = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.10F), 1.0F);
    private static final DustParticleOptions CONTROL_BLUE = new DustParticleOptions(new Vector3f(0.10F, 0.48F, 0.82F), 0.9F);
    private static final DustParticleOptions ABYSS_BLACK_RED = new DustParticleOptions(new Vector3f(0.28F, 0.02F, 0.06F), 1.0F);
    private static final DustParticleOptions CHARGE_CYAN = new DustParticleOptions(new Vector3f(0.30F, 0.92F, 0.86F), 1.05F);

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

    public static void spawnMeleeTelegraph(ServerLevel sl, Vec3 origin, Vec3 direction, double reach, double arcHalfWidth) {
        Vec3 forward = horizontal(direction);
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        int points = 10;
        for (int i = -points; i <= points; i++) {
            double ratio = i / (double) points;
            Vec3 point = origin
                    .add(forward.scale(reach))
                    .add(side.scale(ratio * arcHalfWidth))
                    .add(0.0D, 0.12D, 0.0D);
            send(sl, DANGER_RED, point, 1, 0.02D, 0.01D, 0.02D, 0.0D);
            if (i % 4 == 0) {
                send(sl, ABYSS_BLACK_RED, point.add(0.0D, 0.04D, 0.0D), 1, 0.02D, 0.01D, 0.02D, 0.0D);
            }
        }
    }

    public static void spawnGroundCrackTelegraph(ServerLevel sl, Vec3 center, double radius, int age, int totalTicks) {
        double progress = totalTicks <= 0 ? 1.0D : Math.min(1.0D, age / (double) totalTicks);
        double ringRadius = Math.max(0.35D, radius * (1.0D - progress * 0.55D));
        int points = Math.max(12, (int) Math.ceil(radius * 8.0D));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double x = center.x + Math.cos(angle) * ringRadius;
            double z = center.z + Math.sin(angle) * ringRadius;
            send(sl, DANGER_RED, x, center.y + 0.05D, z, 1, 0.025D, 0.0D, 0.025D, 0.0D);
            if (i % 3 == 0) {
                send(sl, ParticleTypes.SMOKE, x, center.y + 0.04D, z, 1, 0.025D, 0.0D, 0.025D, 0.0D);
            }
        }
        send(sl, ABYSS_BLACK_RED, center.add(0.0D, 0.08D, 0.0D), 3, radius * 0.18D, 0.02D, radius * 0.18D, 0.0D);
    }

    public static void spawnControlMist(ServerLevel sl, Vec3 center, double radius, double height) {
        send(sl, CONTROL_BLUE, center, 4, radius, height, radius, 0.0D);
        send(sl, ParticleTypes.SCULK_SOUL, center, 3, radius * 0.7D, height * 0.7D, radius * 0.7D, 0.01D);
        send(sl, ParticleTypes.SQUID_INK, center, 2, radius * 0.85D, height * 0.65D, radius * 0.85D, 0.0D);
    }

    public static void spawnRangedCharge(ServerLevel sl, Vec3 start, Vec3 direction, double length) {
        Vec3 forward = direction.normalize();
        int samples = Math.max(4, (int) Math.ceil(length / 1.6D));
        send(sl, CHARGE_CYAN, start, 4, 0.12D, 0.12D, 0.12D, 0.0D);
        for (int i = 1; i <= samples; i++) {
            Vec3 point = start.add(forward.scale(i * (length / samples)));
            send(sl, CHARGE_CYAN, point, 1, 0.035D, 0.035D, 0.035D, 0.0D);
            if (i % 3 == 0) {
                send(sl, DANGER_RED, point, 1, 0.025D, 0.025D, 0.025D, 0.0D);
            }
        }
    }

    public static void spawnAftershock(ServerLevel sl, Vec3 center, double radius) {
        int points = Math.max(10, (int) Math.ceil(radius * 6.0D));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            send(sl, ParticleTypes.SONIC_BOOM, x, center.y + 0.04D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (i % 2 == 0) {
                send(sl, ParticleTypes.ASH, x, center.y + 0.02D, z, 1, 0.03D, 0.0D, 0.03D, 0.0D);
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

    private static Vec3 horizontal(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return horizontal.normalize();
    }
}
