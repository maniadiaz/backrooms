package io.github.maniadiaz.bedroom;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BedroomConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEVELOPMENT_COMMANDS;
    public static final ForgeConfigSpec.BooleanValue AUTO_LOBBY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        DEVELOPMENT_COMMANDS = builder.comment("Enable operator-only prototype commands. Requires server restart.")
                .define("developmentCommands", true);
        AUTO_LOBBY = builder.comment("Send arriving players to the lobby; preserve existing match state on reconnect.")
                .define("autoLobby", true);
        SPEC = builder.build();
    }

    private BedroomConfig() {}
}
