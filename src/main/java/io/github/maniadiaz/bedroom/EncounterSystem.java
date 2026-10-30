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
    static final Map<String,Integer> placementFailures=new LinkedHashMap<>();
    private static <T extends Monster>T reject(T entity,String reason){placementFailures.merge(reason,1,Integer::sum);entity.discard();return null;}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { progress.clear(); }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        step(event.getServer(),event.getServer().getPlayerList().getPlayers(),event.getServer().getTickCount());
    }
    static void step(net.minecraft.server.MinecraftServer server,List<ServerPlayer> players,int now){
        var data = MatchData.get(server);
        if (data.phase != MatchData.Phase.ACTIVE) { progress.clear(); return; }
        var level = Campaign.level(server,data.level);
        if (level == null) return;
        var active = players.stream().filter(p -> p.level() == level && LevelZeroQuest.targetable(p) && p.isAlive() && !p.isSpectator()).toList();
        // Preserve cooldown through limbo and reconnections within this round.
        for (ServerPlayer p : active) {
            Progress old = progress.get(p.getUUID());
            if (old == null || old.round != data.round || old.level != data.level)
                old = new Progress(data.round, data.level, new HashSet<>(), 0, now + data.difficulty.firstEncounterSeconds*20);
            var rooms = old.rooms;
            rooms.add(BlockPos.asLong(Math.floorDiv(p.blockPosition().getX(), 16), 0, Math.floorDiv(p.blockPosition().getZ(), 16)));
            progress.put(p.getUUID(), old);
            if (now < old.cooldown) continue;
            // At most one encounter per nearby group, including other Monster subclasses.
            if (!level.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(96)).isEmpty()) continue;
            // Failed placement also backs off; low sanity never retries every tick.
            progress.put(p.getUUID(), new Progress(old.round, old.level, rooms, 0, now + 100));
            int species = p.getRandom().nextInt(3);
            placementFailures.clear();
            for (int attempt = 0; attempt < 96; attempt++) {
                double angle = p.getRandom().nextDouble() * Math.PI * 2;
                double distance = 18 + p.getRandom().nextInt(17);
                BlockPos pos = BlockPos.containing(p.getX() + Math.cos(angle) * distance, 65, p.getZ() + Math.sin(angle) * distance);
                if (!MatchArena.contains(data,pos)){placementFailures.merge("outside",1,Integer::sum);continue;}
                // A lit map must not permanently suppress an encounter rolled as Smiler.
                Monster threat = switch ((species+attempt/32)%3) {
                    case 1 -> smilerCandidate(level,pos,p,active);
                    case 2 -> bacteriaCandidate(level,pos,p,active);
                    default -> candidate(level,pos,p,active);
                };
                if (threat == null) continue;
                if (threat instanceof HoundEntity hound) { hound.round = data.round; hound.matchLevel = data.level; hound.investigate(p); }
                if (threat instanceof SmilerEntity smiler) { smiler.round = data.round; smiler.matchLevel = data.level; smiler.beginEncounter(p); }
                if (threat instanceof BacteriaEntity bacteria) { bacteria.round = data.round; bacteria.matchLevel = data.level; bacteria.investigate(p); }
                var damage=threat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                var speed=threat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if(damage!=null)damage.setBaseValue(damage.getBaseValue()*(.8+data.difficulty.ordinal()*.1+data.level*.1));
                if(speed!=null)speed.setBaseValue(speed.getBaseValue()*(.9+data.difficulty.ordinal()*.055));
                if(!level.addFreshEntity(threat))continue;
                // Hunters begin investigating a real last-known position rather than waiting
                // motionless outside their short-range noise/vision detection radius.
                if(!(threat instanceof SmilerEntity))threat.getNavigation().moveTo(p, .8);
                for (ServerPlayer other : active) if (other.distanceToSqr(p) < 16384) {
                    Progress q = progress.get(other.getUUID());
                    if (q == null) q = new Progress(data.round, data.level, new HashSet<>(), 30, 0);
                    progress.put(other.getUUID(), new Progress(q.round, q.level, q.rooms, 0, now + Math.max(30,data.difficulty.encounterSeconds-data.level*15)*20));
                }
                break;
            }
        }
    }
    static HoundEntity candidate(net.minecraft.server.level.ServerLevel level, BlockPos pos, ServerPlayer target, List<ServerPlayer> active) {
        return validate(level, pos, target, active, HoundEntity.TYPE.get().create(level));
    }
    static SmilerEntity smilerCandidate(net.minecraft.server.level.ServerLevel level, BlockPos pos, ServerPlayer target, List<ServerPlayer> active) {
        if (!level.hasChunkAt(pos) || !SmilerEntity.dark(level, pos.above())) {placementFailures.merge("smilerLightOrChunk",1,Integer::sum);return null;}
        return validate(level, pos, target, active, SmilerEntity.TYPE.get().create(level));
    }
    static BacteriaEntity bacteriaCandidate(net.minecraft.server.level.ServerLevel level, BlockPos pos, ServerPlayer target, List<ServerPlayer> active) {
        return validate(level,pos,target,active,BacteriaEntity.TYPE.get().create(level));
    }
    private static <T extends Monster> T validate(net.minecraft.server.level.ServerLevel level, BlockPos pos, ServerPlayer target, List<ServerPlayer> active, T hound) {
        if (hound == null) return null;
        if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).isSolid()) return reject(hound,"floorOrChunk");
        hound.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        if (!level.getEntitiesOfClass(Monster.class, hound.getBoundingBox().inflate(96)).isEmpty()) return reject(hound,"existingThreat");
        if (!level.noCollision(hound))return reject(hound,"collision");
        if(active.stream().anyMatch(a -> a.distanceToSqr(hound) < 144 || a.hasLineOfSight(hound)))return reject(hound,"visibleOrClose");
        // Candidate stands on the validated solid floor before its first physics tick.
        hound.setOnGround(true);
        var path = hound.getNavigation().createPath(target, 0);
        if (path == null || !path.canReach()) return reject(hound,"unreachable");
        return hound;
    }
    private EncounterSystem() { }
}
