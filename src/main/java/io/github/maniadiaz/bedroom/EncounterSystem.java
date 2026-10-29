package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shared budget for all threat species, not a separate limit for each monster. */
@Mod.EventBusSubscriber(modid = Bedroom.MOD_ID)
public final class EncounterSystem {
    private record Progress(int round, int level, Set<Long> rooms, int nextRoom, int cooldown) { }
    private static final Map<UUID, Progress> progress = new HashMap<>();
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { progress.clear(); }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        var server = event.getServer(); var data = MatchData.get(server);
        if (data.phase != MatchData.Phase.ACTIVE) { progress.clear(); return; }
        var level = server.getLevel(DevelopmentCommands.LEVEL_0);
        if (level == null) return;
        var active = server.getPlayerList().getPlayers().stream().filter(p -> p.level() == level && SurvivalSystem.active(p) && p.isAlive() && !p.isSpectator()).toList();
        // Preserve cooldown through limbo and reconnections within this round.
        for (ServerPlayer p : active) {
            Progress old = progress.get(p.getUUID());
            if (old == null || old.round != data.round || old.level != data.level)
                old = new Progress(data.round, data.level, new HashSet<>(), 30 + p.getRandom().nextInt(11), server.getTickCount() + 1200);
            var rooms = old.rooms;
            rooms.add(BlockPos.asLong(Math.floorDiv(p.blockPosition().getX(), 16), 0, Math.floorDiv(p.blockPosition().getZ(), 16)));
            progress.put(p.getUUID(), old);
            if (server.getTickCount() < old.cooldown || rooms.size() < old.nextRoom && data.members.get(p.getUUID()).survival.sanity >= 40) continue;
            // At most one encounter per nearby group, including other Monster subclasses.
            if (!level.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(96)).isEmpty()) continue;
            // Failed placement also backs off; low sanity never retries every tick.
            progress.put(p.getUUID(), new Progress(old.round, old.level, rooms, rooms.size() + 30 + p.getRandom().nextInt(11), server.getTickCount() + 6000));
            for (int attempt = 0; attempt < 24; attempt++) {
                double angle = p.getRandom().nextDouble() * Math.PI * 2;
                double distance = 28 + p.getRandom().nextInt(17);
                BlockPos pos = BlockPos.containing(p.getX() + Math.cos(angle) * distance, 65, p.getZ() + Math.sin(angle) * distance);
                if (pos.getX() < 1000000) continue;
                HoundEntity hound = candidate(level, pos, p, active);
                if (hound == null) continue;
                hound.round = data.round; hound.matchLevel = data.level;
                level.addFreshEntity(hound);
                for (ServerPlayer other : active) if (other.distanceToSqr(p) < 16384) {
                    Progress q = progress.get(other.getUUID());
                    if (q == null) q = new Progress(data.round, data.level, new HashSet<>(), 30, 0);
                    progress.put(other.getUUID(), new Progress(q.round, q.level, q.rooms, q.rooms.size() + 30 + other.getRandom().nextInt(11), server.getTickCount() + 6000));
                }
                break;
            }
        }
    }
    static HoundEntity candidate(net.minecraft.server.level.ServerLevel level, BlockPos pos, ServerPlayer target, List<ServerPlayer> active) {
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).isSolid()) return null;
        HoundEntity hound = HoundEntity.TYPE.get().create(level);
        if (hound == null) return null;
        hound.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        if (!level.getEntitiesOfClass(Monster.class, hound.getBoundingBox().inflate(96)).isEmpty()) { hound.discard(); return null; }
        if (!level.noCollision(hound) || active.stream().anyMatch(a -> a.distanceToSqr(hound) < 576 || a.hasLineOfSight(hound))) { hound.discard(); return null; }
        // Candidate stands on the validated solid floor before its first physics tick.
        hound.setOnGround(true);
        var path = hound.getNavigation().createPath(target, 0);
        if (path == null || !path.canReach()) { hound.discard(); return null; }
        return hound;
    }
    private EncounterSystem() { }
}
