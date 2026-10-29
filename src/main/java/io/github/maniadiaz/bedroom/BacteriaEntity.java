package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.*;

/** Evidence-driven ground hunter. Search never follows an unseen player's current coordinates. */
public final class BacteriaEntity extends Monster {
    static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Bedroom.MOD_ID);
    static final RegistryObject<EntityType<BacteriaEntity>> TYPE = TYPES.register("bacteria", () -> EntityType.Builder
            .of(BacteriaEntity::new, MobCategory.MONSTER).sized(.75F,1.45F).clientTrackingRange(10).build("bedroom:bacteria"));
    enum State { ROAMING, ALERT, CHASING, SEARCHING }
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(BacteriaEntity.class,EntityDataSerializers.INT);
    static final int ALERT_TICKS = 12, LOST_TICKS = 300, SEARCH_TICKS = 100;
    int round = -1, matchLevel;
    private int lifetime, clock, alertTicks, lostTicks, searchTicks, attackCooldown;
    Vec3 lastKnown;

    public BacteriaEntity(EntityType<? extends BacteriaEntity> type, Level level) { super(type,level); xpReward = 0; }
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(),Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,36)
                .add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.FOLLOW_RANGE,64).build());
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(STATE,0); }
    State state() { return State.values()[entityData.get(STATE)]; }
    private void state(State next) { entityData.set(STATE,next.ordinal()); }
    @Override protected void registerGoals() { }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override protected void playStepSound(BlockPos pos, BlockState block) { }
    @Override protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("BedroomRound",round); tag.putInt("BedroomLevel",matchLevel); tag.putInt("BedroomLifetime",lifetime);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); round = tag.contains("BedroomRound") ? tag.getInt("BedroomRound") : -1;
        matchLevel = tag.getInt("BedroomLevel"); lifetime = tag.getInt("BedroomLifetime");
    }
    boolean valid(ServerPlayer p) { return p.isAlive() && !p.isSpectator() && p.level() == level() && SurvivalSystem.active(p); }
    Vec3 evidence(ServerPlayer p) {
        if (!valid(p)) return null;
        if (distanceToSqr(p) <= 144 && hasLineOfSight(p)) return p.position();
        if (distanceToSqr(p) <= 225 && (p.isSprinting() || p.swinging)) return p.position();
        return BacteriaNoise.heard(this,p);
    }
    private void forget() { setTarget(null); lastKnown = null; lostTicks = searchTicks = alertTicks = 0; state(State.ROAMING); getNavigation().stop(); }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        var data = MatchData.get(getServer());
        if (round < 0) { round = data.round; matchLevel = data.level; }
        if (data.phase != MatchData.Phase.ACTIVE || data.round != round || data.level != matchLevel
                || !level().dimension().equals(DevelopmentCommands.LEVEL_0) || ++lifetime > 3600) { discard(); return; }
        brain(getServer().getPlayerList().getPlayers());
    }
    void brain(Collection<ServerPlayer> players) {
        clock++; if (attackCooldown > 0) attackCooldown--;
        ServerPlayer target = getTarget() instanceof ServerPlayer p && valid(p) ? p : null;
        if (getTarget() != null && target == null) forget();
        Vec3 detected = target == null ? null : evidence(target);
        if (detected == null) {
            ServerPlayer alternative = players.stream().filter(p -> evidence(p) != null)
                    .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (alternative != null) { target = alternative; detected = evidence(alternative); }
        }
        if (detected != null) {
            setTarget(target); lastKnown = detected; lostTicks = searchTicks = 0;
            if (state() == State.ROAMING || state() == State.SEARCHING) {
                state(State.ALERT); alertTicks = ALERT_TICKS; getNavigation().stop();
                playSound(BedroomSounds.BACTERIA_ALERT.get(),.9F,.9F);
            }
        }
        if (target == null) {
            if (clock % 60 == 0) wander(position(),8,.6);
            movementAudio(); return;
        }
        if (state() == State.ALERT) {
            getNavigation().stop(); setDeltaMovement(0,getDeltaMovement().y,0);
            if (--alertTicks > 0) { movementAudio(); return; }
            state(State.CHASING);
        }
        if (detected == null && state() == State.CHASING && ++lostTicks >= LOST_TICKS) {
            state(State.SEARCHING); searchTicks = SEARCH_TICKS; getNavigation().stop();
        }
        if (state() == State.SEARCHING) {
            if (--searchTicks <= 0) { forget(); movementAudio(); return; }
            if (clock % 20 == 0 && lastKnown != null) wander(lastKnown,6,.85);
        } else if (state() == State.CHASING) {
            if (clock % 5 == 0 && lastKnown != null) getNavigation().moveTo(lastKnown.x,lastKnown.y,lastKnown.z,1.55);
            if (detected != null && distanceToSqr(target) <= 144 && hasLineOfSight(target) && attackCooldown == 0
                    && getBoundingBox().inflate(.2).intersects(target.getBoundingBox())) {
                attackCooldown = 30;
                if (doHurtTarget(target)) playSound(BedroomSounds.BACTERIA_ATTACK.get(),.85F,1);
            }
        }
        movementAudio();
    }
    private void wander(Vec3 center, int radius, double speed) {
        for (int i = 0; i < 8; i++) {
            BlockPos point = BlockPos.containing(center).offset(random.nextInt(radius*2+1)-radius,0,random.nextInt(radius*2+1)-radius);
            if (!level().hasChunkAt(point) || !level().getBlockState(point.below()).isSolid()) continue;
            var path = getNavigation().createPath(point,0);
            if (path != null && path.canReach()) { getNavigation().moveTo(path,speed); return; }
        }
    }
    private void movementAudio() {
        // One short drag per interval, not one new sound every movement tick.
        if (state() != State.ALERT && getDeltaMovement().horizontalDistanceSqr() > .0004 && clock % 16 == 0)
            playSound(BedroomSounds.BACTERIA_DRAG.get(),.7F,state() == State.CHASING ? 1.15F : .85F);
        if (clock % 100 == 0) playSound(BedroomSounds.BACTERIA_METAL.get(),.6F,.9F);
    }
}
