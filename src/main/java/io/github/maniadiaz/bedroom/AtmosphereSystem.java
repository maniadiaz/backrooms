package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID)
final class AtmosphereSystem {
    private record Lamp(ServerLevel level, BlockPos pos) {}
    private static final Map<UUID, Set<Lamp>> faults = new HashMap<>();

    static void tick(ServerPlayer p, MatchData.Member member) {
        if (!SurvivalSystem.active(p)) { clear(p); return; }
        AmbientSchedule.Events events = member.ambient.tick(p.getRandom());
        if (events.footsteps()) {
            Vec3 origin = nearbyFloor(p, 10, 18);
            if (origin != null) {
                personal(p, BedroomSounds.DISTANT_STEPS.get(), origin, 1.25F, .8F);
                member.survival.quietTicks = 0;
            }
        }
        if (events.drip()) {
            Vec3 floor = nearbyFloor(p, 3, 10);
            if (floor != null && p.level().getBlockState(BlockPos.containing(floor.x, Campaign.ceiling(p.level()), floor.z)).is(Bedroom.CEILING.get())) {
                p.serverLevel().sendParticles(ParticleTypes.DRIPPING_WATER, floor.x, Campaign.ceiling(p.level())-.05, floor.z, 1, 0, 0, 0, 0);
                p.serverLevel().playSound(null, floor.x, 65.05, floor.z, BedroomSounds.DRIP.get(), SoundSource.AMBIENT, .85F, .9F);
                noteSound(p.serverLevel(), floor, 16, null);
            }
        }
        if (events.fault() && BedroomConfig.LAMP_FAILURES.get()) tryFault(p);
    }

