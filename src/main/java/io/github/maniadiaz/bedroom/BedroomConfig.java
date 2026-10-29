package io.github.maniadiaz.bedroom;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BedroomConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEVELOPMENT_COMMANDS;
    public static final ForgeConfigSpec.BooleanValue AUTO_LOBBY;
    public static final ForgeConfigSpec.BooleanValue COMPANY_HELPS;
    public static final ForgeConfigSpec.BooleanValue LAMP_FAILURES;
    public static final ForgeConfigSpec.BooleanValue RAPID_FLICKER;
    public static final ForgeConfigSpec.BooleanValue SMILER_LETHAL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        DEVELOPMENT_COMMANDS = builder.comment("Enable operator-only prototype commands. Requires server restart.")
                .define("developmentCommands", true);
        AUTO_LOBBY = builder.comment("Send arriving players to the lobby; preserve existing match state on reconnect.")
                .define("autoLobby", true);
        COMPANY_HELPS = builder.comment("Halve monotony sanity drain with an active teammate within 10 blocks.").define("companyHelps", true);
        LAMP_FAILURES = builder.comment("Allow temporary fluorescent outages during active levels.").define("lampFailures", true);
        RAPID_FLICKER = builder.comment("Add brief flicker before/after outages. False uses one sustained outage instead.").define("rapidFlicker", false);
        SMILER_LETHAL = builder.comment("Smiler contact deals 1000 base damage instead of 16. Normal damage and limbo rules still apply.").define("smilerLethal", false);
        SPEC = builder.build();
    }

    private BedroomConfig() {}
}
