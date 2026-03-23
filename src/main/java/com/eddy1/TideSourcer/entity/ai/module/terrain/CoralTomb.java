package com.eddy1.tidesourcer.entity.ai.module.terrain;

import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class CoralTomb {
    private static final int CHARGE_TICKS = 40;
    private static final int NOVA_TICKS = 60;
    private static final int HIT_COOLDOWN_TICKS = 20;
    private static final int WAVE_COUNT = 16;
    private static final double WAVE_RANGE = 34.0D;
    private static final double WAVE_SPACING = 1.9D;
    private static final double WAVE_WIDTH = 0.9D;
    private static final float WAVE_DAMAGE = 28.0F;
    private static final DustParticleOptions WARNING_RING = new DustParticleOptions(new Vector3f(1.0F, 0.14F, 0.14F), 1.0F);
    private static final DustParticleOptions CHEST_GLOW = new DustParticleOptions(new Vector3f(0.28F, 0.84F, 1.0F), 1.15F);

    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        LivingEntity target = boss.getTarget();
        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
        boss.hasImpulse = true;

        if (boss.attackTick == 1) {
            boss.coralBaseAngle = boss.getRandom().nextDouble() * ((Math.PI * 2.0D) / WAVE_COUNT);
            boss.coralHitTicks.clear();
            boss.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.3F, 0.68F);
            boss.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.8F, 0.72F);
            boss.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.8F, 0.55F);
        }

        if (boss.attackTick <= CHARGE_TICKS) {
            renderChestPulse(boss, sl);
            renderShrinkingWarning(target, sl, boss.attackTick);
            playChargeAudio(boss, sl);
            return;
        }

        int novaTick = boss.attackTick - CHARGE_TICKS;
        if (novaTick <= NOVA_TICKS) {
            emitSonicNova(boss, sl, novaTick);
            return;
        }

        boss.coralHitTicks.clear();
        boss.resetAttack();
    }

    private static void renderChestPulse(TideSourcerEntity boss, ServerLevel sl) {
        Vec3 chest = boss.position().add(0.0D, 2.2D, 0.0D);
        sl.sendParticles(CHEST_GLOW, chest.x, chest.y, chest.z, 2, 0.12, 0.12, 0.12, 0.0);
        if (boss.attackTick % 4 == 0) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, chest.x, chest.y, chest.z, 1, 0.12, 0.12, 0.12, 0.0);
        }
    }

    private static void renderShrinkingWarning(LivingEntity target, ServerLevel sl, int tick) {
        if (target == null || !target.isAlive()) {
            return;
        }

        double radius = 2.6D - ((tick - 1) / (double) CHARGE_TICKS) * 2.0D;
        radius = Math.max(0.6D, radius);
        Vec3 center = target.position().add(0.0D, 0.05D, 0.0D);

        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2.0D * i) / 10.0D;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            sl.sendParticles(WARNING_RING, x, center.y, z, 1, 0.02, 0.0, 0.02, 0.0);
        }
    }

    private static void playChargeAudio(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.attackTick % 8 == 0) {
            float pitch = 0.74F + (boss.attackTick / (float) CHARGE_TICKS) * 0.32F;
            AbyssalEffects.play(sl, boss.position(), SoundEvents.WARDEN_HEARTBEAT, 2.6F, pitch);
        }
        if (boss.attackTick % 10 == 0) {
            float pitch = 0.78F + (boss.attackTick / (float) CHARGE_TICKS) * 0.22F;
            boss.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.5F, pitch);
        }
    }

    private static void emitSonicNova(TideSourcerEntity boss, ServerLevel sl, int novaTick) {
        Vec3 center = boss.position().add(0.0D, 1.5D, 0.0D);
        double baseAngle = boss.coralBaseAngle;
        double angleStep = (Math.PI * 2.0D) / WAVE_COUNT;
        double currentDistance = WAVE_RANGE * novaTick / (double) NOVA_TICKS;

        if (novaTick == 1) {
            boss.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4.2F, 0.82F);
            boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.8F, 0.72F);
        }

        for (int wave = 0; wave < WAVE_COUNT; wave++) {
            double angle = baseAngle + angleStep * wave;
            Vec3 dir = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));

            int segmentSamples = Math.max(1, (int) Math.ceil(currentDistance / 0.8D));
            for (int sampleIndex = 0; sampleIndex <= segmentSamples; sampleIndex++) {
                double distance = currentDistance * (sampleIndex / (double) segmentSamples);
                Vec3 sample = center.add(dir.scale(distance));
                sl.sendParticles(ParticleTypes.SONIC_BOOM, sample.x, sample.y, sample.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        AABB hitBox = new AABB(
                center.x - (WAVE_RANGE + 2.0D),
                center.y - 2.5D,
                center.z - (WAVE_RANGE + 2.0D),
                center.x + (WAVE_RANGE + 2.0D),
                center.y + 2.5D,
                center.z + (WAVE_RANGE + 2.0D)
        );
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, hitBox, entity -> entity != boss && entity.isAlive());
        for (LivingEntity entity : targets) {
            int lastHitTick = boss.coralHitTicks.getOrDefault(entity.getUUID(), -HIT_COOLDOWN_TICKS);
            if (novaTick - lastHitTick < HIT_COOLDOWN_TICKS) {
                continue;
            }
            if (!isInsideActiveRay(entity, center, baseAngle, angleStep, currentDistance)) {
                continue;
            }

            boss.coralHitTicks.put(entity.getUUID(), novaTick);
            entity.hurt(boss.damageSources().magic(), WAVE_DAMAGE);
            Vec3 launch = entity.position().subtract(boss.position());
            if (launch.lengthSqr() < 1.0E-4D) {
                launch = new Vec3(0.0D, 0.0D, 1.0D);
            }
            launch = launch.normalize().scale(1.35D);
            entity.setDeltaMovement(launch.x, 0.55D, launch.z);
            entity.hasImpulse = true;
            entity.hurtMarked = true;
        }
    }

    private static boolean isInsideActiveRay(LivingEntity entity, Vec3 center, double baseAngle, double angleStep, double currentDistance) {
        Vec3 offset = entity.position().subtract(center);
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        if (horizontalDistance > WAVE_RANGE || horizontalDistance < 0.45D) {
            return false;
        }
        double distancePadding = 0.9D + entity.getBbWidth() * 0.5D;
        if (horizontalDistance > currentDistance + distancePadding) {
            return false;
        }
        if (Math.abs(offset.y) > 2.2D) {
            return false;
        }

        double angle = Math.atan2(offset.z, offset.x);
        double normalized = wrapRadians(angle - baseAngle);
        double nearestDiff = Math.min(normalized % angleStep, angleStep - (normalized % angleStep));
        double lateralOffset = Math.sin(nearestDiff) * horizontalDistance;
        double targetRadius = WAVE_WIDTH + entity.getBbWidth() * 0.5D;
        return lateralOffset <= targetRadius;
    }

    private static double wrapRadians(double angle) {
        double full = Math.PI * 2.0D;
        double wrapped = angle % full;
        return wrapped < 0.0D ? wrapped + full : wrapped;
    }
}
