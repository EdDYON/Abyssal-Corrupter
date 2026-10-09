package com.eddy1.tidesourcer.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AbyssalConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private AbyssalConfig() {
    }

    public static double scaledHealth(double baseHealth) {
        return baseHealth * COMMON.bossHealthMultiplier.get();
    }

    public static float scaledDamage(float baseDamage) {
        return (float) (baseDamage * COMMON.bossDamageMultiplier.get());
    }

    public static int scaledCooldown(int baseCooldown, boolean phaseTwo) {
        double multiplier = COMMON.cooldownMultiplier.get();
        if (phaseTwo) {
            multiplier *= COMMON.phaseTwoCooldownMultiplier.get();
        }
        return Math.max(1, (int) Math.round(baseCooldown * multiplier));
    }

    public static int scaledParticleCount(int baseCount) {
        if (baseCount <= 0) {
            return 0;
        }
        return switch (COMMON.particleQuality.get()) {
            case OFF -> 0;
            case LOW -> Math.max(1, (int) Math.ceil(baseCount * 0.35D));
            case NORMAL -> baseCount;
            case HIGH -> Math.max(baseCount, (int) Math.ceil(baseCount * 1.35D));
        };
    }

    public static boolean shouldEmitDetailParticle(long gameTime, int normalInterval) {
        ParticleQuality quality = COMMON.particleQuality.get();
        if (quality == ParticleQuality.OFF) {
            return false;
        }
        int interval = switch (quality) {
            case LOW -> normalInterval * 2;
            case NORMAL -> normalInterval;
            case HIGH -> Math.max(1, normalInterval / 2);
            case OFF -> Integer.MAX_VALUE;
        };
        return interval <= 1 || gameTime % interval == 0;
    }

    public enum ParticleQuality {
        OFF,
        LOW,
        NORMAL,
        HIGH
    }

    public static final class Common {
        public final ModConfigSpec.DoubleValue bossHealthMultiplier;
        public final ModConfigSpec.DoubleValue bossDamageMultiplier;
        public final ModConfigSpec.DoubleValue cooldownMultiplier;
        public final ModConfigSpec.DoubleValue phaseTwoCooldownMultiplier;
        public final ModConfigSpec.BooleanValue allowTerrainChanges;
        public final ModConfigSpec.EnumValue<ParticleQuality> particleQuality;
        public final ModConfigSpec.IntValue altarSummonTicks;
        public final ModConfigSpec.IntValue altarCooldownTicks;
        public final ModConfigSpec.BooleanValue altarConsumesCatalyst;
        public final ModConfigSpec.BooleanValue altarBuildsRitualSite;
        public final ModConfigSpec.IntValue altarRitualRadius;
        public final ModConfigSpec.IntValue entranceInvulnerableTicks;

        private Common(ModConfigSpec.Builder builder) {
            builder.push("boss");
            bossHealthMultiplier = builder
                    .comment("Multiplier applied to the Abyssal Corrupter's max health.")
                    .defineInRange("bossHealthMultiplier", 1.0D, 0.1D, 20.0D);
            bossDamageMultiplier = builder
                    .comment("Multiplier applied to configured boss skill damage.")
                    .defineInRange("bossDamageMultiplier", 1.0D, 0.1D, 20.0D);
            cooldownMultiplier = builder
                    .comment("Multiplier applied to boss skill cooldowns. Lower values make skills happen more often.")
                    .defineInRange("cooldownMultiplier", 1.0D, 0.25D, 5.0D);
            phaseTwoCooldownMultiplier = builder
                    .comment("Extra cooldown multiplier used below 60% health.")
                    .defineInRange("phaseTwoCooldownMultiplier", 0.86D, 0.25D, 2.0D);
            allowTerrainChanges = builder
                    .comment("If false, skills keep their hit logic but avoid temporary terrain replacement.")
                    .define("allowTerrainChanges", true);
            builder.pop();

            builder.push("visuals");
            particleQuality = builder
                    .comment("Server-side particle packet quality for boss effects.")
                    .defineEnum("particleQuality", ParticleQuality.NORMAL);
            builder.pop();

            builder.push("summoning");
            altarSummonTicks = builder
                    .comment("Duration of the altar summoning sequence in ticks.")
                    .defineInRange("altarSummonTicks", 100, 20, 400);
            altarCooldownTicks = builder
                    .comment("Cooldown after a successful altar summon in ticks.")
                    .defineInRange("altarCooldownTicks", 1200, 0, 72000);
            altarConsumesCatalyst = builder
                    .comment("If true, survival players must spend an echo shard to activate the altar.")
                    .define("altarConsumesCatalyst", false);
            altarBuildsRitualSite = builder
                    .comment("If true and terrain changes are allowed, activating the altar builds a small abyssal ritual site around it.")
                    .define("altarBuildsRitualSite", true);
            altarRitualRadius = builder
                    .comment("Radius of the generated abyssal ritual site around the altar.")
                    .defineInRange("altarRitualRadius", 60, 60, 96);
            entranceInvulnerableTicks = builder
                    .comment("Short invulnerable entrance time applied after altar summoning.")
                    .defineInRange("entranceInvulnerableTicks", 80, 0, 200);
            builder.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue screenShakeEnabled;
        public final ModConfigSpec.DoubleValue screenShakeStrength;
        public final ModConfigSpec.DoubleValue entranceDarknessStrength;
        public final ModConfigSpec.BooleanValue phaseTwoHudTint;

        private Client(ModConfigSpec.Builder builder) {
            builder.push("visuals");
            screenShakeEnabled = builder
                    .comment("Enable client camera shake from boss skills.")
                    .define("screenShakeEnabled", true);
            screenShakeStrength = builder
                    .comment("Camera shake strength multiplier.")
                    .defineInRange("screenShakeStrength", 1.0D, 0.0D, 3.0D);
            entranceDarknessStrength = builder
                    .comment("Visual darkness strength used by the altar entrance sequence.")
                    .defineInRange("entranceDarknessStrength", 0.55D, 0.0D, 1.0D);
            phaseTwoHudTint = builder
                    .comment("Enable stronger boss bar coloring below 60% health.")
                    .define("phaseTwoHudTint", true);
            builder.pop();
        }
    }
}
