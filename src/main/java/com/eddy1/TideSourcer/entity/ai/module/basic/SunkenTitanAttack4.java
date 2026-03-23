package com.eddy1.tidesourcer.entity.ai.module.basic;

import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SunkenTitanAttack4 {
    private static final int RAY_CHARGE_TICKS = 20;
    private static final int RAY_END_TICKS = 80;
    private static final double RAY_RANGE = 36.0D;
    private static final double RAY_RADIUS = 1.35D;
    private static final float RAY_DAMAGE = 8.0F;

    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.attackVariant == 1) {
            LivingEntity target = boss.getTarget();
            if (target == null) return;

            if (boss.attackTick == 10) boss.playSound(SoundEvents.PHANTOM_SWOOP, 1.5F, 0.6F);

            if (boss.attackTick == 20) {
                if (boss.distanceToSqr(target) <= 400.0D) {
                    boss.playSound(SoundEvents.ENDERMAN_SCREAM, 1.5F, 0.8F);
                    Vec3 toBoss = boss.position().subtract(target.position()).normalize();
                    target.setDeltaMovement(toBoss.x * 3.5D, 0.6D, toBoss.z * 3.5D);
                    target.hurtMarked = true;
                    target.hasImpulse = true;
                    AbyssalEffects.spawnInfectionCloud(sl, target.position().add(0, 1.0, 0), 0.8, 0.6);
                }
            }

            if (boss.attackTick >= 30 && boss.attackTick < 75) {
                if (boss.distanceToSqr(target) <= 400.0D) {
                    Vec3 lookDir = boss.getLookAngle().normalize();
                    double lockX = boss.getX() + lookDir.x * 1.5;
                    double lockZ = boss.getZ() + lookDir.z * 1.5;

                    target.teleportTo(lockX, boss.getY(), lockZ);
                    target.setDeltaMovement(0, -0.1, 0);
                    target.hasImpulse = true;
                    target.hurtMarked = true;

                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 10, false, false, false));
                    if (boss.attackTick % 10 == 0) {
                        target.hurt(boss.damageSources().mobAttack(boss), 6.0F);
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, false, false));
                        AbyssalEffects.spawnFearBurst(sl, target.position().add(0, 1.0, 0), 0.3, 0.3);
                        boss.playSound(SoundEvents.SOUL_ESCAPE.value(), 1.0F, 1.1F);
                    }
                }
            }

            if (boss.attackTick == 75) {
                if (boss.distanceToSqr(target) <= 400.0D) {
                    target.hurt(boss.damageSources().mobAttack(boss), 22.0F);
                    boss.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0F, 0.8F);
                    boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.15F, 0.82F);
                    AbyssalEffects.spawnImpact(sl, target.position().add(0, 1.0, 0), 0.8, 0.8);

                    Vec3 pushDir = target.position().subtract(boss.position()).normalize();
                    target.setDeltaMovement(pushDir.x * 2.0D, 1.2D, pushDir.z * 2.0D);
                    target.hasImpulse = true;
                    target.hurtMarked = true;
                }
            }
            if (boss.attackTick >= 90) boss.resetAttack();

        } else if (boss.attackVariant == 2) {
            if (boss.attackTick == 20) {
                boss.playSound(SoundEvents.WITHER_SHOOT, 2.0F, 0.5F);
                Vec3 dir = boss.getLookAngle().normalize();
                for (int i = 1; i <= 15; i++) {
                    double px = boss.getX() + dir.x * i;
                    double py = boss.getY() + 1.5;
                    double pz = boss.getZ() + dir.z * i;
                    AbyssalEffects.spawnInfectionCloud(sl, new Vec3(px, py, pz), i * 0.3, 1.0);
                }
                AABB hitBox = boss.getBoundingBox().inflate(15.0D);
                List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != boss && e.isAlive());
                for (LivingEntity e : targets) {
                    Vec3 toTarget = e.position().subtract(boss.position()).normalize();
                    if (toTarget.dot(dir) > 0.3 && boss.distanceToSqr(e) <= 225.0D) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
                    }
                }
            }
            if (boss.attackTick >= 60) boss.resetAttack();
        } else if (boss.attackVariant == 3) {
            LivingEntity target = boss.getTarget();
            boss.getNavigation().stop();
            boss.setDeltaMovement(Vec3.ZERO);
            boss.hasImpulse = true;

            if (boss.attackTick == 10) {
                boss.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2.0F, 0.8F);
                boss.playSound(SoundEvents.BEACON_ACTIVATE, 1.6F, 0.68F);
            }
            if (boss.attackTick >= 1 && boss.attackTick < RAY_CHARGE_TICKS && target != null && target.isAlive()) {
                trackRayTarget(boss, target, 4.0F, 2.5F);
            }
            if (boss.attackTick == RAY_CHARGE_TICKS) {
                boss.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.2F, 0.86F);
            }
            if (boss.attackTick >= RAY_CHARGE_TICKS && boss.attackTick <= RAY_END_TICKS) {
                if (target != null && target.isAlive()) {
                    trackRayTarget(boss, target, 3.2F, 2.0F);
                }
                if ((boss.attackTick - RAY_CHARGE_TICKS) % 8 == 0) {
                    boss.playSound(SoundEvents.BEACON_AMBIENT, 1.4F, 0.62F);
                }
                fireAbyssalRay(boss, sl);
            }
            if (boss.attackTick == RAY_END_TICKS + 1) {
                boss.playSound(SoundEvents.BEACON_DEACTIVATE, 1.8F, 0.6F);
            }
            if (boss.attackTick >= 100) boss.resetAttack();
        }
    }

    private static void trackRayTarget(TideSourcerEntity boss, LivingEntity target, float maxYawChange, float maxPitchChange) {
        Vec3 origin = boss.position().add(0.0D, 2.3D, 0.0D);
        Vec3 targetPos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 delta = targetPos.subtract(origin);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float targetYaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(delta.y, horizontal) * (180.0D / Math.PI)));

        float nextYaw = Mth.approachDegrees(boss.getYRot(), targetYaw, maxYawChange);
        float nextPitch = Mth.approachDegrees(boss.getXRot(), targetPitch, maxPitchChange);

        boss.setYRot(nextYaw);
        boss.setYHeadRot(nextYaw);
        boss.yBodyRot = nextYaw;
        boss.setXRot(Mth.clamp(nextPitch, -35.0F, 30.0F));
    }

    private static void fireAbyssalRay(TideSourcerEntity boss, ServerLevel sl) {
        Vec3 direction = boss.getLookAngle().normalize();
        Vec3 start = boss.position().add(0.0D, 2.3D, 0.0D).add(direction.scale(1.2D));
        Vec3 desiredEnd = start.add(direction.scale(RAY_RANGE));
        BlockHitResult blockHit = sl.clip(new ClipContext(start, desiredEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? desiredEnd : blockHit.getLocation();

        int steps = Math.max(1, Mth.ceil(start.distanceTo(end) / 0.75D));
        Set<LivingEntity> hits = new HashSet<>();

        for (int i = 0; i <= steps; i++) {
            Vec3 sample = start.lerp(end, i / (double) steps);
            AABB hitBox = new AABB(
                    sample.x - RAY_RADIUS,
                    sample.y - RAY_RADIUS,
                    sample.z - RAY_RADIUS,
                    sample.x + RAY_RADIUS,
                    sample.y + RAY_RADIUS,
                    sample.z + RAY_RADIUS
            );
            hits.addAll(sl.getEntitiesOfClass(LivingEntity.class, hitBox, entity -> entity != boss && entity.isAlive()));
        }

        for (LivingEntity hit : hits) {
            hit.hurt(boss.damageSources().magic(), RAY_DAMAGE);
            hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 1, false, false, false));
            Vec3 push = direction.scale(0.22D);
            hit.setDeltaMovement(hit.getDeltaMovement().add(push.x, 0.02D, push.z));
            hit.hasImpulse = true;
            hit.hurtMarked = true;
        }
    }
}
