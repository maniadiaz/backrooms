package io.github.maniadiaz.bedroom;

import net.minecraftforge.common.ForgeConfigSpec;

final class AtmosphereClientConfig {
    static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.DoubleValue HUM_VOLUME;
    static final ForgeConfigSpec.BooleanValue FOG;
    static final ForgeConfigSpec.IntValue FOG_DISTANCE;
    static final ForgeConfigSpec.DoubleValue CAMERA_ROLL;
    static {
        var builder = new ForgeConfigSpec.Builder();
        HUM_VOLUME = builder.comment("Fluorescent hum volume, multiplied by Minecraft ambient/master volume. Does not alter sanity.").defineInRange("humVolume", .35, 0, 1);
        FOG = builder.comment("Enable the subtle long-distance dust haze inside an active level.").define("fog", true);
        FOG_DISTANCE = builder.defineInRange("fogDistance", 96, 48, 192);
        CAMERA_ROLL = builder.comment("Optional low-sanity camera roll in degrees. Zero disables camera motion.").defineInRange("cameraRoll", 0.0, 0, .5);
        SPEC = builder.build();
    }
    private AtmosphereClientConfig() {}
}
