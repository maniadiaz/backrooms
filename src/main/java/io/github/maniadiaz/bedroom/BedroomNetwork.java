package io.github.maniadiaz.bedroom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** HUD state travels to every remote client, including friends on an integrated server. */
public final class BedroomNetwork {
    private static final String PROTOCOL = "5";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "match"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public record Hud(boolean visible, String state, int deaths, int seconds, int level, int players, float sanity, int battery, int voteYes, int voteRequired, int voteSeconds,int panels,int charge,String difficulty,String objective) {
        static Hud decode(FriendlyByteBuf buffer) {
            return new Hud(buffer.readBoolean(), buffer.readUtf(24), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readUtf(24),buffer.readUtf(160));
        }
        void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(visible); buffer.writeUtf(state, 24); buffer.writeVarInt(deaths);
            buffer.writeVarInt(seconds); buffer.writeVarInt(level); buffer.writeVarInt(players);
            buffer.writeFloat(sanity); buffer.writeVarInt(battery); buffer.writeVarInt(voteYes); buffer.writeVarInt(voteRequired); buffer.writeVarInt(voteSeconds);
            buffer.writeVarInt(panels);buffer.writeVarInt(charge);buffer.writeUtf(difficulty,24);buffer.writeUtf(objective,160);
        }
    }

    static void register() {
        CHANNEL.messageBuilder(Hud.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Hud::encode).decoder(Hud::decode)
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> BedroomHud.update(message)))
                .add();
        CHANNEL.messageBuilder(Intent.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeBoolean(m.pressing))
                .decoder(b->new Intent(b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)NoClipSystem.input(p,m.pressing);}).add();
        CHANNEL.messageBuilder(Blackout.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->b.writeVarInt(m.ticks))
                .decoder(b->new Blackout(b.readVarInt())).consumerMainThread((m,c)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->NoClipClient.blackout(m.ticks))).add();
    }
    private record Intent(boolean pressing){}
    private record Blackout(int ticks){}
    static void intent(boolean pressing){CHANNEL.sendToServer(new Intent(pressing));}
    static void blackout(ServerPlayer player){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Blackout(20));}

    static Hud snapshot(MatchData data, java.util.UUID id) {
        MatchData.Member m = data.members.get(id);
        boolean waiting = data.waiting.containsKey(id);
        int ticks = m == null ? 0 : m.state == MatchData.State.LIMBO ? m.limboTicks : (m.state == MatchData.State.REST||m.state==MatchData.State.VICTORY) ? data.restTicks : 0;
        var p = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() == null ? null
                : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(id);
        var lamp = p == null ? net.minecraft.world.item.ItemStack.EMPTY : FlashlightSystem.held(p);
        return new Hud(m != null || waiting, m == null ? (data.observers.contains(id) ? "OBSERVING" : "WAITING") : m.state.name(), m == null ? 0 : m.deaths,
                (ticks + 19) / 20, data.level, data.members.size(), m == null ? 100 : (float) m.survival.sanity,
                lamp.isEmpty() ? -1 : SurvivalItems.charge(lamp) * 100 / SurvivalItems.FULL_CHARGE,
                data.vote.yes(), data.vote.voters.size() / 2 + 1, (data.vote.ticks + 19) / 20,data.mission.count(),NoClipSystem.charge(id),data.difficulty.id+" "+data.difficulty.size+"×"+data.difficulty.size,
                p==null?"":MatchArena.direction(p,MatchArena.exit(data)));
    }

    static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot(MatchData.get(player.server), player.getUUID()));
    }

    private BedroomNetwork() {}
}
