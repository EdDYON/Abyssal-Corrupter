package com.eddy1.tidesourcer.entity.ai.module.epic;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.ai.module.SkillCastHelper;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class EchoingPulseDomain {
    private static final int DOMAIN_RADIUS = 20;
    private static final int WALL_RADIUS = 22;
    private static final int WALL_HEIGHT = 9;
    private static final int CAST_TICKS = 30;
    private static final int ACTIVE_TICKS = 900;
    private static final int FLASH_TICKS = 10;
    private static final int CONFUSION_TICKS = 12;
    private static final DustParticleOptions PLAYER_RIPPLE = new DustParticleOptions(new Vector3f(0.24F, 0.82F, 1.0F), 1.1F);
    private static final DustParticleOptions BOSS_RIPPLE = new DustParticleOptions(new Vector3f(1.0F, 0.22F, 0.22F), 1.1F);
    private static final DustParticleOptions HORN_GLOW = new DustParticleOptions(new Vector3f(1.0F, 0.18F, 0.18F), 0.9F);

    public static void handle(TideSourcerEntity boss, ServerLevel sl) {
        LivingEntity target = boss.getTarget();
        if (!SkillCastHelper.snapCasterToGround(boss, sl, 3.5D)) {
            boss.resetAttack();
            return;
        }

        if (boss.attackTick == 1) {
            boss.echoDomainCenter = chooseDomainCenter(boss, target, sl);
            boss.echoDomainConfusionTick = 0;
            placeCombatantsInsideDomain(boss, target, sl);
            boss.playSound(SoundEvents.WARDEN_ROAR, 3.2F, 0.7F);
            boss.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.2F, 0.75F);
            applyDarkFlash(boss, sl);
            if (AbyssalConfig.COMMON.allowTerrainChanges.get()) {
                spreadCatalystFloor(boss, sl);
            }
        }

        if (boss.echoDomainCenter == null) {
            boss.echoDomainCenter = chooseDomainCenter(boss, target, sl);
        }

        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
        boss.hasImpulse = true;
        if (AbyssalConfig.COMMON.allowTerrainChanges.get()) {
            raiseArenaWalls(boss, sl, Math.min(WALL_HEIGHT, Math.max(1, boss.attackTick / 2)));
        }
        renderCastingField(boss, sl);

        if (boss.attackTick >= 8) {
            boss.setInvisible(true);
        }

        if (boss.attackTick >= CAST_TICKS) {
            boss.echoDomainActiveTick = ACTIVE_TICKS;
            boss.setInvisible(true);
            boss.resetAttack();
        }
    }

    public static void tickActiveDomain(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.echoDomainCenter == null) {
            collapseDomain(boss, sl);
            return;
        }
        if (boss.echoDomainActiveTick == 0) {
            collapseDomain(boss, sl);
            return;
        }

        boss.setInvisible(true);
        renderDomainAmbience(boss, sl);
        emitPlayerRipples(boss, sl);
        emitBossRipples(boss, sl);
        emitHornGlow(boss, sl);

        if (boss.tickCount % 20 == 0) {
            AbyssalEffects.play(sl, boss.echoDomainCenter, SoundEvents.WARDEN_HEARTBEAT, 3.0F, 0.82F);
        }
    }

    private static Vec3 chooseDomainCenter(TideSourcerEntity boss, LivingEntity target, ServerLevel sl) {
        Vec3 anchor = boss.position();
        if (target != null && target.isAlive()) {
            anchor = boss.position().lerp(target.position(), 0.5D);
        }
        BlockPos anchorPos = BlockPos.containing(anchor);
        int floorY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, anchorPos.getX(), anchorPos.getZ()) - 1;
        return new Vec3(anchorPos.getX() + 0.5D, floorY + 1.0D, anchorPos.getZ() + 0.5D);
    }

    private static void placeCombatantsInsideDomain(TideSourcerEntity boss, LivingEntity target, ServerLevel sl) {
        Vec3 center = boss.echoDomainCenter;
        if (center == null) {
            return;
        }

        Vec3 bossBias = target != null && target.isAlive()
                ? boss.position().subtract(target.position())
                : boss.getLookAngle();
        Vec3 targetBias = target != null && target.isAlive()
                ? target.position().subtract(boss.position())
                : bossBias.scale(-1.0D);

        moveEntityInsideDomain(boss, center, bossBias, sl, DOMAIN_RADIUS - 5.0D);
        boss.getNavigation().stop();

        if (target != null && target.isAlive()) {
            moveEntityInsideDomain(target, center, targetBias, sl, DOMAIN_RADIUS - 5.0D);
        }
    }

    private static void moveEntityInsideDomain(LivingEntity entity, Vec3 center, Vec3 bias, ServerLevel sl, double maxRadius) {
        Vec3 horizontal = new Vec3(entity.getX() - center.x, 0.0D, entity.getZ() - center.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(bias.x, 0.0D, bias.z);
        }
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }

        double currentRadius = Math.sqrt(horizontal.x * horizontal.x + horizontal.z * horizontal.z);
        Vec3 direction = horizontal.normalize();
        double clampedRadius = Math.min(currentRadius, maxRadius);
        Vec3 destination = center.add(direction.scale(clampedRadius));

        int floorY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(destination.x), Mth.floor(destination.z)) - 1;
        entity.teleportTo(destination.x, floorY + 1.0D, destination.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hasImpulse = true;
        entity.hurtMarked = true;
    }

    private static void applyDarkFlash(TideSourcerEntity boss, ServerLevel sl) {
        Vec3 center = boss.echoDomainCenter;
        if (center == null) {
            return;
        }

        AABB arena = createArenaBounds(center, DOMAIN_RADIUS + 3.0D, 8.0D);
        List<Player> players = sl.getEntitiesOfClass(Player.class, arena, LivingEntity::isAlive);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, FLASH_TICKS, 0, false, false, false));
        }

        sl.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 1.0D, center.z, 6, 1.8, 0.4, 1.8, 0.01);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.1D, center.z, 7, 2.2, 0.1, 2.2, 0.04);
    }

    private static void spreadCatalystFloor(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.echoDomainCenter == null) {
            return;
        }

        int centerX = BlockPos.containing(boss.echoDomainCenter).getX();
        int centerZ = BlockPos.containing(boss.echoDomainCenter).getZ();

        for (int x = centerX - DOMAIN_RADIUS; x <= centerX + DOMAIN_RADIUS; x++) {
            for (int z = centerZ - DOMAIN_RADIUS; z <= centerZ + DOMAIN_RADIUS; z++) {
                double dx = x - boss.echoDomainCenter.x;
                double dz = z - boss.echoDomainCenter.z;
                if (dx * dx + dz * dz > DOMAIN_RADIUS * DOMAIN_RADIUS) {
                    continue;
                }

                int surfaceY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos floorPos = new BlockPos(x, surfaceY, z);
                BlockState currentState = sl.getBlockState(floorPos);

                if (currentState.isAir() || !currentState.getFluidState().isEmpty() || currentState.hasBlockEntity()) {
                    continue;
                }
                if (currentState.is(Blocks.BEDROCK) || currentState.is(Blocks.SCULK_CATALYST)) {
                    continue;
                }

                replaceTrackedBlock(boss.savedEchoDomainBlocks, sl, floorPos, Blocks.SCULK_CATALYST.defaultBlockState());
            }
        }
    }

    private static void raiseArenaWalls(TideSourcerEntity boss, ServerLevel sl, int currentHeight) {
        if (boss.echoDomainCenter == null) {
            return;
        }

        int centerX = BlockPos.containing(boss.echoDomainCenter).getX();
        int centerY = BlockPos.containing(boss.echoDomainCenter).getY() - 1;
        int centerZ = BlockPos.containing(boss.echoDomainCenter).getZ();
        double innerRadiusSq = (WALL_RADIUS - 1.5D) * (WALL_RADIUS - 1.5D);
        double outerRadiusSq = (WALL_RADIUS + 0.5D) * (WALL_RADIUS + 0.5D);

        for (int x = centerX - WALL_RADIUS - 1; x <= centerX + WALL_RADIUS + 1; x++) {
            for (int z = centerZ - WALL_RADIUS - 1; z <= centerZ + WALL_RADIUS + 1; z++) {
                double dx = x - boss.echoDomainCenter.x;
                double dz = z - boss.echoDomainCenter.z;
                double distanceSq = dx * dx + dz * dz;
                if (distanceSq < innerRadiusSq || distanceSq > outerRadiusSq) {
                    continue;
                }

                int surfaceY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                int baseY = Math.max(centerY - 1, surfaceY);

                for (int yOffset = 1; yOffset <= currentHeight; yOffset++) {
                    BlockPos wallPos = new BlockPos(x, baseY + yOffset, z);
                    BlockState currentState = sl.getBlockState(wallPos);
                    if (!currentState.isAir() && !currentState.getCollisionShape(sl, wallPos).isEmpty() && !currentState.is(Blocks.REINFORCED_DEEPSLATE)) {
                        continue;
                    }
                    replaceTrackedBlock(boss.savedEchoDomainBlocks, sl, wallPos, Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
                }

                if (boss.attackTick % 4 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL, x + 0.5D, baseY + currentHeight + 0.2D, z + 0.5D, 1, 0.06, 0.06, 0.06, 0.0);
                }
            }
        }
    }

    private static void renderCastingField(TideSourcerEntity boss, ServerLevel sl) {
        Vec3 center = boss.echoDomainCenter;
        if (center == null) {
            return;
        }

        AbyssalEffects.spawnCharge(sl, boss.position().add(0.0D, 2.1D, 0.0D), 0.35D, 0.55D);
        if (boss.attackTick % 5 == 0) {
            double radius = 4.0D + boss.attackTick * 0.45D;
            spawnRipple(sl, center.add(0.0D, 0.08D, 0.0D), PLAYER_RIPPLE, Math.min(radius, DOMAIN_RADIUS - 1.0D), 14);
        }
    }

    private static void renderDomainAmbience(TideSourcerEntity boss, ServerLevel sl) {
        Vec3 center = boss.echoDomainCenter;
        if (center == null) {
            return;
        }

        if (boss.tickCount % 8 == 0) {
            double angle = sl.random.nextDouble() * Math.PI * 2.0D;
            double radius = 5.0D + sl.random.nextDouble() * (DOMAIN_RADIUS - 5.0D);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            int floorX = BlockPos.containing(x, center.y, z).getX();
            int floorZ = BlockPos.containing(x, center.y, z).getZ();
            int floorY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, floorX, floorZ) - 1;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, floorY + 1.05D, z, 1, 0.08, 0.02, 0.08, 0.0);
            sl.sendParticles(ParticleTypes.SOUL, x, floorY + 1.05D, z, 1, 0.08, 0.02, 0.08, 0.0);
        }
    }

    private static void emitPlayerRipples(TideSourcerEntity boss, ServerLevel sl) {
        Vec3 center = boss.echoDomainCenter;
        if (center == null) {
            return;
        }

        AABB arena = createArenaBounds(center, DOMAIN_RADIUS + 2.0D, 8.0D);
        List<Player> players = sl.getEntitiesOfClass(Player.class, arena, LivingEntity::isAlive);

        for (Player player : players) {
            if (!isInsideArena(center, player.position(), DOMAIN_RADIUS + 1.0D)) {
                continue;
            }

            if (player.isCrouching()) {
                boss.echoDomainConfusionTick = Math.max(boss.echoDomainConfusionTick, CONFUSION_TICKS);
                continue;
            }

            boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.0025D;
            boolean sprinting = player.isSprinting() && moving;
            boolean attacking = player.getAttackAnim(0.0F) > 0.05F;
            if (!moving && !attacking) {
                continue;
            }

            int interval = sprinting || attacking ? 3 : 5;
            if (boss.tickCount % interval != 0) {
                continue;
            }

            double radius = sprinting ? 1.95D : attacking ? 1.6D : 1.2D;
            int points = sprinting ? 14 : 10;
            spawnRipple(sl, player.position().add(0.0D, 0.05D, 0.0D), PLAYER_RIPPLE, radius, points);
        }
    }

    private static void emitBossRipples(TideSourcerEntity boss, ServerLevel sl) {
        double horizontalSpeed = boss.getDeltaMovement().horizontalDistanceSqr();
        if (boss.echoDomainConfusionTick > 0) {
            if (boss.tickCount % 10 == 0) {
                spawnRipple(sl, boss.position().add(0.0D, 0.05D, 0.0D), BOSS_RIPPLE, 0.9D, 6);
            }
            return;
        }

        if (horizontalSpeed <= 0.002D && boss.attackState == 0) {
            return;
        }
        if (boss.tickCount % 4 != 0) {
            return;
        }

        double radius = boss.attackState == 0 ? 1.35D : 1.8D;
        int points = boss.attackState == 0 ? 8 : 12;
        spawnRipple(sl, boss.position().add(0.0D, 0.05D, 0.0D), BOSS_RIPPLE, radius, points);
    }

    private static void emitHornGlow(TideSourcerEntity boss, ServerLevel sl) {
        Vec3 forward = boss.getLookAngle();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(0.35D, 0.0D, 0.0D);
        } else {
            side = side.normalize().scale(0.35D);
        }

        Vec3 headPos = boss.position().add(0.0D, boss.getBbHeight() * 0.92D, 0.0D);
        Vec3 leftHorn = headPos.add(side).add(0.0D, 0.18D, 0.0D);
        Vec3 rightHorn = headPos.subtract(side).add(0.0D, 0.18D, 0.0D);

        sl.sendParticles(HORN_GLOW, leftHorn.x, leftHorn.y, leftHorn.z, 1, 0.04, 0.04, 0.04, 0.0);
        sl.sendParticles(HORN_GLOW, rightHorn.x, rightHorn.y, rightHorn.z, 1, 0.04, 0.04, 0.04, 0.0);
        if (boss.tickCount % 6 == 0) {
            sl.sendParticles(ParticleTypes.SMOKE, leftHorn.x, leftHorn.y, leftHorn.z, 1, 0.03, 0.04, 0.03, 0.0);
            sl.sendParticles(ParticleTypes.SMOKE, rightHorn.x, rightHorn.y, rightHorn.z, 1, 0.03, 0.04, 0.03, 0.0);
        }
    }

    private static void spawnRipple(ServerLevel sl, Vec3 center, DustParticleOptions particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            sl.sendParticles(particle, x, center.y, z, 1, 0.02, 0.0, 0.02, 0.0);
        }
    }

    private static boolean isInsideArena(Vec3 center, Vec3 pos, double radius) {
        double dx = pos.x - center.x;
        double dz = pos.z - center.z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static AABB createArenaBounds(Vec3 center, double radius, double height) {
        return new AABB(center.x - radius, center.y - 2.0D, center.z - radius, center.x + radius, center.y + height, center.z + radius);
    }

    private static void replaceTrackedBlock(Map<BlockPos, BlockState> savedBlocks, ServerLevel sl, BlockPos pos, BlockState newState) {
        if (!AbyssalConfig.COMMON.allowTerrainChanges.get()) {
            return;
        }
        BlockPos immutablePos = pos.immutable();
        savedBlocks.putIfAbsent(immutablePos, sl.getBlockState(immutablePos));
        sl.setBlock(immutablePos, newState, 3);
    }

    private static void collapseDomain(TideSourcerEntity boss, ServerLevel sl) {
        if (boss.echoDomainCenter != null) {
            AbyssalEffects.play(sl, boss.echoDomainCenter, SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.5F, 0.85F);
            AbyssalEffects.spawnImpact(sl, boss.echoDomainCenter.add(0.0D, 1.0D, 0.0D), 1.2D, 0.8D);
        }

        for (Map.Entry<BlockPos, BlockState> entry : boss.savedEchoDomainBlocks.entrySet()) {
            sl.setBlock(entry.getKey(), entry.getValue(), 3);
        }
        boss.savedEchoDomainBlocks.clear();
        boss.echoDomainActiveTick = 0;
        boss.echoDomainConfusionTick = 0;
        boss.echoDomainCenter = null;
        boss.setInvisible(false);
    }
}
