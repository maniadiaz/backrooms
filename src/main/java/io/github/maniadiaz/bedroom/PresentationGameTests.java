package io.github.maniadiaz.bedroom;

import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Bedroom.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PresentationGameTests {
    @GameTest(template="empty")
    public static void backroomsCommandsAndLegacyAliasKeepExternalInventory(GameTestHelper h) throws Exception {
        var p=new net.minecraftforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RenamedMod"));
        p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,2));
        try {
            var dispatcher=p.server.getCommands().getDispatcher();
            h.assertTrue(dispatcher.execute("backrooms lobby",p.createCommandSourceStack())==1&&MatchController.member(p),"New public name must enter the lobby");
            h.assertTrue(dispatcher.execute("backrooms status",p.createCommandSourceStack())==1,"New command must execute nested subcommands");
            h.assertTrue(dispatcher.execute("bedroom leave",p.createCommandSourceStack())==1&&!MatchController.member(p),"Legacy command must stay compatible");
            h.assertTrue(p.getInventory().getItem(0).is(net.minecraft.world.item.Items.DIAMOND)&&p.getInventory().getItem(0).getCount()==2,"Renaming must preserve the external inventory");h.succeed();
        } finally {if(MatchController.member(p))MatchController.leave(p);p.discard();}
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void restartAdmitsLateFriendAndPreservesExternalItems(GameTestHelper h) {
        var players = new LinkedHashMap<UUID, net.minecraft.server.level.ServerPlayer>();
        try {
            FakePlayer host = player(h, "RestartHost", Items.DIAMOND, 2);
            FakePlayer guest = player(h, "RestartGuest", Items.EMERALD, 3);
            players.put(host.getUUID(), host); players.put(guest.getUUID(), guest);
            MatchController.join(host); MatchController.join(guest);
            MatchData d = MatchData.get(host.server);
            h.assertTrue(MatchController.canRestart(host) && !MatchController.canRestart(guest), "Only host/owner/operator can restart");
            var command = host.server.getCommands().getDispatcher().getRoot().getChild("bedroom").getChild("restart");
            h.assertTrue(command.canUse(host.createCommandSourceStack()) && !command.canUse(guest.createCommandSourceStack()), "Lobby restart remains limited to the host");
            d.start(host.getUUID(), players.keySet());
            h.assertTrue(command.canUse(guest.createCommandSourceStack()), "Any active participant may request a vote, without forcing a restart");
            host.getInventory().setItem(0, new ItemStack(Items.DIRT, 64));
            MatchController.death(host);
            FakePlayer late = player(h, "RestartLate", Items.IRON_INGOT, 4);
            players.put(late.getUUID(), late); MatchController.join(late);
            h.assertTrue(d.waiting.containsKey(late.getUUID()) && !d.members.containsKey(late.getUUID()), "Late guest must wait during active match");
            d.awaitingPlayers = true;
            MatchController.restart(host.server, players);
            h.assertTrue(d.phase == MatchData.Phase.LOBBY && !d.awaitingPlayers && d.members.size() == 3 && d.waiting.isEmpty(), "Restart must return group and late friend to lobby without starting automatically");
            for (var p : players.values()) {
                var m = d.members.get(p.getUUID());
                h.assertTrue(m.state == MatchData.State.LOBBY && m.deaths == 0 && m.limboTicks == 0 && p.getInventory().isEmpty(), "Restart must clear run inventory, deaths and timers");
            }
            MatchController.leave(host); MatchController.leave(guest); MatchController.leave(late);
            h.assertTrue(host.getInventory().getItem(0).is(Items.DIAMOND) && host.getInventory().getItem(0).getCount() == 2, "Host external items preserved");
            h.assertTrue(guest.getInventory().getItem(0).is(Items.EMERALD) && guest.getInventory().getItem(0).getCount() == 3, "Guest external items preserved");
            h.assertTrue(late.getInventory().getItem(0).is(Items.IRON_INGOT) && late.getInventory().getItem(0).getCount() == 4, "Late friend external items preserved");
            h.succeed();
        } finally {
            for (var p : players.values()) { if (MatchController.member(p)) MatchController.leave(p); p.discard(); }
        }
    }

    private static FakePlayer player(GameTestHelper h, String name, net.minecraft.world.item.Item item, int count) {
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        p.getInventory().setItem(0, new ItemStack(item, count));
        return p;
    }

    @GameTest(template = "empty")
    public static void nutritionRestoresHungerAndConsumesOne(GameTestHelper h) {
        FakePlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "NutritionTest"));
        try {
            MatchController.join(p);
            MatchData.get(p.server).start(p.getUUID(), java.util.Set.of(p.getUUID()));
            p.getFoodData().setFoodLevel(8);
            ItemStack stack = new ItemStack(Bedroom.NUTRITION_BAR.get(), 3);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            var pos = p.blockPosition().below();
            var click = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,
                    net.minecraft.world.InteractionHand.MAIN_HAND, pos,
                    new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false));
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(click);
            h.assertTrue(!click.isCanceled() && click.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY,
                    "Looking at a protected floor must allow eating while denying block use");
            stack.finishUsingItem(h.getLevel(), p);
            h.assertTrue(p.getFoodData().getFoodLevel() == 16 && stack.getCount() == 2, "Eating restores eight hunger and consumes exactly one bar");
            h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "Bar gives brief regeneration");
            p.getFoodData().setFoodLevel(19);
            stack.finishUsingItem(h.getLevel(), p);
            h.assertTrue(p.getFoodData().getFoodLevel() == 20, "Hunger cannot exceed maximum");
            h.succeed();
        } finally { if (MatchController.member(p)) MatchController.leave(p); p.discard(); }
    }

    @GameTest(template = "empty")
    public static void hudPacketRoundTripAndStates(GameTestHelper h) {
        MatchData d = new MatchData();
        UUID id = UUID.randomUUID();
        d.join(id); d.start(id, java.util.Set.of(id)); d.die(id);
        d.members.get(id).limboTicks = 321;
        var hud = BedroomNetwork.snapshot(d, id);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            hud.encode(buffer);
            h.assertTrue(hud.equals(BedroomNetwork.Hud.decode(buffer)), "Remote HUD serialization must preserve state and personal deaths");
        } finally { buffer.release(); }
        h.assertTrue(hud.visible() && hud.state().equals("LIMBO") && hud.deaths() == 1 && hud.seconds() == 17, "HUD includes limbo countdown and deaths");
        UUID waiting = UUID.randomUUID(); d.waiting.put(waiting, new net.minecraft.nbt.CompoundTag());
        h.assertTrue(BedroomNetwork.snapshot(d, waiting).state().equals("WAITING"), "Waiting friend receives waiting HUD, not ACTIVE");
        h.assertTrue(!BedroomNetwork.snapshot(d, UUID.randomUUID()).visible(), "Outside players must not receive visible game HUD");
        h.succeed();
    }
}
