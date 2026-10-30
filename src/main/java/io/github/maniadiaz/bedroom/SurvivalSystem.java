package io.github.maniadiaz.bedroom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;

final class SurvivalSystem {
    static boolean active(ServerPlayer p) {
        var m = MatchData.get(p.server).members.get(p.getUUID());
        return m != null && m.state == MatchData.State.ACTIVE && MatchData.get(p.server).phase == MatchData.Phase.ACTIVE;
    }
    static void tick(ServerPlayer p, MatchData.Member m, MatchData d) {
        if (m.state != MatchData.State.ACTIVE) { FlashlightSystem.clear(p); AtmosphereSystem.clear(p); return; }
        AtmosphereSystem.tick(p, m);
        if(p.server.getTickCount()%20==0)for(int i=0;i<p.getInventory().getContainerSize();i++){
            var stack=p.getInventory().getItem(i);if(!stack.is(Bedroom.ANOMALOUS_COMPASS.get()))continue;
            var exit=MatchArena.exit(d);stack.getOrCreateTag().putInt("ExitX",exit.getX());stack.getOrCreateTag().putInt("ExitZ",exit.getZ());
        }
        boolean dark = p.level().getBrightness(LightLayer.BLOCK, p.blockPosition().above()) <= 7;
        boolean threat = !p.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, p.getBoundingBox().inflate(15), e -> e.distanceToSqr(p) < 225).isEmpty();
        boolean company = d.members.values().stream().filter(other -> other.state == MatchData.State.ACTIVE && !other.id.equals(m.id))
                .map(other -> p.server.getPlayerList().getPlayer(other.id)).anyMatch(other -> other != null && other.level() == p.level() && other.distanceToSqr(p) < 100);
        SurvivalState s = m.survival;
        s.advance(dark, threat, company, BedroomConfig.COMPANY_HELPS.get(),d.difficulty.sanityMultiplier*(1+d.level*.15));
        if (s.energyTicks > 0 && p.server.getTickCount() % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, s.energyTicks, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, s.energyTicks, 0));
        }
        FlashlightSystem.tick(p);
        if (s.sanity < 25) {
            var slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (slow == null || (s.ownsSlow && slow.getAmplifier() == 0 && slow.getDuration() <= 20)) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true)); s.ownsSlow = true;
            }
        } else if (s.sanity >= 25) removeOwnEffects(p, s);
        if (s.sanity < 50 && --s.hallucinationTicks <= 0) {
            s.hallucinationTicks = 200 + p.getRandom().nextInt(301);
            var origin = AtmosphereSystem.nearbyFloor(p, 3, 7);
            AtmosphereSystem.personal(p, s.sanity < 25 ? BedroomSounds.FALSE_STEPS.get() : BedroomSounds.WHISPER.get(),
                    origin == null ? p.position() : origin, .95F, .8F);
            if (s.sanity < 25 && !p.hasEffect(MobEffects.BLINDNESS)) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false, true)); s.ownsBlind = true;
            }
        }
        if (s.sanity <= 0) { if (++s.panicTicks >= 40) { s.panicTicks = 0; p.hurt(p.damageSources().inWall(), 1); } }
        else s.panicTicks = 0;
        d.setDirty();
    }
    static void removeOwnEffects(ServerPlayer p, SurvivalState s) {
        var slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (s.ownsSlow && slow != null && slow.getAmplifier() == 0 && slow.getDuration() <= 40) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        var blind = p.getEffect(MobEffects.BLINDNESS);
        if (s.ownsBlind && blind != null && blind.getAmplifier() == 0 && blind.getDuration() <= 20) p.removeEffect(MobEffects.BLINDNESS);
        s.ownsSlow = false; s.ownsBlind = false;
    }
    private SurvivalSystem() {}
}
