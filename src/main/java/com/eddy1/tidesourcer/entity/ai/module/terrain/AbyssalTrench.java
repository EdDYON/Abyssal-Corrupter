package com.eddy1.tidesourcer.entity.ai.module.terrain;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AbyssalTrench {
    private static final int CAST_TICKS = 40;
    private static final int ACTIVE_TICKS = 160;
    private static final double HEART_HEIGHT = 8.0D;
    private static final double PULL_RADIUS = 28.0D;

    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        LivingEntity target = boss.getTarget();

        if (boss.attackTick == 1) {
            boss.singularityPos = chooseSingularityPos(boss, target);
            boss.playSound(SoundEvents.WARDEN_ROAR, 3.0F, 0.6F);
            boss.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.8F, 0.55F);
        }

        if (boss.singularityPos == null) {
            boss.singularityPos = chooseSingularityPos(boss, target);
        }

        if (target != null && target.isAlive() && boss.attackTick < CAST_TICKS) {
            Vec3 desired = target.position().add(0.0D, HEART_HEIGHT, 0.0D);
            boss.singularityPos = boss.singularityPos.lerp(desired, 0.08D);
        }

        boss.setDeltaMovement(Vec3.ZERO);
        boss.hasImpulse = true;

        if (boss.attackTick % 5 == 0) {
            AbyssalEffects.spawnControlMist(sl, boss.singularityPos, 1.2D, 0.8D);
        }
        renderChargingRift(boss, sl);

        if (boss.attackTick == CAST_TICKS) {
            AbyssalEffects.play(sl, boss.singularityPos, SoundEvents.WARDEN_HEARTBEAT, 3.4F, 0.8F);
            boss.singularityActiveTick = ACTIVE_TICKS;
            boss.resetAttack();
        }
    }

    public static void tickActiveTrench(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.singularityPos == null) {
            boss.singularityActiveTick = 0;
            return;
        }

        if (boss.singularityActiveTick == 0) {
            collapseSingularity(boss, sl);
            return;
        }

        int age = ACTIVE_TICKS - boss.singularityActiveTick;
        boolean heartbeat = boss.singularityActiveTick % 20 == 0;

        renderActiveSingularity(sl, boss.singularityPos, age, heartbeat);
        pullLivingEntities(boss, sl, heartbeat);
        pullWorldEntities(boss, sl);
        bendBossProjectiles(boss, sl);
        spawnDebrisTrails(sl, boss.singularityPos, age);

        if (heartbeat) {
            AbyssalEffects.play(sl, boss.singularityPos, SoundEvents.WARDEN_HEARTBEAT, 3.6F, 0.78F);
        }
    }

    private static Vec3 chooseSingularityPos(TideSourcerEntity boss, LivingEntity target) {
        if (target != null && target.isAlive()) {
            return target.position().add(0.0D, HEART_HEIGHT, 0.0D);
        }
        return boss.position().add(boss.getLookAngle().scale(10.0D)).add(0.0D, HEART_HEIGHT, 0.0D);
    }

    private static void renderChargingRift(TideSourcerEntity boss, ServerLevel sl) {
        Vec3 chestPos = boss.position().add(0.0D, 2.4D, 0.0D);
        Vec3 singularityPos = boss.singularityPos;
        Vec3 direction = singularityPos.subtract(chestPos);
        int steps = Math.max(4, (int) (direction.length() / 1.2D));

        AbyssalEffects.spawnBeam(sl, chestPos, direction, steps, 1.0D);
        renderActiveSingularity(sl, singularityPos, boss.attackTick, false);
    }

    private static void renderActiveSingularity(ServerLevel sl, Vec3 corePos, int age, boolean heartbeat) {
        double pulse = 1.0D + Math.sin(age * 0.35D) * 0.18D;
        double ringRadius = heartbeat ? 3.2D : 2.5D + pulse;

        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, corePos.x, corePos.y, corePos.z, heartbeat ? 6 : 3, 0.45, 0.45, 0.45, 0.02);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, corePos.x, corePos.y, corePos.z, heartbeat ? 6 : 4, 0.35, 0.35, 0.35, 0.01);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, corePos.x, corePos.y, corePos.z, heartbeat ? 5 : 3, 0.25, 0.25, 0.25, 0.01);

        for (int i = 0; i < 10; i++) {
            double angle = ((Math.PI * 2.0D) / 10.0D) * i + age * 0.08D;
            double x = corePos.x + Math.cos(angle) * ringRadius;
            double z = corePos.z + Math.sin(angle) * ringRadius;
            double y = corePos.y + Math.sin(angle * 2.0D) * 0.4D;
            sl.sendParticles(ParticleTypes.SOUL, x, y, z, 1, 0.03, 0.03, 0.03, 0.0);
            if (heartbeat && i % 2 == 0) {
                sl.sendParticles(ParticleTypes.ASH, x, y, z, 1, 0.05, 0.02, 0.05, 0.0);
            }
        }
    }

    private static void pullLivingEntities(TideSourcerEntity boss, ServerLevel sl, boolean heartbeat) {
        AABB area = new AABB(
                boss.singularityPos.x - PULL_RADIUS,
                boss.singularityPos.y - 14.0D,
                boss.singularityPos.z - PULL_RADIUS,
                boss.singularityPos.x + PULL_RADIUS,
                boss.singularityPos.y + 14.0D,
                boss.singularityPos.z + PULL_RADIUS
        );

        List<LivingEntity> entities = sl.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != boss && entity.isAlive());
        for (LivingEntity entity : entities) {
            Vec3 towardCore = boss.singularityPos.subtract(entity.position());
            double distance = towardCore.length();
            if (distance < 1.0D || distance > PULL_RADIUS) continue;

            double normalized = 1.0D - (distance / PULL_RADIUS);
            double strength = heartbeat ? 0.24D : 0.045D;
            Vec3 pull = towardCore.normalize().scale(strength * (0.35D + normalized));

            entity.setDeltaMovement(entity.getDeltaMovement().add(pull.x, heartbeat ? 0.02D : 0.0D, pull.z));
            entity.hasImpulse = true;
            entity.hurtMarked = true;

            if (heartbeat && distance < 6.0D) {
                entity.hurt(boss.damageSources().magic(), AbyssalConfig.scaledDamage(10.0F));
                AbyssalEffects.spawnImpact(sl, entity.position().add(0.0D, 1.0D, 0.0D), 0.4, 0.4);
            }
        }
    }

    private static void pullWorldEntities(TideSourcerEntity boss, ServerLevel sl) {
        AABB area = new AABB(
                boss.singularityPos.x - PULL_RADIUS,
                boss.singularityPos.y - 14.0D,
                boss.singularityPos.z - PULL_RADIUS,
                boss.singularityPos.x + PULL_RADIUS,
                boss.singularityPos.y + 14.0D,
                boss.singularityPos.z + PULL_RADIUS
        );

        for (ItemEntity item : sl.getEntitiesOfClass(ItemEntity.class, area, entity -> entity.isAlive())) {
            pullEntityToward(item.position(), item, boss.singularityPos, 0.08D);
        }

        for (ExperienceOrb orb : sl.getEntitiesOfClass(ExperienceOrb.class, area, entity -> entity.isAlive())) {
            pullEntityToward(orb.position(), orb, boss.singularityPos, 0.11D);
        }
    }

    private static void bendBossProjectiles(TideSourcerEntity boss, ServerLevel sl) {
        AABB area = new AABB(
                boss.singularityPos.x - (PULL_RADIUS + 6.0D),
                boss.singularityPos.y - 14.0D,
                boss.singularityPos.z - (PULL_RADIUS + 6.0D),
                boss.singularityPos.x + (PULL_RADIUS + 6.0D),
                boss.singularityPos.y + 14.0D,
                boss.singularityPos.z + (PULL_RADIUS + 6.0D)
        );

        List<Projectile> projectiles = sl.getEntitiesOfClass(Projectile.class, area, projectile -> projectile.isAlive() && projectile.getOwner() == boss);
        for (Projectile projectile : projectiles) {
            Vec3 towardCore = boss.singularityPos.subtract(projectile.position());
            double distance = towardCore.length();
            if (distance < 1.0D || distance > PULL_RADIUS + 6.0D) continue;

            Vec3 curve = towardCore.normalize().scale(0.055D);
            projectile.setDeltaMovement(projectile.getDeltaMovement().add(curve));
            projectile.hasImpulse = true;
        }
    }

    private static void spawnDebrisTrails(ServerLevel sl, Vec3 corePos, int age) {
        if (age % 6 != 0) return;

        for (int i = 0; i < 2; i++) {
            double angle = sl.random.nextDouble() * Math.PI * 2.0D;
            double radius = 8.0D + sl.random.nextDouble() * 6.0D;
            Vec3 samplePos = new Vec3(corePos.x + Math.cos(angle) * radius, corePos.y - 7.0D + sl.random.nextDouble() * 2.0D, corePos.z + Math.sin(angle) * radius);
            BlockPos blockPos = BlockPos.containing(samplePos);
            BlockState state = sl.getBlockState(blockPos);

            if (!state.isAir()) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), samplePos.x, samplePos.y, samplePos.z, 2, 0.1, 0.1, 0.1, 0.01);
                AbyssalEffects.spawnBeam(sl, samplePos, corePos.subtract(samplePos), 5, 0.6D);
            } else {
                sl.sendParticles(ParticleTypes.ASH, samplePos.x, samplePos.y, samplePos.z, 2, 0.08, 0.08, 0.08, 0.01);
            }
        }
    }

    private static void collapseSingularity(TideSourcerEntity boss, ServerLevel sl) {
        AbyssalEffects.play(sl, boss.singularityPos, SoundEvents.GLASS_BREAK, 2.4F, 0.7F);
        AbyssalEffects.spawnImpact(sl, boss.singularityPos, 1.6, 1.6);
        boss.singularityPos = null;
    }

    private static void pullEntityToward(Vec3 entityPos, net.minecraft.world.entity.Entity entity, Vec3 corePos, double strength) {
        Vec3 pull = corePos.subtract(entityPos);
        double distance = pull.length();
        if (distance < 0.5D) return;

        Vec3 motion = pull.normalize().scale(strength);
        entity.setDeltaMovement(entity.getDeltaMovement().add(motion));
        entity.hasImpulse = true;
    }
}
