package com.eddy1.tidesourcer.entity.ai.module.basic;

import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.ai.module.SkillCastHelper;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SunkenTitanAttack3 {
    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.attackVariant == 1) {
            if (boss.attackTick >= 15 && boss.attackTick <= 70) {
                if (boss.attackTick % 2 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = sl.random.nextDouble() * 2 * Math.PI;
                        double radius = 1.0 + sl.random.nextDouble() * 11.0;
                        double px = boss.getX() + Math.cos(angle) * radius;
                        double pz = boss.getZ() + Math.sin(angle) * radius;
                        AbyssalEffects.spawnInfectionCloud(sl, new Vec3(px, boss.getY() + sl.random.nextDouble() * 3.0, pz), 0.4, 0.4);
                        AbyssalEffects.spawnCharge(sl, new Vec3(px, boss.getY() + 1.0, pz), 0.2, 0.5);
                    }
                }
                AABB pullBox = boss.getBoundingBox().inflate(12.0D);
                List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != boss && e.isAlive());
                for (LivingEntity e : targets) {
                    double distSqr = e.distanceToSqr(boss);
                    if (distSqr <= 144.0D) {
                        Vec3 pull = boss.position().subtract(e.position()).normalize().scale(0.15D);
                        e.setDeltaMovement(e.getDeltaMovement().add(pull.x, 0.0D, pull.z));
                        e.hasImpulse = true;
                        e.hurtMarked = true;
                        if (distSqr <= 16.0D && boss.attackTick % 10 == 0) {
                            e.hurt(boss.damageSources().mobAttack(boss), 8.0F);
                            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, false, false));
                        }
                    }
                }
            }
            if (boss.attackTick >= 80) boss.resetAttack();
        } else if (boss.attackVariant == 2) {
            if (boss.attackTick == 1 && !SkillCastHelper.snapCasterToGround(boss, sl, 3.0D)) {
                boss.resetAttack();
                return;
            }
            Vec3 center = SkillCastHelper.groundCenter(boss, sl, 1.0D);
            if (boss.attackTick == 1) {
                boss.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.6F, 0.78F);
            }
            if (boss.attackTick >= 6 && boss.attackTick < 20 && boss.attackTick % 4 == 2) {
                renderRepulsionTelegraph(sl, center, boss.attackTick);
                if (boss.attackTick == 14) {
                    boss.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.6F, 0.74F);
                }
            }
            if (boss.attackTick == 20) {
                boss.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.0F);
                AbyssalEffects.spawnFearBurst(sl, center, 3.0, 1.0);
                AbyssalEffects.spawnImpact(sl, center, 3.0, 1.0);
                renderRepulsionReleaseFlash(sl, center);
                spawnRepulsionWave(sl, center);

                AABB pushBox = boss.getBoundingBox().inflate(8.0D);
                List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, pushBox, e -> e != boss && e.isAlive());
                for (LivingEntity e : targets) {
                    Vec3 push = e.position().subtract(boss.position()).normalize().scale(1.8D);
                    e.setDeltaMovement(push.x, 0.6D, push.z);
                    e.hasImpulse = true;
                    e.hurt(boss.damageSources().mobAttack(boss), 10.0F);
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, false, false));
                    AbyssalEffects.spawnImpact(sl, e.position().add(0.0D, 1.0D, 0.0D), 0.35D, 0.35D);
                }

                List<Projectile> projectiles = sl.getEntitiesOfClass(Projectile.class, pushBox);
                for (Projectile p : projectiles) {
                    p.discard();
                }
            }
            if (boss.attackTick > 20 && boss.attackTick <= 28 && boss.attackTick % 4 == 0) {
                renderRepulsionAftershock(sl, center, boss.attackTick - 20);
            }
            if (boss.attackTick >= 80) boss.resetAttack();
        } else if (boss.attackVariant == 3) {
            LivingEntity target = boss.getTarget();
            if (boss.attackTick == 15 && target != null) {
                boss.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2.0F, 0.6F);
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1, false, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 4, false, false, false));
            }
            if (boss.attackTick >= 15 && boss.attackTick < 75 && target != null) {
                Vec3 targetPos = target.position().add(0, 1.0, 0);
                AbyssalEffects.spawnInfectionCloud(sl, targetPos, 1.5, 1.5);
                AbyssalEffects.spawnFearBurst(sl, targetPos, 1.2, 1.2);
            }
            if (boss.attackTick == 75 && target != null) {
                boss.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0F, 0.8F);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.78F);
                AbyssalEffects.spawnImpact(sl, target.position().add(0, 1.0, 0), 1.0, 1.0);
                target.removeEffect(MobEffects.LEVITATION);
                target.setDeltaMovement(0, -2.5D, 0);
                target.hasImpulse = true;
                target.hurt(boss.damageSources().magic(), 30.0F);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, false, false));
            }
            if (boss.attackTick >= 90) boss.resetAttack();
        }
    }

    private static void spawnRepulsionWave(ServerLevel sl, Vec3 center) {
        for (int ring = 1; ring <= 3; ring++) {
            double radius = ring * 2.3D;
            int points = 10 + ring * 2;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i) / points;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                sl.sendParticles(ParticleTypes.SONIC_BOOM, x, center.y + 0.04D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if ((i + ring) % 2 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL, x, center.y + 0.02D, z, 1, 0.04D, 0.0D, 0.04D, 0.0D);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, center.y + 0.06D, z, 1, 0.03D, 0.02D, 0.03D, 0.0D);
                }
            }
        }
    }

    private static void renderRepulsionTelegraph(ServerLevel sl, Vec3 center, int tick) {
        double radius = 1.8D + tick * 0.22D;
        int points = 12 + tick / 2;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            sl.sendParticles(ParticleTypes.SOUL, x, center.y + 0.02D, z, 1, 0.03D, 0.0D, 0.03D, 0.0D);
            if (i % 2 == 0) {
                sl.sendParticles(ParticleTypes.SCULK_SOUL, x, center.y + 0.08D, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }

        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.15D, center.z, 2, 0.2D, 0.05D, 0.2D, 0.0D);
    }

    private static void renderRepulsionAftershock(ServerLevel sl, Vec3 center, int waveIndex) {
        double radius = 4.0D + waveIndex * 0.9D;
        int points = 18 + waveIndex * 2;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            sl.sendParticles(ParticleTypes.SONIC_BOOM, x, center.y + 0.05D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (i % 3 == 0) {
                sl.sendParticles(ParticleTypes.SMOKE, x, center.y + 0.02D, z, 1, 0.02D, 0.0D, 0.02D, 0.0D);
            }
        }
    }

    private static void renderRepulsionReleaseFlash(ServerLevel sl, Vec3 center) {
        sl.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.1D, center.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
        sl.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y + 0.1D, center.z, 10, 0.6D, 0.08D, 0.6D, 0.02D);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.14D, center.z, 12, 0.75D, 0.12D, 0.75D, 0.01D);

        for (int ring = 0; ring < 4; ring++) {
            double radius = 1.2D + ring * 1.45D;
            int points = 14 + ring * 4;
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i) / points;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                sl.sendParticles(ParticleTypes.SONIC_BOOM, x, center.y + 0.03D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                if ((i + ring) % 2 == 0) {
                    sl.sendParticles(ParticleTypes.SCULK_SOUL, x, center.y + 0.08D, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
        }
    }
}
