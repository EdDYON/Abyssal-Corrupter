package com.eddy1.tidesourcer.entity.ai.module.basic;

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

public class SunkenTitanAttack1 {
    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.attackVariant == 1) {
            if (boss.attackTick == 15) {
                performPreciseStrike(boss, 3.0, 0.5, true);
                boss.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.2F, 0.8F);
                Vec3 lookDir = boss.getLookAngle().normalize().scale(1.5);
                AbyssalEffects.spawnImpact(sl, boss.position().add(lookDir).add(0, 1.0, 0), 1.0, 0.5);
            }
            if (boss.attackTick >= 30) boss.resetAttack();
        } else if (boss.attackVariant == 2) {
            if (boss.attackTick >= 10 && boss.attackTick <= 30 && boss.attackTick % 5 == 0) {
                performPreciseStrike(boss, 4.5, 0.2, false);
                boss.playSound(SoundEvents.WITHER_SHOOT, 1.4F, 0.9F + (sl.random.nextFloat() * 0.25F));
                boss.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.1F, 1.0F);

                Vec3 lookDir = boss.getLookAngle().normalize();
                boss.setDeltaMovement(lookDir.x * 0.4, boss.getDeltaMovement().y, lookDir.z * 0.4);
                boss.hasImpulse = true;

                Vec3 particlePos = boss.position().add(lookDir.scale(2.0)).add(0, 1.0, 0);
                AbyssalEffects.spawnFearBurst(sl, particlePos, 0.8, 0.5);
            }
            if (boss.attackTick >= 45) boss.resetAttack();
        } else if (boss.attackVariant == 3) {
            LivingEntity target = boss.getTarget();
            if (boss.attackTick == 10) {
                boss.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2.0F, 0.7F);
            }
            if (boss.attackTick >= 10 && boss.attackTick < 30) {
                AbyssalEffects.spawnCharge(sl, boss.position().add(0, 3.0, 0), 2.0, 0.5);
            }
            if (boss.attackTick >= 30 && boss.attackTick <= 50 && boss.attackTick % 5 == 0 && target != null) {
                boss.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.1F);
                Vec3 startPos = boss.position().add(0, 3.0, 0);
                Vec3 dir = target.position().add(0, target.getBbHeight() / 2.0, 0).subtract(startPos).normalize();
                AbyssalEffects.spawnBeam(sl, startPos, dir, 25, 1.0);
                for (int i = 1; i <= 25; i++) {
                    Vec3 path = startPos.add(dir.scale(i));
                    AABB hitBox = new AABB(path.x - 0.5, path.y - 0.5, path.z - 0.5, path.x + 0.5, path.y + 0.5, path.z + 0.5);
                    List<LivingEntity> hits = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != boss && e.isAlive());
                    for (LivingEntity e : hits) {
                        e.hurt(boss.damageSources().magic(), 12.0F);
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false, false));
                    }
                }
            }
            if (boss.attackTick >= 70) boss.resetAttack();
        }
    }

    private static void performPreciseStrike(TideSourcerEntity boss, double reach, double arcThreshold, boolean useDefaultDamage) {
        Vec3 lookDir = boss.getLookAngle().normalize();
        Vec3 bossPos = boss.position();
        AABB hitBox = boss.getBoundingBox().inflate(reach);
        List<LivingEntity> hitTargets = boss.level().getEntitiesOfClass(LivingEntity.class, hitBox, entity -> {
            if (entity == boss || !entity.isAlive()) return false;
            Vec3 dirToTarget = entity.position().subtract(bossPos).normalize();
            return dirToTarget.dot(lookDir) > arcThreshold;
        });
        for (LivingEntity hitTarget : hitTargets) {
            if (useDefaultDamage) {
                boss.doHurtTarget(hitTarget);
            } else {
                hitTarget.hurt(boss.damageSources().mobAttack(boss), 8.0F);
            }
        }
    }
}
