package io.github.maniadiaz.bedroom;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MissionGameTests {
    @GameTest(template="empty") public static void sharedPanelsConsumeMatchingFusesOnlyOnce(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"Fuses"));
        try{
            MatchController.join(p);var d=MatchData.get(p.server);d.start(p.getUUID(),Set.of(p.getUUID()));MissionSystem.prepare(p.server,d);
            for(int i=0;i<3;i++){
                BlockPos cache=MissionSystem.cache(d,i);p.setPos(cache.getX()+.5,65,cache.getZ()+2.5);
                h.assertTrue(MissionSystem.interact(p,cache,InteractionHand.MAIN_HAND),"Mission cache accepts active participant");
                ItemStack fuse=ItemStack.EMPTY;
                for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(MissionSystem.valid(p.getInventory().getItem(slot),d,i)){fuse=p.getInventory().getItem(slot);p.getInventory().setItem(slot,ItemStack.EMPTY);break;}
                h.assertTrue(!fuse.isEmpty(),"Cache issues numbered fuse");p.setItemInHand(InteractionHand.MAIN_HAND,fuse);
                BlockPos wrong=MissionSystem.panel(d,(i+1)%3);p.setPos(wrong.getX()+.5,65,wrong.getZ()+2.5);MissionSystem.interact(p,wrong,InteractionHand.MAIN_HAND);
                h.assertTrue(d.mission.count()==i&&!fuse.isEmpty(),"Wrong panel must not consume the fuse");
                BlockPos panel=MissionSystem.panel(d,i);p.setPos(panel.getX()+.5,65,panel.getZ()+2.5);MissionSystem.interact(p,panel,InteractionHand.MAIN_HAND);
                h.assertTrue(d.mission.count()==i+1&&fuse.isEmpty(),"Matching panel consumes exactly one fuse");
                MissionSystem.interact(p,panel,InteractionHand.MAIN_HAND);h.assertTrue(d.mission.count()==i+1,"Repeated interaction never adds progress");
            }
            h.assertTrue(d.mission.ready(),"Three panels unlock the shared exit");h.succeed();
        }finally{if(MatchController.member(p))MatchController.leave(p);p.discard();}
    }
    @GameTest(template="empty") public static void eliminatedOrLostFuseCanBeReissuedWithoutValidDuplicates(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"Recovery"));
        try{
            MatchController.join(p);var d=MatchData.get(p.server);d.start(p.getUUID(),Set.of(p.getUUID()));MissionSystem.prepare(p.server,d);
            BlockPos pos=MissionSystem.cache(d,0);p.setPos(pos.getX()+.5,65,pos.getZ()+2.5);MissionSystem.interact(p,pos,InteractionHand.MAIN_HAND);
            h.assertTrue(!MissionSystem.recoverable(p,d,0),"A carried fuse cannot be duplicated from its cache");
            ItemStack old=MissionSystem.fuse(d,0,d.mission.leases[0]);
            var m=d.members.get(p.getUUID());m.inventory.put("items",p.getInventory().save(new net.minecraft.nbt.ListTag()));p.getInventory().clearContent();m.state=MatchData.State.LIMBO;m.limboTicks=600;
            h.assertTrue(!MissionSystem.recoverable(p,d,0),"Recoverable limbo inventory reserves the fuse");
            m.state=MatchData.State.SPECTATING;h.assertTrue(MissionSystem.recoverable(p,d,0),"Permanent elimination releases the fuse");
            m.state=MatchData.State.ACTIVE;MissionSystem.interact(p,pos,InteractionHand.MAIN_HAND);
            h.assertTrue(d.mission.leases[0]==2&&!MissionSystem.valid(old,d,0),"Recovery invalidates any abandoned old copy");
            var saved=MatchData.load(d.save(new net.minecraft.nbt.CompoundTag()));h.assertTrue(saved.mission.leases[0]==2,"Mission lease survives saving within a run");
            d.reset();h.assertTrue(d.mission.count()==0&&!MissionSystem.valid(old,d,0),"Restart clears mission and rejects old fuse");h.succeed();
        }finally{if(MatchController.member(p))MatchController.leave(p);p.discard();}
    }
    @GameTest(template="empty") public static void chargeRequiresFortyContinuousTicksAndSameWall(GameTestHelper h){
        var charge=new NoClipSystem.Charge();var a=new BlockPos(1,65,1);var b=new BlockPos(2,65,1);
        for(int i=0;i<39;i++)h.assertTrue(!charge.advance(a,true),"No premature no-clip");
        h.assertTrue(charge.advance(a,true),"Forty ticks completes charge");charge.advance(null,false);
        for(int i=0;i<20;i++)charge.advance(a,true);charge.advance(b,true);
        h.assertTrue(charge.ticks==1,"Changing wall resets charge");charge.advance(b,false);h.assertTrue(charge.ticks==0,"Releasing intent resets charge");h.succeed();
    }
    @GameTest(template="empty") public static void actualContactRequiresMissionActivePlayerAndEnoughFood(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);var p=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"NoClip"));
        try{
            MatchController.join(p);var d=MatchData.get(p.server);d.start(p.getUUID(),Set.of(p.getUUID()));
            for(int y=65;y<=67;y++)level.setBlock(new BlockPos(24004,y,24004),Bedroom.EXIT.get().defaultBlockState(),3);
            p.setPos(24003.69,65,24004.5);p.setYRot(-90);p.setXRot(0);p.getFoodData().setFoodLevel(20);
            h.assertTrue(NoClipSystem.contact(p)!=null,"Body contact plus forward ray identifies glitch wall");
            var charge=new NoClipSystem.Charge();for(int i=0;i<50;i++)NoClipSystem.advance(p,charge,true);
            h.assertTrue(charge.ticks==0,"Locked objectives prevent charging");
            d.mission.installed=7;for(int i=0;i<39;i++)h.assertTrue(!NoClipSystem.advance(p,charge,true),"Charge requires two seconds");
            h.assertTrue(NoClipSystem.advance(p,charge,true),"Unlocked wall accepts complete physical charge");
            p.setPos(24002.5,65,24004.5);NoClipSystem.advance(p,charge,true);h.assertTrue(charge.ticks==0,"Looking from a distance cannot charge");
            p.setPos(24003.69,65,24004.5);p.getFoodData().setFoodLevel(6);NoClipSystem.advance(p,charge,true);h.assertTrue(charge.ticks==0,"Insufficient hunger cannot count as running");
            p.getFoodData().setFoodLevel(20);d.members.get(p.getUUID()).state=MatchData.State.SPECTATING;NoClipSystem.advance(p,charge,true);h.assertTrue(charge.ticks==0,"Spectators cannot trigger exit");h.succeed();
        }finally{if(MatchController.member(p))MatchController.leave(p);p.discard();}
    }
    @GameTest(template="empty") public static void nodesLeaveGeneratorCrossCorridorsConnected(GameTestHelper h){
        var d=new MatchData();d.round=7;var layout=new Level0Layout(981);
        for(int i=0;i<3;i++)for(BlockPos pos:List.of(MissionSystem.cache(d,i),MissionSystem.panel(d,i))){
            h.assertTrue(!layout.wall(pos.getX(),pos.getZ()),"Mission node starts in an open corridor");
            h.assertTrue(!layout.wall(pos.getX(),pos.getZ()+1),"Adjacent lane must remain open");
        }
        d.mission.leases[0]=1;d.mission.install(0,1);var copy=MatchData.load(d.save(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(copy.mission.count()==1,"Shared progress persists within the current run");d.reset();h.assertTrue(d.mission.count()==0&&d.exitDelay==0,"Reset clears objectives and queued exit");h.succeed();
    }
    @GameTest(template="empty") public static void hudProtocolIncludesSharedMissionAndCharge(GameTestHelper h){
        var original=new BedroomNetwork.Hud(true,"ACTIVE",1,0,0,4,85,70,1,3,20,2,15);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try{original.encode(buffer);h.assertTrue(original.equals(BedroomNetwork.Hud.decode(buffer)),"Remote HUD retains objective and charge fields");}finally{buffer.release();}h.succeed();
    }
}
