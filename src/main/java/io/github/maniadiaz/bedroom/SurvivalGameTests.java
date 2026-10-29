package io.github.maniadiaz.bedroom;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SurvivalGameTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void lowSanityEffectsAndPanicRecovery(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var p = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "PanicTest"));
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND), p) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        try {
            MatchController.join(p); var data = MatchData.get(p.server); data.start(p.getUUID(), Set.of(p.getUUID()));
            // Exercise real ServerPlayer damage after vanilla's 60-tick spawn protection.
            for (int i = 0; i < 65; i++) p.tick();
            var m = data.members.get(p.getUUID()); m.survival.sanity = 0; m.survival.hallucinationTicks = 1; p.setHealth(10);
            for (int i = 0; i < 40; i++) SurvivalSystem.tick(p, m, data);
            h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) && p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Below twenty-five gives owned slow and momentary blindness");
            h.assertTrue(p.getHealth() == 9, "Zero sanity deals one panic damage every two seconds");
            new ItemStack(Bedroom.ALMOND_WATER.get()).finishUsingItem(p.level(), p);
            h.assertTrue(m.survival.sanity == 30 && !p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Almond water removes owned low-sanity effects");
            for (int i = 0; i < 40; i++) SurvivalSystem.tick(p, m, data);
            h.assertTrue(p.getHealth() == 9, "Recovering sanity stops panic damage");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); }
    }

    @GameTest(template = "empty")
    public static void drainCapCompanyAndEnergy(GameTestHelper h) {
        SurvivalState dark = new SurvivalState();
        for (int i = 0; i < 200; i++) dark.advance(true, false, false, true);
        h.assertTrue(Math.abs(dark.sanity - 99) < .00001, "Darkness drains one point per ten seconds");
        SurvivalState alone = new SurvivalState(), company = new SurvivalState();
        alone.quietTicks = company.quietTicks = 3600;
        for (int i = 0; i < 1200; i++) { alone.advance(false, false, false, true); company.advance(false, false, true, true); }
        h.assertTrue(Math.abs(alone.sanity - 99.5) < .00001 && Math.abs(company.sanity - 99.75) < .00001, "Company halves only monotony drain");
        h.assertTrue(SurvivalState.drainPerSecond(.2, .2, .2) == .3, "Environmental cap is three points per ten seconds");
        SurvivalState energy = new SurvivalState(); energy.energyTicks = 600;
        for (int i = 0; i < 1200; i++) energy.advance(false, false, false, false);
        h.assertTrue(energy.sanity == 95 && energy.energyTicks == 0, "Energy deducts five once, outside environmental cap");
        energy.recover(1000); h.assertTrue(energy.sanity == 100, "Sanity clamps maximum");
        energy.recover(-1000); h.assertTrue(energy.sanity == 0, "Sanity clamps minimum");
        var saved = SurvivalState.load(company.save());
        h.assertTrue(saved.sanity == company.sanity && saved.quietTicks == company.quietTicks, "Within-run sanity state serializes accurately");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void voteMajorityTimeoutAndCameraCycle(GameTestHelper h) {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        RestartVote vote = new RestartVote();
        h.assertTrue(vote.start(Set.of(a,b,c)) && !vote.start(Set.of(a)), "Only one vote at a time");
        h.assertTrue(!vote.cast(UUID.randomUUID(), true), "Waiting players cannot vote");
        vote.cast(a, true); h.assertTrue(vote.result() == RestartVote.Result.PENDING, "One of three cannot force restart");
        vote.cast(b, true); h.assertTrue(vote.result() == RestartVote.Result.RESTART, "Majority restarts");
        vote.clear(); vote.start(Set.of(a,b)); vote.cast(a,false);
        h.assertTrue(vote.result() == RestartVote.Result.CONTINUE, "A tie cannot force restart");
        vote.clear(); vote.start(Set.of(a,b,c));
        for (int i = 0; i < 600; i++) vote.tick(Set.of(a,b,c));
        h.assertTrue(vote.result() == RestartVote.Result.CONTINUE, "Timeout defaults to continuing");
        h.assertTrue(MatchController.nextCamera(List.of(a,b,c), a, 1).equals(b)
                && MatchController.nextCamera(List.of(a,b,c), a, -1).equals(c)
                && MatchController.nextCamera(List.of(a), a, 1).equals(a)
                && MatchController.nextCamera(List.of(), a, 1) == null, "Spectator can cycle forward/backward and handle empty lists");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void returningPlayerForfeitsAndWaits(GameTestHelper h) {
        FakePlayer a = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ForfeitA"));
        FakePlayer b = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ForfeitB"));
        try {
            a.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
            MatchController.join(a); MatchController.join(b);
            MatchData d = MatchData.get(a.server); d.start(a.getUUID(), Set.of(a.getUUID(), b.getUUID()));
            a.getInventory().setItem(0, new ItemStack(Items.DIRT, 64));
            d.members.get(a.getUUID()).survival.sanity = 7;
            MatchController.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(a));
            h.assertTrue(!d.members.containsKey(a.getUUID()) && d.phase == MatchData.Phase.ACTIVE, "Disconnect forfeits returning player's run without stopping teammates");
            MatchController.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(a));
            h.assertTrue(d.waiting.containsKey(a.getUUID()) && !d.members.containsKey(a.getUUID()) && a.getInventory().isEmpty(), "Reconnect waits with no restored run items or sanity");
            h.assertTrue(d.members.get(b.getUUID()).state == MatchData.State.ACTIVE, "Remaining friend continues until a restart vote wins");
            MatchController.leave(a);
            h.assertTrue(a.getInventory().getItem(0).is(Items.DIAMOND) && a.getInventory().getItem(0).getCount() == 2, "Forfeiting cannot destroy external inventory");
            h.succeed();
        } finally {
            if (MatchController.member(a)) MatchController.leave(a);
            if (MatchController.member(b)) MatchController.leave(b);
            a.discard(); b.discard();
        }
    }

    @GameTest(template = "empty")
    public static void consumablesAndBattery(GameTestHelper h) {
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "Supplies"));
        try {
            MatchController.join(p); MatchData d = MatchData.get(p.server); d.start(p.getUUID(), Set.of(p.getUUID()));
            SurvivalState s = d.members.get(p.getUUID()).survival; s.sanity = 10;
            p.getFoodData().setFoodLevel(4);
            ItemStack water = new ItemStack(Bedroom.ALMOND_WATER.get()); water.finishUsingItem(p.level(), p);
            h.assertTrue(s.sanity == 40 && p.getFoodData().getFoodLevel() == 10 && water.isEmpty(), "Water restores thirty sanity and six hunger once");
            p.setHealth(5); ItemStack bandage = new ItemStack(Bedroom.BANDAGE.get()); bandage.finishUsingItem(p.level(), p);
            h.assertTrue(p.getHealth() == 11 && bandage.isEmpty(), "Bandage heals three hearts");
            ItemStack drink = new ItemStack(Bedroom.ENERGY_DRINK.get(), 2); drink.finishUsingItem(p.level(), p); s.energyTicks = 300; drink.finishUsingItem(p.level(), p);
            h.assertTrue(s.energyTicks == 600 && drink.isEmpty(), "Energy refreshes a single pending penalty");
            ItemStack lamp = new ItemStack(Bedroom.FLASHLIGHT.get()); lamp.getOrCreateTag().putInt("Charge", 12000);
            p.getInventory().setItem(1, lamp);
            h.assertTrue(SurvivalItems.recharge(p) && SurvivalItems.charge(lamp) == 18000, "Battery adds twenty-five percent");
            SurvivalItems.recharge(p);
            h.assertTrue(!SurvivalItems.recharge(p) && SurvivalItems.charge(lamp) == 24000, "Full lamps do not waste batteries");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); }
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void lootDistributionAndPersistence(GameTestHelper h) {
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        var id = ResourceLocation.fromNamespaceAndPath("bedroom", "chests/supplies");
        var table = level.getServer().getLootData().getLootTable(id);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int utility = 0, special = 0, total = 0;
        for (int i = 0; i < 4000; i++) {
            var items = table.getRandomItems(params, 981723L + i * 179L);
            h.assertTrue(items.size() >= 1 && items.size() <= 3, "Each box yields one to three items");
            h.assertTrue(items.stream().anyMatch(s -> s.is(Bedroom.ALMOND_WATER.get()) || s.is(Bedroom.NUTRITION_BAR.get())), "Every box must contain basic food or water");
            if (items.stream().anyMatch(s -> s.is(Bedroom.BATTERY.get()) || s.is(Bedroom.ENERGY_DRINK.get()) || s.is(Bedroom.FLASHLIGHT.get()))) utility++;
            if (items.stream().anyMatch(s -> s.is(Bedroom.BANDAGE.get()) || s.is(Bedroom.ANOMALOUS_COMPASS.get()))) special++;
            total += items.size();
        }
        h.assertTrue(utility > 1800 && utility < 2200 && special > 450 && special < 750 && total > 6200 && total < 7000, "Loot frequencies must match fifty/fifteen percent and 1.65 average");
        BlockPos pos = new BlockPos(6008, 65, 6008);
        level.setBlock(pos, Bedroom.CARDBOARD_BOX.get().defaultBlockState(), 3);
        SupplyContainer.Contents box = (SupplyContainer.Contents) level.getBlockEntity(pos);
        box.setLootTable(id, 42); box.unpackLootTable(null);
        for (int i = 0; i < box.getContainerSize(); i++) box.removeItemNoUpdate(i);
        var saved = box.saveWithFullMetadata();
        var loaded = new SupplyContainer.Contents(pos, Bedroom.CARDBOARD_BOX.get().defaultBlockState()); loaded.load(saved); loaded.setLevel(level);
        loaded.unpackLootTable(null);
        h.assertTrue(loaded.isEmpty() && !saved.contains("LootTable"), "Empty opened box must not regenerate loot after save/load");
        level.removeBlock(pos, false); h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void supplyPlacementAndFlashlightOcclusion(GameTestHelper h) {
        Level0Layout layout = new Level0Layout(123);
        int previous = Integer.MIN_VALUE;
        for (int cell = -120; cell < 120; cell++) if (layout.supplyCell(cell, 5)) {
            if (previous != Integer.MIN_VALUE) h.assertTrue(cell - previous >= 10 && cell - previous <= 14, "Containers must be ten to fourteen rooms apart along each strip");
            previous = cell;
        }
        var level = h.getLevel().getServer().getLevel(DevelopmentCommands.LEVEL_0);
        boolean found = false;
        for (int cell = 500; cell < 512; cell++) {
            BlockPos pos = new BlockPos(cell * 16 + 3, 65, 8011);
            var state = level.getBlockState(pos);
            if (state.is(Bedroom.CARDBOARD_BOX.get()) || state.is(Bedroom.FILING_CABINET.get())) {
                h.assertTrue(level.getBlockEntity(pos) instanceof SupplyContainer.Contents, "Generated box must have its registered inventory entity");
                found = true;
            }
        }
        h.assertTrue(found, "Generated strip must contain a supply container");
        FakePlayer p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "BeamTest"));
        try {
            p.setPos(4104.5, 65, 4104.5); p.setYRot(0); p.setXRot(0);
            for (int dx = -2; dx <= 2; dx++) level.setBlock(new BlockPos(4104 + dx, 66, 4108), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            FlashlightSystem.project(p);
            h.assertTrue(level.getBlockState(new BlockPos(4104, 66, 4105)).is(Bedroom.BEAM_LIGHT.get()), "Flashlight must project actual light in front of player");
            h.assertTrue(!level.getBlockState(new BlockPos(4104, 66, 4109)).is(Bedroom.BEAM_LIGHT.get()), "Projection cannot pass through a wall");
            FlashlightSystem.clear(p);
            h.assertTrue(!level.getBlockState(new BlockPos(4104, 66, 4105)).is(Bedroom.BEAM_LIGHT.get()), "Turning off must restore air");
            h.succeed();
        } finally {
            FlashlightSystem.clear(p); p.discard();
            for (int dx = -2; dx <= 2; dx++) level.setBlock(new BlockPos(4104 + dx, 66, 4108), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
