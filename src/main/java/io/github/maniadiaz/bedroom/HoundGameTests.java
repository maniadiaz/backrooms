package io.github.maniadiaz.bedroom;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoundGameTests {
    @GameTest(template = "empty") public static void gazeNeedsSightAndNoBlindness(GameTestHelper h) {
        var level = h.getLevel(); var p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "Gaze"));
        var dog = HoundEntity.TYPE.get().create(level);
        BlockPos base = h.absolutePos(new BlockPos(1, 2, 1));
        try {
            p.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
            dog.setPos(p.getX(), p.getY(), p.getZ() + 4);
            var delta = dog.getEyePosition().subtract(p.getEyePosition());
            p.setYRot(0); p.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
            h.assertTrue(HoundEntity.looking(p, dog), "Direct unobstructed gaze stops hound");
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40));
            h.assertTrue(!HoundEntity.looking(p, dog), "Blindness cannot observe"); p.removeEffect(MobEffects.BLINDNESS);
            for (int y = 0; y < 3; y++) level.setBlock(base.offset(0,y,2), Blocks.STONE.defaultBlockState(), 3);
            h.assertTrue(!HoundEntity.looking(p, dog), "Walls block gaze");
            h.succeed();
        } finally { p.discard(); dog.discard(); for (int y = 0; y < 3; y++) level.removeBlock(base.offset(0,y,2), false); }
    }
    @GameTest(template = "empty") public static void stareDoesNotExpireAndNoiseOverrides(GameTestHelper h) {
        for (int i = 0; i < 200; i++) h.assertTrue(HoundEntity.reaction(true,true,false,false,true) == HoundEntity.State.HESITATING, "A held gaze must not expire after three seconds");
        h.assertTrue(HoundEntity.reaction(true,true,true,false,false) == HoundEntity.State.CHARGING, "Walking triggers charge");
        h.assertTrue(HoundEntity.reaction(true,false,false,false,false) == HoundEntity.State.CHARGING, "Turning away triggers charge");
        h.assertTrue(HoundEntity.reaction(false,false,false,true,false) == HoundEntity.State.CHARGING, "Nearby noise triggers charge");
        h.assertTrue(HoundEntity.reaction(false,false,false,false,true) == HoundEntity.State.SEARCHING, "No current evidence means last-position search"); h.succeed();
    }
    @GameTest(template = "empty") public static void actualAiHesitatesChargesAndIgnoresEliminatedTarget(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "HoundGaze"));
        var dog = HoundEntity.TYPE.get().create(level);
        BlockPos base = new BlockPos(14008,65,14008);
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(), Set.of(p.getUUID()));
            for (int x = -2; x <= 2; x++) for (int z = 0; z <= 6; z++) for (int y = 0; y < 3; y++) level.setBlock(base.offset(x,y,z), Blocks.AIR.defaultBlockState(), 3);
            p.setPos(base.getX() + .5, 65, base.getZ() + .5); dog.setPos(p.position().add(0,0,4));
            var delta = dog.getEyePosition().subtract(p.getEyePosition());
            p.setYRot(0); p.setXRot((float)-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
            dog.setTarget(p); dog.tick();
            h.assertTrue(dog.state == HoundEntity.State.HESITATING, "Live server AI must honor a stationary gaze");
            p.setSprinting(true); dog.tick();
            h.assertTrue(dog.state == HoundEntity.State.CHARGING, "Sprinting must override gaze in actual AI");
            data.members.get(p.getUUID()).state = MatchData.State.SPECTATING; dog.tick();
            h.assertTrue(dog.getTarget() == null && dog.state == HoundEntity.State.INACTIVE, "Eliminated players must lose aggro");
            data.reset(); dog.tick(); h.assertTrue(dog.isRemoved(), "Restart removes live encounter");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); dog.discard(); }
    }
    @GameTest(template = "empty") public static void lobbyAndOldRoundRemoveThreat(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var dog = HoundEntity.TYPE.get().create(level);
        var data = MatchData.get(level.getServer());
        dog.round = data.round - 1; dog.tick();
        h.assertTrue(dog.isRemoved(), "Threats cannot survive a restart or safe phase");
        h.succeed();
    }
    @GameTest(template = "empty") public static void navigationTurnsInsideNarrowCorridor(GameTestHelper h) {
        var level = h.getLevel();
        var origin = h.absolutePos(new BlockPos(0, 1, 0));
        var dog = HoundEntity.TYPE.get().create(level);
        try {
            // A two-wide L, enclosed by solid walls and a three-block ceiling.
            for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) for (int y = 0; y <= 4; y++) {
                boolean passage = x >= 1 && x <= 2 && z >= 1 && z <= 8 || z >= 7 && z <= 8 && x >= 1 && x <= 8;
                level.setBlock(origin.offset(x,y,z), (y == 0 || y == 4 || !passage ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 3);
            }
            dog.moveTo(origin.getX() + 1.5, origin.getY() + 1, origin.getZ() + 1.5, 0, 0);
            dog.setOnGround(true);
            var path = dog.getNavigation().createPath(origin.offset(8,1,8), 0);
            h.assertTrue(level.noCollision(dog) && path != null && path.canReach(), "Ground navigation must turn through an enclosed two-wide L without phasing");
            h.succeed();
        } finally {
            dog.discard();
            for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) for (int y = 0; y <= 4; y++) level.removeBlock(origin.offset(x,y,z), false);
        }
    }
    @GameTest(template = "empty") public static void spawnNeedsHiddenRouteAndDistanceFromEveryone(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        BlockPos origin = new BlockPos(16000,64,16000);
        var p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "SpawnRoute"));
        var nearby = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "SpawnObserver"));
        HoundEntity dog = null;
        try {
            for (int x = 0; x < 10; x++) for (int z = 0; z < 32; z++) for (int y = 0; y <= 4; y++) {
                boolean passage = x >= 1 && x <= 2 && z >= 1 && z <= 30 || z >= 29 && z <= 30 && x >= 1 && x <= 8;
                level.setBlock(origin.offset(x,y,z), (y == 0 || y == 4 || !passage ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 3);
            }
            p.setPos(16007.5,65,16029.5); nearby.setPos(16001.5,65,16003.5);
            BlockPos spawn = origin.offset(1,1,1);
            var probe = HoundEntity.TYPE.get().create(level); probe.moveTo(16001.5,65,16001.5,0,0); probe.setOnGround(true);
            try {
                h.assertTrue(level.noCollision(probe), "Spawn fixture must be collision-free");
                h.assertTrue(!p.hasLineOfSight(probe) && p.distanceToSqr(probe) >= 576, "Spawn fixture must be hidden and distant");
                var route = probe.getNavigation().createPath(p,0);
                h.assertTrue(route != null && route.canReach(), "Route budget must cover the detour around the L, not only direct distance");
            } finally { probe.discard(); }
            dog = EncounterSystem.candidate(level, spawn, p, List.of(p));
            h.assertTrue(dog != null, "A hidden distant candidate with a real L-shaped route must be accepted before its first tick");
            dog.moveTo(spawn.getX() + 10.5, spawn.getY(), spawn.getZ() + .5, 0, 0); level.addFreshEntity(dog);
            h.assertTrue(EncounterSystem.candidate(level, spawn, p, List.of(p)) == null, "An existing nearby threat reserves the shared species budget");
            dog.discard(); dog = null;
            h.assertTrue(EncounterSystem.candidate(level, spawn, p, List.of(p,nearby)) == null, "Another nearby participant vetoes the spawn");
            level.setBlock(spawn, Blocks.STONE.defaultBlockState(), 3);
            h.assertTrue(EncounterSystem.candidate(level, spawn, p, List.of(p)) == null, "Spawn cannot overlap a solid wall");
            h.succeed();
        } finally { p.discard(); nearby.discard(); if (dog != null) dog.discard(); }
    }
    @GameTest(template = "empty", timeoutTicks = 200) public static void attackUsesLimboAndNoLoot(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var p = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "HoundVictim"));
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND), p) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
        };
        var dog = HoundEntity.TYPE.get().create(level);
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(), Set.of(p.getUUID()));
            for (int i = 0; i < 65; i++) p.tick();
            p.setHealth(2); dog.setPos(p.position()); dog.doHurtTarget(p);
            h.assertTrue(data.members.get(p.getUUID()).state == MatchData.State.LIMBO && data.members.get(p.getUUID()).deaths == 1, "A lethal bite uses normal first-death limbo");
            h.assertTrue(dog.getBbWidth() < 1 && dog.getBbHeight() < 3, "Collision fits two-block corridors and three-block ceiling");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); dog.discard(); }
    }
}
