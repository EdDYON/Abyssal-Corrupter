package com.eddy1.tidesourcer.entity.ai.module.epic;

import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.ai.module.SkillCastHelper;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

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
    private static final DustParticleOptions TRUE_BODY_PULSE = new DustParticleOptions(new Vector3f(1.0F, 0.16F, 0.20F), 1.0F);
    private static final DustParticleOptions CLONE_DECAY_PULSE = new DustParticleOptions(new Vector3f(0.16F, 0.74F, 0.86F), 0.72F);

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
                Vec3 anchor = SkillCastHelper.findStandablePosition(boss, sl, px, target.getY(), pz, 8);
                double spawnY = anchor != null ? anchor.y : target.getY();

                if (i == trueIndex) {
                    boss.teleportTo(px, spawnY, pz);
                    boss.setDeltaMovement(Vec3.ZERO);
                    boss.hasImpulse = true;
                    boss.hurtMarked = true;
                    boss.getLookControl().setLookAt(target);
                } else {
                    TideSourcerEntity clone = new TideSourcerEntity((EntityType<? extends Monster>) boss.getType(), sl);
                    clone.setPos(px, spawnY, pz);
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
            emitMirageReadabilityCues(boss, sl, false);
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
            emitMirageReadabilityCues(boss, sl, true);
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

    private static void emitMirageReadabilityCues(TideSourcerEntity boss, ServerLevel sl, boolean barragePrep) {
        int realPulseRate = barragePrep ? 3 : 5;
        if (boss.attackTick % realPulseRate == 0) {
            spawnTrueBodyCue(boss, sl, barragePrep);
        }

        int clonePulseRate = barragePrep ? 7 : 9;
        if (boss.attackTick % clonePulseRate == 0) {
            for (TideSourcerEntity clone : boss.activeClones) {
                if (clone != null && clone.isAlive()) {
                    spawnCloneCue(clone, sl, barragePrep);
                }
            }
        }
    }

    private static void spawnTrueBodyCue(TideSourcerEntity boss, ServerLevel sl, boolean barragePrep) {
        Vec3 crown = boss.position().add(0.0D, boss.getBbHeight() * 0.88D, 0.0D);
        Vec3 lateral = horizontalSideVector(boss).scale(0.34D);
        Vec3 leftHorn = crown.add(lateral).add(0.0D, 0.18D, 0.0D);
        Vec3 rightHorn = crown.subtract(lateral).add(0.0D, 0.18D, 0.0D);

        sl.sendParticles(TRUE_BODY_PULSE, crown.x, crown.y, crown.z, barragePrep ? 7 : 4, 0.18D, 0.08D, 0.18D, 0.0D);
        sl.sendParticles(ParticleTypes.SCULK_SOUL, leftHorn.x, leftHorn.y, leftHorn.z, barragePrep ? 3 : 2, 0.03D, 0.04D, 0.03D, 0.0D);
        sl.sendParticles(ParticleTypes.SCULK_SOUL, rightHorn.x, rightHorn.y, rightHorn.z, barragePrep ? 3 : 2, 0.03D, 0.04D, 0.03D, 0.0D);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, crown.x, crown.y + 0.08D, crown.z, barragePrep ? 4 : 2, 0.1D, 0.06D, 0.1D, 0.0D);
        if (barragePrep) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, crown.x, crown.y + 0.04D, crown.z, 2, 0.08D, 0.04D, 0.08D, 0.0D);
        }
    }

    private static void spawnCloneCue(TideSourcerEntity clone, ServerLevel sl, boolean barragePrep) {
        Vec3 chest = clone.position().add(0.0D, clone.getBbHeight() * 0.74D, 0.0D);
        sl.sendParticles(CLONE_DECAY_PULSE, chest.x, chest.y, chest.z, barragePrep ? 2 : 1, 0.08D, 0.05D, 0.08D, 0.0D);
        sl.sendParticles(ParticleTypes.ASH, chest.x, chest.y + 0.02D, chest.z, barragePrep ? 2 : 1, 0.12D, 0.08D, 0.12D, 0.0D);
    }

    private static Vec3 horizontalSideVector(TideSourcerEntity entity) {
        Vec3 look = entity.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            look = look.normalize();
        }
        return new Vec3(-look.z, 0.0D, look.x);
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
