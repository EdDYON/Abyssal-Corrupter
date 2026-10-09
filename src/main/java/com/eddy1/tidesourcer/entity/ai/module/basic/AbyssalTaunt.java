package com.eddy1.tidesourcer.entity.ai.module.basic;

import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.ai.module.SkillCastHelper;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AbyssalTaunt {
    private static final int TAUNT_TICKS = 28;
    private static final float HEAL_AMOUNT = 60.0F;

    private AbyssalTaunt() {
    }

    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        LivingEntity target = boss.getTarget();
        if (!(target instanceof Player player) || !player.isAlive()) {
            boss.resetAttack();
            return;
        }
        if (!SkillCastHelper.snapCasterToGround(boss, sl, 3.5D)) {
            boss.resetAttack();
            return;
        }

        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
        boss.hasImpulse = true;

        if (boss.attackTick == 1) {
            boss.heal(HEAL_AMOUNT);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, true, true));
            boss.playSound(SoundEvents.WARDEN_ROAR, 2.6F, 0.92F);
            boss.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.8F, 0.76F);
            AbyssalEffects.spawnFearBurst(sl, boss.position().add(0.0D, 1.5D, 0.0D), 0.45D, 0.6D);
        }

        if (boss.attackTick % 6 == 0) {
            AbyssalEffects.spawnCharge(sl, boss.position().add(0.0D, 2.0D, 0.0D), 0.25D, 0.35D);
        }

        if (boss.attackTick >= TAUNT_TICKS) {
            boss.resetAttack();
        }
    }
}
