package io.github.maniadiaz.bedroom;

import net.minecraftforge.common.ForgeConfigSpec;

public final class BedroomConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEVELOPMENT_COMMANDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        DEVELOPMENT_COMMANDS = builder.comment("Enable operator-only prototype commands. Requires server restart.")
                .define("developmentCommands", true);
        SPEC = builder.build();
    }

    private BedroomConfig() {}
}
