package com.eddy1.tidesourcer.event;

import com.eddy1.tidesourcer.TideSourcerMod;
import com.eddy1.tidesourcer.client.AbyssalClientEffects;
import com.eddy1.tidesourcer.client.gui.AbyssalBossHud;
import com.eddy1.tidesourcer.client.renderer.TideSourcerRenderer;
import com.eddy1.tidesourcer.command.TideSourcerCommands;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import com.eddy1.tidesourcer.init.EntityInit;
import com.eddy1.tidesourcer.world.AbyssalRitualSite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public class ModEvents {

    @EventBusSubscriber(modid = TideSourcerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            event.put(EntityInit.ABYSSAL_CORRUPTER.get(), TideSourcerEntity.createAttributes().build());
        }
    }

    @EventBusSubscriber(modid = TideSourcerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(EntityInit.ABYSSAL_CORRUPTER.get(), TideSourcerRenderer::new);
        }
    }

    @EventBusSubscriber(modid = TideSourcerMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onBossBarRender(CustomizeGuiOverlayEvent.BossEventProgress event) {
            AbyssalBossHud.render(event);
        }

        @SubscribeEvent
        public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            AbyssalClientEffects.onCameraAngles(event);
        }
    }

    @EventBusSubscriber(modid = TideSourcerMod.MOD_ID)
    public static class ForgeGameEvents {
        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            cleanupTrackedBosses((LivingEntity) event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity().level().isClientSide()) {
                return;
            }
            cleanupTrackedBosses((LivingEntity) event.getEntity());
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            TideSourcerCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            if (AbyssalRitualSite.isProtected(event.getLevel(), event.getPos())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
            if (AbyssalRitualSite.isProtected(event.getLevel(), event.getPos())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
            if (event.getReplacedBlockSnapshots().stream().anyMatch(snapshot -> AbyssalRitualSite.isProtected(event.getLevel(), snapshot.getPos()))) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
            event.getAffectedBlocks().removeIf(pos -> AbyssalRitualSite.isProtected(event.getLevel(), pos));
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                for (TideSourcerEntity boss : TideSourcerEntity.getActiveInstances(level)) {
                    boss.restoreSummoningRitual();
                    boss.interruptEncounter();
                }
            }
        }

        private static void cleanupTrackedBosses(LivingEntity target) {
            MinecraftServer server = target.getServer();
            if (server == null) {
                return;
            }
            for (ServerLevel level : server.getAllLevels()) {
                for (TideSourcerEntity boss : TideSourcerEntity.getActiveInstances(level)) {
                    if (boss.isTrackingTarget(target)) {
                        boss.interruptEncounter();
                    }
                }
            }
        }
    }
}
