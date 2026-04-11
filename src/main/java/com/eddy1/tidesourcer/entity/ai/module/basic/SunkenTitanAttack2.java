package com.eddy1.tidesourcer.entity.ai.module.basic;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SunkenTitanAttack2 {
    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.attackVariant == 1) {
            if (boss.attackTick == 15) {
                LivingEntity target = boss.getTarget();
                if (target != null) {
                    Vec3 delta = target.position().subtract(boss.position());
                    Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
                    if (horizontal.lengthSqr() < 1.0E-4D) {
                        horizontal = boss.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
                    }
                    Vec3 dash = horizontal.normalize();
                    double horizontalDistance = Math.max(1.0D, horizontal.length());
                    double horizontalSpeed = Math.min(1.9D, 0.95D + horizontalDistance * 0.08D);
                    double verticalBoost = 0.42D + Math.min(0.26D, horizontalDistance * 0.02D) + Math.max(0.0D, Math.min(0.18D, delta.y * 0.05D));
                    boss.setDeltaMovement(dash.x * horizontalSpeed, verticalBoost, dash.z * horizontalSpeed);
                    boss.hasImpulse = true;
                    boss.playSound(SoundEvents.PHANTOM_SWOOP, 1.2F, 0.7F);
                }
            }
            if (boss.attackTick > 15 && boss.attackTick < 30) {
                LivingEntity target = boss.getTarget();
                if (target != null && !boss.onGround()) {
                    Vec3 delta = target.position().subtract(boss.position());
                    Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
                    if (horizontal.lengthSqr() > 1.0E-4D) {
                        Vec3 desired = horizontal.normalize();
                        Vec3 current = boss.getDeltaMovement();
                        boss.setDeltaMovement(
                                current.x * 0.86D + desired.x * 0.16D,
                                current.y,
                                current.z * 0.86D + desired.z * 0.16D
                        );
                        boss.hasImpulse = true;
                    }
                }
            }
            if (boss.attackTick == 35) {
                AABB hitBox = boss.getBoundingBox().inflate(4.0D, 0.5D, 4.0D).move(0, -0.5, 0);
                List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != boss && e.isAlive());
                for (LivingEntity e : targets) {
                    if (e.distanceToSqr(boss.getX(), e.getY(), boss.getZ()) <= 16.0D) {
                        e.hurt(boss.damageSources().mobAttack(boss), AbyssalConfig.scaledDamage(22.0F));
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, false, false));
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6D, 0));
                        e.hasImpulse = true;
                    }
                }
                boss.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0F, 0.8F);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.3F, 0.72F);
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    double px = boss.getX() + Math.cos(rad) * 3.5;
                    double pz = boss.getZ() + Math.sin(rad) * 3.5;
                    AbyssalEffects.spawnRiftPillar(sl, px, boss.getY() + 0.2, pz);
                }
                AbyssalEffects.spawnImpact(sl, boss.position().add(0, 0.1, 0), 1.0, 0.3);
            }
            if (boss.attackTick >= 60) boss.resetAttack();
        } else if (boss.attackVariant == 2) {
            if (boss.attackTick == 15) {
                LivingEntity target = boss.getTarget();
                if (target != null) {
                    boss.geyserX = target.getX();
                    boss.geyserY = target.getY();
                    boss.geyserZ = target.getZ();
                } else {
                    boss.geyserX = boss.getX();
                    boss.geyserY = boss.getY();
                    boss.geyserZ = boss.getZ();
                }
                boss.setDeltaMovement(0, 1.0D, 0);
                boss.hasImpulse = true;
                boss.playSound(SoundEvents.WITHER_SPAWN, 1.5F, 1.1F);
            }
            if (boss.attackTick >= 15 && boss.attackTick < 35) {
                AbyssalEffects.spawnInfectionCloud(sl, new Vec3(boss.geyserX, boss.geyserY + 0.5, boss.geyserZ), 1.0, 0.3);
            }
            if (boss.attackTick == 35) {
                boss.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0F, 0.8F);
                boss.playSound(SoundEvents.SOUL_ESCAPE.value(), 2.0F, 0.5F);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.68F);
                for (int h = 0; h <= 6; h++) {
                    AbyssalEffects.spawnRiftPillar(sl, boss.geyserX, boss.geyserY + (h * 0.8), boss.geyserZ);
                }
                AbyssalEffects.spawnImpact(sl, new Vec3(boss.geyserX, boss.geyserY + 0.5, boss.geyserZ), 1.5, 3.0);
                AABB hitBox = new AABB(boss.geyserX - 2.5, boss.geyserY - 1.0, boss.geyserZ - 2.5, boss.geyserX + 2.5, boss.geyserY + 4.0, boss.geyserZ + 2.5);
                List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != boss && e.isAlive());
                for (LivingEntity e : targets) {
                    e.hurt(boss.damageSources().mobAttack(boss), AbyssalConfig.scaledDamage(18.0F));
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 1.2D, 0));
                    e.hasImpulse = true;
                }
            }
            if (boss.attackTick >= 60) boss.resetAttack();
        } else if (boss.attackVariant == 3) {
            if (boss.attackTick == 10) {
                LivingEntity target = boss.getTarget();
                if (target != null) {
                    boss.geyserX = target.getX();
                    boss.geyserY = target.getY();
                    boss.geyserZ = target.getZ();
                }
                boss.playSound(SoundEvents.WITHER_SPAWN, 3.0F, 0.8F);
                boss.setDeltaMovement(0, 3.5D, 0);
                boss.hasImpulse = true;
            }
            if (boss.attackTick >= 10 && boss.attackTick < 50) {
                AbyssalEffects.spawnInfectionCloud(sl, new Vec3(boss.geyserX, boss.geyserY + 0.2, boss.geyserZ), 3.0, 0.2);
                AbyssalEffects.spawnCharge(sl, new Vec3(boss.geyserX, boss.geyserY + 0.2, boss.geyserZ), 3.0, 0.2);
            }
            if (boss.attackTick == 50) {
                boss.teleportTo(boss.geyserX, boss.geyserY + 0.5, boss.geyserZ);
                boss.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.8F);
                boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.62F);
                for (int i = 0; i < 360; i += 5) {
                    double rad = Math.toRadians(i);
                    double px = boss.getX() + Math.cos(rad) * 10.0;
                    double pz = boss.getZ() + Math.sin(rad) * 10.0;
                    AbyssalEffects.spawnFearBurst(sl, new Vec3(px, boss.getY() + 0.5, pz), 0.5, 2.0);
                }
                AbyssalEffects.spawnImpact(sl, boss.position().add(0, 1.0, 0), 2.0, 1.0);

                AABB hitBox = boss.getBoundingBox().inflate(10.0D);
                List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != boss && e.isAlive());
                for (LivingEntity e : targets) {
                    double dist = Math.sqrt(e.distanceToSqr(boss));
                    float damage = (float) (36.0D - dist);
                    if (damage > 0) {
                        e.hurt(boss.damageSources().mobAttack(boss), AbyssalConfig.scaledDamage(damage));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, false, false));
                        Vec3 push = e.position().subtract(boss.position()).normalize().scale(1.5D);
                        e.setDeltaMovement(push.x, 0.8D, push.z);
                        e.hasImpulse = true;
                    }
                }
            }
            if (boss.attackTick >= 70) boss.resetAttack();
        }
    }
}
