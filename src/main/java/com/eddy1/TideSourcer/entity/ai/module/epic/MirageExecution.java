package com.eddy1.tidesourcer.entity.ai.module.epic;

import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

public class MirageExecution {
    public static final int IDENTIFY_TICKS = 60;
    private static final int BARRAGE_PREP_TICKS = 14;
    private static final int RELEASE_TICK = IDENTIFY_TICKS + BARRAGE_PREP_TICKS;
    private static final int FINISH_TICKS = RELEASE_TICK + 18;
    private static final int SECOND_TAUNT_TICK = IDENTIFY_TICKS + 7;
    private static final int CLONE_TOTAL = 8;
    private static final double CLONE_RADIUS = 15.0D;
    private static final float CLONE_SHOT_DAMAGE = 6.0F;
    private static final float FINAL_SHOT_DAMAGE = 24.0F;

    @SuppressWarnings("unchecked")
    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        LivingEntity target = boss.getTarget();
        if (target == null) {
            boss.interruptEncounter();
            return;
        }

        if (boss.attackTick == 1) {
            boss.mirageFailed = false;
            boss.mirageSuccess = false;
            boss.mirageFinalShotFired = false;
            boss.cleanupMirageClones();
            boss.playSound(SoundEvents.ENDERMAN_SCREAM, 2.0F, 0.5F);

            int trueIndex = sl.random.nextInt(CLONE_TOTAL);
            for (int i = 0; i < CLONE_TOTAL; i++) {
                double angle = Math.toRadians(i * 45);
                double px = target.getX() + Math.cos(angle) * CLONE_RADIUS;
                double pz = target.getZ() + Math.sin(angle) * CLONE_RADIUS;

                if (i == trueIndex) {
                    boss.teleportTo(px, target.getY(), pz);
                    boss.getLookControl().setLookAt(target);
                } else {
                    TideSourcerEntity clone = new TideSourcerEntity((EntityType<? extends Monster>) boss.getType(), sl);
                    clone.setPos(px, target.getY(), pz);
                    clone.getLookControl().setLookAt(target);
                    clone.isClone = true;
                    clone.mainBoss = boss;
                    clone.setHealth(1.0F);
                    sl.addFreshEntity(clone);
                    boss.activeClones.add(clone);
                }
            }
        }

        if (boss.attackTick > 1 && boss.attackTick < IDENTIFY_TICKS) {
            AbyssalEffects.spawnCharge(sl, boss.position().add(0.0D, 0.2D, 0.0D), 0.35D, 0.08D);
            if (boss.attackTick % 6 == 0) {
                for (TideSourcerEntity clone : boss.activeClones) {
                    if (clone != null && clone.isAlive()) {
                        AbyssalEffects.spawnCharge(sl, clone.position().add(0.0D, 0.2D, 0.0D), 0.22D, 0.05D);
                    }
                }
            }
            if (boss.mirageFailed) {
                boss.attackTick = IDENTIFY_TICKS;
            } else if (boss.mirageSuccess) {
                boss.playSound(SoundEvents.GLASS_BREAK, 2.0F, 1.5F);
                boss.cleanupMirageClones();
                boss.resetAttack();
                return;
            }
        }

        if (boss.attackTick == IDENTIFY_TICKS) {
            boss.mirageFinalShotFired = false;
            triggerMirageTaunt(boss);
        }

        if (boss.attackTick > IDENTIFY_TICKS && boss.attackTick < RELEASE_TICK) {
            holdMirageFormation(boss, target);
            if (boss.attackTick == SECOND_TAUNT_TICK) {
                triggerMirageTaunt(boss);
            }
            pulseMirageCharge(boss, sl);
        }

        if (boss.attackTick == RELEASE_TICK && !boss.mirageFinalShotFired) {
            fireSimultaneousVolley(boss, sl, target);
            boss.mirageFinalShotFired = true;
        }

        if (boss.attackTick >= FINISH_TICKS) {
            boss.resetAttack();
        }
    }

    private static void triggerMirageTaunt(TideSourcerEntity boss) {
        LivingEntity target = boss.getTarget();
        if (target != null) {
            boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        boss.triggerAnim("attack_controller", "attack7");
        boss.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.9F, 0.74F);
        for (TideSourcerEntity clone : boss.activeClones) {
            if (clone != null && clone.isAlive()) {
                if (target != null) {
                    clone.getLookControl().setLookAt(target, 30.0F, 30.0F);
                }
                clone.setDeltaMovement(Vec3.ZERO);
                clone.hasImpulse = true;
                clone.triggerAnim("attack_controller", "attack7");
            }
        }
    }

    private static void holdMirageFormation(TideSourcerEntity boss, LivingEntity target) {
        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
        boss.hasImpulse = true;
        if (target != null) {
            boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        for (TideSourcerEntity clone : boss.activeClones) {
            if (clone == null || !clone.isAlive()) {
                continue;
            }
            clone.setDeltaMovement(Vec3.ZERO);
            clone.hasImpulse = true;
            if (target != null) {
                clone.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
    }

    private static void pulseMirageCharge(TideSourcerEntity boss, ServerLevel sl) {
        AbyssalEffects.spawnCharge(sl, boss.position().add(0.0D, 1.0D, 0.0D), 0.28D, 0.16D);
        if (boss.attackTick % 4 == 0) {
            for (TideSourcerEntity clone : boss.activeClones) {
                if (clone != null && clone.isAlive()) {
                    AbyssalEffects.spawnCharge(sl, clone.position().add(0.0D, 1.0D, 0.0D), 0.16D, 0.08D);
                }
            }
        }
    }

    private static void fireSimultaneousVolley(TideSourcerEntity boss, ServerLevel sl, LivingEntity target) {
        Vec3 targetPos = target.position().add(0.0D, 1.0D, 0.0D);
        cleanupInactiveClones(boss);

        for (TideSourcerEntity clone : boss.activeClones) {
            if (clone == null || !clone.isAlive()) {
                continue;
            }
            Vec3 start = clone.position().add(0.0D, 1.5D, 0.0D);
            Vec3 dir = targetPos.subtract(start).normalize();
            AbyssalEffects.play(sl, clone.position(), SoundEvents.WARDEN_SONIC_BOOM, 1.7F, 0.9F);
            spawnSonicBoomPath(sl, start, dir, 10, 1.6D);
            target.hurt(boss.damageSources().magic(), CLONE_SHOT_DAMAGE);
            AbyssalEffects.spawnImpact(sl, clone.position().add(0.0D, 1.0D, 0.0D), 0.28D, 0.28D);
            clone.discard();
        }

        Vec3 trueBodyStart = boss.position().add(0.0D, 1.7D, 0.0D);
        Vec3 trueBodyDir = targetPos.subtract(trueBodyStart).normalize();
        boss.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.2F, 0.78F);
        spawnSonicBoomPath(sl, trueBodyStart, trueBodyDir, 14, 1.7D);
        target.hurt(boss.damageSources().magic(), FINAL_SHOT_DAMAGE);
        boss.cleanupMirageClones();
    }

    private static void cleanupInactiveClones(TideSourcerEntity boss) {
        boss.activeClones.removeIf(clone -> clone == null || !clone.isAlive());
    }

    private static void spawnSonicBoomPath(ServerLevel sl, Vec3 start, Vec3 direction, int steps, double spacing) {
        Vec3 normalized = direction.normalize();
        for (int i = 1; i <= steps; i++) {
            Vec3 pos = start.add(normalized.scale(i * spacing));
            sl.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
