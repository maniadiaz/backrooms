package io.github.maniadiaz.bedroom;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AtmosphereGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void nearbySoundBreaksMonotonyExceptOwnFootsteps(GameTestHelper h) {
        var a = new net.minecraftforge.common.util.FakePlayer(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "SoundA"));
        var b = new net.minecraftforge.common.util.FakePlayer(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "SoundB"));
        try {
            MatchController.join(a); MatchController.join(b);
            var data = MatchData.get(a.server); data.start(a.getUUID(), Set.of(a.getUUID(), b.getUUID()));
            a.setPos(72.5, 65, 8.5); b.setPos(74.5, 65, 8.5);
            var ma = data.members.get(a.getUUID()); var mb = data.members.get(b.getUUID());
            ma.survival.quietTicks = mb.survival.quietTicks = 3700;
            AtmosphereSystem.noteSound(a.serverLevel(), a.position(), 16, a.getUUID(), List.of(a,b));
            h.assertTrue(ma.survival.quietTicks == 3700 && mb.survival.quietTicks == 0, "Own footsteps must not reset monotony; teammate footsteps can");
            mb.state = MatchData.State.LIMBO; mb.survival.quietTicks = 3700;
            AtmosphereSystem.noteSound(a.serverLevel(), a.position(), 16, null, List.of(a,b));
            h.assertTrue(ma.survival.quietTicks == 0 && mb.survival.quietTicks == 3700, "Nearby event affects active listeners but not limbo");
            ma.survival.quietTicks = 3700;
            AtmosphereSystem.noteSound(a.serverLevel(), a.position().add(100,0,0), 16, null, List.of(a,b));
            h.assertTrue(ma.survival.quietTicks == 3700, "Out-of-range events cannot reset monotony");
            h.succeed();
        } finally { if (MatchController.member(a)) MatchController.leave(a); if (MatchController.member(b)) MatchController.leave(b); a.discard(); b.discard(); }
    }

    @GameTest(template = "empty")
    public static void timersAreBoundedAndRepeatable(GameTestHelper h) {
        var a = RandomSource.create(7281); var b = RandomSource.create(7281);
        AmbientSchedule first = new AmbientSchedule(), second = new AmbientSchedule();
        int lastStep = 0, steps = 0;
        for (int tick = 1; tick <= 72000; tick++) {
            var e = first.tick(a);
            h.assertTrue(e.equals(second.tick(b)), "Same random seed must reproduce ambient timing");
            if (e.footsteps()) {
                int elapsed = tick - lastStep;
                h.assertTrue(elapsed >= 900 && elapsed <= 1800, "Distant steps must be 45–90 seconds apart");
                lastStep = tick; steps++;
            }
            h.assertTrue(first.drip > 0 && first.fault > 0, "Event timers must reschedule instead of firing each tick");
        }
        h.assertTrue(steps >= 40 && steps <= 80, "Ambient footsteps must remain sparse over one hour");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fluorescentRecoversAndBrokenLightsStayBroken(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        BlockPos pos = new BlockPos(9000, 68, 9000);
        var original = level.getBlockState(pos);
        try {
            level.setBlock(pos, Bedroom.LIGHT.get().defaultBlockState(), 3);
            h.assertTrue(FluorescentBlock.fail(level, pos, false, 60), "Healthy fluorescent must accept a temporary fault");
            h.assertTrue(level.getBlockState(pos).getLightEmission(level, pos) == 0, "Outage must change real emitted light");
            h.assertTrue(!FluorescentBlock.fail(level, pos, false, 60), "Same lamp cannot stack another active fault");
            Bedroom.LIGHT.get().tick(level.getBlockState(pos), level, pos, RandomSource.create(1));
            // Default setting uses a sustained outage; optional rapid mode may add one recovery phase.
            if (level.getBlockState(pos).getValue(FluorescentBlock.FAULT) != 0)
                Bedroom.LIGHT.get().tick(level.getBlockState(pos), level, pos, RandomSource.create(1));
            h.assertTrue(level.getBlockState(pos).getValue(FluorescentBlock.FAULT) == 0 && level.getBlockState(pos).getLightEmission(level, pos) == 13, "Scheduled recovery must restore normal light");
            h.assertTrue(FluorescentBlock.fail(level, pos, true, 60), "Rapid fault starts when explicitly requested");
            h.assertTrue(level.getBlockState(pos).getValue(FluorescentBlock.FAULT) == 1, "Rapid fault begins with a brief dark phase");
            Bedroom.LIGHT.get().tick(level.getBlockState(pos), level, pos, RandomSource.create(1));
            h.assertTrue(level.getBlockState(pos).getValue(FluorescentBlock.FAULT) == 2 && level.getBlockState(pos).getLightEmission(level, pos) == 13, "Rapid sequence briefly relights");
            level.setBlock(pos, Bedroom.DEAD_LIGHT.get().defaultBlockState(), 3);
            h.assertTrue(!FluorescentBlock.fail(level, pos, true, 60) && level.getBlockState(pos).getLightEmission(level, pos) == 0, "Permanently broken lamps must not be repaired by temporary fault handling");
            h.succeed();
        } finally { level.setBlock(pos, original, 3); }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ambientEventsOnlyTickForActivePlayers(GameTestHelper h) {
        var p = new net.minecraftforge.common.util.FakePlayer(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "AmbientTest"));
        try {
            MatchController.join(p); var d = MatchData.get(p.server); var m = d.members.get(p.getUUID());
            AtmosphereSystem.tick(p, m);
            h.assertTrue(!m.ambient.initialized, "Lobby must not start ambient event clocks");
            d.start(p.getUUID(), Set.of(p.getUUID()));
            AtmosphereSystem.tick(p, m);
            h.assertTrue(m.ambient.initialized && m.ambient.footsteps >= 899, "Active level starts one set of clocks");
            int remaining = m.ambient.footsteps;
            d.die(p.getUUID()); AtmosphereSystem.tick(p, m);
            h.assertTrue(m.ambient.footsteps == remaining, "Limbo pauses the level's ambient clocks");
            d.escape(p.getUUID()); d.complete(p.getUUID(), true); AtmosphereSystem.tick(p, m);
            h.assertTrue(!m.ambient.initialized, "Rest must not start the next level's clocks");
            d.resume(p.getUUID()); AtmosphereSystem.tick(p, m);
            h.assertTrue(m.ambient.initialized && m.ambient.footsteps >= 899, "New level starts a fresh ambient schedule");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void ownedOutageIsCleanedOnLeaving(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var p = new net.minecraftforge.common.util.FakePlayer(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "LampOwner"));
        BlockPos guaranteedLamp = new BlockPos(12008, 68, 12008);
        var original = level.getBlockState(guaranteedLamp);
        try {
            MatchController.join(p); var d = MatchData.get(p.server); d.start(p.getUUID(), Set.of(p.getUUID()));
            p.setPos(12008.5, 65, 12008.5);
            level.setBlock(guaranteedLamp, Bedroom.LIGHT.get().defaultBlockState(), 3);
            h.assertTrue(AtmosphereSystem.tryFault(p), "An active area should have an eligible nearby lamp");
            h.assertTrue(!AtmosphereSystem.tryFault(p), "Nearby players cannot stack simultaneous outages in the same area");
            AtmosphereSystem.clear(p);
            h.assertTrue(AtmosphereSystem.tryFault(p), "After cleanup a new fault can start, so old ownership cannot stick");
            MatchController.leave(p);
            for (int x = 11992; x <= 12024; x += 8) for (int z = 11992; z <= 12024; z += 8) {
                BlockPos pos = new BlockPos(x, 68, z);
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Bedroom.LIGHT.get()))
                    h.assertTrue(level.getBlockState(pos).getValue(FluorescentBlock.FAULT) == 0, "Leaving must restore owned active faults");
            }
            h.succeed();
        } finally { AtmosphereSystem.clear(p); if (MatchController.member(p)) MatchController.leave(p); p.discard(); level.setBlock(guaranteedLamp, original, 3); }
    }

    @GameTest(template = "empty")
    public static void soundsAndCarpetAreRegistered(GameTestHelper h) {
        h.assertTrue(Bedroom.CARPET.get().defaultBlockState().getSoundType().getStepSound() == BedroomSounds.WET_STEP.get()
                && Bedroom.WET_CARPET.get().defaultBlockState().getSoundType().getStepSound() == BedroomSounds.WET_STEP.get(), "Both carpet surfaces must use custom wet footsteps");
        try (var stream = Bedroom.class.getResourceAsStream("/assets/bedroom/sounds.json")) {
            h.assertTrue(stream != null, "Sound manifest must be packaged");
            var sounds = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (String event : new String[] {"fluorescent_hum", "wet_step", "distant_steps", "ceiling_drip", "buzz_click", "sanity_whisper", "sanity_steps"}) {
                h.assertTrue(sounds.has(event), "Missing sound event: " + event);
                for (var entry : sounds.getAsJsonObject(event).getAsJsonArray("sounds")) {
                    String name = entry.getAsJsonObject().get("name").getAsString().replace("bedroom:", "");
                    try (var asset = Bedroom.class.getResourceAsStream("/assets/bedroom/sounds/" + name + ".ogg")) {
                        h.assertTrue(asset != null && Arrays.equals(asset.readNBytes(4), new byte[] {'O','g','g','S'}), "Sound asset must be a packaged OGG file: " + name);
                    }
                }
            }
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        h.succeed();
    }
}
