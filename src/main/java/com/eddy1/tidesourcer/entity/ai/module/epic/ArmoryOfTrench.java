package com.eddy1.tidesourcer.entity.ai.module.epic;

import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;

public class ArmoryOfTrench {
    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        LivingEntity target = boss.getTarget();

        if (boss.attackTick == 1) {
            boss.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3.0F, 0.5F);
            boss.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 130, 0, false, false, false));
        }

        if (boss.attackTick > 10 && boss.attackTick < 130) {
            boss.setDeltaMovement(0, 0, 0);
            boss.hasImpulse = true;

            Vec3 lookDir = boss.getLookAngle().normalize();
            Vec3 right = lookDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 center = boss.position().add(0, 2.5, 0).subtract(lookDir.scale(2.5));

            for (int i = 0; i < 8; i++) {
                double angle = sl.random.nextDouble() * Math.PI;
                double radius = 2.0 + sl.random.nextDouble() * 5.0;
                Vec3 p = center.add(right.scale(Math.cos(angle) * radius)).add(0, Math.sin(angle) * radius, 0);
                AbyssalEffects.spawnCharge(sl, p, 0.0, 0.0);
            }
        }

        if (boss.attackTick >= 40 && boss.attackTick <= 100 && boss.attackTick % 4 == 0 && target != null) {
            fireTreasureWeapon(boss, target, sl, false);
        }

        if (boss.attackTick == 110 && target != null) {
            boss.playSound(SoundEvents.SOUL_ESCAPE.value(), 4.0F, 1.0F);
            Vec3 lookDir = boss.getLookAngle().normalize();
            Vec3 right = lookDir.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 center = boss.position().add(0, 2.5, 0).subtract(lookDir.scale(2.5));
            for (int i = 0; i < 40; i++) {
                double angle = sl.random.nextDouble() * Math.PI;
                double radius = 2.0 + sl.random.nextDouble() * 5.0;
                Vec3 p = center.add(right.scale(Math.cos(angle) * radius)).add(0, Math.sin(angle) * radius, 0);
                AbyssalEffects.spawnFearBurst(sl, p, 0.0, 0.0);
            }
        }

        if (boss.attackTick == 120 && target != null) {
            for (int i = 0; i < 20; i++) {
                fireTreasureWeapon(boss, target, sl, true);
            }
            boss.armoryActiveTick = 800;
        }

        if (boss.attackTick >= 140) {
            boss.removeEffect(MobEffects.LEVITATION);
            boss.resetAttack();
        }
    }

    private static void fireTreasureWeapon(TideSourcerEntity boss, LivingEntity target, ServerLevel sl, boolean isVolley) {
        Vec3 lookDir = boss.getLookAngle().normalize();
        Vec3 right = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        double spawnAngle = sl.random.nextDouble() * Math.PI;
        double spawnRadius = 3.0 + sl.random.nextDouble() * 6.0;
        Vec3 spawnPos = boss.position().add(0, 3.5, 0).subtract(lookDir.scale(2.0))
                .add(right.scale(Math.cos(spawnAngle) * spawnRadius))
                .add(0, Math.sin(spawnAngle) * spawnRadius, 0);

        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
        Vec3 targetVel = target.getDeltaMovement();
        targetPos = targetPos.add(targetVel.scale(8.0D));

        Vec3 shootDir = targetPos.subtract(spawnPos).normalize();

        if (isVolley) {
            shootDir = shootDir.add(
                    (sl.random.nextDouble() - 0.5) * 0.4,
                    (sl.random.nextDouble() - 0.5) * 0.4,
                    (sl.random.nextDouble() - 0.5) * 0.4
            ).normalize();
        }

        AbyssalEffects.spawnImpact(sl, spawnPos, 0.2, 0.2);

        float rand = sl.random.nextFloat();
        AbstractArrow weapon;

        if (rand < 0.3F) {
            boss.playSound(SoundEvents.WITHER_SHOOT, 2.0F, 0.8F + sl.random.nextFloat() * 0.4F);
            weapon = new ThrownTrident(EntityType.TRIDENT, sl);
            weapon.setBaseDamage(12.0D);
        } else if (rand < 0.7F) {
            boss.playSound(SoundEvents.SKELETON_SHOOT, 2.0F, 0.5F + sl.random.nextFloat() * 0.5F);
            weapon = new SpectralArrow(EntityType.SPECTRAL_ARROW, sl);
            weapon.setBaseDamage(11.0D);
        } else {
            boss.playSound(SoundEvents.CROSSBOW_SHOOT, 2.0F, 0.8F + sl.random.nextFloat() * 0.4F);
            weapon = new Arrow(EntityType.ARROW, sl);
            weapon.setBaseDamage(14.0D);
        }

        weapon.setOwner(boss);
        weapon.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        weapon.setDeltaMovement(shootDir.scale(3.5D));
        weapon.pickup = AbstractArrow.Pickup.DISALLOWED;
        sl.addFreshEntity(weapon);
        boss.activeArmoryWeapons.add(weapon);
    }
}
