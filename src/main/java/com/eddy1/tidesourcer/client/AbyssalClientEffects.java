package com.eddy1.tidesourcer.client;

import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class AbyssalClientEffects {
    private AbyssalClientEffects() {
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!AbyssalConfig.CLIENT.screenShakeEnabled.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        TideSourcerEntity closestBoss = null;
        double closestDistanceSqr = Double.MAX_VALUE;
        AABB searchArea = minecraft.player.getBoundingBox().inflate(72.0D);
        for (TideSourcerEntity boss : minecraft.level.getEntitiesOfClass(TideSourcerEntity.class, searchArea, entity -> !entity.isClone && entity.isAlive())) {
            double distanceSqr = boss.distanceToSqr(minecraft.player);
            if (distanceSqr < closestDistanceSqr) {
                closestDistanceSqr = distanceSqr;
                closestBoss = boss;
            }
        }
        if (closestBoss == null) {
            return;
        }

        float intensity = getShakeIntensity(closestBoss);
        if (intensity <= 0.0F) {
            return;
        }

        float distanceFactor = (float) Mth.clamp(1.0D - Math.sqrt(closestDistanceSqr) / 72.0D, 0.0D, 1.0D);
        float strength = AbyssalConfig.CLIENT.screenShakeStrength.get().floatValue();
        float time = minecraft.player.tickCount + (float) event.getPartialTick();
        float shake = intensity * distanceFactor * strength;

        event.setPitch(event.getPitch() + Mth.sin(time * 1.73F) * shake * 0.55F);
        event.setYaw(event.getYaw() + Mth.sin(time * 1.21F + 1.7F) * shake * 0.35F);
        event.setRoll(event.getRoll() + Mth.sin(time * 2.07F + 0.4F) * shake * 0.8F);
    }

    private static float getShakeIntensity(TideSourcerEntity boss) {
        int entranceTicks = boss.getSyncedEntranceTicks();
        if (entranceTicks > 0) {
            return 1.1F;
        }

        int state = boss.getSyncedAttackState();
        int tick = boss.getSyncedAttackTick();
        float phaseBonus = boss.isSyncedPhaseTwo() ? 0.08F : 0.0F;

        return switch (state) {
            case 2 -> isNear(tick, 35, 6) || isNear(tick, 50, 8) ? 1.25F + phaseBonus : 0.18F;
            case 3 -> isNear(tick, 20, 7) || isNear(tick, 75, 8) ? 0.9F + phaseBonus : 0.12F;
            case 4 -> tick >= 20 && tick <= 80 ? 0.24F + phaseBonus : 0.08F;
            case 5 -> tick <= 8 ? 0.45F + phaseBonus : 0.0F;
            case 6, 9, 10 -> 0.35F + phaseBonus;
            case 100 -> 0.75F;
            default -> phaseBonus * 0.35F;
        };
    }

    private static boolean isNear(int value, int target, int window) {
        return Math.abs(value - target) <= window;
    }
}
