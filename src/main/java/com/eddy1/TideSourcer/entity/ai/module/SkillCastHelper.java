package com.eddy1.tidesourcer.entity.ai.module;

import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class SkillCastHelper {
    private SkillCastHelper() {
    }

    public static boolean isNearGround(TideSourcerEntity boss, ServerLevel sl, double tolerance) {
        return boss.onGround() || Math.abs(boss.getY() - sampleFeetY(boss, sl)) <= tolerance;
    }

    public static boolean snapCasterToGround(TideSourcerEntity boss, ServerLevel sl, double tolerance) {
        double feetY = sampleFeetY(boss, sl);
        if (Math.abs(boss.getY() - feetY) > tolerance) {
            return false;
        }

        if (Math.abs(boss.getY() - feetY) > 0.05D) {
            boss.teleportTo(boss.getX(), feetY, boss.getZ());
        }

        boss.setDeltaMovement(Vec3.ZERO);
        boss.hasImpulse = true;
        boss.hurtMarked = true;
        return true;
    }

    public static Vec3 groundCenter(TideSourcerEntity boss, ServerLevel sl, double yOffset) {
        return new Vec3(boss.getX(), sampleFeetY(boss, sl) + yOffset, boss.getZ());
    }

    public static Vec3 findStandablePosition(TideSourcerEntity boss, ServerLevel sl, double x, double preferredY, double z, int searchRange) {
        int baseY = Mth.floor(preferredY);
        for (int offset = 0; offset <= searchRange; offset++) {
            Vec3 down = standablePositionAt(boss, sl, x, baseY - offset, z);
            if (down != null) {
                return down;
            }

            if (offset > 0) {
                Vec3 up = standablePositionAt(boss, sl, x, baseY + offset, z);
                if (up != null) {
                    return up;
                }
            }
        }
        return null;
    }

    private static double sampleFeetY(TideSourcerEntity boss, ServerLevel sl) {
        int x = Mth.floor(boss.getX());
        int z = Mth.floor(boss.getZ());
        return sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static Vec3 standablePositionAt(TideSourcerEntity boss, ServerLevel sl, double x, int preferredFeetY, double z) {
        BlockPos supportPos = BlockPos.containing(x, preferredFeetY - 1.0D, z);
        var supportShape = sl.getBlockState(supportPos).getCollisionShape(sl, supportPos);
        if (supportShape.isEmpty()) {
            return null;
        }

        double feetY = supportPos.getY() + supportShape.max(Direction.Axis.Y);
        if (feetY < preferredFeetY - 1.05D || feetY > preferredFeetY + 1.05D) {
            return null;
        }

        Vec3 candidate = new Vec3(x, feetY, z);
        double dx = candidate.x - boss.getX();
        double dy = candidate.y - boss.getY();
        double dz = candidate.z - boss.getZ();
        if (!sl.noCollision(boss, boss.getBoundingBox().move(dx, dy, dz))) {
            return null;
        }

        return candidate;
    }
}
