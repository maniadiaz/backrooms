package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder(Bedroom.MOD_ID) @PrefixGameTestTemplate(false)
public final class CheckpointGameTests {
    @GameTest(template="empty",timeoutTicks=500) public static void loadingARecoveryCheckpointPreservesDoorAndRemainingTime(GameTestHelper h){
        var p=LevelZeroQuestGameTests.begin(h,"SavedGhost");var d=MatchData.get(p.server);var store=CheckpointData.get(p.server);var previous=store.snapshot.copy();
        try{
            p.setPos(MissionSystem.anchor(d)+24.5,65,8.5);MatchController.death(p);var m=d.members.get(p.getUUID());m.limboTicks=1000;
            var location=p.position();var door=RecoverySystem.door(m);var players=Map.<UUID,ServerPlayer>of(p.getUUID(),p);
            h.assertTrue(CheckpointSystem.save(p,players),"Host can save an ongoing recovery before closing the world");d.reset();MatchController.apply(p,d,m,false);
            h.assertTrue(CheckpointSystem.load(p,players),"A recoverable ghost may continue its save slot from the lobby");
            m=d.members.get(p.getUUID());h.assertTrue(m.state==MatchData.State.LIMBO&&m.deaths==1&&m.limboTicks==1000,"Loading does not refill lives or extend the saved recovery timer");
            h.assertTrue(p.position().distanceTo(location)<.1&&RecoverySystem.door(m).distanceTo(door)<.1,"Ghost and personal door return to their saved positions in the same map");h.succeed();
        }finally{store.snapshot=previous;store.setDirty();LevelZeroQuestGameTests.clean(p);}
    }
    @GameTest(template="empty",timeoutTicks=500) public static void checkpointPersistsToDiskAndRestoresWorldPartyProgress(GameTestHelper h)throws Exception{
        var p=LevelZeroQuestGameTests.begin(h,"SaveRestore");var d=MatchData.get(p.server);var store=CheckpointData.get(p.server);var previous=store.snapshot.copy();
        try{
            var m=d.members.get(p.getUUID());m.deaths=3;m.survival.sanity=43;d.quest.notes=3;d.quest.water=true;d.quest.hidden=true;d.quest.sequence=2;
            p.setPos(MissionSystem.anchor(d)+8.5,65,8.5);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Bedroom.BATTERY.get(),5));p.setHealth(13);
            var players=Map.<UUID,ServerPlayer>of(p.getUUID(),p);
            h.assertTrue(CheckpointSystem.save(p,players),"Host can save live progress");var point=m.checkpoint.copy();
            var file=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/backrooms_checkpoint.dat").toFile();
            h.assertTrue(file.isFile()&&NbtIo.readCompressed(file).getCompound("data").getCompound("snapshot").getCompound("quest").getInt("notes")==3,"Save point is actually written to the world data file");
            d.reset();p.getInventory().clearContent();
            var threat=HoundEntity.TYPE.get().create(p.level());threat.moveTo(MissionSystem.anchor(d)+14.5,65,8.5,0,0);p.serverLevel().addFreshEntity(threat);
            var second=BacteriaEntity.TYPE.get().create(p.level());second.moveTo(MissionSystem.anchor(d)+8.5,65,14.5,0,0);p.serverLevel().addFreshEntity(second);
            h.assertTrue(!store.snapshot.isEmpty()&&CheckpointSystem.load(p,players),"Lobby reset preserves the independent save slot and can continue it");
            h.assertTrue(threat.isRemoved()&&second.isRemoved(),"Loading removes multiple old threats safely before resuming the checkpoint");
            m=d.members.get(p.getUUID());
            h.assertTrue(d.quest.notes==3&&d.quest.sequence==2&&m.deaths==3&&m.survival.sanity==43,"Missions, partial puzzle, lives and sanity restore together");
            h.assertTrue(p.getInventory().getItem(0).is(Bedroom.BATTERY.get())&&p.getInventory().getItem(0).getCount()==5&&p.getHealth()==13,"Actual inventory and health restore");
            h.assertTrue(p.distanceToSqr(point.getDouble("x"),65,point.getDouble("z"))<.1,"Player returns to the saved safe position");h.succeed();
        }finally{store.snapshot=previous;store.setDirty();LevelZeroQuestGameTests.clean(p);}
    }
    @GameTest(template="empty",timeoutTicks=500) public static void deathStaysInSameMapAndPersonalDoorRestoresCheckpoint(GameTestHelper h){
        var p=LevelZeroQuestGameTests.begin(h,"RecoveryMap");var d=MatchData.get(p.server);
        try{
            var m=d.members.get(p.getUUID());m.checkpoint=CheckpointSystem.position(MissionSystem.anchor(d)+8.5,65,8.5,d.level);
            p.getInventory().setItem(0,new ItemStack(Bedroom.BATTERY.get(),2));p.setPos(MissionSystem.anchor(d)+24.5,65,8.5);var dimension=p.level().dimension();
            h.assertTrue(MatchController.death(p)&&m.limboTicks==3600&&m.deaths==1,"Death starts three-minute recovery and spends one of ten lives");
            h.assertTrue(p.level().dimension().equals(dimension)&&MatchArena.contains(d,p.blockPosition())&&!LevelZeroQuest.targetable(p),"Recovery remains in the same arena and threats ignore the ghost");
            var target=RecoverySystem.door(m);h.assertTrue(target.distanceTo(p.position())>3&&RecoverySystem.free(p.serverLevel(),BlockPos.containing(target),d),"A real reachable free location receives the personal door");
            p.setPos(target);RecoverySystem.tick(p,d,m);
            h.assertTrue(m.state==MatchData.State.ACTIVE&&m.deaths==1&&p.getInventory().getItem(0).getCount()==2,"Walking into the door restores inventory without refunding the spent life");
            h.assertTrue(p.distanceToSqr(MissionSystem.anchor(d)+8.5,65,8.5)<.1,"Escape returns to the checkpoint");h.succeed();
        }finally{LevelZeroQuestGameTests.clean(p);}
    }
    @GameTest(template="empty",timeoutTicks=500) public static void upperPillarClicksAndDuplicateHandsCannotBreakTheColorPuzzle(GameTestHelper h){
        var p=LevelZeroQuestGameTests.begin(h,"PillarFix");var d=MatchData.get(p.server);
        try{
            d.quest.notes=3;d.quest.water=true;d.quest.hidden=true;
            for(int i=0;i<4;i++){
                var pos=LevelZeroQuest.color(d,i).above();p.setPos(pos.getX()+.5,65,pos.getZ()+2.5);
                var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,pos,new BlockHitResult(pos.getCenter(),Direction.SOUTH,pos,false));
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                h.assertTrue(event.isCanceled(),"The actual block interaction handler must recognize the upper half of the pillar");
                LevelZeroQuest.interact(p,pos,InteractionHand.OFF_HAND);LevelZeroQuest.interact(p,pos.below(),InteractionHand.MAIN_HAND);
                h.assertTrue(i==3?d.quest.puzzle:d.quest.sequence==i+1,"Main/off-hand and top/base duplicates represent one press, not a wrong second color");
            }
            h.assertTrue(p.level().getBlockState(LevelZeroQuest.gate(d)).isAir(),"Four visible pillar presses open the physical maintenance gate");h.succeed();
        }finally{LevelZeroQuestGameTests.clean(p);}
    }
    @GameTest(template="empty",timeoutTicks=300) public static void hungerStaysFullAndNoLifeIsRefundedAtLevelChange(GameTestHelper h){
        var p=LevelZeroQuestGameTests.begin(h,"FoodAndLives");var d=MatchData.get(p.server);
        try{
            for(int i=0;i<200;i++){p.getFoodData().addExhaustion(40);p.getFoodData().tick(p);MatchHunger.keepFull(p);}
            h.assertTrue(p.getFoodData().getFoodLevel()==20&&p.getFoodData().getExhaustionLevel()==0&&p.getFoodData().getSaturationLevel()==0,"Running/exhaustion cannot drain food or create high-saturation regeneration");
            var m=d.members.get(p.getUUID());m.deaths=7;d.complete(p.getUUID(),true);h.assertTrue(m.deaths==7&&m.state==MatchData.State.REST,"Moving to another level preserves the whole-run life budget");h.succeed();
        }finally{LevelZeroQuestGameTests.clean(p);}
    }
    @GameTest(template="empty") public static void allDungeonGeneratorsHaveSolidThreeBlockStructures(GameTestHelper h){
        var server=h.getLevel().getServer();
        for(int stage:new int[]{0,1,2}){
            var generator=(Level0Generator)Campaign.level(server,stage).getChunkSource().getGenerator();int ceiling=stage==1?72:68;
            for(int y=62;y<=64;y++)h.assertTrue(!generator.stateAt(8,y,8).isAir(),"Three floor layers in every level");
            for(int y=ceiling;y<=ceiling+2;y++)h.assertTrue(!generator.stateAt(8,y,8).isAir(),"Three ceiling layers in every level");
            for(int y=65;y<=67;y++)h.assertTrue(generator.stateAt(8,y,8).isAir(),"Thicker structure preserves the circulation height");
        }
        var warehouse=(Level1Generator)Campaign.level(server,1).getChunkSource().getGenerator();
        for(int x=31;x<=33;x++)h.assertTrue(warehouse.stateAt(x,65,4).is(Bedroom.STEEL.get()),"Warehouse partition is physically three blocks thick");h.succeed();
    }
}
