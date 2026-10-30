package io.github.maniadiaz.bedroom;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder(Bedroom.MOD_ID) @PrefixGameTestTemplate(false)
public final class VictoryGameTests {
    private static void solve(ServerPlayer p,MatchData d){
        for(int i=0;i<3;i++){
            p.getInventory().clearContent();var cache=MissionSystem.cache(d,i);p.setPos(cache.getX()+.5,65,cache.getZ()+2.5);
            MissionSystem.interact(p,cache,InteractionHand.MAIN_HAND);p.getInventory().selected=0;
            var panel=MissionSystem.panel(d,i);p.setPos(panel.getX()+.5,65,panel.getZ()+2.5);MissionSystem.interact(p,panel,InteractionHand.MAIN_HAND);
        }
    }
    @GameTest(template="empty",timeoutTicks=300) public static void completeCampaignThroughBothNaturalExitsWithFourPlayers(GameTestHelper h){
        var server=h.getLevel().getServer();var d=MatchData.get(server);Map<UUID,ServerPlayer> team=new LinkedHashMap<>();
        try{
            for(int i=0;i<4;i++){var p=new FakePlayer(Campaign.level(server,0),new GameProfile(UUID.randomUUID(),"FullRun"+i));MatchController.join(p);team.put(p.getUUID(),p);}
            var host=team.values().iterator().next();d.start(host.getUUID(),team.keySet());MissionSystem.prepare(server,d);
            for(var p:team.values())MatchController.apply(p,d,d.members.get(p.getUUID()),false);
            var teammate=team.keySet().stream().filter(id->!id.equals(host.getUUID())).findFirst().orElseThrow();d.die(teammate);
            for(int level=0;level<2;level++){
                solve(host,d);h.assertTrue(d.mission.ready(),"Actual component interactions must solve each level");
                var exit=MatchArena.exit(d);host.serverLevel().getChunkAt(exit);host.setPos(exit.getX()-.31,65,exit.getZ()+.5);host.setYRot(-90);host.setXRot(0);host.getFoodData().setFoodLevel(20);
                h.assertTrue(NoClipSystem.contact(host)!=null,"Each generated exit must accept physical contact");NoClipSystem.input(host,true);
                for(int tick=0;tick<39;tick++)NoClipSystem.step(server,team);
                h.assertTrue(d.exitDelay==0,"Charge from the previous dimension must never carry over");
                NoClipSystem.step(server,team);h.assertTrue(d.exitDelay==20,"Fortieth tick must schedule one-second transition");
                for(int tick=0;tick<19;tick++)NoClipSystem.step(server,team);
                h.assertTrue(d.phase==MatchData.Phase.ACTIVE&&d.exitDelay==1,"Level must remain active until blackout countdown ends");NoClipSystem.step(server,team);
                if(level==0){
                    h.assertTrue(d.phase==MatchData.Phase.REST&&d.level==1,"First exit must lead to rest");
                    for(var m:d.members.values())h.assertTrue(m.state==MatchData.State.REST&&m.deaths==0,"Team revives between levels");
                    d.resume(host.getUUID());MissionSystem.prepare(server,d);for(var p:team.values())MatchController.apply(p,d,d.members.get(p.getUUID()),true);
                }
            }
            h.assertTrue(d.phase==MatchData.Phase.VICTORY,"Second exit must win the two-level campaign");
            for(var p:team.values())h.assertTrue(d.members.get(p.getUUID()).state==MatchData.State.VICTORY&&p.level().dimension().equals(DevelopmentCommands.LEVEL_0)&&p.getX()<200,"All four members must reach the victory lobby");
            d.restTicks=1;d.tick(team.keySet());h.assertTrue(d.phase==MatchData.Phase.LOBBY,"Celebration expires back to a fresh lobby");h.succeed();
        }finally{for(var p:team.values()){if(MatchController.member(p))MatchController.leave(p);p.discard();}}
    }
    @GameTest(template="empty") public static void celebrationHasRoofClearanceAndRealFireworkEntity(GameTestHelper h){
        var level=Campaign.level(h.getLevel().getServer(),0);Celebration.ensure(level);
        h.assertTrue(level.getBlockState(new BlockPos(Celebration.ROOM+10,68,10)).isAir()&&!level.getBlockState(new BlockPos(Celebration.ROOM+10,78,10)).isAir(),"Victory atrium gives fireworks headroom");
        Celebration.firework(level,0);var rockets=level.getEntitiesOfClass(FireworkRocketEntity.class,new AABB(Celebration.ROOM,64,0,Celebration.ROOM+20,79,20));
        h.assertTrue(!rockets.isEmpty(),"Celebration spawns an actual rocket, not a placeholder message");rockets.forEach(FireworkRocketEntity::discard);h.succeed();
    }
}
