package io.github.maniadiaz.bedroom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

final class BedroomSounds {
    static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Bedroom.MOD_ID);
    static final RegistryObject<SoundEvent> HUM = sound("fluorescent_hum");
    static final RegistryObject<SoundEvent> HEARTBEAT=sound("recovery_heartbeat");
    static final RegistryObject<SoundEvent> GEIGER = sound("geiger");
    static final RegistryObject<SoundEvent> WET_STEP = sound("wet_step");
    static final RegistryObject<SoundEvent> DISTANT_STEPS = sound("distant_steps");
    static final RegistryObject<SoundEvent> DRIP = sound("ceiling_drip");
    static final RegistryObject<SoundEvent> BUZZ_CLICK = sound("buzz_click");
    static final RegistryObject<SoundEvent> WHISPER = sound("sanity_whisper");
    static final RegistryObject<SoundEvent> FALSE_STEPS = sound("sanity_steps");
    static final RegistryObject<SoundEvent> HOUND_STEPS = sound("hound_steps");
    static final RegistryObject<SoundEvent> HOUND_BREATH = sound("hound_breath");
    static final RegistryObject<SoundEvent> HOUND_LURE = sound("hound_lure");
    static final RegistryObject<SoundEvent> HOUND_GROWL = sound("hound_growl");
    static final RegistryObject<SoundEvent> HOUND_CHARGE = sound("hound_charge");
    static final RegistryObject<SoundEvent> HOUND_ATTACK = sound("hound_attack");
    static final RegistryObject<SoundEvent> SMILER_HISS = sound("smiler_hiss");
    static final RegistryObject<SoundEvent> SMILER_LAUGH = sound("smiler_laugh");
    static final RegistryObject<SoundEvent> SMILER_ATTACK = sound("smiler_attack");
    static final RegistryObject<SoundEvent> BACTERIA_DRAG = sound("bacteria_drag");
    static final RegistryObject<SoundEvent> BACTERIA_METAL = sound("bacteria_metal");
    static final RegistryObject<SoundEvent> BACTERIA_ALERT = sound("bacteria_alert");
    static final RegistryObject<SoundEvent> BACTERIA_ATTACK = sound("bacteria_attack");
    private static RegistryObject<SoundEvent> sound(String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, id);
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(location));
    }
    static final net.minecraft.world.level.block.SoundType CARPET = new net.minecraftforge.common.util.ForgeSoundType(1.2F, .9F,
            () -> net.minecraft.sounds.SoundEvents.WOOL_BREAK, WET_STEP, () -> net.minecraft.sounds.SoundEvents.WOOL_PLACE,
            () -> net.minecraft.sounds.SoundEvents.WOOL_HIT, WET_STEP);
    private BedroomSounds() {}
}
