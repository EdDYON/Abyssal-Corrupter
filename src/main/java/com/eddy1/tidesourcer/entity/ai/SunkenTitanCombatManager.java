package com.eddy1.tidesourcer.entity.ai;

import com.eddy1.tidesourcer.entity.ai.module.basic.*;
import com.eddy1.tidesourcer.entity.ai.module.movement.SunkenTitanDash;
import com.eddy1.tidesourcer.entity.ai.module.epic.*;
import com.eddy1.tidesourcer.entity.ai.module.terrain.*;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.server.level.ServerLevel;

public class SunkenTitanCombatManager {

    public static void handleCooldowns(TideSourcerEntity boss) {
        if (boss.cdGlobalEpic > 0) boss.cdGlobalEpic--;
        if (boss.cdSlash > 0) boss.cdSlash--;
        if (boss.cdCombo > 0) boss.cdCombo--;
        if (boss.cdWave > 0) boss.cdWave--;
        if (boss.cdLeap > 0) boss.cdLeap--;
        if (boss.cdEruption > 0) boss.cdEruption--;
        if (boss.cdSkyfall > 0) boss.cdSkyfall--;
        if (boss.cdVortex > 0) boss.cdVortex--;
        if (boss.cdRepulsion > 0) boss.cdRepulsion--;
        if (boss.cdPrison > 0) boss.cdPrison--;
        if (boss.cdHook > 0) boss.cdHook--;
        if (boss.cdBreath > 0) boss.cdBreath--;
        if (boss.cdRay > 0) boss.cdRay--;
        if (boss.cdDash > 0) boss.cdDash--;
        if (boss.cdTaunt > 0) boss.cdTaunt--;

        if (boss.cdDomain > 0) boss.cdDomain--;
        if (boss.cdMirage > 0) boss.cdMirage--;
        if (boss.cdArmory > 0) boss.cdArmory--;
        if (boss.cdNova > 0) boss.cdNova--;
        if (boss.cdSingularity > 0) boss.cdSingularity--;
    }

    public static void handleAttacks(TideSourcerEntity boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;

        switch (boss.attackState) {
            case 1 -> SunkenTitanAttack1.handle(boss, sl);
            case 2 -> SunkenTitanAttack2.handle(boss, sl);
            case 3 -> SunkenTitanAttack3.handle(boss, sl);
            case 4 -> SunkenTitanAttack4.handle(boss, sl);

            case 5 -> SunkenTitanDash.handle(boss, sl);

            case 6 -> EchoingPulseDomain.handle(boss, sl);
            case 7 -> MirageExecution.handle(boss, sl);
            case 8 -> ArmoryOfTrench.handle(boss, sl);
            case 9 -> CoralTomb.handle(boss, sl);
            case 10 -> AbyssalTrench.handle(boss, sl);
            case 11 -> AbyssalTaunt.handle(boss, sl);
        }
    }
}
