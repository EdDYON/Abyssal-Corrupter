package com.eddy1.tidesourcer.command;

import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TideSourcerCommands {
    private static final SimpleCommandExceptionType NO_BOSS_FOUND = new SimpleCommandExceptionType(Component.literal("No Abyssal Corrupter found nearby."));
    private static final SimpleCommandExceptionType INVALID_BOSS = new SimpleCommandExceptionType(Component.literal("Target entity is not an Abyssal Corrupter."));
    private static final SimpleCommandExceptionType NO_LIVING_TARGET = new SimpleCommandExceptionType(Component.literal("A living target is required for this test."));
    private static final SimpleCommandExceptionType PLAYER_ONLY_TARGET = new SimpleCommandExceptionType(Component.literal("This test requires a player target."));
    private static final SimpleCommandExceptionType INVALID_VARIANT = new SimpleCommandExceptionType(Component.literal("That state does not support the requested variant."));
    private static final Map<String, SkillSpec> SKILLS = createSkills();
    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(SKILLS.keySet(), builder);

    private TideSourcerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("abyssal_corrupter")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("test")
                        .requires(TideSourcerCommands::canUseTestCommands)
                        .then(Commands.literal("list")
                                .executes(context -> listSkills(context.getSource())))
                        .then(Commands.literal("stop")
                                .executes(context -> stopBoss(context.getSource(), null))
                                .then(Commands.argument("boss", EntityArgument.entity())
                                        .executes(context -> stopBoss(context.getSource(), EntityArgument.getEntity(context, "boss")))))
                        .then(Commands.literal("cooldowns")
                                .executes(context -> clearCooldowns(context.getSource(), null))
                                .then(Commands.argument("boss", EntityArgument.entity())
                                        .executes(context -> clearCooldowns(context.getSource(), EntityArgument.getEntity(context, "boss")))))
                        .then(Commands.literal("state")
                                .then(Commands.argument("state", IntegerArgumentType.integer(1, 11))
                                        .executes(context -> runState(context, null, null))
                                        .then(Commands.argument("variant", IntegerArgumentType.integer(1, 3))
                                                .executes(context -> runState(context, null, null))
                                                .then(Commands.argument("boss", EntityArgument.entity())
                                                        .executes(context -> runState(context, EntityArgument.getEntity(context, "boss"), null))
                                                        .then(Commands.argument("target", EntityArgument.entity())
                                                                .executes(context -> runState(context,
                                                                        EntityArgument.getEntity(context, "boss"),
                                                                        EntityArgument.getEntity(context, "target"))))))
                                        .then(Commands.argument("boss", EntityArgument.entity())
                                                .executes(context -> runState(context, EntityArgument.getEntity(context, "boss"), null))
                                                .then(Commands.argument("target", EntityArgument.entity())
                                                        .executes(context -> runState(context,
                                                                EntityArgument.getEntity(context, "boss"),
                                                                EntityArgument.getEntity(context, "target")))))))
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests(SKILL_SUGGESTIONS)
                                .executes(context -> runSkill(context, null, null))
                                .then(Commands.argument("boss", EntityArgument.entity())
                                        .executes(context -> runSkill(context, EntityArgument.getEntity(context, "boss"), null))
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(context -> runSkill(context,
                                                        EntityArgument.getEntity(context, "boss"),
                                                        EntityArgument.getEntity(context, "target"))))))));
    }

    private static boolean canUseTestCommands(CommandSourceStack source) {
        if (!source.hasPermission(2)) {
            return false;
        }
        return source.getEntity() instanceof ServerPlayer player && player.isCreative();
    }

    private static int runSkill(CommandContext<CommandSourceStack> context, Entity bossArg, Entity targetArg) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String skillName = StringArgumentType.getString(context, "skill");
        SkillSpec spec = SKILLS.get(skillName);
        if (spec == null) {
            throw new SimpleCommandExceptionType(Component.literal("Unknown skill: " + skillName)).create();
        }

        TideSourcerEntity boss = resolveBoss(source, bossArg);
        LivingEntity target = resolveTarget(source, targetArg, boss, spec.playerOnly());
        triggerManualSkill(boss, target, spec.state(), spec.variant());

        source.sendSuccess(() -> Component.literal("Triggered skill '" + skillName + "' in manual test mode."), true);
        return 1;
    }

    private static int runState(CommandContext<CommandSourceStack> context, Entity bossArg, Entity targetArg) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int state = IntegerArgumentType.getInteger(context, "state");
        int variant = context.getNodes().stream().anyMatch(node -> "variant".equals(node.getNode().getName()))
                ? IntegerArgumentType.getInteger(context, "variant")
                : 1;
        if (variant > getMaxVariant(state)) {
            throw INVALID_VARIANT.create();
        }

        TideSourcerEntity boss = resolveBoss(source, bossArg);
        LivingEntity target = resolveTarget(source, targetArg, boss, isPlayerOnlyState(state));
        triggerManualSkill(boss, target, state, variant);

        int finalVariant = variant;
        source.sendSuccess(() -> Component.literal("Triggered state " + state + " variant " + finalVariant + " in manual test mode."), true);
        return 1;
    }

    private static int stopBoss(CommandSourceStack source, Entity bossArg) throws CommandSyntaxException {
        TideSourcerEntity boss = resolveBoss(source, bossArg);
        boss.disableManualTestMode();
        boss.interruptEncounter();
        source.sendSuccess(() -> Component.literal("Stopped the Abyssal Corrupter and exited manual test mode."), true);
        return 1;
    }

    private static int clearCooldowns(CommandSourceStack source, Entity bossArg) throws CommandSyntaxException {
        TideSourcerEntity boss = resolveBoss(source, bossArg);
        boss.clearCombatCooldowns();
        source.sendSuccess(() -> Component.literal("Cleared all Abyssal Corrupter cooldowns."), true);
        return 1;
    }

    private static int listSkills(CommandSourceStack source) {
        String summary = SKILLS.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().state() + "/" + entry.getValue().variant())
                .reduce((left, right) -> left + ", " + right)
                .orElse("No skills registered.");
        source.sendSuccess(() -> Component.literal("Skills: " + summary), false);
        return 1;
    }

    private static TideSourcerEntity resolveBoss(CommandSourceStack source, Entity bossArg) throws CommandSyntaxException {
        if (bossArg != null) {
            if (bossArg instanceof TideSourcerEntity boss && !boss.isClone) {
                return boss;
            }
            throw INVALID_BOSS.create();
        }

        Entity sourceEntity = source.getEntity();
        if (sourceEntity == null) {
            throw NO_BOSS_FOUND.create();
        }

        return sourceEntity.level().getEntitiesOfClass(
                        TideSourcerEntity.class,
                        sourceEntity.getBoundingBox().inflate(96.0D),
                        entity -> entity.isAlive() && !entity.isClone)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(sourceEntity)))
                .orElseThrow(NO_BOSS_FOUND::create);
    }

    private static LivingEntity resolveTarget(CommandSourceStack source, Entity targetArg, TideSourcerEntity boss, boolean playerOnly) throws CommandSyntaxException {
        LivingEntity target = null;
        if (targetArg instanceof LivingEntity living) {
            target = living;
        } else if (targetArg != null) {
            throw NO_LIVING_TARGET.create();
        } else if (source.getEntity() instanceof LivingEntity living && living != boss) {
            target = living;
        } else if (boss.getTarget() != null && boss.getTarget().isAlive()) {
            target = boss.getTarget();
        } else {
            AABB search = boss.getBoundingBox().inflate(64.0D);
            target = boss.level().getEntitiesOfClass(LivingEntity.class, search, entity -> entity != boss && entity.isAlive())
                    .stream()
                    .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(boss)))
                    .orElse(null);
        }

        if (target == null) {
            throw NO_LIVING_TARGET.create();
        }
        if (playerOnly && !(target instanceof Player)) {
            throw PLAYER_ONLY_TARGET.create();
        }
        return target;
    }

    private static void triggerManualSkill(TideSourcerEntity boss, LivingEntity target, int state, int variant) {
        boss.interruptEncounter();
        boss.clearCombatCooldowns();
        boss.enableManualTestMode();
        boss.setTarget(target);
        boss.startAttack(state, variant);
    }

    private static boolean isPlayerOnlyState(int state) {
        return state == 11;
    }

    private static int getMaxVariant(int state) {
        return switch (state) {
            case 1, 2, 3, 4 -> 3;
            default -> 1;
        };
    }

    private static Map<String, SkillSpec> createSkills() {
        Map<String, SkillSpec> skills = new LinkedHashMap<>();
        skills.put("slash", new SkillSpec(1, 1, false));
        skills.put("combo", new SkillSpec(1, 2, false));
        skills.put("beam_burst", new SkillSpec(1, 3, false));
        skills.put("jump_smash", new SkillSpec(2, 1, false));
        skills.put("eruption", new SkillSpec(2, 2, false));
        skills.put("skyfall", new SkillSpec(2, 3, false));
        skills.put("vortex", new SkillSpec(3, 1, false));
        skills.put("spin", new SkillSpec(3, 2, false));
        skills.put("prison", new SkillSpec(3, 3, false));
        skills.put("hook", new SkillSpec(4, 1, false));
        skills.put("breath", new SkillSpec(4, 2, false));
        skills.put("ray", new SkillSpec(4, 3, false));
        skills.put("dash", new SkillSpec(5, 1, false));
        skills.put("domain", new SkillSpec(6, 1, false));
        skills.put("mirage", new SkillSpec(7, 1, false));
        skills.put("armory", new SkillSpec(8, 1, false));
        skills.put("nova", new SkillSpec(9, 1, false));
        skills.put("singularity", new SkillSpec(10, 1, false));
        skills.put("taunt", new SkillSpec(11, 1, true));
        return skills;
    }

    private record SkillSpec(int state, int variant, boolean playerOnly) {
    }
}
