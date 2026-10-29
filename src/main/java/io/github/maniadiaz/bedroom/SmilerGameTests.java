package io.github.maniadiaz.bedroom;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SmilerGameTests {
    private static void face(ServerPlayer p, SmilerEntity entity) {
        Vec3 delta = entity.getEyePosition().subtract(p.getEyePosition());
        p.setYRot((float)-Math.toDegrees(Math.atan2(delta.x,delta.z)));
        p.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
    }
    private static void room(ServerLevel level, BlockPos origin, int width, int length) {
        for (int x = 0; x < width; x++) for (int z = 0; z < length; z++) for (int y = 0; y <= 4; y++)
            level.setBlock(origin.offset(x,y,z), (y == 0 || y == 4 || x == 0 || z == 0 || x == width-1 || z == length-1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 3);
    }
    @GameTest(template = "empty") public static void cooperativeGazeExcludesSpectatorsBlindnessAndWalls(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var a = new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SmilerA"));
        var b = new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SmilerB"));
        var entity = SmilerEntity.TYPE.get().create(level);
        try {
            MatchController.join(a); MatchController.join(b); var data = MatchData.get(a.server); data.start(a.getUUID(),Set.of(a.getUUID(),b.getUUID()));
            room(level,new BlockPos(18000,64,18000),8,10);
            a.setPos(18002.5,65,18002.5); b.setPos(18004.5,65,18002.5); entity.setPos(18003.5,65,18007.5);
            face(a,entity); face(b,entity);
            h.assertTrue(entity.observedBy(List.of(a,b)),"Active cooperative gaze observes Smiler");
            a.setYRot(180); h.assertTrue(entity.observedBy(List.of(a,b)),"One remaining observer must still freeze it");
            data.members.get(b.getUUID()).state = MatchData.State.SPECTATING;
            h.assertTrue(!entity.observedBy(List.of(a,b)),"Eliminated spectators cannot freeze it");
            data.members.get(b.getUUID()).state = MatchData.State.ACTIVE; b.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,40));
            h.assertTrue(!entity.observedBy(List.of(a,b)),"Blind players cannot observe"); b.removeEffect(MobEffects.BLINDNESS);
            for (int x = 1; x < 7; x++) for (int y = 65; y <= 67; y++) level.setBlock(new BlockPos(18000+x,y,18005),Blocks.STONE.defaultBlockState(),3);
            h.assertTrue(!entity.observedBy(List.of(a,b)),"A wall must block gaze");
            h.succeed();
        } finally { if (MatchController.member(a)) MatchController.leave(a); if (MatchController.member(b)) MatchController.leave(b); a.discard(); b.discard(); entity.discard(); }
    }
    @GameTest(template = "empty") public static void gazeGraceThenAdvanceAndSearchLastKnownPosition(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var p = new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SmilerGrace"));
        var entity = SmilerEntity.TYPE.get().create(level);
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(),Set.of(p.getUUID()));
            room(level,new BlockPos(18200,64,18200),8,12);
            p.setPos(18203.5,65,18202.5); entity.setPos(18203.5,65,18208.5); entity.setOnGround(true); entity.beginEncounter(p); face(p,entity);
            for (int i = 0; i < 80; i++) entity.brain(List.of(p));
            h.assertTrue(entity.state == SmilerEntity.State.OBSERVED && entity.getNavigation().isDone(),"Held gaze remains frozen beyond three seconds");
            p.setYRot(180);
            for (int i = 0; i < SmilerEntity.GAZE_GRACE-1; i++) entity.brain(List.of(p));
            h.assertTrue(entity.state == SmilerEntity.State.OBSERVED,"Short gaze interruptions retain grace");
            entity.brain(List.of(p)); h.assertTrue(entity.state == SmilerEntity.State.ADVANCING,"Lost gaze activates aggressive advance after seven ticks");
            Vec3 remembered = entity.lastKnown;
            for (int x = 1; x < 7; x++) for (int y = 65; y <= 67; y++) level.setBlock(new BlockPos(18200+x,y,18205),Blocks.STONE.defaultBlockState(),3);
            p.setPos(18202.5,65,18201.5); entity.brain(List.of(p));
            h.assertTrue(entity.state == SmilerEntity.State.SEARCHING && entity.lastKnown.equals(remembered),"Walls prevent tracking current position");
            for (int i = 0; i < 301; i++) entity.brain(List.of(p));
            h.assertTrue(entity.getTarget() == null,"Fifteen seconds without evidence abandons target");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); entity.discard(); }
    }
    @GameTest(template = "empty", timeoutTicks = 200) public static void contactGazeDamageCooldownAndLethalLimbo(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var previousDifficulty = level.getDifficulty();
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
        var p = new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"SmilerContact"));
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND),p) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
        };
        var entity = SmilerEntity.TYPE.get().create(level);
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(),Set.of(p.getUUID()));
            for (int i = 0; i < 65; i++) p.tick();
            room(level,new BlockPos(18400,64,18400),8,10); p.setPos(18403.5,65,18403.5); p.setHealth(20);
            entity.setPos(p.position().add(0,0,.7)); entity.beginEncounter(p); face(p,entity);
            var m = data.members.get(p.getUUID()); m.survival.sanity = 80;
            entity.brain(List.of(p)); h.assertTrue(p.getHealth() == 20 && m.survival.sanity == 80,"Observed Smiler cannot strike even at contact");
            p.setYRot(180); for (int i = 0; i < SmilerEntity.GAZE_GRACE; i++) entity.brain(List.of(p));
            h.assertTrue(p.getHealth() == 4 && m.survival.sanity == 0,"Unobserved contact deals sixteen damage and zero sanity; health="+p.getHealth()+", sanity="+m.survival.sanity+", state="+entity.state);
            p.invulnerableTime = 0; entity.brain(List.of(p));
            h.assertTrue(p.getHealth() == 4 && m.deaths == 0,"Entity cooldown blocks repeated strikes independently of vanilla invulnerability");
            new net.minecraft.world.item.ItemStack(Bedroom.ALMOND_WATER.get()).finishUsingItem(level,p);
            h.assertTrue(m.survival.sanity == 30,"Almond water restores sanity after contact");
            entity.strike(p,true);
            h.assertTrue(m.state == MatchData.State.LIMBO && m.deaths == 1 && m.survival.sanity >= 30,"Lethal mode follows limbo and preserves its recovered sanity");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); entity.discard(); level.getServer().setDifficulty(previousDifficulty,true); }
    }
    @GameTest(template = "empty") public static void oldRoundAndLobbyDiscardSavedSmiler(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var entity = SmilerEntity.TYPE.get().create(level); var data = MatchData.get(level.getServer());
        entity.round = data.round - 1; var tag = new net.minecraft.nbt.CompoundTag(); entity.addAdditionalSaveData(tag);
        var loaded = SmilerEntity.TYPE.get().create(level); loaded.readAdditionalSaveData(tag); loaded.tick();
        h.assertTrue(loaded.isRemoved() && loaded.round == data.round-1,"Old saved encounters must expire across restart");
        entity.discard(); h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 160) public static void darkSpawnHasRouteAndSharesBudgetWithHound(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        BlockPos origin = new BlockPos(18800,64,18800), spawn = origin.offset(1,1,1);
        var chunks = Set.of(new net.minecraft.world.level.ChunkPos(origin),new net.minecraft.world.level.ChunkPos(origin.offset(9,0,31)));
        // Fake players do not keep chunks loaded while the asynchronous light engine settles.
        chunks.forEach(chunk -> level.setChunkForced(chunk.x,chunk.z,true));
        var p = new FakePlayer(level,new GameProfile(UUID.randomUUID(),"SmilerSpawn"));
        for (int x = 0; x < 10; x++) for (int z = 0; z < 32; z++) for (int y = 0; y <= 4; y++) {
            boolean passage = x >= 1 && x <= 2 && z >= 1 && z <= 30 || z >= 29 && z <= 30 && x >= 1 && x <= 8;
            level.setBlock(origin.offset(x,y,z),(y == 0 || y == 4 || !passage ? Blocks.STONE : Blocks.AIR).defaultBlockState(),3);
        }
        p.setPos(origin.getX()+7.5,65,origin.getZ()+29.5);
        level.setBlock(spawn.above(2),Blocks.GLOWSTONE.defaultBlockState(),3);
        h.runAfterDelay(20, () -> {
            try {
                h.assertTrue(level.getBrightness(LightLayer.BLOCK,spawn.above()) > 7,"Fixture light must propagate before testing bright rejection");
                h.assertTrue(EncounterSystem.smilerCandidate(level,spawn,p,List.of(p)) == null,"Smiler cannot spawn in lit space");
                level.setBlock(spawn.above(2),Blocks.AIR.defaultBlockState(),3);
                h.runAfterDelay(20, () -> {
                    SmilerEntity smiler = null; HoundEntity dog = null;
                    try {
                        h.assertTrue(SmilerEntity.dark(level,spawn.above()),"Enclosed fixture must become dark");
                        var probe = SmilerEntity.TYPE.get().create(level);
                        try {
                            probe.moveTo(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,0,0); probe.setOnGround(true);
                            h.assertTrue(level.hasChunkAt(spawn) && level.getBlockState(spawn.below()).isSolid(),"Spawn must retain loaded solid floor");
                            h.assertTrue(level.noCollision(probe),"Spawn must have room for the Smiler collider");
                            h.assertTrue(!p.hasLineOfSight(probe) && p.distanceToSqr(probe) >= 576,"Candidate must remain hidden and distant");
                            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,probe.getBoundingBox().inflate(96)).isEmpty(),"Fixture must not contain other threats");
                            var route = probe.getNavigation().createPath(p,0);
                            h.assertTrue(route != null && route.canReach(),"Smiler must navigate the long L with its own dimensions");
                        } finally { probe.discard(); }
                        smiler = EncounterSystem.smilerCandidate(level,spawn,p,List.of(p));
                        h.assertTrue(smiler != null,"Dark hidden candidate must navigate the two-wide L"); smiler.discard(); smiler = null;
                        dog = HoundEntity.TYPE.get().create(level); dog.moveTo(spawn.getX()+10.5,65,spawn.getZ()+.5,0,0); level.addFreshEntity(dog);
                        h.assertTrue(EncounterSystem.smilerCandidate(level,spawn,p,List.of(p)) == null,"Hound reserves Smiler budget"); dog.discard();
                        smiler = SmilerEntity.TYPE.get().create(level); smiler.moveTo(spawn.getX()+10.5,65,spawn.getZ()+.5,0,0); level.addFreshEntity(smiler);
                        h.assertTrue(EncounterSystem.candidate(level,spawn,p,List.of(p)) == null,"Smiler reserves Hound budget");
                        h.succeed();
                    } finally { p.discard(); if (smiler != null) smiler.discard(); if (dog != null) dog.discard(); chunks.forEach(chunk -> level.setChunkForced(chunk.x,chunk.z,false)); }
                });
            } catch (RuntimeException error) { p.discard(); chunks.forEach(chunk -> level.setChunkForced(chunk.x,chunk.z,false)); throw error; }
        });
    }
    @GameTest(template = "empty") public static void emissiveFaceAndMonoAudioArePackaged(GameTestHelper h) throws Exception {
        var loader = SmilerGameTests.class.getClassLoader();
        try (var stream = loader.getResourceAsStream("assets/bedroom/textures/entity/smiler.png")) {
            h.assertTrue(stream != null,"Face texture is packaged"); var face = javax.imageio.ImageIO.read(stream);
            int transparent = 0, bright = 0;
            for (int x = 0; x < face.getWidth(); x++) for (int y = 0; y < face.getHeight(); y++) {
                int pixel = face.getRGB(x,y); if ((pixel >>> 24) == 0) transparent++; if ((pixel & 255) > 180) bright++;
            }
            h.assertTrue(transparent > 3000 && bright > 100,"Only glowing eyes and grin should occupy the transparent face plane");
        }
        for (String name : List.of("hiss","laugh","attack")) {
            try (var stream = loader.getResourceAsStream("assets/bedroom/sounds/ambient/smiler_"+name+".ogg")) {
                h.assertTrue(stream != null,"Smiler audio must be packaged"); byte[] bytes = stream.readAllBytes(); boolean mono = false;
                for (int i = 0; i < bytes.length-16; i++) if (bytes[i] == 1 && bytes[i+1] == 'v' && bytes[i+2] == 'o' && bytes[i+3] == 'r' && bytes[i+4] == 'b' && bytes[i+5] == 'i' && bytes[i+6] == 's') {
                    int rate = (bytes[i+12]&255) | (bytes[i+13]&255)<<8 | (bytes[i+14]&255)<<16 | (bytes[i+15]&255)<<24;
                    mono = bytes[i+11] == 1 && rate == 32000; break;
                }
                h.assertTrue(mono,"Positional OGG must be mono at 32 kHz");
            }
        }
        h.succeed();
    }
}
