package io.github.maniadiaz.bedroom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.*;

/** Server-driven hunter. No vanilla player-target goal can select spectators. */
public final class HoundEntity extends Monster {
    static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Bedroom.MOD_ID);
    static final RegistryObject<EntityType<HoundEntity>> TYPE = TYPES.register("hound", () ->
            EntityType.Builder.of(HoundEntity::new, MobCategory.MONSTER).sized(.7F, 1.35F).clientTrackingRange(10).build("bedroom:hound"));
    enum State { INACTIVE, STALKING, HESITATING, CHARGING, SEARCHING }
    State state = State.INACTIVE;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> VISUAL_STATE=net.minecraft.network.syncher.SynchedEntityData.defineId(HoundEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(VISUAL_STATE,0);}
    State visualState(){return State.values()[entityData.get(VISUAL_STATE)];}
    private Vec3 lastKnown;
    private Vec3 previousTarget;
    private int evidence, attackCooldown, voiceCooldown, lifetime;
    int round = -1, matchLevel;

    public HoundEntity(EntityType<? extends HoundEntity> type, Level level) { super(type, level); xpReward = 0; }
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, .29).add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 64).build());
    }
    @Override protected void registerGoals() { }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("BedroomRound", round); tag.putInt("BedroomLevel", matchLevel); tag.putInt("BedroomLifetime", lifetime);
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag); round = tag.contains("BedroomRound") ? tag.getInt("BedroomRound") : -1; matchLevel = tag.getInt("BedroomLevel"); lifetime = tag.getInt("BedroomLifetime");
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source) { }

    static boolean looking(ServerPlayer player, HoundEntity hound) {
        Vec3 delta = hound.getEyePosition().subtract(player.getEyePosition());
        return !player.hasEffect(MobEffects.BLINDNESS) && delta.lengthSqr() > .01
                && player.getLookAngle().dot(delta.normalize()) > .94 && player.hasLineOfSight(hound);
    }
    static State reaction(boolean visible, boolean looking, boolean moving, boolean noisy, boolean hunting) {
        if (visible && looking && !moving && !noisy) return State.HESITATING;
        if (noisy || visible && (!looking || moving)) return State.CHARGING;
        return hunting ? State.SEARCHING : State.STALKING;
    }
    void investigate(ServerPlayer p){setTarget(p);lastKnown=p.position();evidence=0;state=State.SEARCHING;}
    private boolean valid(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && p.level() == level() && SurvivalSystem.active(p);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        var data = MatchData.get(getServer());
        if (round < 0) { round = data.round; matchLevel = data.level; }
        if (data.phase != MatchData.Phase.ACTIVE || round != data.round || matchLevel != data.level
                || !Campaign.dungeon(level().dimension()) || ++lifetime > 3600) { discard(); return; }
        if (attackCooldown > 0) attackCooldown--;
        if (voiceCooldown > 0) voiceCooldown--;
        ServerPlayer target = getTarget() instanceof ServerPlayer p && valid(p) ? p : null;
        // A runner can draw the charge even while another teammate stares.
        for (ServerPlayer p : getServer().getPlayerList().getPlayers()) {
            if (!valid(p) || distanceToSqr(p) > 225) continue;
            if (p.isSprinting() || target == null && (hasLineOfSight(p) || data.members.get(p.getUUID()).survival.sanity < 30)) {
                target = p; if (p.isSprinting()) break;
            }
        }
        if (tickCount % 100 == 0) playSound(BedroomSounds.HOUND_BREATH.get(), .65F, .9F);
        if ((state == State.INACTIVE || state == State.STALKING) && tickCount % 180 == 0) playSound(BedroomSounds.HOUND_LURE.get(), .6F, .8F);
        if (target == null) { getNavigation().stop(); setTarget(null); previousTarget = null; state = State.INACTIVE; entityData.set(VISUAL_STATE,state.ordinal()); return; }
        if (getTarget() != target) { previousTarget = null; evidence = 0; lastKnown = null; }
        setTarget(target);
        boolean visible = distanceToSqr(target) <= 225 && hasLineOfSight(target);
        boolean noisy = distanceToSqr(target) <= 225 && (target.isSprinting() || target.swinging);
        boolean moving = previousTarget != null && target.position().subtract(previousTarget).horizontalDistanceSqr() > .0025;
        if (previousTarget == null || moving) previousTarget = target.position();
        State next = reaction(visible, looking(target, this), moving, noisy, state == State.CHARGING || state == State.SEARCHING);
        if (visible || noisy) { lastKnown = target.position(); evidence = 0; }
        else if (++evidence > 300) { setTarget(null); lastKnown = null; previousTarget = null; state = State.INACTIVE; entityData.set(VISUAL_STATE,state.ordinal()); getNavigation().stop(); return; }
        if (next != state) {
            if (voiceCooldown == 0 && (next == State.CHARGING || next == State.HESITATING)) {
                playSound(next == State.CHARGING ? BedroomSounds.HOUND_CHARGE.get() : BedroomSounds.HOUND_GROWL.get(), .8F, .85F);
                voiceCooldown = 40;
            }
            state = next;
        }
        entityData.set(VISUAL_STATE,state.ordinal());
        if (state == State.HESITATING) { getNavigation().stop(); setDeltaMovement(0, getDeltaMovement().y, 0); getLookControl().setLookAt(target, 30, 30); }
        else if (lastKnown != null && tickCount % 10 == 0) getNavigation().moveTo(lastKnown.x, lastKnown.y, lastKnown.z, state == State.CHARGING ? 1.55 : .8);
        if (state == State.CHARGING && visible && distanceToSqr(target) < 2.25 && attackCooldown == 0) {
            attackCooldown = 30;
            if (doHurtTarget(target)) playSound(BedroomSounds.HOUND_ATTACK.get(), .85F, 1);
        }
    }
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState block) {
        playSound(BedroomSounds.HOUND_STEPS.get(), .65F, .8F);
    }
}
