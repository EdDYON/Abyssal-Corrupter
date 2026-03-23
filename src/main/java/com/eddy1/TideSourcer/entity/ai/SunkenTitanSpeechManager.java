package com.eddy1.tidesourcer.entity.ai;

import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SunkenTitanSpeechManager {

    private static final Map<UUID, Long> speechCooldowns = new HashMap<>();

    public static void announceSkill(TideSourcerEntity boss, int state, int variant) {
        if (boss.level().isClientSide() || boss.isClone) return;

        long currentTime = boss.level().getGameTime();
        long lastSpoke = speechCooldowns.getOrDefault(boss.getUUID(), 0L);

        boolean isPhase = state == 100;
        boolean isEpic = state >= 6 && state <= 10;
        boolean isBasic = state >= 1 && state <= 4;

        if (isBasic && (currentTime - lastSpoke < 300 || boss.getRandom().nextFloat() > 0.3F)) return;
        if (isEpic && currentTime - lastSpoke < 100) return;

        String translationKey = "";
        int maxLines = 1;

        if (isBasic) {
            translationKey = "chat.abyssal_corrupter.skill.basic";
            maxLines = 4;
        } else if (state == 6) {
            translationKey = "chat.abyssal_corrupter.skill.echo_pulse";
            maxLines = 3;
        } else if (state == 7) {
            translationKey = "chat.abyssal_corrupter.skill.mirage";
            maxLines = 3;
        } else if (state == 8) {
            translationKey = "chat.abyssal_corrupter.skill.armory";
            maxLines = 3;
        } else if (state == 9) {
            translationKey = "chat.abyssal_corrupter.skill.coral";
            maxLines = 3;
        } else if (state == 10) {
            translationKey = "chat.abyssal_corrupter.skill.trench";
            maxLines = 3;
        } else if (state == 100) {
            translationKey = "chat.abyssal_corrupter.skill.phase_transition";
            maxLines = 4;
        }

        if (!translationKey.isEmpty()) {
            int lineIndex = boss.getRandom().nextInt(maxLines) + 1;
            Component message = Component.translatable(translationKey + "." + lineIndex);

            for (ServerPlayer player : boss.bossEvent.getPlayers()) {
                player.sendSystemMessage(message);
            }

            speechCooldowns.put(boss.getUUID(), currentTime);
        }
    }
}