    static Vec3 nearbyFloor(ServerPlayer p, int minimum, int maximum) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = p.getRandom().nextDouble() * Math.PI * 2;
            double distance = minimum + p.getRandom().nextDouble() * (maximum - minimum);
            BlockPos pos = BlockPos.containing(p.getX() + Math.cos(angle) * distance, 65, p.getZ() + Math.sin(angle) * distance);
            if (p.level().hasChunkAt(pos) && p.level().getBlockState(pos).isAir() && p.level().getBlockState(pos.above()).isAir())
                return new Vec3(pos.getX() + .5, 65.05, pos.getZ() + .5);
        }
        return null;
    }

    static boolean tryFault(ServerPlayer p) {
        List<BlockPos> lamps = new ArrayList<>();
        int cx = Math.floorDiv(p.getBlockX(), 8) * 8, cz = Math.floorDiv(p.getBlockZ(), 8) * 8;
        for (int dx = -16; dx <= 16; dx += 8) for (int dz = -16; dz <= 16; dz += 8) {
            BlockPos pos = new BlockPos(cx + dx, Campaign.lampY(p.level()), cz + dz);
            if (!p.level().hasChunkAt(pos)) continue;
            var state = p.level().getBlockState(pos);
            if (!state.is(Bedroom.LIGHT.get())) continue;
            if (state.getValue(FluorescentBlock.FAULT) != 0) return false;
            if (p.distanceToSqr(Vec3.atCenterOf(pos)) <= 144) lamps.add(pos);
        }
        if (lamps.isEmpty()) return false;
        BlockPos pos = lamps.get(p.getRandom().nextInt(lamps.size()));
        if (!FluorescentBlock.fail(p.serverLevel(), pos, BedroomConfig.RAPID_FLICKER.get(), 40 + p.getRandom().nextInt(41))) return false;
        // Retain only faults that have not already recovered; no unbounded trail of positions.
        Set<Lamp> owned = faults.computeIfAbsent(p.getUUID(), key -> new HashSet<>());
        owned.removeIf(lamp -> !lamp.level.hasChunkAt(lamp.pos) || !lamp.level.getBlockState(lamp.pos).is(Bedroom.LIGHT.get())
                || lamp.level.getBlockState(lamp.pos).getValue(FluorescentBlock.FAULT) == 0);
        owned.add(new Lamp(p.serverLevel(), pos));
        p.serverLevel().playSound(null, pos, BedroomSounds.BUZZ_CLICK.get(), SoundSource.BLOCKS, 1.15F, .85F);
        noteSound(p.serverLevel(), Vec3.atCenterOf(pos), 20, null);
        return true;
    }

    static void personal(ServerPlayer p, SoundEvent sound, Vec3 origin, float volume, float pitch) {
        p.connection.send(new ClientboundSoundPacket(Holder.direct(sound), SoundSource.AMBIENT,
                origin.x, origin.y, origin.z, volume, pitch, p.getRandom().nextLong()));
    }

    static void noteSound(ServerLevel level, Vec3 origin, double range, UUID excluded) {
        noteSound(level, origin, range, excluded, level.getServer().getPlayerList().getPlayers());
    }

    static void noteSound(ServerLevel level, Vec3 origin, double range, UUID excluded, Collection<ServerPlayer> listeners) {
        MatchData data = MatchData.get(level.getServer());
        listeners.forEach(p -> {
            var m = data.members.get(p.getUUID());
            if (m == null || m.state != MatchData.State.ACTIVE || m.id.equals(excluded)) return;
            if (p.level() == level && p.distanceToSqr(origin) < range * range) m.survival.quietTicks = 0;
        });
    }

    @SubscribeEvent public static void entitySound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Campaign.dungeon(level.dimension())
                || event.getSound() == null || event.getNewVolume() <= 0) return;
        var entity = event.getEntity();
        if (entity instanceof ServerPlayer player && !SurvivalSystem.active(player)) return;
        noteSound(level, entity.position(), 16, entity instanceof net.minecraft.world.entity.player.Player ? entity.getUUID() : null);
    }

    @SubscribeEvent public static void positionedSound(PlayLevelSoundEvent.AtPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Campaign.dungeon(level.dimension())
                || event.getSound() == null || event.getNewVolume() <= 0) return;
        var id = event.getSound().value().getLocation();
        if (id.equals(BedroomSounds.HUM.getId())) return;
        UUID excluded = null;
        if (event.getSource() == SoundSource.PLAYERS && (id.equals(BedroomSounds.WET_STEP.getId()) || id.getPath().endsWith(".step"))) {
            var nearest = level.getServer().getPlayerList().getPlayers().stream()
                    .filter(p -> SurvivalSystem.active(p) && p.level() == level && p.distanceToSqr(event.getPosition()) < 4)
                    .min(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(event.getPosition()))).orElse(null);
            if (nearest != null) excluded = nearest.getUUID();
        }
        noteSound(level, event.getPosition(), 16, excluded);
    }

    static void clear(ServerPlayer p) {
        for (Lamp lamp : faults.getOrDefault(p.getUUID(), Set.of())) {
            if (!lamp.level.hasChunkAt(lamp.pos)) continue; // Scheduled block ticks recover unloaded lamps on their next load.
            var state = lamp.level.getBlockState(lamp.pos);
            if (state.is(Bedroom.LIGHT.get()) && state.getValue(FluorescentBlock.FAULT) != 0)
                lamp.level.setBlock(lamp.pos, state.setValue(FluorescentBlock.FAULT, 0), 3);
        }
        faults.remove(p.getUUID());
    }
    static void clearAll() {
        for (Set<Lamp> lamps : faults.values()) for (Lamp lamp : lamps) {
            if (lamp.level.hasChunkAt(lamp.pos)) {
                var state = lamp.level.getBlockState(lamp.pos);
                if (state.is(Bedroom.LIGHT.get())) lamp.level.setBlock(lamp.pos, state.setValue(FluorescentBlock.FAULT, 0), 3);
            }
        }
        faults.clear();
    }
    private AtmosphereSystem() {}
}
