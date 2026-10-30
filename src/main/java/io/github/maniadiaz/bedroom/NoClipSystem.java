package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Bedroom.MOD_ID)
public final class NoClipSystem {
    static final int REQUIRED=40;
    static final class Charge {
        BlockPos wall;
        int ticks;
        boolean advance(BlockPos current,boolean allowed) {
            if(!allowed||current==null) { wall=null; ticks=0; return false; }
            if(!current.equals(wall)) { wall=current.immutable(); ticks=0; }
            return ++ticks>=REQUIRED;
        }
    }
    private static final class Input { boolean pressing; int received,round=-1,level=-1; final Charge charge=new Charge(); }
    private static final Map<UUID,Input> inputs=new HashMap<>();
    static void input(ServerPlayer p,boolean pressing) {
        if(!SurvivalSystem.active(p)||!Campaign.dungeon(p.level().dimension()))return;
        var input=inputs.computeIfAbsent(p.getUUID(),id->new Input());var d=MatchData.get(p.server);
        if(input.round!=d.round||input.level!=d.level){input.charge.advance(null,false);input.round=d.round;input.level=d.level;}
        input.pressing=pressing; input.received=p.server.getTickCount();
        if(!pressing)input.charge.advance(null,false);
    }
    static int charge(UUID id) { var in=inputs.get(id);return in==null?0:Math.min(REQUIRED,in.charge.ticks); }
    static BlockPos contact(ServerPlayer p) {
        if(!p.isAlive()||p.isSpectator()||p.getAbilities().flying)return null;
        Vec3 from=p.getEyePosition(),to=from.add(p.getLookAngle().scale(2));
        var hit=p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.BLOCK||!Campaign.exit(p.level().getBlockState(hit.getBlockPos())))return null;
        return p.getBoundingBox().inflate(.08,0,.08).intersects(new AABB(hit.getBlockPos()))?new BlockPos(hit.getBlockPos().getX(),65,hit.getBlockPos().getZ()):null;
    }
    static boolean advance(ServerPlayer p,Charge charge,boolean intent) {
        var d=MatchData.get(p.server);
        return charge.advance(contact(p),intent&&!LevelZeroQuest.hidden(p)&&p.isShiftKeyDown()&&SurvivalSystem.active(p)&&p.serverLevel()==Campaign.level(p.server,d.level)&&Campaign.dungeon(p.level().dimension())&&MissionSystem.exitReady(d));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var server=event.getServer();Map<UUID,ServerPlayer> players=new LinkedHashMap<>();server.getPlayerList().getPlayers().forEach(p->players.put(p.getUUID(),p));step(server,players);
    }
    static void step(net.minecraft.server.MinecraftServer server,Map<UUID,ServerPlayer> players){
        var d=MatchData.get(server);
        if(d.phase!=MatchData.Phase.ACTIVE){inputs.clear();return;}
        if(d.exitDelay>0) {
            if(--d.exitDelay==0) {
                var actor=players.values().stream().filter(SurvivalSystem::active).findFirst().orElse(null);
                if(actor!=null&&MissionSystem.exitReady(d))MatchController.complete(actor,false,players);
            }
            return;
        }
        inputs.entrySet().removeIf(entry->{var p=players.get(entry.getKey());return p==null||!SurvivalSystem.active(p);});
        for(var entry:inputs.entrySet()) {
            var p=players.get(entry.getKey());var in=entry.getValue();
            if(p!=null&&advance(p,in.charge,in.pressing&&in.round==d.round&&in.level==d.level&&server.getTickCount()-in.received<=15)) {
                d.nextDestination=d.level==0?2:1;d.exitDelay=20;
                for(var member:d.members.values()) {
                    var other=players.get(member.id);if(other!=null)BedroomNetwork.blackout(other);
                }
                break;
            }
            if(p!=null&&in.charge.ticks>0&&server.getTickCount()%5==0)BedroomNetwork.sync(p);
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event){inputs.clear();}
    private NoClipSystem(){}
}
