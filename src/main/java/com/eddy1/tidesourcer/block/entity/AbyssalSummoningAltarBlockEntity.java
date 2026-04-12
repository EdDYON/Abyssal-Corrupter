package com.eddy1.tidesourcer.block.entity;

import com.eddy1.tidesourcer.block.AbyssalSummoningAltarBlock;
import com.eddy1.tidesourcer.config.AbyssalConfig;
import com.eddy1.tidesourcer.entity.ai.AbyssalEffects;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import com.eddy1.tidesourcer.init.BlockEntityInit;
import com.eddy1.tidesourcer.init.EntityInit;
import com.eddy1.tidesourcer.world.AbyssalRitualSite;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AbyssalSummoningAltarBlockEntity extends BlockEntity {
    private static final double BOSS_CHECK_RADIUS = 96.0D;
    private static final int RITUAL_CLEAR_HEIGHT = 30;
    private static final int RITUAL_COLUMNS_PER_TICK = 64;
    private int summoningTicks;
    private int cooldownTicks;
    private int ritualTotalTicks;
    private UUID summonerId;
    private AbyssalRitualSite activeRitualSite;
    private final List<RitualColumn> pendingRitualColumns = new ArrayList<>();
    private int pendingRitualColumnIndex;
    private boolean ritualDecorationsBuilt = true;

    public AbyssalSummoningAltarBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityInit.ABYSSAL_SUMMONING_ALTAR.get(), pos, blockState);
    }

    public boolean tryStartSummoning(Player player, ItemStack catalyst) {
        if (this.level == null) {
            return false;
        }
        if (this.level.isClientSide()) {
            return true;
        }

        ServerLevel serverLevel = (ServerLevel) this.level;
        if (this.summoningTicks > 0) {
            player.displayClientMessage(Component.translatable("message.abyssal_corrupter.altar.already_summoning"), true);
            return false;
        }
        if (this.cooldownTicks > 0) {
            player.displayClientMessage(Component.translatable("message.abyssal_corrupter.altar.cooldown"), true);
            return false;
        }
        if (hasNearbyBoss(serverLevel, this.worldPosition)) {
            player.displayClientMessage(Component.translatable("message.abyssal_corrupter.altar.boss_nearby"), true);
            return false;
        }
        if (AbyssalConfig.COMMON.altarConsumesCatalyst.get() && !player.getAbilities().instabuild) {
            if (!catalyst.is(Items.ECHO_SHARD)) {
                player.displayClientMessage(Component.translatable("message.abyssal_corrupter.altar.needs_catalyst"), true);
                return false;
            }
            catalyst.shrink(1);
        }

        this.startRitualSiteGeneration(serverLevel);
        this.showRitualTitle(serverLevel, RitualTitleStage.OPENING);
        this.summonerId = player.getUUID();
        this.ritualTotalTicks = this.calculateRitualDuration();
        this.summoningTicks = this.ritualTotalTicks;
        this.setActive(true);
        this.setChanged();
        AbyssalEffects.play(serverLevel, Vec3.atCenterOf(this.worldPosition), SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.8F, 0.65F);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbyssalSummoningAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (altar.cooldownTicks > 0) {
            altar.cooldownTicks--;
            if (altar.cooldownTicks == 0) {
                altar.setChanged();
            }
        }

        if (altar.summoningTicks <= 0) {
            return;
        }

        altar.tickRitualSiteGeneration(serverLevel);
        if (serverLevel.getGameTime() % 20L == 0L) {
            altar.clearRitualDrops(serverLevel);
        }

        int totalTicks = Math.max(20, altar.ritualTotalTicks);
        int elapsed = totalTicks - altar.summoningTicks;
        Vec3 center = Vec3.atCenterOf(pos).add(0.0D, 0.25D, 0.0D);

        altar.renderSummoning(serverLevel, center, elapsed, totalTicks);
        altar.summoningTicks--;

        if (altar.summoningTicks <= 0) {
            if (!altar.isRitualSiteGenerationComplete()) {
                altar.summoningTicks = 1;
                return;
            }
            altar.finishSummon(serverLevel);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.summoningTicks = tag.getInt("SummoningTicks");
        this.cooldownTicks = tag.getInt("CooldownTicks");
        this.ritualTotalTicks = tag.getInt("RitualTotalTicks");
        if (this.ritualTotalTicks <= 0 && this.summoningTicks > 0) {
            this.ritualTotalTicks = this.summoningTicks;
        }
        if (tag.hasUUID("Summoner")) {
            this.summonerId = tag.getUUID("Summoner");
        } else {
            this.summonerId = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("SummoningTicks", this.summoningTicks);
        tag.putInt("CooldownTicks", this.cooldownTicks);
        tag.putInt("RitualTotalTicks", this.ritualTotalTicks);
        if (this.summonerId != null) {
            tag.putUUID("Summoner", this.summonerId);
        }
    }

    @Override
    public void setRemoved() {
        if (this.activeRitualSite != null && this.level instanceof ServerLevel serverLevel) {
            this.activeRitualSite.restore(serverLevel);
            this.activeRitualSite = null;
        }
        this.clearRitualGenerationState();
        super.setRemoved();
    }

    private void renderSummoning(ServerLevel serverLevel, Vec3 center, int elapsed, int totalTicks) {
        float progress = elapsed / (float) Math.max(1, totalTicks);
        double ritualRadius = Math.max(60.0D, AbyssalConfig.COMMON.altarRitualRadius.get());
        double radius = 4.0D + progress * (ritualRadius - 4.0D);

        if (elapsed == 20) {
            AbyssalEffects.play(serverLevel, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.8F, 0.58F);
        }
        if (elapsed == 60) {
            AbyssalEffects.play(serverLevel, center, SoundEvents.WARDEN_HEARTBEAT, 2.4F, 0.6F);
        }
        int sealTick = Math.max(40, totalTicks / 2);
        if (elapsed == sealTick) {
            this.showRitualTitle(serverLevel, RitualTitleStage.SEAL);
            AbyssalEffects.play(serverLevel, center, SoundEvents.SCULK_CATALYST_BLOOM, 2.0F, 0.48F);
        }
        if (elapsed == Math.max(0, totalTicks - 20)) {
            AbyssalEffects.play(serverLevel, center, SoundEvents.ELDER_GUARDIAN_CURSE, 2.2F, 0.55F);
            applyRitualScreenPressure(serverLevel, 100, 1, 96.0D);
        }

        if (AbyssalConfig.shouldEmitDetailParticle(serverLevel.getGameTime(), 2)) {
            AbyssalEffects.spawnInfectionCloud(serverLevel, center.add(0.0D, 0.45D, 0.0D), 0.75D + progress, 0.35D);
        }
        if (AbyssalConfig.shouldEmitDetailParticle(serverLevel.getGameTime(), 4)) {
            int points = Math.min(84, Math.max(28, (int) Math.ceil(ritualRadius * 2.2D)));
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i / points) - progress * Math.PI * 3.0D;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                AbyssalEffects.send(serverLevel, ParticleTypes.SOUL_FIRE_FLAME, x, center.y + 0.03D, z, 1, 0.03D, 0.01D, 0.03D, 0.0D);
                if (i % 2 == 0) {
                    AbyssalEffects.send(serverLevel, ParticleTypes.SCULK_SOUL, x, center.y + 0.08D, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                }
            }
        }
        if (elapsed % 10 == 0) {
            AbyssalEffects.send(serverLevel, ParticleTypes.REVERSE_PORTAL, center.add(0.0D, 1.0D + progress * 0.8D, 0.0D), 12, 0.8D, 0.5D, 0.8D, 0.06D);
        }
    }

    private void finishSummon(ServerLevel serverLevel) {
        Vec3 spawnPos = Vec3.atCenterOf(this.worldPosition).add(0.0D, 1.0D, 0.0D);
        TideSourcerEntity boss = EntityInit.ABYSSAL_CORRUPTER.get().create(serverLevel);
        if (boss != null) {
            boss.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, serverLevel.random.nextFloat() * 360.0F, 0.0F);
            boss.setPersistenceRequired();
            boss.beginEntrance(AbyssalConfig.COMMON.entranceInvulnerableTicks.get());
            boss.claimSummoningRitual(this.activeRitualSite);
            this.activeRitualSite = null;
            this.clearRitualGenerationState();

            Player summoner = this.summonerId == null ? null : serverLevel.getPlayerByUUID(this.summonerId);
            if (summoner != null && summoner.isAlive()) {
                boss.setTarget(summoner);
            }

            serverLevel.addFreshEntity(boss);
            this.clearRitualDrops(serverLevel);
            AbyssalEffects.spawnImpact(serverLevel, spawnPos, 1.6D, 1.0D);
            AbyssalEffects.play(serverLevel, spawnPos, SoundEvents.WARDEN_ROAR, 3.0F, 0.72F);
            this.showRitualTitle(serverLevel, RitualTitleStage.ARRIVAL);
        } else if (this.activeRitualSite != null) {
            this.activeRitualSite.restore(serverLevel);
            this.activeRitualSite = null;
            this.clearRitualGenerationState();
        }

        this.summoningTicks = 0;
        this.ritualTotalTicks = 0;
        this.cooldownTicks = Math.max(0, AbyssalConfig.COMMON.altarCooldownTicks.get());
        this.summonerId = null;
        this.setActive(false);
        this.setChanged();
    }

    private void startRitualSiteGeneration(ServerLevel serverLevel) {
        int radius = Math.max(60, AbyssalConfig.COMMON.altarRitualRadius.get());
        this.activeRitualSite = new AbyssalRitualSite(this.worldPosition, radius);
        this.clearRitualGenerationState();

        if (!AbyssalConfig.COMMON.allowTerrainChanges.get() || !AbyssalConfig.COMMON.altarBuildsRitualSite.get()) {
            this.ritualDecorationsBuilt = true;
            return;
        }

        double maxDistance = radius + 0.35D;
        double maxDistanceSquared = maxDistance * maxDistance;
        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                double distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
                if (distanceSquared <= maxDistanceSquared) {
                    this.pendingRitualColumns.add(new RitualColumn(offsetX, offsetZ, distanceSquared));
                }
            }
        }
        this.pendingRitualColumns.sort(Comparator.comparingDouble(RitualColumn::distanceSquared));
        this.ritualDecorationsBuilt = this.pendingRitualColumns.isEmpty();
    }

    private int calculateRitualDuration() {
        int configuredTicks = Math.max(20, AbyssalConfig.COMMON.altarSummonTicks.get());
        if (this.pendingRitualColumns.isEmpty()) {
            return configuredTicks;
        }

        int buildTicks = (int) Math.ceil(this.pendingRitualColumns.size() / (double) RITUAL_COLUMNS_PER_TICK);
        return Math.max(configuredTicks, buildTicks + 40);
    }

    private void tickRitualSiteGeneration(ServerLevel serverLevel) {
        if (this.activeRitualSite == null || this.pendingRitualColumns.isEmpty()) {
            return;
        }

        int radius = this.activeRitualSite.radius();
        int baseY = this.worldPosition.getY() - 1;
        int budget = RITUAL_COLUMNS_PER_TICK;
        while (budget-- > 0 && this.pendingRitualColumnIndex < this.pendingRitualColumns.size()) {
            RitualColumn column = this.pendingRitualColumns.get(this.pendingRitualColumnIndex++);
            this.buildRitualColumn(serverLevel, this.activeRitualSite, column, radius, baseY);
        }

        if (this.pendingRitualColumnIndex >= this.pendingRitualColumns.size() && !this.ritualDecorationsBuilt) {
            this.buildRitualDecorations(serverLevel, this.activeRitualSite, radius);
            this.ritualDecorationsBuilt = true;
        }
    }

    private boolean isRitualSiteGenerationComplete() {
        return this.pendingRitualColumnIndex >= this.pendingRitualColumns.size() && this.ritualDecorationsBuilt;
    }

    private void clearRitualGenerationState() {
        this.pendingRitualColumns.clear();
        this.pendingRitualColumnIndex = 0;
        this.ritualDecorationsBuilt = true;
    }

    private void buildRitualColumn(ServerLevel serverLevel, AbyssalRitualSite ritualSite, RitualColumn column, int radius, int baseY) {
        BlockPos floorPos = new BlockPos(this.worldPosition.getX() + column.offsetX(), baseY, this.worldPosition.getZ() + column.offsetZ());
        if (canReplaceRitualFloor(serverLevel, floorPos)) {
            ritualSite.place(serverLevel, floorPos, this.ritualFloorState(column.offsetX(), column.offsetZ(), radius, column.distanceSquared()));
        }
        clearRitualColumn(serverLevel, ritualSite, column.offsetX(), column.offsetZ());
    }

    private void buildRitualDecorations(ServerLevel serverLevel, AbyssalRitualSite ritualSite, int radius) {
        int pillarOffset = Math.max(60, radius);
        int diagonalOffset = Math.max(42, (int) Math.round(radius * 0.70D));
        buildCentralDais(serverLevel, ritualSite);
        buildRitualPillar(serverLevel, ritualSite, this.worldPosition.offset(diagonalOffset, 0, diagonalOffset), true);
        buildRitualPillar(serverLevel, ritualSite, this.worldPosition.offset(-diagonalOffset, 0, diagonalOffset), true);
        buildRitualPillar(serverLevel, ritualSite, this.worldPosition.offset(diagonalOffset, 0, -diagonalOffset), true);
        buildRitualPillar(serverLevel, ritualSite, this.worldPosition.offset(-diagonalOffset, 0, -diagonalOffset), true);

        buildRitualPillar(serverLevel, ritualSite, this.worldPosition.offset(pillarOffset, 0, 0), false);
        buildRitualPillar(serverLevel, ritualSite, this.worldPosition.offset(-pillarOffset, 0, 0), false);
        buildRitualPillar(serverLevel, ritualSite, this.worldPosition.offset(0, 0, pillarOffset), false);
        buildRitualPillar(serverLevel, ritualSite, this.worldPosition.offset(0, 0, -pillarOffset), false);

        buildRitualFang(serverLevel, ritualSite, this.worldPosition.offset(pillarOffset, 0, 0), 4);
        buildRitualFang(serverLevel, ritualSite, this.worldPosition.offset(-pillarOffset, 0, 0), 4);
        buildRitualFang(serverLevel, ritualSite, this.worldPosition.offset(0, 0, pillarOffset), 4);
        buildRitualFang(serverLevel, ritualSite, this.worldPosition.offset(0, 0, -pillarOffset), 4);
        buildInnerCrown(serverLevel, ritualSite, Math.max(10, radius / 2));

        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        AbyssalEffects.spawnImpact(serverLevel, center, 6.0D, 1.0D);
        AbyssalEffects.spawnAftershock(serverLevel, center, radius + 0.5D);
        AbyssalEffects.play(serverLevel, center, SoundEvents.SCULK_CATALYST_BLOOM, 2.1F, 0.62F);
        AbyssalEffects.play(serverLevel, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.5F, 0.42F);
        spawnRitualPillars(serverLevel, pillarOffset);
        spawnRitualPillars(serverLevel, diagonalOffset);
    }

    private record RitualColumn(int offsetX, int offsetZ, double distanceSquared) {
    }

    private void clearRitualColumn(ServerLevel serverLevel, AbyssalRitualSite ritualSite, int offsetX, int offsetZ) {
        int x = this.worldPosition.getX() + offsetX;
        int z = this.worldPosition.getZ() + offsetZ;
        for (int yOffset = 0; yOffset < RITUAL_CLEAR_HEIGHT; yOffset++) {
            BlockPos pos = new BlockPos(x, this.worldPosition.getY() + yOffset, z);
            if (pos.equals(this.worldPosition) || serverLevel.getBlockEntity(pos) != null) {
                continue;
            }

            BlockState state = serverLevel.getBlockState(pos);
            if (!state.isAir() && state.getDestroySpeed(serverLevel, pos) >= 0.0F) {
                ritualSite.place(serverLevel, pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private BlockState ritualFloorState(int offsetX, int offsetZ, int radius, double distanceSquared) {
        int absX = Math.abs(offsetX);
        int absZ = Math.abs(offsetZ);
        int noise = Math.floorMod(offsetX * 31 + offsetZ * 17 + this.worldPosition.getX() * 7 + this.worldPosition.getZ() * 11, 12);

        if (absX <= 2 && absZ <= 2) {
            return noise % 4 == 0 ? Blocks.SCULK.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        boolean majorLine = absX <= 1 || absZ <= 1 || Math.abs(absX - absZ) <= 1;
        if (majorLine && absX + absZ > 2) {
            return noise % 3 == 0 ? Blocks.SCULK.defaultBlockState() : Blocks.SOUL_SOIL.defaultBlockState();
        }
        if (absX == absZ && absX > 1 && noise % 2 == 0) {
            return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }

        double innerEdge = Math.max(1, radius - 2);
        if (distanceSquared >= innerEdge * innerEdge) {
            return noise % 5 == 0 ? Blocks.REINFORCED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        if (noise == 0 || noise == 7) {
            return Blocks.SCULK.defaultBlockState();
        }
        return noise % 3 == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    private void buildCentralDais(ServerLevel serverLevel, AbyssalRitualSite ritualSite) {
        for (int offsetX = -3; offsetX <= 3; offsetX++) {
            for (int offsetZ = -3; offsetZ <= 3; offsetZ++) {
                if (offsetX == 0 && offsetZ == 0) {
                    continue;
                }
                int distance = Math.abs(offsetX) + Math.abs(offsetZ);
                if (distance <= 4 && (Math.abs(offsetX) == 3 || Math.abs(offsetZ) == 3 || distance <= 2)) {
                    BlockState state = distance <= 2 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.CHISELED_DEEPSLATE.defaultBlockState();
                    placeDecoration(serverLevel, ritualSite, this.worldPosition.offset(offsetX, 0, offsetZ), state);
                }
            }
        }
    }

    private void buildRitualPillar(ServerLevel serverLevel, AbyssalRitualSite ritualSite, BlockPos basePos, boolean tall) {
        int height = tall ? 9 : 7;
        for (int y = 0; y < height; y++) {
            BlockState state;
            if (y == height - 1) {
                state = Blocks.SCULK_CATALYST.defaultBlockState();
            } else if (y % 3 == 1) {
                state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
            } else if (y % 3 == 2) {
                state = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            } else {
                state = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
            }
            placeDecoration(serverLevel, ritualSite, basePos.above(y), state);
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (Math.floorMod(basePos.getX() + basePos.getZ() + direction.get2DDataValue(), 2) == 0) {
                placeDecoration(serverLevel, ritualSite, basePos.relative(direction), Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState());
                placeDecoration(serverLevel, ritualSite, basePos.relative(direction).above(), Blocks.SCULK.defaultBlockState());
                if (tall) {
                    placeDecoration(serverLevel, ritualSite, basePos.relative(direction).above(2), Blocks.CRYING_OBSIDIAN.defaultBlockState());
                }
            }
        }
    }

    private void buildRitualFang(ServerLevel serverLevel, AbyssalRitualSite ritualSite, BlockPos basePos, int height) {
        for (int y = 0; y < height; y++) {
            BlockState state = y == height - 1
                    ? Blocks.SCULK.defaultBlockState()
                    : (y % 2 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : Blocks.CRYING_OBSIDIAN.defaultBlockState());
            placeDecoration(serverLevel, ritualSite, basePos.above(y), state);
        }
    }

    private void buildInnerCrown(ServerLevel serverLevel, AbyssalRitualSite ritualSite, int offset) {
        buildRitualFang(serverLevel, ritualSite, this.worldPosition.offset(offset, 0, offset), 3);
        buildRitualFang(serverLevel, ritualSite, this.worldPosition.offset(-offset, 0, offset), 3);
        buildRitualFang(serverLevel, ritualSite, this.worldPosition.offset(offset, 0, -offset), 3);
        buildRitualFang(serverLevel, ritualSite, this.worldPosition.offset(-offset, 0, -offset), 3);
    }

    private void spawnRitualPillars(ServerLevel serverLevel, int pillarOffset) {
        double y = this.worldPosition.getY() + 0.2D;
        AbyssalEffects.spawnRiftPillar(serverLevel, this.worldPosition.getX() + pillarOffset + 0.5D, y, this.worldPosition.getZ() + pillarOffset + 0.5D);
        AbyssalEffects.spawnRiftPillar(serverLevel, this.worldPosition.getX() - pillarOffset + 0.5D, y, this.worldPosition.getZ() + pillarOffset + 0.5D);
        AbyssalEffects.spawnRiftPillar(serverLevel, this.worldPosition.getX() + pillarOffset + 0.5D, y, this.worldPosition.getZ() - pillarOffset + 0.5D);
        AbyssalEffects.spawnRiftPillar(serverLevel, this.worldPosition.getX() - pillarOffset + 0.5D, y, this.worldPosition.getZ() - pillarOffset + 0.5D);
    }

    private void placeDecoration(ServerLevel serverLevel, AbyssalRitualSite ritualSite, BlockPos pos, BlockState state) {
        if (canReplaceDecoration(serverLevel, pos)) {
            ritualSite.place(serverLevel, pos, state);
        }
    }

    private boolean canReplaceRitualFloor(ServerLevel serverLevel, BlockPos pos) {
        BlockState state = serverLevel.getBlockState(pos);
        return !pos.equals(this.worldPosition)
                && serverLevel.getBlockEntity(pos) == null
                && state.getDestroySpeed(serverLevel, pos) >= 0.0F;
    }

    private static boolean canReplaceDecoration(ServerLevel serverLevel, BlockPos pos) {
        BlockState state = serverLevel.getBlockState(pos);
        return serverLevel.getBlockEntity(pos) == null
                && (state.isAir() || state.canBeReplaced());
    }

    private void showRitualTitle(ServerLevel serverLevel, RitualTitleStage stage) {
        AABB area = new AABB(this.worldPosition).inflate(96.0D, 48.0D, 96.0D);
        Component title = this.ritualTitleComponent(stage);
        Component subtitle = this.ritualSubtitleComponent(stage);
        int stayTicks = switch (stage) {
            case OPENING -> 90;
            case SEAL -> 70;
            case ARRIVAL -> 80;
        };
        int darknessTicks = switch (stage) {
            case OPENING -> 80;
            case SEAL -> 70;
            case ARRIVAL -> 120;
        };
        int darknessAmplifier = stage == RitualTitleStage.ARRIVAL ? 1 : 0;

        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area, ServerPlayer::isAlive)) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(8, stayTicks, 18));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, darknessTicks, darknessAmplifier, false, false, false));
        }
    }

    private Component ritualTitleComponent(RitualTitleStage stage) {
        String key = switch (stage) {
            case OPENING -> "title.abyssal_corrupter.ritual.opening.title";
            case SEAL -> "title.abyssal_corrupter.ritual.seal.title";
            case ARRIVAL -> "title.abyssal_corrupter.ritual.arrival.title";
        };
        ChatFormatting mainColor = switch (stage) {
            case OPENING -> ChatFormatting.DARK_AQUA;
            case SEAL -> ChatFormatting.DARK_PURPLE;
            case ARRIVAL -> ChatFormatting.RED;
        };
        ChatFormatting accentColor = switch (stage) {
            case OPENING -> ChatFormatting.AQUA;
            case SEAL -> ChatFormatting.LIGHT_PURPLE;
            case ARRIVAL -> ChatFormatting.DARK_RED;
        };
        return Component.literal("<< ")
                .withStyle(accentColor, ChatFormatting.BOLD)
                .append(Component.translatable(key).withStyle(mainColor, ChatFormatting.BOLD))
                .append(Component.literal(" >>").withStyle(accentColor, ChatFormatting.BOLD));
    }

    private Component ritualSubtitleComponent(RitualTitleStage stage) {
        String key = switch (stage) {
            case OPENING -> "title.abyssal_corrupter.ritual.opening.subtitle";
            case SEAL -> "title.abyssal_corrupter.ritual.seal.subtitle";
            case ARRIVAL -> "title.abyssal_corrupter.ritual.arrival.subtitle";
        };
        ChatFormatting color = switch (stage) {
            case OPENING -> ChatFormatting.GRAY;
            case SEAL -> ChatFormatting.AQUA;
            case ARRIVAL -> ChatFormatting.GOLD;
        };
        return Component.translatable(key).withStyle(color, ChatFormatting.ITALIC);
    }

    private void applyRitualScreenPressure(ServerLevel serverLevel, int duration, int amplifier, double radius) {
        AABB area = new AABB(this.worldPosition).inflate(radius, 48.0D, radius);
        List<Player> players = serverLevel.getEntitiesOfClass(Player.class, area, LivingEntity::isAlive);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, amplifier, false, false, false));
        }
    }

    private void clearRitualDrops(ServerLevel serverLevel) {
        double radius = this.activeRitualSite == null ? 60.0D : this.activeRitualSite.radius() + 2.0D;
        double radiusSquared = radius * radius;
        AABB area = new AABB(this.worldPosition).inflate(radius, RITUAL_CLEAR_HEIGHT + 8.0D, radius);
        Vec3 center = Vec3.atCenterOf(this.worldPosition);

        for (ItemEntity itemEntity : serverLevel.getEntitiesOfClass(ItemEntity.class, area)) {
            if (horizontalDistanceSquared(center, itemEntity.position()) <= radiusSquared) {
                itemEntity.discard();
            }
        }
        for (ExperienceOrb orb : serverLevel.getEntitiesOfClass(ExperienceOrb.class, area)) {
            if (horizontalDistanceSquared(center, orb.position()) <= radiusSquared) {
                orb.discard();
            }
        }
    }

    private static double horizontalDistanceSquared(Vec3 center, Vec3 pos) {
        double dx = pos.x - center.x;
        double dz = pos.z - center.z;
        return dx * dx + dz * dz;
    }

    private void setActive(boolean active) {
        if (this.level == null) {
            return;
        }
        BlockState state = this.getBlockState();
        if (state.hasProperty(AbyssalSummoningAltarBlock.ACTIVE) && state.getValue(AbyssalSummoningAltarBlock.ACTIVE) != active) {
            this.level.setBlock(this.worldPosition, state.setValue(AbyssalSummoningAltarBlock.ACTIVE, active), 3);
        }
    }

    private static boolean hasNearbyBoss(ServerLevel serverLevel, BlockPos pos) {
        AABB area = new AABB(pos).inflate(BOSS_CHECK_RADIUS);
        return !serverLevel.getEntitiesOfClass(TideSourcerEntity.class, area, entity -> !entity.isClone && entity.isAlive()).isEmpty();
    }

    private enum RitualTitleStage {
        OPENING,
        SEAL,
        ARRIVAL
    }
}
