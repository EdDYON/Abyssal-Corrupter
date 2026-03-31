package com.eddy1.tidesourcer.client.gui;

import com.eddy1.tidesourcer.TideSourcerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

public final class AbyssalBossHud {
    private static final String BOSS_NAME_KEY = "entity.abyssal_corrupter.abyssal_corrupter";
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(TideSourcerMod.MOD_ID, "textures/gui/boss_bar_background.png");
    private static final ResourceLocation CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath(TideSourcerMod.MOD_ID, "textures/gui/boss_bar_core.png");
    private static final ResourceLocation PROGRESS_TEXTURE = ResourceLocation.fromNamespaceAndPath(TideSourcerMod.MOD_ID, "textures/gui/boss_bar_progress.png");
    private static final ResourceLocation FLOW_TEXTURE = ResourceLocation.fromNamespaceAndPath(TideSourcerMod.MOD_ID, "textures/gui/boss_bar_flow.png");

    private static final int FRAME_WIDTH = 214;
    private static final int FRAME_HEIGHT = 24;
    private static final int BAR_X_OFFSET = 16;
    private static final int BAR_Y_OFFSET = 15;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int CORE_X_OFFSET = 97;
    private static final int CORE_Y_OFFSET = 1;
    private static final int CORE_WIDTH = 20;
    private static final int CORE_HEIGHT = 12;
    private static final int FLOW_TILE_WIDTH = 48;
    private static final int TITLE_COLOR = 0xD9E2DE;
    private static final int TITLE_SHADOW = 0x0B1014;

    private AbyssalBossHud() {
    }

    public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent bossEvent = event.getBossEvent();
        if (!isAbyssalBoss(bossEvent)) {
            return;
        }

