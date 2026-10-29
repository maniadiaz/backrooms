package io.github.maniadiaz.bedroom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT)
public final class AtmosphereClient {
    private static Hum hum;
    private static ClientLevel world;
    private static boolean wasScene;
    private static boolean wasActive;

    static boolean scene() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Campaign.dungeon(mc.level.dimension())) return false;
        return BedroomHud.activeLevel() || (BedroomHud.canCycleCamera() && mc.getCameraEntity() != null && mc.getCameraEntity() != mc.player);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level) { reset(); world = mc.level; }
        boolean scene = scene(), active = BedroomHud.activeLevel();
        if (scene && !wasScene) mc.getSoundManager().stop(null, SoundSource.MUSIC);
        if (wasActive && !active) {
            mc.getSoundManager().stop(BedroomSounds.WHISPER.getId(), SoundSource.AMBIENT);
            mc.getSoundManager().stop(BedroomSounds.FALSE_STEPS.getId(), SoundSource.AMBIENT);
            mc.getSoundManager().stop(BedroomSounds.DISTANT_STEPS.getId(), SoundSource.AMBIENT);
        }
        if (!scene) {
            if (wasScene || hum != null) reset();
        } else if (AtmosphereClientConfig.HUM_VOLUME.get() <= 0 || mc.options.getSoundSourceVolume(SoundSource.MASTER) <= 0
                || mc.options.getSoundSourceVolume(SoundSource.AMBIENT) <= 0) {
            stopHum();
        } else if (hum == null || hum.isStopped()) {
            hum = new Hum(); mc.getSoundManager().play(hum);
        }
        wasScene = scene; wasActive = active;
    }

    static void reset() {
        Minecraft mc = Minecraft.getInstance();
        stopHum();
        SmilerClient.reset();
        if (wasScene) for (var sound : new net.minecraftforge.registries.RegistryObject[] {
                BedroomSounds.WET_STEP, BedroomSounds.DISTANT_STEPS, BedroomSounds.DRIP, BedroomSounds.BUZZ_CLICK, BedroomSounds.WHISPER, BedroomSounds.FALSE_STEPS,
                BedroomSounds.HOUND_STEPS, BedroomSounds.HOUND_BREATH, BedroomSounds.HOUND_LURE, BedroomSounds.HOUND_GROWL, BedroomSounds.HOUND_CHARGE, BedroomSounds.HOUND_ATTACK,
                BedroomSounds.SMILER_LAUGH, BedroomSounds.SMILER_ATTACK,
                BedroomSounds.BACTERIA_DRAG, BedroomSounds.BACTERIA_METAL, BedroomSounds.BACTERIA_ALERT, BedroomSounds.BACTERIA_ATTACK }) {
            mc.getSoundManager().stop(sound.getId(), null);
        }
        wasScene = false; wasActive = false;
    }

    private static void stopHum() {
        if (hum != null) { hum.finish(); Minecraft.getInstance().getSoundManager().stop(hum); hum = null; }
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); world = null; }
    @SubscribeEvent public static void music(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
        if (scene() && event.getSound() != null && event.getSound().getSource() == SoundSource.MUSIC) event.setSound(null);
    }

    private static boolean customFog(net.minecraft.client.Camera camera) {
        if (!scene() || !AtmosphereClientConfig.FOG.get() || camera.getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return false;
        return !(camera.getEntity() instanceof net.minecraft.world.entity.LivingEntity living)
                || (!living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) && !living.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS));
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        if (event.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN || !customFog(event.getCamera())) return;
        float far = Math.min(event.getFarPlaneDistance(), AtmosphereClientConfig.FOG_DISTANCE.get());
        event.setFarPlaneDistance(far); event.setNearPlaneDistance(Math.min(32, far * .5F));
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.CYLINDER); event.setCanceled(true);
    }
    @SubscribeEvent public static void fogColor(ViewportEvent.ComputeFogColor event) {
        if (!customFog(event.getCamera())) return;
        event.setRed(.39F); event.setGreen(.37F); event.setBlue(.28F);
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (!BedroomHud.activeLevel() || !scene() || BedroomHud.sanity() >= 50 || Minecraft.getInstance().level == null) return;
        double tick = Minecraft.getInstance().level.getGameTime() + event.getPartialTick();
        event.setRoll(event.getRoll() + (float) (Math.sin(tick * .025) * (1 - BedroomHud.sanity() / 50) * AtmosphereClientConfig.CAMERA_ROLL.get()));
    }

    @Mod.EventBusSubscriber(modid = Bedroom.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void audio(net.minecraftforge.client.event.sound.SoundEngineLoadEvent event) { reset(); }
    }

    private static final class Hum extends AbstractTickableSoundInstance {
        Hum() {
            super(BedroomSounds.HUM.get(), SoundSource.AMBIENT, RandomSource.create());
            looping = true; delay = 0; relative = true; attenuation = SoundInstance.Attenuation.NONE; volume = 0; pitch = 1;
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            if (!scene()) { stop(); return; }
            float target = AtmosphereClientConfig.HUM_VOLUME.get().floatValue();
            volume += Math.max(-.025F, Math.min(.025F, target - volume));
        }
        void finish() { stop(); }
    }
    private AtmosphereClient() {}
}
