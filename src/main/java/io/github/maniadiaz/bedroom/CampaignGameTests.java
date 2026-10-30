package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.gametest.*;

@GameTestHolder(Bedroom.MOD_ID) @PrefixGameTestTemplate(false)
public final class CampaignGameTests {
    @GameTest(template="empty") public static void oldWarehouseLampsUpgradeWithoutBlockingRunningJumps(GameTestHelper h){
        var level=Campaign.level(h.getLevel().getServer(),1);var pos=new BlockPos(26008,68,26008);
        var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"JumpClearance"));
        try{
            level.setBlock(pos,Bedroom.LIGHT.get().defaultBlockState(),3);
            for(int y=69;y<=71;y++)level.setBlock(new BlockPos(pos.getX(),y,pos.getZ()),net.minecraft.world.level.block.Blocks.CHAIN.defaultBlockState(),3);
            WarehouseLighting.upgrade(level,level.getChunkAt(pos));
            h.assertTrue(level.getBlockState(pos).isAir()&&level.getBlockState(pos.above()).isAir()&&level.getBlockState(pos.above(2)).is(Bedroom.LIGHT.get()),"Existing lamp and low chain move out of the jump path");
            p.setPos(pos.getX()+.5,66.25,pos.getZ()+.5);
            h.assertTrue(level.noCollision(p),"Standing player at jump apex fits below the actual upgraded lamp");
            p.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(0,0,1));
            h.assertTrue(p.getZ()>pos.getZ()+1.4&&!p.horizontalCollision,"Forward movement at jump height must not hit lamp supports");
            WarehouseLighting.upgrade(level,level.getChunkAt(pos));
            h.assertTrue(level.getBlockState(pos.above(2)).is(Bedroom.LIGHT.get()),"Reloading an upgraded chunk preserves the raised lamp");
            h.assertTrue(Campaign.lampY(level)==70,"Outage logic must target the raised lamps");h.succeed();
        }finally{p.discard();}
    }
    @GameTest(template="empty") public static void warehouseDimensionPaletteAndAislesConnect(GameTestHelper h){
        var level=Campaign.level(h.getLevel().getServer(),1);
        h.assertTrue(level!=null&&level.getChunkSource().getGenerator() instanceof Level1Generator,"Warehouse must load as its own dimension and generator");
        var generator=(Level1Generator)level.getChunkSource().getGenerator();
        h.assertTrue(generator.stateAt(8,64,8).is(Bedroom.CONCRETE.get())||generator.stateAt(8,64,8).is(Bedroom.WET_CONCRETE.get()),"Warehouse has concrete rather than carpet");
        h.assertTrue(generator.stateAt(8,72,8).is(Bedroom.STEEL.get())&&generator.stateAt(8,71,8).is(net.minecraft.world.level.block.Blocks.CHAIN),"High metal ceiling and suspended lamps exist");
        h.assertTrue(generator.stateAt(8,68,8).isAir()&&generator.stateAt(8,69,8).isAir()&&!generator.stateAt(8,70,8).isAir(),"Raised lamps leave running-jump headroom");
        boolean[][] seen=new boolean[128][128];var queue=new ArrayDeque<BlockPos>();queue.add(new BlockPos(8,65,8));seen[8][8]=true;
        while(!queue.isEmpty()){
            var p=queue.remove();for(var direction:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)){
                var n=p.relative(direction);int x=n.getX(),z=n.getZ();if(x<0||x>=128||z<0||z>=128||seen[x][z])continue;
                if(!generator.stateAt(x,65,z).isAir()||!generator.stateAt(x,66,z).isAir())continue;
                seen[x][z]=true;queue.add(n);
            }
        }
        for(int[] point:new int[][]{{23,24},{87,40},{39,88},{103,104},{107,104},{111,104},{114,115}})
            h.assertTrue(seen[point[0]][point[1]],"Each mission node and exit must have a reachable approach");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void fourParticipantsReviveRestThenEnterRealLevelOne(GameTestHelper h){
        var server=h.getLevel().getServer();var data=MatchData.get(server);Map<UUID,ServerPlayer> team=new LinkedHashMap<>();
        try{
            for(int i=0;i<4;i++){var p=new FakePlayer(Campaign.level(server,0),new GameProfile(UUID.randomUUID(),"Campaign"+i));MatchController.join(p);team.put(p.getUUID(),p);}
            var host=team.values().iterator().next();data.start(host.getUUID(),team.keySet());
            var ids=new ArrayList<>(team.keySet());data.die(ids.get(1));for(int death=0;death<MatchData.MAX_DEATHS;death++){data.die(ids.get(2));if(death<MatchData.MAX_DEATHS-1)data.escape(ids.get(2));}
            h.assertTrue(MatchController.complete(host,false,team),"Normal Level 0 exit must start real progression");
            h.assertTrue(data.phase==MatchData.Phase.REST&&data.level==1&&data.restTicks==1800,"Ninety-second rest precedes Level 1");
            for(var m:data.members.values())h.assertTrue(m.state==(m.deaths>=10?MatchData.State.SPECTATING:MatchData.State.REST)&&m.survival.sanity==100,"Only participants with lives revive; deaths are preserved across levels");
            h.assertTrue(data.resume(host.getUUID()),"Host button can skip rest");MissionSystem.prepare(server,data);
            for(var p:team.values()){MatchController.apply(p,data,data.members.get(p.getUUID()),true);h.assertTrue(data.members.get(p.getUUID()).deaths>=10?p.isSpectator():p.level().dimension().equals(Campaign.LEVEL_1),"Living participants enter warehouse; exhausted participants stay spectators");}
            h.succeed();
        }finally{for(var p:team.values()){if(MatchController.member(p))MatchController.leave(p);p.discard();}}
    }
    @GameTest(template="empty",timeoutTicks=200) public static void relayMissionRejectsLevelZeroFuses(GameTestHelper h){
        var server=h.getLevel().getServer();var p=new FakePlayer(Campaign.level(server,0),new GameProfile(UUID.randomUUID(),"Relays"));
        try{
            MatchController.join(p);var d=MatchData.get(server);d.start(p.getUUID(),Set.of(p.getUUID()));d.mission.leases[0]=1;var old=MissionSystem.fuse(d,0,1);
            d.complete(p.getUUID(),true);d.resume(p.getUUID());MissionSystem.prepare(server,d);MatchController.apply(p,d,d.members.get(p.getUUID()),false);
            h.assertTrue(!MissionSystem.valid(old,d,0),"Level-zero item cannot solve warehouse objective");
            for(int i=0;i<3;i++){
                p.getInventory().clearContent();var pos=MissionSystem.cache(d,i);p.setPos(pos.getX()+.5,65,pos.getZ()+2.5);MissionSystem.interact(p,pos,InteractionHand.MAIN_HAND);
                h.assertTrue(p.getInventory().getItem(0).is(Bedroom.RELAY.get()),"Warehouse supplies a relay rather than a fuse");
                p.getInventory().selected=0;pos=MissionSystem.panel(d,i);p.setPos(pos.getX()+.5,65,pos.getZ()+2.5);MissionSystem.interact(p,pos,InteractionHand.MAIN_HAND);
            }
            h.assertTrue(d.mission.ready(),"Three warehouse relays unlock loading gate");h.succeed();
        }finally{if(MatchController.member(p))MatchController.leave(p);p.discard();}
    }
}