        event.setCanceled(true);
        event.setIncrement(20);

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int maxX = Math.max(4, guiGraphics.guiWidth() - FRAME_WIDTH - 4);
        int frameX = Mth.clamp(event.getX() - (FRAME_WIDTH - BAR_WIDTH) / 2, 4, maxX);
        int frameY = Math.max(0, event.getY() - BAR_Y_OFFSET);
        renderBar(guiGraphics, bossEvent, frameX, frameY, event.getY(), getAnimationTime(event));
    }

    private static void renderBar(GuiGraphics guiGraphics, LerpingBossEvent bossEvent, int frameX, int frameY, int vanillaBarY, float animTime) {
        int barX = frameX + BAR_X_OFFSET;
        int barY = frameY + BAR_Y_OFFSET;
        float progress = Mth.clamp(bossEvent.getProgress(), 0.0F, 1.0F);
        int progressWidth = Mth.floor(progress * BAR_WIDTH);
        float danger = 1.0F - progress;
        float pulse = 0.52F + 0.48F * (float)Math.sin(animTime * (0.11F + danger * 0.05F));
        int coreX = frameX + CORE_X_OFFSET;
        int coreY = frameY + CORE_Y_OFFSET + Mth.floor((float)Math.sin(animTime * 0.07F) * (0.7F + danger * 0.35F));

        guiGraphics.blit(BACKGROUND_TEXTURE, frameX, frameY, 0.0F, 0.0F, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
        renderBarBreath(guiGraphics, barX, barY, pulse, danger);

        if (progressWidth > 0) {
            guiGraphics.blit(PROGRESS_TEXTURE, barX, barY, progressWidth, BAR_HEIGHT, 0.0F, 0.0F, progressWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
            renderFlow(guiGraphics, barX, barY, progressWidth, animTime, danger);
            renderCorruptionTint(guiGraphics, barX, barY, progressWidth, pulse, danger);
            renderFrontEdge(guiGraphics, barX + progressWidth - 1, barY, pulse, danger);
        }

        renderCoreAura(guiGraphics, coreX + CORE_WIDTH / 2, coreY + CORE_HEIGHT / 2, pulse, danger);
        guiGraphics.blit(CORE_TEXTURE, coreX, coreY, 0.0F, 0.0F, CORE_WIDTH, CORE_HEIGHT, CORE_WIDTH, CORE_HEIGHT);

        int titleX = frameX + FRAME_WIDTH / 2;
        int titleY = Math.max(1, vanillaBarY - 9);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, bossEvent.getName(), titleX, titleY + 1, TITLE_SHADOW);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, bossEvent.getName(), titleX, titleY, TITLE_COLOR);
    }

    private static void renderBarBreath(GuiGraphics guiGraphics, int barX, int barY, float pulse, float danger) {
        int topLine = argb(8 + Mth.floor(10 * pulse), 60, 112, 104);
        int topAccent = argb(16 + Mth.floor(18 * pulse), 104, 162, 150);
        int bottomLine = argb(10 + Mth.floor(14 * danger), 82, 34, 56);

        guiGraphics.fill(barX + 12, barY - 2, barX + BAR_WIDTH - 12, barY - 1, topLine);
        guiGraphics.fill(barX + 30, barY - 3, barX + 42, barY - 2, topAccent);
        guiGraphics.fill(barX + BAR_WIDTH - 42, barY - 3, barX + BAR_WIDTH - 30, barY - 2, topAccent);
        guiGraphics.fill(barX + 24, barY + BAR_HEIGHT + 1, barX + BAR_WIDTH - 24, barY + BAR_HEIGHT + 2, bottomLine);
    }

    private static void renderFlow(GuiGraphics guiGraphics, int barX, int barY, int progressWidth, float animTime, float danger) {
        int scroll = Mth.floor(animTime * (0.85F + danger * 1.2F)) % FLOW_TILE_WIDTH;
        guiGraphics.enableScissor(barX, barY, barX + progressWidth, barY + BAR_HEIGHT);
        guiGraphics.setColor(0.70F, 0.84F, 0.81F, 0.18F + 0.10F * danger);
        for (int drawX = barX - scroll; drawX < barX + progressWidth; drawX += FLOW_TILE_WIDTH) {
            guiGraphics.blit(FLOW_TEXTURE, drawX, barY, FLOW_TILE_WIDTH, BAR_HEIGHT, 0.0F, 0.0F, FLOW_TILE_WIDTH, BAR_HEIGHT, FLOW_TILE_WIDTH, BAR_HEIGHT);
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.disableScissor();
    }

    private static void renderCorruptionTint(GuiGraphics guiGraphics, int barX, int barY, int progressWidth, float pulse, float danger) {
        if (progressWidth <= 2) {
            return;
        }

        int upperColor = argb(10 + Mth.floor(12 * pulse), lerp(38, 66, danger), lerp(102, 72, danger), lerp(94, 82, danger));
        int lowerColor = argb(14 + Mth.floor(18 * danger), lerp(52, 104, danger), lerp(20, 24, danger), lerp(34, 56, danger));
        guiGraphics.fill(barX + 1, barY + 1, barX + progressWidth - 1, barY + 2, upperColor);
        guiGraphics.fill(barX + 1, barY + BAR_HEIGHT - 1, barX + progressWidth - 1, barY + BAR_HEIGHT, lowerColor);

        int corruption = argb(18 + Mth.floor(18 * danger), 164, 72, 92);
        for (int x = barX + 6; x < barX + progressWidth - 2; x += 24) {
            guiGraphics.fill(x, barY + 2, x + 1, barY + 3, corruption);
        }
    }

    private static void renderFrontEdge(GuiGraphics guiGraphics, int edgeX, int barY, float pulse, float danger) {
        int outer = argb(
            34 + Mth.floor(30 * pulse),
            lerp(108, 184, danger),
            lerp(146, 86, danger),
            lerp(138, 94, danger)
        );
        int inner = argb(
            52 + Mth.floor(34 * pulse),
            lerp(170, 228, danger),
            lerp(188, 124, danger),
            lerp(178, 138, danger)
        );
        guiGraphics.fill(edgeX - 1, barY - 1, edgeX + 2, barY + BAR_HEIGHT + 1, outer);
        guiGraphics.fill(edgeX, barY, edgeX + 1, barY + BAR_HEIGHT, inner);
    }

    private static void renderCoreAura(GuiGraphics guiGraphics, int centerX, int centerY, float pulse, float danger) {
        int outer = argb(10 + Mth.floor(12 * pulse), lerp(32, 88, danger), lerp(56, 48, danger), lerp(70, 74, danger));
        int inner = argb(18 + Mth.floor(16 * pulse), lerp(86, 154, danger), lerp(92, 78, danger), lerp(96, 106, danger));
        guiGraphics.fill(centerX - 6, centerY - 2, centerX + 6, centerY + 3, outer);
        guiGraphics.fill(centerX - 3, centerY - 1, centerX + 3, centerY + 2, inner);
    }

    private static float getAnimationTime(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (minecraft.player != null) {
            return minecraft.player.tickCount + partialTick;
        }
        if (minecraft.level != null) {
            return minecraft.level.getGameTime() + partialTick;
        }
        return partialTick;
    }

    private static int lerp(int from, int to, float delta) {
        return Mth.floor(from + (to - from) * Mth.clamp(delta, 0.0F, 1.0F));
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (Mth.clamp(alpha, 0, 255) << 24)
            | (Mth.clamp(red, 0, 255) << 16)
            | (Mth.clamp(green, 0, 255) << 8)
            | Mth.clamp(blue, 0, 255);
    }

    private static boolean isAbyssalBoss(LerpingBossEvent bossEvent) {
        if (bossEvent.getName().getContents() instanceof TranslatableContents translatable) {
            return BOSS_NAME_KEY.equals(translatable.getKey());
        }

        return bossEvent.getName().getString().equals(Component.translatable(BOSS_NAME_KEY).getString());
    }
}
