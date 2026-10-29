package io.github.maniadiaz.bedroom;

import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.mojang.authlib.GameProfile;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MatchGameTests {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);
    private static final UUID D = new UUID(0, 4);

    private static MatchData solo(GameTestHelper h) {
        MatchData d = new MatchData();
        h.assertTrue(d.join(A) && d.start(A, Set.of(A)), "Solo must start");
        return d;
    }

    @GameTest(template = "empty")
    public static void capacityAndStart(GameTestHelper h) {
        MatchData d = new MatchData();
        for (UUID id : Set.of(A, B, C, D)) h.assertTrue(d.join(id), "Four players must fit");
        h.assertTrue(!d.join(new UUID(0, 5)), "Fifth player must be rejected");
        UUID host = d.host;
        UUID other = d.members.keySet().stream().filter(id -> !id.equals(host)).findFirst().orElseThrow();
        h.assertTrue(!d.start(other, Set.of(A, B, C, D)), "Guest cannot start");
        h.assertTrue(!d.start(host, Set.of(host)), "Offline roster cannot start");
        h.assertTrue(d.start(host, Set.of(A, B, C, D)), "Full party must start");
        int round = d.round;
        h.assertTrue(!d.start(host, Set.of(A, B, C, D)) && d.round == round, "Double start must not reset round");
        h.assertTrue(!d.join(new UUID(0, 6)), "No midgame join");
        MatchData two = new MatchData(); two.join(A); two.join(B);
        h.assertTrue(two.start(A, Set.of(A, B)), "Two-player start");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void twoLimbosAndThirdDeath(GameTestHelper h) {
        MatchData d = solo(h);
        for (int death = 1; death <= 2; death++) {
            h.assertTrue(d.die(A), "Death must be handled");
            h.assertTrue(d.members.get(A).limboTicks == 600, "Limbo starts with 30 seconds");
            for (int i = 0; i < 580; i++) d.tick(Set.of(A));
            h.assertTrue(d.phase == MatchData.Phase.ACTIVE && d.escape(A), "Solo must escape limbo at 29 seconds");
            h.assertTrue(d.members.get(A).deaths == death, "Escaping cannot restore a life");
        }
        h.assertTrue(d.die(A) && d.members.get(A).state == MatchData.State.SPECTATING, "Third death is spectator");
        h.assertTrue(!d.escape(A), "Third death cannot escape");
        d.tick(Set.of(A));
        h.assertTrue(d.phase == MatchData.Phase.LOBBY, "Eliminated solo returns to lobby");
        h.assertTrue(d.start(A, Set.of(A)) && d.round == 2 && d.members.get(A).deaths == 0, "New game starts clean");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void timeoutAndTeamRecovery(GameTestHelper h) {
        MatchData d = new MatchData(); d.join(A); d.join(B); d.start(A, Set.of(A, B));
        d.die(A); d.die(B);
        for (int i = 0; i < 599; i++) d.tick(Set.of(A, B));
        h.assertTrue(d.phase == MatchData.Phase.ACTIVE && d.members.get(A).limboTicks == 1, "All-limbo team retains last tick");
        h.assertTrue(d.escape(B), "One teammate can return");
        d.tick(Set.of(A, B));
        h.assertTrue(!d.escape(A) && d.members.get(A).state == MatchData.State.SPECTATING, "Exactly 30 seconds expires");
        h.assertTrue(d.phase == MatchData.Phase.ACTIVE, "Survivor keeps team alive");
        d.die(B);
        for (int i = 0; i < 600; i++) d.tick(Set.of(A, B));
        h.assertTrue(d.phase == MatchData.Phase.LOBBY, "All exhausted returns team to lobby");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void completionAndRest(GameTestHelper h) {
        MatchData d = new MatchData(); d.join(A); d.join(B); d.start(A, Set.of(A, B));
        d.die(B);
        h.assertTrue(!d.complete(B, true), "Limbo cannot finish level");
        h.assertTrue(d.complete(A, true), "Survivor finishes level");
        h.assertTrue(d.members.get(B).state == MatchData.State.REST && d.members.get(B).deaths == 0, "Completion revives limbo teammate");
        h.assertTrue(!d.complete(A, true) && !d.resume(B), "No double complete or guest skip");
        for (int i = 0; i < 1799; i++) d.tick(Set.of(A, B));
        h.assertTrue(d.phase == MatchData.Phase.REST && d.restTicks == 1, "Rest lasts full 90 seconds");
        d.tick(Set.of(A, B));
        h.assertTrue(d.phase == MatchData.Phase.ACTIVE, "Rest resumes automatically");
        d.complete(A, true);
        h.assertTrue(d.resume(A) && !d.resume(A), "Button skips once");
        d.complete(A, false);
        h.assertTrue(d.phase == MatchData.Phase.LOBBY, "Prototype completion returns lobby");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void persistenceAndDisconnect(GameTestHelper h) {
        MatchData d = new MatchData(); d.join(A); d.join(B); d.start(A, Set.of(A, B));
        d.die(A); d.tick(Set.of(B));
        d.members.get(A).external.putString("test", "external inventory");
        d.members.get(A).inventory.putString("test", "run inventory");
        d.members.get(A).location.putDouble("x", 202.5);
        MatchData loaded = MatchData.load(d.save(new CompoundTag()));
        h.assertTrue(loaded.awaitingPlayers, "Restart must wait for a participant before resolving the saved match");
        h.assertTrue(loaded.members.get(A).limboTicks == 599 && loaded.members.get(A).deaths == 1, "Reload preserves timer and deaths");
        h.assertTrue(loaded.host.equals(B) && loaded.members.get(A).external.getString("test").equals("external inventory"), "Host and external snapshot persist");
        h.assertTrue(loaded.members.get(A).inventory.getString("test").equals("run inventory"), "Run snapshot persists separately");
        h.assertTrue(loaded.members.get(A).location.getDouble("x") == 202.5, "Location must persist independently of hidden inventory");
        loaded.tick(Set.of());
        h.assertTrue(loaded.phase == MatchData.Phase.ACTIVE, "Offline limbo continues");
        for (int i = 0; i < 598; i++) loaded.tick(Set.of());
        h.assertTrue(loaded.phase == MatchData.Phase.LOBBY, "Offline active teammate cannot keep exhausted game alive");
        h.assertTrue(loaded.members.get(A).external.contains("test"), "Defeat cannot erase external inventory");
        MatchData slots = new MatchData(); slots.join(A); slots.join(B); int slot = slots.members.get(B).slot;
        slots.remove(A); slots.join(C);
        h.assertTrue(slots.members.get(B).slot == slot && slots.members.get(C).slot != slot, "Limbo slots cannot shift when host leaves");
        slots.members.get(C).external.putString("test", "offline return");
        slots.tick(Set.of(B));
        h.assertTrue(!slots.members.containsKey(C) && slots.pendingReturns.get(C).getString("test").equals("offline return"),
                "Offline lobby slot must be released without losing external inventory");
        MatchData reloaded = MatchData.load(slots.save(new CompoundTag()));
        h.assertTrue(reloaded.pendingReturns.containsKey(C), "Offline inventory return must survive restart");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void inventoryIsolation(GameTestHelper h) {
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "BedroomTest"));
        p.setGameMode(GameType.CREATIVE);
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        p.experienceLevel = 12;
        p.setPos(4, 70, 4);
        try {
            h.assertTrue(MatchController.join(p) == 1, "Player must join lobby");
            h.assertTrue(p.getInventory().isEmpty() && p.gameMode.getGameModeForPlayer() == GameType.ADVENTURE, "Lobby isolates inventory and game mode");
            h.assertTrue(p.level().dimension().equals(DevelopmentCommands.LEVEL_0), "Lobby teleport must enter Bedroom");
            p.getInventory().setItem(0, new ItemStack(Items.DIRT, 32));
            h.assertTrue(MatchController.leave(p) == 1, "Player must leave");
            h.assertTrue(p.getInventory().getItem(0).is(Items.DIAMOND) && p.getInventory().getItem(0).getCount() == 7, "External items restored exactly");
            h.assertTrue(p.experienceLevel == 12 && p.gameMode.getGameModeForPlayer() == GameType.CREATIVE, "External XP and mode restored");
            h.assertTrue(!MatchController.member(p), "Membership removed");
            h.succeed();
        } finally {
            if (MatchController.member(p)) MatchController.leave(p);
            p.discard();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void deathEventsAndLimboButton(GameTestHelper h) {
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "BedroomDeath"));
        try {
            h.assertTrue(MatchController.join(p) == 1, "Join for event integration");
            MatchData d = MatchData.get(p.server);
            h.assertTrue(d.start(p.getUUID(), Set.of(p.getUUID())), "Start event test");
            p.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
            for (int attempt = 1; attempt <= 2; attempt++) {
                var death = new net.minecraftforge.event.entity.living.LivingDeathEvent(p, p.damageSources().generic());
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(death);
                MatchData.Member m = d.members.get(p.getUUID());
                h.assertTrue(death.isCanceled() && m.state == MatchData.State.LIMBO, "Lethal event must transfer to limbo instead of vanilla death");
                h.assertTrue(p.getInventory().isEmpty(), "Limbo must hide run inventory");
                var pos = MatchRooms.button(MatchRooms.WHITE + m.slot * 32);
                p.setPos(pos.getX() + 0.5, 65, pos.getZ() - 0.5);
                var click = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,
                        net.minecraft.world.InteractionHand.MAIN_HAND, pos,
                        new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false));
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(click);
                h.assertTrue(m.state == MatchData.State.ACTIVE && m.deaths == attempt, "Limbo button must restore active state without resetting deaths");
                h.assertTrue(p.getInventory().getItem(0).is(Items.GOLD_INGOT) && p.getInventory().getItem(0).getCount() == 3,
                        "Limbo button must restore run inventory once");
            }
            var blockPos = p.blockPosition();
            var breakEvent = new net.minecraftforge.event.level.BlockEvent.BreakEvent(p.level(), blockPos,
                    p.level().getBlockState(blockPos), p);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(breakEvent);
            h.assertTrue(breakEvent.isCanceled(), "Participant must not break blocks");
            h.assertTrue(p.server.getCommands().getDispatcher().getRoot().getChild("bedroom").canUse(p.createCommandSourceStack()),
                    "Public lobby command must not inherit operator-only root restriction");
            var death = new net.minecraftforge.event.entity.living.LivingDeathEvent(p, p.damageSources().generic());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(death);
            h.assertTrue(d.members.get(p.getUUID()).state == MatchData.State.SPECTATING && p.isSpectator(), "Third death must activate spectator mode");
            h.succeed();
        } finally {
            if (MatchController.member(p)) MatchController.leave(p);
            p.discard();
        }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void limboHasTraversableMaze(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        int x = 512;
        MatchRooms.ensure(level, x, true, true);
        var start = MatchRooms.spawn(x);
        var goal = new net.minecraft.core.BlockPos(x + 18, 65, 18);
        var distances = new java.util.HashMap<net.minecraft.core.BlockPos, Integer>();
        var queue = new java.util.ArrayDeque<net.minecraft.core.BlockPos>();
        distances.put(start, 0); queue.add(start);
        while (!queue.isEmpty()) {
            var pos = queue.remove();
            for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                var next = pos.relative(direction);
                if (next.getX() <= x || next.getX() >= x + 20 || next.getZ() <= 0 || next.getZ() >= 20) continue;
                if (distances.containsKey(next) || !level.getBlockState(next).isAir() || !level.getBlockState(next.above()).isAir()) continue;
                distances.put(next, distances.get(pos) + 1); queue.add(next);
            }
        }
        h.assertTrue(distances.containsKey(goal), "Limbo exit must be reachable");
        h.assertTrue(distances.get(goal) > 40 && distances.get(goal) < 110, "Limbo must require a detour but remain short enough for 30 seconds");
        h.assertTrue(level.getBlockState(MatchRooms.button(x)).is(net.minecraft.world.level.block.Blocks.STONE_BUTTON), "Limbo must have a physical exit button");
        h.succeed();
    }
}
