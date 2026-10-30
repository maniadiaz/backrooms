package io.github.maniadiaz.bedroom;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder(Bedroom.MOD_ID) @PrefixGameTestTemplate(false)
public final class LevelZeroQuestGameTests {
    @GameTest(template="empty",timeoutTicks=500) public static void finiteBeginnerMapConnectsFiveMissionRoomsAndGatesStorage(GameTestHelper h){
        var data=MatchData.get(h.getLevel().getServer());var previous=data.difficulty;data.difficulty=MatchDifficulty.PRINCIPIANTE;
        var p=begin(h,"QuestRoutes");var d=MatchData.get(p.server);
        try{
            var before=reachable(p.serverLevel(),d);
            for(var target:List.of(LevelZeroQuest.note(d,0),LevelZeroQuest.locker(d),LevelZeroQuest.note(d,1),LevelZeroQuest.security(d),LevelZeroQuest.note(d,3),LevelZeroQuest.note(d,4)))
                h.assertTrue(approach(before,d,target),"Connected approach to mission room: "+target);
            h.assertTrue(!before[index(d,LevelZeroQuest.storage(d))],"Storage remains physically sealed before the color solution");
            solve(p,d);var after=reachable(p.serverLevel(),d);
            h.assertTrue(after[index(d,LevelZeroQuest.storage(d))]&&approach(after,d,LevelZeroQuest.note(d,2)),"Correct sequence opens a real walkable storage entrance");
            for(var difficulty:MatchDifficulty.values()){
                var geometry=new MatchData();geometry.round=d.round;geometry.difficulty=difficulty;
                for(int i=0;i<5;i++)h.assertTrue(MatchArena.contains(geometry,LevelZeroQuest.note(geometry,i)),"All notes remain inside every requested map size");
                for(var center:List.of(LevelZeroQuest.puzzle(geometry),LevelZeroQuest.storage(geometry),LevelZeroQuest.security(geometry),LevelZeroQuest.radiation(geometry),LevelZeroQuest.door(geometry)))
                    h.assertTrue(MatchArena.contains(geometry,center.offset(-5,0,-5))&&MatchArena.contains(geometry,center.offset(5,0,5)),"Special room fits within the arena perimeter");
            }
            h.succeed();
        }finally{clean(p);data.difficulty=previous;data.setDirty();}
    }
    private static int index(MatchData d,BlockPos p){return p.getZ()*MatchArena.size(d)+p.getX()-MissionSystem.anchor(d);}
    private static boolean approach(boolean[] seen,MatchData d,BlockPos p){for(var direction:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST))if(seen[index(d,p.relative(direction))])return true;return false;}
    private static boolean[] reachable(ServerLevel level,MatchData d){
        int n=MatchArena.size(d),origin=MissionSystem.anchor(d);boolean[] blocked=new boolean[n*n],seen=new boolean[n*n];var queue=new ArrayDeque<Integer>();
        for(int z=0;z<n;z++)for(int x=0;x<n;x++){var pos=new BlockPos(origin+x,65,z);blocked[z*n+x]=!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()||!level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty();}
        int start=8*n+8;seen[start]=true;queue.add(start);
        while(!queue.isEmpty()){int i=queue.remove(),x=i%n,z=i/n;for(int next:new int[]{x>0?i-1:-1,x<n-1?i+1:-1,z>0?i-n:-1,z<n-1?i+n:-1})if(next>=0&&!blocked[next]&&!seen[next]){seen[next]=true;queue.add(next);}}
        return seen;
    }
    static void click(ServerPlayer p,BlockPos pos){p.setPos(pos.getX()+.5,65,pos.getZ()+2.5);LevelZeroQuest.interact(p,pos,InteractionHand.MAIN_HAND);}
    static void suit(ServerPlayer p,MatchData d){
        for(var entry:Map.of(EquipmentSlot.HEAD,QuestContent.HELMET.get(),EquipmentSlot.CHEST,QuestContent.SUIT.get(),EquipmentSlot.FEET,QuestContent.BOOTS.get()).entrySet()){
            for(int i=0;i<p.getInventory().getContainerSize();i++)if(LevelZeroQuest.valid(p.getInventory().getItem(i),entry.getValue(),d)){var stack=p.getInventory().removeItemNoUpdate(i);p.setItemSlot(entry.getKey(),stack);break;}
        }
    }
    static void solve(ServerPlayer p,MatchData d){
        click(p,LevelZeroQuest.note(d,0));
        var water=new ItemStack(Bedroom.ALMOND_WATER.get());Bedroom.ALMOND_WATER.get().finishUsingItem(water,p.level(),p);
        click(p,LevelZeroQuest.locker(d));p.setPos(LevelZeroQuest.locker(d).getX()+.5,65,LevelZeroQuest.locker(d).getZ()+.5);
        for(int i=0;i<200;i++)LevelZeroQuest.tick(p);
        click(p,LevelZeroQuest.note(d,1));for(int i=0;i<4;i++)click(p,LevelZeroQuest.color(d,i));
        click(p,LevelZeroQuest.note(d,2));click(p,LevelZeroQuest.storage(d).offset(-1,0,0));
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(QuestContent.WRENCH.get())){p.setItemInHand(InteractionHand.MAIN_HAND,p.getInventory().removeItemNoUpdate(i));break;}
        click(p,LevelZeroQuest.security(d));suit(p,d);LevelZeroQuest.tick(p);
        click(p,LevelZeroQuest.note(d,3));click(p,LevelZeroQuest.radiation(d));click(p,LevelZeroQuest.note(d,4));
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(QuestContent.KEY.get())){p.setItemInHand(InteractionHand.MAIN_HAND,p.getInventory().removeItemNoUpdate(i));break;}
    }
    static ServerPlayer begin(GameTestHelper h,String name){
        var level=Campaign.level(h.getLevel().getServer(),0);var p=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),name));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND),p){@Override public void send(net.minecraft.network.protocol.Packet<?> packet){}};
        MatchController.join(p);var d=MatchData.get(p.server);d.start(p.getUUID(),Set.of(p.getUUID()));MissionSystem.prepare(p.server,d);
        p.setPos(MissionSystem.anchor(d)+8.5,65,8.5);for(int i=0;i<65;i++)p.tick();p.setHealth(20);return p;
    }
    static void clean(ServerPlayer p){var d=MatchData.get(p.server);var level=Campaign.level(p.server,d.level);if(level!=null)level.getEntitiesOfClass(Monster.class,new net.minecraft.world.phys.AABB(MissionSystem.anchor(d)-20,60,-20,MissionSystem.anchor(d)+MatchArena.size(d)+20,78,MatchArena.size(d)+20)).forEach(Monster::discard);if(MatchController.member(p))MatchController.leave(p);p.discard();}
    @GameTest(template="empty",timeoutTicks=400) public static void fiveMissionsUseRealItemsAndUnlockOnlyAfterEveryTask(GameTestHelper h){
        var p=begin(h,"FiveMissions");var d=MatchData.get(p.server);
        try{
            click(p,LevelZeroQuest.radiation(d));h.assertTrue(!d.quest.console&&!MissionSystem.exitReady(d),"Console and exit must be locked before tutorial and suit");
            solve(p,d);h.assertTrue(d.quest.notes==31&&d.quest.tutorial()&&d.quest.puzzle&&d.quest.equipped&&d.quest.console&&MissionSystem.exitReady(d),"Real note, water, locker, puzzle, toolbox, armor and console interactions solve the campaign");
            h.assertTrue(LevelZeroQuest.protectedBySuit(p)&&LevelZeroQuest.valid(p.getMainHandItem(),QuestContent.KEY.get(),d),"Player actually equips all three pieces and holds the key");
            var copy=MatchData.load(d.save(new net.minecraft.nbt.CompoundTag()));h.assertTrue(copy.quest.ready()&&copy.quest.notes==31,"Shared mission state persists accurately");
            d.reset();h.assertTrue(!d.quest.ready()&&d.quest.notes==0&&!MissionSystem.exitReady(d),"Restart clears every mission gate");h.succeed();
        }finally{clean(p);}
    }
    @GameTest(template="empty",timeoutTicks=400) public static void lockerNeedsTenContinuousSecondsAndReleasesThreatTargets(GameTestHelper h){
        var p=begin(h,"Locker");var d=MatchData.get(p.server);
        try{
            click(p,LevelZeroQuest.note(d,0));LevelZeroQuest.drank(p);click(p,LevelZeroQuest.locker(d));p.setPos(LevelZeroQuest.locker(d).getX()+.5,65,LevelZeroQuest.locker(d).getZ()+.5);
            var threat=SmilerEntity.TYPE.get().create(p.level());h.assertTrue(LevelZeroQuest.hidden(p)&&!threat.valid(p),"A hidden active participant is excluded from threat targeting");threat.discard();
            for(int i=0;i<199;i++)LevelZeroQuest.tick(p);h.assertTrue(!d.quest.hidden,"Hiding cannot complete before 200 active ticks");
            p.setPos(p.getX()+4,65,p.getZ());LevelZeroQuest.tick(p);h.assertTrue(!LevelZeroQuest.hidden(p)&&!d.quest.hidden,"Walking out cancels the tutorial timer");
            click(p,LevelZeroQuest.locker(d));p.setPos(LevelZeroQuest.locker(d).getX()+.5,65,LevelZeroQuest.locker(d).getZ()+.5);for(int i=0;i<200;i++)LevelZeroQuest.tick(p);
            h.assertTrue(d.quest.hidden&&!LevelZeroQuest.hidden(p)&&LevelZeroQuest.targetable(p),"After ten seconds the tutorial completes and normal targeting resumes");h.succeed();
        }finally{clean(p);}
    }
    @GameTest(template="empty",timeoutTicks=400) public static void wrongColorsSpawnRealBacteriaAndHazmatIsIndividual(GameTestHelper h){
        var p=begin(h,"ColorTrap");var d=MatchData.get(p.server);
        try{
            d.quest.notes=3;d.quest.water=true;d.quest.hidden=true;
            for(int x=-24;x<=24;x+=16)for(int z=-24;z<=24;z+=16)p.serverLevel().getChunkAt(LevelZeroQuest.puzzle(d).offset(x,0,z));
            click(p,LevelZeroQuest.color(d,1));
            h.assertTrue(!d.quest.puzzle&&d.quest.sequence==0&&!p.serverLevel().getEntitiesOfClass(BacteriaEntity.class,p.getBoundingBox().inflate(40)).isEmpty(),"Wrong first color resets sequence and attracts an actual Bacteria");
            d.quest.equipped=true; // Another teammate's shared milestone must not grant immunity.
            p.setPos(LevelZeroQuest.radiation(d).getX()+2.5,65,LevelZeroQuest.radiation(d).getZ()+.5);p.invulnerableTime=0;float health=p.getHealth();LevelZeroQuest.environment(p,d,20);
            h.assertTrue(p.getHealth()<health,"Unprotected player takes actual radiation damage");
            p.setItemSlot(EquipmentSlot.HEAD,LevelZeroQuest.item(QuestContent.HELMET.get(),d));p.setItemSlot(EquipmentSlot.CHEST,LevelZeroQuest.item(QuestContent.SUIT.get(),d));p.setItemSlot(EquipmentSlot.FEET,LevelZeroQuest.item(QuestContent.BOOTS.get(),d));
            health=p.getHealth();p.invulnerableTime=0;LevelZeroQuest.environment(p,d,40);h.assertTrue(p.getHealth()==health,"All three equipped pieces block radiation");
            p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);p.invulnerableTime=0;LevelZeroQuest.environment(p,d,60);h.assertTrue(p.getHealth()<health,"Removing one piece loses immunity");h.succeed();
        }finally{clean(p);}
    }
    @GameTest(template="empty",timeoutTicks=400) public static void glitchRoutesThroughActualDarkroomsDimensionThenWarehouse(GameTestHelper h){
        var p=begin(h,"DarkRoute");var d=MatchData.get(p.server);var team=Map.<UUID,ServerPlayer>of(p.getUUID(),p);
        try{
            solve(p,d);charge(p,d,team);h.assertTrue(d.phase==MatchData.Phase.REST&&d.level==2,"Glitch route chooses Darkrooms rather than warehouse");
            d.resume(p.getUUID());MissionSystem.prepare(p.server,d);MatchController.apply(p,d,d.members.get(p.getUUID()),true);
            h.assertTrue(p.level().dimension().equals(Campaign.DARKROOMS)&&p.serverLevel().getChunkSource().getGenerator() instanceof DarkroomsGenerator,"Player enters a real dedicated Darkrooms dimension");
            charge(p,d,team);h.assertTrue(d.phase==MatchData.Phase.REST&&d.level==1,"Escaping Darkrooms reconnects to Zona de Carga after rest");h.succeed();
        }finally{clean(p);}
    }
    static void charge(ServerPlayer p,MatchData d,Map<UUID,ServerPlayer> team){var exit=MatchArena.exit(d);p.setPos(exit.getX()-.31,65,exit.getZ()+.5);p.setYRot(-90);p.setXRot(0);p.setShiftKeyDown(true);NoClipSystem.input(p,true);for(int i=0;i<40;i++)NoClipSystem.step(p.server,team);for(int i=0;i<20;i++)NoClipSystem.step(p.server,team);}
}
