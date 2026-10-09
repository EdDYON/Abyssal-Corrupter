package com.eddy1.tidesourcer.entity.ai.module.movement;

import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class SunkenTitanDash {
    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.attackTick == 1) {
            boss.playSound(SoundEvents.PHANTOM_SWOOP, 1.2F, 1.2F);
            LivingEntity target = boss.getTarget();
            Vec3 dashVec = chooseDashVector(boss, target, sl);
            boss.setDeltaMovement(dashVec);
            boss.hasImpulse = true;
        }

        if (boss.attackTick >= 1 && boss.attackTick <= 8) {
            AbyssalEffects.spawnInfectionCloud(sl, boss.position().add(0, 1.0, 0), 0.3, 0.5);
            AbyssalEffects.spawnFearBurst(sl, boss.position().add(0, 0.2, 0), 0.5, 0.2);
        }

        if (boss.attackTick >= 10) {
            boss.resetAttack();
        }
    }

    private static Vec3 chooseDashVector(TideSourcerEntity boss, LivingEntity target, ServerLevel sl) {
        if (target == null) {
            Vec3 fallback = boss.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            if (fallback.lengthSqr() < 1.0E-4D) {
                fallback = new Vec3(1.0D, 0.0D, 0.0D);
            }
            return fallback.normalize().scale(1.5D).add(0.0D, 0.2D, 0.0D);
        }

        Vec3 toTarget = target.position().subtract(boss.position()).multiply(1.0D, 0.0D, 1.0D);
        if (toTarget.lengthSqr() < 1.0E-4D) {
            toTarget = boss.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (toTarget.lengthSqr() < 1.0E-4D) {
            toTarget = new Vec3(1.0D, 0.0D, 0.0D);
        }

        Vec3 forward = toTarget.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double sideBias = sl.random.nextBoolean() ? 1.0D : -1.0D;
        boolean disengage = boss.distanceToSqr(target) < 20.25D;

        Vec3 dash;
        if (disengage) {
            dash = forward.scale(-1.15D).add(side.scale(sideBias * 1.05D));
        } else {
            dash = forward.scale(1.65D).add(side.scale(sideBias * 0.55D));
        }

        return dash.normalize().scale(1.85D).add(0.0D, 0.22D, 0.0D);
    }
}
