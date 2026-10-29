package io.github.maniadiaz.bedroom;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BacteriaGameTests {
    private static ServerLevel level(GameTestHelper h) { return h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0); }
    private static FakePlayer player(ServerLevel level,String name) { return new FakePlayer(level,new GameProfile(UUID.randomUUID(),name)); }
    private static void room(ServerLevel level,int x,int length) {
        for (int dx = 0; dx < 10; dx++) for (int z = 0; z < length; z++) for (int y = 64; y <= 68; y++)
            level.setBlock(new BlockPos(x+dx,y,x+z),(y==64||y==68||dx==0||dx==9||z==0||z==length-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
    }
    private static void wall(ServerLevel level,int x,int z) {
        for (int dx = 1; dx < 9; dx++) for (int y = 65; y < 68; y++) level.setBlock(new BlockPos(x+dx,y,x+z),Blocks.STONE.defaultBlockState(),3);
    }
    @GameTest(template = "empty") public static void sightNoiseAndCapturedPositionRespectWalls(GameTestHelper h) {
        var level = level(h); var p = player(level,"BacteriaNoise"); var entity = BacteriaEntity.TYPE.get().create(level);
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(),Set.of(p.getUUID()));
            room(level,22000,22); p.setPos(22003.5,65,22006.5); entity.setPos(22003.5,65,22016.5);
            h.assertTrue(entity.evidence(p).equals(p.position()),"Close unobstructed vision detects a quiet player");
            wall(level,22000,10); h.assertTrue(entity.evidence(p)==null,"A silent player behind a wall must be hidden");
            p.setSprinting(true); h.assertTrue(entity.evidence(p).equals(p.position()),"Running is audible through a wall within fifteen blocks"); p.setSprinting(false);
            Vec3 origin = p.position(); BacteriaNoise.record(p); p.setPos(22003.5,65,22005.5);
            h.assertTrue(entity.evidence(p).equals(origin),"An interaction reveals its captured location, not later silent movement");
            data.members.get(p.getUUID()).state = MatchData.State.SPECTATING;
            h.assertTrue(entity.evidence(p)==null,"Eliminated observers cannot generate valid evidence");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); entity.discard(); }
    }
    @GameTest(template = "empty") public static void alertChaseSearchReacquireAndForget(GameTestHelper h) {
        var level = level(h); var p = player(level,"BacteriaSearch"); var entity = BacteriaEntity.TYPE.get().create(level);
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(),Set.of(p.getUUID()));
            room(level,22200,20); p.setPos(22203.5,65,22203.5); entity.setPos(22203.5,65,22210.5); entity.setOnGround(true);
            entity.brain(List.of(p)); h.assertTrue(entity.state()==BacteriaEntity.State.ALERT,"First evidence must give a warning");
            for (int i = 1; i < BacteriaEntity.ALERT_TICKS; i++) entity.brain(List.of(p));
            h.assertTrue(entity.state()==BacteriaEntity.State.CHASING,"Continued detection must not restart the alert every tick");
            Vec3 remembered = entity.lastKnown; wall(level,22200,7); p.setPos(22202.5,65,22202.5);
            entity.brain(List.of(p)); h.assertTrue(entity.lastKnown.equals(remembered),"Corners must prevent tracking current coordinates");
            for (int i = 1; i < BacteriaEntity.LOST_TICKS; i++) entity.brain(List.of(p));
            h.assertTrue(entity.state()==BacteriaEntity.State.SEARCHING,"Fifteen seconds without evidence starts local search");
            p.setSprinting(true); entity.brain(List.of(p)); p.setSprinting(false);
            h.assertTrue(entity.state()==BacteriaEntity.State.ALERT && entity.lastKnown.equals(p.position()),"New nearby noise reacquires a hidden target");
            for (int i = 0; i < BacteriaEntity.ALERT_TICKS+BacteriaEntity.LOST_TICKS+BacteriaEntity.SEARCH_TICKS+5; i++) entity.brain(List.of(p));
            h.assertTrue(entity.getTarget()==null && entity.state()==BacteriaEntity.State.ROAMING,"A quiet hidden player must eventually escape");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); entity.discard(); }
    }
    @GameTest(template = "empty") public static void eliminatedTargetLosesAggroAndActiveTeammateCanAttract(GameTestHelper h) {
        var level = level(h); var a = player(level,"BacteriaA"); var b = player(level,"BacteriaB"); var entity = BacteriaEntity.TYPE.get().create(level);
        try {
            MatchController.join(a); MatchController.join(b); var data = MatchData.get(a.server); data.start(a.getUUID(),Set.of(a.getUUID(),b.getUUID()));
            room(level,22400,18); a.setPos(22403.5,65,22403.5); b.setPos(22404.5,65,22405.5); entity.setPos(22403.5,65,22410.5);
            entity.brain(List.of(a)); h.assertTrue(entity.getTarget()==a,"An active participant can be targeted");
            data.members.get(a.getUUID()).state = MatchData.State.SPECTATING; entity.brain(List.of(a,b));
            h.assertTrue(entity.getTarget()==b,"A valid teammate can replace an eliminated target");
            data.members.get(b.getUUID()).state = MatchData.State.LIMBO; entity.brain(List.of(a,b));
            h.assertTrue(entity.getTarget()==null && entity.lastKnown==null,"Spectators and limbo cannot retain aggro or tracking");
            data.remove(a.getUUID()); h.assertTrue(entity.evidence(a)==null,"Disconnected members cannot be detected");
            h.succeed();
        } finally { if (MatchController.member(a)) MatchController.leave(a); if (MatchController.member(b)) MatchController.leave(b); a.discard(); b.discard(); entity.discard(); }
    }
    @GameTest(template = "empty") public static void contactWarningDamageCooldownAndLimbo(GameTestHelper h) {
        var level = level(h); var difficulty = level.getDifficulty(); level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var p = new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"BacteriaBite"));
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND),p) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
        };
        var entity = BacteriaEntity.TYPE.get().create(level);
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(),Set.of(p.getUUID()));
            for (int i = 0; i < 65; i++) p.tick();
            room(level,22600,12); p.setPos(22603.5,65,22603.5); p.setHealth(20); entity.setPos(p.position().add(0,0,.7));
            var member = data.members.get(p.getUUID()); member.survival.sanity = 80;
            entity.brain(List.of(p)); h.assertTrue(p.getHealth()==20,"Alert windup must precede contact damage");
            for (int i = 1; i < BacteriaEntity.ALERT_TICKS; i++) entity.brain(List.of(p));
            h.assertTrue(p.getHealth()==12 && member.survival.sanity==80,"Bacteria deals eight base damage without a forced sanity reset");
            p.invulnerableTime = 0; entity.brain(List.of(p)); h.assertTrue(p.getHealth()==12,"Attack cooldown is independent of vanilla invulnerability");
            p.setHealth(2); p.invulnerableTime = 0;
            for (int i = 0; i < 30; i++) entity.brain(List.of(p));
            h.assertTrue(member.state==MatchData.State.LIMBO && member.deaths==1,"Lethal contact uses normal first-death limbo");
            entity.brain(List.of(p)); h.assertTrue(entity.getTarget()==null,"Entering limbo drops the pursuit"); h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); entity.discard(); level.getServer().setDifficulty(difficulty,true); }
    }
    @GameTest(template = "empty") public static void narrowRouteAndAllSpeciesShareBudget(GameTestHelper h) {
        var level = level(h); var p = player(level,"BacteriaRoute"); BlockPos origin = new BlockPos(22800,64,22800), spawn = origin.offset(1,1,1);
        BacteriaEntity entity = null; HoundEntity dog = null; SmilerEntity smiler = null;
        try {
            for (int x = 0; x < 10; x++) for (int z = 0; z < 32; z++) for (int y = 0; y <= 4; y++) {
                boolean corridor = x>=1&&x<=2&&z>=1&&z<=30 || z>=29&&z<=30&&x>=1&&x<=8;
                level.setBlock(origin.offset(x,y,z),(y==0||y==4||!corridor?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            }
            p.setPos(origin.getX()+7.5,65,origin.getZ()+29.5);
            entity = EncounterSystem.bacteriaCandidate(level,spawn,p,List.of(p));
            h.assertTrue(entity!=null && entity.getBbWidth()<1 && entity.getBbHeight()<3,"A hidden Bacteria must fit and navigate the two-wide L"); entity.discard();
            dog = HoundEntity.TYPE.get().create(level); dog.moveTo(spawn.getX()+10.5,65,spawn.getZ()+.5,0,0); level.addFreshEntity(dog);
            h.assertTrue(EncounterSystem.bacteriaCandidate(level,spawn,p,List.of(p))==null,"Hound reserves Bacteria encounter budget"); dog.discard();
            smiler = SmilerEntity.TYPE.get().create(level); smiler.moveTo(spawn.getX()+10.5,65,spawn.getZ()+.5,0,0); level.addFreshEntity(smiler);
            h.assertTrue(EncounterSystem.bacteriaCandidate(level,spawn,p,List.of(p))==null,"Smiler reserves Bacteria encounter budget"); smiler.discard();
            entity = BacteriaEntity.TYPE.get().create(level); entity.moveTo(spawn.getX()+10.5,65,spawn.getZ()+.5,0,0); level.addFreshEntity(entity);
            h.assertTrue(EncounterSystem.candidate(level,spawn,p,List.of(p))==null,"Bacteria reserves Hound encounter budget"); h.succeed();
        } finally { p.discard(); if (entity!=null) entity.discard(); if (dog!=null) dog.discard(); if (smiler!=null) smiler.discard(); }
    }
    @GameTest(template = "empty") public static void saveAndRestartDiscardOldEncounter(GameTestHelper h) {
        var level = level(h); var p = player(level,"BacteriaReset"); var entity = BacteriaEntity.TYPE.get().create(level);
        var loaded = BacteriaEntity.TYPE.get().create(level);
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(),Set.of(p.getUUID()));
            entity.setPos(23000,65,23000); entity.tick(); h.assertTrue(!entity.isRemoved(),"Current active encounter stays alive");
            var tag = new net.minecraft.nbt.CompoundTag(); entity.addAdditionalSaveData(tag); tag.putInt("BedroomRound",data.round+10);
            loaded.readAdditionalSaveData(tag); loaded.tick(); h.assertTrue(loaded.isRemoved(),"A saved encounter from another round expires");
            data.reset(); entity.tick(); h.assertTrue(entity.isRemoved(),"Restart immediately retires loaded Bacteria"); h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); entity.discard(); loaded.discard(); }
    }
    @GameTest(template = "empty") public static void originalSkinAndMonoSoundsArePackaged(GameTestHelper h) throws Exception {
        var loader = BacteriaGameTests.class.getClassLoader();
        try (var stream = loader.getResourceAsStream("assets/bedroom/textures/entity/bacteria.png")) {
            h.assertTrue(stream!=null,"Bacteria skin is packaged"); var image = javax.imageio.ImageIO.read(stream);
            h.assertTrue(image.getWidth()==64 && image.getHeight()==64,"Model UVs require the 64-pixel skin");
        }
        for (String name : List.of("drag","metal","alert","attack")) {
            try (var stream = loader.getResourceAsStream("assets/bedroom/sounds/ambient/bacteria_"+name+".ogg")) {
                h.assertTrue(stream!=null,"Positional Bacteria sound must be packaged"); byte[] bytes = stream.readAllBytes(); boolean mono = false;
                for (int i = 0; i < bytes.length-16; i++) if (bytes[i]==1 && bytes[i+1]=='v' && bytes[i+2]=='o' && bytes[i+3]=='r' && bytes[i+4]=='b' && bytes[i+5]=='i' && bytes[i+6]=='s') {
                    mono = bytes[i+11]==1 && java.nio.ByteBuffer.wrap(bytes,i+12,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt()==32000; break;
                }
                h.assertTrue(mono,"Spatial OGG must be mono at 32 kHz");
            }
        }
        h.succeed();
    }
}
