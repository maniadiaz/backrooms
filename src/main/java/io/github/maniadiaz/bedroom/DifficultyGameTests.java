package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

@GameTestHolder(Bedroom.MOD_ID) @PrefixGameTestTemplate(false)
public final class DifficultyGameTests {
    @GameTest(template="empty") public static void controlGuidanceUsesPlayerOrientation(GameTestHelper h){
        var p=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Guidance"));
        try{
            p.setPos(.5,65,.5);p.setYRot(0);
            h.assertTrue(MatchArena.direction(p,new BlockPos(0,65,10)).startsWith("DELANTE"),"South is ahead at zero yaw");
            h.assertTrue(MatchArena.direction(p,new BlockPos(-10,65,0)).startsWith("DERECHA"),"West is right when facing south");
            h.assertTrue(MatchArena.direction(p,new BlockPos(10,65,0)).startsWith("IZQUIERDA"),"East is left when facing south");
            p.setYRot(90);h.assertTrue(MatchArena.direction(p,new BlockPos(-10,65,0)).startsWith("DELANTE"),"Guidance rotates with the player");h.succeed();
        }finally{p.discard();}
    }
    @GameTest(template="empty") public static void boundedMazesKeepEveryObjectiveReachable(GameTestHelper h){
        var d=new MatchData();d.round=29;
        for(int stage=0;stage<2;stage++)for(var difficulty:MatchDifficulty.values()){
            d.level=stage;d.difficulty=difficulty;int n=MatchArena.size(d),origin=MatchArena.origin(d);
            var generator=(Level0Generator)Campaign.level(h.getLevel().getServer(),stage).getChunkSource().getGenerator();
            boolean[] blocked=new boolean[n*n],seen=new boolean[n*n];int[] queue=new int[n*n];
            var exit=MatchArena.exit(d);
            for(int z=0;z<n;z++)for(int x=0;x<n;x++){
                boolean room=Math.abs(origin+x-exit.getX())<=4&&Math.abs(z-exit.getZ())<=4;
                blocked[z*n+x]=x==0||z==0||x==n-1||z==n-1||(!room&&!generator.stateAt(origin+x,65,z).isAir());
            }
            blocked[exit.getZ()*n+exit.getX()-origin]=true;
            for(int i=0;i<3;i++)for(var node:List.of(MissionSystem.cache(d,i),MissionSystem.panel(d,i)))blocked[node.getZ()*n+node.getX()-origin]=true;
            int head=0,tail=1;queue[0]=8*n+8;seen[queue[0]]=true;
            while(head<tail){int at=queue[head++];for(int next:new int[]{at-1,at+1,at-n,at+n}){
                if(next<0||next>=n*n||blocked[next]||seen[next])continue;
                seen[next]=true;queue[tail++]=next;
            }}
            for(int i=0;i<3;i++)for(var node:List.of(MissionSystem.cache(d,i),MissionSystem.panel(d,i)))
                h.assertTrue(seen[(node.getZ()+1)*n+node.getX()-origin],"Reachable approach: level "+stage+" "+difficulty.id+" "+node);
            h.assertTrue(seen[exit.getZ()*n+exit.getX()-origin-1],"Exit has a reachable running approach");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void onlyHostCanChooseDifficultyAndActiveAttemptsStayLocked(GameTestHelper h)throws Exception{
        var host=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DifficultyHost"));
        var guest=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DifficultyGuest"));
        var d=MatchData.get(host.server);var previous=d.difficulty;
        try{
            MatchController.join(host);MatchController.join(guest);var dispatcher=host.server.getCommands().getDispatcher();
            h.assertTrue(dispatcher.execute("backrooms difficulty hardcore",guest.createCommandSourceStack().withPermission(0))==0,"Guest cannot override the host");
            h.assertTrue(dispatcher.execute("backrooms difficulty hardcore",host.createCommandSourceStack().withPermission(0))==1&&d.difficulty==MatchDifficulty.HARDCORE,"Host can select Hardcore without operator");
            d.start(host.getUUID(),Set.of(host.getUUID(),guest.getUUID()));
            h.assertTrue(dispatcher.execute("backrooms difficulty facil",host.createCommandSourceStack().withPermission(4))==0&&d.difficulty==MatchDifficulty.HARDCORE,"Even an operator cannot resize an active attempt");h.succeed();
        }finally{if(MatchController.member(host))MatchController.leave(host);if(MatchController.member(guest))MatchController.leave(guest);d.difficulty=previous;d.setDirty();host.discard();guest.discard();}
    }
    @GameTest(template="empty") public static void sizesObjectivesAndSanityScaleAcrossAllDifficulties(GameTestHelper h){
        int[] sizes={100,150,250,350,500};double previousLoss=0;
        var d=new MatchData();d.round=17;
        for(var difficulty:MatchDifficulty.values()){
            d.difficulty=difficulty;h.assertTrue(MatchArena.size(d)==sizes[difficulty.ordinal()],"Requested finite dimensions");
            for(int i=0;i<3;i++){
                h.assertTrue(MatchArena.contains(d,MissionSystem.cache(d,i))&&MatchArena.contains(d,MissionSystem.panel(d,i)),"Every objective must be inside the selected arena");
                h.assertTrue(MissionSystem.panel(d,i).distSqr(MatchArena.exit(d))<20,"All panels must be visible together by the exit");
            }
            h.assertTrue(!MatchArena.contains(d,new BlockPos(MatchArena.origin(d),65,7))&&!MatchArena.contains(d,new BlockPos(MatchArena.origin(d)+sizes[difficulty.ordinal()]-1,65,7)),"Both borders are excluded");
            var sanity=new SurvivalState();for(int t=0;t<200;t++)sanity.advance(true,true,false,true,difficulty.sanityMultiplier);
            double loss=100-sanity.sanity;h.assertTrue(loss>previousLoss&&loss<=3*difficulty.sanityMultiplier+.0001,"Higher difficulty increases bounded sanity pressure");previousLoss=loss;
            var restored=MatchData.load(d.save(new net.minecraft.nbt.CompoundTag()));h.assertTrue(restored.difficulty==difficulty,"Difficulty survives save/load");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=400) public static void timedEncounterActuallySpawnsInsideFiniteArena(GameTestHelper h){
        var server=h.getLevel().getServer();var level=Campaign.level(server,0);
        var p=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RealEncounter"));
        var d=MatchData.get(server);List<Monster> spawned=new ArrayList<>();
        try{
            MatchController.join(p);d.difficulty=MatchDifficulty.FACIL;d.start(p.getUUID(),Set.of(p.getUUID()));MissionSystem.prepare(server,d);
            MatchController.apply(p,d,d.members.get(p.getUUID()),false);
            // FakePlayer deliberately ignores network teleports; position the fixture explicitly.
            p.setPos(MatchArena.origin(d)+8.5,65,8.5);
            var area=new AABB(MatchArena.origin(d),64,0,MatchArena.origin(d)+150,73,150);
            level.getEntitiesOfClass(Monster.class,area).forEach(Monster::discard);
            for(int x=0;x<150;x+=16)for(int z=0;z<150;z+=16)level.getChunkAt(new BlockPos(MatchArena.origin(d)+x,65,z));
            p.getRandom().setSeed(91284);EncounterSystem.step(server,List.of(p),0);
            h.assertTrue(level.getEntitiesOfClass(Monster.class,area).isEmpty(),"No encounter before initial grace period");
            for(int now=1200;now<=1800;now+=100){EncounterSystem.step(server,List.of(p),now);spawned=level.getEntitiesOfClass(Monster.class,area);if(!spawned.isEmpty())break;}
            h.assertTrue(spawned.size()==1,"Timed production encounter must spawn one real entity without exploring 30 rooms: "+EncounterSystem.placementFailures+" position="+p.blockPosition()+" active="+SurvivalSystem.active(p));
            var entity=spawned.get(0);h.assertTrue(MatchArena.contains(d,entity.blockPosition())&&entity.distanceToSqr(p)>=144&&!p.hasLineOfSight(entity),"Encounter stays in bounds, away from players and out of sight");
            EncounterSystem.step(server,List.of(p),1900);h.assertTrue(level.getEntitiesOfClass(Monster.class,area).size()==1,"Shared encounter budget prevents stacking");
            h.assertTrue(!level.getBlockState(new BlockPos(MatchArena.origin(d),65,8)).isAir(),"Actual generated perimeter blocks the old infinite corridor");
            h.succeed();
        }finally{spawned.forEach(Monster::discard);if(MatchController.member(p))MatchController.leave(p);p.discard();}
    }
}
