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
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Bedroom.MOD_ID, "match"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public record Hud(boolean visible, String state, int deaths, int seconds, int level, int players) {
        static Hud decode(FriendlyByteBuf buffer) {
            return new Hud(buffer.readBoolean(), buffer.readUtf(24), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
        void encode(FriendlyByteBuf buffer) {
            buffer.writeBoolean(visible); buffer.writeUtf(state, 24); buffer.writeVarInt(deaths);
            buffer.writeVarInt(seconds); buffer.writeVarInt(level); buffer.writeVarInt(players);
        }
    }

    static void register() {
        CHANNEL.messageBuilder(Hud.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Hud::encode).decoder(Hud::decode)
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> BedroomHud.update(message)))
                .add();
    }

    static Hud snapshot(MatchData data, java.util.UUID id) {
        MatchData.Member m = data.members.get(id);
        boolean waiting = data.waiting.containsKey(id);
        int ticks = m == null ? 0 : m.state == MatchData.State.LIMBO ? m.limboTicks : m.state == MatchData.State.REST ? data.restTicks : 0;
        return new Hud(m != null || waiting, m == null ? "WAITING" : m.state.name(), m == null ? 0 : m.deaths,
                (ticks + 19) / 20, data.level, data.members.size());
    }

    static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), snapshot(MatchData.get(player.server), player.getUUID()));
    }

    private BedroomNetwork() {}
}
