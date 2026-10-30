package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.*;

/** Personal recovery in the unchanged live map. The door is rendered only for its owner. */
final class RecoverySystem {
    static Vec3 returnPoint(ServerPlayer p,MatchData d,MatchData.Member m){
        Vec3 start=new Vec3(MissionSystem.anchor(d)+8.5,65,8.5);
        if(m.checkpoint.contains("x")&&m.checkpoint.getInt("level")==d.level){var candidate=new Vec3(m.checkpoint.getDouble("x"),65,m.checkpoint.getDouble("z"));var level=Campaign.level(p.server,d.level);if(level!=null&&MatchArena.contains(d,BlockPos.containing(candidate))){level.getChunkAt(BlockPos.containing(candidate));if(free(level,BlockPos.containing(candidate),d))return candidate;}}
        return start;
    }
    static boolean free(ServerLevel level,BlockPos pos,MatchData d){return level!=null&&MatchArena.contains(d,pos)&&level.hasChunkAt(pos)&&level.getBlockState(pos.below()).isSolid()&&level.noCollision(null,new AABB(pos.getX()+.2,65,pos.getZ()+.2,pos.getX()+.8,66.8,pos.getZ()+.8));}
    static BlockPos chooseDoor(ServerLevel level,BlockPos from,MatchData d){
        var queue=new ArrayDeque<BlockPos>();var distances=new HashMap<BlockPos,Integer>();queue.add(from);distances.put(from,0);BlockPos best=from;
        while(!queue.isEmpty()&&distances.size()<4096){var pos=queue.remove();int distance=distances.get(pos);if(distance>distances.get(best))best=pos;if(distance>=64)return pos;
            for(var direction:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)){var next=pos.relative(direction);if(!distances.containsKey(next)&&free(level,next,d)){distances.put(next,distance+1);queue.add(next);}}
        }return best;
    }
    static void enter(ServerPlayer p,MatchData d,MatchData.Member m){
        var level=Campaign.level(p.server,d.level);if(level==null)return;d.quest.hiding.remove(p.getUUID());
        var from=p.level()==level&&free(level,new BlockPos(p.getBlockX(),65,p.getBlockZ()),d)?new Vec3(p.getX(),65,p.getZ()):returnPoint(p,d,m);
        level.getChunkAt(BlockPos.containing(from));
        if(m.recoveryDoor.isEmpty()){var door=chooseDoor(level,BlockPos.containing(from),d);m.recoveryDoor=CheckpointSystem.position(door.getX()+.5,65,door.getZ()+.5,d.level);}
        p.setCamera(p);p.teleportTo(level,from.x,65,from.z,p.getYRot(),0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,m.limboTicks+40,0,false,false));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,m.limboTicks+40,0,false,false));
    }
    static Vec3 door(MatchData.Member m){return new Vec3(m.recoveryDoor.getDouble("x"),65,m.recoveryDoor.getDouble("z"));}
    static void tick(ServerPlayer p,MatchData d,MatchData.Member m){
        if(m.state!=MatchData.State.LIMBO||m.recoveryDoor.isEmpty()||p.serverLevel()!=Campaign.level(p.server,d.level))return;
        var target=door(m);double distance=p.position().distanceTo(target);
        if(distance<1.1){if(d.escape(p.getUUID())){MatchController.apply(p,d,m,true);MatchAnnouncements.show(p,"HAS VUELTO","Regresaste al punto guardado. Conserva tus vidas.",net.minecraft.ChatFormatting.AQUA);}return;}
        double close=1-Math.min(1,distance/70);int interval=(int)(14+(1-close)*46);
        if(--m.heartbeatTicks<=0){m.heartbeatTicks=interval;var direction=target.subtract(p.position()).normalize();AtmosphereSystem.personal(p,BedroomSounds.HEARTBEAT.get(),p.position().add(direction.scale(4)),(float)(.4+.5*close),(float)(.8+.25*close));}
    }
    private RecoverySystem(){}
}
