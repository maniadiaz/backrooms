package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

final class CheckpointSystem {
    static BlockPos station(MatchData d){return new BlockPos(MissionSystem.anchor(d)+MatchArena.aisle(MatchArena.size(d)/2),65,MatchArena.aisle(MatchArena.size(d)/2));}
    static void prepare(net.minecraft.server.MinecraftServer server,MatchData d){
        var level=Campaign.level(server,d.level);if(level==null)return;
        var center=station(d);for(int y=0;y<3;y++)for(int x=-1;x<=2;x++)for(int z=-1;z<=2;z++)level.setBlock(center.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
        level.setBlock(center,QuestContent.CHECKPOINT.get().defaultBlockState(),3);
        level.setBlock(new BlockPos(MissionSystem.anchor(d)+11,65,8),QuestContent.CHECKPOINT.get().defaultBlockState(),3);
    }
    static CompoundTag position(double x,double y,double z,int level){var t=new CompoundTag();t.putDouble("x",x);t.putDouble("y",y);t.putDouble("z",z);t.putInt("level",level);return t;}
    static CompoundTag safePosition(ServerPlayer p,MatchData d){
        for(int radius=0;radius<=4;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            var pos=new BlockPos(p.getBlockX()+x,65,p.getBlockZ()+z);if(RecoverySystem.free(p.serverLevel(),pos,d))return position(pos.getX()+.5,65,pos.getZ()+.5,d.level);
        }return position(MissionSystem.anchor(d)+8.5,65,8.5,d.level);
    }
    static boolean save(ServerPlayer actor){var players=new LinkedHashMap<UUID,ServerPlayer>();actor.server.getPlayerList().getPlayers().forEach(p->players.put(p.getUUID(),p));players.putIfAbsent(actor.getUUID(),actor);return save(actor,players);}
    static boolean save(ServerPlayer actor,Map<UUID,ServerPlayer> players){
        var d=MatchData.get(actor.server);
        if(!actor.getUUID().equals(d.host)||(d.phase!=MatchData.Phase.ACTIVE&&d.phase!=MatchData.Phase.REST)||d.exitDelay>0){LevelZeroQuest.say(actor,"Solo el anfitrión puede guardar una partida activa o en descanso, fuera de una transición.");return false;}
        if(!d.quest.hiding.isEmpty()){LevelZeroQuest.say(actor,"Terminen el escondite antes de guardar el checkpoint.");return false;}
        for(var m:d.members.values()){
            var p=players.get(m.id);if(p==null)continue;
            if(p.serverLevel()==Campaign.level(actor.server,d.level))m.location=position(p.getX(),p.getY(),p.getZ(),d.level);
            if(m.state==MatchData.State.ACTIVE||m.state==MatchData.State.REST){
                m.inventory=MatchController.inventorySnapshot(p);
                m.checkpoint=m.state==MatchData.State.ACTIVE?safePosition(p,d):position(MissionSystem.anchor(d)+8.5,65,8.5,d.level);
            }
        }
        var store=CheckpointData.get(actor.server);store.snapshot=d.save(new CompoundTag());store.highestRound=Math.max(store.highestRound,d.round);store.setDirty();d.setDirty();actor.server.overworld().getDataStorage().save();
        MatchAnnouncements.show(actor,"CHECKPOINT GUARDADO",Campaign.name(d.level)+" — progreso, vidas e inventarios guardados.",net.minecraft.ChatFormatting.AQUA);return true;
    }
    static boolean load(ServerPlayer actor){var players=new LinkedHashMap<UUID,ServerPlayer>();actor.server.getPlayerList().getPlayers().forEach(p->players.put(p.getUUID(),p));players.putIfAbsent(actor.getUUID(),actor);return load(actor,players);}
    static boolean load(ServerPlayer actor,Map<UUID,ServerPlayer> players){
        var d=MatchData.get(actor.server);var store=CheckpointData.get(actor.server);
        if(d.phase!=MatchData.Phase.LOBBY||!actor.getUUID().equals(d.host)||store.snapshot.isEmpty()){LevelZeroQuest.say(actor,"El anfitrión puede continuar desde el lobby cuando exista un checkpoint guardado.");return false;}
        if(!players.keySet().containsAll(d.members.keySet())){LevelZeroQuest.say(actor,"Todos los integrantes del lobby deben estar conectados.");return false;}
        var saved=MatchData.load(store.snapshot.copy());
        if(saved.phase!=MatchData.Phase.ACTIVE&&saved.phase!=MatchData.Phase.REST)return false;
        boolean survivor=d.members.keySet().stream().anyMatch(id->{var m=saved.members.get(id);return m==null||m.deaths<MatchData.MAX_DEATHS&&(m.state==MatchData.State.ACTIVE||m.state==MatchData.State.LIMBO||m.state==MatchData.State.REST);});
        if(!survivor){LevelZeroQuest.say(actor,"Los presentes no tienen un participante recuperable en ese guardado. Empiecen desde cero o reúnan al equipo guardado.");return false;}
        for(var key:List.of(DevelopmentCommands.LEVEL_0,Campaign.LEVEL_1,Campaign.DARKROOMS)){
            var level=actor.server.getLevel(key);if(level!=null)java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false).filter(entity->entity instanceof HoundEntity||entity instanceof SmilerEntity||entity instanceof BacteriaEntity).toList().forEach(net.minecraft.world.entity.Entity::discard);
        }
        d.phase=saved.phase;d.round=saved.round;d.level=saved.level;d.difficulty=saved.difficulty;d.restTicks=saved.restTicks;d.nextDestination=saved.nextDestination;d.quest=saved.quest;d.mission=saved.mission;d.exitDelay=0;d.awaitingPlayers=false;d.vote.clear();
        for(var m:d.members.values()){
            var old=saved.members.get(m.id);var p=players.get(m.id);
            if(old!=null){m.deaths=old.deaths;m.inventory=old.inventory.copy();m.survival=old.survival;m.checkpoint=old.checkpoint.copy();m.recoveryDoor=old.recoveryDoor.copy();m.location=old.location.copy();m.limboTicks=old.limboTicks;m.state=old.state;}
            else{m.deaths=0;m.inventory=new CompoundTag();m.survival=new SurvivalState();m.checkpoint=new CompoundTag();m.state=d.phase==MatchData.Phase.REST?MatchData.State.REST:MatchData.State.ACTIVE;}
            if(m.deaths>=MatchData.MAX_DEATHS)m.state=MatchData.State.SPECTATING;
            if(m.state==MatchData.State.LIMBO&&m.location.contains("x")){
                var level=Campaign.level(actor.server,d.level);var pos=BlockPos.containing(m.location.getDouble("x"),65,m.location.getDouble("z"));
                if(MatchArena.contains(d,pos)){level.getChunkAt(pos);if(RecoverySystem.free(level,pos,d))p.teleportTo(level,m.location.getDouble("x"),65,m.location.getDouble("z"),0,0);else m.recoveryDoor=new CompoundTag();}
            }
            MatchController.apply(p,d,m,old!=null);
            if(old==null&&(m.state==MatchData.State.ACTIVE||m.state==MatchData.State.REST))MatchController.starter(p);
            if(m.state==MatchData.State.ACTIVE){p.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);if(m.inventory.contains("health"))p.setHealth(Math.max(1,Math.min(p.getMaxHealth(),m.inventory.getFloat("health"))));}
            MatchAnnouncements.show(p,"CHECKPOINT CARGADO",Campaign.name(d.level)+" — continúen desde el último guardado.",net.minecraft.ChatFormatting.AQUA);
        }
        MissionSystem.prepare(actor.server,d);d.setDirty();return true;
    }
    private CheckpointSystem(){}
}
