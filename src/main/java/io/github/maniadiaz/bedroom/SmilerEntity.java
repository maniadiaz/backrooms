package io.github.maniadiaz.bedroom;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.*;

/** Ground collision/navigation with visual flotation; never uses ghost navigation. */
public final class SmilerEntity extends Monster {
    static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Bedroom.MOD_ID);
    static final RegistryObject<EntityType<SmilerEntity>> TYPE = TYPES.register("smiler", () -> EntityType.Builder
            .of(SmilerEntity::new, MobCategory.MONSTER).sized(.65F, 1.8F).clientTrackingRange(10).build("bedroom:smiler"));
    enum State { STALKING, OBSERVED, ADVANCING, SEARCHING }
    State state = State.STALKING;
    static final int GAZE_GRACE = 7;
    int round = -1, matchLevel;
    private int lifetime, unseenTicks = GAZE_GRACE, evidence, attackCooldown;
    private boolean wasObserved;
    Vec3 lastKnown;
    private Vec3 previousTarget;

    public SmilerEntity(EntityType<? extends SmilerEntity> type, Level level) { super(type, level); xpReward = 0; }
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, .23).add(Attributes.ATTACK_DAMAGE, 16).add(Attributes.FOLLOW_RANGE, 64).build());
    }
    @Override protected void registerGoals() { }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override protected void playStepSound(BlockPos pos, BlockState block) { }
    @Override protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("BedroomRound", round); tag.putInt("BedroomLevel", matchLevel); tag.putInt("BedroomLifetime", lifetime);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); round = tag.contains("BedroomRound") ? tag.getInt("BedroomRound") : -1;
        matchLevel = tag.getInt("BedroomLevel"); lifetime = tag.getInt("BedroomLifetime");
    }
    boolean valid(ServerPlayer p) { return p.isAlive() && !p.isSpectator() && p.level() == level() && SurvivalSystem.active(p); }
    static boolean dark(Level level, BlockPos pos) { return level.getBrightness(LightLayer.BLOCK, pos) <= 7; }
    boolean looking(ServerPlayer p) {
        Vec3 delta = getEyePosition().subtract(p.getEyePosition());
        return valid(p) && distanceToSqr(p) <= 400 && !p.hasEffect(MobEffects.BLINDNESS)
                && delta.lengthSqr() > .001 && p.getLookAngle().dot(delta.normalize()) > .94 && p.hasLineOfSight(this);
    }
    boolean observedBy(Collection<ServerPlayer> players) { return players.stream().anyMatch(this::looking); }
    void beginEncounter(ServerPlayer target) { setTarget(target); lastKnown = target.position(); evidence = 0; }
    private void stopApproach() { getNavigation().stop(); setDeltaMovement(0, getDeltaMovement().y, 0); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide) return;
        var data = MatchData.get(getServer());
        if (round < 0) { round = data.round; matchLevel = data.level; }
        if (data.phase != MatchData.Phase.ACTIVE || round != data.round || matchLevel != data.level
                || !level().dimension().equals(DevelopmentCommands.LEVEL_0) || ++lifetime > 3600) { discard(); return; }
        brain(getServer().getPlayerList().getPlayers());
    }
    /** Same authoritative brain used by tick and multiplayer integration fixtures. */
    void brain(Collection<ServerPlayer> players) {
        if (attackCooldown > 0) attackCooldown--;
        ServerPlayer target = getTarget() instanceof ServerPlayer p && valid(p) ? p : null;
        if (target == null) {
            target = players.stream().filter(this::valid).filter(p -> distanceToSqr(p) <= 400 && hasLineOfSight(p))
                    .min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            lastKnown = null; previousTarget = null; evidence = 0;
            if (target != null) beginEncounter(target);
        }
        boolean observed = observedBy(players);
        if (observed) { unseenTicks = 0; wasObserved = true; state = State.OBSERVED; stopApproach(); }
        else if (unseenTicks < GAZE_GRACE) { unseenTicks++; stopApproach(); }
        if (tickCount % 20 == 0) AtmosphereSystem.noteSound((net.minecraft.server.level.ServerLevel)level(), position(), 20, null);
        if (tickCount % 80 == 0 && players.stream().anyMatch(p -> valid(p) && distanceToSqr(p) < 25)) playSound(BedroomSounds.SMILER_LAUGH.get(), .8F, .9F);
        if (target == null) { setTarget(null); stopApproach(); if (!observed) state = State.STALKING; return; }
        boolean visible = distanceToSqr(target) <= 400 && hasLineOfSight(target);
        if (visible) { lastKnown = target.position(); evidence = 0; }
        else if (++evidence > 300) { setTarget(null); lastKnown = null; previousTarget = null; state = State.STALKING; stopApproach(); return; }
        boolean retreating = previousTarget != null && target.position().subtract(previousTarget).multiply(1,0,1).dot(target.getLookAngle().multiply(1,0,1)) < -.02
                && dark(level(), target.blockPosition().above()) && visible;
        previousTarget = target.position();
        if (!observed && unseenTicks >= GAZE_GRACE) {
            state = !visible ? State.SEARCHING : wasObserved || retreating ? State.ADVANCING : State.STALKING;
            if (lastKnown != null && tickCount % 5 == 0) getNavigation().moveTo(lastKnown.x, lastKnown.y, lastKnown.z, state == State.ADVANCING ? 1.7 : .55);
        }
        if (!observed && unseenTicks >= GAZE_GRACE && visible && attackCooldown == 0
                && getBoundingBox().inflate(.2).intersects(target.getBoundingBox())) {
            attackCooldown = 30;
            strike(target, BedroomConfig.SMILER_LETHAL.get());
        }
    }
    boolean strike(ServerPlayer player, boolean lethal) {
        if (!valid(player)) return false;
        boolean hit = player.hurt(damageSources().mobAttack(this), lethal ? 1000 : 16);
        // A lethal bite already transitioned to limbo: never overwrite its recovered sanity.
        if (hit) {
            if (valid(player)) { MatchData.get(player.server).members.get(player.getUUID()).survival.sanity = 0; MatchData.get(player.server).setDirty(); }
            playSound(BedroomSounds.SMILER_ATTACK.get(), .9F, 1);
        }
        return hit;
    }
}
